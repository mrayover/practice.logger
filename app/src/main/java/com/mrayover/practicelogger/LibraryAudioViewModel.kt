package com.mrayover.practicelogger

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
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
    var switching by mutableStateOf(false); private set
    internal var sourceEditBusy by mutableStateOf(false)
    private var contextGeneration = 0
    internal var beforeCheckpoint: suspend (BlockAudioState) -> Unit = {}

    fun openBlock(blockId: String, mediaId: String, duration: Long, selected: () -> Unit = {}) {
        if (switching || sourceEditBusy || saving) return
        if (blockKey == (blockId to mediaId)) { selected(); return }
        switching = true
        val generation = contextGeneration
        viewModelScope.launch {
            try {
                controller.pauseForOtherOutput()
                checkpointAwait()
                val saved = writes.withLock { db.withTransaction {
                    check(db.blockMedia().contains(blockId, "AUDIO", mediaId)) { "This audio reference was removed." }
                    blockAudio.state(blockId, mediaId)
                } }
                if (generation != contextGeneration) return@launch
                blockSession?.cancel()
                analysis?.cancel()
                contextGeneration++
                controller.close()
                id = null
                blockKey = blockId to mediaId
                lastSaved = saved
                selectedMarkerId = saved?.selectedMarkerId
                blockAudioError = null
                val range = saved?.takeIf { validAudioRange(it.aMs, it.bMs, duration) }
                open(mediaId, LibraryAudioState(mediaId = mediaId, durationMs = duration,
                    positionMs = saved?.positionMs?.coerceIn(0, duration) ?: 0,
                    speedPercent = saved?.speedPercent ?: 100,
                    aMs = range?.aMs ?: 0, bMs = range?.bMs ?: duration,
                    repeatRange = range?.repeat ?: false))
                selected()
                blockSession = viewModelScope.launch {
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
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { if (generation == contextGeneration) blockAudioError = "Could not switch audio. Your current source and settings were kept. Check storage, then choose the source again to retry." }
            finally { switching = false }
        }
    }

    fun selectBlockMarker(id: String?) {
        selectedMarkerId = id
        checkpoint()
    }

    internal fun checkpointBefore(action: () -> Unit) {
        if (switching || sourceEditBusy || saving) return
        switching = true
        val generation = contextGeneration
        controller.pauseForOtherOutput()
        viewModelScope.launch {
            try { checkpointAwait(); if (generation == contextGeneration) action() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { blockAudioError = "Could not save audio settings. Check storage and retry Record Reference." }
            finally { switching = false }
        }
    }

    fun checkpoint() {
        val snapshot = checkpointSnapshot() ?: return
        viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            try { persistCheckpoint(snapshot) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                if (blockKey == (snapshot.blockId to snapshot.mediaId))
                    blockAudioError = "Could not save Block audio settings. The app will retry."
            }
        }
    }

    private fun checkpointSnapshot(): BlockAudioState? {
        val (block, media) = blockKey ?: return null
        val playback = controller.state.value
        if (playback.mediaId != media || playback.durationMs <= 0) return null
        return BlockAudioState(block, media, playback.positionMs, playback.speedPercent,
            playback.aMs, playback.bMs, playback.repeatRange, selectedMarkerId)
    }

    /** Final checkpoint is awaited behind every earlier write; failed writes never count as saved. */
    internal suspend fun checkpointAwait() {
        val snapshot = checkpointSnapshot()
        if (snapshot != null) persistCheckpoint(snapshot)
        else writes.withLock { /* drain queued writes even during preparation */ }
    }

    private suspend fun persistCheckpoint(snapshot: BlockAudioState) {
        val generation = contextGeneration
        writes.withLock {
            if (snapshot != lastSaved) {
                beforeCheckpoint(snapshot)
                blockAudio.saveState(snapshot)
                check(blockAudio.state(snapshot.blockId, snapshot.mediaId) == snapshot) { "This audio reference was removed before its settings could be saved." }
            }
            if (generation == contextGeneration && blockKey == (snapshot.blockId to snapshot.mediaId)) {
                lastSaved = snapshot
                blockAudioError = null
            }
        }
    }

    fun open(mediaId: String, initial: LibraryAudioState? = null) {
        if (id == mediaId) return
        id = mediaId; controller.open(mediaId, initial); retryWaveform()
    }
    fun retryWaveform() {
        val mediaId = id ?: return
        val generation = contextGeneration
        analysis?.cancel(); waveform = null; waveformError = null
        analysis = viewModelScope.launch {
            try { val loaded = cache.load(mediaId); if (id == mediaId && generation == contextGeneration) waveform = loaded }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { if (id == mediaId && generation == contextGeneration) waveformError = "Waveform could not load. Retry analysis." }
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
        contextGeneration++
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
