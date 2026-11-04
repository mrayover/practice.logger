package com.mrayover.practicelogger

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class PianoPcmTest {
    @Test fun equalTemperamentAcrossRegisters() {
        assertEquals(440.0, pianoFrequency(69), .000001)
        assertEquals(2.0, pianoFrequency(72) / pianoFrequency(60), .000001)
        for (note in 24..95) assertEquals(2.0.pow(1.0/12), pianoFrequency(note+1)/pianoFrequency(note), .000001)
        assertEquals(82.4069, pianoFrequency(40), .001)
        assertEquals("C4", pianoNoteName(60)); assertEquals("C7", pianoNoteName(96))
    }
    @Test fun realPcmHasExpectedFundamentalAndSmoothRelease() {
        for (note in listOf(24, 40, 60, 69, 72, 84, 96)) {
            val synth = PianoPcm(); assertTrue(synth.noteOn(1, note))
            synth.render(ShortArray(24000))
            val samples = ShortArray(48000); synth.render(samples)
            val crossings = (1 until samples.size).count { samples[it-1] <= 0 && samples[it] > 0 }
            assertEquals("rendered pitch $note", pianoFrequency(note), crossings.toDouble(), 2.0)
            synth.noteOff(1); val tail = ShortArray(3000); synth.render(tail)
            assertTrue(tail.takeLast(500).all { it.toInt() == 0 }); assertEquals(0, synth.voiceCount())
            assertTrue((1 until tail.size).maxOf { abs(tail[it].toInt()-tail[it-1].toInt()) } < 5000)
        }
    }
    @Test fun chordsUnisonLimitsAndRapidReleaseStayBounded() {
        val synth = PianoPcm()
        for (i in 0L..15L) assertTrue(synth.noteOn(i, 60 + (i % 12).toInt()))
        assertFalse(synth.noteOn(17, 72)); assertEquals(16, synth.voiceCount())
        val chord = ShortArray(12000); synth.render(chord)
        assertTrue(chord.any { it.toInt() != 0 }); assertTrue(chord.all { abs(it.toInt()) < 28000 })
        synth.stopAll(); synth.render(ShortArray(3000)); assertEquals(0, synth.voiceCount())
        repeat(100) { synth.noteOn(it.toLong(),60); synth.render(ShortArray(100)); synth.noteOff(it.toLong()) }
        synth.stopAll(); synth.render(ShortArray(3000)); assertEquals(0, synth.voiceCount())
        assertTrue(synth.noteOn(1,60)); assertTrue(synth.noteOn(2,60)); synth.noteOff(1)
        synth.render(ShortArray(3000)); assertEquals(1,synth.voiceCount())
        synth.noteOff(2); assertTrue("glissando reuses a finger before its old release ends",synth.noteOn(2,62))
        synth.render(ShortArray(3000)); assertEquals(1,synth.voiceCount())
    }
    @Test fun blackWhiteGeometryAndOutsideCancellation() {
        val keys = PianoGeometry(60,72)
        assertEquals(60,keys.hit(10f,150f,800f,180f))
        assertEquals(61,keys.hit(100f,10f,800f,180f))
        assertEquals(62,keys.hit(100f,150f,800f,180f))
        assertEquals(72,keys.hit(799f,170f,800f,180f))
        assertNull(keys.hit(-1f,20f,800f,180f)); assertNull(keys.hit(100f,181f,800f,180f))
        assertEquals(11,PianoGeometry(60,77).whites.size)
    }

    @Test fun everyChromaticWindowHasPlayableBlackEdgesAndWhiteCenters() {
        for (span in listOf(12, 16, 17)) {
            for (first in 24..96 - span) {
                val keys = PianoGeometry(first, first + span)
                val width = 300f
                val unit = width / keys.whites.size
                keys.whites.forEachIndexed { index, note ->
                    assertEquals(note, keys.hit(unit * (index + .5f), 80f, width, 100f))
                }
                keys.blacks.forEach { note ->
                    val left = keys.blackLeft(note)
                    assertTrue(left >= 0 && left + .62f <= keys.whites.size)
                    assertEquals(note, keys.hit(unit * (left + .31f), 30f, width, 100f))
                }
            }
        }
    }
}
