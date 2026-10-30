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
    private fun click(text: String) { compose.onNodeWithText(text, substring = false).scrollIfNeeded().performClick() }
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
            shown("Practice Now"); compose.waitForIdle(); capturePractice("01-home"); click("Practice Now")
            shown("Add Warmup fixture"); click("Add Warmup fixture")
            shown("1. Warmup fixture")
            click("Add Skip fixture"); shown("2. Skip fixture")
            click("Add Overtime fixture"); shown("3. Overtime fixture")
            compose.onNode(hasText("Up") and hasAnyAncestor(hasTestTag("placement-2"))).scrollIfNeeded().performClick()
            shown("2. Overtime fixture")
            compose.onNode(hasText("Down") and hasAnyAncestor(hasTestTag("placement-1"))).scrollIfNeeded().performClick()
            shown("3. Overtime fixture")
            compose.onNode(hasText("Change duration") and hasAnyAncestor(hasTestTag("placement-0"))).scrollIfNeeded().performClick()
            compose.onNodeWithText("Minutes").performTextReplacement("2")
            compose.onNodeWithText("Apply duration").performClick()
            shown("Session duration: 2:00")
            runBlocking {
                val draft = db.sessions().withStatus("DRAFT")!!
                val bundle = db.sessions().find(draft.id)!!
                SessionRepository(db).setDuration(bundle.ordered.last().id, 1000L)
                assertEquals(1, db.blocks().find(bundle.ordered.first().sourceBlockId)!!.durationMinutes)
            }
            capturePractice("02-builder"); click("Start Session"); shown("Warmup fixture"); click("Start"); compose.waitForIdle(); capturePractice("03-focus")
            compose.waitUntil(10000) { compose.onAllNodesWithText("1:59").fetchSemanticsNodes().isNotEmpty() }
            click("Complete"); shown("Skip fixture"); click("Skip")
            shown("Overtime fixture"); click("Start"); shown("Continue")
            compose.onNodeWithText("Overtime fixture").assertExists()
            compose.onNodeWithTag("practice-timer").assertTextEquals("0:00")
            capturePractice("04-zero-choice"); click("Continue")
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
            shown("Resume")
            revealPracticeNotes(compose)
            compose.onNodeWithText("Quick note (optional)").performScrollTo().performTextInput("Saved while unfinished")
            click("Save Block note"); shown("Block note saved")
            revealPracticeNotes(compose)
            compose.onNodeWithText("Quick note (optional)").performScrollTo().performTextReplacement("Saved on leaving ♫")
            leavePracticeForLater(compose); openHomePractice(compose); shown("Continue Session")
            click("Continue Session"); shown("Overtime fixture")
            revealPracticeNotes(compose)
            compose.onNodeWithText("Quick note (optional)").performScrollTo().assertTextContains("Saved on leaving ♫")
            revealPracticeNotes(compose)
            compose.onNodeWithText("Quick note (optional)").performTextReplacement("Saved on Back ♫")
            val device = androidx.test.uiautomator.UiDevice.getInstance(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation())
            // The first Back may dismiss the IME; the next must save and leave Focus.
            repeat(2) {
                if (compose.onAllNodesWithTag("home-menu").fetchSemanticsNodes().isEmpty()) {
                    device.pressBack(); device.waitForIdle(); compose.waitForIdle()
                    if (compose.onAllNodesWithText("Back to Main").fetchSemanticsNodes().isNotEmpty()) compose.onNodeWithText("Back to Main").performClick()
                }
            }
            openHomePractice(compose); shown("Continue Session")
        } finally { db.close() }
    }
}

class PracticeRecoveryTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun shown(text: String) { compose.waitUntil(15000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() } }
    @Test fun recoverPausedThenCompleteAndRetainSnapshots() {
        openHomePractice(compose); shown("Continue Session"); compose.onNodeWithText("Continue Session").performClick()
        shown("Overtime fixture")
        revealPracticeNotes(compose)
        compose.onNodeWithText("Quick note (optional)").performScrollTo().assertTextContains("Saved on Back ♫")
        compose.onNodeWithText("Resume").assertExists().scrollIfNeeded().performClick()
        compose.onNodeWithText("Pause").assertExists().scrollIfNeeded().performClick()
        compose.onNodeWithText("Complete").scrollIfNeeded().performClick()
        shown("Session Summary"); compose.waitForIdle(); compose.onNodeWithText("Session Summary").assertIsDisplayed(); androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().waitForIdleSync(); capturePractice("05-summary")
        compose.onNodeWithText("Original warmup action").assertExists()
        compose.onNodeWithText("Session note (optional)").performScrollTo().performTextInput("Restart verified")
        compose.onNodeWithText("Save note").scrollIfNeeded().performClick(); shown("Note saved")
        val db = BlockDatabase.open(compose.activity.applicationContext)
        try {
            runBlocking {
                val session = db.sessions().withStatus("COMPLETED")!!
                val bundle = db.sessions().find(session.id)!!
                assertEquals(listOf("COMPLETED", "SKIPPED", "COMPLETED"), bundle.ordered.map { it.outcome })
                assertTrue(bundle.ordered.first().activeMs in 1000L..15_000L)
                assertEquals(0L, bundle.ordered[1].activeMs)
                assertTrue(bundle.ordered.last().overtimeAccepted)
                assertEquals("Saved on Back ♫", bundle.ordered.last().note)
                val source = bundle.ordered.first().sourceBlockId
                BlockRepository(db).save(source, BlockDraft("Renamed after practice", "Changed after practice"))
                BlockRepository(db).setArchived(source, true)
            }
        } finally { db.close() }
        compose.onNodeWithText("Home", substring = false).scrollIfNeeded().performClick()
        openHomeRoutines(compose); shown("Recent Practice")
    }
}

class HistoryReopenTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun savedSummaryAndOriginalSnapshotsReopenAfterRestart() {
        openHomeRoutines(compose)
        compose.waitUntil(15000) { compose.onAllNodes(hasText("2 completed · 1 skipped", substring = true)).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(hasText("2 completed · 1 skipped", substring = true)).scrollIfNeeded().performClick()
        compose.onNodeWithText("Session Summary").assertExists()
        compose.onNodeWithText("Original warmup action").assertExists()
        compose.onNodeWithText("Warmup fixture", substring = false).assertExists()
        compose.onNodeWithText("Session note (optional)").performScrollTo().assertTextContains("Restart verified")
        compose.onNodeWithText("Changed after practice").assertDoesNotExist()
    }
}

private fun capturePractice(name: String) {
    val instrumentation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
    val directory = java.io.File(instrumentation.targetContext.getExternalFilesDir(null), "screenshots").apply { mkdirs() }
    val bitmap = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
    java.io.File(directory, name + ".png").outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    bitmap.recycle()
}
