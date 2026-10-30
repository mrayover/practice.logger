package com.mrayover.practicelogger

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.semantics.SemanticsActions
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ToolsWorkflowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun click(text: String) { compose.onNodeWithText(text).scrollIfNeeded().performClick() }
    private fun banner() { compose.onNodeWithTag("home-brand", useUnmergedTree = true).assertIsDisplayed() }
    @Test fun standaloneToolsRunAndStopOnExitWithoutChangingPractice() {
        val vm = ViewModelProvider(compose.activity)[ToolsViewModel::class.java]
        openHomeTools(compose); banner(); captureStage3("20-tools-menu")
        click("Metronome"); banner()
        compose.onNodeWithTag("tool-bpm").assertTextEquals("120")
        compose.onNodeWithContentDescription("Increase BPM by 1").performClick()
        compose.onNodeWithContentDescription("Increase BPM by 5").performClick()
        compose.onNodeWithTag("tool-bpm").assertTextEquals("126")
        compose.onNodeWithContentDescription("Decrease BPM by 1").performClick()
        compose.onNodeWithContentDescription("Decrease BPM by 5").performClick()
        compose.onNodeWithTag("tool-bpm").assertTextEquals("120")
        click("Start click"); compose.waitUntil(5000) { vm.clicking }
        compose.onNodeWithText("Stop click").assertExists()
        compose.runOnIdle { assertTrue(vm.clicking) }
        captureStage3("21-tool-metronome")
        compose.activityRule.scenario.recreate()
        compose.waitUntil(5000) { !vm.clicking }; banner()
        click("Start click"); click("Back to Tools"); compose.runOnIdle { assertFalse(vm.clicking) }
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.executeShellCommand("pm grant com.mrayover.practicelogger.app android.permission.RECORD_AUDIO")
        click("Tuner"); banner(); click("Start tuner")
        compose.waitUntil(10000) { vm.listening && vm.tunerFramesRead >= 8192 }
        captureStage3("22-tool-tuner")
        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        assertFalse(vm.listening)
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        compose.onNodeWithText("Start tuner").assertExists()
        click("Back to Tools"); click("Timer"); banner()
        click("Start stopwatch"); compose.waitUntil(5000) { vm.stopwatchMs >= 1000 }
        click("Stop stopwatch"); val elapsed = vm.stopwatchMs; assertTrue(elapsed >= 1000)
        compose.onNodeWithTag("minute-wheel").performScrollTo().performTouchInput { swipeUp(durationMillis = 500) }
        compose.waitUntil(5000) { vm.minutes != 5 }
        compose.onNodeWithTag("minute-wheel").performSemanticsAction(SemanticsActions.SetProgress) { it(1f) }
        compose.waitUntil(5000) { vm.minutes == 1 }
        click("Start timer"); compose.waitUntil(5000) { vm.remainingMs < 59500 }
        assertTrue(vm.countdownRunning)
        captureStage3("23-tool-timer")
        compose.waitUntil(65000) { vm.remainingMs == 0L && !vm.countdownRunning }
        compose.onNodeWithText("Time’s up").performScrollTo().assertIsDisplayed()
        captureStage3("24-tool-timer-complete")
        click("Start timer"); compose.waitUntil(5000) { vm.remainingMs < 59500 }
        click("Back to Tools"); compose.runOnIdle {
            assertFalse(vm.countdownRunning); assertFalse(vm.stopwatchRunning); assertEquals(elapsed, vm.stopwatchMs)
        }
        val pausedRemaining = vm.remainingMs
        click("Timer"); compose.runOnIdle { assertEquals(pausedRemaining, vm.remainingMs) }
        compose.activityRule.scenario.recreate()
        compose.runOnIdle { assertEquals(pausedRemaining, vm.remainingMs); assertFalse(vm.countdownRunning) }
        click("Reset stopwatch"); assertEquals(0, vm.stopwatchMs)
        click("Reset timer"); assertEquals(60000, vm.remainingMs)
        click("Back to Tools"); click("Back to Home"); banner()
        openHomeRoutines(compose)
        compose.onNodeWithText("Unfinished sessions").assertDoesNotExist()
        click("Back to Home")
    }
}
