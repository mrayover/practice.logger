package com.mrayover.practicelogger

import android.annotation.SuppressLint
import android.media.*
import android.os.Build
import android.os.SystemClock
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.*

/** One mono PCM frame per sample. Only the final division is rounded, never each beat. */
data class PhraseSpec(val bpm: Int, val beats: Int) {
    init { require(bpm in 30..300 && beats in listOf(4, 8, 16, 32)) }
    val frames: Int get() = (60.0 * RATE * beats / bpm).roundToInt()
    val durationMs: Long get() = frames * 1000L / RATE
    companion object { const val RATE = 48_000 }
}

/** Canonical PCM16 mono WAV; exact frame boundaries without compressed encoder padding. */
object PhraseWave {
    fun write(file: File, samples: ShortArray) {
        val bytes = ByteBuffer.allocate(44 + samples.size * 2).order(ByteOrder.LITTLE_ENDIAN)
        bytes.put("RIFF".toByteArray()).putInt(36 + samples.size * 2).put("WAVEfmt ".toByteArray())
        bytes.putInt(16).putShort(1).putShort(1).putInt(PhraseSpec.RATE).putInt(PhraseSpec.RATE * 2)
        bytes.putShort(2).putShort(16).put("data".toByteArray()).putInt(samples.size * 2)
        samples.forEach(bytes::putShort)
        FileOutputStream(file).use { it.write(bytes.array()); it.fd.sync() }
    }
    fun read(file: File): ShortArray {
        require(file.length() in 46..(44 + PhraseSpec(30, 32).frames * 2L)) { "Invalid phrase size." }
        val bytes = ByteBuffer.wrap(file.readBytes()).order(ByteOrder.LITTLE_ENDIAN)
        fun text(n: Int) = ByteArray(n).also(bytes::get).toString(Charsets.US_ASCII)
        require(text(4) == "RIFF" && bytes.int == bytes.capacity() - 8 && text(8) == "WAVEfmt ")
        require(bytes.int == 16 && bytes.short.toInt() == 1 && bytes.short.toInt() == 1)
        require(bytes.int == PhraseSpec.RATE && bytes.int == PhraseSpec.RATE * 2)
        require(bytes.short.toInt() == 2 && bytes.short.toInt() == 16 && text(4) == "data")
        require(bytes.int == bytes.remaining() && bytes.remaining() % 2 == 0)
        return ShortArray(bytes.remaining() / 2) { bytes.short }
    }
    fun countIn(bpm: Int): ShortArray {
        val length = PhraseSpec(bpm, 4).frames
        return ShortArray(length).also { samples ->
            repeat(4) { beat ->
                val start = (60.0 * PhraseSpec.RATE * beat / bpm).roundToInt()
                repeat(480) { i ->
                    samples[start + i] = (sin(2 * PI * 1400 * i / PhraseSpec.RATE) * 11000 * (1.0 - i / 480.0)).toInt().toShort()
                }
            }
        }
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

/** Only one worker owns reads/writes. cancel synchronously stops native audio before returning. */
class PhraseCapture {
    private val lock = Any()
    @Volatile private var cancelled = false
    private var input: AudioRecord? = null
    private var output: AudioTrack? = null
    fun cancel() = synchronized(lock) {
        cancelled = true
        runCatching { input?.stop() }; runCatching { output?.pause(); output?.flush() }
    }
    @SuppressLint("MissingPermission") // ViewModel checks permission at the explicit record action.
    fun capture(spec: PhraseSpec, file: File, onCount: (Int) -> Unit, onFrames: (Int) -> Unit,
        hasSpaceAndPermission: () -> Boolean) {
        val minimum = AudioRecord.getMinBufferSize(PhraseSpec.RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        check(minimum > 0)
        val mic = AudioRecord.Builder().setAudioSource(MediaRecorder.AudioSource.MIC)
            .setAudioFormat(AudioFormat.Builder().setSampleRate(PhraseSpec.RATE).setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_IN_MONO).build()).setBufferSizeInBytes(max(minimum * 2, 9600)).build()
        try {
            check(mic.state == AudioRecord.STATE_INITIALIZED)
            val clicks = PhraseWave.countIn(spec.bpm)
            synchronized(lock) { check(!cancelled); input = mic; output = phraseTrack(clicks, false); output!!.play() }
            val deadline = SystemClock.elapsedRealtime() + spec.copy(beats = 4).durationMs + 5000
            var last = 0
            while (true) {
                check(!cancelled && hasSpaceAndPermission()) { "Phrase interrupted." }
                val frames = synchronized(lock) { output!!.playbackHeadPosition.toLong() and 0xffffffffL }
                if (frames >= clicks.size) break
                check(SystemClock.elapsedRealtime() < deadline) { "Count-in output stalled." }
                val beat = (frames * spec.bpm / (60L * PhraseSpec.RATE)).toInt().coerceIn(0, 3) + 1
                if (beat != last) { last = beat; onCount(beat) }
                Thread.sleep(5)
            }
            // Four output-clock beats end before capture starts. Device I/O latency is not calibrated.
            synchronized(lock) {
                check(!cancelled); output?.release(); output = null
                mic.startRecording(); check(mic.recordingState == AudioRecord.RECORDSTATE_RECORDING)
            }
            onCount(0)
            val samples = ShortArray(spec.frames)
            var position = 0
            while (position < samples.size) {
                check(!cancelled && hasSpaceAndPermission()) { "Phrase interrupted." }
                if (Build.VERSION.SDK_INT >= 29) check(mic.activeRecordingConfiguration?.isClientSilenced != true) { "Microphone silenced." }
                val read = mic.read(samples, position, min(960, samples.size - position), AudioRecord.READ_BLOCKING)
                check(read > 0) { "Microphone read failed." }
                position += read; onFrames(position)
            }
            synchronized(lock) { check(!cancelled); mic.stop() }
            check(!cancelled); PhraseWave.write(file, samples)
        } finally {
            synchronized(lock) {
                runCatching { mic.stop() }; mic.release(); input = null
                runCatching { output?.release() }; output = null
            }
        }
    }
}
