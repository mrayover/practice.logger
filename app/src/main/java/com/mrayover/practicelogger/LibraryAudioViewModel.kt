package com.mrayover.practicelogger

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import java.io.File

class LibraryAudioViewModel(app: Application) : AndroidViewModel(app) {
    private val db = BlockDatabase.appInstance(app)
    private val library = MediaLibraryRepository(db, File(app.filesDir, "media-library"))
    val controller = LibraryAudioController(app, library, viewModelScope)
    val markers = MasterAudioMarkerRepository(db)
    private val cache = AudioWaveformCache(library, File(app.cacheDir, "audio-waveforms"))
    var waveform by mutableStateOf<AudioWaveform?>(null); private set
    var waveformError by mutableStateOf<String?>(null); private set
    var message by mutableStateOf<String?>(null); private set
    var saving by mutableStateOf(false); private set
    private var analysis: Job? = null
    private var id: String? = null
    fun open(mediaId: String) {
        if (id == mediaId) return
        id = mediaId; controller.open(mediaId); retryWaveform()
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
    fun close() { analysis?.cancel(); controller.close(); id = null; waveform = null; message = null }
    override fun onCleared() { controller.dispose() }
}
