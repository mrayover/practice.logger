package com.mrayover.practicelogger

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.exifinterface.media.ExifInterface
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
import java.util.UUID

@Entity(tableName = "block_images", foreignKeys = [ForeignKey(entity = Block::class,
    parentColumns = ["id"], childColumns = ["blockId"], onDelete = ForeignKey.RESTRICT)])
data class BlockImage(@PrimaryKey val blockId: String, val fileName: String,
    val mimeType: String, val width: Int, val height: Int, val byteCount: Long)

@Dao interface BlockImageDao {
    @Query("SELECT * FROM block_images") fun observeAll(): Flow<List<BlockImage>>
    @Query("SELECT * FROM block_images") suspend fun readAll(): List<BlockImage>
    @Query("SELECT * FROM block_images WHERE blockId = :id") suspend fun find(id: String): BlockImage?
    @Upsert suspend fun save(image: BlockImage)
    @Query("DELETE FROM block_images WHERE blockId = :id") suspend fun remove(id: String): Int
}

val IMAGE_MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `block_images` (`blockId` TEXT NOT NULL, `fileName` TEXT NOT NULL, `mimeType` TEXT NOT NULL, `width` INTEGER NOT NULL, `height` INTEGER NOT NULL, `byteCount` INTEGER NOT NULL, PRIMARY KEY(`blockId`), FOREIGN KEY(`blockId`) REFERENCES `blocks`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT)")
    }
}

// Historical table/type names are retained: MIME distinguishes images from PDFs without a schema change.
data class ReferencePage(val bitmap: Bitmap, val pageCount: Int)

/** One private original per stable Block. Never persist picker URIs or put media into JSON. */
class ImageReferenceRepository(private val db: BlockDatabase, private val directory: File,
    private val freeBytes: () -> Long = { directory.usableSpace }) {
    companion object {
        const val MAX_BYTES = 20 * 1024 * 1024
        const val MAX_PDF_PAGES = 100
        private val filesMutex = Mutex() // Shared by editor, viewer and recovery instances.
        private val ownedName = Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(img|pdf|pending)")
    }
    val images = db.images().observeAll()
    private fun resolve(name: String): File {
        require(ownedName.matches(name)) { "Invalid reference storage name." }
        return File(directory, name).also { check(it.canonicalFile.parentFile == directory.canonicalFile) }
    }
    private fun inspect(file: File): BitmapFactory.Options {
        require(file.isFile && file.length() in 1..MAX_BYTES.toLong()) { "Reference image is missing or too large." }
        return BitmapFactory.Options().apply {
            inJustDecodeBounds = true
            BitmapFactory.decodeFile(file.path, this)
            require(outMimeType in setOf("image/jpeg", "image/png", "image/webp")) { "Choose a valid JPEG, PNG or WebP image." }
            require(outWidth in 1..16384 && outHeight in 1..16384 && outWidth.toLong() * outHeight <= 40_000_000L) {
                "Choose an image up to 40 megapixels and 16384 pixels per side."
            }
        }
    }
    private fun decodeFile(file: File): Bitmap {
        val bounds = inspect(file)
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 2048) sample *= 2
        val decoded = checkNotNull(BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply {
            inSampleSize = sample; inPreferredConfig = Bitmap.Config.ARGB_8888
        })) { "Reference image could not be decoded. Replace it in the Block editor." }
        // BitmapFactory returns stored pixels; camera portrait/flip tags must be applied explicitly.
        // Read the existing private original so already-attached photos also display correctly.
        val orientation = runCatching { ExifInterface(file).getAttributeInt(
            ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        val matrix = Matrix().apply {
            when (orientation) {
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> setScale(-1f, 1f)
                ExifInterface.ORIENTATION_ROTATE_180 -> setRotate(180f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> setScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> { setRotate(90f); postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_ROTATE_90 -> setRotate(90f)
                ExifInterface.ORIENTATION_TRANSVERSE -> { setRotate(-90f); postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_ROTATE_270 -> setRotate(-90f)
            }
        }
        if (matrix.isIdentity) return decoded
        return try { Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true) }
            finally { decoded.recycle() }
    }
    private fun isPdf(file: File): Boolean = file.inputStream().use { input ->
        val header = ByteArray(5)
        input.read(header) == 5 && header.contentEquals("%PDF-".toByteArray(Charsets.US_ASCII))
    }
    private fun <T> withPdf(file: File, action: (PdfRenderer) -> T): T {
        require(file.isFile && file.length() in 1..MAX_BYTES.toLong()) { "PDF is missing or larger than 20 MiB." }
        // Both resources are closed even if construction, page opening or rendering fails.
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
            PdfRenderer(descriptor).use { renderer ->
                require(renderer.pageCount in 1..MAX_PDF_PAGES) { "Choose a PDF with 1–100 pages." }
                action(renderer)
            }
        }
    }
    private fun renderPage(page: PdfRenderer.Page, edge: Int): Bitmap {
        require(page.width in 1..14400 && page.height in 1..14400) { "This PDF page size is unsupported." }
        val ratio = edge.toDouble() / maxOf(page.width, page.height)
        val bitmap = Bitmap.createBitmap((page.width * ratio).toInt().coerceAtLeast(1),
            (page.height * ratio).toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        try {
            bitmap.eraseColor(android.graphics.Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            return bitmap
        } catch (failure: Throwable) { bitmap.recycle(); throw failure }
    }
    private suspend fun inspectPdf(file: File): Pair<Int, Int> {
        val context = currentCoroutineContext()
        return withPdf(file) { renderer ->
        var first = 0 to 0
        // Validate every page using one small temporary bitmap, never an array of page images.
        repeat(renderer.pageCount) { index ->
            context.ensureActive()
            renderer.openPage(index).use { page ->
                if (index == 0) first = page.width to page.height
                renderPage(page, 256).recycle()
            }
        }
        first
        }
    }
    suspend fun loadPage(image: BlockImage, index: Int = 0): ReferencePage = withContext(Dispatchers.IO) {
        filesMutex.withLock {
            if (image.mimeType == "application/pdf") withPdf(resolve(image.fileName)) { renderer ->
                require(index in 0 until renderer.pageCount) { "PDF page is unavailable." }
                renderer.openPage(index).use { ReferencePage(renderPage(it, 2048), renderer.pageCount) }
            } else ReferencePage(decodeFile(resolve(image.fileName)), 1)
        }
    }
    suspend fun decode(image: BlockImage): Bitmap = loadPage(image).bitmap
    suspend fun import(blockId: String, input: () -> InputStream): BlockImage = withContext(Dispatchers.IO) {
        filesMutex.withLock {
            check(directory.isDirectory || directory.mkdirs()) { "Reference storage is unavailable." }
            val id = UUID.randomUUID().toString()
            val pending = resolve("$id.pending")
            try {
                input().use { source -> FileOutputStream(pending).use { output ->
                    val buffer = ByteArray(8192); var total = 0L
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val count = source.read(buffer); if (count < 0) break
                        total += count
                        require(total <= MAX_BYTES) { "Choose an image or PDF no larger than 20 MiB." }
                        check(freeBytes() >= count + 1024L * 1024) { "Free some storage before attaching a reference." }
                        output.write(buffer, 0, count)
                    }
                    output.fd.sync()
                } }
                val pdf = isPdf(pending)
                val dimensions: Pair<Int, Int>
                val mime: String
                if (pdf) {
                    try { dimensions = inspectPdf(pending) }
                    catch (cancelled: CancellationException) { throw cancelled }
                    catch (failure: Exception) { throw IllegalArgumentException("Choose a readable, unencrypted PDF with 1–100 supported pages. Your existing reference was kept.", failure) }
                    mime = "application/pdf"
                } else {
                    val bounds = inspect(pending)
                    decodeFile(pending).recycle()
                    dimensions = bounds.outWidth to bounds.outHeight; mime = bounds.outMimeType!!
                }
                currentCoroutineContext().ensureActive()
                val target = resolve("$id." + if (pdf) "pdf" else "img")
                val image = BlockImage(blockId, target.name, mime, dimensions.first, dimensions.second, pending.length())
                // Once the file is finalized, cancellation cannot split commit and cleanup.
                withContext(NonCancellable) {
                    check(!target.exists() && pending.renameTo(target)) { "Could not finish the reference file." }
                    try { db.withTransaction {
                        checkNotNull(db.blocks().find(blockId)) { "This Block is no longer available." }
                        db.images().save(image)
                    } } catch (failure: Exception) {
                        // Retain on uncertain commit/read failure; recovery only removes known unreferenced files.
                        runCatching { if (db.images().find(blockId)?.fileName != target.name) target.delete() }
                        throw failure
                    }
                    runCatching { cleanUnreferenced() }
                }
                image
            } finally { pending.delete() }
        }
    }
    suspend fun remove(blockId: String): Boolean = withContext(Dispatchers.IO + NonCancellable) {
        filesMutex.withLock {
            db.withTransaction { db.images().remove(blockId) }
            runCatching { cleanUnreferenced(); true }.getOrDefault(false)
        }
    }
    private suspend fun cleanUnreferenced(): Int {
        val retained = db.images().readAll().map { it.fileName }.toSet()
        if (!directory.exists()) return 0
        var removed = 0
        checkNotNull(directory.listFiles()).forEach { file ->
            if (ownedName.matches(file.name) && file.name !in retained) {
                check(file.delete()) { "Reference file cleanup will be retried on next launch." }; removed++
            }
        }
        return removed
    }
    suspend fun recover(): Int = withContext(Dispatchers.IO) { filesMutex.withLock { cleanUnreferenced() } }
}
