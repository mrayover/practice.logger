package com.mrayover.practicelogger

import android.graphics.Bitmap
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import java.io.*
import java.util.UUID

class MediaLibraryStorageTest {
    private suspend fun rejected(action: suspend () -> Unit) {
        var failed = false
        try { action() } catch (_: Exception) { failed = true }
        assertTrue("Invalid operation accepted", failed)
    }
    @Test fun platformVisualAudioValidationFailureCancellationAndProtectedDeletion(): Unit = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, BlockDatabase::class.java).build()
        val dir = File(context.cacheDir, "l1-${UUID.randomUUID()}")
        try {
            val repo = MediaLibraryRepository(db, dir)
            for ((extension, format) in listOf("jpg" to Bitmap.CompressFormat.JPEG, "png" to Bitmap.CompressFormat.PNG, "webp" to Bitmap.CompressFormat.WEBP)) {
                val bytes = ByteArrayOutputStream().use { output ->
                    val bitmap = Bitmap.createBitmap(48, 32, Bitmap.Config.ARGB_8888)
                    bitmap.compress(format, 95, output); bitmap.recycle(); output.toByteArray()
                }
                val asset = repo.import("Platform.$extension", null) { bytes.inputStream() }
                assertEquals(48, asset.width); assertArrayEquals(bytes, File(dir, asset.fileName).readBytes())
            }
            val pdf = repo.import("Score.pdf", "application/pdf") { l1Fixture("references/score.pdf").inputStream() }
            assertEquals(2, pdf.pageCount)
            for (extension in listOf("mp3", "m4a")) {
                val bytes = l1Fixture("audio/backing.$extension")
                val asset = repo.import("Backing.$extension", null) { bytes.inputStream() }
                assertTrue(asset.durationMs!! > 0); assertArrayEquals(bytes, File(dir, asset.fileName).readBytes())
                rejected { repo.import("Wrong.$extension", null) { l1Fixture("audio/backing.${if (extension == "mp3") "m4a" else "mp3"}").inputStream() } }
                rejected { repo.import("Broken.$extension", null) { byteArrayOf(1, 2, 3).inputStream() } }
            }
            for (fixture in listOf("corrupt.pdf", "locked.pdf", "owner-locked.pdf", "too-many.pdf"))
                rejected { repo.import(fixture, "application/pdf") { l1Fixture("references/$fixture").inputStream() } }
            rejected { MediaLibraryRepository(db, dir) { 0 }.import("No-space.m4a", null) { l1Fixture("audio/backing.m4a").inputStream() } }
            val expected = db.media().readAll()
            val job = launch { repo.import("cancel.png", null) { object : InputStream() {
                override fun read() = 1
                override fun read(b: ByteArray, off: Int, len: Int): Int { Thread.sleep(5); b.fill(1); return len }
            } } }
            delay(30); job.cancelAndJoin()
            assertEquals(expected, db.media().readAll()); assertEquals(expected.size, dir.list()!!.size)
            // Future durable links are protected by RESTRICT, without prematurely adding L3 tables.
            db.openHelper.writableDatabase.execSQL("CREATE TABLE test_media_links (id TEXT PRIMARY KEY NOT NULL, mediaId TEXT NOT NULL REFERENCES media_assets(id) ON DELETE RESTRICT)")
            db.openHelper.writableDatabase.execSQL("INSERT INTO test_media_links VALUES ('one','${pdf.id}')")
            rejected { repo.delete(pdf.id) }; assertNotNull(db.media().find(pdf.id)); assertTrue(repo.available(pdf))
            db.openHelper.writableDatabase.execSQL("DELETE FROM test_media_links")
            assertTrue(repo.delete(pdf.id)); assertFalse(File(dir, pdf.fileName).exists())
            assertEquals(expected.size-1, dir.list()!!.size)
        } finally { db.close(); dir.deleteRecursively() }
    }
}
