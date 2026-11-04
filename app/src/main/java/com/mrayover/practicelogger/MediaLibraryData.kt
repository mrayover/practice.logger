package com.mrayover.practicelogger

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.Locale
import java.util.UUID
import java.security.MessageDigest

/** Independent ownership. Future restrictive foreign keys target id; never copy originals per Block. */
@Entity(tableName = "media_assets", indices = [Index(value = ["fileName"], unique = true), Index("category"), Index("importedAt"), Index("title")])
data class MediaAsset(@PrimaryKey val id: String, val title: String, val originalName: String,
    val mimeType: String, val category: String, val fileName: String, val byteCount: Long,
    val importedAt: Long, val width: Int? = null, val height: Int? = null,
    val pageCount: Int? = null, val durationMs: Long? = null, val sha256: String = "")

@Dao interface MediaAssetDao {
    @Query("SELECT * FROM media_assets ORDER BY importedAt DESC, id") fun observeAll(): Flow<List<MediaAsset>>
    @Query("SELECT * FROM media_assets ORDER BY importedAt DESC, id") suspend fun readAll(): List<MediaAsset>
    @Query("SELECT EXISTS(SELECT 1 FROM media_assets)") suspend fun hasEntries(): Boolean
    @Query("SELECT * FROM media_assets WHERE id = :id") suspend fun find(id: String): MediaAsset?
    @Insert suspend fun insert(asset: MediaAsset)
    @Query("UPDATE media_assets SET title = :title WHERE id = :id") suspend fun rename(id: String, title: String): Int
    @Query("DELETE FROM media_assets WHERE id = :id") suspend fun remove(id: String): Int
}

val MEDIA_MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `media_assets` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `originalName` TEXT NOT NULL, `mimeType` TEXT NOT NULL, `category` TEXT NOT NULL, `fileName` TEXT NOT NULL, `byteCount` INTEGER NOT NULL, `importedAt` INTEGER NOT NULL, `width` INTEGER, `height` INTEGER, `pageCount` INTEGER, `durationMs` INTEGER, `sha256` TEXT NOT NULL, PRIMARY KEY(`id`))")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_media_assets_fileName` ON `media_assets` (`fileName`)")
        for (column in listOf("category", "importedAt", "title")) db.execSQL("CREATE INDEX IF NOT EXISTS `index_media_assets_$column` ON `media_assets` (`$column`)")
    }
}

enum class MediaFilter(val label: String, val category: String?) {
    ALL("All", null), IMAGES("Images", "IMAGE"), PDFS("PDFs", "PDF"), AUDIO("Audio", "AUDIO")
}
fun libraryItems(assets: List<MediaAsset>, query: String, filter: MediaFilter, byName: Boolean): List<MediaAsset> {
    val selected = assets.filter { (filter.category == null || it.category == filter.category) &&
        (it.title.contains(query.trim(), true) || it.originalName.contains(query.trim(), true)) }
    return if (byName) selected.sortedWith(compareBy<MediaAsset> { it.title.lowercase(Locale.ROOT) }.thenBy { it.id })
        else selected.sortedWith(compareByDescending<MediaAsset> { it.importedAt }.thenBy { it.id })
}

/** This repository only ever touches files/media-library. Legacy cleanup has separate directories/rows. */
class MediaLibraryRepository(private val db: BlockDatabase, private val directory: File,
    private val freeBytes: () -> Long = { directory.usableSpace }) {
    companion object {
        // Serialize import/finalize/commit/delete/recovery across all repository instances.
        private val filesMutex = Mutex()
        private val ownedName = Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(pending|jpg|png|webp|pdf|mp3|m4a|wav)")
        private val formats = mapOf("jpg" to "image/jpeg", "jpeg" to "image/jpeg", "png" to "image/png",
            "webp" to "image/webp", "pdf" to "application/pdf", "mp3" to "audio/mpeg", "m4a" to "audio/mp4", "wav" to "audio/wav")
        fun title(value: String): String = value.trim().also {
            require(it.length in 1..255 && it.none(Char::isISOControl)) { "Use a title of 1–255 characters without control characters." }
        }
    }
    val assets = db.media().observeAll()
    /** Callers provide only canonical IDs. File paths stay inside repository/engine code. */
    internal suspend fun original(id: String, category: String? = null): Pair<MediaAsset, File> = withContext(Dispatchers.IO) {
        val asset = checkNotNull(db.media().find(id)) { "This Library item is no longer available." }
        require(category == null || asset.category == category)
        check(available(asset)) { "The original is missing or changed. Import your original again." }
        asset to resolve(asset.fileName)
    }
    suspend fun visualPage(id: String, page: Int): ReferencePage = withContext(Dispatchers.IO) {
        filesMutex.withLock {
            val (asset, file) = original(id)
            require(asset.category in setOf("IMAGE", "PDF"))
            ImageReferenceRepository(db, directory).loadOriginalPage(file, asset.mimeType, page)
        }
    }
    private data class Integrity(val modified: Long, val size: Long, val expected: String, val valid: Boolean)
    private val integrity = java.util.concurrent.ConcurrentHashMap<String, Integrity>()
    private fun resolve(name: String): File {
        require(ownedName.matches(name)) { "Invalid Library storage name." }
        return File(directory, name).also { check(it.canonicalFile.parentFile == directory.canonicalFile) }
    }
    fun available(asset: MediaAsset): Boolean = runCatching {
        val file = resolve(asset.fileName)
        if (!file.isFile || file.length() != asset.byteCount || !file.canRead()) return@runCatching false
        val modified = file.lastModified()
        val cached = integrity[asset.fileName]
        if (cached != null && cached.modified == modified && cached.size == asset.byteCount && cached.expected == asset.sha256) cached.valid
        else {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val bytes = ByteArray(32 * 1024)
                while (true) { val count = input.read(bytes); if (count < 0) break; digest.update(bytes, 0, count) }
            }
            val valid = digest.digest().joinToString("") { "%02x".format(it) } == asset.sha256
            integrity[asset.fileName] = Integrity(modified, asset.byteCount, asset.sha256, valid)
            valid
        }
    }.getOrDefault(false)

    suspend fun import(name: String, declaredMime: String?, role: MediaRole? = null, open: () -> InputStream): MediaAsset = withContext(Dispatchers.IO) {
        filesMutex.withLock {
            require(name.isNotBlank() && name.length <= 255 && name.none(Char::isISOControl)) { "Choose a filename of 1–255 characters." }
            val extension = name.substringAfterLast('.', "").lowercase(Locale.ROOT)
            val mime = formats[extension] ?: throw IllegalArgumentException("Choose JPEG, PNG, WebP, PDF, MP3, M4A or PCM16 WAV.")
            val normalizedMime = when (declaredMime?.lowercase(Locale.ROOT)) {
                "audio/x-wav", "audio/wave", "audio/vnd.wave" -> "audio/wav"
                "audio/x-m4a", "audio/m4a" -> "audio/mp4"
                "image/jpg" -> "image/jpeg"
                else -> declaredMime?.lowercase(Locale.ROOT)
            }
            require(normalizedMime.isNullOrBlank() || normalizedMime in setOf("application/octet-stream", "application/binary") || normalizedMime == mime) {
                "The file type and filename disagree. Choose an original supported file."
            }
            val audio = mime.startsWith("audio/")
            val category = if (audio) "AUDIO" else if (mime == "application/pdf") "PDF" else "IMAGE"
            require(role == null || role.accepts(category)) { "Choose a compatible media type." }
            val limit = if (audio) AudioImport.MAX_BYTES else ImageReferenceRepository.MAX_BYTES.toLong()
            check(directory.isDirectory || directory.mkdirs()) { "Library storage is unavailable." }
            if (audio) check(freeBytes() >= AudioImport.MAX_BYTES + 1024 * 1024) { "Free at least 51 MiB before importing audio." }
            val id = UUID.randomUUID().toString()
            val pending = resolve("$id.pending")
            val digest = MessageDigest.getInstance("SHA-256")
            try {
                open().use { input -> FileOutputStream(pending).use { output ->
                    val buffer = ByteArray(32 * 1024); var total = 0L
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val count = input.read(buffer); if (count < 0) break
                        require(count > 0) { "The file provider returned no data. Try again." }
                        total += count
                        require(total <= limit) { "Choose visual files up to 20 MiB or audio up to 50 MiB." }
                        check(freeBytes() >= count + 1024L * 1024) { "Free some storage before importing." }
                        output.write(buffer, 0, count)
                        digest.update(buffer, 0, count)
                    }
                    require(total > 0) { "The file is empty." }
                    output.fd.sync()
                } }
                val visual = if (!audio) ImageReferenceRepository(db, directory).inspectOriginal(pending) else null
                require(visual == null || visual.mimeType == mime) { "The file contents do not match its image/PDF type." }
                val duration = if (audio) inspectImportedAudio(pending, extension) else null
                currentCoroutineContext().ensureActive()
                val target = resolve("$id.${if (extension == "jpeg") "jpg" else extension}")
                val asset = MediaAsset(id, title(name), name, mime, category,
                    target.name, pending.length(), System.currentTimeMillis(), visual?.width, visual?.height, visual?.pageCount, duration,
                    digest.digest().joinToString("") { "%02x".format(it) })
                withContext(NonCancellable) {
                    check(!target.exists() && pending.renameTo(target)) { "Could not finish the Library file." }
                    try { db.withTransaction { db.media().insert(asset) } }
                    catch (failure: Exception) {
                        // On an uncertain read/commit outcome retain bytes; recovery retries only after a successful read.
                        runCatching { if (db.media().find(id) == null) target.delete() }
                        throw failure
                    }
                }
                asset
            } finally { pending.delete() }
        }
    }
    suspend fun rename(id: String, value: String) = withContext(Dispatchers.IO) {
        check(db.media().rename(id, title(value)) == 1) { "This Library item is no longer available." }
    }
    suspend fun delete(id: String): Boolean = withContext(Dispatchers.IO + NonCancellable) {
        filesMutex.withLock {
            val asset = checkNotNull(db.media().find(id)) { "This Library item is no longer available." }
            val file = resolve(asset.fileName)
            try { db.withTransaction { db.markers().removeOwned(id); check(db.media().remove(id) == 1) } }
            catch (protected: android.database.sqlite.SQLiteConstraintException) {
                throw IllegalStateException("This Library item is still referenced. Remove its links before deleting it.", protected)
            }
            integrity.remove(asset.fileName)
            !file.exists() || file.delete()
        }
    }
    suspend fun recover(): Int = withContext(Dispatchers.IO) {
        filesMutex.withLock {
            val retained = db.media().readAll().map { it.fileName }.toSet()
            if (!directory.exists()) return@withLock 0
            var removed = 0
            checkNotNull(directory.listFiles()).forEach { file ->
                if (file.isFile && ownedName.matches(file.name) && file.name !in retained) {
                    check(file.delete()) { "Library file cleanup will retry after reopening." }; removed++
                }
            }
            removed
        }
    }
}
