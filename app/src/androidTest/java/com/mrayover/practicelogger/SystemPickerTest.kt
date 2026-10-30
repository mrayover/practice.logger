package com.mrayover.practicelogger

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.regex.Pattern

private const val PICKER_FILE = "practice-logger-picker.json"
private val device get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
private fun shownPickerApp(compose: androidx.compose.ui.test.junit4.AndroidComposeTestRule<*, MainActivity>, text: String) {
    compose.waitUntil(15000) { compose.onAllNodesWithText(text, substring = true).fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty() }
}
private fun pickerDownloads() {
    val roots = device.wait(Until.findObject(By.desc("Show roots")), 10000)
        ?: error("Document picker did not show navigation")
    roots.click()
    assertTrue(device.wait(Until.hasObject(By.text("Downloads")), 10000))
    device.findObjects(By.text("Downloads")).last().click()
    device.waitForIdle()
}
private fun pickerFailureEvidence() {
    val out = java.io.ByteArrayOutputStream()
    device.dumpWindowHierarchy(out)
    println("PICKER FAILURE HIERARCHY: " + out.toString("UTF-8"))
    captureStage3("picker-failure")
}
private fun cancelPicker() {
    repeat(3) {
        device.pressBack()
        if (device.wait(Until.gone(By.pkg(Pattern.compile(".*documentsui"))), 1500)) return
    }
    error("Could not cancel document picker")
}
fun captureStage3(name: String) {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val directory = java.io.File(context.getExternalFilesDir(null), "screenshots").apply { mkdirs() }
    device.waitForIdle()
    device.takeScreenshot(java.io.File(directory, name + ".png"))
}

class SystemPickerExportTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun exportAndCancelThroughAndroidDocumentsUi() {
        val db = BlockDatabase.open(compose.activity.applicationContext)
        try {
            runBlocking {
                val id = BlockRepository(db).save(null, BlockDraft("Picker recovery", "Retain original action", "2", "95", "Reference"))
                val repo = SessionRepository(db)
                repo.addBlock(id); repo.addBlock(id)
                val session = repo.start(); repo.checkpoint(session, 1234); repo.finishBlock(session, "SKIPPED", "first note")
                repo.checkpoint(session, 500)
                val goalRepo = GoalRepository(db)
                val goalId = goalRepo.save(null, "Picker Goal", "Memorize", 42, "Goal note")
                goalRepo.setBlocks(goalId, listOf(id))
                db.goals().insert(GoalSession(session, goalId))
                goalRepo.setCompleted(goalId, true)
                RoutineRepository(db).save(null, "Picker saved routine", listOf(RoutineSelection(id, 180_000), RoutineSelection(id, 60_000)))
                BlockRepository(db).setArchived(id, true)
            }
            openHomeTools(compose)
            shownPickerApp(compose, "Backup and restore")
            compose.onNodeWithText("Backup and restore").scrollIfNeeded().performClick()
            compose.onNodeWithText("Export backup").scrollIfNeeded().performClick()
            pickerDownloads()
            val filename = checkNotNull(device.wait(Until.findObject(By.clazz("android.widget.EditText")), 10000))
            filename.text = PICKER_FILE
            checkNotNull(device.wait(Until.findObject(By.text(Pattern.compile("SAVE", Pattern.CASE_INSENSITIVE))), 10000)).click()
            shownPickerApp(compose, "Backup saved.")
            val exported = BackupCodec.decode(device.executeShellCommand("cat /sdcard/Download/$PICKER_FILE").toByteArray(Charsets.UTF_8))
            runBlocking { assertEquals(BackupRepository(db).snapshot(), exported) }
            assertEquals(1, exported.sessions.count { it.status == "IN_PROGRESS" })
            assertEquals("Picker Goal", exported.goals.single().title)
            assertEquals(42, exported.goals.single().progress)
            assertTrue(exported.goals.single().completed)
            assertEquals(exported.goals.single().id, exported.sessionGoals.single().goalId)
            assertEquals(1, exported.goalBlocks.size)
            assertEquals("Picker saved routine", exported.routines.single().name)
            assertEquals(listOf(180_000L, 60_000L), exported.routinePlacements.map { it.configuredMs })
            compose.onNodeWithText("Choose backup to restore").scrollIfNeeded().performClick()
            pickerDownloads()
            cancelPicker()
            shownPickerApp(compose, "Export backup")
            compose.onNodeWithText("Restore this backup?").assertDoesNotExist()
            compose.onNodeWithText("Export backup").scrollIfNeeded().performClick()
            pickerDownloads(); cancelPicker()
            shownPickerApp(compose, "Export backup")
            runBlocking { assertEquals(exported, BackupRepository(db).snapshot()) }
            captureStage3("07-backup-picker-return")
        } catch (e: Throwable) { pickerFailureEvidence(); throw e }
        finally { db.close() }
    }
}

/** CI clears only its disposable emulator app between export and restore, never owner data. */
class SystemPickerRestoreTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun restoreIntoFreshAppThroughAndroidDocumentsUi() {
        val db = BlockDatabase.open(compose.activity.applicationContext)
        try {
            val expected = BackupCodec.decode(device.executeShellCommand("cat /sdcard/Download/$PICKER_FILE").toByteArray(Charsets.UTF_8))
            runBlocking { assertTrue(BackupRepository(db).snapshot().blocks.isEmpty()) }
            openHomeTools(compose)
            shownPickerApp(compose, "Backup and restore")
            compose.onNodeWithText("Backup and restore").scrollIfNeeded().performClick()
            compose.onNodeWithText("Choose backup to restore").scrollIfNeeded().performClick()
            pickerDownloads()
            checkNotNull(device.wait(Until.findObject(By.text(PICKER_FILE)), 10000)).click()
            shownPickerApp(compose, "Restore this backup?")
            compose.onNodeWithText("Cancel").performClick()
            runBlocking { assertTrue(BackupRepository(db).snapshot().blocks.isEmpty()) }
            compose.onNodeWithText("Choose backup to restore").scrollIfNeeded().performClick()
            pickerDownloads(); checkNotNull(device.wait(Until.findObject(By.text(PICKER_FILE)), 10000)).click()
            shownPickerApp(compose, "Restore this backup?")
            compose.activityRule.scenario.recreate(); shownPickerApp(compose, "Restore this backup?")
            compose.onNodeWithText("Restore backup").performClick()
            shownPickerApp(compose, "Backup restored.")
            runBlocking { assertEquals(expected, BackupRepository(db).snapshot()) }
            compose.onNodeWithTag("home-brand").scrollIfNeeded().performClick()
            openHomePractice(compose)
            shownPickerApp(compose, "Continue Session")
            compose.onNodeWithText("Continue Session").scrollIfNeeded().performClick()
            shownPickerApp(compose, "Picker recovery")
            compose.onNodeWithText("Resume").assertExists()
            compose.onNodeWithText("Block 2 of 2").assertExists()
            runBlocking {
                val bundle = db.sessions().find(expected.sessions.single { it.status == "IN_PROGRESS" }.id)!!
                assertEquals(500L, bundle.current!!.activeMs)
                assertEquals("first note", bundle.ordered[0].note)
            }
            captureStage3("08-restored-paused")
            leavePracticeForLater(compose)
        } catch (e: Throwable) { pickerFailureEvidence(); throw e }
        finally { db.close() }
    }
}

class SystemPickerReopenTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun restoredPracticeAndHistorySurviveProcessRestart() {
        val db = BlockDatabase.open(compose.activity.applicationContext)
        try {
            val expected = BackupCodec.decode(device.executeShellCommand("cat /sdcard/Download/$PICKER_FILE").toByteArray(Charsets.UTF_8))
            runBlocking { assertEquals(expected, BackupRepository(db).snapshot()) }
            openHomePractice(compose)
            shownPickerApp(compose, "Continue Session")
            compose.onNodeWithText("Continue Session").scrollIfNeeded().performClick()
            shownPickerApp(compose, "Picker recovery")
            compose.onNodeWithText("Resume").assertExists()
            compose.onNodeWithText("Complete").scrollIfNeeded().performClick()
            shownPickerApp(compose, "Session Summary")
            compose.onNodeWithText("Session Summary").assertIsDisplayed()
            captureStage3("09-restored-summary")
        } finally { db.close() }
    }
}
