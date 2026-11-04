package com.mrayover.practicelogger

import android.media.*
import android.content.Context
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.*

class PianoNativeTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val inst get() = InstrumentationRegistry.getInstrumentation()
    private fun await(label: String, check: () -> Boolean) {
        val end = System.currentTimeMillis() + 10000
        while (!check() && System.currentTimeMillis() < end) Thread.sleep(20)
        assertTrue(label, check())
    }
    @Test fun actualPolyphonicOutputBorrowsFocusAndCleansUpWithoutStoppingReference() {
        val context = inst.targetContext
        val db = BlockDatabase.appInstance(context)
        val repository = MediaLibraryRepository(db,File(context.filesDir,"media-library"))
        // A real platform-decoded reference fixture, synthesized locally without asset downloads.
        val frames = 48000 * 8
        val wav = ByteBuffer.allocate(44 + frames*2).order(ByteOrder.LITTLE_ENDIAN)
        wav.put("RIFF".toByteArray()).putInt(36+frames*2).put("WAVEfmt ".toByteArray()).putInt(16)
        wav.putShort(1).putShort(1).putInt(48000).putInt(96000).putShort(2).putShort(16)
        wav.put("data".toByteArray()).putInt(frames*2)
        repeat(frames) { wav.putShort((sin(2*PI*220*it/48000)*4000).toInt().toShort()) }
        val asset = runBlocking { repository.import("K1 coexistence.wav","audio/wav") { wav.array().inputStream() } }
        val scope = CoroutineScope(SupervisorJob()+Dispatchers.Main.immediate)
        lateinit var reference: LibraryAudioController
        lateinit var piano: PianoAudioEngine
        var pianoReady = false
        var resets = 0
        inst.runOnMainSync { reference = LibraryAudioController(context,repository,scope); reference.open(asset.id) }
        await("reference prepared") { !reference.state.value.preparing && reference.state.value.durationMs > 0 }
        try {
            inst.runOnMainSync { reference.repeatFile(true); reference.play() }
            await("reference playing") { reference.state.value.playing }
            val before = reference.state.value.positionMs
            inst.runOnMainSync {
                piano = PianoAudioEngine(context,reference) { resets++ }
                pianoReady = true
                assertTrue(piano.noteOn(1,60)); assertTrue(piano.noteOn(2,64)); assertTrue(piano.noteOn(3,67))
                assertEquals(3,piano.voiceCount())
            }
            await("actual piano PCM written while reference advances") { piano.nonZeroFramesWritten > 4800 && reference.state.value.positionMs > before + 100 }
            assertTrue(reference.state.value.playing)
            inst.runOnMainSync { piano.noteOff(1); piano.noteOff(2); piano.noteOff(3) }
            await("release tails finish") { piano.voiceCount() == 0 }
            assertTrue(reference.state.value.playing)
            inst.runOnMainSync { piano.close() }
            assertTrue("closing keyboard retains reference",reference.state.value.playing)
            inst.runOnMainSync { reference.seek(1000); reference.speed(75) }
            await("reference still valid after keyboard close") { reference.state.value.positionMs > 1100 }
            inst.runOnMainSync { piano = PianoAudioEngine(context,reference) { resets++ }; assertTrue(piano.noteOn(4,40)); reference.setForeground(false) }
            assertEquals(0,piano.voiceCount()); assertFalse(reference.state.value.playing); assertTrue(resets > 0)
            inst.runOnMainSync { reference.setForeground(true) }
            assertFalse(reference.state.value.playing)
            inst.runOnMainSync { assertFalse(piano.noteOn(5,60)); reference.play() }
            await("explicit reference restart") { reference.state.value.playing }
            inst.runOnMainSync { assertTrue(piano.noteOn(6,72)) }
            // A competing platform focus owner must stop both outputs; gain never resumes notes.
            val manager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val competing = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build())
                .setOnAudioFocusChangeListener { }.build()
            inst.runOnMainSync { manager.requestAudioFocus(competing) }
            await("reference focus interruption propagates to borrowed piano") { !reference.state.value.playing && piano.voiceCount() == 0 }
            inst.runOnMainSync { manager.abandonAudioFocusRequest(competing) }
            assertFalse(reference.state.value.playing); assertEquals(0,piano.voiceCount())
        } finally {
            inst.runOnMainSync { if (pianoReady) piano.close(); reference.dispose(); scope.cancel() }
        }
    }
    @Test fun standaloneRepetitionBackgroundAndCloseReleaseOutput() {
        lateinit var piano: PianoAudioEngine
        inst.runOnMainSync { piano = PianoAudioEngine(inst.targetContext) }
        try {
            inst.runOnMainSync { assertTrue(piano.noteOn(1,69)) }
            await("standalone output") { piano.nonZeroFramesWritten > 2400 }
            repeat(15) { index -> inst.runOnMainSync { piano.noteOff(index.toLong()+1); piano.noteOn(index.toLong()+2,60 + index%12) }; Thread.sleep(10) }
            inst.runOnMainSync { piano.stopAll() }; await("rapid repetition release") { piano.voiceCount() == 0 }
            inst.runOnMainSync { piano.noteOn(99,24); piano.setForeground(false) }
            assertEquals(0,piano.voiceCount())
            inst.runOnMainSync { piano.setForeground(true) }; assertEquals(0,piano.voiceCount())
            inst.runOnMainSync { assertTrue(piano.noteOn(100,96)); piano.close(); assertFalse(piano.noteOn(101,60)) }
            val frames = piano.framesWritten; Thread.sleep(100); assertEquals(frames,piano.framesWritten)
        } finally { inst.runOnMainSync { piano.close() } }
    }
}
