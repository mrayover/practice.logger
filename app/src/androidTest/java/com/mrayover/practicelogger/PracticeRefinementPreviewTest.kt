package com.mrayover.practicelogger

import android.Manifest
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

/** Bounded P4 presentation checks; uses real existing audio/recording ownership. */
class PracticeRefinementPreviewTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val device get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private fun shown(text: String) = compose.waitUntil(15000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    private fun click(text: String) { shown(text); compose.onNodeWithText(text).scrollIfNeeded().assertIsDisplayed().performClick() }
    private fun tag(name: String) = compose.onNodeWithTag(name).scrollIfNeeded().assertIsDisplayed()
    private fun shot(name: String) {
        compose.waitForIdle()
        val dir=File(compose.activity.getExternalFilesDir(null),"practice-refinement").apply { mkdirs() }
        device.takeScreenshot(File(dir,"$name.png"))
    }
    private fun modeRow() {
        val bounds=MetronomeMode.entries.map { tag("mode-${it.name}").fetchSemanticsNode().boundsInRoot }
        val row=tag("practice-metronome-modes").fetchSemanticsNode().boundsInRoot
        bounds.forEach { assertTrue("Mode outside row: $it / $row",it.left>=row.left-1 && it.right<=row.right+1) }
        assertTrue(bounds.zipWithNext().all { (a,b) -> a.right<=b.left+1 && kotlin.math.abs(a.center.y-b.center.y)<=1 })
        compose.onNodeWithText("Flash Only").assertExists()
    }
    @Test fun compactPanelWorkspaceCaptureAndReviewPreserveState() {
        val app=compose.activity.application
        val db=BlockDatabase.open(app)
        val practice=ViewModelProvider(compose.activity)[PracticeViewModel::class.java]
        val audio=ViewModelProvider(compose.activity)[RecordingViewModel::class.java]
        try {
            device.executeShellCommand("pm grant ${app.packageName} ${Manifest.permission.RECORD_AUDIO}")
            val session=runBlocking {
                val b=BlockRepository(db).save(null,BlockDraft("P4 compact preview","Keep the phrase relaxed.","5","90"))
                ImageReferenceRepository(db,File(app.filesDir,"block-images")).import(b) {
                    InstrumentationRegistry.getInstrumentation().context.assets.open("references/score.pdf")
                }
                val repo=SessionRepository(db);repo.addBlock(b);repo.start()
            }
            shown("Practice Now");compose.runOnIdle { practice.openSession(session) };shown("90 BPM")
            compose.waitUntil(15000) { audio.ready }
            tag("practice-reference-toggle").performClick();click("Next page");shown("Page 2 of 2")
            tag("reference-viewer").performTouchInput {
                pinch(Offset(width*.4f,height*.4f),Offset(width*.6f,height*.6f),Offset(width*.15f,height*.15f),Offset(width*.85f,height*.85f))
            }
            shown("Reset PDF zoom");click("Start");click("Start click")
            compose.waitUntil(10000) { practice.metronomePulse.sequence>=2 }
            val sequence=practice.metronomePulse.sequence
            val elapsed=practice.catalog.value.active!!.current!!.activeMs
            val frames=practice.metronomeNonZeroFramesWritten()
            tag("practice-metronome-expand").performClick();modeRow()
            // A normal portrait viewport fits all options without inner or transport scrolling.
            compose.onNodeWithTag("practice-metronome-panel-scrollbar").assertDoesNotExist()
            compose.onNodeWithTag("practice-transport-scrollbar").assertDoesNotExist()
            tag("metronome-panel-bpm").assertIsDisplayed();tag("metronome-accent").assertIsDisplayed()
            tag("practice-metronome-pulse").assertIsDisplayed();shot("compact-portrait")
            tag("practice-reference-toggle").assertIsEnabled().performClick()
            compose.onNodeWithTag("practice-metronome-panel").assertDoesNotExist()
            shown("Page 2 of 2");shown("Reset PDF zoom")
            tag("practice-metronome-expand").performClick()
            tag("practice-description-toggle").performClick();shown("Keep the phrase relaxed.")
            compose.runOnIdle { assertTrue(practice.clicking);assertTrue(practice.running);assertEquals(90,practice.workingBpm) }
            compose.waitUntil(10000) { practice.metronomePulse.sequence>sequence && practice.metronomeNonZeroFramesWritten()>frames && practice.catalog.value.active!!.current!!.activeMs>elapsed }
            // Repeated explicit close still restores the selected workspace.
            tag("practice-metronome-expand").performClick();tag("practice-metronome-expand").performClick();shown("Keep the phrase relaxed.")
            click("Record Loop");tag("practice-reference-toggle").performClick()
            compose.onNodeWithTag("practice-loop-panel").assertDoesNotExist();shown("Page 2 of 2");shown("Reset PDF zoom")
            tag("practice-free-record").performClick()
            compose.waitUntil(15000) { audio.recording && audio.elapsedMs>=1200 }
            val captured=audio.elapsedMs
            tag("practice-metronome-expand").performClick();tag("practice-reference-toggle").performClick()
            compose.runOnIdle { assertTrue(audio.recording);assertTrue(practice.running);assertTrue(practice.clicking) }
            tag("practice-recording-stop").assertIsDisplayed()
            compose.waitUntil(10000) { audio.elapsedMs>captured }
            tag("practice-recording-stop").performClick()
            compose.waitUntil(15000) { audio.reviewTake!=null && !audio.busy }
            val pending=audio.reviewTake!!.id
            tag("practice-description-toggle").performClick();shown("Keep the phrase relaxed.")
            compose.runOnIdle { assertEquals(pending,audio.reviewTake!!.id);assertTrue(audio.takes.value.none { it.id==pending }) }
            tag("review-save").assertIsDisplayed();tag("practice-review-expand").performClick()
            tag("review-preview").assertIsDisplayed()
            tag("practice-reference-toggle").performClick();shown("Page 2 of 2");shown("Reset PDF zoom")
            tag("review-save").performClick()
            compose.waitUntil(15000) { audio.reviewTake==null && audio.takes.value.any { it.id==pending } }
            compose.runOnIdle { assertTrue(practice.running);assertTrue(practice.clicking);assertEquals(90,practice.workingBpm) }
            shot("workspace-after-save")
        } finally { shot("workspace-final");compose.runOnIdle { audio.finishCapture();audio.stopPlayback();practice.stopClick() };db.close() }
    }
    @Test fun narrowAndEnlargedModesRemainOneRowWithAccessibleOverflow() {
        // Isolate the affected presentation at 320dp and 200% text; no audio engine substitution.
        compose.runOnIdle {
            compose.activity.setContent {
                val density=LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density,2f)) {
                    PracticeTheme {
                        var config by remember { mutableStateOf(MetronomeConfig()) }
                        Box(Modifier.width(320.dp).height(480.dp)) {
                            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=16.dp)) {
                                MetronomeOptions(config,MetronomePulse(),90,{config=it},{},compact=true)
                            }
                        }
                    }
                }
            }
        }
        modeRow()
        MetronomeMode.entries.forEach { mode ->
            tag("mode-${mode.name}").performClick().assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected,true))
        }
        tag("meter-7/8").performClick().assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected,true))
        tag("metronome-accent").performClick()
        tag("metronome-panel-bpm").performTextReplacement("126")
        tag("metronome-apply-bpm").assertIsEnabled()
        shot("narrow-200-percent")
    }
}
