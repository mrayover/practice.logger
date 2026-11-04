package com.mrayover.practicelogger

import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
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
    @Test fun narrowKeyboardKeepsEdgeKeysVisibleAndPlayable() {
        val pitches = mutableListOf<Int>()
        compose.runOnIdle {
            compose.activity.setContent {
                MaterialTheme {
                    Box(Modifier.width(260.dp)) {
                        PianoKeyboard({ _, note -> pitches.add(note); Unit }, {}, {})
                    }
                }
            }
        }
        val register = compose.onNodeWithTag("piano-register")
        register.assertIsDisplayed().assertTextEquals("C4–C5")
        val distance = with(compose.density) { 38.dp.toPx() }
        compose.onNodeWithTag("piano-range-handle").performTouchInput {
            swipe(center, center - Offset(distance, 0f))
        }
        register.assertIsDisplayed().assertTextEquals("C♯4–C♯5")
        compose.onNodeWithTag("piano-key-61").assertIsDisplayed()
        compose.onNodeWithTag("piano-key-73").assertIsDisplayed()
        compose.onNodeWithTag("piano-keyboard").performTouchInput {
            click(Offset(width * .025f, height * .2f))
            click(Offset(width * .975f, height * .2f))
        }
        compose.runOnIdle { assertEquals(listOf(61, 73), pitches) }
        shot("narrow-sharp-edges")
    }

    @Test fun toolsMultitouchRegistersCancelRotationAndExistingTools() {
        openHomeTools(compose); click("Reference Piano")
        val register = compose.onNodeWithTag("piano-register")
        val handle = compose.onNodeWithTag("piano-range-handle")
        val previous = compose.onNodeWithContentDescription("Previous octave")
        val next = compose.onNodeWithContentDescription("Next octave")
        register.assertTextEquals("C4–E5")
        val keyboard = compose.onNodeWithTag("piano-keyboard").performScrollTo().assertIsDisplayed()
        keyboard.performTouchInput {
            down(0,Offset(width*.05f,height*.85f)); down(1,Offset(width*.45f,height*.85f)); down(2,Offset(width*.25f,height*.85f))
        }
        compose.onNodeWithTag("piano-notes").assertTextEquals("C4 · E4 · G4")
        compose.onNodeWithTag("piano-key-60").assertIsSelected()
        shot("portrait-chord")
        keyboard.performTouchInput { moveTo(0,Offset(width*.15f,height*.85f)); up(1); up(2) }
        compose.onNodeWithTag("piano-notes").assertTextEquals("D4")
        keyboard.performTouchInput { cancel() }
        compose.onNodeWithTag("piano-notes").assertTextEquals("—")
        keyboard.performTouchInput { down(Offset(width*.1f,height*.2f)) }
        compose.onNodeWithTag("piano-notes").assertTextEquals("C♯4")
        previous.performSemanticsAction(SemanticsActions.OnClick) { it() }
        register.assertTextEquals("C3–E4")
        compose.onNodeWithTag("piano-notes").assertTextEquals("—")
        keyboard.performTouchInput { moveTo(Offset(width*.2f,height*.85f)); up() }
        compose.onNodeWithTag("piano-notes").assertTextEquals("—")
        next.performClick()
        val dragDistance = with(compose.density) { 38.dp.toPx() }
        handle.performTouchInput { swipe(center, center - Offset(dragDistance, 0f)) }
        register.assertTextEquals("C♯4–F5")
        keyboard.performTouchInput { down(Offset(width*.025f,height*.2f)) }
        compose.onNodeWithTag("piano-notes").assertTextEquals("C♯4")
        keyboard.performTouchInput { up() }
        next.performClick()
        register.assertTextEquals("C♯5–F6")
        previous.performClick()
        register.assertTextEquals("C♯4–F5")
        handle.performTouchInput { swipe(center, center + Offset(dragDistance, 0f)) }
        register.assertTextEquals("C4–E5")
        repeat(3) { previous.performClick() }
        register.assertTextEquals("C1–E2")
        previous.assertIsNotEnabled()
        handle.performTouchInput { swipe(center, center + Offset(dragDistance, 0f)) }
        register.assertTextEquals("C1–E2")
        repeat(4) { next.performClick() }
        next.assertIsNotEnabled()
        repeat(8) {
            val actions = handle.fetchSemanticsNode().config[SemanticsActions.CustomActions]
            compose.runOnIdle { assertTrue(actions.first { it.label == "Shift up one semitone" }.action()) }
        }
        register.assertTextEquals("G♯5–C7")
        handle.performTouchInput { swipe(center, center - Offset(dragDistance, 0f)) }
        register.assertTextEquals("G♯5–C7")
        next.assertIsNotEnabled()
        repeat(2) { previous.performClick() }
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
        click("Back to Tools"); compose.runOnIdle { assertFalse(tools.clicking) }
        UiDevice.getInstance(inst).executeShellCommand("pm grant com.mrayover.practicelogger.app android.permission.RECORD_AUDIO")
        click("Tuner"); click("Start tuner"); compose.waitUntil(10000) { tools.tunerFramesRead > 8192 }
        click("Back to Tools"); compose.runOnIdle { assertFalse(tools.listening) }
        click("Timer"); compose.onNodeWithText("Stopwatch").assertExists(); click("Back to Tools")
        click("Audio Settings"); compose.onNodeWithText("Back to Tools").assertExists()
    }
}
