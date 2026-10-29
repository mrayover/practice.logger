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
class PdfReferenceTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private fun bytes(name: String) = javaClass.classLoader!!.getResourceAsStream("references/$name")!!.use { it.readBytes() }
    private suspend fun rejected(action: suspend () -> Unit) {
        var failed = false
        try { action() } catch (_: Exception) { failed = true }
        assertTrue("Invalid operation accepted", failed)
    }
    @Test fun pdfPersistsRendersOnePageAtATimeAndProtectsArchivedOwnership(): Unit = runBlocking {
        val name = "pdf-reopen.db"; context.deleteDatabase(name)
        var db = BlockDatabase.open(context, name)
        val dir = File(context.cacheDir, "pdf-${UUID.randomUUID()}")
        try {
            val blocks = BlockRepository(db); val id = blocks.save(null, BlockDraft("PDF", "Play"))
            var repo = ImageReferenceRepository(db, dir)
            val original = bytes("score.pdf"); val pdf = repo.import(id) { ByteArrayInputStream(original) }
            assertEquals("application/pdf", pdf.mimeType); assertTrue(pdf.fileName.endsWith(".pdf"))
            assertArrayEquals(original, File(dir, pdf.fileName).readBytes())
            for (page in 0..1) {
                val rendered = repo.loadPage(pdf, page)
                try {
                    assertEquals(2, rendered.pageCount)
                    assertEquals(page == 0, rendered.bitmap.height > rendered.bitmap.width)
                    assertTrue(maxOf(rendered.bitmap.height, rendered.bitmap.width) <= 2048)
                } finally { rendered.bitmap.recycle() }
            }
            rejected { repo.loadPage(pdf, 2) }
            blocks.setArchived(id, true)
            assertEquals(1, blocks.reviewDeletion(id).dependencies.images)
            assertTrue(blocks.deletePermanently(id) is BlockDeletionResult.Blocked)
            rejected { db.blocks().delete(id) }
            val data = BackupRepository(db).snapshot(); val encoded = BackupCodec.encode(data)
            assertFalse(String(encoded).contains(pdf.fileName)); assertFalse(String(encoded).contains("application/pdf"))
            val target = Room.inMemoryDatabaseBuilder(context, BlockDatabase::class.java).build()
            try { BackupRepository(target).restore(BackupCodec.decode(encoded)); assertEquals(data, BackupRepository(target).snapshot()); assertTrue(target.images().readAll().isEmpty()) } finally { target.close() }
            db.close(); db = BlockDatabase.open(context, name); repo = ImageReferenceRepository(db, dir)
            assertEquals(pdf, db.images().find(id)); assertTrue(db.blocks().find(id)!!.archived)
            repo.decode(pdf).recycle(); assertArrayEquals(original, File(dir, pdf.fileName).readBytes())
            repo.remove(id); assertEquals(BlockDeletionResult.Deleted, BlockRepository(db).deletePermanently(id))
        } finally { db.close(); context.deleteDatabase(name); dir.deleteRecursively() }
    }
    @Test fun invalidEncryptedOversizeCancelledAndFailedCommitRetainOriginalAndCleanPending(): Unit = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, BlockDatabase::class.java).build()
        val dir = File(context.cacheDir, "pdf-${UUID.randomUUID()}")
        try {
            val id = BlockRepository(db).save(null, BlockDraft("PDF", "Read")); val repo = ImageReferenceRepository(db, dir)
            val pdf = repo.import(id) { ByteArrayInputStream(bytes("single.pdf")) }
            assertEquals(1, repo.loadPage(pdf).also { it.bitmap.recycle() }.pageCount)
            for (fixture in listOf("corrupt.pdf", "locked.pdf", "too-many.pdf")) rejected { repo.import(id) { ByteArrayInputStream(bytes(fixture)) } }
            rejected { repo.import(id) { object : InputStream() { override fun read(): Int = throw IOException("unreadable") } } }
            val job = launch { repo.import(id) { object : InputStream() {
                override fun read(): Int = 1
                override fun read(b: ByteArray, off: Int, len: Int): Int { Thread.sleep(5); b.fill(1); return len }
            } } }
            delay(30); job.cancelAndJoin()
            rejected { repo.import(id) { object : InputStream() {
                override fun read(): Int = 1
                override fun read(b: ByteArray, off: Int, len: Int): Int { b.fill(1); return len }
            } } }
            rejected { ImageReferenceRepository(db, dir) { 0 }.import(id) { ByteArrayInputStream(bytes("score.pdf")) } }
            db.openHelper.writableDatabase.execSQL("CREATE TRIGGER reject_pdf BEFORE UPDATE ON block_images BEGIN SELECT RAISE(ABORT, 'test'); END")
            rejected { repo.import(id) { ByteArrayInputStream(bytes("score.pdf")) } }
            assertEquals(pdf, db.images().find(id)); assertEquals(listOf(pdf.fileName), dir.list()!!.toList())
            File(dir,pdf.fileName).writeText("corrupt"); rejected { repo.decode(pdf) }
            assertEquals(pdf,db.images().find(id))
        } finally { db.close(); dir.deleteRecursively() }
    }
    @Test fun imagePdfImageReplacementAndConservativeCleanup(): Unit = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, BlockDatabase::class.java).build()
        val dir = File(context.cacheDir, "pdf-${UUID.randomUUID()}")
        try {
            val id = BlockRepository(db).save(null, BlockDraft("PDF", "Read")); val repo = ImageReferenceRepository(db, dir)
            val bitmap = Bitmap.createBitmap(100,200,Bitmap.Config.ARGB_8888)
            val png = ByteArrayOutputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it); it.toByteArray() }; bitmap.recycle()
            val image = repo.import(id) { ByteArrayInputStream(png) }
            val pdf = repo.import(id) { ByteArrayInputStream(bytes("score.pdf")) }
            assertFalse(File(dir,image.fileName).exists()); assertEquals(1,db.images().readAll().size)
            val replacement = repo.import(id) { ByteArrayInputStream(png) }
            assertFalse(File(dir,pdf.fileName).exists()); assertEquals("image/png",replacement.mimeType)
            assertArrayEquals(png,File(dir,replacement.fileName).readBytes())
            File(dir,"${UUID.randomUUID()}.pdf").writeText("orphan")
            File(dir,"${UUID.randomUUID()}.pending").writeText("pending")
            File(dir,"other.pdf").writeText("keep"); assertEquals(2,repo.recover())
            assertTrue(File(dir,"other.pdf").exists()); repo.remove(id); assertEquals(listOf("other.pdf"),dir.list()!!.toList())
        } finally { db.close(); dir.deleteRecursively() }
    }
}
