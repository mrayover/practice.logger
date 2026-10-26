package com.mrayover.practicelogger

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SessionRepositoryTest {
    private val context get() = ApplicationProvider.getApplicationContext<android.content.Context>()
    @Test fun orderedPlacementsOverridesAndSnapshotsStayIndependent() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, BlockDatabase::class.java).build()
        try {
            val blocks = BlockRepository(db) { 100L }; val sessions = SessionRepository(db) { 200L }
            val a = blocks.save(null, BlockDraft("A", "Original action", "5", "120", "Reference"))
            val b = blocks.save(null, BlockDraft("B", "Other action"))
            sessions.addBlock(a); sessions.addBlock(b); sessions.addBlock(a)
            val draftId = db.sessions().withStatus("DRAFT")!!.id
            var bundle = db.sessions().find(draftId)!!
            val first = bundle.ordered.first(); val last = bundle.ordered.last()
            sessions.move(last.id, -1)
            sessions.setDuration(first.id, 90_000L)
            bundle = db.sessions().find(draftId)!!
            assertEquals(listOf(a, a, b), bundle.ordered.map { it.sourceBlockId })
            assertEquals(90_000L, bundle.ordered.first().configuredMs)
            assertEquals(300_000L, bundle.ordered[1].configuredMs)
            assertEquals(5, db.blocks().find(a)!!.durationMinutes)
            blocks.save(a, BlockDraft("Changed", "New action")); blocks.setArchived(a, true)
            assertEquals("A", db.sessions().find(draftId)!!.ordered.first().name)
            assertEquals("Original action", db.sessions().find(draftId)!!.ordered.first().action)
            sessions.remove(last.id)
            assertEquals(listOf(0, 1), db.sessions().find(draftId)!!.ordered.map { it.position })
            sessions.discardDraft()
            assertNull(db.sessions().find(draftId))
            assertNotNull(db.blocks().find(a))
        } finally { db.close() }
    }
    @Test fun threeBlockSessionRecoveryOutcomesAndHistoryAfterReopen() = runBlocking {
        val name = "session-recovery-test.db"; context.deleteDatabase(name)
        var db = BlockDatabase.open(context, name)
        try {
            val blockRepo = BlockRepository(db) { 100L }
            val ids = (1..3).map { blockRepo.save(null, BlockDraft("Block " + it, "Action " + it)) }
            var repo = SessionRepository(db) { 1_000L }
            ids.forEach { repo.addBlock(it) }
            val id = repo.start()
            repo.checkpoint(id, 1_234L)
            assertFalse(repo.finishBlock(id, "COMPLETED", "Early"))
            repo.checkpoint(id, 500L)
            assertFalse(repo.finishBlock(id, "SKIPPED", ""))
            repo.checkpoint(id, 2_100L)
            db.close(); db = BlockDatabase.open(context, name); repo = SessionRepository(db) { 9_000L }
            val recovered = db.sessions().find(id)!!
            assertEquals(2, recovered.session.currentIndex)
            assertEquals(2_100L, recovered.current!!.activeMs)
            assertEquals(listOf("COMPLETED", "SKIPPED", "PENDING"), recovered.ordered.map { it.outcome })
            // Paused/background wall time adds nothing; only explicit active deltas count.
            assertEquals(3_834L, recovered.activeMs)
            repo.checkpoint(id, 900L)
            assertTrue(repo.finishBlock(id, "COMPLETED", "Done"))
            repo.saveSessionNote(id, "Good session")
            val completed = db.sessions().find(id)!!
            assertEquals("COMPLETED", completed.session.status)
            assertEquals(4_734L, completed.activeMs)
            assertEquals(9_000L, completed.session.endedAt)
            assertEquals("Good session", completed.session.note)
            BlockRepository(db).save(ids[0], BlockDraft("Renamed", "Changed"))
            assertEquals("Block 1", db.sessions().find(id)!!.ordered[0].name)
            try { repo.checkpoint(id, 1000); fail("Changed completed history") } catch (_: IllegalStateException) { }
            assertEquals(completed, db.sessions().find(id))
        } finally { db.close(); context.deleteDatabase(name) }
    }
    @Test fun unfinishedBlockNotesSurviveReopenAndCannotChangeOtherPlacementsOrHistory() = runBlocking {
        val name = "unfinished-note-test.db"; context.deleteDatabase(name)
        var db = BlockDatabase.open(context, name)
        try {
            val block = BlockRepository(db).save(null, BlockDraft("Notes", "Play"))
            var repo = SessionRepository(db)
            repo.addBlock(block); repo.addBlock(block)
            val id = repo.start()
            val first = db.sessions().find(id)!!.current!!
            repo.checkpoint(id, 1234)
            repo.saveBlockNote(id, first.id, "  Slow phrase ♫  ")
            db.close(); db = BlockDatabase.open(context, name); repo = SessionRepository(db)
            val reopened = db.sessions().find(id)!!
            assertEquals("Slow phrase ♫", reopened.current!!.note)
            assertEquals(1234L, reopened.current!!.activeMs)
            assertEquals("PENDING", reopened.current!!.outcome)
            assertEquals("", reopened.ordered[1].note)
            try { repo.saveBlockNote(id, reopened.ordered[1].id, "Wrong"); fail("Edited future Block") } catch (_: IllegalStateException) { }
            repo.saveBlockNote(id, first.id, "")
            assertEquals("", db.sessions().find(id)!!.current!!.note)
            repo.finishBlock(id, "COMPLETED", "Final first")
            try { repo.saveBlockNote(id, first.id, "Wrong"); fail("Edited prior Block") } catch (_: IllegalStateException) { }
            val last = db.sessions().find(id)!!.current!!
            repo.finishBlock(id, "SKIPPED", "Final second")
            val history = db.sessions().find(id)
            try { repo.saveBlockNote(id, last.id, "Wrong"); fail("Edited history") } catch (_: IllegalStateException) { }
            assertEquals(history, db.sessions().find(id))
        } finally { db.close(); context.deleteDatabase(name) }
    }
    @Test fun emptySessionsAndBadOverridesAreRejected() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, BlockDatabase::class.java).build()
        try {
            val repo = SessionRepository(db)
            try { repo.start(); fail("Started empty session") } catch (_: IllegalStateException) { }
            val block = BlockRepository(db).save(null, BlockDraft("A", "Play"))
            repo.addBlock(block)
            val draft = db.sessions().withStatus("DRAFT")!!
            val placement = db.sessions().find(draft.id)!!.current!!
            try { repo.setDuration(placement.id, 0); fail("Accepted zero") } catch (_: IllegalArgumentException) { }
            try { repo.move(placement.id, -1); fail("Moved out of range") } catch (_: IllegalArgumentException) { }
            val active = repo.start()
            try { repo.addBlock(block); fail("Edited active session") } catch (_: IllegalStateException) { }
            assertEquals(1, db.sessions().find(active)!!.placements.size)
        } finally { db.close() }
    }
    @Test fun zeroWaitOvertimeAndLargeDurationsAreSafe() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, BlockDatabase::class.java).build()
        try {
            val repo = SessionRepository(db)
            val block = BlockRepository(db).save(null, BlockDraft("Long", "Play", Int.MAX_VALUE.toString()))
            repo.addBlock(block)
            val draft = db.sessions().withStatus("DRAFT")!!
            val placement = db.sessions().find(draft.id)!!.current!!
            assertEquals(Int.MAX_VALUE.toLong() * 60_000L, placement.configuredMs)
            repo.setDuration(placement.id, 1000)
            val id = repo.start()
            repo.checkpoint(id, 1500)
            assertEquals(1000L, db.sessions().find(id)!!.current!!.activeMs)
            assertEquals("PENDING", db.sessions().find(id)!!.current!!.outcome)
            repo.checkpoint(id, 5000)
            assertEquals(1000L, db.sessions().find(id)!!.current!!.activeMs)
            repo.continueOvertime(id); repo.checkpoint(id, 650)
            assertEquals(1650L, db.sessions().find(id)!!.current!!.activeMs)
            assertTrue(repo.finishBlock(id, "COMPLETED", ""))
        } finally { db.close() }
    }
    @Test fun versionOneMigrationPreservesExistingBlocks() = runBlocking {
        val name = "migration-one-two.db"; context.deleteDatabase(name)
        val file = context.getDatabasePath(name); file.parentFile!!.mkdirs()
        val sqlite = android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(file, null)
        sqlite.execSQL("CREATE TABLE blocks (id TEXT NOT NULL, name TEXT NOT NULL, action TEXT NOT NULL, durationMinutes INTEGER NOT NULL, tempo INTEGER, notes TEXT NOT NULL, archived INTEGER NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, PRIMARY KEY(id))")
        sqlite.execSQL("INSERT INTO blocks VALUES ('legacy', 'Old Block', 'Keep this action', 7, 120, 'Existing note', 0, 1, 2)")
        sqlite.execSQL("CREATE TABLE room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)")
        sqlite.execSQL("INSERT INTO room_master_table VALUES (42, '740ceb46ea18aabd35c3fd36b704888e')")
        sqlite.version = 1; sqlite.close()
        val db = BlockDatabase.open(context, name)
        try {
            assertEquals("Old Block", db.blocks().find("legacy")!!.name)
            assertEquals(7, db.blocks().find("legacy")!!.durationMinutes)
            assertEquals("Existing note", db.blocks().find("legacy")!!.notes)
            val repo = SessionRepository(db); repo.addBlock("legacy")
            assertNotNull(repo.start())
        } finally { db.close(); context.deleteDatabase(name) }
    }
}

class PracticeClockTest {
    private val entry = Placement("p", "s", "b", "Name", "Action", null, "", 0, 1000)
    @Test fun activeDeltasStopAtZeroUntilExplicitContinue() {
        val atZero = advancePractice(entry, 5000)
        assertEquals(1000L, atZero.activeMs)
        assertEquals(atZero, advancePractice(atZero, 2000))
        val overtime = advancePractice(atZero.copy(overtimeAccepted = true), 600)
        assertEquals(1600L, overtime.activeMs)
        assertEquals(overtime.copy(outcome = "COMPLETED"), advancePractice(overtime.copy(outcome = "COMPLETED"), 500))
    }
    @Test fun displayRoundingAndOverflowStayTruthful() {
        assertEquals("0:01", practiceTime(1, true))
        assertEquals("0:00", practiceTime(0, true))
        assertEquals("61:01", practiceTime(3_661_999))
        assertEquals(Long.MAX_VALUE, advancePractice(entry.copy(activeMs = Long.MAX_VALUE - 1, overtimeAccepted = true), 10).activeMs)
        try { advancePractice(entry, -1); fail("Negative active time accepted") } catch (_: IllegalArgumentException) { }
    }
}
