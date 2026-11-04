package com.mrayover.practicelogger

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

class LibraryAudioViewModel(app: Application) : AndroidViewModel(app) {
    private val db = BlockDatabase.appInstance(app)
    private val library = MediaLibraryRepository(db, File(app.filesDir, "media-library"))
    val controller = LibraryAudioController(app, library, viewModelScope)
    val blockAudio = BlockAudioRepository(db)
    val markers = MasterAudioMarkerRepository(db)
    private val cache = AudioWaveformCache(library, File(app.cacheDir, "audio-waveforms"))
    var waveform by mutableStateOf<AudioWaveform?>(null); private set
    var waveformError by mutableStateOf<String?>(null); private set
    var message by mutableStateOf<String?>(null); private set
    var saving by mutableStateOf(false); private set
    private var analysis: Job? = null
    private var id: String? = null
    private var blockSession: Job? = null
    private val writes = Mutex()
    private var blockKey: Pair<String, String>? = null
    private var lastSaved: BlockAudioState? = null
    var selectedMarkerId by mutableStateOf<String?>(null); private set
    var blockAudioError by mutableStateOf<String?>(null); private set

    fun openBlock(blockId: String, mediaId: String, duration: Long) {
        if (blockKey == (blockId to mediaId)) return
        close()
        blockKey = blockId to mediaId
        blockSession = viewModelScope.launch {
            try {
                val saved = writes.withLock { blockAudio.state(blockId, mediaId) }
                lastSaved = saved
                selectedMarkerId = saved?.selectedMarkerId
                val range = saved != null && validAudioRange(saved.aMs, saved.bMs, duration)
                open(mediaId, LibraryAudioState(mediaId = mediaId, durationMs = duration,
                    positionMs = saved?.positionMs?.coerceIn(0, duration) ?: 0,
                    speedPercent = saved?.speedPercent ?: 100,
                    aMs = if (range) saved!!.aMs else 0, bMs = if (range) saved!!.bMs else duration,
                    repeatRange = range && saved!!.repeat))
                launch {
                    var previous = controller.state.value
                    controller.state.collect { current ->
                        if (current.speedPercent != previous.speedPercent || current.aMs != previous.aMs ||
                            current.bMs != previous.bMs || current.repeatRange != previous.repeatRange ||
                            (previous.playing && !current.playing)) {
                            checkpoint()
                            delay(250)
                        }
                        previous = current
                    }
                }
                while (isActive) {
                    delay(750)
                    checkpoint()
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { blockAudioError = "Could not restore Block audio. Close and reopen Transcribe to retry." }
        }
    }

    fun selectBlockMarker(id: String) {
        selectedMarkerId = id
        checkpoint()
    }

    fun checkpoint() {
        val (block, media) = blockKey ?: return
        val playback = controller.state.value
        if (playback.mediaId != media || playback.preparing || playback.durationMs <= 0) return
        val snapshot = BlockAudioState(block, media, playback.positionMs, playback.speedPercent,
            playback.aMs, playback.bMs, playback.repeatRange, selectedMarkerId)
        if (snapshot == lastSaved) return
        lastSaved = snapshot
        viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                writes.withLock { blockAudio.saveState(snapshot) }
                if (blockKey == (block to media)) blockAudioError = null
            }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                if (blockKey == (block to media)) {
                    if (lastSaved == snapshot) lastSaved = null
                    blockAudioError = "Could not save Block audio settings. The app will retry."
                }
            }
        }
    }

    fun open(mediaId: String, initial: LibraryAudioState? = null) {
        if (id == mediaId) return
        id = mediaId; controller.open(mediaId, initial); retryWaveform()
    }
    fun retryWaveform() {
        val mediaId = id ?: return
        analysis?.cancel(); waveform = null; waveformError = null
        analysis = viewModelScope.launch {
            try { waveform = cache.load(mediaId) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { waveformError = "Waveform could not load. Retry analysis." }
        }
    }
    fun save(markerId: String?, start: Long, end: Long?, label: String, note: String, done: () -> Unit) {
        if (saving) return
        val mediaId = id ?: return
        saving = true; message = null
        viewModelScope.launch {
            try { markers.save(mediaId, markerId, start, end, label, note); done() }
            catch (failure: Exception) { message = failure.message ?: "Could not save marker. Try again." }
            finally { saving = false }
        }
    }
    fun delete(markerId: String, done: () -> Unit) {
        if (saving) return
        val mediaId = id ?: return
        saving = true; message = null
        viewModelScope.launch {
            try { markers.delete(mediaId, markerId); done() }
            catch (_: Exception) { message = "Could not delete marker. Try again." }
            finally { saving = false }
        }
    }
    fun close() {
        if (blockKey != null) {
            controller.pause()
            checkpoint()
        }
        blockSession?.cancel()
        blockKey = null
        lastSaved = null
        selectedMarkerId = null
        blockAudioError = null
        analysis?.cancel()
        controller.close()
        id = null
        waveform = null
        message = null
    }
    override fun onCleared() { controller.dispose() }
}
