package com.mrayover.practicelogger

import android.content.Context
import android.graphics.Bitmap
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.*
import java.util.UUID

@RunWith(RobolectricTestRunner::class) @Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ImageReferenceTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private fun memory() = Room.inMemoryDatabaseBuilder(context, BlockDatabase::class.java).build()
    private fun dir() = File(context.cacheDir, "images-${UUID.randomUUID()}").apply { mkdirs() }
    private fun bytes(format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG): ByteArray {
        val bitmap = Bitmap.createBitmap(600, 400, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.GREEN)
        return ByteArrayOutputStream().use { output -> bitmap.compress(format, 90, output); bitmap.recycle(); output.toByteArray() }
    }
    private suspend fun attach(repo: ImageReferenceRepository, id: String, data: ByteArray = bytes()) = repo.import(id) { ByteArrayInputStream(data) }
    private suspend fun rejected(action: suspend () -> Unit) {
        try { action(); fail("Invalid import accepted") } catch (_: Exception) { }
    }
    @Test fun originalPersistedAndReplacementRemovalAreSafe(): Unit = runBlocking {
        val name = "image-reopen.db"; context.deleteDatabase(name)
        var db = BlockDatabase.open(context, name); val directory = dir()
        try {
            val id = BlockRepository(db).save(null, BlockDraft("Image", "Play"))
            val jpeg = bytes(); var repo = ImageReferenceRepository(db, directory)
            val first = attach(repo, id, jpeg)
            assertArrayEquals(jpeg, File(directory, first.fileName).readBytes())
            db.close(); db = BlockDatabase.open(context, name); repo = ImageReferenceRepository(db, directory)
            assertEquals(first, db.images().find(id)); repo.decode(first).recycle()
            BlockRepository(db).setArchived(id, true)
            assertEquals(first, db.images().find(id))
            val second = attach(repo, id, bytes(Bitmap.CompressFormat.PNG))
            assertEquals("image/png", second.mimeType); assertFalse(File(directory, first.fileName).exists())
            assertEquals(listOf(second.fileName), directory.list()!!.toList())
            assertTrue(repo.remove(id)); assertNull(db.images().find(id)); assertTrue(directory.list()!!.isEmpty())
        } finally { db.close(); context.deleteDatabase(name); directory.deleteRecursively() }
    }
    @Test fun malformedOversizeLowStorageInterruptedAndFailedCommitPreserveOld(): Unit = runBlocking {
        val db = memory(); val directory = dir()
        try {
            val id = BlockRepository(db).save(null, BlockDraft("Image", "Play"))
            val repo = ImageReferenceRepository(db, directory); val first = attach(repo, id)
            rejected { attach(repo, id, "not an image.jpg".toByteArray()) }
            rejected { repo.import(id) { object : InputStream() {
                override fun read() = 1
                override fun read(b: ByteArray, off: Int, len: Int): Int { b.fill(1); return len }
            } } }
            rejected { attach(ImageReferenceRepository(db, directory) { 0 }, id) }
            rejected { repo.import(id) { object : InputStream() { override fun read(): Int = throw IOException("read failed") } } }
            db.openHelper.writableDatabase.execSQL("CREATE TRIGGER reject_image BEFORE UPDATE ON block_images BEGIN SELECT RAISE(ABORT, 'test'); END")
            rejected { attach(repo, id) }
            assertEquals(first, db.images().find(id)); assertEquals(listOf(first.fileName), directory.list()!!.toList())
            repo.decode(first).recycle()
        } finally { db.close(); directory.deleteRecursively() }
    }
    @Test fun supportedFormatsMissingCorruptAndUnsafePaths(): Unit = runBlocking {
        val db = memory(); val directory = dir()
        try {
            val id = BlockRepository(db).save(null, BlockDraft("Image", "Play")); val repo = ImageReferenceRepository(db, directory)
            for (format in listOf(Bitmap.CompressFormat.JPEG, Bitmap.CompressFormat.PNG, Bitmap.CompressFormat.WEBP)) {
                val image = attach(repo, id, bytes(format)); assertEquals(600, repo.decode(image).also { it.recycle() }.width)
            }
            val image = db.images().find(id)!!
            rejected { repo.decode(image.copy(fileName = "../outside.jpg")) }
            File(directory, image.fileName).writeText("corrupt"); rejected { repo.decode(image) }
            File(directory, image.fileName).delete(); rejected { repo.decode(image) }
            assertEquals(image, db.images().find(id)) // Missing media still protects deletion until deliberate removal.
        } finally { db.close(); directory.deleteRecursively() }
    }
    @Test fun dependencyRecheckAndForeignKeyPreventSilentDeletion(): Unit = runBlocking {
        val db = memory(); val directory = dir()
        try {
            val blocks = BlockRepository(db); val id = blocks.save(null, BlockDraft("Image", "Play"))
            assertTrue(blocks.reviewDeletion(id).dependencies.canDelete)
            val repo = ImageReferenceRepository(db, directory); attach(repo, id)
            val review = blocks.reviewDeletion(id)
            assertEquals(1, review.dependencies.images); assertTrue(review.dependencies.explanation().contains("Remove the image first"))
            assertEquals(BlockDeletionResult.Blocked(review), blocks.deletePermanently(id))
            rejected { db.blocks().delete(id) }
            repo.remove(id); assertEquals(BlockDeletionResult.Deleted, blocks.deletePermanently(id))
        } finally { db.close(); directory.deleteRecursively() }
    }
    @Test fun backupExcludesImagesAndRestoreCreatesNoMissingMedia(): Unit = runBlocking {
        val db = memory(); val target = memory(); val directory = dir()
        try {
            val id = BlockRepository(db).save(null, BlockDraft("Image", "Play")); val repo = ImageReferenceRepository(db, directory)
            val image = attach(repo, id); db.logs().save(DailyLog("2026-10-29", "Keep Log"))
            val data = BackupRepository(db).snapshot(); val encoded = BackupCodec.encode(data)
            assertFalse(String(encoded).contains(image.fileName)); assertFalse(String(encoded).contains("block_images"))
            BackupRepository(target).restore(BackupCodec.decode(encoded))
            assertEquals(data, BackupRepository(target).snapshot()); assertTrue(target.images().readAll().isEmpty())
            rejected { BackupRepository(db).restore(data) }
            assertEquals(image, db.images().find(id))
        } finally { db.close(); target.close(); directory.deleteRecursively() }
    }
    @Test fun recoveryDeletesOnlyGeneratedUnreferencedFiles(): Unit = runBlocking {
        val db = memory(); val directory = dir()
        try {
            val id = BlockRepository(db).save(null, BlockDraft("Image", "Play")); val repo = ImageReferenceRepository(db, directory)
            val image = attach(repo, id)
            File(directory, "${UUID.randomUUID()}.pending").writeText("interrupted")
            File(directory, "${UUID.randomUUID()}.img").writeText("orphan")
            File(directory, "unrelated.txt").writeText("keep")
            assertEquals(2, repo.recover()); assertTrue(File(directory, image.fileName).exists()); assertTrue(File(directory, "unrelated.txt").exists())
        } finally { db.close(); directory.deleteRecursively() }
    }
}
