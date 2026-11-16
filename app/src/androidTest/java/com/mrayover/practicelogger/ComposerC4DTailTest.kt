package com.mrayover.practicelogger

import android.content.Intent
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

/** Resume only the previously unexecuted narrow/save/cleanup tail; cold reopening follows. */
class ComposerC4DTailTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Test fun visibleNarrowFaderFinalSaveAndCleanup() {
        val inst=InstrumentationRegistry.getInstrumentation();val device=UiDevice.getInstance(inst)
        device.executeShellCommand("wm size 1080x1920");device.executeShellCommand("wm density 480")
        val dir=File(inst.targetContext.getExternalFilesDir(null),"composer-c4d").apply { mkdirs() }
        val store=CompositionStore(File(inst.targetContext.filesDir,"composer/projects"))
        val tracks=(0..7).map { i ->
            val patterns=(0..1).map { j -> CompositionPattern(newCompositionId(),if(j==0) "A" else "B",if(i%2==0) 4 else 8,
                listOf(CompositionNote(newCompositionId(),48+i+j,0,960,80+j))) }
            CompositionTrack(newCompositionId(),"Track ${i+1}",ComposerInstruments.all[i].reference,patterns,
                TrackMixer(if(i==0) .37f else 1f,when(i) { 0 -> -1f;2 -> 1f;else -> 0f },i==3,i==1,patterns[if(i==1) 1 else 0].id))
        }
        val p=store.create(CompositionProject.empty("C4D tail").copy(formatVersion=3,masterVolume=.23f,tracks=tracks))
        inst.runOnMainSync { compose.activity.startActivity(Intent(compose.activity,ComposerActivity::class.java).putExtra(ComposerActivity.PROJECT_ID,p.id)) }
        compose.waitUntil(30000) { compose.onAllNodesWithTag("piano-roll").fetchSemanticsNodes(false).isNotEmpty() }
        compose.onNodeWithText("Mixer").performClick();compose.waitForIdle()
        var a: ComposerActivity?=null
        inst.runOnMainSync { a=ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).filterIsInstance<ComposerActivity>().single() }
        val vm=ViewModelProvider(a!!)[ComposerViewModel::class.java]
        val last=tracks.last().id
        compose.onNodeWithTag("mixer-strip-$last").performScrollTo();compose.waitForIdle()
        compose.onNodeWithTag("mixer-volume-$last").assertIsDisplayed()
        val bounds=compose.onNodeWithTag("mixer-volume-$last").fetchSemanticsNode().boundsInWindow
        assertTrue("Visible narrow fader must retain48dp",bounds.height>=48*a!!.resources.displayMetrics.density)
        compose.onNodeWithTag("mixer-master").assertIsDisplayed();assertTrue(device.takeScreenshot(File(dir,"05-narrow-visible-eighth-master.png")))
        inst.runOnMainSync { vm.edit(p.copy(tracks=tracks.take(4))) };compose.waitForIdle()
        compose.onNodeWithText("Save").performClick();compose.waitUntil(15000) { !vm.busy && !vm.dirty }
        val exact=vm.project!!;assertEquals(exact,store.read(exact.id));File(dir,"project.json").writeText(CompositionCodec.encode(exact))
        val audio=a!!.auditionEngine!!;compose.waitUntil(30000) { audio.ready }
        fun play() { compose.onNodeWithText("Play").performClick();compose.waitUntil(10000) { audio.playing && audio.appliedMasterGain==.23f } }
        play();compose.onNodeWithText("Stop").performClick();compose.waitUntil(10000) { !audio.playing && audio.voiceCount()==0 };play()
        inst.runOnMainSync { audio.setForeground(false) };compose.waitUntil(10000) { !audio.playing && audio.voiceCount()==0 };inst.runOnMainSync { audio.setForeground(true) }
        File(dir,"tail-result.txt").writeText("PASS visible eighth-strip narrow fader ${bounds.height/a!!.resources.displayMetrics.density}dp, pinned Master, final exact Save, master reapplied after output reopening and interruption voice cleanup. Cold process follows.\n")
    }
}
