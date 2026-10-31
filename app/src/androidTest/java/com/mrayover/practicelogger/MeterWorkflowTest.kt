package com.mrayover.practicelogger

import android.Manifest
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class MeterWorkflowTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private val device get()=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val practice get()=ViewModelProvider(compose.activity)[PracticeViewModel::class.java]
    private val audio get()=ViewModelProvider(compose.activity)[RecordingViewModel::class.java]
    private fun shown(text:String)=compose.waitUntil(15000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    private fun click(text:String) { shown(text);compose.onNodeWithText(text).scrollIfNeeded().assertIsDisplayed().performClick() }
    private fun tag(value:String)=compose.onNodeWithTag(value).scrollIfNeeded().assertIsDisplayed()
    private fun keyboard()=compose.runOnIdle { compose.activity.currentFocus?.clearFocus();compose.activity.getSystemService(android.view.inputmethod.InputMethodManager::class.java).hideSoftInputFromWindow(compose.activity.window.decorView.windowToken,0) }
    private fun shot(name:String) { compose.waitForIdle();val dir=File(compose.activity.getExternalFilesDir(null),"p3-meter");dir.mkdirs();device.takeScreenshot(File(dir,"$name.png")) }
    private fun focus(db:BlockDatabase,name:String,reference:Boolean=false):String {
        val app=compose.activity.application
        val (block,session)=runBlocking {
            val b=BlockRepository(db).save(null,BlockDraft(name,"Keep practicing","5","120"))
            if(reference) ImageReferenceRepository(db,File(app.filesDir,"block-images")).import(b) { InstrumentationRegistry.getInstrumentation().context.assets.open("references/score.pdf") }
            val repo=SessionRepository(db);repo.addBlock(b);b to repo.start()
        }
        shown("Practice Now");compose.runOnIdle { practice.openSession(session) };shown("Practice Description")
        return block
    }
    @Test fun expansionRestoresPdfZoomNotesAndTimerWhileMeterAndTempoChange() {
        val app=compose.activity.application;val db=BlockDatabase.open(app)
        val notes=ViewModelProvider(compose.activity)[BlockNotesViewModel::class.java]
        try {
            focus(db,"P3 retained workspace",true)
            tag("practice-reference-toggle").performClick();shown("Page 1 of 2");click("Next page");shown("Page 2 of 2")
            tag("reference-viewer").performTouchInput { pinch(Offset(width*.4f,height*.4f),Offset(width*.6f,height*.6f),Offset(width*.15f,height*.15f),Offset(width*.85f,height*.85f)) };shown("Reset PDF zoom")
            click("Notes");tag("practice-new-note").performTextReplacement("Keep P3 draft");keyboard();click("Return to Practice")
            click("Start");click("Start click");compose.waitUntil(10000) { practice.metronomePulse.sequence>=2 }
            val sequence=practice.metronomePulse.sequence
            tag("practice-metronome-expand").performClick();tag("practice-metronome-panel").assertExists()
            compose.onNodeWithTag("reference-viewer").assertDoesNotExist()
            tag("metronome-accent").assertIsOff();tag("meter-3/4").performClick();tag("metronome-accent").performClick().assertIsOn()
            tag("metronome-panel-bpm").performTextReplacement("180");keyboard();tag("metronome-apply-bpm").performClick()
            compose.waitUntil(10000) { practice.workingBpm==180 && practice.metronomePulse.config.meter==MetronomeMeter.THREE_FOUR && practice.metronomePulse.sequence>sequence }
            compose.runOnIdle { assertTrue(practice.running);assertTrue(practice.clicking) };shot("expanded-three-four")
            tag("practice-metronome-expand").performClick();shown("Page 2 of 2");shown("Reset PDF zoom")
            compose.waitUntil(10000) { practice.metronomePulse.lit }
            compose.onNodeWithTag("practice-metronome-pulse",useUnmergedTree=true).assertIsDisplayed()
            compose.onNodeWithTag("practice-metronome-meter",useUnmergedTree=true).assertTextContains("3/4")
            shot("collapsed-pulse-pdf")
            click("Notes");tag("practice-new-note").assertTextContains("Keep P3 draft")
            compose.runOnIdle { notes.edit(practice.catalog.value.active!!.current!!.id,"") };click("Return to Practice")
            click("Stop click");compose.onNodeWithTag("practice-metronome-meter",useUnmergedTree=true).assertDoesNotExist()
            device.pressBack();click("Back to Main")
        } finally { compose.runOnIdle { practice.stopClick();audio.stopPlayback() };db.close() }
    }
    @Test fun toolsParityEighthNoteCountingAllModesAndForegroundStop() {
        shown("Practice Now");click("Tools");click("Metronome")
        val tools=ViewModelProvider(compose.activity)[ToolsViewModel::class.java]
        tag("metronome-panel-bpm").performTextReplacement("240");keyboard();tag("metronome-apply-bpm").performClick()
        tag("meter-6/8").performClick();tag("mode-FLASH_ONLY").performClick();tag("metronome-accent").assertIsOff()
        click("Start click")
        compose.waitUntil(15000) { tools.metronomePulse.sequence>=8 && tools.metronomePulse.config.meter==MetronomeMeter.SIX_EIGHT }
        assertEquals(0L,tools.metronomeNonZeroFramesWritten());assertTrue(tools.metronomeFramesWritten()>48000)
        compose.waitUntil(10000) { tools.metronomePulse.lit };tag("tool-metronome-pulse").assertIsDisplayed()
        tag("meter-7/8").performClick();compose.waitUntil(10000) { tools.metronomePulse.config.meter==MetronomeMeter.SEVEN_EIGHT }
        tag("mode-SOUND_ONLY").performClick();compose.waitUntil(10000) { tools.metronomePulse.config.mode==MetronomeMode.SOUND_ONLY && tools.metronomeNonZeroFramesWritten()>0 }
        assertFalse(tools.metronomePulse.lit)
        tag("mode-SOUND_FLASH").performClick();tag("metronome-accent").performClick().assertIsOn()
        compose.waitUntil(10000) { tools.metronomePulse.lit && tools.metronomePulse.config.accent };shot("tools-seven-eight")
        device.pressHome();compose.waitUntil(10000) { !tools.clicking && !tools.metronomePulse.lit }
        device.executeShellCommand("am start -W -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -f 0x10200000 -n ${compose.activity.packageName}/com.mrayover.practicelogger.MainActivity")
        shown("Start click");assertFalse(tools.clicking);click("Back to Tools")
    }
    @Test fun changingMetronomePanelPreservesFreeCaptureAndDoesNotChangePhraseCountIn() {
        val app=compose.activity.application;val db=BlockDatabase.open(app)
        try {
            val block=focus(db,"P3 recording owner")
            device.executeShellCommand("pm grant ${app.packageName} ${Manifest.permission.RECORD_AUDIO}")
            compose.waitUntil(10000) { audio.ready };tag("practice-free-record").performClick()
            compose.waitUntil(10000) { audio.recording && audio.elapsedMs>=1000 }
            tag("practice-metronome-expand").performClick();tag("meter-5/4").performClick();tag("mode-FLASH_ONLY").performClick();click("Start click")
            compose.waitUntil(10000) { practice.metronomePulse.sequence>=2 }
            compose.runOnIdle { assertTrue(audio.recording);assertEquals(0L,practice.metronomeNonZeroFramesWritten()) }
            click("Stop click");tag("practice-free-record").performClick()
            compose.waitUntil(15000) { !audio.busy && audio.reviewTake!=null };tag("review-save").performClick()
            compose.waitUntil(15000) { !audio.busy && audio.reviewTake==null && audio.takes.value.any { it.blockId==block } }
            compose.runOnIdle { audio.startPhrase(block,PhraseSpec(120,5,3),false) }
            compose.waitUntil(5000) { audio.phrasing && audio.countInTotal==3L }
            compose.onNodeWithText("Start click").assertIsNotEnabled();tag("meter-7/8").performClick()
            compose.waitUntil(15000) { !audio.busy && audio.reviewTake?.mode=="LOOP" }
            assertEquals(5,audio.reviewTake!!.beatCount);assertEquals(2500L,audio.reviewTake!!.durationMs);assertEquals(3L,audio.countInTotal)
            assertEquals(1,runBlocking { db.takes().readAll().count { it.blockId==block } })
            tag("review-discard").performClick();shot("recording-review-retained")
            tag("practice-metronome-expand").performClick();device.pressBack();click("Back to Main")
        } finally { compose.runOnIdle { practice.stopClick();audio.stopPlayback() };db.close() }
    }
}
