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
class ReflectionRepositoryTest {
    @Test fun completionSnapshotsAndOptionalAnswersStayIndependentAfterReopen() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "block-reflection-test.db"
        context.deleteDatabase(name)
        var db = BlockDatabase.open(context, name)
        val legacy = Room.inMemoryDatabaseBuilder(context, BlockDatabase::class.java).build()
        try {
            val goals = GoalRepository(db)
            val firstGoal = goals.save(null, "First", "", 0, "")
            val completionGoal = goals.save(null, "Second", "", 0, "")
            val blocks = BlockRepository(db)
            val draft = BlockDraft("Same Block", "Play", primaryGoalId = firstGoal, primaryGoalChanged = true)
            val block = blocks.save(null, draft)
            var repo = SessionRepository(db) { 1234L }
            repo.addBlocks(listOf(block, block, block))
            val id = repo.start()
            val (first, second, skipped) = db.sessions().find(id)!!.ordered
            repo.checkpoint(id, 800L)
            blocks.save(block, draft.copy(primaryGoalId = completionGoal))
            assertFalse(repo.finishBlock(id, "COMPLETED", "Practice note", placementId = first.id))
            val committed = db.sessions().find(id)!!
            val completion = BlockCompletion(first.id, id, block, 1234L, completionGoal, 2, goalCompletionColor(2))
            assertEquals(CompletedPlacement(completion, null), committed.completions.single())
            assertEquals("COMPLETED", committed.ordered.first().outcome)
            assertEquals(second.id, committed.current!!.id)
            assertTrue(runCatching { repo.finishBlock(id, "COMPLETED", "", placementId = first.id) }.isFailure)

            assertTrue(runCatching { repo.saveBlockReflection(BlockReflection(first.id, 0)) }.isFailure)
            assertTrue(runCatching { repo.saveBlockReflection(BlockReflection(first.id, 6)) }.isFailure)
            for (rating in 1..5) {
                val review = BlockReflection(first.id, rating, revisit = "Slow entrance")
                repo.saveBlockReflection(review)
                assertEquals(CompletedPlacement(completion, review), db.sessions().find(id)!!.completions.single())
                assertEquals(800L, db.sessions().find(id)!!.activeMs)
            }

            goals.move(completionGoal, -1)
            goals.setCompleted(completionGoal, true)
            blocks.save(block, draft.copy(primaryGoalId = null))
            assertFalse(repo.finishBlock(id, "COMPLETED", "", placementId = second.id))
            assertTrue(repo.finishBlock(id, "SKIPPED", "", placementId = skipped.id))
            val finished = db.sessions().find(id)!!
            assertEquals("COMPLETED", finished.session.status)
            assertNull(finished.reflection)
            assertEquals(setOf(first.id, second.id), finished.completions.map { it.completion.placementId }.toSet())
            assertEquals(CompletedPlacement(BlockCompletion(second.id, id, block, 1234L, null, null, null), null),
                finished.completions.single { it.completion.placementId == second.id })
            assertEquals(completion, finished.completions.single { it.completion.placementId == first.id }.completion)
            assertEquals(PlacementGoalSnapshot(first.id, firstGoal, 1), db.goalAttribution().snapshot(first.id))
            assertNull(db.goalAttribution().snapshot(second.id))
            val reflection = SessionReflection(id, energy = "Good")
            repo.saveSessionReflection(SessionReflection(id), "")
            assertNull(db.sessions().find(id)!!.reflection)
            repo.saveSessionReflection(reflection, " Session note ")
            db.close()
            db = BlockDatabase.open(context, name)
            repo = SessionRepository(db)
            val reopened = db.sessions().find(id)!!
            assertEquals(finished.completions, reopened.completions)
            assertEquals(finished.placements, reopened.placements)
            assertEquals(reflection, reopened.reflection)
            assertEquals("Session note", reopened.session.note)
            assertTrue(runCatching { repo.finishBlock(id, "COMPLETED", "", placementId = skipped.id) }.isFailure)

            val json = BackupCodec.encode(BackupRepository(db).snapshot())
            BackupRepository(legacy).restore(BackupCodec.decode(json))
            val oldHistory = legacy.sessions().find(id)!!
            assertEquals(reopened.session, oldHistory.session)
            assertEquals(reopened.placements, oldHistory.placements)
            assertTrue(oldHistory.completions.isEmpty())
            assertNull(oldHistory.reflection)
        } finally {
            db.close()
            legacy.close()
            context.deleteDatabase(name)
        }
    }
}
