package com.mrayover.practicelogger

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class FreeRecordingWorkflowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun sheet(text: String) { if (!compose.onNodeWithText(text).isDisplayed()) compose.onNodeWithTag("take-list").performScrollToNode(hasText(text)); compose.onNodeWithText(text).performClick() }
    @Test fun menuTapStartsCaptureAndKeepsIndependentTakesThroughRecreation(): Unit {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.executeShellCommand("pm grant com.mrayover.practicelogger.app android.permission.RECORD_AUDIO")
        val db = BlockDatabase.open(compose.activity.applicationContext)
        try {
            val blocks = runBlocking { db.blocks().readAll() }
            val linked = runBlocking { db.takes().readAll().filter { it.blockId != null } }
            val vm = ViewModelProvider(compose.activity)[RecordingViewModel::class.java]
            compose.waitUntil(15000) { compose.onAllNodesWithTag("home-menu").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Recording").performScrollTo().performClick()
            captureStage3("30-cream-recording-menu")
            compose.onNodeWithText("Free recording").performScrollTo().performClick()
            compose.waitUntil(15000) { vm.recording && vm.elapsedMs >= 1500 }
            compose.onNodeWithText("Record phrase").assertDoesNotExist()
            sheet("Stop and save take")
            compose.waitUntil(15000) { !vm.busy && vm.takes.value.count { it.blockId == null } == 1 }
            sheet("Record take"); compose.waitUntil(15000) { vm.recording && vm.elapsedMs >= 1500 }
            compose.activityRule.scenario.recreate()
            compose.waitUntil(15000) { !vm.recording && !vm.busy && vm.takes.value.count { it.blockId == null } == 2 }
            val takes = runBlocking { db.takes().readAll().filter { it.blockId == null } }
            val take = takes.first()
            compose.onNodeWithTag("take-list").performScrollToNode(hasTestTag("rename-" + take.id))
            compose.onNodeWithTag("rename-" + take.id).performClick()
            compose.onNodeWithText("Take label (optional)").performTextReplacement("Standalone saved take")
            compose.onNodeWithText("Save take label").performClick()
            compose.waitUntil(10000) { vm.takes.value.any { it.label == "Standalone saved take" } }
            compose.onNodeWithTag("take-list").performScrollToNode(hasTestTag("play-" + take.id))
            compose.onNodeWithTag("play-" + take.id).performClick()
            compose.waitUntil(10000) { vm.playingId == take.id }
            compose.runOnIdle { vm.stopPlayback() }
            sheet("Close recordings")
            compose.onNodeWithText("Saved free recordings").performScrollTo().performClick()
            compose.runOnIdle { assertFalse(vm.recording) }
            compose.onNodeWithTag("take-list").performScrollToNode(hasText("Standalone saved take"))
            captureStage3("31-free-recordings-saved")
            assertEquals(blocks, runBlocking { db.blocks().readAll() })
            assertEquals(linked, runBlocking { db.takes().readAll().filter { it.blockId != null } })
            sheet("Close recordings")
        } finally { db.close() }
    }
}

class FreeRecordingReopenTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun standaloneCollectionReopensAfterColdRestartWithoutCapturing(): Unit {
        compose.waitUntil(15000) { compose.onAllNodesWithTag("home-menu").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Recording").performScrollTo().performClick()
        compose.onNodeWithText("Saved free recordings").performScrollTo().performClick()
        val vm = ViewModelProvider(compose.activity)[RecordingViewModel::class.java]
        compose.waitUntil(15000) { vm.ready && vm.takes.value.count { it.blockId == null } == 2 }
        compose.runOnIdle { assertFalse(vm.recording); assertNull(vm.playingId) }
        compose.onNodeWithTag("take-list").performScrollToNode(hasText("Standalone saved take"))
        val db = BlockDatabase.open(compose.activity.applicationContext)
        try {
            val repo = TakeRepository(db, compose.activity.applicationContext)
            runBlocking { db.takes().readAll().filter { it.blockId == null } }.forEach {
                assertTrue(it.durationMs >= 1000); assertTrue(repo.audioFile(it).length() > 0)
            }
        } finally { db.close() }
        compose.onNodeWithTag("take-list").performScrollToNode(hasText("Close recordings"))
        compose.onNodeWithText("Close recordings").performClick()
    }
}
