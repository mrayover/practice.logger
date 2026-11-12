package com.mrayover.practicelogger

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
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
    private fun hideKeyboard() {
        // Dialog input owns a separate window; dismiss the visible IME through Android Back.
        val view = compose.activity.window.decorView
        if (androidx.core.view.ViewCompat.getRootWindowInsets(view)?.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime()) == true) {
            UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressBack()
        }
        try {
            compose.waitUntil(5000) { androidx.core.view.ViewCompat.getRootWindowInsets(view)?.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime()) != true }
        } catch (failure: Throwable) {
            val folder = File(compose.activity.getExternalFilesDir(null), "home-preview").apply { mkdirs() }
            UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).takeScreenshot(File(folder, "w2-input-failure.png"))
            File(folder, "w2-input-semantics.txt").writeText(compose.onRoot(useUnmergedTree = true).printToString())
            throw failure
        }
    }
    private fun home() { compose.onNodeWithTag("home-brand", useUnmergedTree = true).performClick(); waitFor("home-weekly-target") }
    private fun save(text: String) {
        compose.onNodeWithTag("weekly-target-input").performTextReplacement(text)
        hideKeyboard()
        compose.onNodeWithTag("save-weekly-target").assertIsDisplayed().assertIsEnabled().performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithTag("weekly-target-input").fetchSemanticsNodes().isEmpty() }
    }
    @Test fun liveHomeWeekEditingHistoryRecreationAndCumulativeStatistics() {
        waitFor("home-weekly-target")
        val week = practiceWeek()
        val start = LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        runBlocking {
            val block = BlockRepository(db).save(null, BlockDraft("Weekly practice", "Play"))
            db.sessions().insert(PracticeSession("weekly-preview", start, status = "IN_PROGRESS"))
            db.sessions().insert(Placement("weekly-placement", "weekly-preview", block, "Weekly practice", "Play", null, "", 0, 60_000, 90 * 60_000L))
            db.weeklyTargets().save(WeeklyPracticeTarget(week.minusWeeks(1).toString(), 60))
        }
        clickTag("home-weekly-target")
        waitFor("edit-weekly-target")
        compose.onNodeWithText("90 min practiced").assertIsDisplayed()
        clickTag("edit-weekly-target")
        compose.onNodeWithTag("weekly-target-input").performTextReplacement("0")
        compose.onNodeWithTag("save-weekly-target").assertIsNotEnabled()
        save("180")
        compose.onNodeWithText("50% of target · 90 minutes remaining").assertIsDisplayed()
        home()
        compose.onNodeWithText("50% of target · 90 minutes remaining").performScrollTo().assertIsDisplayed()
        val folder = File(compose.activity.getExternalFilesDir(null), "home-preview").apply { mkdirs() }
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.takeScreenshot(File(folder, "w2-home.png"))
        clickTag("home-statistics")
        waitFor("statistics-screen")
        device.pressBack(); waitFor("home-weekly-target")
        clickTag("home-weekly-target"); clickTag("edit-weekly-target"); save("60")
        compose.onNodeWithText("150% of target · 30 minutes extra").assertIsDisplayed()
        compose.onNodeWithText("Previous week").performClick()
        compose.onNodeWithText("0 / 60 min planned").assertIsDisplayed()
        compose.onNodeWithTag("edit-weekly-target").assertDoesNotExist()
        compose.onNodeWithText("Next week").performClick()
        compose.onNodeWithText("150% of target · 30 minutes extra").assertIsDisplayed()
        compose.onNodeWithText("Next week").performClick()
        clickTag("edit-weekly-target"); save("120")
        compose.onNodeWithText("Previous week").performClick()
        compose.onNodeWithText("90 / 60 min planned").assertIsDisplayed()
        device.takeScreenshot(File(folder, "w2-week.png"))
        compose.activityRule.scenario.recreate()
        waitFor("edit-weekly-target")
        compose.onNodeWithText("90 / 60 min planned").assertIsDisplayed()
        clickTag("edit-weekly-target"); clickTag("clear-weekly-target")
        compose.waitUntil(15000) { runBlocking { db.weeklyTargets().find(week.toString()) == null } }
        home()
        compose.onNodeWithText("90 min practiced").performScrollTo().assertIsDisplayed()
        assertEquals(60, runBlocking { db.weeklyTargets().find(week.minusWeeks(1).toString())!!.targetMinutes })
        assertEquals(120, runBlocking { db.weeklyTargets().find(week.plusWeeks(1).toString())!!.targetMinutes })
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
