package com.mrayover.practicelogger

import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
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
import kotlin.math.abs

class ComposerC4AWorkflowTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private val inst get()=InstrumentationRegistry.getInstrumentation()
    private val device get()=UiDevice.getInstance(inst)
    private val dir get()=File(inst.targetContext.getExternalFilesDir(null),"composer-c4a").apply { mkdirs() }
    @get:Rule val failure=object: org.junit.rules.TestWatcher() {
        override fun failed(e: Throwable?,d: org.junit.runner.Description?) { device.takeScreenshot(File(dir,"failure.png")); device.dumpWindowHierarchy(File(dir,"failure.xml")) }
    }
    private fun stage(message: String) { File(dir,"progress.txt").appendText("$message\n"); android.util.Log.i("C4A",message) }
    private fun activity(): ComposerActivity { var a: ComposerActivity?=null; inst.runOnMainSync { a=ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).filterIsInstance<ComposerActivity>().single() }; return a!! }
    private fun vm()=ViewModelProvider(activity())[ComposerViewModel::class.java]
    private fun tap(t: String) { compose.waitUntil(20000) { compose.onAllNodesWithText(t).fetchSemanticsNodes(false).isNotEmpty() }; compose.onNodeWithText(t).performClick() }
    private fun shot(name: String) { compose.waitForIdle(); device.waitForIdle(); Thread.sleep(350); assertTrue(device.takeScreenshot(File(dir,"$name.png"))) }
    private fun workspace() { compose.waitUntil(30000) { compose.onAllNodesWithTag("piano-roll").fetchSemanticsNodes(false).isNotEmpty() }; compose.waitForIdle() }
    private fun precise(dial: String,value: String) {
        stage("Precise $dial starting")
        compose.onNodeWithTag("value-$dial").performClick()
        compose.onNodeWithTag("parameter-number").performClick().performTextReplacement(value); device.pressBack(); tap("Set value"); stage("Precise $dial complete")
    }
    private fun nativeSoundChecks() {
        val h=ComposerSynth.create(); val pcm=ShortArray(480)
        val results=StringBuilder()
        try {
            val bytes=inst.targetContext.assets.open("composer/sounds/organ.sf2").use { it.readBytes() }
            assertTrue(ComposerSynth.load(h,2,bytes))
            fun shape(channel: Int,s: ComposerSound?) { val v=s ?: ComposerSound(); ComposerSynth.shape(h,channel,s!=null,v.attackMs,v.releaseMs,v.cutoffHz,v.resonanceQ,v.sustain,v.pan) }
            fun render(s: ComposerSound?,channel: Int=0,offAt: Int=100,blocks: Int=300): ShortArray {
                ComposerSynth.clear(h,0,15); shape(channel,s)
                val voice=ComposerSynth.on(h,2,channel,60,110); val out=ShortArray(blocks*480)
                repeat(blocks) { i -> if(i==offAt) ComposerSynth.off(h,2,voice); ComposerSynth.render(h,pcm,0,240); pcm.copyInto(out,i*480) }
                return out
            }
            fun energy(a: ShortArray,from: Int=0,to: Int=a.size): Long = a.sliceArray(from until to).sumOf { abs(it.toInt()).toLong() }
            val basic=ComposerSound(attackMs=0f,releaseMs=100f,sustain=1f)
            val original=render(basic)
            val slow=render(basic.copy(attackMs=1500f)); assertTrue(energy(slow,0,4800)<energy(original,0,4800)/3)
            val release=render(basic.copy(releaseMs=2000f)); assertTrue(energy(release,60000)>energy(original,60000)+10000)
            val sustain=render(basic.copy(sustain=.1f),offAt=250); val held=render(basic,offAt=250)
            assertTrue(energy(sustain,48000,90000)<energy(held,48000,90000)/2)
            val low=render(basic.copy(cutoffHz=200f)); val q=render(basic.copy(cutoffHz=200f,resonanceQ=3.5f))
            assertFalse(original.contentEquals(low)); assertFalse(low.contentEquals(q)); assertTrue(energy(low)>1000)
            val left=render(basic.copy(pan=-1f)); val right=render(basic.copy(pan=1f))
            assertEquals(0L,left.filterIndexed { i,_ -> i%2==1 }.sumOf { abs(it.toInt()).toLong() })
            assertEquals(0L,right.filterIndexed { i,_ -> i%2==0 }.sumOf { abs(it.toInt()).toLong() })
            val untouched=render(null,1); shape(0,basic.copy(cutoffHz=40f,pan=-1f)); val untouchedAgain=render(null,1)
            assertArrayEquals("Channel 0 edit must not alter channel 1 using same bank",untouched,untouchedAgain)
            ComposerSynth.clear(h,0,15); shape(8,basic); ComposerSynth.on(h,2,8,60,100); ComposerSynth.on(h,2,8,64,100); assertTrue(ComposerSynth.voices(h)>=2)
            ComposerSynth.clear(h,8,15); assertEquals(0,ComposerSynth.voices(h)); ComposerSynth.render(h,pcm,0,240); assertTrue(pcm.all { it.toInt()==0 })
            results.append("PASS attack early envelope, release tail, sustain amplitude, cutoff/resonance PCM differences, stereo hard L/R, same-bank channel isolation, polyphony and clear silence.\n")
            results.append("attack early ${energy(original,0,4800)} / ${energy(slow,0,4800)}; release tails ${energy(original,60000)} / ${energy(release,60000)}\n")
        } finally { ComposerSynth.destroy(h) }
        File(dir,"native-pcm.txt").writeText(results.toString())
    }
    @Test fun editorPresetsAudioKeyboardLifecycleAndColdReopen() {
        val store=CompositionStore(File(inst.targetContext.filesDir,"composer/projects"))
        if(InstrumentationRegistry.getArguments().getString("c4aCold")=="true") {
            val exact=CompositionCodec.decode(File(dir,"project.json").readText()); assertEquals(exact,store.read(exact.id))
            val ref=exact.tracks[0].instrument
            assertEquals(ref,ComposerPresetStore(File(inst.targetContext.filesDir,"composer/presets")).read(ref.presetId).reference)
            inst.runOnMainSync { compose.activity.startActivity(Intent(compose.activity,ComposerActivity::class.java).putExtra(ComposerActivity.PROJECT_ID,exact.id)) }
            workspace(); tap("Edit Instrument"); compose.onNodeWithTag("value-Cutoff").assertTextEquals("850 Hz"); shot("09-cold-reopened-custom")
            tap("Back"); val audio=activity().auditionEngine!!; compose.waitUntil(30000) { audio.ready }; tap("Play"); compose.waitUntil(30000) { audio.playing && audio.nonZeroFramesWritten>0 }; tap("Stop")
            return
        }
        stage("Native PCM starting"); nativeSoundChecks(); stage("Native PCM passed")
        device.executeShellCommand("wm size 1440x3120"); device.executeShellCommand("wm density 480")
        device.executeShellCommand("settings put secure show_ime_with_hard_keyboard 1")
        val pattern=CompositionPattern(newCompositionId(),"A",4,listOf(CompositionNote(newCompositionId(),60,0,960,87)))
        val track=CompositionTrack(newCompositionId(),"Keys",ComposerInstruments.all[2].reference,listOf(pattern))
        val original=store.create(CompositionProject.empty("C4A synth").copy(tracks=listOf(track,track.copy(id=newCompositionId(),title="Other",patterns=emptyList()))))
        inst.runOnMainSync { compose.activity.startActivity(Intent(compose.activity,ComposerActivity::class.java).putExtra(ComposerActivity.PROJECT_ID,original.id)) }; workspace(); stage("Workspace ready")
        val audio=activity().auditionEngine!!; compose.waitUntil(30000) { audio.ready }; tap("Edit Instrument"); shot("01-factory"); stage("Factory editor captured")
        assertSame(audio,activity().auditionEngine)
        compose.onNodeWithTag("dial-Attack").performTouchInput { swipe(center,center-Offset(0f,40f),400) }
        compose.onNodeWithTag("value-Attack").assert(!hasText("10 ms"))
        precise("Attack","125"); precise("Release","1500"); precise("Cutoff","850"); precise("Resonance","2.4"); precise("Sustain","65"); precise("Pan","-0.4")
        shot("02-modified"); stage("All parameters edited"); tap("Back"); tap("Keep editing")
        tap("Save New Preset"); compose.onNodeWithTag("preset-name").performClick().performTextReplacement("Velvet organ"); shot("03-save-dialog"); device.pressBack(); tap("Save preset")
        compose.waitUntil(15000) { compose.onAllNodesWithText("Save preset").fetchSemanticsNodes(false).isEmpty() }
        stage("Preset saved"); tap("Presets ▾"); tap("Organ"); compose.onNodeWithTag("value-Cutoff").assertTextEquals("18000 Hz")
        tap("Presets ▾"); compose.onNodeWithText("My Presets").performScrollTo().assertIsDisplayed(); shot("04-my-presets")
        compose.onAllNodesWithText("Velvet organ").onLast().performScrollTo().performClick(); compose.onNodeWithTag("value-Cutoff").assertTextEquals("850 Hz")
        compose.onNodeWithContentDescription("Next octave").performClick(); shot("05-upper-range")
        compose.onNodeWithContentDescription("Previous octave").performClick()
        val range=compose.onNodeWithTag("piano-range-handle").fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsActions.CustomActions]
        compose.runOnIdle { assertTrue(range.first { it.label=="Shift up one semitone" }.action()) }; shot("05b-semitone-range")
        val down=compose.onNodeWithTag("piano-range-handle").fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsActions.CustomActions].first { it.label=="Shift down one semitone" }
        compose.runOnIdle { assertTrue(down.action()) }
        stage("Keyboard range complete"); val keyboard=compose.onNodeWithTag("instrument-keyboard"); val kb=keyboard.fetchSemanticsNode().boundsInRoot
        val c=compose.onNodeWithTag("piano-key-60",useUnmergedTree=true).fetchSemanticsNode().boundsInRoot.center
        val e=compose.onNodeWithTag("piano-key-64",useUnmergedTree=true).fetchSemanticsNode().boundsInRoot.center
        keyboard.performTouchInput { down(0,c-kb.topLeft); down(1,e-kb.topLeft) }
        compose.waitUntil(10000) { audio.voiceCount()>=2 }
        keyboard.performTouchInput { up(1); up(0) }
        compose.waitUntil(10000) { audio.voiceCount()==0 }
        stage("Chord passed"); tap("Use on Track"); workspace()
        assertEquals(original.tracks[0].patterns,vm().project!!.tracks[0].patterns); assertEquals(original.tracks[0].id,vm().project!!.tracks[0].id); assertEquals(original.tracks[1],vm().project!!.tracks[1])
        val ref=vm().project!!.tracks[0].instrument; assertTrue(validCompositionId(ref.presetId)); assertEquals(850f,ref.sound!!.cutoffHz)
        tap("Play"); compose.waitUntil(10000) { audio.playing }; assertEquals(ref.sound,audio.playbackPlan!!.tracks[0].sound); tap("Stop")
        tap("Edit Instrument"); compose.onNodeWithTag("value-Cutoff").assertTextEquals("850 Hz")
        // Ordinary recreation retains pending sound values without restarting branding.
        precise("Attack","200")
        stage("Recreation starting"); val a=activity(); inst.runOnMainSync { a.recreate() }
        compose.waitUntil(30000) { compose.onAllNodesWithTag("value-Attack").fetchSemanticsNodes(false).isNotEmpty() }
        compose.onNodeWithTag("value-Attack").assertTextEquals("200 ms"); tap("Back"); tap("Discard changes"); workspace()
        tap("Edit Instrument"); compose.onNodeWithTag("value-Attack").assertTextEquals("125 ms")
        device.executeShellCommand("wm size 720x1280"); device.executeShellCommand("wm density 320"); compose.waitForIdle(); shot("06-narrow-landscape"); stage("Narrow captured")
        val keyTop=compose.onNodeWithTag("instrument-keyboard").fetchSemanticsNode().boundsInRoot.top
        for(dial in listOf("Attack","Release","Cutoff","Resonance","Sustain","Pan")) {
            val node=compose.onNodeWithTag("value-$dial").assertIsDisplayed().fetchSemanticsNode()
            assertTrue("Unclipped $dial",node.boundsInRoot.bottom<=keyTop+1)
        }
        compose.onNodeWithTag("piano-key-60",useUnmergedTree=true).assertIsDisplayed()
        device.executeShellCommand("settings put system font_scale 1.6"); compose.waitForIdle(); shot("07-enlarged-fallback"); stage("Enlarged captured")
        compose.onNodeWithTag("value-Pan").performScrollTo().assertIsDisplayed(); tap("Back"); workspace()
        device.executeShellCommand("settings put system font_scale 1.0"); device.executeShellCommand("wm size 1440x3120"); device.executeShellCommand("wm density 480"); compose.waitForIdle()
        tap("Save"); compose.waitUntil(15000) { !vm().busy }; val saved=store.read(original.id); File(dir,"project.json").writeText(CompositionCodec.encode(saved))
        tap("Edit Instrument"); val owner=activity().auditionEngine!!; compose.waitUntil(30000) { owner.ready }
        compose.onNodeWithTag("piano-key-60",useUnmergedTree=true).performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
        compose.waitUntil(10000) { owner.voiceCount()>0 }
        val manager=inst.targetContext.getSystemService(AudioManager::class.java)
        val focus=AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE).setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build()).setOnAudioFocusChangeListener { }.build()
        assertEquals(AudioManager.AUDIOFOCUS_REQUEST_GRANTED,manager.requestAudioFocus(focus)); compose.waitUntil(10000) { owner.voiceCount()==0 }; manager.abandonAudioFocusRequest(focus)
        tap("Back"); compose.waitUntil(10000) { owner.voiceCount()==0 }; shot("08-return-composition")
        assertEquals(saved,store.read(original.id)); stage("Workflow complete")
    }
}
