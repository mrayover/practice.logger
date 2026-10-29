package com.mrayover.practicelogger

import android.graphics.Bitmap
import android.graphics.BitmapFactory
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

/** One private original per stable Block. Never persist picker URIs or put media into JSON. */
class ImageReferenceRepository(private val db: BlockDatabase, private val directory: File,
    private val freeBytes: () -> Long = { directory.usableSpace }) {
    companion object {
        const val MAX_BYTES = 20 * 1024 * 1024
        private val filesMutex = Mutex() // Shared by editor, viewer and recovery instances.
        private val ownedName = Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(img|pending)")
    }
    val images = db.images().observeAll()
    private fun resolve(name: String): File {
        require(ownedName.matches(name)) { "Invalid image storage name." }
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
        return checkNotNull(BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply {
            inSampleSize = sample; inPreferredConfig = Bitmap.Config.ARGB_8888
        })) { "Reference image could not be decoded. Replace it in the Block editor." }
    }
    suspend fun decode(image: BlockImage): Bitmap = withContext(Dispatchers.IO) {
        filesMutex.withLock { decodeFile(resolve(image.fileName)) }
    }
    suspend fun import(blockId: String, input: () -> InputStream): BlockImage = withContext(Dispatchers.IO) {
        filesMutex.withLock {
            check(directory.isDirectory || directory.mkdirs()) { "Image storage is unavailable." }
            val id = UUID.randomUUID().toString()
            val pending = resolve("$id.pending"); val target = resolve("$id.img")
            try {
                input().use { source -> FileOutputStream(pending).use { output ->
                    val buffer = ByteArray(8192); var total = 0L
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val count = source.read(buffer); if (count < 0) break
                        total += count
                        require(total <= MAX_BYTES) { "Choose an image smaller than 20 MiB." }
                        check(freeBytes() >= count + 1024L * 1024) { "Free some storage before attaching an image." }
                        output.write(buffer, 0, count)
                    }
                    output.fd.sync()
                } }
                val bounds = inspect(pending)
                decodeFile(pending).recycle() // Validate actual content with bounded decoding before replacing metadata.
                val image = BlockImage(blockId, target.name, bounds.outMimeType!!, bounds.outWidth, bounds.outHeight, pending.length())
                // Once the file is finalized, cancellation cannot split commit and cleanup.
                withContext(NonCancellable) {
                    check(!target.exists() && pending.renameTo(target)) { "Could not finish the image file." }
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
                check(file.delete()) { "Image file cleanup will be retried on next launch." }; removed++
            }
        }
        return removed
    }
    suspend fun recover(): Int = withContext(Dispatchers.IO) { filesMutex.withLock { cleanUnreferenced() } }
}
