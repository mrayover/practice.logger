package com.mrayover.practicelogger

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat

/** Microphone samples are analyzed in memory only; no audio file or database row is created. */
internal class TunerAudio(private val context: Context, private val result: (TuningPitch?) -> Unit,
    private val failed: (String) -> Unit) {
    private val main = Handler(Looper.getMainLooper())
    private val lock = Any()
    @Volatile private var generation = 0L
    @Volatile var framesRead = 0L
        private set
    private var recorder: AudioRecord? = null

    fun start(): Boolean {
        stop()
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            failed("Allow microphone access to use the tuner."); return false
        }
        var created: AudioRecord? = null
        try {
            val rate = listOf(48_000, 44_100).first { AudioRecord.getMinBufferSize(it, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT) > 0 }
            val minimum = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            val audio = AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, rate, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, maxOf(minimum, 32768))
            created = audio
            check(audio.state == AudioRecord.STATE_INITIALIZED)
            val token = ++generation
            framesRead = 0L
            synchronized(lock) { recorder = audio; audio.startRecording() }
            check(audio.recordingState == AudioRecord.RECORDSTATE_RECORDING)
            Thread({
                try {
                    val pcm = ShortArray(8192)
                    while (generation == token) {
                        var offset = 0
                        while (offset < pcm.size && generation == token) {
                            val count = audio.read(pcm, offset, pcm.size - offset, AudioRecord.READ_BLOCKING)
                            check(count > 0); offset += count; framesRead += count
                        }
                        if (generation == token) {
                            val pitch = PitchDetector.detect(pcm, rate)
                            main.post { if (generation == token) result(pitch) }
                        }
                    }
                } catch (_: Exception) {
                    main.post { if (generation == token) { stop(); failed("Microphone stopped. Check microphone access, then try again.") } }
                } finally {
                    synchronized(lock) { if (recorder === audio) recorder = null; audio.release() }
                }
            }, "practice-tuner").start()
            return true
        } catch (_: Exception) {
            synchronized(lock) { recorder = null; runCatching { created?.release() } }
            failed("Tuner could not start. Check microphone access and try again.")
            return false
        }
    }

    fun stop() {
        ++generation
        synchronized(lock) { recorder?.let { runCatching { it.stop() } }; recorder = null }
        // The stopped reader owns release, so stop never races a read against release.
    }
}
