package com.mrayover.practicelogger

import android.content.Intent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import androidx.test.uiautomator.UiDevice
import com.mrayover.practicelogger.composer.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class ComposerC4DWorkflowTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private val inst get()=InstrumentationRegistry.getInstrumentation()
    private val device get()=UiDevice.getInstance(inst)
    private val dir get()=File(inst.targetContext.getExternalFilesDir(null),"composer-c4d").apply { mkdirs() }
    @get:Rule val failure=object: org.junit.rules.TestWatcher() {
        override fun failed(e: Throwable?,d: org.junit.runner.Description?) { device.takeScreenshot(File(dir,"failure.png")); device.dumpWindowHierarchy(File(dir,"failure.xml")) }
    }
    private fun activity(): ComposerActivity { var a: ComposerActivity?=null; inst.runOnMainSync { a=ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).filterIsInstance<ComposerActivity>().single() }; return a!! }
    private fun vm()=ViewModelProvider(activity())[ComposerViewModel::class.java]
    private fun current()=vm().project!!
    private fun tap(t: String) { compose.waitUntil(20000) { compose.onAllNodesWithText(t).fetchSemanticsNodes(false).any { !it.config.contains(SemanticsProperties.Disabled) } };val n=compose.onNodeWithText(t);if(!n.isDisplayed()) n.performScrollTo();n.performClick();compose.waitForIdle() }
    private fun shot(name: String) { compose.waitForIdle();device.waitForIdle();Thread.sleep(200);assertTrue(device.takeScreenshot(File(dir,"$name.png"))) }
    private fun level(tag: String,value: Float) { compose.onNodeWithTag(tag).performSemanticsAction(SemanticsActions.SetProgress) { it(value) };compose.waitForIdle() }
    private fun progress(tag: String)=compose.onNodeWithTag(tag).fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo].current
    private fun waitTag(tag: String) { compose.waitUntil(30000) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes(false).isNotEmpty() };compose.waitForIdle() }
    private fun save() { tap("Save");compose.waitUntil(15000) { !vm().busy && !vm().dirty } }
    @Test fun nativePanPersistentMixerSaveAsAndColdReopen() {
        val store=CompositionStore(File(inst.targetContext.filesDir,"composer/projects"))
        if(InstrumentationRegistry.getArguments().getString("c4dCold")=="true") {
            val exact=CompositionCodec.decode(File(dir,"project.json").readText());assertEquals(exact,store.read(exact.id))
            inst.runOnMainSync { compose.activity.startActivity(Intent(compose.activity,ComposerActivity::class.java).putExtra(ComposerActivity.PROJECT_ID,exact.id)) };waitTag("piano-roll");tap("Mixer")
            exact.tracks.take(3).forEach { assertEquals(it.mixer.volume,progress("mixer-volume-${it.id}"),.001f);assertEquals(it.mixer.pan,progress("mixer-pan-${it.id}"),.001f) }
            assertEquals(exact.masterVolume,progress("mixer-master"),.001f)
            val audio=activity().auditionEngine!!;compose.waitUntil(30000) { audio.ready };tap("Play");compose.waitUntil(15000) { audio.playing && audio.nonZeroFramesWritten>0 && audio.appliedMasterGain==exact.masterVolume }
            assertEquals(exact.tracks[1].patterns.last().notes,audio.playbackPlan!!.tracks[1].notes);tap("Stop");return
        }
        if(InstrumentationRegistry.getArguments().getString("c4dSkipNative")!="true") c4dNativeChecks(File(dir,"native-result.txt"))
        device.executeShellCommand("wm size 1440x3120");device.executeShellCommand("wm density 480")
        val tracks=(0..3).map { i -> CompositionTrack(newCompositionId(),"Track ${i+1}",ComposerInstruments.all[i].reference,(0..1).map { j ->
            CompositionPattern(newCompositionId(),if(j==0) "A" else "B",if(i%2==0) 4 else 8,listOf(CompositionNote(newCompositionId(),48+i+j,0,960,80+j))) }) }
        val initial=store.create(CompositionProject.empty("C4D Mixer").copy(tracks=tracks))
        inst.runOnMainSync { compose.activity.startActivity(Intent(compose.activity,ComposerActivity::class.java).putExtra(ComposerActivity.PROJECT_ID,initial.id)) };waitTag("piano-roll")
        val audio=activity().auditionEngine!!;compose.waitUntil(30000) { audio.ready };tap("Play");compose.waitUntil(15000) { audio.playing };val plan=audio.playbackPlan!!
        tap("Mixer");waitTag("mixer-master");assertSame(audio,activity().auditionEngine)
        level("mixer-pan-${tracks[0].id}",-1f);level("mixer-pan-${tracks[1].id}",0f);level("mixer-pan-${tracks[2].id}",1f)
        level("mixer-volume-${tracks[0].id}",.37f);level("mixer-master",.23f)
        assertEquals(-1f,current().tracks[0].mixer.pan);assertEquals(1f,current().tracks[2].mixer.pan);assertEquals(.37f,current().tracks[0].mixer.volume)
        assertSame(plan,audio.playbackPlan);assertTrue(audio.playing);shot("01-four-pan-left-center-right-master")
        val before=vm().undoCount
        compose.onNodeWithTag("mixer-volume-${tracks[0].id}").performTouchInput { swipe(Offset(width/2f,height*.7f),Offset(width/2f,height*.3f),600) }
        assertEquals(before+1,vm().undoCount)
        val model=vm();inst.runOnMainSync { model.undo() };compose.waitForIdle();assertEquals(.37f,current().tracks[0].mixer.volume)
        compose.onNodeWithTag("mixer-mute-${tracks[3].id}").performClick();compose.onNodeWithTag("mixer-solo-${tracks[1].id}").performClick()
        compose.onNodeWithTag("mixer-pattern-${tracks[1].id}").performClick();tap("B")
        compose.waitUntil(15000) { audio.playing && audio.playbackPlan!==plan };assertEquals(tracks[1].patterns[1].id,current().tracks[1].mixer.selectedPatternId)
        tap("Loop");waitTag("piano-roll");assertTrue(audio.playing);assertEquals(.37f,progress("composer-volume"),.001f);tap("Mixer")
        val old=activity();inst.runOnMainSync { old.recreate() };waitTag("mixer-master");assertEquals(.23f,progress("mixer-master"),.001f);assertEquals(-1f,progress("mixer-pan-${tracks[0].id}"),.001f)
        save();val original=current();assertEquals(original,store.read(original.id))
        tap("Save As");compose.onNodeWithTag("composer-copy-title").performTextReplacement("C4D independent copy");tap("Create copy")
        compose.waitUntil(15000) { !vm().busy && current().id!=original.id };waitTag("mixer-master")
        val copy=current();assertEquals(original,store.read(original.id));assertEquals(copy,store.read(copy.id));assertEquals(original.tracks[0].mixer,copy.tracks[0].mixer)
        assertNotEquals(original.tracks[1].mixer.selectedPatternId,copy.tracks[1].mixer.selectedPatternId);assertEquals(copy.tracks[1].patterns.last().id,copy.tracks[1].mixer.selectedPatternId)
        val copyVm=vm();var eight=copy;repeat(4) { eight=ComposerManagement.addTrack(eight) };val eightFinal=eight
        inst.runOnMainSync { copyVm.edit(eightFinal) };compose.waitForIdle();shot("02-eight-tracks-start")
        compose.onNodeWithTag("mixer-strip-${eight.tracks.last().id}").performScrollTo();shot("03-eight-tracks-end-master")
        device.executeShellCommand("wm size 1080x1920");device.executeShellCommand("wm density 480");compose.waitForIdle();Thread.sleep(800);shot("04-narrow-landscape")
        compose.onNodeWithTag("mixer-master").assertIsDisplayed()
        compose.onNodeWithTag("mixer-strip-${eight.tracks.last().id}").performScrollTo();compose.waitForIdle()
        val bounds=compose.onNodeWithTag("mixer-volume-${eight.tracks.last().id}").fetchSemanticsNode().boundsInWindow
        assertTrue("Narrow vertical fader remains usable",bounds.height>=48*activity().resources.displayMetrics.density)
        val finalVm=vm();inst.runOnMainSync { finalVm.edit(copy) };compose.waitForIdle();save()
        val exact=current();File(dir,"project.json").writeText(CompositionCodec.encode(exact))
        val finalAudio=activity().auditionEngine!!;compose.waitUntil(30000) { finalAudio.ready };tap("Play");compose.waitUntil(10000) { finalAudio.playing && finalAudio.appliedMasterGain==.23f }
        tap("Stop");compose.waitUntil(10000) { !finalAudio.playing && finalAudio.voiceCount()==0 };tap("Play");compose.waitUntil(10000) { finalAudio.playing && finalAudio.appliedMasterGain==.23f }
        inst.runOnMainSync { finalAudio.setForeground(false) };compose.waitUntil(10000) { !finalAudio.playing && finalAudio.voiceCount()==0 };inst.runOnMainSync { finalAudio.setForeground(true) }
        File(dir,"result.txt").writeText("PASS live Pan/Volume without restart, shared Loop/Mixer, owned pattern restart, logical gesture Undo, mute/solo, recreation, Save and Save As identity mapping/original preservation, 4/8 tracks/narrow rendering, master output reopen, interruption cleanup. Cold reopen separately.\n")
    }
}
