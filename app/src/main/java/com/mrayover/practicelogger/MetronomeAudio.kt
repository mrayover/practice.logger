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

enum class MetronomeMeter(val beats: Int, val unit: Int) {
    TWO_FOUR(2,4), THREE_FOUR(3,4), FOUR_FOUR(4,4), FIVE_FOUR(5,4), SIX_EIGHT(6,8), SEVEN_EIGHT(7,8);
    val label: String get() = "$beats/$unit"
}
enum class MetronomeMode(val sound: Boolean, val flash: Boolean, val label: String) {
    SOUND_FLASH(true,true,"Sound + Flash"), SOUND_ONLY(true,false,"Sound only"), FLASH_ONLY(false,true,"Flash only / silent")
}
data class MetronomeConfig(val meter: MetronomeMeter = MetronomeMeter.FOUR_FOUR, val accent: Boolean = false,
    val mode: MetronomeMode = MetronomeMode.SOUND_FLASH, val sound: ClickSound = ClickSound.CLASSIC)
data class MetronomePulse(val sequence: Long = 0, val beat: Int = 0, val config: MetronomeConfig = MetronomeConfig(), val lit: Boolean = false)
internal data class MetronomeBeat(val frame: Long, val sequence: Long, val beat: Int, val config: MetronomeConfig)

/** One fractional sample phase for sound, bar position and flash events, even with accent off. */
internal class BeatPcm(private val sampleRate: Int = 48_000) {
    private var phase = 1.0
    private var clickFrame = sampleRate
    private var rendered = 0L
    private var sequence = 0L
    private var beat = 0
    private var active = MetronomeConfig()
    fun render(output: ShortArray, bpm: Int, requested: MetronomeConfig = MetronomeConfig(), onBeat: (MetronomeBeat) -> Unit = {}) {
        require(bpm in 30..300)
        val clickLength = sampleRate / 100
        output.indices.forEach { i ->
            if (phase >= 1.0) {
                phase -= 1.0; clickFrame = 0
                // Meter changes begin a new bar at the next beat; no beat phase or timer restart.
                beat = if (active.meter != requested.meter) 1 else beat % requested.meter.beats + 1
                active = requested; sequence++
                onBeat(MetronomeBeat(rendered,sequence,beat,active))
            }
            val accented = active.accent && beat == 1
            output[i] = if (active.mode.sound && clickFrame < clickLength) {
                val envelope = 1.0 - clickFrame.toDouble() / clickLength
                (sin(2 * PI * active.sound.frequency * (if (accented) 4.0/3 else 1.0) * clickFrame / sampleRate) * envelope *
                    (active.sound.amplitude * (if (accented) 1.2 else 1.0))).toInt().toShort()
            } else 0
            clickFrame++; rendered++
            phase += bpm.toDouble() / (60 * sampleRate)
        }
    }
}

/** A brief two-note cue, mixed into the same sample clock. Click phase still advances
 * under the cue; ducking changes only amplitude, never BPM or beat scheduling. */
internal class CompletionPcm(private val rate: Int = 48_000) {
    private var frame = rate
    val length = rate * 3 / 5
    private var sound = AlarmSound.TWO_TONE
    @Synchronized fun trigger(value: AlarmSound = AlarmSound.TWO_TONE) { sound = value; frame = if (value == AlarmSound.OFF) length else 0 }
    @Synchronized fun cancel() { frame = length }
    @Synchronized fun active() = frame < length
    @Synchronized fun mix(samples: ShortArray): Int {
        var mixed = 0
        for (i in samples.indices) {
            if (frame >= length) break
            val segment = when(sound) { AlarmSound.RISING -> rate / 5; AlarmSound.TWO_TONE -> rate * 3 / 10; else -> length }
            val noteLength = when(sound) { AlarmSound.BEEP -> rate / 8; AlarmSound.BELL -> length; else -> rate / 4 }
            val local = frame % segment
            val frequency = when(sound) {
                AlarmSound.BELL -> 1318.5
                AlarmSound.RISING -> when(frame / segment) { 0 -> 659.25; 1 -> 830.61; else -> 1046.5 }
                AlarmSound.BEEP -> 880.0
                else -> if (frame < rate * 3 / 10) 784.0 else 1046.5
            }
            val envelope = if (local < noteLength) minOf(1.0, local / (rate * .008)) * (1.0 - local.toDouble() / noteLength) else 0.0
            samples[i] = (samples[i] * .12 + sin(2 * PI * frequency * local / rate) * envelope * 26_000).toInt().coerceIn(-32768, 32767).toShort()
            frame++; mixed++
        }
        return mixed
    }
}

/** One foreground click stream. Blocking writes are paced by AudioTrack's sample consumption. */
internal class MetronomeAudio(private val context: Context, private val pulseChanged: (MetronomePulse) -> Unit = {}, private val interrupted: (String?) -> Unit) {
    private val manager = context.getSystemService(AudioManager::class.java)
    private val main = Handler(Looper.getMainLooper())
    private val lock = Any()
    @Volatile private var generation = 0L
    @Volatile private var tempo = 120
    @Volatile private var configuration = MetronomeConfig()
    @Volatile internal var sampleFramesWritten = 0L; private set
    @Volatile internal var nonZeroClickFramesWritten = 0L; private set
    private var lastPulseSequence = 0L
    fun configure(value: MetronomeConfig) { configuration = value }
    private var track: AudioTrack? = null
    @Volatile private var clickEnabled = false
    private val completion = CompletionPcm()
    @Volatile internal var completionFramesWritten = 0L; private set

    fun playCompletion(sound: AlarmSound = AudioPreferences(context).alarm) {
        completionFramesWritten = 0
        if (sound == AlarmSound.OFF || RecordingViewModel.isCapturingAudio) return
        completion.trigger(sound)
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
        sampleFramesWritten = 0; nonZeroClickFramesWritten = 0
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
                    val beats = java.util.ArrayDeque<MetronomeBeat>()
                    while (generation == token) {
                        if (clickEnabled) pcm.render(chunk, tempo, configuration) { beats.addLast(it) } else chunk.fill(0)
                        val nonZeroClick = chunk.count { it.toInt() != 0 }
                        if (RecordingViewModel.isCapturingAudio) completion.cancel()
                        val cueFrames = completion.mix(chunk)
                        var offset = 0
                        while (offset < chunk.size && generation == token) {
                            val written = audio.write(chunk, offset, chunk.size - offset, AudioTrack.WRITE_BLOCKING)
                            check(written > 0)
                            offset += written
                        }
                        if (generation == token) {
                            completionFramesWritten += cueFrames
                            sampleFramesWritten += chunk.size
                            nonZeroClickFramesWritten += nonZeroClick
                            // Emit only after AudioTrack consumes the event's sample boundary.
                            val played = audio.playbackHeadPosition.toLong() and 0xffffffffL
                            while (!beats.isEmpty() && beats.peekFirst().frame < played) {
                                val event = beats.removeFirst()
                                main.post {
                                    if (generation == token && clickEnabled) {
                                        lastPulseSequence = event.sequence
                                        val pulse = MetronomePulse(event.sequence,event.beat,event.config,event.config.mode.flash)
                                        pulseChanged(pulse)
                                        if (pulse.lit) main.postDelayed({
                                            if (generation == token && clickEnabled && lastPulseSequence == event.sequence) pulseChanged(pulse.copy(lit = false))
                                        }, 90) // One-shot flash decay, never a separate beat clock.
                                    }
                                }
                            }
                        }
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
        lastPulseSequence = 0; pulseChanged(MetronomePulse())
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
