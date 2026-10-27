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

/** One foreground click stream. Blocking writes are paced by AudioTrack's sample consumption. */
internal class MetronomeAudio(context: Context, private val interrupted: (String?) -> Unit) {
    private val manager = context.getSystemService(AudioManager::class.java)
    private val main = Handler(Looper.getMainLooper())
    private val lock = Any()
    @Volatile private var generation = 0L
    @Volatile private var tempo = 120
    private var track: AudioTrack? = null
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
        var created: AudioTrack? = null
        try {
            if (manager.requestAudioFocus(focus) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
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
                    while (generation == token) {
                        pcm.render(chunk, tempo)
                        var offset = 0
                        while (offset < chunk.size && generation == token) {
                            val written = audio.write(chunk, offset, chunk.size - offset, AudioTrack.WRITE_BLOCKING)
                            check(written > 0)
                            offset += written
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
