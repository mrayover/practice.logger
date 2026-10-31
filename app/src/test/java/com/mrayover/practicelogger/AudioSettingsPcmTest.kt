package com.mrayover.practicelogger
import org.junit.Assert.*
import org.junit.Test

class AudioSettingsPcmTest {
    @Test fun everyClickSoundIsDistinctAccentedAndSilentModeRemainsZero() {
        val distinct = mutableSetOf<Int>()
        ClickSound.entries.forEach { sound ->
            val plain = ShortArray(48000); val accent = ShortArray(48000); val silent = ShortArray(48000)
            BeatPcm().render(plain,120,MetronomeConfig(sound=sound))
            BeatPcm().render(accent,120,MetronomeConfig(accent=true,sound=sound))
            BeatPcm().render(silent,120,MetronomeConfig(mode=MetronomeMode.FLASH_ONLY,sound=sound))
            assertTrue(plain.any { it.toInt()!=0 }); assertFalse(plain.contentEquals(accent)); assertTrue(silent.all { it.toInt()==0 })
            distinct.add(plain.contentHashCode())
        }
        assertEquals(4,distinct.size)
    }
    @Test fun alarmChoicesAreDistinctBoundedAndOffLeavesClickUntouched() {
        val distinct=mutableSetOf<Int>()
        AlarmSound.entries.forEach { sound ->
            val cue=CompletionPcm();cue.trigger(sound);val output=ShortArray(48000);val written=cue.mix(output)
            assertFalse(cue.active())
            if(sound==AlarmSound.OFF) { assertEquals(0,written);assertTrue(output.all { it.toInt()==0 }) }
            else { assertEquals(cue.length,written);assertTrue(output.any { it.toInt()!=0 });distinct.add(output.contentHashCode()) }
        }
        assertEquals(4,distinct.size)
    }
}
