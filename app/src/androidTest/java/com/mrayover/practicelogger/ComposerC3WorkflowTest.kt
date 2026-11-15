package com.mrayover.practicelogger

import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.semantics.SemanticsActions
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
import java.security.MessageDigest

class ComposerC3WorkflowTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private val instrumentation get()=InstrumentationRegistry.getInstrumentation()
    private val device get()=UiDevice.getInstance(instrumentation)
    @get:Rule val failureCapture=object : org.junit.rules.TestWatcher() {
        override fun failed(e: Throwable?,description: org.junit.runner.Description?) {
            val dir=File(instrumentation.targetContext.getExternalFilesDir(null),"composer-c3").apply { mkdirs() }
            device.takeScreenshot(File(dir,"failure.png")); device.dumpWindowHierarchy(File(dir,"failure.xml"))
        }
    }
    private fun waitText(text: String) { compose.waitUntil(20000) { compose.onAllNodesWithText(text).fetchSemanticsNodes(false).isNotEmpty() } }
    private fun tap(text: String) { waitText(text); val n=compose.onNodeWithText(text); if(!n.isDisplayed()) n.performScrollTo(); n.performClick() }
    private fun screenshot(name: String) {
        compose.waitForIdle(); device.waitForIdle()
        val dir=File(instrumentation.targetContext.getExternalFilesDir(null),"composer-c3").apply { mkdirs() }
        assertTrue(device.takeScreenshot(File(dir,"$name.png")))
    }
    @Test fun banksRealOutputLoopControlsFocusAndSavedInstruments() {
        val app=instrumentation.targetContext
        val manifest=org.json.JSONArray(app.assets.open("composer/sounds/manifest.json").bufferedReader().use { it.readText() })
        // Actual packaged banks: identity/hash, initialization, distinct velocity response,
        // sustained/chord output, MIDI extremes, independent overlapping pitch releases.
        val synth=ComposerSynth.create(); val buffer=ShortArray(480)
        try {
            assertFalse(ComposerSynth.load(synth,0,byteArrayOf(1,2,3)))
            ComposerInstruments.all.forEachIndexed { i,sound ->
                val bytes=app.assets.open("composer/sounds/${sound.asset}.sf2").use { it.readBytes() }
                assertEquals(manifest.getJSONObject(i).getLong("bytes"),bytes.size.toLong())
                val hash=MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 255) }
                assertEquals(manifest.getJSONObject(i).getString("sha256"),hash)
                assertTrue("Load ${sound.title}",ComposerSynth.load(synth,i,bytes))
                fun energy(velocity: Int): Long {
                    ComposerSynth.clear(synth,0,15)
                    ComposerSynth.on(synth,i,0,if(sound.drums) 36 else 60,velocity)
                    var sum=0L
                    repeat(80) { ComposerSynth.render(synth,buffer,0,240); for(s in buffer) sum+=kotlin.math.abs(s.toInt()) }
                    return sum
                }
                val soft=energy(40); val loud=energy(110)
                assertTrue("Audible PCM ${sound.title}",loud>10000)
                assertTrue("Velocity response ${sound.title}: $soft / $loud",loud>soft)
                if(!sound.drums) for(pitch in listOf(0,127)) {
                    ComposerSynth.clear(synth,0,15); ComposerSynth.on(synth,i,0,pitch,110)
                    var peak=0
                    repeat(80) { ComposerSynth.render(synth,buffer,0,240); for(s in buffer) peak=maxOf(peak,kotlin.math.abs(s.toInt())) }
                    assertTrue("MIDI $pitch on ${sound.title}",peak>0)
                }
                ComposerSynth.clear(synth,0,15)
            }
            val first=ComposerSynth.on(synth,0,0,60,100)
            val second=ComposerSynth.on(synth,0,0,60,100)
            ComposerSynth.off(synth,0,second)
            repeat(500) { ComposerSynth.render(synth,buffer,0,240) }
            assertTrue(ComposerSynth.voices(synth)>0)
            ComposerSynth.off(synth,0,first)
            ComposerSynth.on(synth,0,1,0,90); ComposerSynth.on(synth,0,2,127,90)
            ComposerSynth.clear(synth,0,15); assertEquals(0,ComposerSynth.voices(synth))
            ComposerSynth.render(synth,buffer,0,240); assertTrue(buffer.all { it.toInt()==0 })
        } finally { ComposerSynth.destroy(synth) }
        fun track(title: String,bank: Int,bars: Int,pitch: Int): CompositionTrack {
            val a=CompositionPattern(newCompositionId(),"A",bars,listOf(CompositionNote(newCompositionId(),pitch,0,960,100),CompositionNote(newCompositionId(),pitch,1280,160,70)))
            val b=CompositionPattern(newCompositionId(),"B",bars,a.notes.map { it.copy(id=newCompositionId(),startTick=it.startTick+80) })
            return CompositionTrack(newCompositionId(),title,if(bank==0) InstrumentReference(1,"reference-piano","default") else ComposerInstruments.all[bank].reference,listOf(a,b))
        }
        val store=CompositionStore(File(app.filesDir,"composer/projects"))
        val saved=store.create(CompositionProject.empty("C3 native ensemble",300).copy(tracks=listOf(track("Keys",0,1,60),track("Electric",1,2,64),track("Drums",8,1,36))))
        instrumentation.runOnMainSync { compose.activity.startActivity(Intent(compose.activity,ComposerActivity::class.java).putExtra(ComposerActivity.PROJECT_ID,saved.id)) }
        waitText("Pattern A")
        var editor: ComposerActivity?=null
        lateinit var vm: ComposerViewModel
        instrumentation.runOnMainSync {
            editor=ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).single() as ComposerActivity
            vm=ViewModelProvider(editor!!)[ComposerViewModel::class.java]
        }
        val engine=editor!!.auditionEngine!!
        compose.waitUntil(30000) { engine.ready }
        tap("Pattern A"); tap("B")
        tap("Instrument Keys"); tap("Electric")
        tap("Pattern A"); tap("B")
        tap("Instrument Electric"); tap("Keys"); waitText("Pattern B")
        tap("Sound Acoustic piano"); screenshot("sound-picker"); tap("Organ")
        compose.runOnIdle { assertEquals("organ",vm.project!!.tracks[0].instrument.instrumentId); assertEquals(saved.tracks[0].patterns,vm.project!!.tracks[0].patterns) }
        tap("Save"); waitText("Saved on this device")
        val exact=store.read(saved.id); assertEquals(vm.project,exact)
        // UI chord and transition to scroll must release native voices without editing.
        val d=editor!!.resources.displayMetrics.density
        val roll=compose.onNodeWithTag("piano-roll")
        fun point(x: Float,y: Float)=Offset(x*d,y*d)
        // Both viewport bounds use the same key/note transforms; no invalid rows.
        roll.performTouchInput { swipe(point(20f,80f),point(20f,5000f),500) }
        roll.performTouchInput { click(point(350f,40f)) }
        assertEquals(127,vm.project!!.tracks[0].patterns[1].notes.last().pitch)
        screenshot("highest-pitch-alignment"); tap("Undo")
        roll.performTouchInput { swipe(point(20f,80f),point(20f,-5000f),500) }
        val bottom=roll.fetchSemanticsNode().size.height/d-13f
        roll.performTouchInput { click(point(350f,bottom)) }
        assertEquals(0,vm.project!!.tracks[0].patterns[1].notes.last().pitch)
        screenshot("lowest-pitch-alignment"); tap("Undo")
        roll.performTouchInput { down(0,point(20f,70f)); down(1,point(20f,122f)) }
        compose.waitUntil(5000) { engine.voiceCount()>0 }
        roll.performTouchInput { up(1); moveTo(0,point(20f,145f),300); up(0) }
        compose.waitUntil(5000) { engine.voiceCount()==0 }
        assertEquals(exact,vm.project)
        screenshot("instruments-and-toolbar")
        tap("Play")
        compose.waitUntil(5000) { engine.playing && engine.positionFrame()>4800 }
        assertEquals(exact.tracks[0].patterns[1].notes,engine.playbackPlan!!.tracks[0].notes)
        assertEquals(exact.tracks[1].patterns[1].notes,engine.playbackPlan!!.tracks[1].notes)
        assertEquals(7680L,engine.playbackPlan!!.regionTicks)
        val start=engine.nonZeroFramesWritten
        compose.waitUntil(10000) { engine.positionFrame()>engine.playbackPlan!!.frame(15360) }
        assertTrue(engine.playing); assertTrue(engine.nonZeroFramesWritten>start)
        screenshot("multitrack-loop-playing")
        // Live mixer controls route to all banks without changing the project.
        tap("Mute ☐"); tap("Mute ☑"); tap("Solo ☐"); tap("Solo ☑")
        compose.onNodeWithTag("composer-volume").performScrollTo().performSemanticsAction(SemanticsActions.SetProgress) { it(0.35f) }
        assertEquals(exact,vm.project)
        tap("Stop"); compose.waitUntil(5000) { !engine.playing && engine.voiceCount()==0 }
        tap("Beginning"); compose.waitUntil(3000) { engine.positionFrame()==0L }
        tap("Repeat ☑"); tap("Play")
        compose.waitUntil(5000) { engine.playing }
        compose.waitUntil(10000) { !engine.playing && engine.voiceCount()==0 }
        compose.waitUntil(5000) { engine.positionFrame()>=engine.playbackPlan!!.frame(7680) }
        tap("Repeat ☐"); tap("Play"); compose.waitUntil(5000) { engine.playing }
        val manager=app.getSystemService(AudioManager::class.java)
        val focus=AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build()).setOnAudioFocusChangeListener {}.build()
        instrumentation.runOnMainSync { assertEquals(AudioManager.AUDIOFOCUS_REQUEST_GRANTED,manager.requestAudioFocus(focus)) }
        compose.waitUntil(5000) { !engine.playing && engine.voiceCount()==0 }
        instrumentation.runOnMainSync { manager.abandonAudioFocusRequest(focus) }
        assertFalse(engine.playing)
        tap("Play"); compose.waitUntil(5000) { engine.playing }
        instrumentation.runOnMainSync { editor!!.recreate() }
        compose.waitUntil(5000) { !engine.playing && engine.voiceCount()==0 }
        waitText("Pattern B")
        assertEquals(exact,store.read(saved.id))
        screenshot("recreated-instrument-assignment")
        // Saved expectations are checked after a separate force-stop by ComposerColdReopenTest.
        val dir=File(app.getExternalFilesDir(null),"composer-c3").apply { mkdirs() }
        File(dir,"cold-expected.json").writeText(CompositionCodec.encode(exact))
        tap("Back")
    }
}
