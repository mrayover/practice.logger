package com.mrayover.practicelogger

import android.annotation.SuppressLint
import android.media.*
import android.os.Build
import android.os.SystemClock
import java.io.File
import java.io.FileOutputStream
import java.io.DataInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread
import kotlin.math.*

/** Counts are musical beats; the execution budget is time, not a fixed menu of beat choices. */
data class PhraseSpec(val bpm: Int, val beats: Int, val countInBeats: Long = 4) {
    init {
        require(bpm in 30..300) { "Enter a whole BPM from 30 to 300." }
        require(beats in 1..64) { "Enter a whole loop length from 1 to 64 beats." }
        require(countInBeats > 0) { "Enter a positive whole count-in beat count." }
        // Check before multiplication; even Long.MAX_VALUE cannot overflow this calculation.
        require(countInBeats <= MAX_COUNT_IN_MS * bpm / 60_000L) {
            "This count-in would exceed the 15-minute execution budget. Reduce count-in beats or increase BPM."
        }
    }
    val frames: Int get() = (60.0 * RATE * beats / bpm).roundToInt()
    val durationMs: Long get() = frames * 1000L / RATE
    val countInFrames: Long get() = (60.0 * RATE * countInBeats / bpm).roundToLong()
    val countInDurationMs: Long get() = (60_000.0 * countInBeats / bpm).roundToLong()
    companion object {
        const val RATE = 48_000
        const val MAX_FRAMES = 6_144_000 // 30 BPM x 64 beats = 128 seconds.
        const val MAX_COUNT_IN_MS = 15 * 60_000L
    }
}

fun recordingSpaceRequired(spec: PhraseSpec?): Long = 2 * 1024 * 1024L + (spec?.frames?.toLong() ?: 0L) * 2 + 44
fun requireRecordingSpace(available: Long, spec: PhraseSpec?) {
    check(available >= recordingSpaceRequired(spec)) { "Free some storage before recording." }
}

/** One rounded absolute sample boundary per beat; memory stays bounded for long count-ins. */
class PhraseGuide(private val spec: PhraseSpec, private val enabled: Boolean) {
    val totalFrames: Long = spec.countInFrames + if (enabled) spec.frames else 0
    fun fill(offset: Long, destination: ShortArray): Int {
        require(offset >= 0 && destination.isNotEmpty())
        val count = minOf(destination.size.toLong(), (totalFrames - offset).coerceAtLeast(0)).toInt()
        java.util.Arrays.fill(destination, 0, count, 0.toShort())
        if (count == 0) return 0
        fun ticks(base: Long, length: Long, beats: Long) {
            var beat = maxOf(0, floor((offset - base) * spec.bpm.toDouble() / (60.0 * PhraseSpec.RATE)).toLong() - 1)
            while (beat < beats) {
                val start = base + (60.0 * PhraseSpec.RATE * beat / spec.bpm).roundToLong()
                if (start >= offset + count) break
                val from = maxOf(offset, start)
                val to = minOf(offset + count, start + 480, base + length)
                for (frame in from until to) {
                    val i = frame - start
                    destination[(frame - offset).toInt()] =
                        (sin(2 * PI * 1400 * i / PhraseSpec.RATE) * 11000 * (1.0 - i / 480.0)).toInt().toShort()
                }
                beat++
            }
        }
        ticks(0, spec.countInFrames, spec.countInBeats)
        if (enabled) ticks(spec.countInFrames, spec.frames.toLong(), spec.beats.toLong())
        return count
    }
}

/** Canonical PCM16 WAV. Only one full PCM array is needed; file IO uses small byte chunks. */
object PhraseWave {
    private fun header(frames: Int): ByteArray {
        require(frames in 1..PhraseSpec.MAX_FRAMES) { "Invalid phrase size." }
        return ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray()).putInt(36 + frames * 2).put("WAVEfmt ".toByteArray())
            putInt(16).putShort(1).putShort(1).putInt(PhraseSpec.RATE).putInt(PhraseSpec.RATE * 2)
            putShort(2).putShort(16).put("data".toByteArray()).putInt(frames * 2)
        }.array()
    }
    fun write(file: File, samples: ShortArray) {
        FileOutputStream(file).use { output ->
            output.write(header(samples.size))
            val chunk = ByteBuffer.allocate(8192).order(ByteOrder.LITTLE_ENDIAN)
            var position = 0
            while (position < samples.size) {
                chunk.clear()
                repeat(minOf(chunk.capacity() / 2, samples.size - position)) { chunk.putShort(samples[position++]) }
                output.write(chunk.array(), 0, chunk.position())
            }
            output.fd.sync()
        }
    }
    fun frames(file: File): Int {
        require(file.length() in 46..(44 + PhraseSpec.MAX_FRAMES * 2L) && (file.length() - 44) % 2 == 0L) { "Invalid phrase size." }
        val bytes = ByteArray(44)
        DataInputStream(file.inputStream()).use { it.readFully(bytes) }
        val count = ((file.length() - 44) / 2).toInt()
        require(bytes.contentEquals(header(count))) { "Invalid or incomplete PCM16 phrase." }
        return count
    }
    fun read(file: File): ShortArray {
        val samples = ShortArray(frames(file))
        DataInputStream(file.inputStream()).use { input ->
            val chunk = ByteArray(8192)
            input.readFully(chunk, 0, 44)
            var position = 0
            while (position < samples.size) {
                val count = minOf(chunk.size / 2, samples.size - position)
                input.readFully(chunk, 0, count * 2)
                val buffer = ByteBuffer.wrap(chunk, 0, count * 2).order(ByteOrder.LITTLE_ENDIAN)
                repeat(count) { samples[position++] = buffer.short }
            }
            require(input.read() == -1) { "Unexpected phrase bytes." }
        }
        return samples
    }
}

fun phraseTrack(samples: ShortArray, repeat: Boolean): AudioTrack {
    val audio = AudioTrack.Builder().setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
        .setAudioFormat(AudioFormat.Builder().setSampleRate(PhraseSpec.RATE).setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
        .setTransferMode(AudioTrack.MODE_STATIC).setBufferSizeInBytes(samples.size * 2).build()
    try {
        // MODE_STATIC is valid but has no data until the first write completes.
        check(audio.state != AudioTrack.STATE_UNINITIALIZED)
        check(audio.write(samples, 0, samples.size) == samples.size)
        check(audio.state == AudioTrack.STATE_INITIALIZED)
        if (repeat) check(audio.setLoopPoints(0, samples.size, -1) == AudioTrack.SUCCESS)
        return audio
    } catch (failure: Exception) { audio.release(); throw failure }
}

/** One capture owner, one output sample clock, bounded streaming guide. */
class PhraseCapture {
    private val lock = Any()
    @Volatile private var cancelled = false
    private var input: AudioRecord? = null
    private var output: AudioTrack? = null
    internal fun outputFramesPlayed(): Long = synchronized(lock) {
        output?.let { it.playbackHeadPosition.toLong() and 0xffffffffL } ?: 0L
    }
    fun cancel() = synchronized(lock) {
        cancelled = true
        runCatching { input?.stop() }; runCatching { output?.pause(); output?.flush() }
    }
    @SuppressLint("MissingPermission")
    fun capture(spec: PhraseSpec, file: File, clickWhileRecording: Boolean, onCount: (Int) -> Unit, onFrames: (Int) -> Unit,
        hasSpaceAndPermission: () -> Boolean) {
        val minimum = AudioRecord.getMinBufferSize(PhraseSpec.RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        check(minimum > 0)
        val mic = AudioRecord.Builder().setAudioSource(MediaRecorder.AudioSource.MIC)
            .setAudioFormat(AudioFormat.Builder().setSampleRate(PhraseSpec.RATE).setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_IN_MONO).build()).setBufferSizeInBytes(max(minimum * 2, 9600)).build()
        var writer: Thread? = null
        val guideFailure = AtomicReference<Throwable?>(null)
        try {
            check(mic.state == AudioRecord.STATE_INITIALIZED)
            val outMinimum = AudioTrack.getMinBufferSize(PhraseSpec.RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
            check(outMinimum > 0)
            val track = AudioTrack.Builder().setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                .setAudioFormat(AudioFormat.Builder().setSampleRate(PhraseSpec.RATE).setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setTransferMode(AudioTrack.MODE_STREAM).setBufferSizeInBytes(max(outMinimum * 2, 9600)).build()
            synchronized(lock) { output = track; check(!cancelled); input = mic; check(track.state == AudioTrack.STATE_INITIALIZED); track.play() }
            val guide = PhraseGuide(spec, clickWhileRecording)
            writer = thread(name = "phrase-guide", isDaemon = true) {
                try {
                    val chunk = ShortArray(960)
                    var offset = 0L
                    while (!cancelled) {
                        val count = guide.fill(offset, chunk)
                        if (count == 0) break
                        var position = 0
                        while (position < count && !cancelled) {
                            val written = track.write(chunk, position, count - position, AudioTrack.WRITE_BLOCKING)
                            check(written > 0) { "Count-in output failed." }; position += written
                        }
                        offset += count
                    }
                } catch (failure: Throwable) { if (!cancelled) guideFailure.set(failure) }
            }
            val deadline = SystemClock.elapsedRealtime() + spec.countInDurationMs + 5000
            var last = 0
            while (true) {
                check(!cancelled && hasSpaceAndPermission()) { "Phrase interrupted." }
                check(guideFailure.get() == null) { "Count-in output failed." }
                val frames = synchronized(lock) { output!!.playbackHeadPosition.toLong() and 0xffffffffL }
                if (frames >= spec.countInFrames) break
                check(SystemClock.elapsedRealtime() < deadline) { "Count-in output stalled." }
                val beat = (frames * spec.bpm / (60L * PhraseSpec.RATE)).coerceIn(0, spec.countInBeats - 1).toInt() + 1
                if (beat != last) { last = beat; onCount(beat) }
                Thread.sleep(5)
            }
            // Mic starts only after the selected output-clock count-in; I/O latency is not calibrated.
            if (!clickWhileRecording) writer?.join(1000)
            synchronized(lock) {
                check(!cancelled)
                if (!clickWhileRecording) { output?.release(); output = null }
                mic.startRecording(); check(mic.recordingState == AudioRecord.RECORDSTATE_RECORDING)
            }
            onCount(0)
            val samples = ShortArray(spec.frames)
            val captureDeadline = SystemClock.elapsedRealtime() + spec.durationMs + 5000
            var position = 0
            while (position < samples.size) {
                check(!cancelled && hasSpaceAndPermission()) { "Phrase interrupted." }
                check(guideFailure.get() == null) { "Recording guide output failed." }
                check(SystemClock.elapsedRealtime() < captureDeadline) { "Microphone capture stalled." }
                if (Build.VERSION.SDK_INT >= 29) check(mic.activeRecordingConfiguration?.isClientSilenced != true) { "Microphone silenced." }
                val read = mic.read(samples, position, min(960, samples.size - position), AudioRecord.READ_BLOCKING)
                check(read > 0) { "Microphone read failed." }
                position += read; onFrames(position)
            }
            synchronized(lock) {
                check(!cancelled); mic.stop()
                output?.pause(); output?.flush()
            }
            check(!cancelled); PhraseWave.write(file, samples)
        } finally {
            synchronized(lock) {
                runCatching { mic.stop() }; mic.release(); input = null
                runCatching { output?.pause(); output?.flush(); output?.release() }; output = null
            }
            writer?.join(1000)
        }
    }
}
