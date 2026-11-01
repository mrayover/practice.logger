package com.mrayover.practicelogger

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID

@RunWith(RobolectricTestRunner::class) @Config(sdk=[35])
class AudioImportTest {
    private val context get() = ApplicationProvider.getApplicationContext<android.content.Context>()
    private fun wave(seconds: Int = 1, bits: Short = 16): ByteArray {
        val size = seconds*8000*2
        return ByteBuffer.allocate(44+size).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray()); putInt(36+size); put("WAVEfmt ".toByteArray()); putInt(16)
            putShort(1);putShort(1);putInt(8000);putInt(16000);putShort(2);putShort(bits);put("data".toByteArray());putInt(size)
        }.array()
    }
    @Test fun importedIdentityPersistsReopensRenamesExportsDeletesWithoutChangingEarlierTakes(): Unit = runBlocking {
        val name="import-${UUID.randomUUID()}.db"; val dir=File(context.cacheDir,UUID.randomUUID().toString())
        var db=BlockDatabase.open(context,name)
        try {
            val block=BlockRepository(db).save(null,BlockDraft("Import","Play"))
            var repo=TakeRepository(db,dir) { 1000 }
            val old=repo.prepare(block).also { it.file.writeText("old audio") }.let { repo.save(it) }
            val bytes=wave(131) // Beyond PhraseWave's 128-second maximum, at an external sample rate.
            val draft=AudioImport(repo).prepare(block,"A long backing filename.wav") { bytes.inputStream() }
            assertEquals(1,db.takes().readAll().size)
            val imported=repo.save(draft)
            assertTrue(imported.isLoop);assertEquals(TakeKind.IMPORTED_LOOP,imported.kind)
            assertNull(imported.beatCount);assertNull(imported.recordedBpm);assertEquals(131000L,imported.durationMs)
            assertEquals("audio/wav",imported.audioMime);assertFalse(imported.fileName.contains("backing"))
            db.close();db=BlockDatabase.open(context,name);repo=TakeRepository(db,dir) { 1000 };repo.recover()
            assertEquals(imported,db.takes().find(imported.id))
            repo.rename(imported.id,"Renamed backing");val renamed=db.takes().find(imported.id)!!
            assertEquals(TakeKind.IMPORTED_LOOP,renamed.kind)
            val exported=ByteArrayOutputStream();repo.export(renamed,exported);assertArrayEquals(bytes,exported.toByteArray())
            assertTrue(repo.delete(imported.id));assertEquals(listOf(old),db.takes().readAll());assertTrue(repo.audioFile(old).exists())
        } finally { db.close();context.deleteDatabase(name);dir.deleteRecursively() }
    }
    @Test fun badAudioCopyFailureAndCancellationLeaveNoStagedFilesAndPreserveSavedAudio(): Unit = runBlocking {
        val name="import-${UUID.randomUUID()}.db";val dir=File(context.cacheDir,UUID.randomUUID().toString());val db=BlockDatabase.open(context,name)
        try {
            val block=BlockRepository(db).save(null,BlockDraft("Import","Play"));val repo=TakeRepository(db,dir) { 1000 }
            val old=repo.prepare(block).also { it.file.writeText("kept") }.let { repo.save(it) }
            val source=AudioImport(repo)
            for (bytes in listOf(byteArrayOf(),"not a wave".toByteArray(),wave(bits=24),wave(901),wave().copyOf(100))) {
                try { source.prepare(block,"bad.wav") { bytes.inputStream() };fail("Accepted invalid/oversized duration") } catch (_: IllegalArgumentException) { }
                assertEquals(listOf(old.fileName),dir.list()!!.toList())
            }
            try { source.prepare(block,"failed.wav") { throw IOException("provider") };fail() } catch (_: IOException) { }
            try { source.prepare(block,"cancelled.wav") { object: InputStream() { override fun read(): Int = throw CancellationException("cancel") } };fail() } catch (_: CancellationException) { }
            assertEquals(listOf(old.fileName),dir.list()!!.toList());assertEquals(listOf(old),db.takes().readAll())
        } finally { db.close();context.deleteDatabase(name);dir.deleteRecursively() }
    }
    @Test fun failedCommitIsRetryableAndRecoveryProtectsActiveImport(): Unit = runBlocking {
        val name="import-${UUID.randomUUID()}.db";val dir=File(context.cacheDir,UUID.randomUUID().toString());val db=BlockDatabase.open(context,name)
        try {
            val block=BlockRepository(db).save(null,BlockDraft("Import","Play"));val repo=TakeRepository(db,dir)
            val draft=AudioImport(repo).prepare(block,"backing.wav") { wave().inputStream() }
            assertEquals(0,repo.recover());assertTrue(draft.file.exists())
            db.openHelper.writableDatabase.execSQL("CREATE TRIGGER reject_import BEFORE INSERT ON takes BEGIN SELECT RAISE(ABORT,'test'); END")
            try { repo.save(draft);fail() } catch (_: Exception) { }
            assertTrue(draft.file.exists());assertTrue(db.takes().readAll().isEmpty())
            db.openHelper.writableDatabase.execSQL("DROP TRIGGER reject_import")
            val saved=repo.save(draft);assertTrue(repo.audioFile(saved).exists());assertEquals(0,repo.recover())
        } finally { db.close();context.deleteDatabase(name);dir.deleteRecursively() }
    }
    @Test fun byteLimitAndFormatsAreExplicit(): Unit = runBlocking {
        assertEquals("mp3",AudioImport.extension("BACKING.MP3"))
        for (name in listOf("","bad.ogg","bad.mp4","bad.wav\n","x".repeat(256)+".wav")) {
            try { AudioImport.extension(name);fail(name) } catch (_: IllegalArgumentException) { }
        }
        val source=object: InputStream() {
            var remaining=AudioImport.MAX_BYTES+1
            override fun read(): Int = if (remaining-- > 0) 0 else -1
            override fun read(b: ByteArray, off: Int, len: Int): Int { if (remaining<=0) return -1;val count=minOf(len.toLong(),remaining).toInt();remaining-=count;return count }
        }
        try { AudioImport.copy(source,object: OutputStream(){ override fun write(b:Int){};override fun write(b:ByteArray,off:Int,len:Int){} });fail() } catch (_: IllegalArgumentException) { }
    }
}
