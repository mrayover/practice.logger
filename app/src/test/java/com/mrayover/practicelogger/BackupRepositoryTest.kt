package com.mrayover.practicelogger

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BackupRepositoryTest {
    private val context get() = ApplicationProvider.getApplicationContext<android.content.Context>()
    private fun memory() = Room.inMemoryDatabaseBuilder(context, BlockDatabase::class.java).build()
    private fun rejected(operation: () -> Unit) { try { operation(); fail("Accepted invalid backup") } catch (_: IllegalArgumentException) { } catch (_: IllegalStateException) { } catch (_: org.json.JSONException) { } }
    @Test fun fullRoundTripToEmptyDatabasePreservesHistoryDraftAndArchivedBlocks(): Unit = runBlocking {
        val source = memory(); val name = "backup-restored.db"; context.deleteDatabase(name)
        var target = BlockDatabase.open(context, name)
        try {
            val blocks = BlockRepository(source) { 123L }; val practice = SessionRepository(source) { 456L }
            val a = blocks.save(null, BlockDraft("Å étude 🎵", "Play\nslowly", "7", "123", "Quoted \"reference\""))
            practice.addBlock(a); val completed = practice.start()
            practice.checkpoint(completed, 2000); practice.finishBlock(completed, "COMPLETED", "block note")
            practice.saveSessionNote(completed, "session note")
            blocks.save(a, BlockDraft("Changed", "New action")); blocks.setArchived(a, true)
            val b = blocks.save(null, BlockDraft("Second", "Play"))
            practice.addBlock(b); practice.addBlock(b)
            val draft = source.sessions().withStatus("DRAFT")!!
            practice.setDuration(source.sessions().find(draft.id)!!.ordered[0].id, 90_000)
            val original = BackupRepository(source).snapshot()
            val file = java.io.File(context.cacheDir, "roundtrip.json")
            file.writeBytes(BackupCodec.encode(original))
            val decoded = file.inputStream().use { BackupCodec.decode(readBackup(it)) }
            assertEquals(original, decoded)
            BackupRepository(target).restore(decoded)
            target.close(); target = BlockDatabase.open(context, name)
            assertEquals(original, BackupRepository(target).snapshot())
            assertEquals("Å étude 🎵", target.sessions().find(completed)!!.current!!.name)
            assertTrue(target.blocks().find(a)!!.archived)
            assertEquals(90_000L, target.sessions().find(draft.id)!!.current!!.configuredMs)
            file.delete()
        } finally { source.close(); target.close(); context.deleteDatabase(name) }
    }
    @Test fun activeOvertimeRecoveryPreservesTimeAndCurrentPosition(): Unit = runBlocking {
        val source = memory(); val target = memory()
        try {
            val id = BlockRepository(source).save(null, BlockDraft("A", "Play"))
            val repo = SessionRepository(source); repo.addBlock(id); repo.addBlock(id)
            val draft = source.sessions().withStatus("DRAFT")!!
            repo.setDuration(source.sessions().find(draft.id)!!.ordered[1].id, 1000)
            val session = repo.start(); repo.finishBlock(session, "SKIPPED", "skip")
            repo.checkpoint(session, 1000); repo.continueOvertime(session); repo.checkpoint(session, 234)
            val data = BackupCodec.decode(BackupCodec.encode(BackupRepository(source).snapshot()))
            BackupRepository(target).restore(data)
            val recovered = target.sessions().find(session)!!
            assertEquals(1, recovered.session.currentIndex); assertEquals(1234L, recovered.current!!.activeMs)
            assertTrue(recovered.current!!.overtimeAccepted)
            SessionRepository(target).finishBlock(session, "COMPLETED", "restored")
        } finally { source.close(); target.close() }
    }
    @Test fun invalidFilesAndNonemptyTargetsNeverModifyExistingData(): Unit = runBlocking {
        val source = memory(); val target = memory()
        try {
            val a = BlockRepository(source).save(null, BlockDraft("A", "Play"))
            SessionRepository(source).addBlock(a)
            val data = BackupRepository(source).snapshot()
            val encoded = BackupCodec.encode(data)
            for (bad in listOf("not json", "{}", JSONObject(String(encoded)).put("version", 2).toString(), JSONObject(String(encoded)).put("version", 1.5).toString())) {
                rejected { BackupCodec.decode(bad.toByteArray()) }
            }
            rejected { BackupCodec.decode((String(encoded) + " trailing").toByteArray()) }
            rejected { BackupCodec.decode(("[".repeat(1000) + "0" + "]".repeat(1000)).toByteArray()) }
            rejected { BackupCodec.validate(data.copy(blocks = data.blocks + data.blocks)) }
            rejected { BackupCodec.validate(data.copy(blocks = emptyList())) }
            rejected { BackupCodec.validate(data.copy(placements = data.placements.map { it.copy(position = 2) })) }
            rejected { BackupCodec.validate(data.copy(placements = data.placements.map { it.copy(activeMs = -1) })) }
            rejected { BackupCodec.validate(data.copy(placements = data.placements.map { it.copy(overtimeAccepted = true) })) }
            rejected { BackupCodec.validate(data.copy(sessions = data.sessions.map { it.copy(status = "UNKNOWN") })) }
            rejected { readBackup(java.io.ByteArrayInputStream(ByteArray(BACKUP_LIMIT + 1))) }
            // Validate all records before the first insert; no partial restore.
            try { BackupRepository(target).restore(data.copy(placements = data.placements.map { it.copy(sessionId = "missing") })); fail() } catch (_: IllegalArgumentException) { }
            assertTrue(BackupRepository(target).snapshot().blocks.isEmpty())
            target.openHelper.writableDatabase.execSQL("CREATE TRIGGER reject_placement BEFORE INSERT ON placements BEGIN SELECT RAISE(ABORT, 'test disk/write failure'); END")
            try { BackupRepository(target).restore(data); fail("Partial restore committed") } catch (_: android.database.sqlite.SQLiteException) { }
            assertTrue(BackupRepository(target).snapshot().blocks.isEmpty())
            assertTrue(BackupRepository(target).snapshot().sessions.isEmpty())
            target.openHelper.writableDatabase.execSQL("DROP TRIGGER reject_placement")
            BlockRepository(target).save(null, BlockDraft("Keep me", "Do not replace"))
            val before = BackupRepository(target).snapshot()
            try { BackupRepository(target).restore(data); fail() } catch (_: IllegalStateException) { }
            assertEquals(before, BackupRepository(target).snapshot())
        } finally { source.close(); target.close() }
    }
}
