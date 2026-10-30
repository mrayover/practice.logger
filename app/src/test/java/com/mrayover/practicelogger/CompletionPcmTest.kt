package com.mrayover.practicelogger

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class CompletionPcmTest {
    @Test fun shortCueIsNonSilentBoundedAndDucksWithoutChangingBeatPhase() {
        val cue=CompletionPcm(); val plain=BeatPcm(); val mixed=BeatPcm()
        cue.trigger(); var rendered=0; var audible=0; var peak=0
        repeat(100) {
            val original=ShortArray(480);val actual=ShortArray(480)
            plain.render(original,113);mixed.render(actual,113)
            rendered+=cue.mix(actual)
            audible+=actual.count { it.toInt()!=0 }; peak=maxOf(peak,actual.maxOf { abs(it.toInt()) })
            if (rendered==cue.length && it>60) assertArrayEquals(original,actual)
        }
        assertEquals(28_800,rendered);assertTrue(audible>10_000);assertTrue(peak in 10_000..32767);assertFalse(cue.active())
        val silence=ShortArray(480);assertEquals(0,cue.mix(silence));assertTrue(silence.all { it.toInt()==0 })
        cue.trigger();cue.cancel();assertFalse(cue.active())
    }
}
