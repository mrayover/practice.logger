package com.mrayover.practicelogger

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.LocalDate

class WeeklyTargetWorkflowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val db get() = BlockDatabase.appInstance(compose.activity.application)
    private fun waitFor(tag: String) = compose.waitUntil(15000) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty() }
    private fun clickTag(tag: String) = compose.onNodeWithTag(tag).performScrollTo().assertIsDisplayed().assertIsEnabled().performClick()
    private fun home() {
        compose.onNodeWithTag("home-brand", useUnmergedTree = true).performClick()
        waitFor("home-weekly-target")
    }

    private fun visible(text: String) {
        compose.waitUntil(15_000) { compose.onAllNodesWithText(text, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodesWithText(text, useUnmergedTree = true).onFirst().performScrollTo().assertIsDisplayed()
    }

    @Test fun livePlanAndGoalMinutesSurviveRecreation() {
        waitFor("home-weekly-target")
        val week = practiceWeek()
        val start = LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        val blocks = BlockRepository(db)
        val goals = GoalRepository(db)
        val draft = BlockDraft("Weekly practice", "Play", "30")
        val (block, firstGoal, nextGoal) = runBlocking {
            val block = blocks.save(null, draft, week)
            val firstGoal = goals.save(null, "First Goal", "", 37, "")
            val nextGoal = goals.save(null, "Second Goal", "", "")
            goals.setBlocks(firstGoal, listOf(block))
            db.sessions().insert(PracticeSession("weekly-preview", start, status = "IN_PROGRESS"))
            db.goals().insert(GoalSession("weekly-preview", firstGoal))
            db.sessions().insert(Placement("weekly-placement", "weekly-preview", block, "Weekly practice", "Play", null, "", 0, 60_000, 90 * 60_000L))
            db.sessions().insert(PracticeSession("weekly-unassigned", start, status = "IN_PROGRESS"))
            db.sessions().insert(Placement("weekly-unassigned-placement", "weekly-unassigned", block, "Weekly practice", "Play", null, "", 0, 60_000, 6 * 60_000L))
            db.weeklyTargets().save(WeeklyPracticeTarget(week.toString(), 80))
            Triple(block, firstGoal, nextGoal)
        }
        visible("96 / 90 min")
        visible("6 min over")
        visible("90 / 90 min · This week")
        visible("0 / 0 min · This week")
        compose.onNodeWithText("Edit weekly plan", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        clickTag("home-weekly-target")
        compose.onNodeWithText("Prepare Week").assertIsDisplayed()
        openBlockPill(compose, "week-list", "week-block-$block")
        clickTag("week-repetitions-plus-$block")
        home()
        visible("96 / 120 min")
        visible("24 min left")
        runBlocking {
            blocks.save(block, draft.copy(duration = "15"))
            goals.setBlocks(firstGoal, emptyList())
            goals.setBlocks(nextGoal, listOf(block))
            goals.save(firstGoal, "First Goal", "Still historical", "")
        }
        visible("96 / 60 min")
        visible("90 / 0 min · This week")
        visible("0 / 60 min · This week")
        compose.activityRule.scenario.recreate()
        visible("96 / 60 min")
        visible("90 / 0 min · This week")
        runBlocking { blocks.removeFromWeek(week, block) }
        visible("96 / 0 min")
        visible("No plan")
        assertEquals(80, runBlocking { db.weeklyTargets().find(week.toString())!!.targetMinutes })
        assertEquals(37, runBlocking { db.goals().find(firstGoal)!!.goal.progress })
        clickTag("home-statistics")
        waitFor("statistics-screen")
    }

    @Test fun retiredWeeklyTargetsStillRoundTripAndProtectRestore() {
        val week = practiceWeek()
        runBlocking {
        val context = compose.activity.application
        fun memory() = androidx.room.Room.inMemoryDatabaseBuilder(context, BlockDatabase::class.java).addCallback(mediaConstraintCallback).build()
        val source = memory(); val target = memory()
        val root = File(context.cacheDir, "w2-atomic").apply { mkdirs() }
        val destination = File(root, "restored").apply { mkdirs() }
        try {
            source.weeklyTargets().save(WeeklyPracticeTarget(week.toString(), 180))
            val archive = File(root, "source.zip"); PortableZip(context, source, root).create(archive)
            val failed = PortableRestore(context, target, destination, atBoundary = { if (it == RestoreBoundary.BEFORE_COMMIT) error("Injected W2 rollback") })
            assertTrue(runCatching { failed.restore(failed.stage { archive.inputStream() }) }.isFailure)
            assertTrue(portableTargetEmpty(target.openHelper.writableDatabase))
            val restore = PortableRestore(context, target, destination)
            val staged = restore.stage { archive.inputStream() }
            target.weeklyTargets().save(WeeklyPracticeTarget(week.plusWeeks(1).toString(), 90))
            assertTrue(runCatching { restore.restore(staged) }.exceptionOrNull() is NonEmptyRestoreException)
            assertEquals(90, target.weeklyTargets().observeAll().first().single().targetMinutes)
            target.weeklyTargets().remove(week.plusWeeks(1).toString())
            restore.discard(staged)
            restore.restore(restore.stage { archive.inputStream() })
            assertEquals(180, target.weeklyTargets().observeAll().first().single().targetMinutes)
            assertTrue(runCatching { restore.stage { archive.inputStream() } }.exceptionOrNull() is NonEmptyRestoreException)
        } finally { source.close(); target.close(); root.deleteRecursively() }
        }
    }
}
