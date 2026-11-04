package com.mrayover.practicelogger

import android.content.*
import android.media.*
import android.os.*

/** Standalone owns focus; embedded borrows the reference controller's focus and interruptions.
 * Embedded construction requires that owner. Closing this engine never closes/pauses the owner.
 * Call on the main thread; PCM and output writes run on one audio thread.
 */
class PianoAudioEngine(context: Context, private val reference: LibraryAudioController? = null,
    private val interrupted: (String?) -> Unit = {}) {
    private val app = context.applicationContext
    private val manager = app.getSystemService(AudioManager::class.java)
    private val main = Handler(Looper.getMainLooper())
    private val pcm = PianoPcm()
    private val attributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
    private val focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(attributes)
        .setWillPauseWhenDucked(true).setOnAudioFocusChangeListener({ change ->
            if (change != AudioManager.AUDIOFOCUS_GAIN) interrupt(null)
        }, main).build()
    private var foreground = true
    private var closed = false
    @Volatile private var generation = 0
    private var companionOutput = false
    private var track: AudioTrack? = null
    @Volatile internal var framesWritten = 0L; private set
    @Volatile internal var nonZeroFramesWritten = 0L; private set
    internal fun voiceCount() = pcm.voiceCount()
    private val removeCompanion = reference?.registerCompanion { interrupt(null) }
    private val noisy = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) { interrupt(null) }
    }
    init {
        if (Build.VERSION.SDK_INT >= 33) app.registerReceiver(noisy, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY), Context.RECEIVER_NOT_EXPORTED)
        else app.registerReceiver(noisy, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
    }
    fun noteOn(token: Long, note: Int): Boolean {
        require(note in 24..96)
        if (closed || !foreground) return false
        if (track == null && !start()) return false
        return pcm.noteOn(token, note)
    }
    fun noteOff(token: Long) = pcm.noteOff(token)
    fun stopAll() = pcm.stopAll()
    fun setForeground(value: Boolean) { foreground = value; if (!value) interrupt(null) }
    private fun interrupt(message: String?) { stopOutput(); interrupted(message) }
    private fun start(): Boolean {
        try {
            val granted = if (reference == null) manager.requestAudioFocus(focus) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
                else reference.acquireCompanionOutput().also { companionOutput = it }
            if (!granted) {
                interrupt("Audio is in use. Try a key again."); return false
            }
            val minimum = AudioTrack.getMinBufferSize(48_000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
            check(minimum > 0)
            val audio = AudioTrack.Builder().setAudioAttributes(attributes)
                .setAudioFormat(AudioFormat.Builder().setSampleRate(48_000).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).setEncoding(AudioFormat.ENCODING_PCM_16BIT).build())
                .setTransferMode(AudioTrack.MODE_STREAM).setBufferSizeInBytes(maxOf(minimum, 960)).setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY).build()
            track = audio; check(audio.state == AudioTrack.STATE_INITIALIZED)
            val token = ++generation
            audio.play()
            Thread({
                try {
                    Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
                    val chunk = ShortArray(240)
                    var quietFrames = 0
                    while (generation == token) {
                        pcm.render(chunk)
                        var offset = 0
                        while (offset < chunk.size && generation == token) {
                            val written = audio.write(chunk, offset, chunk.size - offset, AudioTrack.WRITE_BLOCKING)
                            check(written > 0); offset += written
                        }
                        if (generation == token) {
                            framesWritten += offset; nonZeroFramesWritten += chunk.count { it.toInt() != 0 }
                            quietFrames = if (pcm.voiceCount() == 0) quietFrames + chunk.size else 0
                            if (quietFrames >= 48_000) { main.post { if (generation == token && pcm.voiceCount() == 0) stopOutput() }; quietFrames = 0 }
                        }
                    }
                } catch (_: Exception) { main.post { if (generation == token) interrupt("Piano output stopped. Try a key again.") } }
            }, "reference-piano").start()
            return true
        } catch (_: Exception) { interrupt("Piano output is unavailable. Try a key again."); return false }
    }
    private fun stopOutput() {
        generation++; pcm.clear()
        val old = track; track = null
        runCatching { old?.pause() }; runCatching { old?.flush() }; runCatching { old?.release() }
        if (companionOutput) {
            companionOutput = false
            reference?.releaseCompanionOutput()
        }
        if (reference == null) runCatching { manager.abandonAudioFocusRequest(focus) }
    }
    fun close() { if (closed) return; closed = true; removeCompanion?.invoke(); stopOutput(); app.unregisterReceiver(noisy) }
}
