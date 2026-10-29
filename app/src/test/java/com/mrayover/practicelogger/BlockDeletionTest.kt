package com.mrayover.practicelogger

import android.content.Context
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
class BlockDeletionTest {
    private fun memory() = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), BlockDatabase::class.java).build()
    private suspend fun block(db: BlockDatabase, name: String = "Unused") = BlockRepository(db).save(null, BlockDraft(name, "Practice"))
    private suspend fun refusedWithoutChanges(db: BlockDatabase, id: String, expected: BlockDependencies) {
        val before = BackupRepository(db).snapshot()
        val takes = db.takes().readAll()
        val review = BlockRepository(db).reviewDeletion(id)
        assertEquals(expected, review.dependencies)
        assertFalse(review.dependencies.canDelete)
        assertEquals(BlockDeletionResult.Blocked(review), BlockRepository(db).deletePermanently(id))
        assertEquals(before, BackupRepository(db).snapshot())
        assertEquals(takes, db.takes().readAll())
    }
    @Test fun activeAndArchivedDeletionSurviveFileReopenAndJsonRestore() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "permanent-deletion-test.db"
        context.deleteDatabase(name)
        var db = BlockDatabase.open(context, name)
        val target = memory()
        try {
            val active = block(db, "Unused active")
            val archived = block(db, "Unused archived")
            BlockRepository(db).setArchived(archived, true)
            val kept = block(db, "Keep historical data")
            val sessions = SessionRepository(db)
            sessions.addBlock(kept); val sessionId = sessions.start()
            sessions.finishBlock(sessionId, "COMPLETED", "Keep note")
            val routine = RoutineRepository(db).save(null, "Kept Routine", listOf(RoutineSelection(kept, 60000)))
            val goal = GoalRepository(db).save(null, "Kept Goal", "Outcome", 30, "Goal notes")
            GoalRepository(db).setBlocks(goal, listOf(kept))
            val baseline = BackupRepository(db).snapshot()
            for (id in listOf(active, archived)) {
                assertTrue(BlockRepository(db).reviewDeletion(id).dependencies.canDelete)
                assertEquals(BlockDeletionResult.Deleted, BlockRepository(db).deletePermanently(id))
            }
            val after = BackupRepository(db).snapshot()
            assertEquals(baseline.copy(blocks = baseline.blocks.filter { it.id == kept }), after)
            db.close(); db = BlockDatabase.open(context, name)
            assertNull(db.blocks().find(active)); assertNull(db.blocks().find(archived))
            assertNotNull(db.routines().find(routine))
            assertEquals(after, BackupRepository(db).snapshot())
            val restored = BackupCodec.decode(BackupCodec.encode(after))
            BackupRepository(target).restore(restored)
            assertEquals(after, BackupRepository(target).snapshot())
        } finally { db.close(); target.close(); context.deleteDatabase(name) }
    }
    @Test fun activeAndCompletedGoalAssociationsRefuseDeletion() = runBlocking {
        val db = memory()
        try {
            val id = block(db)
            db.goals().insert(Goal("g1", "Active", createdAt = 1, updatedAt = 1))
            db.goals().insert(Goal("g2", "Completed", completed = true, priority = -1, createdAt = 1, updatedAt = 1))
            db.goals().insert(GoalBlock("g1", id, 0)); db.goals().insert(GoalBlock("g2", id, 0))
            refusedWithoutChanges(db, id, BlockDependencies(2, 0, 0, 0, 0))
        } finally { db.close() }
    }
    @Test fun routineCountIsDistinctRoutinesNotDuplicatePlacements() = runBlocking {
        val db = memory()
        try {
            val id = block(db)
            val rows = listOf(RoutineSelection(id, 60000), RoutineSelection(id, 120000))
            RoutineRepository(db).save(null, "First", rows)
            RoutineRepository(db).save(null, "Second", rows)
            refusedWithoutChanges(db, id, BlockDependencies(0, 2, 0, 0, 0))
        } finally { db.close() }
    }
    @Test fun recordingsRefuseDeletionIncludingMissingFileMetadataAndPreserveAudio() = runBlocking {
        val db = memory()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val audio = java.io.File(context.cacheDir, "delete-protected-audio.m4a")
        val bytes = byteArrayOf(1, 7, 3, 9)
        try {
            val id = block(db)
            audio.writeBytes(bytes)
            db.takes().insert(BlockTake("t1", id, 1, durationMs = 123, fileName = audio.name))
            db.takes().insert(BlockTake("t2", id, 2, "LOOP", 500, "missing.wav", recordedBpm = 120, beatCount = 4))
            db.takes().insert(BlockTake("standalone", null, 3, durationMs = 100, fileName = "standalone.m4a"))
            refusedWithoutChanges(db, id, BlockDependencies(0, 0, 2, 0, 0))
            assertArrayEquals(bytes, audio.readBytes())
        } finally { db.close(); audio.delete() }
    }
    @Test fun everySessionStateProtectsSourceBlockEvenWithoutForeignKey() = runBlocking {
        val db = memory()
        try {
            for (status in listOf("DRAFT", "IN_PROGRESS", "COMPLETED")) {
                val id = block(db, status)
                db.sessions().insert(PracticeSession(status, 1, endedAt = if (status == "COMPLETED") 2 else null, status = status))
                db.sessions().insert(Placement(status, status, id, status, "Practice", null, "", 0, 60000,
                    outcome = if (status == "COMPLETED") "COMPLETED" else "PENDING"))
                refusedWithoutChanges(db, id, BlockDependencies(0, 0, 0, 1, if (status == "COMPLETED") 1 else 0))
            }
        } finally { db.close() }
    }
    @Test fun confirmationRechecksReferencesAddedAfterEligibleReview() = runBlocking {
        val db = memory()
        try {
            val id = block(db)
            assertTrue(BlockRepository(db).reviewDeletion(id).dependencies.canDelete)
            SessionRepository(db).addBlock(id)
            refusedWithoutChanges(db, id, BlockDependencies(0, 0, 0, 1, 0))
        } finally { db.close() }
    }
    @Test fun databaseFailureAndMissingBlockNeverReturnDeleted() = runBlocking {
        val db = memory()
        try {
            val id = block(db)
            db.openHelper.writableDatabase.execSQL("CREATE TRIGGER refuse_block_delete BEFORE DELETE ON blocks BEGIN SELECT RAISE(ABORT, 'storage failure'); END")
            try { BlockRepository(db).deletePermanently(id); fail("Delete reported success") } catch (_: android.database.sqlite.SQLiteException) { }
            assertNotNull(db.blocks().find(id))
            try { BlockRepository(db).deletePermanently("missing"); fail("Missing reported success") } catch (_: IllegalStateException) { }
        } finally { db.close() }
    }
    @Test fun combinedCountsAndCompletedHistoryMessageAreAccurate() = runBlocking {
        val db = memory()
        try {
            val id = block(db)
            for (i in 0..1) {
                db.goals().insert(Goal("g$i", "Goal$i", priority = i, createdAt = 1, updatedAt = 1))
                db.goals().insert(GoalBlock("g$i", id, 0))
            }
            RoutineRepository(db).save(null, "Routine", listOf(RoutineSelection(id, 60000), RoutineSelection(id, 60000)))
            repeat(3) { db.takes().insert(BlockTake("t$it", id, 1, durationMs = 100, fileName = "$it.m4a")) }
            db.sessions().insert(PracticeSession("history", 1, 2, "COMPLETED", currentIndex = 3))
            repeat(4) { db.sessions().insert(Placement("p$it", "history", id, "History", "Practice", null, "", it, 60000, outcome = "COMPLETED")) }
            val expected = BlockDependencies(2, 1, 3, 4, 4)
            refusedWithoutChanges(db, id, expected)
            for (text in listOf("2 Goal", "1 saved Routines", "3 recordings", "4 practice session entries", "completed practice history", "Export"))
                assertTrue(expected.explanation().contains(text))
        } finally { db.close() }
    }
}
