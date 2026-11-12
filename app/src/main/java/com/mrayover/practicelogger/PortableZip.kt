package com.mrayover.practicelogger

import android.content.Context
import android.util.JsonReader
import android.util.JsonToken
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.RandomAccessFile
import java.security.MessageDigest
import java.util.zip.CRC32
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

internal const val PORTABLE_ZIP_LIMIT = 3L * 1024 * 1024 * 1024
internal const val PORTABLE_SPACE_RESERVE = 8L * 1024 * 1024
private const val METADATA_LIMIT = 16L * 1024 * 1024
private const val ENTRY_LIMIT = 12_000
private const val VALUE_LIMIT = 500_000
private const val FORMAT = "practice.logger.portable"
private const val RECORDS = "records.json"
private const val MANIFEST = "manifest.json"

internal suspend fun <T> withPortableMediaLocks(action: suspend () -> T): T =
    ImageReferenceRepository.filesMutex.withLock {
        MediaLibraryRepository.filesMutex.withLock {
            TakeRepository.filesMutex.withLock { action() }
        }
    }

internal data class PortableEntry(val path: String, val type: String, val owner: String, val bytes: Long, val sha256: String) {
    fun encode() = JSONObject().put("path", path).put("type", type).put("owner", owner)
        .put("bytes", bytes).put("sha256", sha256)

    companion object {
        fun decode(json: JSONObject): PortableEntry {
            require(json.keys().asSequence().toSet() == setOf("path", "type", "owner", "bytes", "sha256"))
            val size = json.get("bytes")
            require(size is Int || size is Long)
            return PortableEntry(json.get("path") as String, json.get("type") as String, json.get("owner") as String,
                (size as Number).toLong(), json.get("sha256") as String).also {
                require(it.bytes in 1..PORTABLE_ZIP_LIMIT && portableDigest.matches(it.sha256))
            }
        }
    }
}

internal data class VerifiedPortableZip(
    val records: PortableRecords,
    val preferences: PortablePreferences,
    val originals: List<PortableOriginal>,
    val inventory: Map<String, PortableEntry>,
    val mediaBytes: Long,
    val appVersion: String
)

internal data class PortableCopy(val bytes: Long, val sha256: String, val crc: Long)

/** Bound actual bytes even when the provider or ZIP header claims a different length. */
internal suspend fun copyPortable(input: InputStream, output: OutputStream?, limit: Long, space: File? = null): PortableCopy {
    val digest = MessageDigest.getInstance("SHA-256")
    val crc = CRC32()
    val buffer = ByteArray(64 * 1024)
    var total = 0L
    var nextSpaceCheck = 0L
    while (true) {
        currentCoroutineContext().ensureActive()
        val count = input.read(buffer)
        if (count < 0) break
        require(count > 0 && total + count <= limit) { "Backup entry exceeds its size limit." }
        if (space != null && total >= nextSpaceCheck) {
            check(space.usableSpace >= PORTABLE_SPACE_RESERVE + buffer.size) { "Free storage before continuing." }
            nextSpaceCheck = total + 1024 * 1024
        }
        output?.write(buffer, 0, count)
        digest.update(buffer, 0, count)
        crc.update(buffer, 0, count)
        total += count
    }
    return PortableCopy(total, digest.digest().joinToString("") { "%02x".format(it) }, crc.value)
}

/** Strict JSON with bounded nesting and duplicate-key rejection. */
internal fun readPortableObject(bytes: ByteArray, allowDecimals: Boolean = false): JSONObject {
    val text = Charsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(bytes)).toString()
    JsonReader(text.reader()).use { reader ->
        var values = 0
        fun value(depth: Int): Any {
            require(++values <= VALUE_LIMIT) { "Too many backup metadata values." }
            require(depth <= 12) { "Backup metadata nesting is too deep." }
            return when (reader.peek()) {
                JsonToken.BEGIN_OBJECT -> JSONObject().apply {
                    reader.beginObject()
                    while (reader.hasNext()) {
                        val name = reader.nextName()
                        require(!has(name)) { "Duplicate metadata key." }
                        put(name, value(depth + 1))
                    }
                    reader.endObject()
                }
                JsonToken.BEGIN_ARRAY -> JSONArray().apply {
                    reader.beginArray()
                    while (reader.hasNext()) {
                        require(length() < ZIP_RECORD_LIMIT) { "Too many backup metadata entries." }
                        put(value(depth + 1))
                    }
                    reader.endArray()
                }
                JsonToken.STRING -> reader.nextString()
                JsonToken.NUMBER -> {
                    val number = reader.nextString()
                    number.toLongOrNull() ?: run {
                        require(allowDecimals && ('.' in number || 'e' in number.lowercase())) { "Expected a whole number." }
                        requireNotNull(number.toDoubleOrNull()?.takeIf { it.isFinite() }) { "Expected a finite number." }
                    }
                }
                JsonToken.BOOLEAN -> reader.nextBoolean()
                JsonToken.NULL -> { reader.nextNull(); JSONObject.NULL }
                else -> error("Invalid backup metadata.")
            }
        }
        val result = value(0) as? JSONObject ?: error("Invalid backup metadata object.")
        require(reader.peek() == JsonToken.END_DOCUMENT) { "Trailing backup metadata." }
        return result
    }
}

internal class PortableZip(
    private val context: Context,
    private val db: BlockDatabase,
    private val files: File = context.filesDir
) {
    suspend fun create(destination: File): VerifiedPortableZip = withContext(Dispatchers.IO) {
        require(!destination.exists())
        try {
            withPortableMediaLocks {
                val payload = db.withTransaction {
                    val sql = db.openHelper.writableDatabase
                    val tables = JSONObject()
                    var estimate = 0L
                    var values = portableTables.size + 16
                    for (table in portableTables) {
                        tables.put(table.name, table.snapshot(sql) { row ->
                            values += row.length() + 1
                            require(values <= VALUE_LIMIT) { "Too many backup record values." }
                            estimate += row.toString().toByteArray(Charsets.UTF_8).size + 1L
                            require(estimate <= METADATA_LIMIT) { "Portable backup records exceed 16 MiB." }
                        })
                    }
                    JSONObject().put("version", PORTABLE_VERSION).put("tables", tables)
                        .put("preferences", PortablePreferences.snapshot(context).encode())
                }
                val records = PortableRecords(payload, PORTABLE_VERSION)
                val originals = records.originals()
                require(originals.size + 2 <= ENTRY_LIMIT) { "Too many originals for one archive." }
                var mediaSize = 0L
                for (original in originals) {
                    val file = original.file(files)
                    records.validateOverlayPages(original, original.validateFile(file, db))
                    mediaSize = Math.addExact(mediaSize, file.length())
                    require(mediaSize <= PORTABLE_ZIP_LIMIT - METADATA_LIMIT * 2)
                }
                val recordBytes = payload.toString().toByteArray(Charsets.UTF_8)
                require(recordBytes.size <= METADATA_LIMIT)
                val parent = checkNotNull(destination.parentFile)
                check(parent.isDirectory || parent.mkdirs())
                check(parent.usableSpace >= mediaSize + recordBytes.size + PORTABLE_SPACE_RESERVE) {
                    "Free enough private storage for a complete backup."
                }
                val manifestEntries = JSONArray()
                FileOutputStream(destination).use { file ->
                    ZipOutputStream(file.buffered()).use { zip ->
                        suspend fun add(path: String, type: String, owner: String, input: InputStream, bound: Long): PortableEntry {
                            zip.putNextEntry(ZipEntry(path))
                            val copy = input.use { copyPortable(it, zip, bound, parent) }
                            zip.closeEntry()
                            return PortableEntry(path, type, owner, copy.bytes, copy.sha256)
                        }
                        manifestEntries.put(add(RECORDS, "records", "app", recordBytes.inputStream(), METADATA_LIMIT).encode())
                        zip.setLevel(Deflater.NO_COMPRESSION)
                        for (original in originals) {
                            val source = original.file(files)
                            val entry = add(original.path, original.store, original.owner, source.inputStream(), original.limit)
                            require(entry.bytes == source.length() && (original.bytes == null || entry.bytes == original.bytes))
                            require(original.originalHash == null || entry.sha256 == original.originalHash) {
                                "Original integrity check failed for ${original.owner}."
                            }
                            manifestEntries.put(entry.encode())
                        }
                        zip.setLevel(Deflater.DEFAULT_COMPRESSION)
                        @Suppress("DEPRECATION")
                        val app = context.packageManager.getPackageInfo(context.packageName, 0)
                        val manifest = JSONObject().put("format", FORMAT).put("version", PORTABLE_VERSION).put("roomSchema", 25)
                            .put("appVersion", app.versionName ?: "unknown").put("appVersionCode", app.versionCode)
                            .put("counts", records.counts).put("entries", manifestEntries)
                            .toString().toByteArray(Charsets.UTF_8)
                        require(manifest.size <= METADATA_LIMIT)
                        add(MANIFEST, "manifest", "app", manifest.inputStream(), METADATA_LIMIT)
                        zip.finish()
                        zip.flush()
                        file.fd.sync()
                    }
                }
            }
            verify(destination)
        } catch (failure: Throwable) {
            destination.delete()
            throw failure
        }
    }

    /** If [stage] is supplied, originals are materialized only at validated private paths. */
    suspend fun verify(source: File, stage: File? = null): VerifiedPortableZip = withContext(Dispatchers.IO) {
        val expectedEntries = checkZipDirectory(source)
        ZipFile(source).use { zip ->
            val entries = linkedMapOf<String, ZipEntry>()
            val iterator = zip.entries()
            var expanded = 0L
            while (iterator.hasMoreElements()) {
                val entry = iterator.nextElement()
                require(!entry.isDirectory && entry.size in 1..PORTABLE_ZIP_LIMIT && entries.put(entry.name, entry) == null) {
                    "Invalid or duplicate ZIP entry."
                }
                expanded = Math.addExact(expanded, entry.size)
                require(entries.size <= ENTRY_LIMIT && expanded <= PORTABLE_ZIP_LIMIT)
            }
            require(entries.size == expectedEntries)
            suspend fun read(entry: ZipEntry, limit: Long, expected: PortableEntry?, output: OutputStream?): PortableCopy {
                require(entry.size <= limit && (expected == null || expected.bytes == entry.size))
                val copy = zip.getInputStream(entry).use { copyPortable(it, output, entry.size, stage) }
                require(copy.bytes == entry.size && copy.crc == entry.crc && (expected == null || copy.sha256 == expected.sha256)) {
                    "Backup integrity check failed for ${entry.name}."
                }
                return copy
            }
            suspend fun metadata(path: String, expected: PortableEntry? = null, allowDecimals: Boolean = false): JSONObject {
                val entry = requireNotNull(entries[path]) { "Backup is missing $path." }
                val output = ByteArrayOutputStream()
                read(entry, METADATA_LIMIT, expected, output)
                return readPortableObject(output.toByteArray(), allowDecimals)
            }
            val manifest = metadata(MANIFEST)
            require(manifest.keys().asSequence().toSet() == setOf("format", "version", "roomSchema", "appVersion", "appVersionCode", "counts", "entries"))
            val version = when (manifest.get("version")) {
                1L -> 1
                2L -> 2
                3L -> 3
                4L -> 4
                5L -> 5
                6L -> 6
                7L -> 7
                8L -> 8
                9L -> 9
                PORTABLE_VERSION.toLong() -> PORTABLE_VERSION
                else -> error("Unsupported portable backup format or version.")
            }
            val roomVersions = when (version) {
                1 -> setOf(16L, 17L)
                2 -> setOf(18L)
                3 -> setOf(19L)
                4 -> setOf(20L)
                5 -> setOf(21L)
                6 -> setOf(22L)
                7 -> setOf(23L)
                8 -> setOf(24L)
                else -> setOf(25L)
            }
            require(manifest.get("format") == FORMAT && manifest.get("roomSchema") in roomVersions) {
                "Unsupported portable backup format or version."
            }
            val appVersion = manifest.get("appVersion") as String
            require(appVersion.length in 1..80 && (manifest.get("appVersionCode") as? Long ?: 0) > 0)
            val inventory = manifest.getJSONArray("entries")
            val listed = linkedMapOf<String, PortableEntry>()
            for (i in 0 until inventory.length()) {
                val item = PortableEntry.decode(inventory.getJSONObject(i))
                require(listed.put(item.path, item) == null) { "Duplicate archive inventory entry." }
            }
            require(MANIFEST !in listed && listed.keys + MANIFEST == entries.keys) { "ZIP entries do not match the manifest." }
            val record = requireNotNull(listed[RECORDS])
            require(record.type == "records" && record.owner == "app")
            val data = PortableRecords(metadata(RECORDS, record, allowDecimals = version >= 2), version)
            val counts = manifest.getJSONObject("counts")
            require(counts.keys().asSequence().toSet() == data.counts.keys().asSequence().toSet())
            require(counts.keys().asSequence().all { counts.get(it) == data.counts.getInt(it).toLong() })
            val settings = PortablePreferences.decode(data.preferences)
            val originals = data.originals()
            require(listed.keys == originals.mapTo(hashSetOf(RECORDS)) { it.path }) { "Unexpected or missing original paths." }
            data.validateRelationships(context)
            val mediaBytes = originals.sumOf { listed.getValue(it.path).bytes }
            if (stage != null) {
                require(stage.isDirectory && stage.list()?.isEmpty() == true)
                check(stage.usableSpace >= mediaBytes + PORTABLE_SPACE_RESERVE) { "Free storage to stage the restored originals." }
            }
            for (original in originals) {
                val expected = listed.getValue(original.path)
                require(expected.type == original.store && expected.owner == original.owner && expected.bytes <= original.limit)
                require(original.bytes == null || expected.bytes == original.bytes)
                require(original.originalHash == null || expected.sha256 == original.originalHash)
                val entry = entries.getValue(original.path)
                if (stage == null) read(entry, original.limit, expected, null)
                else {
                    val file = original.file(stage)
                    val parent = file.parentFile!!
                    check(parent.isDirectory || parent.mkdirs())
                    FileOutputStream(file).use { output ->
                        read(entry, original.limit, expected, output)
                        output.fd.sync()
                    }
                    data.validateOverlayPages(original, original.validateFile(file, db))
                }
            }
            VerifiedPortableZip(data, settings, originals, listed - RECORDS, mediaBytes, appVersion)
        }
    }

    /** The portable format uses a single-disk, comment-free ZIP32 directory. Bound it before opening ZipFile. */
    private fun checkZipDirectory(source: File): Int = RandomAccessFile(source, "r").use { file ->
        val length = file.length()
        require(length in 22..PORTABLE_ZIP_LIMIT) { "Portable ZIP is truncated or larger than 3 GiB." }
        file.seek(length - 22)
        require(file.readInt() == 0x504b0506 && file.readInt() == 0) { "Invalid portable ZIP directory." }
        fun short() = java.lang.Short.reverseBytes(file.readShort()).toInt() and 0xffff
        fun uint() = Integer.reverseBytes(file.readInt()).toLong() and 0xffffffffL
        val count = short()
        require(count in 2..ENTRY_LIMIT && short() == count)
        val size = uint()
        val offset = uint()
        require(short() == 0 && size in 1..(ENTRY_LIMIT * 256L) && offset + size == length - 22) {
            "Invalid portable ZIP directory bounds."
        }
        count
    }
}
