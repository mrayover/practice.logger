package com.mrayover.practicelogger

import org.junit.Assert.*
import org.junit.Test

class MeterPcmTest {
    @Test fun allMetersCountEveryBeatAndWrapWithAccentOff() {
        for(meter in MetronomeMeter.entries) {
            val events=mutableListOf<MetronomeBeat>();val pcm=BeatPcm();val config=MetronomeConfig(meter=meter)
            pcm.render(ShortArray(24000*meter.beats*2),120,config,events::add)
            assertEquals(meter.beats*2,events.size)
            assertEquals(List(meter.beats*2) { it%meter.beats+1 },events.map { it.beat })
            assertTrue(events.all { !it.config.accent })
        }
    }
    @Test fun accentOnlyChangesFirstBeatAndOffClicksAreIdentical() {
        val straight=ShortArray(24000*4);val accented=ShortArray(straight.size)
        BeatPcm().render(straight,120,MetronomeConfig(meter=MetronomeMeter.THREE_FOUR))
        BeatPcm().render(accented,120,MetronomeConfig(meter=MetronomeMeter.THREE_FOUR,accent=true))
        assertArrayEquals(straight.copyOfRange(0,480),straight.copyOfRange(24000,24480))
        assertFalse(straight.copyOfRange(0,480).contentEquals(accented.copyOfRange(0,480)))
        assertArrayEquals(straight.copyOfRange(24000,24480),accented.copyOfRange(24000,24480))
        assertArrayEquals(accented.copyOfRange(0,480),accented.copyOfRange(72000,72480))
    }
    @Test fun allOutputModesShareClockAndSilentModeContainsNoHiddenClick() {
        val events=mutableMapOf<MetronomeMode,List<MetronomeBeat>>();val data=mutableMapOf<MetronomeMode,ShortArray>()
        for(mode in MetronomeMode.entries) {
            val beats=mutableListOf<MetronomeBeat>();val pcm=ShortArray(48000)
            BeatPcm().render(pcm,120,MetronomeConfig(mode=mode),beats::add);data[mode]=pcm;events[mode]=beats
        }
        assertArrayEquals(data[MetronomeMode.SOUND_FLASH],data[MetronomeMode.SOUND_ONLY])
        assertTrue(data[MetronomeMode.FLASH_ONLY]!!.all { it==0.toShort() })
        assertEquals(events[MetronomeMode.SOUND_FLASH]!!.map { it.frame },events[MetronomeMode.FLASH_ONLY]!!.map { it.frame })
        assertTrue(events[MetronomeMode.FLASH_ONLY]!!.all { it.config.mode.flash && !it.config.mode.sound })
    }
    @Test fun meterAccentAndModeApplyAtNextBeatWithoutResettingPhase() {
        val pcm=BeatPcm();val events=mutableListOf<MetronomeBeat>()
        pcm.render(ShortArray(6000),120,MetronomeConfig(),events::add)
        val updated=MetronomeConfig(MetronomeMeter.FIVE_FOUR,true,MetronomeMode.FLASH_ONLY)
        pcm.render(ShortArray(24000),120,updated,events::add)
        assertEquals(listOf(0L,24000L),events.map { it.frame });assertEquals(listOf(1,1),events.map { it.beat })
        assertEquals(updated,events.last().config)
    }
    @Test fun bpmChangesKeepFractionalBeatPhaseAndDoNotCreateExtraBeat() {
        val pcm=BeatPcm();val events=mutableListOf<MetronomeBeat>()
        pcm.render(ShortArray(12000),120,onBeat=events::add)
        pcm.render(ShortArray(24001),240,onBeat=events::add)
        assertEquals(3,events.size)
        assertTrue(kotlin.math.abs(events[1].frame-18000L)<=1)
        assertTrue(kotlin.math.abs(events[2].frame-30000L)<=1)
        assertEquals(listOf(1,2,3),events.map { it.beat })
    }
    @Test fun chunkingAndCompletionMixNeverAlterBarOrBeatClock() {
        val config=MetronomeConfig(MetronomeMeter.SEVEN_EIGHT,true)
        val whole=ShortArray(96000);val one=mutableListOf<MetronomeBeat>();BeatPcm().render(whole,113,config,one::add)
        val pcm=BeatPcm();val split=ShortArray(96000);val many=mutableListOf<MetronomeBeat>()
        repeat(200) { val chunk=ShortArray(480);pcm.render(chunk,113,config,many::add);chunk.copyInto(split,it*480) }
        assertArrayEquals(whole,split);assertEquals(one,many)
        val silent=BeatPcm();val cue=CompletionPcm();cue.trigger();val events=mutableListOf<MetronomeBeat>()
        repeat(100) { val chunk=ShortArray(480);silent.render(chunk,120,MetronomeConfig(mode=MetronomeMode.FLASH_ONLY),events::add);assertTrue(chunk.all { it==0.toShort() });cue.mix(chunk) }
        assertEquals(listOf(0L,24000L),events.map { it.frame })
    }
}
