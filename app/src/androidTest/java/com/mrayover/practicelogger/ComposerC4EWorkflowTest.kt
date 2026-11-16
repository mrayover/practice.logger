package com.mrayover.practicelogger

import android.content.Intent
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

class ComposerC4EWorkflowTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private val inst get()=InstrumentationRegistry.getInstrumentation()
    private val device get()=UiDevice.getInstance(inst)
    private val dir get()=File(inst.targetContext.getExternalFilesDir(null),"composer-c4e").apply { mkdirs() }
    @get:Rule val failure=object: org.junit.rules.TestWatcher() {
        override fun failed(e: Throwable?,d: org.junit.runner.Description?) { device.takeScreenshot(File(dir,"failure.png"));device.dumpWindowHierarchy(File(dir,"failure.xml")) }
    }
    private fun activity(): ComposerActivity { var a: ComposerActivity?=null;inst.runOnMainSync { a=ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).filterIsInstance<ComposerActivity>().single() };return a!! }
    private fun vm()=ViewModelProvider(activity())[ComposerViewModel::class.java]
    private fun tap(text:String) {
        compose.waitUntil(20000) { compose.onAllNodesWithText(text).fetchSemanticsNodes(false).any { !it.config.contains(SemanticsProperties.Disabled) } }
        compose.onNodeWithText(text).performClick();compose.waitForIdle()
    }
    private fun tag(tag:String) { compose.waitUntil(30000) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes(false).isNotEmpty() };compose.waitForIdle() }
    private fun level(tag:String,value:Float) { compose.onNodeWithTag(tag).performSemanticsAction(SemanticsActions.SetProgress) { it(value) };compose.waitForIdle() }
    private fun shot(name:String) { compose.waitForIdle();device.waitForIdle();assertTrue(device.takeScreenshot(File(dir,"$name.png"))) }
    @Test fun processingPlaybackUndoSaveAndColdReopen() {
        val store=CompositionStore(File(inst.targetContext.filesDir,"composer/projects"))
        val cold=InstrumentationRegistry.getArguments().getString("c4eCold")=="true"
        val initial=if(cold) CompositionCodec.decode(File(dir,"project.json").readText()) else {
            val tracks=(0..3).map { i -> CompositionTrack(newCompositionId(),"Track ${i+1}",ComposerInstruments.all[i].reference,
                listOf(CompositionPattern(newCompositionId(),"A",if(i%2==0) 4 else 8,listOf(CompositionNote(newCompositionId(),48+i,0,1920,85))))) }
            store.create(CompositionProject.empty("C4E Mixer").copy(tracks=tracks))
        }
        if(!cold && InstrumentationRegistry.getArguments().getString("c4eSkipNative")!="true") c4dNativeChecks(File(dir,"native-c4d-path.txt"))
        device.executeShellCommand("wm size 1080x1920");device.executeShellCommand("wm density 480")
        inst.runOnMainSync { compose.activity.startActivity(Intent(compose.activity,ComposerActivity::class.java).putExtra(ComposerActivity.PROJECT_ID,initial.id)) }
        tag("piano-roll");val model=vm();val audio=activity().auditionEngine!!
        compose.waitUntil(30000) { audio.ready };tap("Play");compose.waitUntil(15000) { audio.playing && audio.nonZeroFramesWritten>0 }
        val plan=audio.playbackPlan!!;tap("Mixer");tag("mixer-master");shot(if(cold) "04-cold-mixer" else "01-mixer")
        val id=initial.tracks[0].id
        compose.onNodeWithTag("mixer-eq-$id").performClick();tag("channel-processing")
        if(cold) {
            assertEquals(initial,model.project);assertEquals(initial,store.read(initial.id))
            val expected=initial.tracks[0].mixer.processing
            for((i,v) in listOf(expected.lowDb,expected.lowMidDb,expected.highMidDb,expected.highDb).withIndex())
                assertEquals(v,compose.onNodeWithTag("eq-band-$i").fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo].current,.001f)
            assertEquals(expected.width,compose.onNodeWithTag("stereo-width").fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo].current,.001f)
            shot("05-cold-eq");tap("Close");tap("Stop");return
        }
        for(i in 0..3) compose.onNodeWithTag("eq-band-$i").assertIsDisplayed()
        compose.onNodeWithTag("stereo-width").assertIsDisplayed();compose.onNodeWithTag("eq-bypass").assertIsDisplayed();shot("02-neutral-editor")
        val history=model.undoCount
        // Real pointer gesture has many live samples and exactly one completed edit.
        compose.onNodeWithTag("eq-band-0").performTouchInput { swipe(centerLeft,centerRight,500) };compose.waitForIdle()
        assertEquals(history+1,model.undoCount);assertEquals(12f,model.project!!.tracks[0].mixer.processing.lowDb,.1f)
        inst.runOnMainSync { model.undo() };compose.waitForIdle();assertEquals(ChannelProcessing(),model.project!!.tracks[0].mixer.processing)
        level("eq-band-0",-7.5f);level("eq-band-1",4.2f);level("eq-band-2",-3.1f);level("eq-band-3",6.4f);level("stereo-width",1.65f)
        compose.onNodeWithTag("eq-bypass").performClick();compose.waitForIdle()
        val settings=ChannelProcessing(-7.5f,4.2f,-3.1f,6.4f,false,1.65f)
        assertEquals(settings,model.project!!.tracks[0].mixer.processing);assertEquals(initial.tracks.drop(1),model.project!!.tracks.drop(1))
        assertEquals(4,model.project!!.formatVersion);assertTrue(audio.playing);assertSame(plan,audio.playbackPlan);assertSame(audio,activity().auditionEngine);shot("03-changed-bypass-width")
        tap("Close");tap("Loop");assertEquals(settings,model.project!!.tracks[0].mixer.processing);tap("Mixer")
        tap("Save");compose.waitUntil(15000) { !model.busy && !model.dirty };val saved=model.project!!;assertEquals(saved,store.read(saved.id))
        inst.runOnMainSync { model.saveAs("C4E independent") };compose.waitUntil(15000) { !model.busy && model.project!!.id!=saved.id }
        val copy=model.project!!;assertEquals(saved,store.read(saved.id));assertEquals(settings,copy.tracks[0].mixer.processing);assertNotEquals(id,copy.tracks[0].id)
        assertEquals(copy,store.read(copy.id));File(dir,"project.json").writeText(CompositionCodec.encode(copy))
        tap("Stop");compose.waitUntil(10000) { !audio.playing && audio.voiceCount()==0 }
        File(dir,"result.txt").writeText("PASS four bands/width/bypass, real gesture one Undo, independent tracks, no Loop restart, shared navigation, exact Save/Save As and cleanup. Cold process separately.\n")
    }
}
