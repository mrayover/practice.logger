package com.mrayover.practicelogger

import android.Manifest
import android.content.res.Configuration
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.After
import org.junit.Rule
import org.junit.Test
import java.io.File

class ThemeWorkflowTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private val device get()=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private fun shown(text: String) = compose.waitUntil(15000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    private fun click(text: String) { shown(text); compose.onNodeWithText(text).scrollIfNeeded().performClick() }
    private fun tag(name: String)=compose.onNodeWithTag(name).scrollIfNeeded().assertIsDisplayed()
    private fun choose(choice: ThemeChoice) { compose.runOnIdle { compose.activity.themePreferences.save(choice) }; compose.waitForIdle() }
    private fun shot(name: String) {
        val dir=File(compose.activity.getExternalFilesDir(null),"theme").apply { mkdirs() }
        compose.waitForIdle(); device.takeScreenshot(File(dir,"$name.png"))
    }
    private fun background(palette: PracticePalette) {
        compose.waitForIdle()
        val pixels=compose.onNodeWithTag("app-surface").captureToImage().toPixelMap()
        val expected=palette.scheme.background
        var count=0;var total=0
        for(y in 0 until pixels.height step 10) for(x in 0 until pixels.width step 10) {
            val c=pixels[x,y];total++
            if(kotlin.math.abs(c.red-expected.red)<.02f && kotlin.math.abs(c.green-expected.green)<.02f && kotlin.math.abs(c.blue-expected.blue)<.02f) count++
        }
        assertTrue("${palette.id} background not rendered ($count/$total)", count>total*.15)
        compose.runOnIdle {
            assertEquals(palette.id==LightPalette.id,WindowCompat.getInsetsController(compose.activity.window,compose.activity.window.decorView).isAppearanceLightNavigationBars)
            assertFalse(WindowCompat.getInsetsController(compose.activity.window,compose.activity.window.decorView).isAppearanceLightStatusBars)
        }
    }
    @After fun resetAppearance() {
        device.executeShellCommand("settings put system font_scale 1.0")
        device.executeShellCommand("cmd uimode night no")
        compose.runOnIdle { compose.activity.requestedOrientation=android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
        choose(ThemeChoice.LIGHT)
    }
    @Test fun homeSettingsBelowLogPersistsAndSystemFollowsDevice() {
        shown("Practice Now");choose(ThemeChoice.LIGHT);background(LightPalette)
        val settings=compose.onNodeWithText("Settings").scrollIfNeeded().assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val log=compose.onNodeWithText("Log").fetchSemanticsNode().boundsInRoot
        assertTrue("Settings must follow Log",settings.top>=log.bottom)
        shot("home-light");click("Settings")
        tag("theme-dark").performClick()
        compose.waitUntil(10000) { compose.activity.themePreferences.choice.value==ThemeChoice.DARK }
        tag("theme-dark").assertIsSelected();background(DarkPalette);shot("settings-dark")
        assertEquals(ThemeChoice.DARK,ThemePreferences(compose.activity.application).choice.value)
        compose.activityRule.scenario.recreate();compose.waitForIdle()
        tag("theme-dark").assertIsSelected();background(DarkPalette)
        device.executeShellCommand("cmd uimode night yes")
        compose.waitUntil(15000) { compose.activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK==Configuration.UI_MODE_NIGHT_YES }
        // Explicit Light must override a dark OS; System then follows it.
        tag("theme-light").performClick();compose.waitUntil(10000) { compose.activity.themePreferences.choice.value==ThemeChoice.LIGHT };background(LightPalette)
        tag("theme-system").performClick();compose.waitUntil(10000) { compose.activity.themePreferences.choice.value==ThemeChoice.SYSTEM };background(DarkPalette)
        device.executeShellCommand("cmd uimode night no")
        compose.waitUntil(15000) { compose.activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK==Configuration.UI_MODE_NIGHT_NO }
        compose.waitForIdle();background(LightPalette)
        choose(ThemeChoice.DARK);compose.onNodeWithTag("home-brand").performClick();shown("Practice Now");background(DarkPalette);shot("home-dark")
    }
    @Test fun paletteRecompositionKeepsPracticeCaptureReviewAndPdfContent() {
        val app=compose.activity.application; val db=BlockDatabase.open(app)
        val practice=ViewModelProvider(compose.activity)[PracticeViewModel::class.java]
        val audio=ViewModelProvider(compose.activity)[RecordingViewModel::class.java]
        try {
            device.executeShellCommand("pm grant ${app.packageName} ${Manifest.permission.RECORD_AUDIO}")
            val (block,session)=runBlocking {
                val b=BlockRepository(db).save(null,BlockDraft("Theme recording","Keep the current workspace.","5","120"))
                ImageReferenceRepository(db,File(app.filesDir,"block-images")).import(b) {
                    InstrumentationRegistry.getInstrumentation().context.assets.open("references/score.pdf")
                }
                val repo=SessionRepository(db);repo.addBlock(b);b to repo.start()
            }
            val media=File(app.filesDir,"block-images").walkTopDown().filter { it.isFile }.associate { it.name to it.readBytes().contentHashCode() }
            shown("Practice Now");compose.runOnIdle { practice.openSession(session) };shown("Practice Description")
            compose.waitUntil(15000) { audio.ready }
            compose.runOnIdle { practice.resume() };compose.waitUntil(10000) { practice.running && !practice.busy }
            tag("practice-reference-toggle").performClick();compose.waitUntil(10000) { compose.onAllNodesWithTag("reference-viewer").fetchSemanticsNodes().isNotEmpty() }
            val page=compose.onNodeWithTag("reference-page").fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.Text]
            choose(ThemeChoice.DARK)
            assertEquals(page,compose.onNodeWithTag("reference-page").fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.Text])
            tag("reference-viewer").performTouchInput { pinch(androidx.compose.ui.geometry.Offset(width*.4f,height*.4f),androidx.compose.ui.geometry.Offset(width*.6f,height*.6f),androidx.compose.ui.geometry.Offset(width*.15f,height*.15f),androidx.compose.ui.geometry.Offset(width*.85f,height*.85f)) }
            choose(ThemeChoice.LIGHT)
            compose.onNodeWithText("Reset PDF zoom").assertExists()
            compose.runOnIdle { audio.startCapture(block) };compose.waitUntil(10000) { audio.recording && audio.elapsedMs>500 }
            val elapsed=audio.elapsedMs
            choose(ThemeChoice.DARK)
            compose.runOnIdle { assertTrue(audio.recording);assertTrue(practice.running);assertNull(audio.reviewTake);assertTrue(audio.elapsedMs>=elapsed) }
            tag("practice-recording-stop").assertIsDisplayed();shot("capture-dark")
            compose.runOnIdle { audio.finishCapture() };compose.waitUntil(15000) { !audio.busy && audio.reviewTake!=null }
            val pending=audio.reviewTake!!
            choose(ThemeChoice.LIGHT);compose.runOnIdle { assertEquals(pending.id,audio.reviewTake?.id);assertNull(audio.playingId) }
            compose.runOnIdle { audio.previewReview() };compose.waitUntil(10000) { !audio.busy && audio.playingId==pending.id }
            choose(ThemeChoice.DARK);compose.runOnIdle { assertEquals(pending.id,audio.playingId);assertEquals(pending.id,audio.reviewTake?.id) }
            assertNull(runBlocking { db.takes().find(pending.id) })
            compose.runOnIdle { audio.stopPlayback();audio.discardReview();practice.pause() }
            assertEquals(media,File(app.filesDir,"block-images").walkTopDown().filter { it.isFile }.associate { it.name to it.readBytes().contentHashCode() })
        } finally { compose.runOnIdle { audio.stopPlayback();audio.discardReview();audio.leaveBlock();practice.pause() };db.close() }
    }
    @Test fun darkFormsDialogsDatePickerAndLargeSettingsRemainReachable() {
        shown("Practice Now");choose(ThemeChoice.DARK)
        click("Edit");click("New Goal")
        compose.onAllNodes(hasSetTextAction())[0].performTextInput("Unsaved theme Goal")
        compose.onNodeWithTag("home-brand").performClick();shown("Discard unsaved Goal changes?");shot("dialog-dark")
        click("Discard");shown("Practice Now")
        click("Log");click("Choose date");shot("date-picker-dark");click("Cancel")
        compose.onNodeWithTag("home-brand").performClick();shown("Practice Now");click("Settings")
        device.executeShellCommand("settings put system font_scale 2.0")
        compose.activityRule.scenario.recreate();compose.waitForIdle();tag("theme-dark").assertIsSelected();shot("settings-large-dark")
        compose.runOnIdle { compose.activity.requestedOrientation=android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
        compose.waitUntil(15000) { compose.activity.resources.configuration.orientation==Configuration.ORIENTATION_LANDSCAPE }
        tag("theme-system").assertIsDisplayed();shot("settings-landscape-dark")
        tag("default-count-in").assertIsDisplayed()
        compose.onNodeWithTag("audio-settings-scroll-scrollbar").assertExists()
    }
}
