package com.mrayover.practicelogger

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class RecordingWaveformTest {
    @Test fun timelineClampsTapAndDragPositions() {
        val timeline = AudioTimeline(4321)
        assertEquals(0L, timeline.timeAt(-.5f))
        assertEquals(2160L, timeline.timeAt(.5f))
        assertEquals(4321L, timeline.timeAt(2f))
        assertEquals(0, recordingFrame(-1, 48000))
        assertEquals(24000, recordingFrame(500, 48000))
        assertEquals(47999, recordingFrame(Long.MAX_VALUE, 48000))
    }
    @Test fun realPcmPeaksCacheInvalidationAndCorruptFallback() = runBlocking {
        val file = File.createTempFile("rw1", ".wav")
        try {
            val samples = ShortArray(48000) { if (it < 24000) 0 else 16384 }
            PhraseWave.write(file, samples)
            val wave = RecordingWaveforms.load(file, 1000)
            assertEquals(0f, wave.peaks[0], 0f)
            assertEquals(.5f, wave.peaks[150], .001f)
            assertSame(wave, RecordingWaveforms.load(file, 1000))
            file.writeBytes(byteArrayOf(1,2,3))
            try { RecordingWaveforms.load(file, 1000); fail("Corrupt WAV accepted") } catch (_: IllegalArgumentException) {}
            file.delete()
            try { RecordingWaveforms.load(file, 1000); fail("Missing WAV accepted") } catch (_: IllegalArgumentException) {}
        } finally { file.delete() }
    }
}
