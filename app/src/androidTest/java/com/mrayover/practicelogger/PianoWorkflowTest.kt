package com.mrayover.practicelogger

import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class PianoWorkflowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val inst get() = InstrumentationRegistry.getInstrumentation()
    private fun click(text: String) { compose.onNodeWithText(text).scrollIfNeeded().performClick() }
    private fun shot(name: String) {
        val folder = File(inst.targetContext.getExternalFilesDir(null),"k1-native").apply { mkdirs() }
        UiDevice.getInstance(inst).takeScreenshot(File(folder,"$name.png"))
    }
    @Test fun toolsMultitouchRegistersCancelRotationAndExistingTools() {
        openHomeTools(compose); click("Reference Piano")
        compose.onNodeWithTag("piano-register").assertTextEquals("C4–C5")
        val keyboard = compose.onNodeWithTag("piano-keyboard").performScrollTo().assertIsDisplayed()
        keyboard.performTouchInput {
            down(0,Offset(width/16f,height*.85f)); down(1,Offset(width*4.5f/8,height*.85f)); down(2,Offset(width*2.5f/8,height*.85f))
        }
        compose.onNodeWithTag("piano-notes").assertTextEquals("C4 · E4 · G4")
        compose.onNodeWithTag("piano-key-60").assertIsSelected()
        shot("portrait-chord")
        keyboard.performTouchInput { moveTo(0,Offset(width*1.5f/8,height*.85f)); up(1); up(2) }
        compose.onNodeWithTag("piano-notes").assertTextEquals("D4")
        keyboard.performTouchInput { cancel() }
        compose.onNodeWithTag("piano-notes").assertTextEquals("—")
        keyboard.performTouchInput { down(Offset(width/8f,height*.2f)) }
        compose.onNodeWithTag("piano-notes").assertTextEquals("C♯4")
        keyboard.performTouchInput { up() }
        compose.onNodeWithContentDescription("Previous octave").performClick()
        compose.onNodeWithTag("piano-register").assertTextEquals("C3–C4")
        repeat(3) { compose.onNodeWithContentDescription("Previous octave").performClick() }
        compose.onNodeWithTag("piano-register").assertTextEquals("C1–C2")
        compose.onNodeWithContentDescription("Previous octave").assertIsNotEnabled()
        repeat(5) { compose.onNodeWithContentDescription("Next octave").performClick() }
        compose.onNodeWithTag("piano-register").assertTextEquals("C6–C7")
        compose.onNodeWithContentDescription("Next octave").assertIsNotEnabled()
        repeat(2) { compose.onNodeWithContentDescription("Previous octave").performClick() }
        compose.onNodeWithTag("piano-keyboard").performScrollTo().performTouchInput { down(Offset(width*.05f,height*.85f)) }
        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        compose.onNodeWithTag("piano-notes").assertTextEquals("—")
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("piano-notes").assertTextEquals("—")
        inst.runOnMainSync { compose.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
        compose.waitUntil(10000) { compose.activity.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE }
        compose.onNodeWithTag("piano-keyboard").performScrollTo().assertIsDisplayed(); shot("landscape")
        compose.onNodeWithText("Back to Tools").performScrollTo().performClick()
        inst.runOnMainSync { compose.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT }
        compose.waitUntil(10000) { compose.activity.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT }
        click("Metronome"); click("Start click")
        val tools = ViewModelProvider(compose.activity)[ToolsViewModel::class.java]
        compose.waitUntil(5000) { tools.metronomeNonZeroFramesWritten() > 100 }
        click("Back to Tools"); assertFalse(tools.clicking)
        UiDevice.getInstance(inst).executeShellCommand("pm grant com.mrayover.practicelogger.app android.permission.RECORD_AUDIO")
        click("Tuner"); click("Start tuner"); compose.waitUntil(10000) { tools.tunerFramesRead > 8192 }
        click("Back to Tools"); assertFalse(tools.listening)
        click("Timer"); compose.onNodeWithText("Stopwatch").assertExists(); click("Back to Tools")
        click("Audio Settings"); compose.onNodeWithText("Back to Tools").assertExists()
    }
}
