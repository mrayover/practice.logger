package com.mrayover.practicelogger

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class PracticeWorkflowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun shown(text: String) { compose.waitUntil(15000) { compose.onAllNodesWithText(text, substring = false).fetchSemanticsNodes().isNotEmpty() } }
    private fun click(text: String) { compose.onNodeWithText(text, substring = false).performScrollTo().performClick() }
    @Test fun buildThreePracticeOutcomesAndLeaveRecoverableOvertime() {
        val context = compose.activity.applicationContext
        val db = BlockDatabase.open(context)
        try {
            runBlocking {
                val repo = BlockRepository(db)
                repo.save(null, BlockDraft("Warmup fixture", "Original warmup action", "1"))
                repo.save(null, BlockDraft("Skip fixture", "Skip this exercise", "1"))
                repo.save(null, BlockDraft("Overtime fixture", "Keep practicing after zero", "1"))
            }
            shown("Practice Now"); click("Practice Now")
            shown("Add Warmup fixture"); click("Add Warmup fixture")
            shown("1. Warmup fixture")
            click("Add Skip fixture"); shown("2. Skip fixture")
            click("Add Overtime fixture"); shown("3. Overtime fixture")
            compose.onNode(hasText("Up") and hasAnyAncestor(hasTestTag("placement-2"))).performScrollTo().performClick()
            shown("2. Overtime fixture")
            compose.onNode(hasText("Down") and hasAnyAncestor(hasTestTag("placement-1"))).performScrollTo().performClick()
            shown("3. Overtime fixture")
            compose.onNode(hasText("Change duration") and hasAnyAncestor(hasTestTag("placement-0"))).performScrollTo().performClick()
            compose.onNodeWithText("Minutes").performTextReplacement("2")
            compose.onNodeWithText("Apply duration").performClick()
            shown("Session duration: 2:00")
            runBlocking {
                val draft = db.sessions().withStatus("DRAFT")!!
                val bundle = db.sessions().find(draft.id)!!
                SessionRepository(db).setDuration(bundle.ordered.last().id, 1000L)
                assertEquals(1, db.blocks().find(bundle.ordered.first().sourceBlockId)!!.durationMinutes)
            }
            click("Start Session"); shown("Warmup fixture"); click("Start")
            compose.waitUntil(10000) { compose.onAllNodesWithText("1:59").fetchSemanticsNodes().isNotEmpty() }
            click("Complete"); shown("Skip fixture"); click("Skip")
            shown("Overtime fixture"); click("Start"); shown("Continue")
            compose.onNodeWithText("Overtime fixture").assertExists()
            compose.onNodeWithTag("practice-timer").assertTextEquals("0:00")
            click("Continue")
            compose.waitUntil(10000) { compose.onAllNodesWithText("+0:01").fetchSemanticsNodes().isNotEmpty() }
            click("Pause"); shown("Resume")
            compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
            Thread.sleep(400)
            val before = runBlocking { val active = db.sessions().withStatus("IN_PROGRESS")!!; db.sessions().find(active.id)!!.current!!.activeMs }
            Thread.sleep(800)
            val after = runBlocking { val active = db.sessions().withStatus("IN_PROGRESS")!!; db.sessions().find(active.id)!!.current!!.activeMs }
            assertEquals(before, after)
            compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
            shown("Resume"); click("Resume")
            compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
            Thread.sleep(400)
            val background = runBlocking { val active = db.sessions().withStatus("IN_PROGRESS")!!; db.sessions().find(active.id)!!.current!!.activeMs }
            Thread.sleep(800)
            assertEquals(background, runBlocking { val active = db.sessions().withStatus("IN_PROGRESS")!!; db.sessions().find(active.id)!!.current!!.activeMs })
            compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
            shown("Resume"); click("Save for later"); shown("Continue Session")
        } finally { db.close() }
    }
}

class PracticeRecoveryTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun shown(text: String) { compose.waitUntil(15000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() } }
    @Test fun recoverPausedThenCompleteAndRetainSnapshots() {
        shown("Continue Session"); compose.onNodeWithText("Continue Session").performClick()
        shown("Overtime fixture"); compose.onNodeWithText("Resume").assertExists().performScrollTo().performClick()
        compose.onNodeWithText("Pause").assertExists().performScrollTo().performClick()
        compose.onNodeWithText("Complete").performScrollTo().performClick()
        shown("Session Summary")
        compose.onNodeWithText("Original warmup action").assertExists()
        compose.onNodeWithText("Session note (optional)").performScrollTo().performTextInput("Restart verified")
        compose.onNodeWithText("Save note").performScrollTo().performClick(); shown("Note saved")
        val db = BlockDatabase.open(compose.activity.applicationContext)
        try {
            runBlocking {
                val session = db.sessions().withStatus("COMPLETED")!!
                val bundle = db.sessions().find(session.id)!!
                assertEquals(listOf("COMPLETED", "SKIPPED", "COMPLETED"), bundle.ordered.map { it.outcome })
                assertTrue(bundle.ordered.first().activeMs in 1000L..15_000L)
                assertEquals(0L, bundle.ordered[1].activeMs)
                assertTrue(bundle.ordered.last().overtimeAccepted)
                val source = bundle.ordered.first().sourceBlockId
                BlockRepository(db).save(source, BlockDraft("Renamed after practice", "Changed after practice"))
                BlockRepository(db).setArchived(source, true)
            }
        } finally { db.close() }
        compose.onNodeWithText("Home", substring = false).performScrollTo().performClick()
        shown("Recent Practice")
    }
}

class HistoryReopenTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun savedSummaryAndOriginalSnapshotsReopenAfterRestart() {
        compose.waitUntil(15000) { compose.onAllNodes(hasText("2 completed · 1 skipped", substring = true)).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(hasText("2 completed · 1 skipped", substring = true)).performScrollTo().performClick()
        compose.onNodeWithText("Session Summary").assertExists()
        compose.onNodeWithText("Original warmup action").assertExists()
        compose.onNodeWithText("Warmup fixture", substring = false).assertExists()
        compose.onNodeWithText("Session note (optional)").performScrollTo().assertTextContains("Restart verified")
        compose.onNodeWithText("Changed after practice").assertDoesNotExist()
    }
}
