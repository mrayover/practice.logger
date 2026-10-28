package com.mrayover.practicelogger

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*
import kotlin.random.Random

class ToolLogicTest {
    private fun tone(frequency: Double, rate: Int = 48000, harmonics: Boolean = false): ShortArray = ShortArray(8192) { i ->
        val phase = 2 * PI * frequency * i / rate
        ((if (harmonics) 0.18 * sin(phase) + 0.45 * sin(2 * phase) + 0.1 * sin(3 * phase) else 0.6 * sin(phase)) * 32767).toInt().toShort()
    }
    @Test fun chromaticPitchAcrossInstrumentRangeAndSampleRates() {
        for (rate in listOf(44100, 48000)) for (frequency in listOf(55.0, 65.406, 82.407, 110.0, 220.0, 440.0, 880.0, 1318.51)) {
            val result = PitchDetector.detect(tone(frequency, rate), rate)
            assertNotNull("$frequency at $rate", result)
            assertTrue("$frequency measured ${result!!.frequency}", abs(1200 * log2(result.frequency / frequency)) < 4)
        }
    }
    @Test fun dominantHarmonicDoesNotReplaceFundamental() {
        val pitch = PitchDetector.detect(tone(110.0, harmonics = true), 48000)!!
        assertEquals("A2", pitch.note)
        assertEquals(110.0, pitch.frequency, 0.5)
    }
    @Test fun concertPitchAndCentsHaveCorrectDirection() {
        val a = PitchDetector.detect(tone(440.0), 48000)!!
        assertEquals("A4", a.note); assertEquals(0.0, a.cents, 2.0)
        for (cents in listOf(-30.0, 30.0)) {
            val pitch = PitchDetector.detect(tone(440 * 2.0.pow(cents / 1200)), 48000)!!
            assertEquals("A4", pitch.note); assertEquals(cents, pitch.cents, 3.0)
        }
    }
    @Test fun silenceQuietNoiseAndInvalidInputDoNotInventNotes() {
        assertNull(PitchDetector.detect(ShortArray(8192), 48000))
        assertNull(PitchDetector.detect(ShortArray(8192) { 1000 }, 48000))
        val random = Random(42)
        assertNull(PitchDetector.detect(ShortArray(8192) { random.nextInt(-18000, 18000).toShort() }, 48000))
        assertNull(PitchDetector.detect(ShortArray(8192) { (tone(440.0)[it] / 1000).toShort() }, 48000))
        assertNull(PitchDetector.detect(ShortArray(5), 48000))
        assertNull(PitchDetector.detect(tone(440.0), 8000))
    }
    @Test fun stopwatchCountsOnlyStartedIntervalsAndResetStopsIt() {
        var now = 1000L
        val clock = ToolClock { now }
        now += 5000; assertEquals(0, clock.elapsed)
        clock.start(); now += 1234; clock.start(); assertEquals(1234, clock.elapsed)
        clock.pause(); now += 90000; assertEquals(1234, clock.elapsed)
        clock.start(); now += 60000; clock.pause(); assertEquals(61234, clock.elapsed)
        clock.reset(); assertFalse(clock.running); assertEquals(0, clock.elapsed)
    }
}
