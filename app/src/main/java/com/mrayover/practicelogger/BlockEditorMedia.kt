package com.mrayover.practicelogger

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.*
import androidx.room.withTransaction
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.io.File
import java.time.LocalDate

/** An editor owns temporary originals only. Canonical ownership starts at the atomic Save. */
internal class BlockEditorMedia(private val app: Application, private val db: BlockDatabase,
    private val library: MediaLibraryRepository, private val legacy: ImageReferenceRepository,
    private val associations: BlockMediaRepository) {
    private val root = File(app.filesDir, "block-editor-staging")
    private lateinit var directory: File
    private lateinit var stageLibrary: MediaLibraryRepository
    private var blockId: String? = null
    private var visualCheck = VisualChangeCheck(null, false)
    private var originalAudio: String? = null
    private var originalVisual: String? = null
    var ready by mutableStateOf(false); private set
    var visual by mutableStateOf<MediaAsset?>(null); private set
    var audio by mutableStateOf<MediaAsset?>(null); private set
    var visualChanged by mutableStateOf(false); private set
    var audioChanged by mutableStateOf(false); private set
    private var staged = linkedMapOf<String, MediaAsset>()
    val dirty get() = visualChanged || audioChanged
    private var token: String? = null
    private var creationId = java.util.UUID.randomUUID().toString()
    private var savedId: String? = null

    suspend fun open(session: String, id: String?) = withContext(Dispatchers.IO) {
        if (token == session && ready) return@withContext
        require(session.matches(Regex("[0-9a-f-]{36}")))
        ready = false; token = session; blockId = id; savedId = null; creationId = java.util.UUID.randomUUID().toString()
        directory = File(root, session)
        check(directory.mkdirs() || directory.isDirectory)
        stageLibrary = MediaLibraryRepository(db, directory)
        // Deterministic recovery: reopen the saved editor manifest; expire abandoned sessions after 7 days.
        root.listFiles()?.filter { it.isDirectory && it.name != session &&
            it.name.matches(Regex("[0-9a-f-]{36}")) && System.currentTimeMillis() - it.lastModified() > 7 * 86_400_000L }
            ?.forEach { it.deleteRecursively() }
        db.withTransaction {
            visualCheck = id?.let { db.reviewVisualChange(it) } ?: VisualChangeCheck(null, false)
            originalVisual = id?.let { db.blockMedia().find(it, "VISUAL")?.mediaId }
            originalAudio = id?.let { db.blockMedia().find(it, "AUDIO")?.mediaId }
            visual = originalVisual?.let { db.media().find(it) }
            audio = originalAudio?.let { db.media().find(it) }
        }
        staged.clear(); visualChanged = false; audioChanged = false
        val manifest = File(directory, "draft.json")
        if (manifest.exists()) {
            val json = JSONObject(manifest.readText())
            creationId = json.getString("creationId")
            savedId = json.optString("savedId").ifEmpty { null }
                ?: creationId.takeIf { id == null && db.blocks().find(it) != null }
            if (savedId != null && db.blocks().find(checkNotNull(savedId)) != null) {
                ready = true; return@withContext
            }
            check(json.optString("block") == (id ?: ""))
            visualCheck = VisualChangeCheck(json.optString("identity").ifEmpty { null }, json.getBoolean("markings"))
            originalAudio = json.optString("originalAudio").ifEmpty { null }
            originalVisual = json.optString("originalVisual").ifEmpty { null }
            val items = json.getJSONArray("staged")
            for (index in 0 until items.length()) {
                val item = items.getJSONObject(index)
                val asset = MediaAsset(item.getString("id"), item.getString("title"), item.getString("name"),
                    item.getString("mime"), item.getString("category"), item.getString("file"), item.getLong("bytes"),
                    item.getLong("time"), item.optInt("width").takeIf { it > 0 }, item.optInt("height").takeIf { it > 0 },
                    item.optInt("pages").takeIf { it > 0 }, item.optLong("duration").takeIf { it > 0 }, item.getString("hash"))
                check(stageLibrary.available(asset)) { "A pending original is unavailable. Discard this editor and select it again." }
                staged[asset.id] = asset
            }
            visualChanged = json.getBoolean("visualChanged"); audioChanged = json.getBoolean("audioChanged")
            suspend fun selected(key: String): MediaAsset? = json.optString(key).ifEmpty { null }?.let { staged[it] ?: db.media().find(it) }
            visual = selected("visual"); audio = selected("audio")
        }
        // Incomplete copies/promotions not referenced by the synced manifest belong only to this editor.
        directory.listFiles()?.filter { it.name != "draft.json" && it.name !in staged.values.map { a -> a.fileName } }
            ?.forEach { it.delete() }
        persist()
        ready = true
    }

    private fun persist() {
        val json = JSONObject().put("savedId", savedId ?: "").put("creationId", creationId).put("block", blockId ?: "").put("identity", visualCheck.identity ?: "")
            .put("markings", visualCheck.hasMarkings).put("originalAudio", originalAudio ?: "")
            .put("originalVisual", originalVisual ?: "").put("visualChanged", visualChanged).put("audioChanged", audioChanged)
            .put("visual", visual?.id ?: "").put("audio", audio?.id ?: "")
        val items = org.json.JSONArray()
        staged.values.forEach { a -> items.put(JSONObject().put("id", a.id).put("title", a.title).put("name", a.originalName)
            .put("mime", a.mimeType).put("category", a.category).put("file", a.fileName).put("bytes", a.byteCount)
            .put("time", a.importedAt).put("width", a.width).put("height", a.height).put("pages", a.pageCount)
            .put("duration", a.durationMs).put("hash", a.sha256)) }
        json.put("staged", items)
        val pending = File(directory, "draft.tmp")
        java.io.FileOutputStream(pending).use { it.write(json.toString().toByteArray()); it.fd.sync() }
        check(pending.renameTo(File(directory, "draft.json"))) { "Could not retain pending references. Try again." }
    }

    suspend fun select(role: MediaRole, asset: MediaAsset?) = withContext(Dispatchers.IO) {
        check(ready)
        require(asset == null || role.accepts(asset.category))
        if (role == MediaRole.VISUAL) { visual = asset; visualChanged = asset?.id != originalVisual || visualCheck.identity?.startsWith("legacy:") == true }
        else { audio = asset; audioChanged = asset?.id != originalAudio }
        persist()
    }
    suspend fun choose(role: MediaRole, id: String) = select(role, checkNotNull(db.media().find(id)) { "This Library item is unavailable." })
    suspend fun import(role: MediaRole, uri: Uri) = withContext(Dispatchers.IO) {
        val resolver = app.contentResolver
        val mime = resolver.getType(uri)
        val name = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst() && !it.isNull(0)) it.getString(0) else null
        } ?: when (mime) { "image/jpeg" -> "Photo.jpg"; "image/png" -> "Photo.png"; "image/webp" -> "Photo.webp"; else -> error("Choose a file with its original filename.") }
        stage(role, name, mime) { checkNotNull(resolver.openInputStream(uri)) }
    }
    internal suspend fun stage(role: MediaRole, name: String, mime: String?, open: () -> java.io.InputStream) {
        val asset = stageLibrary.stage(name, mime, role, open)
        staged[asset.id] = asset; select(role, asset)
    }
    suspend fun photo(capture: ReferencePhotoCapture, name: String) {
        try {
            val asset = stageLibrary.stage("Camera photo.jpg", "image/jpeg", MediaRole.VISUAL) { capture.file(name).inputStream() }
            staged[asset.id] = asset; select(MediaRole.VISUAL, asset)
        } finally { capture.discard(name) }
    }
    suspend fun preview(asset: MediaAsset, page: Int): ReferencePage =
        if (asset.id in staged) ImageReferenceRepository(db, directory).loadOriginalPage(File(directory, asset.fileName), asset.mimeType, page)
        else library.visualPage(asset.id, page)

    suspend fun discard() = withContext(Dispatchers.IO) {
        if (::directory.isInitialized) check(directory.deleteRecursively() || !directory.exists()) { "Temporary cleanup failed. Retry." }
        ready = false; token = null; staged.clear(); visualChanged = false; audioChanged = false
    }

    suspend fun save(id: String?, draft: BlockDraft, week: LocalDate?, appendToBuilder: Boolean = false, beforeCommit: suspend () -> Unit = {}): String = withContext(Dispatchers.IO + NonCancellable) {
        check(ready && id == blockId)
        require(draft.errors().isEmpty())
        savedId?.let { return@withContext it }
        // A creation ID is reserved only in the private manifest, never as a hidden Block.
        // A committed row is the witness after death between commit and navigation/cleanup.
        if (id == null && db.blocks().find(creationId) != null) {
            val committed = creationId; runCatching { discard() }; return@withContext committed
        }
        val selected = listOfNotNull(visual.takeIf { visualChanged }, audio.takeIf { audioChanged }).distinctBy { it.id }
        val promoted = mutableListOf<Pair<MediaAsset, File>>()
        val canonical = File(app.filesDir, "media-library")
        val result = MediaLibraryRepository.filesMutex.withLock {
            ImageReferenceRepository.filesMutex.withLock {
                try {
                    check(canonical.mkdirs() || canonical.isDirectory)
                    selected.filter { it.id in staged }.forEach { asset ->
                        check(stageLibrary.available(asset)) { "A pending original is missing or changed." }
                        val target = File(canonical, asset.fileName)
                        check(!target.exists()) { "Pending media already exists. Reopen the editor to recover." }
                        promoted += asset to target
                        File(directory, asset.fileName).inputStream().use { input -> java.io.FileOutputStream(target).use { output -> input.copyTo(output); output.fd.sync() } }
                    }
                    db.withTransaction {
                        // Compare the original identities before writing; no silent overwrite after a concurrent edit.
                        if (id != null) {
                            if (visualChanged) check(db.visualIdentity(id) == visualCheck.identity) { "Visual reference changed. Reopen this editor to review it." }
                            if (audioChanged) check(db.blockMedia().find(id, "AUDIO")?.mediaId == originalAudio) { "Audio reference changed. Reopen this editor to review it." }
                        }
                        promoted.forEach { (asset, _) -> db.media().insert(asset) }
                        selected.forEach { asset -> checkNotNull(db.media().find(asset.id)); check(library.available(asset)) { "A reference original is unavailable." } }
                        val saved = BlockRepository(db).save(id, draft, week, creationId)
                        if (visualChanged) {
                            db.discardVisualMarkings(saved, visualCheck.identity, visualCheck)
                            db.blockMedia().remove(saved, "VISUAL")
                            db.images().remove(saved)
                            visual?.let { db.blockMedia().save(BlockMediaLink(saved, "VISUAL", it.id)) }
                        }
                        if (audioChanged) {
                            // UI explicitly confirms every replacement/removal of an existing reference.
                            if (audio == null) originalAudio?.let { associations.removeAudio(saved, it, it) }
                            else associations.saveAudioLink(saved, checkNotNull(audio).id, originalAudio, originalAudio)
                        }
                        if (id == null && appendToBuilder) SessionRepository(db).addBlock(saved)
                        beforeCommit()
                        saved
                    }
                } catch (failure: Exception) {
                    // Keep bytes if commit outcome cannot be read. Canonical recovery removes only proven orphans.
                    promoted.forEach { (asset, file) -> runCatching { if (db.media().find(asset.id) == null) file.delete() } }
                    throw failure
                }
            }
        }
        savedId = result
        // Keep a tiny receipt until expiry so recreation cannot create/append a second Block.
        runCatching { persist() }
        staged.values.forEach { File(directory, it.fileName).delete() }
        staged.clear()
        runCatching { legacy.recover() }
        result
    }
}
