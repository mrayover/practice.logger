package com.mrayover.practicelogger

import android.app.Activity
import android.content.Intent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import com.mrayover.practicelogger.composer.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class ComposerPianoRollWorkflowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val device get() = UiDevice.getInstance(instrumentation)
    @get:Rule val failureCapture = object : org.junit.rules.TestWatcher() {
        override fun failed(e: Throwable?, description: org.junit.runner.Description?) {
            val dir=File(instrumentation.targetContext.getExternalFilesDir(null),"composer-c2").apply { mkdirs() }
            device.takeScreenshot(File(dir,"failure.png")); device.dumpWindowHierarchy(File(dir,"failure.xml"))
            if (::vm.isInitialized) File(dir,"failure-project.json").writeText(CompositionCodec.encode(vm.project!!))
        }
    }
    private lateinit var vm: ComposerViewModel
    private fun waitText(text: String) {
        compose.waitUntil(15000) { compose.onAllNodesWithText(text).fetchSemanticsNodes(false).isNotEmpty() }
    }
    private fun tap(text: String) {
        waitText(text)
        val n=compose.onNodeWithText(text)
        if (!n.isDisplayed()) n.performScrollTo()
        n.performClick()
    }
    private fun notes() = vm.project!!.tracks[0].patterns[0].notes
    private fun screenshot(name: String) {
        compose.waitForIdle(); device.waitForIdle()
        val dir=File(compose.activity.getExternalFilesDir(null),"composer-c2").apply { mkdirs() }
        val file=File(dir,"$name.png")
        assertTrue(device.takeScreenshot(file))
        val bitmap=android.graphics.BitmapFactory.decodeFile(file.path)
        val bounds=compose.onNodeWithTag("piano-roll").fetchSemanticsNode().boundsInWindow
        val d=compose.activity.resources.displayMetrics.density
        var lightKeys=0
        for(y in (bounds.top+32*d).toInt() until (bounds.bottom-4*d).toInt() step 4) {
            val pixel=bitmap.getPixel((bounds.left+20*d).toInt(),y)
            if(android.graphics.Color.red(pixel)>140 && android.graphics.Color.green(pixel)>140 && android.graphics.Color.blue(pixel)>140) lightKeys++
        }
        assertTrue("Rendered keyboard must remain visible after navigation/recreation ($name)",lightKeys>20)
    }
    @Test fun touchEditingClipboardUndoAndExactSave() {
        val store=CompositionStore(File(compose.activity.filesDir,"composer/projects"))
        val saved=store.create(CompositionProject.empty("C2 native etude"))
        instrumentation.runOnMainSync { compose.activity.startActivity(Intent(compose.activity,ComposerActivity::class.java).putExtra(ComposerActivity.PROJECT_ID,saved.id)) }
        waitText("Create piano patterns A/B")
        device.wait(Until.findObject(By.text("Got it")),1000)?.click()
        var editor: ComposerActivity?=null
        instrumentation.runOnMainSync {
            editor=ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).single() as ComposerActivity
            vm=ViewModelProvider(editor!!)[ComposerViewModel::class.java]
        }
        tap("Create piano patterns A/B")
        compose.onNodeWithTag("piano-roll").assertIsDisplayed()
        val d=editor!!.resources.displayMetrics.density
        fun point(x: Float,y: Float)=Offset(x*d,y*d)
        val roll=compose.onNodeWithTag("piano-roll")
        roll.performTouchInput { click(point(142f,93f)) }
        compose.runOnIdle { assertEquals(1,notes().size); assertEquals(82,notes().single().pitch); assertEquals(720L,notes().single().startTick); assertEquals(240L,notes().single().durationTick) }
        val id=notes().single().id
        roll.performTouchInput { click(point(146f,93f)) }
        assertEquals(id,notes().single().id)
        // Body move: 480 ticks right, two semitones down; one completed undo operation.
        val beforeMoveUndo=vm.undoCount
        roll.performTouchInput { swipe(point(145f,93f),point(197.8f,145f),600) }
        compose.runOnIdle { assertEquals(1200L,notes().single().startTick); assertEquals(80,notes().single().pitch); assertEquals(beforeMoveUndo+1,vm.undoCount) }
        // Right edge has an expanded hit area even at the default short note width.
        roll.performTouchInput { swipe(point(219f,145f),point(271.8f,145f),600) }
        compose.runOnIdle { assertEquals(720L,notes().single().durationTick) }
        tap("Undo"); assertEquals(240L,notes().single().durationTick)
        tap("Undo"); assertEquals(720L,notes().single().startTick); assertEquals(82,notes().single().pitch)
        // Double tap deletes and Undo restores the original stable note identity.
        roll.performTouchInput { doubleClick(point(146f,93f)) }
        compose.runOnIdle { assertTrue(notes().isEmpty()) }
        tap("Undo"); assertEquals(id,notes().single().id)
        // Add a second voice, select both, copy across A/B, preserve exact values.
        roll.performTouchInput { advanceEventTime(400); click(point(248f,119f)) }
        assertEquals(2,notes().size)
        val unchanged=notes().toList()
        tap("Grid 1/16"); tap("1/32T")
        tap("Duration 1/16"); tap("1/8T")
        tap("Snap ☑")
        roll.performTouchInput { advanceEventTime(400); click(point(200f,171f)) }
        compose.runOnIdle { val free=notes().last(); assertEquals(320L,free.durationTick); assertTrue(free.startTick % 80 != 0L) }
        tap("Undo"); assertEquals(unchanged,notes())
        tap("Snap ☐"); tap("Grid 1/32T"); tap("1/16"); tap("Duration 1/8T"); tap("1/16")
        assertEquals(unchanged,notes())
        tap("Tool: Draw")
        roll.performTouchInput { swipe(point(130f,75f),point(300f,135f),600) }
        tap("Delete selected"); assertTrue(notes().isEmpty())
        tap("Undo"); assertEquals(unchanged,notes())
        val source=notes().toList()
        tap("Select All"); tap("Copy"); tap("Pattern A"); tap("B")
        roll.performTouchInput { click(point(167.6f,14f)) }
        tap("Paste")
        compose.runOnIdle {
            val b=vm.project!!.tracks[0].patterns[1].notes
            assertEquals(2,b.size); assertEquals(960L,b.minOf { it.startTick })
            assertEquals(source.map { it.pitch },b.map { it.pitch }); assertEquals(source.map { it.velocity },b.map { it.velocity })
            assertEquals(source.map { it.durationTick },b.map { it.durationTick }); assertTrue(b.none { it.id in source.map { n -> n.id } })
        }
        screenshot("cross-pattern-selected")
        tap("Undo"); assertTrue(vm.project!!.tracks[0].patterns[1].notes.isEmpty())
        tap("Paste")
        // Two fingers cancel an in-flight edit and navigate; they never insert or commit a move.
        val beforeNav=vm.project
        roll.performTouchInput {
            down(0,point(190f,93f)); down(1,point(300f,145f))
            moveTo(0,point(170f,82f)); moveTo(1,point(340f,165f))
            up(1); up(0)
        }
        compose.runOnIdle { assertEquals(beforeNav,vm.project) }
        // Keyboard chord and cancellation use actual native pointer IDs.
        roll.performTouchInput { down(0,point(20f,70f)); down(1,point(20f,122f)); advanceEventTime(250); up(1); up(0) }
        assertEquals(beforeNav,vm.project)
        compose.waitUntil(3000) { editor!!.auditionEngine!!.nonZeroFramesWritten > 0 && editor!!.auditionEngine!!.voiceCount() == 0 }
        screenshot("piano-roll-landscape")
        tap("+ Instrument"); tap("Paste")
        compose.runOnIdle {
            val copies=vm.project!!.tracks[1].patterns[0].notes
            assertEquals(2,copies.size); assertEquals(0L,copies.minOf { it.startTick })
            assertEquals(source.map { it.pitch },copies.map { it.pitch }); assertEquals(source.map { it.velocity },copies.map { it.velocity })
        }
        tap("Undo"); assertTrue(vm.project!!.tracks[1].patterns[0].notes.isEmpty())
        tap("Undo"); assertEquals(1,vm.project!!.tracks.size)
        tap("Pattern A"); tap("B")
        val unsaved=vm.project
        instrumentation.runOnMainSync { editor!!.recreate() }
        waitText("Pattern B")
        instrumentation.runOnMainSync {
            editor=ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).single() as ComposerActivity
            vm=ViewModelProvider(editor!!)[ComposerViewModel::class.java]
        }
        compose.runOnIdle { assertEquals(unsaved,vm.project); assertTrue(vm.dirty) }
        tap("Save"); waitText("Saved on this device")
        val exact=store.read(saved.id)
        assertEquals(vm.project,exact)
        instrumentation.runOnMainSync { editor!!.recreate() }
        waitText("Pattern B")
        screenshot("recreated-saved-notes")
        assertEquals(exact,store.read(saved.id))
        tap("Back")
        // This directly launched test returns to MainActivity; C1 workflow covers library navigation.
        val coldDir=File(compose.activity.getExternalFilesDir(null),"composer-c2").apply { mkdirs() }
        File(coldDir,"cold-expected.json").writeText(CompositionCodec.encode(exact))
    }
}
