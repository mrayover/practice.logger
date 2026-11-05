package com.mrayover.practicelogger

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

internal class ExportStorage(context: Context) {
    private val resolver = context.contentResolver
    private val prefs = context.getSharedPreferences("recording-export", Context.MODE_PRIVATE)

    companion object {
        const val MODES = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        private val operation = Mutex()
    }

    data class Destination(val uri: Uri, val label: String)
    private data class Entry(val id: String, val name: String, val mime: String, val flags: Int) {
        val directory get() = mime == Document.MIME_TYPE_DIR
        val canCreate get() = directory && flags and Document.FLAG_DIR_SUPPORTS_CREATE != 0
    }

    fun selected(): Destination? = prefs.getString("root", null)?.let {
        Destination(Uri.parse(it), prefs.getString("label", null) ?: "Selected folder")
    }

    fun unfinishedCopy(): String? = prefs.getString("pending-name", null)

    suspend fun accessible(destination: Destination): Boolean = withContext(Dispatchers.IO) {
        runCatching { root(destination.uri) }.isSuccess
    }

    suspend fun choose(uri: Uri, flags: Int) = withContext(Dispatchers.IO) {
        operation.withLock {
            val previous = selected()
            require(DocumentsContract.isTreeUri(uri) && flags and MODES == MODES) {
                "Choose a folder that allows reading and writing."
            }
            val alreadyHeld = persistedModes(uri)
            resolver.takePersistableUriPermission(uri, flags and MODES)
            try {
                val entry = root(uri)
                check(prefs.edit().putString("root", uri.toString()).putString("label", entry.name).commit())
            } catch (failure: Exception) {
                if (previous?.uri != uri) release(uri, MODES and alreadyHeld.inv())
                throw failure
            }
            if (previous != null && previous.uri != uri) release(previous.uri, persistedModes(previous.uri))
        }
    }

    suspend fun forget() = withContext(Dispatchers.IO) {
        operation.withLock {
            val previous = selected() ?: return@withLock
            check(prefs.edit().remove("root").remove("label").commit())
            release(previous.uri, persistedModes(previous.uri))
        }
    }

    suspend fun export(destination: Destination, take: BlockTake, blockName: String, source: File,
        copy: suspend (java.io.OutputStream) -> Long): String = withContext(Dispatchers.IO) {
        operation.withLock {
            check(source.isFile && source.length() > 0) { "Saved audio is unavailable." }
            val tree = destination.uri
            val root = try {
                check(selected()?.uri == tree)
                root(tree)
            } catch (_: Exception) {
                error("The selected export folder is unavailable. Choose a folder again or use Save As.")
            }
            val recordings = folder(tree, root, "Recordings")
            val suffix = take.blockId?.let { " - ${exportBlockIdentity(it)}" }
            val owner = folder(tree, recordings,
                suffix?.let { exportSafeName(blockName, "Block") + it } ?: "Free Recordings", suffix)
            val day = folder(tree, owner, exportDate(take))
            val name = exportFileName(take)
            val siblings = children(tree, day)
            val unique = uniqueExportName(name, siblings.map { it.name })
            val output = create(tree, day, take.audioMime, unique, siblings)
            write(output, "${destination.label}/${recordings.name}/${owner.name}/${day.name}/$unique", source, copy)
        }
    }

    suspend fun saveAs(uri: Uri, name: String, source: File, copy: suspend (java.io.OutputStream) -> Long): String =
        withContext(Dispatchers.IO) { operation.withLock { write(uri, name, source, copy) } }

    data class ZipPublication(val location: String, val readBack: Boolean)

    fun unfinishedZipCopy(): String? = prefs.getString("zip-pending-name", null)

    suspend fun saveZipAs(uri: Uri, name: String, source: File): ZipPublication =
        withContext(Dispatchers.IO) { operation.withLock { publishZip(uri, name, source) } }

    suspend fun saveZipInSelectedFolder(name: String, source: File): ZipPublication = withContext(Dispatchers.IO) {
        operation.withLock {
            val destination = checkNotNull(selected()) { "Choose a backup folder in Settings or use Save As." }
            val tree = destination.uri
            val backups = folder(tree, root(tree), "Backups")
            val siblings = children(tree, backups)
            val finalName = uniqueExportName(name, siblings.map { it.name })
            val pendingName = uniqueExportName("Pending-$finalName", siblings.map { it.name })
            val pending = create(tree, backups, "application/zip", pendingName, siblings)
            val prefix = "${destination.label}/${backups.name}/"
            publishZip(pending, prefix + pendingName, source) {
                val document = entries(pending).single()
                check(document.flags and Document.FLAG_SUPPORTS_RENAME != 0) {
                    "The selected folder does not support safe ZIP publication. Use Save As."
                }
                val latestNames = children(tree, backups).map { it.name }
                val freshName = uniqueExportName(finalName, latestNames)
                val renamed = checkNotNull(DocumentsContract.renameDocument(resolver, pending, freshName)) {
                    "The provider could not publish the verified ZIP. Use Save As."
                }
                val safeUri = documentUri(tree, DocumentsContract.getDocumentId(renamed))
                prefix + (query(safeUri, arrayOf(Document.COLUMN_DISPLAY_NAME), optional = true)
                    .singleOrNull()?.firstOrNull() ?: freshName)
            }
        }
    }

    private suspend fun publishZip(uri: Uri, name: String, source: File,
        finalize: (() -> String)? = null): ZipPublication {
        try {
            check(prefs.edit().putString("zip-pending-name", name).commit()) { "Could not record the pending backup copy." }
            val size = source.length()
            require(source.isFile && size in 1..PORTABLE_ZIP_LIMIT)
            val written = checkNotNull(resolver.openOutputStream(uri, "wt")) { "The provider refused the backup file." }.use { output ->
                source.inputStream().use { copyPortable(it, output, size) }.also { output.flush() }
            }
            check(written.bytes == size) { "The verified private ZIP could not be read completely." }
            val input = try { resolver.openInputStream(uri) }
            catch (_: java.io.FileNotFoundException) { null }
            catch (_: SecurityException) { null }
            val readBack = input?.use { copyPortable(it, null, size) }
            if (readBack != null) check(readBack == written) { "The destination ZIP failed integrity verification." }
            currentCoroutineContext().ensureActive()
            check(finalize == null || readBack != null) { "The selected folder cannot verify the ZIP. Use Save As." }
            val location = finalize?.invoke() ?: name
            prefs.edit().remove("zip-pending-name").commit()
            return ZipPublication(location, readBack != null)
        } catch (failure: Exception) {
            val removed = runCatching { DocumentsContract.deleteDocument(resolver, uri) }.getOrDefault(false)
            if (removed) prefs.edit().remove("zip-pending-name").commit()
            if (failure is CancellationException) throw failure
            val cleanup = if (removed) "Incomplete copy removed." else "A partial copy may remain at $name."
            throw IllegalStateException("${failure.message ?: "Backup copy failed."} $cleanup", failure)
        }
    }

    private suspend fun write(uri: Uri, path: String, source: File, copy: suspend (java.io.OutputStream) -> Long): String {
        try {
            check(prefs.edit().putString("pending-name", path).putString("pending-uri", uri.toString()).commit()) {
                "Could not record the pending copy."
            }
            val size = source.length()
            check(source.isFile && size > 0) { "Saved audio is unavailable." }
            val copied = checkNotNull(resolver.openOutputStream(uri, "w")) { "The provider refused the file." }.use { copy(it) }
            check(copied == size) { "The copy length did not match the recording." }
            currentCoroutineContext().ensureActive()
            val document = query(uri, arrayOf(Document.COLUMN_DISPLAY_NAME, Document.COLUMN_SIZE), optional = true)
            val actualSize = document.singleOrNull()?.get(1)?.toLongOrNull()
            check(actualSize == null || actualSize == size) { "The provider reported an incomplete copy." }
            val actualName = document.singleOrNull()?.firstOrNull()
            clearPending()
            return actualName?.let { path.substringBeforeLast('/', "").let { parent ->
                if (parent.isEmpty()) it else "$parent/$it"
            } } ?: path
        } catch (failure: Exception) {
            // Only delete the document this attempt was given or created.
            val removed = runCatching { DocumentsContract.deleteDocument(resolver, uri) }.getOrDefault(false)
            if (removed) clearPending()
            if (failure is CancellationException) throw failure
            val reason = if (failure is IllegalStateException) failure.message else null
            error(listOfNotNull(reason, if (removed) "Incomplete copy removed." else "A partial copy may remain at $path.").joinToString(" "))
        }
    }

    private fun clearPending() {
        prefs.edit().remove("pending-name").remove("pending-uri").apply()
    }

    private fun persistedModes(uri: Uri): Int {
        val grant = resolver.persistedUriPermissions.firstOrNull { it.uri == uri } ?: return 0
        return (if (grant.isReadPermission) Intent.FLAG_GRANT_READ_URI_PERMISSION else 0) or
            (if (grant.isWritePermission) Intent.FLAG_GRANT_WRITE_URI_PERMISSION else 0)
    }

    private fun release(uri: Uri, modes: Int) {
        if (modes != 0) runCatching { resolver.releasePersistableUriPermission(uri, modes) }
    }

    private fun root(tree: Uri): Entry {
        check(persistedModes(tree) == MODES)
        val uri = documentUri(tree, DocumentsContract.getTreeDocumentId(tree))
        return entries(uri).single().also { check(it.canCreate) }
    }

    private fun documentUri(tree: Uri, id: String) = DocumentsContract.buildDocumentUriUsingTree(tree, id)

    private fun folder(tree: Uri, parent: Entry, name: String, suffix: String? = null): Entry {
        val siblings = children(tree, parent)
        val matches = siblings.filter {
            it.name.equals(name, ignoreCase = true) || (suffix != null && it.name.endsWith(suffix, ignoreCase = true))
        }
        check(matches.size <= 1 && matches.all { it.directory }) { "An export folder name is ambiguous or occupied." }
        matches.singleOrNull()?.let { return it }
        val created = create(tree, parent, Document.MIME_TYPE_DIR, name, siblings)
        return entries(created).single().also {
            check(it.directory && (it.name.equals(name, ignoreCase = true) ||
                (suffix != null && it.name.endsWith(suffix, ignoreCase = true)))) { "The provider changed the folder name." }
        }
    }

    private fun create(tree: Uri, parent: Entry, mime: String, name: String, siblings: List<Entry>): Uri {
        check(parent.canCreate) { "The provider does not allow creation here." }
        val uri = checkNotNull(DocumentsContract.createDocument(resolver, documentUri(tree, parent.id), mime, name)) {
            "The provider refused to create $name."
        }
        val id = DocumentsContract.getDocumentId(uri)
        check(uri.authority == tree.authority && id != parent.id && siblings.none { it.id == id }) {
            "The provider did not return a new document."
        }
        return documentUri(tree, id)
    }

    private fun children(tree: Uri, parent: Entry) =
        entries(DocumentsContract.buildChildDocumentsUriUsingTree(tree, parent.id))

    private fun entries(uri: Uri) = query(uri, arrayOf(Document.COLUMN_DOCUMENT_ID, Document.COLUMN_DISPLAY_NAME,
        Document.COLUMN_MIME_TYPE, Document.COLUMN_FLAGS)).map {
        Entry(checkNotNull(it[0]), checkNotNull(it[1]), checkNotNull(it[2]), it[3]?.toIntOrNull() ?: 0)
    }

    private fun query(uri: Uri, columns: Array<String>, optional: Boolean = false): List<List<String?>> {
        val read = {
            checkNotNull(resolver.query(uri, columns, null, null, null)).use { cursor ->
                check(!cursor.extras.getBoolean(DocumentsContract.EXTRA_LOADING)) { "The provider is still loading. Try again or use Save As." }
                val indices = columns.map(cursor::getColumnIndex)
                buildList {
                    while (cursor.moveToNext()) add(indices.map { if (it < 0 || cursor.isNull(it)) null else cursor.getString(it) })
                }
            }
        }
        return if (optional) runCatching(read).getOrDefault(emptyList()) else read()
    }
}

internal fun exportSafeName(value: String, fallback: String): String =
    value.replace(Regex("[\\\\/:*?\"<>|\\p{C}]"), "_").trim { it.isWhitespace() || it == '.' }
        .let { name -> String(name.codePoints().limit(40).toArray(), 0, name.codePointCount(0, name.length).coerceAtMost(40)) }
        .trimEnd(' ', '.').ifBlank { fallback }

internal fun exportBlockIdentity(id: String): String =
    if (id.matches(Regex("[a-f0-9-]{36}"))) id else java.security.MessageDigest.getInstance("SHA-256")
        .digest(id.toByteArray(Charsets.UTF_8)).take(16).joinToString("") { "%02x".format(it) }

private fun exportTime(take: BlockTake) = Instant.ofEpochMilli(take.createdAt).atZone(ZoneId.systemDefault())

internal fun exportDate(take: BlockTake): String = exportTime(take).toLocalDate().toString()

internal fun exportFileName(take: BlockTake): String {
    val time = exportTime(take).format(DateTimeFormatter.ofPattern("HH-mm-ss", Locale.ROOT))
    val label = when (take.kind) {
        TakeKind.RECORDED_LOOP -> "${take.beatCount}beats ${take.recordedBpm}bpm"
        TakeKind.IMPORTED_LOOP -> exportSafeName(take.label, "Imported loop")
        TakeKind.FREE -> exportSafeName(take.label, "Take")
    }
    return "$time - $label - ${exportBlockIdentity(take.id).take(8)}.${take.fileName.substringAfterLast('.')}"
}

internal fun uniqueExportName(name: String, existing: List<String>): String {
    val occupied = existing.mapTo(hashSetOf()) { it.lowercase(Locale.ROOT) }
    if (name.lowercase(Locale.ROOT) !in occupied) return name
    val stem = name.substringBeforeLast('.')
    val extension = name.substringAfterLast('.')
    return generateSequence(2) { it + 1 }.map { "$stem ($it).$extension" }
        .first { it.lowercase(Locale.ROOT) !in occupied }
}