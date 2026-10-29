package com.mrayover.practicelogger

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.regex.Pattern

private const val AUDIO_EXPORT = "practice-logger-free-take.m4a"

class RecordingWorkflowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val device get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private fun shown(text: String) { compose.waitUntil(15000) { compose.onAllNodesWithText(text, substring = true).fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty() } }
    private fun click(text: String) {
        val node = compose.onNodeWithText(text, substring = false)
        try { node.assertIsDisplayed() } catch (_: AssertionError) { node.performScrollTo() }
        node.performClick()
    }
    private fun sheet(text: String) { if (text !in setOf("Stop and save take", "Cancel phrase")) compose.onNodeWithTag("take-list").performScrollToNode(hasText(text)); compose.onNodeWithText(text, substring = false).performClick() }
    private fun takeAction(prefix: String, take: BlockTake) {
        val tag = prefix + "-" + take.id
        compose.onNodeWithTag("take-list").performScrollToNode(hasTestTag(tag)); compose.onNodeWithTag(tag).performClick()
    }
    @Test fun nativeFreeTakesPermissionLifecyclePlaybackExportAndSafeDelete() {
        val db = BlockDatabase.open(compose.activity.applicationContext)
        try {
            val block = runBlocking { BlockRepository(db).save(null, BlockDraft("Recorder fixture", "Play and listen", "5", "120")) }
            shown("Practice Now"); click("Practice Now"); shown("Add Recorder fixture"); click("Add Recorder fixture")
            click("Start Session"); shown("Ready when you are"); click("Recordings"); shown("Block recordings")
            val vm = ViewModelProvider(compose.activity)[RecordingViewModel::class.java]
            val practice = ViewModelProvider(compose.activity)[PracticeViewModel::class.java]
            compose.waitUntil(10000) { vm.ready }
            assertEquals(PackageManager.PERMISSION_DENIED, compose.activity.checkSelfPermission(Manifest.permission.RECORD_AUDIO))
            sheet("Record take")
            var deny = device.wait(Until.findObject(By.res(Pattern.compile(".*:id/permission_deny_button"))), 10000)
                ?: device.findObject(By.text(Pattern.compile("(?i).*don.t allow.*|.*deny.*")))
            // Some API26 emulator images crash System UI during a permission transition.
            // Recover that system process once, retaining logs and every permission/data assertion.
            if (deny == null && device.hasObject(By.text("System UI has stopped"))) {
                val crash = device.executeShellCommand("logcat -b crash -d")
                println("SYSTEM UI CRASH: $crash")
                checkNotNull(device.findObject(By.res("android:id/aerr_close"))).click()
                deny = device.wait(Until.findObject(By.res(Pattern.compile(".*:id/permission_deny_button"))), 10000)
                    ?: device.findObject(By.text(Pattern.compile("(?i).*don.t allow.*|.*deny.*")))
            }
            if (deny != null) {
                val denySelector = By.res(Pattern.compile(".*:id/permission_deny_button"))
                deny.click()
                // Android can expose the button before its dialog accepts touch input.
                // Reacquire after the transition, and require dismissal before checking the app.
                if (!device.wait(Until.gone(denySelector), 2000)) {
                    checkNotNull(device.findObject(denySelector)).click()
                    assertTrue("Microphone denial dialog did not dismiss", device.wait(Until.gone(denySelector), 10000))
                }
            } // Repeated denied requests may also be refused without displaying a dialog.
            shown("Microphone permission was not allowed.")
            compose.runOnIdle { assertFalse(vm.recording) }
            assertTrue(runBlocking { db.takes().readAll() }.isEmpty())
            device.executeShellCommand("pm grant com.mrayover.practicelogger.app android.permission.RECORD_AUDIO")
            sheet("Close recordings"); click("Start"); shown("Practicing"); click("Start click"); shown("Stop click")
            click("Recordings"); shown("Block recordings")
            // Emulator capture exercises the real encoder/container path, not perceived microphone quality.
            repeat(3) { index ->
                sheet("Record take"); shown("Stop and save take")
                compose.waitUntil(10000) { vm.elapsedMs >= 1500L }
                if (index == 0) {
                    sheet("Pause timer"); compose.runOnIdle { assertTrue(vm.recording); assertFalse(practice.running) }
                    sheet("Resume timer"); compose.waitUntil(10000) { practice.running }
                    compose.runOnIdle { assertTrue(vm.recording); assertTrue(practice.running) }
                }
                if (index == 2) {
                    compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
                    compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
                } else sheet("Stop and save take")
                compose.waitUntil(15000) { !vm.busy && vm.takes.value.count { it.blockId == block } == index + 1 }
                compose.runOnIdle { assertFalse(vm.recording) }
                if (index < 2) compose.runOnIdle { assertTrue(practice.running); assertTrue(practice.clicking) }
            }
            val takes = runBlocking { db.takes().readAll().filter { it.blockId == block }.sortedBy { it.createdAt } }
            assertEquals(3, takes.size); assertTrue(takes.all { it.durationMs >= 1000 && it.mode == "FREE" })
            val repository = TakeRepository(db, compose.activity.applicationContext)
            takes.forEach { assertTrue(repository.audioFile(it).length() > 0) }
            takeAction("play", takes[0]); compose.waitUntil(10000) { vm.playingId == takes[0].id }
            takeAction("play", takes[1]); compose.waitUntil(10000) { vm.playingId == takes[1].id }
            compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
            compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
            compose.runOnIdle { assertNull(vm.playingId); assertNull(vm.preparingId); assertFalse(vm.recording) }
            takeAction("rename", takes[0]); shown("Take label (optional)")
            compose.onNodeWithText("Take label (optional)").performTextReplacement("First free take")
            compose.onNodeWithText("Save take label").performClick()
            compose.waitUntil(10000) { vm.takes.value.any { it.id == takes[0].id && it.label == "First free take" } }
            takeAction("delete", takes[2]); shown("Delete this take?"); compose.onNodeWithText("Keep take").performClick()
            assertEquals(3, runBlocking { db.takes().readAll().size })
            takeAction("delete", takes[2]); compose.onNodeWithText("Delete take permanently").performClick()
            compose.waitUntil(10000) { !vm.busy && vm.takes.value.size == 2 }
            assertFalse(java.io.File(compose.activity.filesDir, "recordings/" + takes[2].fileName).exists())
            takeAction("export", takes[0])
            checkNotNull(device.wait(Until.findObject(By.desc("Show roots")), 10000)).click()
            checkNotNull(device.wait(Until.findObject(By.text("Downloads")), 10000))
            device.findObjects(By.text("Downloads")).last().click()
            checkNotNull(device.wait(Until.findObject(By.clazz("android.widget.EditText")), 10000)).text = AUDIO_EXPORT
            checkNotNull(device.wait(Until.findObject(By.text(Pattern.compile("(?i)save"))), 10000)).click()
            shown("Audio exported.")
            val exported = device.executeShellCommand("stat -c %s /sdcard/Download/$AUDIO_EXPORT").trim().toLong()
            assertEquals(repository.audioFile(takes[0]).length(), exported)
            // An unavailable destination must keep the original take intact.
            compose.runOnIdle { vm.export(takes[0], android.net.Uri.parse("content://not.a.provider/missing")) }
            shown("Export failed."); assertTrue(repository.audioFile(takes[0]).exists())
            compose.runOnIdle { assertFalse(practice.running) } // Explicit backgrounding paused the timer, not recording controls.
            sheet("Close recordings"); click("Resume"); shown("Practicing"); click("Recordings")
            takeAction("play", takes[0]); compose.waitUntil(10000) { vm.playingId == takes[0].id }
            sheet("Close recordings"); compose.runOnIdle { assertNull(vm.playingId); assertTrue(practice.running) }
            click("Save for later"); shown("Practice Now")
            runBlocking {
                BlockRepository(db).save(block, BlockDraft("Recorder renamed", "Still the same Block"))
                BlockRepository(db).setArchived(block, true)
            }
            click("Blocks"); click("Archived"); shown("Recorder renamed"); compose.onNodeWithText("Recorder renamed").performClick()
            shown("Edit Block"); click("Recordings"); shown("Block recordings")
            compose.onNodeWithTag("take-list").performScrollToNode(hasText("First free take")); compose.onNodeWithText("First free take").assertExists()
            assertEquals(2, runBlocking { db.takes().readAll().count { it.blockId == block } })
            captureStage3("20-free-takes")
            sheet("Close recordings"); click("Cancel"); click("Back to Home"); shown("Practice Now")
        } catch (failure: Throwable) {
            val tree = java.io.ByteArrayOutputStream(); device.dumpWindowHierarchy(tree); println(tree.toString("UTF-8"))
            captureStage3("recording-failure"); throw AssertionError("Recorder workflow failed. UI: " + tree.toString("UTF-8"), failure)
        } finally { db.close() }
    }
}

class RecordingReopenTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun click(text: String) {
        val node = compose.onNodeWithText(text, substring = false)
        try { node.assertIsDisplayed() } catch (_: AssertionError) { node.performScrollTo() }
        node.performClick()
    }
    @Test fun savedTakesSurviveProcessRestartAndArchiveWithNoAutomaticAudio() {
        compose.waitUntil(15000) { compose.onAllNodesWithText("Practice Now").fetchSemanticsNodes().isNotEmpty() }
        val db = BlockDatabase.open(compose.activity.applicationContext)
        try {
            val block = runBlocking { db.blocks().readAll().single { it.name == "Recorder renamed" } }
            val takes = runBlocking { db.takes().readAll().filter { it.blockId == block.id } }
            assertTrue(block.archived); assertEquals(2, takes.size)
            val repository = TakeRepository(db, compose.activity.applicationContext)
            takes.forEach { assertTrue(repository.audioFile(it).length() > 0) }
            click("Blocks"); click("Archived"); compose.onNodeWithText("Recorder renamed").performClick(); click("Recordings")
            val vm = ViewModelProvider(compose.activity)[RecordingViewModel::class.java]
            compose.runOnIdle { assertFalse(vm.recording); assertNull(vm.playingId) }
            val first = takes.single { it.label == "First free take" }
            compose.onNodeWithTag("take-list").performScrollToNode(hasTestTag("play-" + first.id))
            compose.onNodeWithTag("play-" + first.id).performClick()
            compose.waitUntil(10000) { vm.playingId == first.id }
            compose.activityRule.scenario.recreate()
            compose.runOnIdle { assertFalse(vm.recording); assertNull(vm.playingId) }
            compose.onNodeWithTag("take-list").performScrollToNode(hasText("Close recordings")); compose.onNodeWithText("Close recordings").performClick()
            click("Cancel"); click("Back to Home")
        } finally { db.close() }
    }
}
