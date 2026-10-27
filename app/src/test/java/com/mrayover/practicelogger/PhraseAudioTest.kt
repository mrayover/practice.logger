package com.mrayover.practicelogger

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class PhraseAudioTest {
    @Test fun phraseFramesAndCountInUseOneRoundedBoundaryAtEveryTempoAndLength() {
        for (bpm in 30..300) for (beats in listOf(4, 8, 16, 32)) {
            val spec = PhraseSpec(bpm, beats)
            assertTrue(kotlin.math.abs(spec.frames - 60.0 * PhraseSpec.RATE * beats / bpm) <= 0.5)
        }
        for (bpm in listOf(60, 120, 180)) {
            val clicks = PhraseWave.countIn(bpm)
            assertEquals(PhraseSpec(bpm, 4).frames, clicks.size)
            val clusters = clicks.indices.count { i -> clicks[i] != 0.toShort() && (i == 0 || clicks[i - 1] == 0.toShort()) && i % (60 * PhraseSpec.RATE / bpm) < 3 }
            assertEquals(4, clusters)
            repeat(4) { beat ->
                val start = 60 * PhraseSpec.RATE * beat / bpm
                assertTrue(clicks.sliceArray(start until start + 480).any { it != 0.toShort() })
                assertTrue(clicks.sliceArray(start + 480 until minOf(start + 900, clicks.size)).all { it == 0.toShort() })
            }
        }
        for ((bpm, beats) in listOf(29 to 8, 301 to 8, 120 to 5)) {
            try { PhraseSpec(bpm, beats); fail("Invalid selection accepted") } catch (_: IllegalArgumentException) { }
        }
    }
    @Test fun wavRoundTripPreservesExactPcmAndRejectsTruncatedOrCorruptedData() {
        val file = File.createTempFile("phrase", ".wav")
        try {
            val spec = PhraseSpec(180, 8)
            val samples = ShortArray(spec.frames) { (it % 32767 - 16000).toShort() }
            PhraseWave.write(file, samples)
            assertEquals(44 + samples.size * 2L, file.length())
            assertArrayEquals(samples, PhraseWave.read(file))
            val original = file.readBytes()
            file.writeBytes(original.copyOf(original.size - 2))
            try { PhraseWave.read(file); fail("Truncated WAV accepted") } catch (_: IllegalArgumentException) { }
            file.writeBytes(original.also { it[24] = 0 })
            try { PhraseWave.read(file); fail("Wrong sample rate accepted") } catch (_: IllegalArgumentException) { }
        } finally { file.delete() }
    }
}
