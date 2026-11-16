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

/** Only the horizontal gesture/scroll conflict; does not repeat PCM or persistence campaigns. */
class ComposerC4DTouchTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Test fun panDragOwnsGestureAndCommitsOnce() {
        val inst=InstrumentationRegistry.getInstrumentation();val device=UiDevice.getInstance(inst)
        device.executeShellCommand("wm size 1080x1920");device.executeShellCommand("wm density 480")
        val store=CompositionStore(File(inst.targetContext.filesDir,"composer/projects"))
        var p=CompositionProject.empty("Pan touch")
        repeat(8) { p=ComposerManagement.addTrack(p) };p=store.create(p)
        inst.runOnMainSync { compose.activity.startActivity(Intent(compose.activity,ComposerActivity::class.java).putExtra(ComposerActivity.PROJECT_ID,p.id)) }
        compose.waitUntil(30000) { compose.onAllNodesWithTag("piano-roll").fetchSemanticsNodes(false).isNotEmpty() }
        compose.onNodeWithText("Mixer").performClick();compose.waitForIdle()
        var activity: ComposerActivity?=null
        inst.runOnMainSync { activity=ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).filterIsInstance<ComposerActivity>().single() }
        val vm=ViewModelProvider(activity!!)[ComposerViewModel::class.java]
        val first=p.tracks[0].id;val strip=compose.onNodeWithTag("mixer-strip-$first")
        val beforeBounds=strip.fetchSemanticsNode().boundsInWindow
        val before=vm.undoCount
        compose.onNodeWithTag("mixer-pan-$first").performTouchInput { swipe(Offset(width*.05f,height/2f),Offset(width*.95f,height/2f),600) }
        compose.waitForIdle()
        assertEquals(before+1,vm.undoCount);assertTrue(vm.project!!.tracks[0].mixer.pan>.9f)
        assertEquals(beforeBounds.left,strip.fetchSemanticsNode().boundsInWindow.left,.5f)
        inst.runOnMainSync { vm.undo() };compose.waitForIdle();assertEquals(0f,vm.project!!.tracks[0].mixer.pan)
        val dir=File(inst.targetContext.getExternalFilesDir(null),"composer-c4d-touch").apply { mkdirs() }
        assertTrue(device.takeScreenshot(File(dir,"pan-gesture-narrow.png")))
        // The surrounding area still scrolls to the eighth strip.
        compose.onNodeWithTag("mixer-strip-${p.tracks.last().id}").performScrollTo();compose.waitForIdle()
        compose.onNodeWithTag("mixer-pan-${p.tracks.last().id}").assertIsDisplayed()
        File(dir,"result.txt").writeText("PASS actual horizontal Pan drag reaches Right, one Undo, channel viewport unchanged during Pan, centered Undo and separate eight-strip scrolling.\n")
    }
}
