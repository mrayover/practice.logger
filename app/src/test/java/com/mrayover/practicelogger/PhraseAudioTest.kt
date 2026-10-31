package com.mrayover.practicelogger

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import kotlin.math.*

class PhraseAudioTest {
    @Test fun everyWholeLoopLengthUsesOneRoundedBoundaryAtAllSupportedTempos() {
        for (bpm in 30..300) for (beats in 1..64) {
            val spec = PhraseSpec(bpm,beats)
            assertTrue(abs(spec.frames - 60.0 * PhraseSpec.RATE * beats / bpm) <= .5)
        }
        assertEquals(6_144_000,PhraseSpec(30,64).frames);assertEquals(128000L,PhraseSpec(30,64).durationMs)
    }
    @Test fun chunkedGuideHasExactCountInAndCaptureOnsetsWithoutBeatRoundingDrift() {
        val chunk=ShortArray(960)
        for (bpm in listOf(30,73,120,299,300)) for (beats in listOf(3,5,7,11,64)) for (intro in listOf(1L,3L,7L,12L)) {
            val spec=PhraseSpec(bpm,beats,intro);val guide=PhraseGuide(spec,true)
            for (beat in 0 until intro) {
                val onset=(60.0*PhraseSpec.RATE*beat/bpm).roundToLong()
                assertTrue(guide.fill(onset,chunk)>480);assertEquals(0,chunk[0].toInt());assertNotEquals(0,chunk[1].toInt())
                assertTrue(chunk.sliceArray(480 until 960).all { it==0.toShort() })
            }
            repeat(beats) { beat ->
                val onset=spec.countInFrames+(60.0*PhraseSpec.RATE*beat/bpm).roundToLong()
                guide.fill(onset,chunk);assertEquals(0,chunk[0].toInt());assertNotEquals(0,chunk[1].toInt())
            }
            assertEquals(spec.countInFrames+spec.frames,guide.totalFrames)
            assertEquals(0,PhraseGuide(spec,false).fill(spec.countInFrames,chunk))
        }
    }
    @Test fun chunkBoundariesAgreeAndLongCountInNeverNeedsALongArray() {
        val spec=PhraseSpec(73,7,3);val guide=PhraseGuide(spec,true)
        val small=ShortArray(257);val large=ShortArray(960)
        for(offset in listOf(0L,252L, spec.countInFrames-200, spec.countInFrames+17, guide.totalFrames-300)) {
            val count=guide.fill(offset,small);guide.fill(offset,large)
            assertArrayEquals(small.copyOf(count),large.copyOf(count))
        }
        val long=PhraseSpec(300,64,4500);val bounded=ShortArray(960)
        assertEquals(900000L,long.countInDurationMs)
        assertEquals(960,PhraseGuide(long,true).fill(long.countInFrames-960,bounded))
    }
    @Test fun invalidAndOverflowingSelectionsExplainTheExecutionLimit() {
        for ((bpm,beats,intro) in listOf(Triple(29,8,4L),Triple(301,8,4L),Triple(120,0,4L),Triple(120,65,4L),Triple(120,128,4L),Triple(120,8,0L),Triple(30,8,Long.MAX_VALUE))) {
            try { PhraseSpec(bpm,beats,intro);fail("Invalid request accepted") } catch (expected:IllegalArgumentException) { assertFalse(expected.message.isNullOrBlank()) }
        }
        try { PhraseSpec(30,3,451);fail("Over-budget intro accepted") } catch(expected:IllegalArgumentException) { assertTrue(expected.message!!.contains("15-minute")) }
        assertEquals(450L,PhraseSpec(30,3,450).countInBeats)
    }
    @Test fun maximumWavRoundTripIsExactAndRejectsCorruptionOrTruncation() {
        val file=File.createTempFile("phrase",".wav")
        try {
            val spec=PhraseSpec(30,64);val samples=ShortArray(spec.frames) { (it%32000-16000).toShort() }
            PhraseWave.write(file,samples);assertEquals(44+spec.frames*2L,file.length())
            assertEquals(spec.frames,PhraseWave.frames(file));assertArrayEquals(samples,PhraseWave.read(file))
            java.io.RandomAccessFile(file,"rw").use { it.setLength(file.length()-2) }
            try { PhraseWave.read(file);fail("Truncated WAV accepted") } catch(_:IllegalArgumentException) { }
            PhraseWave.write(file,ShortArray(48000))
            java.io.RandomAccessFile(file,"rw").use { it.seek(24);it.write(0) }
            try { PhraseWave.frames(file);fail("Bad rate accepted") } catch(_:IllegalArgumentException) { }
        } finally { file.delete() }
    }
}
