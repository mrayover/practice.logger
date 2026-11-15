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

class ComposerC4CWorkflowTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private val inst get()=InstrumentationRegistry.getInstrumentation()
    private val device get()=UiDevice.getInstance(inst)
    private val dir get()=File(inst.targetContext.getExternalFilesDir(null),"composer-c4c").apply { mkdirs() }
    @get:Rule val failure=object: org.junit.rules.TestWatcher() {
        override fun failed(e: Throwable?,d: org.junit.runner.Description?) { device.takeScreenshot(File(dir,"failure.png")); device.dumpWindowHierarchy(File(dir,"failure.xml")) }
    }
    private fun activity(): ComposerActivity { var a: ComposerActivity?=null; inst.runOnMainSync { a=ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).filterIsInstance<ComposerActivity>().single() }; return a!! }
    private fun vm()=ViewModelProvider(activity())[ComposerViewModel::class.java]
    private fun current()=vm().project!!
    private fun tap(t: String) { compose.waitUntil(20000) { compose.onAllNodesWithText(t).fetchSemanticsNodes(false).any { !it.config.contains(SemanticsProperties.Disabled) } }; val n=compose.onNodeWithText(t); if(!n.isDisplayed()) n.performScrollTo(); n.performClick(); compose.waitForIdle() }
    private fun shot(name: String) { compose.waitForIdle(); device.waitForIdle(); Thread.sleep(300); assertTrue(device.takeScreenshot(File(dir,"$name.png"))) }
    private fun level(tag: String,value: Float) { compose.onNodeWithTag(tag).performSemanticsAction(SemanticsActions.SetProgress) { it(value) }; compose.waitForIdle() }
    private fun progress(tag: String)=compose.onNodeWithTag(tag).fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo].current
    private fun waitTag(tag: String) { compose.waitUntil(30000) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes(false).isNotEmpty() }; compose.waitForIdle() }
    private fun choose(track: CompositionTrack,title: String) { compose.onNodeWithTag("mixer-pattern-${track.id}").performClick(); tap(title) }
    private fun save() { tap("Save"); compose.waitUntil(15000) { !vm().busy && !vm().dirty } }
    @Test fun velocitySharedMixerPatternsLifecycleAndColdReopen() {
        val store=CompositionStore(File(inst.targetContext.filesDir,"composer/projects"))
        if(InstrumentationRegistry.getArguments().getString("c4cCold")=="true") {
            val exact=CompositionCodec.decode(File(dir,"project.json").readText()); assertEquals(exact,store.read(exact.id))
            inst.runOnMainSync { compose.activity.startActivity(Intent(compose.activity,ComposerActivity::class.java).putExtra(ComposerActivity.PROJECT_ID,exact.id)) }; waitTag("piano-roll")
            val audio=activity().auditionEngine!!; compose.waitUntil(30000) { audio.ready }; tap("Play"); compose.waitUntil(15000) { audio.playing && audio.nonZeroFramesWritten>0 }
            assertEquals(exact.tracks[0].patterns[0].notes,audio.playbackPlan!!.tracks[0].notes); tap("Stop"); return
        }
        device.executeShellCommand("wm size 1440x3120"); device.executeShellCommand("wm density 480")
        val tracks=listOf(0,3,8,7).mapIndexed { index,bank ->
            CompositionTrack(newCompositionId(),"Track ${index+1}",ComposerInstruments.all[bank].reference,(0..2).map { j ->
                CompositionPattern(newCompositionId(),listOf("A","B","C")[j],listOf(4,8,2)[j],if(index==0 && j==0) listOf(
                    CompositionNote(newCompositionId(),82,720,240,47),CompositionNote(newCompositionId(),81,1680,240,113),CompositionNote(newCompositionId(),80,2640,240,100))
                    else listOf(CompositionNote(newCompositionId(),if(bank==8) 36 else 48+index+j,0,960,80+j))) })
        }
        val initial=store.create(CompositionProject.empty("C4C Mixer").copy(tracks=tracks))
        inst.runOnMainSync { compose.activity.startActivity(Intent(compose.activity,ComposerActivity::class.java).putExtra(ComposerActivity.PROJECT_ID,initial.id)) }; waitTag("piano-roll")
        val audio=activity().auditionEngine!!; compose.waitUntil(30000) { audio.ready }
        val density=activity().resources.displayMetrics.density
        val roll=compose.onNodeWithTag("piano-roll")
        assertEquals(100f,progress("composer-velocity"),0f)
        val beforeDefault=vm().undoCount; level("composer-velocity",73f); assertEquals(beforeDefault,vm().undoCount)
        roll.performTouchInput { click(Offset(460*density,93*density)) }; assertEquals(73,current().tracks[0].patterns[0].notes.last().velocity)
        tap("Undo"); assertEquals(initial.tracks[0].patterns[0].notes,current().tracks[0].patterns[0].notes)
        roll.performTouchInput { advanceEventTime(400); click(Offset(146*density,93*density)) }
        assertEquals(47f,progress("composer-velocity"),0f)
        val beforeSingle=current().tracks[0].patterns[0].notes; val undoBefore=vm().undoCount
        compose.onNodeWithTag("composer-velocity").performTouchInput { swipe(Offset(width*.3f,height/2f),Offset(width*.7f,height/2f),700) }
        assertEquals(undoBefore+1,vm().undoCount)
        val single=current().tracks[0].patterns[0].notes
        assertNotEquals(47,single[0].velocity); assertEquals(beforeSingle[0].copy(velocity=single[0].velocity),single[0]); assertEquals(beforeSingle.drop(1),single.drop(1))
        tap("Undo"); assertEquals(beforeSingle,current().tracks[0].patterns[0].notes)
        tap("Actions"); tap("Select All"); compose.onNodeWithText("Velocity Mixed").assertIsDisplayed(); shot("01-loop-velocity-mixed")
        val undoMulti=vm().undoCount; level("composer-velocity",91f); assertEquals(undoMulti+1,vm().undoCount); assertTrue(current().tracks[0].patterns[0].notes.all { it.velocity==91 })
        val beforeMode=current(); tap("Play"); compose.waitUntil(15000) { audio.playing }; val firstPlan=audio.playbackPlan!!
        tap("Mixer"); waitTag("mixer-master"); assertSame(audio,activity().auditionEngine); assertSame(firstPlan,audio.playbackPlan); assertTrue(audio.playing); assertEquals(beforeMode,current()); shot("02-four-strips-master")
        tracks.forEach { compose.onNodeWithTag("mixer-pattern-${it.id}").assertIsDisplayed() }
        compose.onNodeWithTag("mixer-pattern-${tracks[1].id}").performClick(); compose.onNodeWithText("A").assertIsDisplayed(); compose.onNodeWithText("B").assertIsDisplayed(); compose.onNodeWithText("C").assertIsDisplayed(); shot("03-pattern-menu"); tap("C")
        compose.waitUntil(15000) { audio.playing && audio.playbackPlan!==firstPlan }; assertEquals(tracks[1].patterns[2].notes,audio.playbackPlan!!.tracks[1].notes)
        assertEquals(firstPlan.tracks[0],audio.playbackPlan!!.tracks[0]); assertEquals(tracks[1].patterns[2].lengthBars*initial.barTicks(),audio.playbackPlan!!.tracks[1].length); shot("04-bass-pattern-c")
        choose(tracks[2],"B"); compose.waitUntil(15000) { audio.playbackPlan!!.tracks[2].notes==tracks[2].patterns[1].notes }; assertEquals(8*initial.barTicks(),audio.playbackPlan!!.regionTicks)
        compose.onNodeWithTag("mixer-mute-${tracks[0].id}").performClick(); compose.onNodeWithTag("mixer-solo-${tracks[0].id}").performClick(); compose.onNodeWithTag("mixer-solo-${tracks[1].id}").performClick()
        level("mixer-volume-${tracks[0].id}",.37f); level("mixer-master",.23f); compose.waitUntil(5000) { audio.appliedMasterGain==.23f }
        tap("Loop"); waitTag("piano-roll"); assertSame(audio,activity().auditionEngine); assertTrue(audio.playing)
        compose.onNodeWithText("Mute ☑").assertIsDisplayed(); compose.onNodeWithText("Solo ☑").assertIsDisplayed(); assertEquals(.37f,progress("composer-volume"),.001f)
        // Selected notes and zoom/position survive the Loop/Mixer round trip.
        compose.onNodeWithText("Velocity 91").assertIsDisplayed(); tap("Mute ☑"); level("composer-volume",.62f)
        tap("Instrument 1 · Acoustic piano"); tap("2 · Electric bass"); compose.onNodeWithText("Pattern C").assertIsDisplayed(); tap("Pattern C"); tap("B")
        compose.waitUntil(15000) { audio.playbackPlan!!.tracks[1].notes==tracks[1].patterns[1].notes }
        tap("Mixer"); assertEquals(.62f,progress("mixer-volume-${tracks[0].id}"),.001f); compose.onNodeWithTag("mixer-pattern-${tracks[1].id}").assertTextContains("Pattern B")
        tap("Stop"); compose.waitUntil(10000) { !audio.playing && audio.voiceCount()==0 }; tap("Play"); compose.waitUntil(10000) { audio.playing && audio.appliedMasterGain==.23f }
        tap("Arrange"); compose.waitUntil(10000) { !audio.playing && audio.voiceCount()==0 }; tap("Mixer")
        val old=activity(); inst.runOnMainSync { old.recreate() }; waitTag("mixer-master"); val recreated=activity().auditionEngine!!; assertNotSame(audio,recreated)
        assertEquals(.23f,progress("mixer-master"),.001f); assertEquals(.62f,progress("mixer-volume-${tracks[0].id}"),.001f); compose.onNodeWithTag("mixer-pattern-${tracks[1].id}").assertTextContains("Pattern B")
        compose.waitUntil(30000) { recreated.ready }; tap("Play"); compose.waitUntil(10000) { recreated.playing && recreated.appliedMasterGain==.23f }; assertEquals(tracks[1].patterns[1].notes,recreated.playbackPlan!!.tracks[1].notes); tap("Stop")
        // Render eight tracks, including a genuinely empty channel, then one track.
        val four=current(); var eight=four
        repeat(4) { eight=ComposerManagement.addTrack(eight) }
        val last=eight.tracks.last(); eight=eight.copy(tracks=eight.tracks.map { if(it.id==last.id) it.copy(patterns=emptyList()) else it })
        val eightFinal=eight; inst.runOnMainSync { vm().edit(eightFinal) }; compose.waitForIdle(); shot("05-eight-strips")
        compose.onNodeWithTag("mixer-strip-${last.id}").performScrollTo(); compose.onNodeWithTag("mixer-pattern-${last.id}").assertIsNotEnabled(); shot("06-eight-empty-last")
        device.executeShellCommand("wm size 1080x1920"); device.executeShellCommand("wm density 480"); compose.waitForIdle(); Thread.sleep(800); shot("07-narrow-mixer")
        compose.onNodeWithTag("mixer-master").assertIsDisplayed(); val masterBounds=compose.onNodeWithTag("mixer-master").fetchSemanticsNode().boundsInWindow; assertTrue(masterBounds.height>48*density)
        inst.runOnMainSync { vm().edit(four.copy(tracks=four.tracks.take(1))) }; compose.waitForIdle(); shot("08-one-track")
        inst.runOnMainSync { vm().edit(four) }; compose.waitForIdle(); save(); val exact=current(); File(dir,"project.json").writeText(CompositionCodec.encode(exact)); assertEquals(exact,store.read(exact.id))
        tap("Play"); compose.waitUntil(10000) { recreated.playing }; inst.runOnMainSync { recreated.setForeground(false) }; compose.waitUntil(10000) { !recreated.playing && recreated.voiceCount()==0 }; inst.runOnMainSync { recreated.setForeground(true) }
        File(dir,"result.txt").writeText("PASS velocity default/single/multiple/mixed/Undo, shared audio/patterns/mix, live restart and independent lengths, platform master gain/reopen, recreation, 1/4/8 tracks and narrow layout. Physical audio acceptance remains separate. Cold reopen runs separately.\n")
    }
}
