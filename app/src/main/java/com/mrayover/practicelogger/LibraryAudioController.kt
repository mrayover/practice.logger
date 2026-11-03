package com.mrayover.practicelogger

import android.content.*
import android.media.*
import android.os.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class LibraryAudioState(val mediaId: String? = null, val durationMs: Long = 0, val positionMs: Long = 0,
    val preparing: Boolean = false, val playing: Boolean = false, val speedPercent: Int = 100,
    val aMs: Long = 0, val bMs: Long = 0, val repeatRange: Boolean = false, val repeatFile: Boolean = false,
    val error: String? = null)

/** One foreground platform engine. No Compose, Block, take, timer or practice-session dependency.
 * Public inputs are canonical IDs/source milliseconds; paths never leave repository/engine internals.
 */
class LibraryAudioController(context: Context, private val repository: MediaLibraryRepository, private val scope: CoroutineScope) {
    private val app = context.applicationContext
    private val manager = app.getSystemService(AudioManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private val attributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
    private val mutable = MutableStateFlow(LibraryAudioState())
    val state = mutable.asStateFlow()
    private var player: MediaPlayer? = null
    private var opening: Job? = null
    private var generation = 0
    private var foreground = true
    private var seekPending = false
    private var queuedSeek: Long? = null
    private var resumeAfterSeek = false
    private var selectedOnce = false
    private val focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(attributes)
        .setWillPauseWhenDucked(true).setOnAudioFocusChangeListener({ change ->
            if (change != AudioManager.AUDIOFOCUS_GAIN) suspendPlayback("Audio interrupted. Press Play to continue.")
        }, handler).build()
    private val noisy = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) { suspendPlayback("Audio route changed. Press Play to continue.") }
    }
    private val ticker = object : Runnable {
        override fun run() {
            val audio = player
            if (audio != null && mutable.value.playing && !seekPending) guarded {
                val position = audio.currentPosition.toLong().coerceIn(0, mutable.value.durationMs)
                mutable.value = mutable.value.copy(positionMs = position)
                if (position >= mutable.value.bMs) {
                    if (mutable.value.repeatRange) seek(mutable.value.aMs)
                    else if (selectedOnce) { selectedOnce = false; pause(); seek(mutable.value.bMs) }
                }
            }
            if (mutable.value.playing) handler.postDelayed(this, 15)
        }
    }
    init {
        if (Build.VERSION.SDK_INT >= 33) app.registerReceiver(noisy, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY), Context.RECEIVER_NOT_EXPORTED)
        else app.registerReceiver(noisy, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
    }
    private fun guarded(action: () -> Unit) {
        try { action() } catch (_: Exception) { suspendPlayback("Playback failed. Retry the original.") }
    }
    fun open(id: String) {
        if (mutable.value.mediaId == id && (player != null || opening?.isActive == true)) return
        val previous = mutable.value.takeIf { it.mediaId == id }
        releasePlayer()
        mutable.value = previous?.copy(playing = false, preparing = true, error = null) ?: LibraryAudioState(mediaId = id, preparing = true)
        prepare(id, false)
    }
    private fun prepare(id: String, start: Boolean) {
        val token = generation
        opening = scope.launch {
            try {
                val (asset, file) = repository.original(id, "AUDIO")
                if (generation != token || !foreground) return@launch
                val duration = checkNotNull(asset.durationMs)
                mutable.value = mutable.value.copy(durationMs = duration, bMs = mutable.value.bMs.takeIf { it in 1..duration } ?: duration)
                val audio = MediaPlayer(); player = audio
                audio.setAudioAttributes(attributes); audio.setDataSource(file.path)
                audio.setOnErrorListener { source, _, _ -> if (player === source) suspendPlayback("This original could not be played. Retry playback."); true }
                audio.setOnPreparedListener {
                    if (player === it && generation == token && foreground) guarded {
                        mutable.value = mutable.value.copy(preparing = false)
                        resumeAfterSeek = start
                        if (mutable.value.positionMs > 0) seek(mutable.value.positionMs) else if (start) playPrepared()
                    }
                }
                audio.setOnSeekCompleteListener {
                    if (player === it) guarded {
                        seekPending = false
                        val queued = queuedSeek; queuedSeek = null
                        if (queued != null) seek(queued)
                        else {
                            mutable.value = mutable.value.copy(positionMs = it.currentPosition.toLong().coerceIn(0, mutable.value.durationMs))
                            if (resumeAfterSeek) { resumeAfterSeek = false; playPrepared() }
                        }
                    }
                }
                audio.setOnCompletionListener {
                    if (player === it) {
                        mutable.value = mutable.value.copy(playing = false, positionMs = mutable.value.durationMs)
                        if (mutable.value.repeatRange || mutable.value.repeatFile) {
                            resumeAfterSeek = true; seek(if (mutable.value.repeatRange) mutable.value.aMs else 0)
                        } else manager.abandonAudioFocusRequest(focus)
                    }
                }
                audio.prepareAsync()
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { if (generation == token) suspendPlayback("Original is missing, changed or unplayable. Retry playback.") }
        }
    }
    fun play() {
        if (!foreground || mutable.value.preparing || mutable.value.mediaId == null) return
        mutable.value = mutable.value.copy(error = null)
        if (player == null) {
            mutable.value = mutable.value.copy(preparing = true)
            prepare(mutable.value.mediaId!!, true)
        } else if (mutable.value.positionMs >= mutable.value.durationMs ||
            (mutable.value.repeatRange && mutable.value.positionMs !in mutable.value.aMs until mutable.value.bMs)) {
            resumeAfterSeek = true; seek(if (mutable.value.repeatRange) mutable.value.aMs else 0)
        } else if (seekPending) resumeAfterSeek = true else playPrepared()
    }
    private fun playPrepared() = guarded {
        if (!foreground) return@guarded
        if (manager.requestAudioFocus(focus) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            mutable.value = mutable.value.copy(error = "Audio output is in use. Try Play again."); return@guarded
        }
        val audio = checkNotNull(player)
        audio.playbackParams = PlaybackParams().setPitch(1f).setSpeed(mutable.value.speedPercent / 100f)
        audio.start(); mutable.value = mutable.value.copy(playing = true)
        handler.removeCallbacks(ticker); handler.post(ticker)
    }
    fun pause() = guarded {
        resumeAfterSeek = false
        if (mutable.value.playing) player?.pause()
        mutable.value = mutable.value.copy(playing = false)
        handler.removeCallbacks(ticker)
        manager.abandonAudioFocusRequest(focus)
    }
    fun stop() { selectedOnce = false; pause(); seek(if (mutable.value.repeatRange) mutable.value.aMs else 0) }
    fun playSelection() { selectedOnce = true; seek(mutable.value.aMs); play() }
    fun seek(positionMs: Long) {
        val target = positionMs.coerceIn(0, mutable.value.durationMs)
        mutable.value = mutable.value.copy(positionMs = target)
        if (mutable.value.preparing || player == null) return
        if (seekPending) { queuedSeek = target; return }
        guarded { seekPending = true; player!!.seekTo(target, MediaPlayer.SEEK_CLOSEST) }
    }
    fun speed(percent: Int) {
        require(percent in 50..100 && percent % 5 == 0)
        mutable.value = mutable.value.copy(speedPercent = percent)
        if (mutable.value.playing) guarded { player?.playbackParams = PlaybackParams().setPitch(1f).setSpeed(percent / 100f) }
    }
    fun range(a: Long, b: Long): Boolean {
        if (!validAudioRange(a, b, mutable.value.durationMs)) return false
        mutable.value = mutable.value.copy(aMs = a, bMs = b)
        // Move only when the currently repeating phrase no longer contains the cursor.
        if (mutable.value.repeatRange && mutable.value.positionMs !in a until b) seek(a)
        return true
    }
    fun repeatRange(enabled: Boolean) {
        if (enabled && !validAudioRange(mutable.value.aMs, mutable.value.bMs, mutable.value.durationMs)) return
        mutable.value = mutable.value.copy(repeatRange = enabled, repeatFile = if (enabled) false else mutable.value.repeatFile)
        selectedOnce = false
        if (enabled && mutable.value.positionMs !in mutable.value.aMs until mutable.value.bMs) seek(mutable.value.aMs)
    }
    fun repeatFile(enabled: Boolean) { selectedOnce = false; mutable.value = mutable.value.copy(repeatFile = enabled, repeatRange = if (enabled) false else mutable.value.repeatRange) }
    fun setForeground(value: Boolean) { foreground = value; if (!value) suspendPlayback(null) }
    private fun suspendPlayback(error: String?) {
        // Keep source position/range/speed, but release decoder and never auto-resume.
        if (mutable.value.playing && !seekPending) runCatching { mutable.value = mutable.value.copy(positionMs = player?.currentPosition?.toLong() ?: mutable.value.positionMs) }
        releasePlayer(); mutable.value = mutable.value.copy(preparing = false, playing = false, error = error)
    }
    private fun releasePlayer() {
        handler.removeCallbacks(ticker)
        generation++; opening?.cancel(); opening = null
        val old = player; player = null; seekPending = false; queuedSeek = null; resumeAfterSeek = false
        runCatching { old?.release() }; runCatching { manager.abandonAudioFocusRequest(focus) }
    }
    fun close() { selectedOnce = false; releasePlayer(); mutable.value = LibraryAudioState() }
    fun dispose() { close(); handler.removeCallbacks(ticker); runCatching { app.unregisterReceiver(noisy) } }
}
