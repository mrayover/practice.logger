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

class ComposerC5AWorkflowTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private val inst get()=InstrumentationRegistry.getInstrumentation()
    private val device get()=UiDevice.getInstance(inst)
    private val dir get()=File(inst.targetContext.getExternalFilesDir(null),"composer-c5a").apply { mkdirs() }
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
        if(InstrumentationRegistry.getArguments().getString("c5aCold")=="true") {
            val expected=CompositionCodec.decode(File(dir,"project.json").readText())
            assertEquals(expected,store.read(expected.id)); open(expected.id); assertEquals(expected,current()); shot("09-cold-reopen"); return
        }
        device.executeShellCommand("wm size 1440x3120"); device.executeShellCommand("wm density 480")
        val a=CompositionPattern(newCompositionId(),"A",2,listOf(CompositionNote(newCompositionId(),60,0,960,80)))
        val piano=CompositionTrack(newCompositionId(),"Melody",ComposerInstruments.all[0].reference,listOf(a))
        val bass=CompositionTrack(newCompositionId(),"Foundation",ComposerInstruments.all[3].reference,listOf(a.copy(id=newCompositionId(),title="B",notes=a.notes.map { it.copy(id=newCompositionId(),pitch=43) })))
        val initial=store.create(CompositionProject.empty("C5A arrangement").copy(tracks=listOf(piano,bass)))
        open(initial.id); shot("01-empty")
        val density=inst.targetContext.resources.displayMetrics.density
        fun point(bar: Int,lane: Int=0)=Offset((156+bar*88+20)*density,(36+lane*72+26)*density)
        fun chooseBar(bar: Int) { compose.onNodeWithTag("arrange-timeline").performTouchInput { click(Offset((156+bar*88+12)*density,18*density)) } }
        chooseBar(0); tap("Add Pattern"); tap("1 · Acoustic piano — Melody"); tap("A · 2 bars")
        assertEquals(1,current().clips.size); shot("02-piano")
        chooseBar(4); tap("Add Pattern"); tap("2 · Electric bass — Foundation"); tap("B · 2 bars")
        assertEquals(4*initial.barTicks(),current().clips[1].startTick); shot("03-two-lanes")
        val beforeMove=current(); val count=vm().undoCount
        compose.onNodeWithTag("arrange-timeline").performTouchInput { down(point(0)); moveTo(point(1),500); advanceEventTime(150) }
        assertEquals(beforeMove,current()); shot("04-drag-preview")
        compose.onNodeWithTag("arrange-timeline").performTouchInput { up() }
        assertEquals(beforeMove.clips[0].copy(startTick=initial.barTicks()),current().clips[0]); assertEquals(count+1,vm().undoCount)
        tap("Undo"); assertEquals(beforeMove,current())
        compose.onNodeWithTag("arrange-timeline").performTouchInput { swipe(point(0),point(1),500) }
        val moved=current(); tap("Delete Clip"); assertEquals(1,current().clips.size); shot("05-last-piano-deleted")
        tap("Undo"); assertEquals(moved,current()); shot("06-restored")
        val audio=activity().auditionEngine
        tap("Loop"); compose.onNodeWithTag("piano-roll").assertIsDisplayed(); assertSame(audio,activity().auditionEngine)
        tap("Mixer"); assertEquals(moved,current()); tap("Arrange"); assertSame(audio,activity().auditionEngine)
        // Hand navigation owns scrolling, even when the pointer starts on a clip.
        tap("Select ✓")
        compose.onNodeWithTag("arrange-timeline").performTouchInput { swipe(point(2),point(0),500) }
        assertEquals(moved,current()); shot("07-scrolled-fixed-labels")
        save(); val saved=current(); assertEquals(saved,store.read(saved.id))
        tap("Return"); open(saved.id); assertEquals(saved,current())
        val originalActivity=activity(); inst.runOnMainSync { originalActivity.recreate() }
        compose.waitUntil(30000) { compose.onAllNodesWithTag("arrange-timeline").fetchSemanticsNodes(false).isNotEmpty() }
        assertEquals(saved,current())
        device.executeShellCommand("wm size 1080x1920"); device.executeShellCommand("wm density 480")
        compose.waitForIdle(); shot("08-narrow")
        assertTrue(compose.onNodeWithTag("arrange-timeline").fetchSemanticsNode().boundsInRoot.height>100*density)
        File(dir,"project.json").writeText(CompositionCodec.encode(saved))
    }
}
