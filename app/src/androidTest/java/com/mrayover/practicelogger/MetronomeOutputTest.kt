package com.mrayover.practicelogger

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList

class MetronomeOutputTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Test fun nativeSilentPcmAndAudibleModeUseOnePlaybackClockAndStopCleanly() {
        val pulses=CopyOnWriteArrayList<MetronomePulse>();val errors=CopyOnWriteArrayList<String?>()
        val holder=arrayOfNulls<MetronomeAudio>(1)
        lateinit var engine:MetronomeAudio
        try {
            compose.runOnIdle {
                engine=MetronomeAudio(compose.activity.application,{ pulses.add(it) }) { errors.add(it) };holder[0]=engine
                engine.configure(MetronomeConfig(MetronomeMeter.THREE_FOUR,false,MetronomeMode.FLASH_ONLY));assertTrue(engine.start(300))
            }
            compose.waitUntil(15000) { pulses.any { it.sequence>=6 } }
            assertTrue(errors.isEmpty());assertEquals(0L,engine.nonZeroClickFramesWritten);assertTrue(engine.sampleFramesWritten>48000)
            val beats=pulses.filter { it.sequence>0 }.distinctBy { it.sequence }
            assertEquals(listOf(1,2,3,1,2,3),beats.take(6).map { it.beat });assertTrue(beats.all { it.config.mode==MetronomeMode.FLASH_ONLY })
            val before=engine.sampleFramesWritten
            compose.runOnIdle { engine.configure(MetronomeConfig(MetronomeMeter.SEVEN_EIGHT,true,MetronomeMode.SOUND_ONLY));engine.changeTempo(240) }
            compose.waitUntil(10000) { pulses.any { it.config.mode==MetronomeMode.SOUND_ONLY && it.sequence>6 } && engine.nonZeroClickFramesWritten>0 }
            assertTrue(engine.sampleFramesWritten>before)
            assertTrue(pulses.filter { it.config.mode==MetronomeMode.SOUND_ONLY }.all { !it.lit })
            compose.runOnIdle { engine.stop() };assertEquals(0L,pulses.last().sequence);assertFalse(pulses.last().lit)
            println("P3 NATIVE OUTPUT PASS api=${android.os.Build.VERSION.SDK_INT} silentFrames=$before audibleFrames=${engine.nonZeroClickFramesWritten}")
        } finally { compose.runOnIdle { holder[0]?.stop() } }
    }
}
