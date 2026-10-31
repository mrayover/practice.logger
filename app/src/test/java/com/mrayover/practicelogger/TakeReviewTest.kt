package com.mrayover.practicelogger

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TakeReviewTest {
    private fun checkStorage(test: suspend (BlockDatabase, TakeRepository, File) -> Unit) = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val db = Room.inMemoryDatabaseBuilder(context, BlockDatabase::class.java).build()
        val directory = File(context.cacheDir, UUID.randomUUID().toString())
        val repo = TakeRepository(db, directory) { if (it.length() > 0) 1500 else 0 }
        try { repo.recover(); test(db, repo, directory) } finally { db.close(); directory.deleteRecursively() }
    }
    @Test fun inspectionRecoveryAndDiscardNeverPublishOrChangeSavedAudio() = checkStorage { db, repo, directory ->
        val saved = repo.prepare(null).also { it.file.writeText("saved audio") }.let { repo.save(it) }
        val draft = repo.prepare(null).also { it.file.writeText("review audio") }
        val metadata = repo.inspect(draft)
        assertEquals(draft.id, metadata.id); assertEquals("FREE", metadata.mode)
        assertEquals(listOf(saved), db.takes().readAll()); assertEquals(0, repo.recover())
        assertTrue(draft.file.exists()); assertTrue(repo.audioFile(metadata).exists())
        repo.abandon(draft)
        assertFalse(draft.file.exists()); assertEquals("saved audio", repo.audioFile(saved).readText())
        assertEquals(listOf(saved), db.takes().readAll())
        val cold = File(directory, UUID.randomUUID().toString()+".pending.m4a").also { it.writeText("interrupted") }
        assertEquals(1, repo.recover()); assertFalse(cold.exists())
    }
    @Test fun failedDatabaseCommitRollsBackToSameTemporaryBytesThenSaveCanRetry() = checkStorage { db, repo, _ ->
        val saved = repo.prepare(null).also { it.file.writeText("keep") }.let { repo.save(it) }
        val draft = repo.prepare(null).also { it.file.writeText("new take") }
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_review BEFORE INSERT ON takes BEGIN SELECT RAISE(ABORT, 'injected storage failure'); END")
        try { repo.save(draft); fail("Save unexpectedly succeeded") } catch (_: android.database.sqlite.SQLiteException) { }
        assertEquals("new take", draft.file.readText()); assertEquals(0, repo.recover())
        assertEquals(listOf(saved), db.takes().readAll())
        db.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_review")
        val accepted = repo.save(draft)
        assertFalse(draft.file.exists()); assertEquals(draft.id, accepted.id)
        assertEquals("new take", repo.audioFile(accepted).readText()); assertEquals("keep", repo.audioFile(saved).readText())
        assertEquals(2, db.takes().readAll().size)
    }
    @Test fun incompleteLoopCannotBeReviewedOrSavedAndCompleteLoopRequiresExplicitSave() = checkStorage { db, repo, _ ->
        val block = BlockRepository(db).save(null, BlockDraft("Keep Block", "Play"))
        val spec = PhraseSpec(240,4)
        val incomplete = repo.prepare(block,spec)
        PhraseWave.write(incomplete.file,ShortArray(spec.frames-1))
        try { repo.inspect(incomplete); fail("Incomplete loop reviewed") } catch (_: IllegalArgumentException) { }
        try { repo.save(incomplete); fail("Incomplete loop saved") } catch (_: IllegalArgumentException) { }
        repo.abandon(incomplete)
        val draft = repo.prepare(block,spec)
        val samples = ShortArray(spec.frames) { (it%100).toShort() }; PhraseWave.write(draft.file,samples)
        val reviewed = repo.inspect(draft)
        assertTrue(db.takes().readAll().isEmpty()); assertEquals("LOOP",reviewed.mode)
        assertEquals(4,reviewed.beatCount); assertEquals(240,reviewed.recordedBpm)
        val saved = repo.save(draft)
        assertArrayEquals(samples,PhraseWave.read(repo.audioFile(saved)))
        assertEquals(listOf(saved),db.takes().readAll())
    }
    @Test fun rejectedEmptyTakeAndRerecordLeaveExistingRowsAndStableIdsUntouched() = checkStorage { db,repo,_ ->
        val block = BlockRepository(db).save(null,BlockDraft("Stable","Keep"))
        val before = db.blocks().readAll()
        val empty = repo.prepare(block)
        try { repo.inspect(empty); fail("Empty review accepted") } catch (_: IllegalArgumentException) { }
        repo.abandon(empty)
        val first = repo.prepare(block).also { it.file.writeText("discard") }; repo.inspect(first); repo.abandon(first)
        val replacement = repo.prepare(block).also { it.file.writeText("replacement") }
        assertNotEquals(first.id,replacement.id); assertTrue(db.takes().readAll().isEmpty())
        val saved = repo.save(replacement)
        assertEquals(before,db.blocks().readAll()); assertTrue(BackupRepository(db).snapshot().sessions.isEmpty())
        assertEquals("replacement",repo.audioFile(saved).readText())
    }
}
