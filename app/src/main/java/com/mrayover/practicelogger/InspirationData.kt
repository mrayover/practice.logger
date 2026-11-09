package com.mrayover.practicelogger

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

internal const val INSPIRATION_STORE = "inspirations"
internal const val INSPIRATION_METADATA_LIMIT = 16_384
internal const val INSPIRATION_LIMIT = 20L * 1024 * 1024
internal val inspirationFormats = mapOf(
    "jpg" to "image/jpeg", "jpeg" to "image/jpeg", "png" to "image/png",
    "webp" to "image/webp", "pdf" to "application/pdf", "txt" to "text/plain", "md" to "text/plain"
)

@Entity(
    tableName = "inspirations",
    foreignKeys = [ForeignKey(entity = MediaAsset::class, parentColumns = ["id"],
        childColumns = ["mediaId"], onDelete = ForeignKey.RESTRICT)],
    indices = [Index("mediaId"), Index("fileName", unique = true)]
)
data class Inspiration(
    @PrimaryKey val id: String,
    val title: String,
    val author: String = "",
    val source: String = "",
    val category: String = "",
    val note: String = "",
    val fileName: String? = null,
    val mimeType: String? = null,
    val byteCount: Long? = null,
    val sha256: String? = null,
    val mediaId: String? = null,
    val pageCount: Int? = null
)

@Dao
interface InspirationDao {
    @Query("SELECT * FROM inspirations ORDER BY title COLLATE NOCASE, id")
    fun observe(): Flow<List<Inspiration>>

    @Query("SELECT * FROM inspirations WHERE id = :id")
    suspend fun find(id: String): Inspiration?

    @Query("SELECT fileName FROM inspirations WHERE fileName IS NOT NULL")
    suspend fun fileNames(): List<String>

    @Query("SELECT EXISTS(SELECT 1 FROM inspirations)")
    suspend fun hasEntries(): Boolean

    @Insert suspend fun insert(row: Inspiration)
    @Update suspend fun update(row: Inspiration): Int
    @Delete suspend fun delete(row: Inspiration): Int
}

internal val INSPIRATION_MIGRATION_22_23 = object : Migration(22, 23) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""CREATE TABLE IF NOT EXISTS inspirations (
            id TEXT NOT NULL PRIMARY KEY, title TEXT NOT NULL, author TEXT NOT NULL,
            source TEXT NOT NULL, category TEXT NOT NULL, note TEXT NOT NULL, fileName TEXT,
            mimeType TEXT, byteCount INTEGER, sha256 TEXT, mediaId TEXT, pageCount INTEGER,
            FOREIGN KEY(mediaId) REFERENCES media_assets(id) ON UPDATE NO ACTION ON DELETE RESTRICT)""")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_inspirations_mediaId ON inspirations(mediaId)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_inspirations_fileName ON inspirations(fileName)")
    }
}

internal data class InspirationInput(val name: String, val open: () -> InputStream)

internal fun readInspirationText(input: InputStream): String =
    Charsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(input.readBytes())).toString()

internal class InspirationRepository(private val db: BlockDatabase, private val root: File) {
    private val directory = File(root, INSPIRATION_STORE)
    private val library = MediaLibraryRepository(db, File(root, "media-library"))
    private val dao = db.inspirations()
    val entries = dao.observe()

    suspend fun text(row: Inspiration): String = withContext(Dispatchers.IO) {
        if (row.mimeType != "text/plain") "" else MediaLibraryRepository.filesMutex.withLock {
            original(row).inputStream().use(::readInspirationText)
        }
    }

    suspend fun visualPage(row: Inspiration, page: Int): ReferencePage {
        row.mediaId?.let { return library.visualPage(it, page) }
        return withContext(Dispatchers.IO) {
            MediaLibraryRepository.filesMutex.withLock {
                ImageReferenceRepository(db, directory).loadOriginalPage(original(row), checkNotNull(row.mimeType), page)
            }
        }
    }

    private suspend fun original(row: Inspiration): File {
        val file = portableMediaFile(root, INSPIRATION_STORE, checkNotNull(row.fileName))
        check(file.isFile && file.length() == row.byteCount && file.length() in 1..INSPIRATION_LIMIT) {
            "This original is missing or changed."
        }
        check(file.inputStream().use { copyPortable(it, null, INSPIRATION_LIMIT) }.sha256 == row.sha256) {
            "This original is missing or changed."
        }
        return file
    }

    suspend fun save(
        previous: Inspiration?, details: Inspiration, replacement: InspirationInput? = null,
        removeOriginal: Boolean = false
    ): Inspiration = withContext(Dispatchers.IO) {
        val title = MediaLibraryRepository.title(details.title)
        require(listOf(details.author, details.source, details.category, details.note).all { it.length <= INSPIRATION_METADATA_LIMIT }) {
            "Keep each metadata field at 16,384 characters or fewer."
        }
        MediaLibraryRepository.filesMutex.withLock {
            val metadata = (previous ?: Inspiration(UUID.randomUUID().toString(), title)).copy(
                title = title, author = details.author.trim(), source = details.source.trim(),
                category = details.category.trim(), note = details.note.trim()
            )
            var saved = metadata
            var pending: File? = null
            var promoted: File? = null
            try {
                if (removeOriginal || replacement != null) {
                    saved = saved.copy(fileName = null, mimeType = null, byteCount = null, sha256 = null,
                        mediaId = null, pageCount = null)
                }
                if (replacement != null) {
                    val extension = replacement.name.substringAfterLast('.', "").lowercase(java.util.Locale.ROOT)
                    val mime = requireNotNull(inspirationFormats[extension]) { "Choose a PDF, JPEG, PNG, WebP, UTF-8 TXT or MD file." }
                    check(directory.isDirectory || directory.mkdirs()) { "Inspiration storage is unavailable." }
                    val staged = File.createTempFile("import-", ".pending", directory)
                    pending = staged
                    val copy = FileOutputStream(staged).use { output ->
                        replacement.open().use { copyPortable(it, output, INSPIRATION_LIMIT, directory) }
                            .also { output.fd.sync() }
                    }
                    require(copy.bytes > 0) { "The file is empty." }
                    val visual = if (mime == "text/plain") {
                        staged.inputStream().use(::readInspirationText)
                        null
                    } else ImageReferenceRepository(db, directory).inspectOriginal(staged).also {
                        require(it.mimeType == mime) { "The file contents do not match its filename." }
                    }
                    val existing = if (visual == null) null else db.media().findMatching(mime, copy.bytes, copy.sha256)
                        .firstOrNull(library::available)
                    saved = if (existing != null) saved.copy(mediaId = existing.id, mimeType = mime,
                        pageCount = existing.pageCount)
                    else {
                        val filename = UUID.randomUUID().toString() + "." + if (extension == "jpeg") "jpg" else extension
                        promoted = portableMediaFile(root, INSPIRATION_STORE, filename)
                        saved.copy(fileName = filename, mimeType = mime, byteCount = copy.bytes, sha256 = copy.sha256,
                            pageCount = visual?.pageCount)
                    }
                }
                currentCoroutineContext().ensureActive()
                withContext(NonCancellable) {
                    promoted?.let { target ->
                        check(!target.exists() && checkNotNull(pending).renameTo(target)) { "Could not finish saving the original." }
                        syncPrivateDirectory(directory)
                        syncPrivateDirectory(root)
                    }
                    db.withTransaction {
                        if (previous == null) dao.insert(saved)
                        else {
                            check(dao.find(previous.id) == previous) { "This inspiration changed. Reopen it before editing." }
                            check(dao.update(saved) == 1)
                        }
                    }
                    if (previous?.fileName != saved.fileName) deleteOwned(previous?.fileName)
                }
                saved
            } catch (failure: Throwable) {
                // An uncertain commit keeps its original until a successful ownership read.
                withContext(NonCancellable) {
                    val target = promoted
                    if (target != null) runCatching { if (dao.find(saved.id)?.fileName != target.name) target.delete() }
                }
                throw failure
            } finally {
                pending?.delete()
            }
        }
    }

    suspend fun delete(row: Inspiration) = withContext(Dispatchers.IO + NonCancellable) {
        MediaLibraryRepository.filesMutex.withLock {
            db.withTransaction {
                check(dao.find(row.id) == row) { "This inspiration changed. Reopen it before deleting." }
                check(dao.delete(row) == 1)
            }
            deleteOwned(row.fileName)
        }
    }

    private fun deleteOwned(name: String?) {
        if (name != null) runCatching { portableMediaFile(root, INSPIRATION_STORE, name).delete() }
    }

    suspend fun recover() = withContext(Dispatchers.IO) {
        MediaLibraryRepository.filesMutex.withLock {
            val retained = dao.fileNames().toHashSet()
            directory.listFiles()?.filter { it.isFile && it.name !in retained }?.forEach { file ->
                if (file.name.startsWith("import-") && file.extension == "pending") file.delete()
                else deleteOwned(file.name)
            }
        }
    }
}
