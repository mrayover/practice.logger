package com.mrayover.practicelogger

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class) @Config(sdk = [35])
class PracticeStatisticsTest {
    private fun memory() = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext<android.content.Context>(), BlockDatabase::class.java).build()
    private suspend fun session(db: BlockDatabase, id: String = "s", status: String = "IN_PROGRESS", start: Long = 100) {
        db.sessions().insert(PracticeSession(id, start, status = status))
    }
    private suspend fun placement(db: BlockDatabase, id: String, ms: Long, block: String = "b", session: String = "s",
        outcome: String = "PENDING", prep: Long = 0) {
        db.sessions().insert(Placement(id, session, block, "Block $block", "Play", null, "", 0,
            60_000, ms, outcome, prepMs = prep))
    }
    private suspend fun goal(db: BlockDatabase, id: String) {
        db.goals().insert(Goal(id, id, createdAt = 1, updatedAt = 1))
    }
    private suspend fun total(db: BlockDatabase) = db.transcribeActivities().observePracticeTime().first().sumOf { it.activeMs }

    @Test fun emptyHasNoInventedPractice() = runBlocking {
        val db = memory()
        try { assertTrue(db.transcribeActivities().observeFacts().first().isEmpty()); assertEquals(0L, total(db)) }
        finally { db.close() }
    }

    @Test fun completedRepeatedAndMixedBlocksCountPlacementsNotIdentities() = runBlocking {
        val db = memory()
        try {
            session(db, status = "COMPLETED")
            placement(db, "p1", 60_000, outcome = "COMPLETED")
            placement(db, "p2", 35_000, outcome = "COMPLETED")
            placement(db, "p3", 90_000, block = "other", outcome = "COMPLETED")
            val rows = db.transcribeActivities().observeFacts().first()
            assertEquals(3, rows.size); assertEquals(185_000L, total(db))
            assertEquals(95_000L, rows.filter { it.blockId == "b" }.sumOf { it.activeMs })
        } finally { db.close() }
    }

    @Test fun unfinishedPrepOvertimeAndSkippedOutcomes() = runBlocking {
        val db = memory()
        try {
            session(db)
            placement(db, "unfinished", 12_000, prep = 300_000)
            placement(db, "overtime", 125_000, outcome = "COMPLETED")
            placement(db, "skipWorked", 4_000, outcome = "SKIPPED")
            placement(db, "skipEmpty", 0, outcome = "SKIPPED", prep = 600_000)
            placement(db, "prepOnly", 0, prep = 900_000)
            session(db, "draft", "DRAFT"); placement(db, "draftRow", 999_000, session = "draft")
            val rows = db.transcribeActivities().observeFacts().first()
            assertEquals(141_000L, total(db)); assertEquals(5, rows.size)
            assertEquals("Skipped after practice", practiceOutcome(rows.single { it.placementId == "skipWorked" }))
            assertEquals("Skipped without practice", practiceOutcome(rows.single { it.placementId == "skipEmpty" }))
            assertEquals("Unfinished", practiceOutcome(rows.single { it.placementId == "unfinished" }))
        } finally { db.close() }
    }

    @Test fun snapshotsOverrideSessionGoalAndExplicitNoGoalBlocksFallback() = runBlocking {
        val db = memory()
        try {
            goal(db, "launch"); goal(db, "primary"); session(db)
            db.goals().insert(GoalSession("s", "launch"))
            placement(db, "primary", 10); placement(db, "none", 20); placement(db, "legacy", 30)
            db.goalAttribution().insert(PlacementGoalSnapshot("primary", "primary", 2))
            db.goalAttribution().insert(PlacementGoalSnapshot("none", null, null))
            val rows = db.transcribeActivities().observeFacts().first().associateBy { it.placementId }
            assertEquals("primary", rows["primary"]!!.goalId)
            assertNull(rows["none"]!!.goalId); assertEquals("NO_GOAL", rows["none"]!!.attribution)
            assertEquals("LEGACY_SESSION", rows["legacy"]!!.attribution)
            val totals = db.goals().observeTime().first().associate { it.goalId to it.activeMs }
            assertEquals(mapOf("primary" to 10L, "launch" to 30L), totals)
            assertEquals(total(db), totals.values.sum() + rows.values.filter { it.goalId == null }.sumOf { it.activeMs })
        } finally { db.close() }
    }

    @Test fun ordinaryLegacyTimeRemainsUnknownRegardlessOfCurrentLinks() = runBlocking {
        val db = memory()
        try {
            goal(db, "a"); goal(db, "b")
            val block = BlockRepository(db).save(null, BlockDraft("Study", "Play"))
            db.goals().insert(GoalBlock("a", block, 0)); db.goals().insert(GoalBlock("b", block, 0))
            db.goalAttribution().setPrimary(BlockPrimaryGoal(block, "a"))
            session(db); placement(db, "old", 100, block)
            assertTrue(db.goals().observeTime().first().isEmpty())
            val row = db.transcribeActivities().observeFacts().first().single()
            assertEquals("UNKNOWN", row.attribution); assertNull(row.goalId)
            assertEquals(100L, total(db))
        } finally { db.close() }
    }

    @Test fun firstAccrualAttributionSurvivesReassignmentReorderCompletionAndReopening() = runBlocking {
        val db = memory()
        try {
            val goals = GoalRepository(db)
            val a = goals.save(null, "A", "", 23, "")
            val b = goals.save(null, "B", "", 50, "")
            val blocks = BlockRepository(db)
            val draft = BlockDraft("Study", "Play", primaryGoalId = a, primaryGoalChanged = true)
            val block = blocks.save(null, draft)
            val sessions = SessionRepository(db)
            val ordinary = sessions.startBlock(block); sessions.checkpoint(ordinary, 500)
            blocks.save(block, draft.copy(primaryGoalId = b))
            goals.move(b, -1); goals.setCompleted(a, true); goals.setCompleted(a, false)
            sessions.checkpoint(ordinary, 250)
            val launched = sessions.startGoal(b); sessions.checkpoint(launched, 1000)
            val totals = db.goals().observeTime().first().associate { it.goalId to it.activeMs }
            assertEquals(mapOf(a to 750L, b to 1000L), totals)
            assertEquals(1750L, total(db)); assertEquals(23, db.goals().find(a)!!.goal.progress)
        } finally { db.close() }
    }

    @Test fun completedStandaloneCountsOnceAndUnfinishedStandaloneDoesNotCount() = runBlocking {
        val db = memory()
        try {
            goal(db, "g"); session(db); placement(db, "p", 20)
            db.goalAttribution().insert(PlacementGoalSnapshot("p", "g", 1))
            db.goals().insert(GoalSession("s", "g"))
            db.transcribeActivities().insert(TranscribeActivity("done", 100, 200, 30, "g"))
            db.transcribeActivities().insert(TranscribeActivity("none", 100, 200, 40))
            db.transcribeActivities().insert(TranscribeActivity("unfinished", 100, activeMs = 500, goalId = "g"))
            assertEquals(90L, total(db)); assertEquals(50L, db.goals().observeTime().first().single().activeMs)
            assertEquals(3, db.transcribeActivities().observeFacts().first().size)
        } finally { db.close() }
    }

    @Test fun confidenceAndStableRecordKeysRemainAvailableWithoutDuplicatingNotes() = runBlocking {
        val db = memory()
        try {
            val block = BlockRepository(db).save(null, BlockDraft("Study", "Play"))
            session(db); placement(db, "p", 123, block = block, outcome = "COMPLETED")
            db.sessions().insert(BlockCompletion("p", "s", block, 200, null, null, null))
            db.sessions().save(BlockReflection("p", security = 4, revisit = "Tomorrow"))
            db.practiceNotes().insert(PracticeNote("n1", block, "s", "p", 100, "First note"))
            db.practiceNotes().insert(PracticeNote("n2", block, "s", "p", 200, "Second note"))
            val row = db.transcribeActivities().observeFacts().first().single()
            assertEquals(4, row.security); assertEquals(200L, row.completedAt)
            assertEquals("s", row.sessionId); assertEquals("p", row.placementId)
            assertEquals(123L, total(db))
        } finally { db.close() }
    }

    @Test fun mondayWeekUsesLocalStartDateWithoutMidnightSplitting() {
        val zone = ZoneId.of("America/Los_Angeles")
        val monday = LocalDate.of(2026, 10, 26)
        fun fact(id: String, date: LocalDate, ms: Long) = PracticeFact(id, id, "s", "b", "Block",
            date.atTime(23, 59).atZone(zone).toInstant().toEpochMilli(), null, ms, "PENDING", "IN_PROGRESS", null, "UNKNOWN", null, "")
        val rows = listOf(fact("before", monday.minusDays(1), 3600_000), fact("start", monday, 10),
            fact("last", monday.plusDays(6), 20), fact("after", monday.plusDays(7), 100))
        assertEquals(monday, logWeek(monday.plusDays(4)))
        assertEquals(30L, statisticsWeekMs(rows, monday.plusDays(4), zone))
    }
}
