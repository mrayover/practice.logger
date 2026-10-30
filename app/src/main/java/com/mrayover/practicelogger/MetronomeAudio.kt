package com.mrayover.practicelogger

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import android.os.Process
import kotlin.math.PI
import kotlin.math.sin

fun initialWorkingBpm(placement: Placement): Int =
    placement.lastUsedBpm ?: placement.tempo?.takeIf { it in 30..300 } ?: 120

fun parseWorkingBpm(text: String): Int? = text.trim().toIntOrNull()?.takeIf { it in 30..300 }

/** A sample clock, not a UI delay. Fractional phase avoids cumulative beat rounding drift. */
internal class BeatPcm(private val sampleRate: Int = 48_000) {
    private var phase = 1.0
    private var clickFrame = sampleRate // First render starts a click.
    fun render(output: ShortArray, bpm: Int) {
        require(bpm in 30..300)
        val clickLength = sampleRate / 100 // 10 ms, identical timbre at every tempo.
        output.indices.forEach { i ->
            if (phase >= 1.0) { phase -= 1.0; clickFrame = 0 }
            output[i] = if (clickFrame < clickLength) {
                val envelope = 1.0 - clickFrame.toDouble() / clickLength
                (sin(2 * PI * 1800 * clickFrame / sampleRate) * envelope * 20_000).toInt().toShort()
            } else 0
            clickFrame++
            phase += bpm.toDouble() / (60 * sampleRate)
        }
    }
}

/** A brief two-note cue, mixed into the same sample clock. Click phase still advances
 * under the cue; ducking changes only amplitude, never BPM or beat scheduling. */
internal class CompletionPcm(private val rate: Int = 48_000) {
    private var frame = rate
    val length = rate * 3 / 5
    @Synchronized fun trigger() { frame = 0 }
    @Synchronized fun cancel() { frame = length }
    @Synchronized fun active() = frame < length
    @Synchronized fun mix(samples: ShortArray): Int {
        var mixed = 0
        for (i in samples.indices) {
            if (frame >= length) break
            val noteLength = rate / 4
            val local = if (frame < rate * 3 / 10) frame else frame - rate * 3 / 10
            val frequency = if (frame < rate * 3 / 10) 784.0 else 1046.5
            val envelope = if (local < noteLength) minOf(1.0, local / (rate * .008)) * (1.0 - local.toDouble() / noteLength) else 0.0
            samples[i] = (samples[i] * .12 + sin(2 * PI * frequency * local / rate) * envelope * 26_000).toInt().coerceIn(-32768, 32767).toShort()
            frame++; mixed++
        }
        return mixed
    }
}

/** One foreground click stream. Blocking writes are paced by AudioTrack's sample consumption. */
internal class MetronomeAudio(context: Context, private val interrupted: (String?) -> Unit) {
    private val manager = context.getSystemService(AudioManager::class.java)
    private val main = Handler(Looper.getMainLooper())
    private val lock = Any()
    @Volatile private var generation = 0L
    @Volatile private var tempo = 120
    private var track: AudioTrack? = null
    @Volatile private var clickEnabled = false
    private val completion = CompletionPcm()
    @Volatile internal var completionFramesWritten = 0L; private set

    fun playCompletion() {
        completion.trigger()
        completionFramesWritten = 0
        // No extra focus request: a cue must not interrupt a phrase or take playback.
        if (track == null) startOutput(requestFocus = false)
    }
    private val attributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
    private val focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(attributes).setWillPauseWhenDucked(true)
        .setOnAudioFocusChangeListener({ change ->
            if (change != AudioManager.AUDIOFOCUS_GAIN && track != null) {
                stop(); interrupted(null) // Focus gain never restarts the click.
            }
        }, main).build()

    fun changeTempo(bpm: Int) { require(bpm in 30..300); tempo = bpm }

    fun start(bpm: Int): Boolean {
        stop()
        changeTempo(bpm)
        clickEnabled = true
        return startOutput(requestFocus = true)
    }

    private fun startOutput(requestFocus: Boolean): Boolean {
        var created: AudioTrack? = null
        try {
            if (requestFocus && manager.requestAudioFocus(focus) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                interrupted("Click could not start because audio is in use. Try again."); return false
            }
            val minimum = AudioTrack.getMinBufferSize(48_000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
            check(minimum > 0)
            val audio = AudioTrack.Builder().setAudioAttributes(attributes)
                .setAudioFormat(AudioFormat.Builder().setSampleRate(48_000).setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT).build())
                .setTransferMode(AudioTrack.MODE_STREAM).setBufferSizeInBytes(maxOf(minimum, 1920)).build()
            created = audio
            check(audio.state == AudioTrack.STATE_INITIALIZED)
            val token = ++generation
            synchronized(lock) { track = audio; audio.play() }
            Thread({
                try {
                    Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
                    val pcm = BeatPcm()
                    val chunk = ShortArray(480)
                    var quietFrames = 0
                    while (generation == token) {
                        if (clickEnabled) pcm.render(chunk, tempo) else chunk.fill(0)
                        val cueFrames = completion.mix(chunk)
                        var offset = 0
                        while (offset < chunk.size && generation == token) {
                            val written = audio.write(chunk, offset, chunk.size - offset, AudioTrack.WRITE_BLOCKING)
                            check(written > 0)
                            offset += written
                        }
                        if (generation == token) completionFramesWritten += cueFrames
                        quietFrames = if (!clickEnabled && !completion.active()) quietFrames + chunk.size else 0
                        // Let the final queued samples drain, then release this cue-only stream.
                        if (quietFrames >= 24_000) {
                            main.post { if (generation == token && !clickEnabled && !completion.active()) stop() }
                            break
                        }
                    }
                } catch (_: Exception) {
                    main.post {
                        if (generation == token) { stop(); interrupted("Click stopped because audio output failed. Try again.") }
                    }
                } finally {
                    synchronized(lock) { if (track === audio) track = null; audio.release() }
                }
            }, "practice-metronome").start()
            return true
        } catch (_: Exception) {
            synchronized(lock) { track = null; created?.release() }
            manager.abandonAudioFocusRequest(focus)
            interrupted("Click could not start. Check media volume and audio output, then retry.")
            return false
        }
    }

    fun stop() {
        clickEnabled = false
        completion.cancel()
        ++generation
        synchronized(lock) {
            track?.let { audio ->
                // Discard queued samples now; stop also interrupts an outstanding blocking write.
                runCatching { audio.pause(); audio.flush(); audio.stop() }
            }
            track = null // The writer owns release and cannot overlap audible playback after stop.
        }
        manager.abandonAudioFocusRequest(focus)
    }
}
