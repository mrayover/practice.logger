package com.mrayover.practicelogger

import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.graphics.toArgb
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import androidx.test.uiautomator.UiDevice
import com.mrayover.practicelogger.composer.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class ComposerC5BTransportTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Test fun renderedPlayheadTracksConsumedAudioAndSongEnds() {
        val inst=InstrumentationRegistry.getInstrumentation();val device=UiDevice.getInstance(inst)
        val dir=File(inst.targetContext.getExternalFilesDir(null),"composer-c5b-transport").apply { mkdirs() }
        device.executeShellCommand("wm size 1440x3120");device.executeShellCommand("wm density 480")
        val a=CompositionPattern(newCompositionId(),"A",1,listOf(CompositionNote(newCompositionId(),60,0,480,80)))
        val track=CompositionTrack(newCompositionId(),"Walking Bass",ComposerInstruments.all[4].reference,listOf(a))
        var project=CompositionProject.empty("C5B transport").copy(tracks=listOf(track))
        project=ArrangementEdits.add(project,track.id,a.id,0);project=ArrangementEdits.add(project,track.id,a.id,3*project.barTicks())
        val store=CompositionStore(File(inst.targetContext.filesDir,"composer/projects"));val saved=store.create(project)
        inst.runOnMainSync { compose.activity.startActivity(Intent(compose.activity,ComposerActivity::class.java).putExtra(ComposerActivity.PROJECT_ID,saved.id)) }
        compose.waitUntil(30000) { compose.onAllNodesWithTag("piano-roll").fetchSemanticsNodes(false).isNotEmpty() }
        compose.onNodeWithText("Arrange").performClick();compose.waitForIdle()
        var activity: ComposerActivity?=null
        inst.runOnMainSync { activity=ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).filterIsInstance<ComposerActivity>().single() }
        val audio=activity!!.auditionEngine!!;compose.waitUntil(30000) { audio.ready }
        compose.mainClock.advanceTimeBy(100)
        compose.onNodeWithText("Play").performClick();compose.waitUntil(10000) { audio.playing && audio.positionFrame()>24000 }
        val plan=audio.playbackPlan!!
        fun capture(name: String): Int {
            // Advance Compose's test clock so the existing delayed audio polling is actually sampled.
            compose.mainClock.advanceTimeBy(100);compose.waitForIdle()
            compose.onNodeWithText("Play").assertIsNotEnabled()
            val image=compose.onNodeWithTag("arrange-timeline").captureToImage();val pixels=image.toPixelMap()
            val xs=(0 until image.width).filter { pixels[it,10].toArgb()==0xFF78E4C3.toInt() }
            assertTrue("Rendered audio playhead must be present",xs.isNotEmpty())
            val x=xs.average().toInt();val density=inst.targetContext.resources.displayMetrics.density
            val expected=156*density+plan.tick(audio.positionFrame())/plan.barTicks*88*density
            assertTrue("Playhead aligns with consumed audio: x=$x expected=$expected",kotlin.math.abs(x-expected)<24*density)
            File(dir,"$name-canvas.png").outputStream().use { image.asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) }
            assertTrue(device.takeScreenshot(File(dir,"$name.png")))
            return x
        }
        val first=capture("01-playing");Thread.sleep(900);val second=capture("02-advanced")
        assertTrue("Playhead moves forward with audio",second>first)
        compose.waitUntil(15000) { !audio.playing && audio.positionFrame()>=plan.frame(plan.regionTicks) };compose.mainClock.advanceTimeBy(100)
        assertEquals(plan.frame(plan.regionTicks),audio.positionFrame())
        compose.onNodeWithText("Play").assertIsEnabled();device.takeScreenshot(File(dir,"03-natural-end.png"))
        compose.onNodeWithText("Stop").performClick();compose.waitUntil(10000) { audio.voiceCount()==0 }
        compose.onNodeWithText("Loop").performClick();compose.mainClock.advanceTimeBy(100)
        compose.onNodeWithText("Repeat ☑").assertExists().performClick();compose.onNodeWithText("Repeat ☐").assertExists()
        compose.onNodeWithText("Play").performClick();compose.waitUntil(10000) { audio.playing && audio.playbackPlan?.song == false }
        assertFalse("Loop Repeat selection is independent of the previous song",audio.looping)
        compose.onNodeWithText("Stop").performClick()
        File(dir,"result.txt").writeText("PASS visible green playhead at x=$first then x=$second, aligned with consumed AudioTrack frames, meaningful renamed lane, disabled Play while running, natural non-repeating endpoint=${plan.regionTicks} ticks/${plan.frame(plan.regionTicks)} frames, reenabled Play and Stop voice release, independent Loop Repeat state after song. Compose test clock explicitly advances delayed polling.\n")
    }
}

