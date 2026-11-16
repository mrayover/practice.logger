package com.mrayover.practicelogger

import android.content.Intent
import androidx.compose.ui.geometry.Offset
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

class ComposerC5BWorkflowTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private val inst get()=InstrumentationRegistry.getInstrumentation()
    private val device get()=UiDevice.getInstance(inst)
    private val dir get()=File(inst.targetContext.getExternalFilesDir(null),"composer-c5b").apply { mkdirs() }
    @get:Rule val failure=object: org.junit.rules.TestWatcher() {
        override fun failed(e: Throwable?,d: org.junit.runner.Description?) { device.takeScreenshot(File(dir,"failure.png")); device.dumpWindowHierarchy(File(dir,"failure.xml")) }
    }
    private fun activity(): ComposerActivity { var a: ComposerActivity?=null; inst.runOnMainSync { a=ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).filterIsInstance<ComposerActivity>().single() }; return a!! }
    private fun vm()=ViewModelProvider(activity())[ComposerViewModel::class.java]
    private fun current()=vm().project!!
    private fun tap(t: String) {
        compose.waitUntil(20000) { compose.onAllNodesWithText(t).fetchSemanticsNodes(false).any { !it.config.contains(androidx.compose.ui.semantics.SemanticsProperties.Disabled) } }
        compose.onNodeWithText(t).performClick(); compose.waitForIdle()
    }
    private fun shot(name: String) { compose.waitForIdle(); device.waitForIdle(); assertTrue(device.takeScreenshot(File(dir,"$name.png"))) }
    private fun open(id: String) {
        inst.runOnMainSync { compose.activity.startActivity(Intent(compose.activity,ComposerActivity::class.java).putExtra(ComposerActivity.PROJECT_ID,id)) }
        compose.waitUntil(30000) { compose.onAllNodesWithTag("piano-roll").fetchSemanticsNodes(false).isNotEmpty() }; tap("Arrange")
        compose.onNodeWithTag("arrange-timeline").assertIsDisplayed()
    }
    private fun save() { tap("Save"); compose.waitUntil(15000) { !vm().busy && !vm().dirty } }
    @Test fun arrangementGesturesPersistenceAndColdReopen() {
        val store=CompositionStore(File(inst.targetContext.filesDir,"composer/projects"))
        if(InstrumentationRegistry.getArguments().getString("c5bCold")=="true") {
            val expected=CompositionCodec.decode(File(dir,"project.json").readText())
            assertEquals(expected,store.read(expected.id)); open(expected.id); assertEquals(expected,current()); shot("09-cold-reopen"); return
        }
        songNativeChecks(File(dir,"native-result.txt"))
        device.executeShellCommand("wm size 1440x3120"); device.executeShellCommand("wm density 480")
        val a=CompositionPattern(newCompositionId(),"A",1,listOf(CompositionNote(newCompositionId(),60,0,480,80)))
        val piano=CompositionTrack(newCompositionId(),"Piano 1",ComposerInstruments.all[5].reference,listOf(a))
        val bass=CompositionTrack(newCompositionId(),"Piano 2",ComposerInstruments.all[4].reference,listOf(a.copy(id=newCompositionId(),title="Bass B",notes=a.notes.map { it.copy(id=newCompositionId(),pitch=43) })))
        val initial=store.create(CompositionProject.empty("C5B song").copy(tracks=listOf(piano,bass)))
        open(initial.id); shot("01-empty")
        val density=inst.targetContext.resources.displayMetrics.density
        fun point(bar: Int,lane: Int=0)=Offset((156+bar*88+20)*density,(36+lane*52+26)*density)
        fun chooseBar(bar: Int) { compose.onNodeWithTag("arrange-timeline").performTouchInput { click(Offset((156+bar*88+12)*density,18*density)) } }
        fun place(bar: Int,lane: Int,pattern: String) {
            compose.onNodeWithTag("arrange-timeline").performTouchInput { click(point(bar,lane)) };tap(pattern)
        }
        chooseBar(2);assertTrue(current().clips.isEmpty())
        compose.onNodeWithTag("arrange-timeline").performTouchInput { click(point(0,1)) }
        compose.onNodeWithText("A · 1 bar").assertDoesNotExist();tap("Cancel");assertTrue(current().clips.isEmpty())
        place(0,0,"A · 1 bar");assertEquals(1,current().clips.size);shot("02-strings")
        place(1,1,"Bass B · 1 bar");assertEquals(initial.barTicks(),current().clips[1].startTick);shot("03-two-lanes")
        // Duplicate and Repeat each commit one immutable edit; collision rejects the entire batch.
        compose.onNodeWithTag("arrange-timeline").performTouchInput { click(point(0)) }
        var countBefore=vm().undoCount;tap("Actions");tap("Duplicate Clip")
        assertEquals(3,current().clips.size);assertEquals(countBefore+1,vm().undoCount)
        tap("Undo");assertEquals(2,current().clips.size)
        tap("Actions");tap("Repeat Clip…");tap("2 additional")
        assertEquals(4,current().clips.size);tap("Undo");assertEquals(2,current().clips.size)
        place(1,0,"A · 1 bar");place(3,1,"Bass B · 1 bar");shot("04-song-placements")
        val audioOwner=activity().auditionEngine!!;compose.waitUntil(30000) { audioOwner.ready }
        tap("Play");compose.waitUntil(15000) { audioOwner.playing && audioOwner.positionFrame()>2400 && audioOwner.nonZeroFramesWritten>0 }
        val song=audioOwner.playbackPlan!!;assertTrue(song.song);assertFalse(audioOwner.looping)
        shot("05-playing-audio-clock")
        tap("Mixer");assertSame(audioOwner,activity().auditionEngine);assertTrue(audioOwner.playing)
        compose.onNodeWithTag("mixer-volume-${piano.id}").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.SetProgress) { it(.37f) }
        compose.onNodeWithTag("mixer-pan-${bass.id}").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.SetProgress) { it(.5f) }
        compose.onNodeWithTag("mixer-master").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.SetProgress) { it(.4f) }
        compose.waitForIdle();assertSame(song,audioOwner.playbackPlan);assertTrue(audioOwner.playing)
        tap("Arrange");assertSame(song,audioOwner.playbackPlan);assertTrue(audioOwner.playing)
        tap("Actions");tap("Beginning");compose.waitUntil(10000) { audioOwner.positionFrame()<48000 }
        tap("Stop");compose.waitUntil(10000) { !audioOwner.playing && audioOwner.voiceCount()==0 }
        tap("Play");compose.waitUntil(10000) { audioOwner.playing }
        compose.onNodeWithTag("arrange-timeline").performTouchInput { click(point(3,1)) }
        tap("Delete Clip");compose.waitUntil(10000) { !audioOwner.playing && audioOwner.voiceCount()==0 };tap("Undo")
        // Remove the adjacent strings instance so the real drag has a free destination.
        compose.onNodeWithTag("arrange-timeline").performTouchInput { click(point(1)) };tap("Delete Clip")
        val beforeMove=current(); val count=vm().undoCount
        compose.onNodeWithTag("arrange-timeline").performTouchInput { down(point(0)); moveTo(point(1),500); advanceEventTime(150) }
        assertEquals(beforeMove,current()); shot("04-drag-preview")
        compose.onNodeWithTag("arrange-timeline").performTouchInput { up() }
        assertEquals(beforeMove.clips[0].copy(startTick=initial.barTicks()),current().clips[0]); assertEquals(count+1,vm().undoCount)
        tap("Undo"); assertEquals(beforeMove,current())
        compose.onNodeWithTag("arrange-timeline").performTouchInput { swipe(point(0),point(1),500) }
        val moved=current(); tap("Delete Clip"); assertEquals(beforeMove.clips.size-1,current().clips.size); shot("07-last-strings-deleted-empty-lane")
        tap("Undo"); assertEquals(moved,current()); shot("08-restored")
        val audio=activity().auditionEngine
        tap("Loop"); compose.onNodeWithTag("piano-roll").assertIsDisplayed(); assertSame(audio,activity().auditionEngine)
        tap("Play");compose.waitUntil(10000) { audio!!.playing };assertFalse(audio!!.playbackPlan!!.song);tap("Stop");
        tap("Mixer"); assertEquals(moved,current()); tap("Arrange"); assertSame(audio,activity().auditionEngine)
        // Hand navigation owns scrolling, even when the pointer starts on a clip.
        tap("Select ✓")
        compose.onNodeWithTag("arrange-timeline").performTouchInput { swipe(point(2),point(0),500) }
        assertEquals(moved,current()); shot("07-scrolled-fixed-labels")
        val denseVm=vm();var dense=current();repeat(6) { dense=ComposerManagement.addTrack(dense) };val eight=dense
        inst.runOnMainSync { denseVm.edit(eight) };compose.waitForIdle();shot("10-eight-tracks")
        compose.onNodeWithTag("arrange-timeline").performTouchInput { swipe(Offset(60*density,170*density),Offset(60*density,55*density),500) }
        assertEquals(eight,current());shot("11-eight-tracks-vertical-scroll")
        inst.runOnMainSync { denseVm.edit(moved) };compose.waitForIdle()
        save(); val saved=current(); assertEquals(saved,store.read(saved.id))
        tap("Return"); open(saved.id); assertEquals(saved,current())
        val originalActivity=activity(); inst.runOnMainSync { originalActivity.recreate() }
        compose.waitUntil(30000) { compose.onAllNodesWithTag("arrange-timeline").fetchSemanticsNodes(false).isNotEmpty() }
        assertEquals(saved,current())
        device.executeShellCommand("wm size 1080x1920"); device.executeShellCommand("wm density 480")
        compose.waitForIdle(); shot("08-narrow")
        assertTrue(compose.onNodeWithTag("arrange-timeline").fetchSemanticsNode().boundsInRoot.height>100*density)
        tap("Save As");compose.onNodeWithTag("composer-copy-title").performTextReplacement("C5B independent song");tap("Create copy")
        compose.waitUntil(15000) { !vm().busy && current().id!=saved.id }
        val copied=current();assertEquals(saved,store.read(saved.id));assertEquals(copied.clips.size,saved.clips.size)
        assertEquals(copied,store.read(copied.id));copied.clips.forEach { c -> assertTrue(copied.tracks.any { it.id==c.trackId && it.patterns.any { p -> p.id==c.patternId } }) }
        File(dir,"project.json").writeText(CompositionCodec.encode(copied))
    }
}
