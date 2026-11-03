package com.mrayover.practicelogger

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
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID

@RunWith(RobolectricTestRunner::class) @Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MediaLibraryTest {
    private val context get() = ApplicationProvider.getApplicationContext<android.content.Context>()
    private fun memory() = Room.inMemoryDatabaseBuilder(context, BlockDatabase::class.java).build()
    private fun directory() = File(context.cacheDir, UUID.randomUUID().toString())
    private fun image(format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG): ByteArray = ByteArrayOutputStream().use { output ->
        Bitmap.createBitmap(32, 24, Bitmap.Config.ARGB_8888).let { bitmap -> bitmap.compress(format, 95, output); bitmap.recycle() }
        output.toByteArray()
    }
    private fun wave(seconds: Int = 1): ByteArray = ByteBuffer.allocate(44 + seconds * 16000).order(ByteOrder.LITTLE_ENDIAN).apply {
        put("RIFF".toByteArray()); putInt(capacity()-8); put("WAVEfmt ".toByteArray()); putInt(16)
        putShort(1); putShort(1); putInt(8000); putInt(16000); putShort(2); putShort(16)
        put("data".toByteArray()); putInt(capacity()-44)
    }.array()
    private suspend fun rejected(action: suspend () -> Unit) {
        try { action(); fail("Accepted invalid operation") } catch (_: Exception) { }
    }
    @Test fun formatsIdentitiesRenameDeleteAndReopenKeepOneOriginal(): Unit = runBlocking {
        val name = "library-${UUID.randomUUID()}.db"; var db = BlockDatabase.open(context, name); val dir = directory()
        try {
            var repo = MediaLibraryRepository(db, dir)
            for ((extension, format) in listOf("jpg" to Bitmap.CompressFormat.JPEG, "png" to Bitmap.CompressFormat.PNG, "webp" to Bitmap.CompressFormat.WEBP)) {
                val bytes = image(format); val asset = repo.import("Study.$extension", null) { bytes.inputStream() }
                assertArrayEquals(bytes, File(dir, asset.fileName).readBytes()); assertEquals(32, asset.width); assertEquals("IMAGE", asset.category)
            }
            val bytes = wave(); val audio = repo.import("Backing.wav", "audio/wav") { bytes.inputStream() }
            assertEquals(1000L, audio.durationMs)
            val imported = db.media().readAll(); assertEquals(4, imported.map { it.id }.toSet().size); assertEquals(4, dir.list()!!.size)
            repo.rename(audio.id, "New name"); assertArrayEquals(bytes, File(dir, audio.fileName).readBytes())
            assertEquals(audio.copy(title = "New name"), db.media().find(audio.id))
            db.close(); db = BlockDatabase.open(context, name); repo = MediaLibraryRepository(db, dir)
            assertEquals(0, repo.recover()); assertEquals(4, db.media().readAll().size)
            assertTrue(repo.delete(audio.id)); assertFalse(File(dir, audio.fileName).exists()); assertEquals(3, db.media().readAll().size)
            assertEquals(3, dir.list()!!.size)
        } finally { db.close(); context.deleteDatabase(name); dir.deleteRecursively() }
    }
    @Test fun invalidContentMimeSpaceProviderSizeAndCancellationLeaveNoPartialRows(): Unit = runBlocking {
        val db = memory(); val dir = directory(); val repo = MediaLibraryRepository(db, dir)
        try {
            val kept = repo.import("kept.png", "image/png") { image().inputStream() }
            rejected { repo.import("wrong.jpg", "image/jpeg") { image().inputStream() } }
            rejected { repo.import("wrong.png", "application/pdf") { image().inputStream() } }
            rejected { repo.import("empty.wav", null) { byteArrayOf().inputStream() } }
            rejected { repo.import("bad.wav", null) { wave().copyOf(100).inputStream() } }
            rejected { repo.import("long.wav", null) { wave(901).inputStream() } }
            rejected { repo.import("bad.pdf", null) { "%PDF-1.4\ntruncated".byteInputStream() } }
            rejected { repo.import("unsupported.gif", null) { image().inputStream() } }
            rejected { MediaLibraryRepository(db, dir) { 0 }.import("no-space.png", null) { image().inputStream() } }
            rejected { repo.import("provider.png", null) { throw IOException("provider") } }
            rejected { repo.import("cancel.png", null) { throw CancellationException("cancel") } }
            rejected { repo.import("large.png", null) { object : InputStream() {
                var left = ImageReferenceRepository.MAX_BYTES + 1
                override fun read() = if (left-- > 0) 1 else -1
                override fun read(b: ByteArray, off: Int, len: Int): Int {
                    if (left <= 0) return -1
                    val n = minOf(len, left); left -= n; return n
                }
            } } }
            assertEquals(listOf(kept), db.media().readAll()); assertEquals(listOf(kept.fileName), dir.list()!!.toList())
        } finally { db.close(); dir.deleteRecursively() }
    }
    @Test fun failedCommitAndInterruptedFilesRecoverWithoutDeletingOwnedOrUnknown(): Unit = runBlocking {
        val db = memory(); val dir = directory(); val repo = MediaLibraryRepository(db, dir)
        try {
            val kept = repo.import("kept.png", null) { image().inputStream() }
            db.openHelper.writableDatabase.execSQL("CREATE TRIGGER reject_media BEFORE INSERT ON media_assets BEGIN SELECT RAISE(ABORT,'test'); END")
            rejected { repo.import("failed.png", null) { image().inputStream() } }
            assertEquals(listOf(kept.fileName), dir.list()!!.toList())
            File(dir, "${UUID.randomUUID()}.pending").writeText("interrupted copy")
            File(dir, "${UUID.randomUUID()}.mp3").writeText("finalized before commit")
            File(dir, "unrelated.txt").writeText("keep")
            assertEquals(2, repo.recover()); assertTrue(repo.available(kept)); assertTrue(File(dir, "unrelated.txt").exists())
            File(dir, kept.fileName).writeText("corrupt")
            assertFalse(repo.available(kept)); assertEquals(0, repo.recover()); assertTrue(File(dir, kept.fileName).exists())
            File(dir, kept.fileName).delete(); assertFalse(repo.available(kept)); assertNotNull(db.media().find(kept.id))
            assertTrue(repo.delete(kept.id))
        } finally { db.close(); dir.deleteRecursively() }
    }
    @Test fun recoveryWaitsForActiveImportAcrossInstances(): Unit = runBlocking {
        val db = memory(); val dir = directory(); val entered = CompletableDeferred<Unit>(); val release = java.util.concurrent.CountDownLatch(1)
        try {
            val bytes = image()
            val import = async(Dispatchers.IO) { MediaLibraryRepository(db, dir).import("active.png", null) {
                entered.complete(Unit); check(release.await(10, java.util.concurrent.TimeUnit.SECONDS)); bytes.inputStream()
            } }
            entered.await()
            val recovery = async(Dispatchers.IO) { MediaLibraryRepository(db, dir).recover() }
            delay(100); assertFalse(recovery.isCompleted); release.countDown()
            val saved = import.await(); assertEquals(0, recovery.await()); assertTrue(File(dir, saved.fileName).exists())
        } finally { release.countDown(); db.close(); dir.deleteRecursively() }
    }
    @Test fun libraryOnlyRestoreRefusesEveryJsonReaderAndExportStillExcludesMedia(): Unit = runBlocking {
        val db = memory(); val dir = directory()
        try {
            val repo = MediaLibraryRepository(db, dir); val asset = repo.import("kept.png", null) { image().inputStream() }
            val backup = BackupRepository(db); val encoded = BackupCodec.encode(backup.snapshot())
            assertFalse(encoded.toString(Charsets.UTF_8).contains(asset.id)); assertFalse(encoded.toString(Charsets.UTF_8).contains("media_assets"))
            for (version in 1..7) {
                val json = org.json.JSONObject(encoded.toString(Charsets.UTF_8)).put("version", version)
                try { backup.restore(BackupCodec.decode(json.toString().toByteArray())); fail("Merged Library-only install") }
                catch (_: NonEmptyRestoreException) { }
            }
            assertEquals(listOf(asset), db.media().readAll()); assertTrue(repo.available(asset))
        } finally { db.close(); dir.deleteRecursively() }
    }
    @Test fun searchSortAndFiltersHaveStableIdentityOrdering() {
        fun asset(id: String, title: String, category: String, timestamp: Long) = MediaAsset(id, title, "Original-$id", "test", category, "$id.png", 1, timestamp)
        val rows = listOf(asset("b", "ALPHA", "PDF", 2), asset("a", "alpha", "IMAGE", 2), asset("c", "Beta", "AUDIO", 1))
        assertEquals(listOf("a", "b", "c"), libraryItems(rows, "", MediaFilter.ALL, true).map { it.id })
        assertEquals(listOf("a", "b", "c"), libraryItems(rows, "", MediaFilter.ALL, false).map { it.id })
        assertEquals(listOf(rows[2]), libraryItems(rows, "ORIGINAL-C", MediaFilter.AUDIO, false))
        assertEquals(listOf(rows[0]), libraryItems(rows, "alpha", MediaFilter.PDFS, true))
        assertTrue(libraryItems(rows, "nomatch", MediaFilter.ALL, false).isEmpty())
    }
}
