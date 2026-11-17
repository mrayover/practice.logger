package com.mrayover.practicelogger

import android.content.Intent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
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

class ComposerC5CWorkflowTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private val inst get()=InstrumentationRegistry.getInstrumentation()
    private val device get()=UiDevice.getInstance(inst)
    private val dir get()=File(inst.targetContext.getExternalFilesDir(null),"composer-c5c").apply { mkdirs() }
    @get:Rule val failure=object: org.junit.rules.TestWatcher() {
        override fun failed(e: Throwable?,d: org.junit.runner.Description?) { device.takeScreenshot(File(dir,"failure.png"));device.dumpWindowHierarchy(File(dir,"failure.xml")) }
    }
    private fun activity(): ComposerActivity { var a: ComposerActivity?=null;inst.runOnMainSync { a=ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).filterIsInstance<ComposerActivity>().single() };return a!! }
    private fun vm()=ViewModelProvider(activity())[ComposerViewModel::class.java]
    private fun current()=vm().project!!
    private fun tap(text: String) {
        compose.waitUntil(20000) { compose.onAllNodesWithText(text).fetchSemanticsNodes(false).any { !it.config.contains(androidx.compose.ui.semantics.SemanticsProperties.Disabled) } }
        compose.onNodeWithText(text).performClick();compose.waitForIdle()
    }
    private fun shot(name: String) { compose.waitForIdle();device.waitForIdle();assertTrue(device.takeScreenshot(File(dir,"$name.png"))) }
    private fun open(id: String) {
        inst.runOnMainSync { compose.activity.startActivity(Intent(compose.activity,ComposerActivity::class.java).putExtra(ComposerActivity.PROJECT_ID,id)) }
        compose.waitUntil(30000) { compose.onAllNodesWithTag("piano-roll").fetchSemanticsNodes(false).isNotEmpty() };tap("Arrange")
    }
    @Test fun uniqueEditReturnUndoAndPersistence() {
        val store=CompositionStore(File(inst.targetContext.filesDir,"composer/projects"))
        if(InstrumentationRegistry.getArguments().getString("c5cCold")=="true") {
            val expected=CompositionCodec.decode(File(dir,"project.json").readText())
            assertEquals(expected,store.read(expected.id));open(expected.id);assertEquals(expected,current());shot("06-cold-reopen");return
        }
        device.executeShellCommand("wm size 1440x3120");device.executeShellCommand("wm density 480")
        val a=CompositionPattern(newCompositionId(),"A",2,listOf(CompositionNote(newCompositionId(),81,0,960,80)))
        val tracks=(0..7).map { i -> CompositionTrack(newCompositionId(),"Voice ${i+1}",ComposerInstruments.all[i].reference,
            listOf(if(i==3) a else CompositionPattern(newCompositionId(),"Other",1))) }
        var fixture=CompositionProject.empty("C5C variations",240).copy(tracks=tracks)
        for(bar in listOf(0L,4L,8L)) fixture=ArrangementEdits.add(fixture,tracks[3].id,a.id,bar*fixture.barTicks())
        val saved=store.create(fixture);open(saved.id)
        val audio=activity().auditionEngine!!;compose.waitUntil(30000) { audio.ready }
        val density=inst.targetContext.resources.displayMetrics.density
        fun timeline()=compose.onNodeWithTag("arrange-timeline")
        tap("Actions");compose.onNodeWithText("Edit Pattern").assertIsNotEnabled();compose.onNodeWithText("Make Unique").assertIsNotEnabled();device.pressBack();compose.waitForIdle()
        tap("Zoom −");tap("Select ✓")
        timeline().performTouchInput { swipe(Offset(400*density,200*density),Offset(376*density,148*density),400) }
        tap("Hand ✓")
        fun selectMiddle() { timeline().performTouchInput { click(Offset((156+4*70.4f-24+20)*density,166*density)) } }
        selectMiddle();shot("01-original-shared-clips")
        val count=vm().undoCount
        tap("Play");compose.waitUntil(15000) { audio.playing && audio.nonZeroFramesWritten>0 }
        tap("Actions");tap("Make Unique")
        compose.waitUntil(10000) { !audio.playing && audio.voiceCount()==0 }
        val unique=current();val variation=unique.tracks[3].patterns.last()
        assertEquals(count+1,vm().undoCount);assertEquals("A Variation",variation.title)
        assertEquals(saved.clips[1].copy(patternId=variation.id),unique.clips[1]);assertEquals(saved.clips[0],unique.clips[0]);assertEquals(saved.clips[2],unique.clips[2])
        shot("02-middle-unique")
        tap("Undo");assertEquals(saved,current());assertEquals(count,vm().undoCount)
        tap("Actions");tap("Make Unique")
        val beforeEdit=current();val uniqueId=beforeEdit.clips[1].patternId
        val view=timeline().captureToImage().asAndroidBitmap();val entrance=activity().brandFirstDrawMs;val history=vm().undoCount
        tap("Actions");tap("Edit Pattern")
        compose.onNodeWithTag("piano-roll").assertIsDisplayed();assertSame(audio,activity().auditionEngine)
        assertEquals(beforeEdit,current());assertEquals(history,vm().undoCount);assertEquals(entrance,activity().brandFirstDrawMs)
        compose.onNodeWithText("Pattern A Variation").assertIsDisplayed()
        assertTrue(compose.onAllNodesWithText("Instrument 4 · Electric bass",substring=true).fetchSemanticsNodes(false).isNotEmpty())
        val roll=compose.onNodeWithTag("piano-roll");val image=roll.captureToImage();val pixels=image.toPixelMap()
        val key=62*density;val row=26*density;val ruler=28*density
        val xs=(0 until image.width).filter { pixels[it,(ruler+3.5f*row).toInt()].toArgb()==0xFF64BBDF.toInt() }
        assertTrue(xs.isNotEmpty());assertEquals(((image.width-key-2)/4).toDouble(),(xs.last()-xs.first()+1).toDouble(),4.0)
        val scale=(image.width-key-2)/3840
        roll.performTouchInput { click(Offset(key+1200*scale,ruler+4.5f*row)) }
        compose.waitUntil(10000) { current().tracks[3].patterns.last().notes.size==2 }
        assertEquals(a,current().tracks[3].patterns[0]);assertEquals(uniqueId,current().tracks[3].patterns.last().id)
        shot("03-edit-variation-one-bar-fit")
        tap("Play");compose.waitUntil(10000) { audio.playing && !audio.playbackPlan!!.song }
        assertEquals(uniqueId,audio.playbackPlan!!.tracks[3].patternId);tap("Stop")
        tap("Arrange");assertTrue("zoom, both scroll axes and clip selection restored",view.sameAs(timeline().captureToImage().asAndroidBitmap()))
        assertEquals(entrance,activity().brandFirstDrawMs);shot("04-returned-same-view")
        // Shared edit opens the original by clip identity, despite Loop previously targeting the variation.
        timeline().performTouchInput { click(Offset((156+8*70.4f-24+20)*density,166*density)) }
        val beforeShared=current();tap("Actions");tap("Edit Pattern");assertEquals(beforeShared,current())
        compose.onNodeWithText("Pattern A").assertIsDisplayed()
        roll.performTouchInput { click(Offset(key+1920*scale,ruler+5.5f*row)) }
        compose.waitUntil(10000) { current().tracks[3].patterns[0].notes.size==2 }
        assertEquals(beforeShared.tracks[3].patterns.last(),current().tracks[3].patterns.last())
        tap("Arrange");tap("Play");compose.waitUntil(10000) { audio.playing && audio.playbackPlan!!.song }
        val expected=LoopPlan.songSnapshot(current());assertEquals(expected,audio.playbackPlan);tap("Stop")
        tap("Save");compose.waitUntil(15000) { !vm().busy && !vm().dirty }
        val committed=current();assertEquals(committed,store.read(committed.id))
        tap("Return");open(committed.id);assertEquals(committed,current())
        tap("Save As");compose.onNodeWithTag("composer-copy-title").performTextReplacement("C5C independent");tap("Create copy")
        compose.waitUntil(15000) { !vm().busy && current().id!=committed.id }
        val copy=current();assertEquals(committed,store.read(committed.id));assertEquals(copy,store.read(copy.id))
        assertEquals(copy.clips[0].patternId,copy.clips[2].patternId);assertNotEquals(copy.clips[0].patternId,copy.clips[1].patternId)
        File(dir,"project.json").writeText(CompositionCodec.encode(copy));shot("05-save-as")
        File(dir,"result.txt").writeText("PASS single Undo, source/variation independence, stable navigation without project edits, one-bar fit, exact rendered Arrange view retention, song/Loop playback, Save/reopen and Save As. Physical phone acceptance separate.\n")
    }
}
