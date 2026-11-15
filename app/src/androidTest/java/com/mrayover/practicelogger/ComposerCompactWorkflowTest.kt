package com.mrayover.practicelogger

import android.content.Intent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.semantics.SemanticsProperties
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

class ComposerCompactWorkflowTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private val inst get()=InstrumentationRegistry.getInstrumentation()
    private val device get()=UiDevice.getInstance(inst)
    private val dir get()=File(inst.targetContext.getExternalFilesDir(null),"composer-compact").apply { mkdirs() }
    @get:Rule val capture=object: org.junit.rules.TestWatcher() {
        override fun failed(e: Throwable?,d: org.junit.runner.Description?) { device.takeScreenshot(File(dir,"failure.png")); device.dumpWindowHierarchy(File(dir,"failure.xml")) }
        override fun finished(d: org.junit.runner.Description?) { device.executeShellCommand("wm size reset"); device.executeShellCommand("wm density reset"); device.executeShellCommand("settings put system font_scale 1.0") }
    }
    private fun waitText(s: String) { compose.waitUntil(25000) { compose.onAllNodesWithText(s).fetchSemanticsNodes(false).isNotEmpty() } }
    private fun tap(s: String) { waitText(s); compose.onNodeWithText(s).assertIsDisplayed().performClick() }
    private fun shot(s: String) { compose.waitForIdle(); device.waitForIdle(); assertTrue(device.takeScreenshot(File(dir,"$s.png"))) }
    private fun activity(): ComposerActivity { var a: ComposerActivity?=null; val get = { a=ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).filterIsInstance<ComposerActivity>().single() }; if(android.os.Looper.myLooper()==android.os.Looper.getMainLooper()) get() else inst.runOnMainSync { get() }; return requireNotNull(a) }
    private fun vm(): ComposerViewModel { var result: ComposerViewModel?=null; val get = { result=ViewModelProvider(activity())[ComposerViewModel::class.java] }; if(android.os.Looper.myLooper()==android.os.Looper.getMainLooper()) get() else inst.runOnMainSync { get() }; return requireNotNull(result) }
    private fun awaitWorkspace() { compose.waitUntil(25000) { compose.onAllNodesWithTag("piano-roll").fetchSemanticsNodes(false).isNotEmpty() } }
    private fun dimensions(size: String,density: Int) { device.executeShellCommand("wm size $size"); device.executeShellCommand("wm density $density"); device.waitForIdle(); awaitWorkspace(); compose.waitForIdle() }
    private fun fit(name: String, half: Boolean=true) {
        val rows=listOf("composer-application-row","composer-context-row","composer-performance-row","composer-editing-row").map { compose.onNodeWithTag(it).fetchSemanticsNode() }
        val root=compose.onNodeWithTag("piano-roll").fetchSemanticsNode().boundsInRoot
        rows.forEachIndexed { i,n ->
            assertFalse(n.config.contains(SemanticsProperties.HorizontalScrollAxisRange))
            assertTrue(n.boundsInRoot.left>=0); assertTrue(n.boundsInRoot.right<=root.right+1)
            if(i>0) assertTrue(rows[i-1].boundsInRoot.bottom<=n.boundsInRoot.top+1)
        }
        if(half) assertTrue("Canvas should occupy half usable screen: $root",root.height>=(root.bottom+20-rows.first().boundsInRoot.top)/2)
        File(dir,"$name-bounds.txt").writeText("rows=${rows.map { it.boundsInRoot }}\ncanvas=$root\ncanvas fraction=${root.height/(root.bottom+20)}")
        shot(name)
    }
    @Test fun compactRowsMenusSaveAsAndIndependentReopen() {
        val store=CompositionStore(File(inst.targetContext.filesDir,"composer/projects"))
        if(InstrumentationRegistry.getArguments().getString("compactCold")=="true") {
            val original=CompositionCodec.decode(File(dir,"original.json").readText()); val copy=CompositionCodec.decode(File(dir,"copy.json").readText())
            assertEquals(original,store.read(original.id)); assertEquals(copy,store.read(copy.id))
            for(p in listOf(original,copy)) {
                inst.runOnMainSync { compose.activity.startActivity(Intent(compose.activity,ComposerActivity::class.java).putExtra(ComposerActivity.PROJECT_ID,p.id)) }
                awaitWorkspace(); compose.runOnIdle { assertEquals(p,vm().project) }; tap("Return")
            }
            return
        }
        device.executeShellCommand("wm size 1440x3120"); device.executeShellCommand("wm density 480")
        val pattern=CompositionPattern(newCompositionId(),"A",4,listOf(CompositionNote(newCompositionId(),80,0,720,100),CompositionNote(newCompositionId(),83,960,240,87)))
        val track=CompositionTrack(newCompositionId(),"Piano 1",ComposerInstruments.all[0].reference,listOf(pattern,pattern.copy(id=newCompositionId(),title="B",notes=emptyList())))
        val original=store.create(CompositionProject.empty("Compact original",120,7,8).copy(tracks=listOf(track)))
        inst.runOnMainSync { compose.activity.startActivity(Intent(compose.activity,ComposerActivity::class.java).putExtra(ComposerActivity.PROJECT_ID,original.id)) }
        awaitWorkspace(); fit("01-primary-loop")
        tap("Instrument 1"); shot("02-instrument-menu"); device.pressBack()
        tap("Pattern A"); shot("03-pattern-menu"); tap("B"); tap("Pattern B"); tap("A")
        tap("Actions"); shot("04-actions-menu"); tap("Select All"); tap("Actions"); tap("Copy"); tap("Actions"); tap("Paste"); tap("Undo")
        compose.runOnIdle { assertEquals(original.tracks,vm().project!!.tracks) }
        val audio=activity().auditionEngine
        compose.waitUntil(30000) { audio?.ready==true }; tap("Play"); compose.waitUntil(10000) { audio?.playing==true }
        tap("Grid 1/16"); tap("1/8"); assertSame(audio,activity().auditionEngine); assertTrue(audio!!.playing)
        tap("Repeat ☑"); tap("Repeat ☐"); tap("Mute ☐"); tap("Mute ☑"); tap("Solo ☐"); tap("Solo ☑")
        compose.onNodeWithTag("composer-volume").performTouchInput { click(Offset(width*.7f,height/2f)) }
        tap("Stop"); compose.waitUntil(10000) { !audio.playing }
        tap("↤"); tap("Beginning"); tap("Tool: Draw"); tap("Hand"); compose.onNodeWithTag("piano-roll").performTouchInput { swipe(Offset(20f,height*.8f),Offset(20f,height*.3f)) }
        tap("Tool: Hand"); tap("Draw"); tap("Zoom +"); tap("Zoom −"); tap("Snap ☑"); tap("Snap ☐")
        compose.runOnIdle { val current=vm().project!!; val active=current.tracks.single().patterns.first(); vm().edit(current.replacePattern(active.copy(notes=active.notes + CompositionNote(newCompositionId(),77,1920,240,99))).copy(tempoBpm=143)) }
        tap("Save As"); shot("05-save-as-dialog")
        compose.onNodeWithTag("composer-copy-title").performClick().performTextReplacement("Compact copy")
        shot("06-save-as-keyboard"); device.pressBack(); tap("Create copy")
        compose.waitUntil(15000) { compose.onAllNodesWithText("Create copy").fetchSemanticsNodes(false).isEmpty() }
        val copy=vm().project!!; assertNotEquals(original.id,copy.id); assertEquals(143,copy.tempoBpm); assertEquals(3,copy.tracks.single().patterns.first().notes.size); assertEquals(99,copy.tracks.single().patterns.first().notes.last().velocity); assertEquals(original,store.read(original.id))
        assertEquals(0,vm().undoCount); assertFalse(vm().dirty)
        compose.runOnIdle { vm().edit(copy.copy(tempoBpm=145)) }; tap("Save")
        compose.waitUntil(15000) { !vm().busy }; val saved=store.read(copy.id); assertEquals(145,saved.tempoBpm); assertEquals(original,store.read(original.id))
        File(dir,"original.json").writeText(CompositionCodec.encode(original)); File(dir,"copy.json").writeText(CompositionCodec.encode(saved))
        val copyAudio=activity().auditionEngine; tap("Arrange"); shot("07-arrange"); tap("Loop"); assertSame(copyAudio,activity().auditionEngine)
        val editor=activity(); inst.runOnMainSync { editor.recreate() }; awaitWorkspace(); assertEquals(saved,vm().project)
        dimensions("1080x1920",480); fit("08-narrow-loop")
        dimensions("1600x2560",320); fit("09-large-loop")
        dimensions("1080x1920",480); device.executeShellCommand("settings put system font_scale 1.6"); device.waitForIdle(); awaitWorkspace(); fit("10-large-text",false)
        compose.onNode(hasText("More ▾") and hasAnyAncestor(hasTestTag("composer-performance-row"))).performClick()
        compose.onNodeWithTag("composer-volume").assertIsDisplayed(); shot("10b-overflow-volume"); device.pressBack()
        device.executeShellCommand("settings put system font_scale 1.0"); dimensions("1440x3120",480)
        compose.runOnIdle { vm().edit(vm().project!!.copy(tempoBpm=146)) }; tap("Return"); tap("Keep editing"); tap("Undo")
        tap("Return")
        inst.runOnMainSync { compose.activity.startActivity(Intent(compose.activity,ComposerLibraryActivity::class.java)) }
        waitText("Compact original"); compose.onNodeWithText("Compact original").assertIsDisplayed(); compose.onNodeWithText("Compact copy").assertIsDisplayed(); shot("11-library-copies")
    }
}
