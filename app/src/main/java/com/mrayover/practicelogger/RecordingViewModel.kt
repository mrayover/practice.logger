package com.mrayover.practicelogger

import android.Manifest
import android.app.Application
import android.content.*
import android.content.pm.PackageManager
import android.media.*
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

@Suppress("DEPRECATION")
class RecordingViewModel(application: Application) : AndroidViewModel(application) {
    companion object { private var captureOwner: RecordingViewModel? = null }
    private val app = application
    private val db = BlockDatabase.appInstance(application)
    private val repository = TakeRepository(db, application)
    val takes = repository.takes.catch { message = "Could not load recordings. Close and reopen to retry."; emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    var ready by mutableStateOf(false); private set
    var busy by mutableStateOf(false); private set
    var recording by mutableStateOf(false); private set
    var elapsedMs by mutableLongStateOf(0L); private set
    var playingId by mutableStateOf<String?>(null); private set
    var preparingId by mutableStateOf<String?>(null); private set
    var message by mutableStateOf<String?>(null); private set
    private var visibleBlockId: String? = null
    private var foreground = false
    private var recorder: MediaRecorder? = null
    private var pending: PendingTake? = null
    private var captureStart = 0L
    private var player: MediaPlayer? = null
    private val manager = app.getSystemService(AudioManager::class.java)
    private val attributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
    private val focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(attributes)
        .setWillPauseWhenDucked(true).setOnAudioFocusChangeListener({ change ->
            if (change != AudioManager.AUDIOFOCUS_GAIN) stopPlayback()
        }, Handler(Looper.getMainLooper())).build()
    private val noisy = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            stopPlayback(); finishCapture("Audio route changed. Recording stopped.")
        }
    }

    init {
        if (Build.VERSION.SDK_INT >= 33) app.registerReceiver(noisy, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY), Context.RECEIVER_NOT_EXPORTED)
        else app.registerReceiver(noisy, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
        viewModelScope.launch {
            try {
                val cleaned = withContext(Dispatchers.IO) { repository.recover() }
                ready = true
                if (cleaned > 0) message = "An unfinished recording was removed after interruption. Saved takes were kept."
            } catch (_: Exception) { message = "Recording storage could not be opened. Check free space and restart to retry." }
        }
        viewModelScope.launch {
            while (isActive) {
                delay(250)
                if (recording) {
                    elapsedMs = SystemClock.elapsedRealtime() - captureStart
                    try {
                        when {
                            !hasMicrophonePermission() -> finishCapture("Microphone permission was removed. Take not saved.", discard = true)
                            Build.VERSION.SDK_INT >= 29 && recorder?.activeRecordingConfiguration?.isClientSilenced == true ->
                                finishCapture("The microphone is in use or was silenced. Take not saved. Try again when it is available.", discard = true)
                            repository.availableBytes() < 512 * 1024 -> finishCapture("Storage is low. Recording stopped.")
                        }
                    } catch (_: Exception) { finishCapture("Microphone failed. Take not saved. Try again.", discard = true) }
                }
            }
        }
    }

    fun enterBlock(id: String) {
        if (visibleBlockId != id) { stopPlayback(); finishCapture(); visibleBlockId = id }
    }
    fun leaveBlock() { visibleBlockId = null; stopPlayback(); finishCapture() }
    fun setForeground(value: Boolean) {
        foreground = value
        if (!value) { stopPlayback(); finishCapture("Recording stopped when the app left the foreground.") }
    }
    fun hasMicrophonePermission() = app.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    fun permissionResult(granted: Boolean) {
        message = if (granted) "Microphone allowed. Tap Record take to begin." else
            "Microphone permission was not allowed. Tap Record take to retry; if Android no longer asks, allow Microphone in this app's Android settings."
    }
    @Suppress("DEPRECATION")
    fun startCapture(blockId: String) {
        if (!ready || busy || recording || !foreground || visibleBlockId != blockId) return
        if (!hasMicrophonePermission()) { permissionResult(false); return }
        if (captureOwner != null) { message = "Another app window is recording. Stop that take first."; return }
        captureOwner = this
        stopPlayback(); message = null
        var created: MediaRecorder? = null
        var draft: PendingTake? = null
        try {
            draft = repository.prepare(blockId)
            val audio = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(app) else MediaRecorder()
            created = audio
            audio.setAudioSource(MediaRecorder.AudioSource.MIC)
            audio.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            audio.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            audio.setAudioChannels(1); audio.setAudioSamplingRate(44_100); audio.setAudioEncodingBitRate(128_000)
            audio.setOutputFile(draft.file.absolutePath)
            audio.setOnErrorListener { source, _, _ ->
                if (recorder === source) finishCapture("Microphone or storage failed. Take not saved. Try again.", discard = true)
            }
            audio.prepare(); audio.start()
            recorder = audio; pending = draft; captureStart = SystemClock.elapsedRealtime(); elapsedMs = 0; recording = true
        } catch (_: Exception) {
            captureOwner = null
            runCatching { created?.release() }; draft?.let(repository::abandon)
            message = "Could not record. Check microphone permission, other apps using the microphone, and free storage, then retry."
        }
    }
    /** Microphone stop/release is synchronous; only validation/commit continues after leaving. */
    fun finishCapture(reason: String? = null, discard: Boolean = false) {
        val audio = recorder ?: return
        val draft = pending ?: return
        recorder = null; pending = null; recording = false
        val stopped = runCatching { audio.stop() }.isSuccess
        runCatching { audio.release() }
        if (captureOwner === this) captureOwner = null
        busy = true
        viewModelScope.launch {
            try {
                if (!stopped || discard) {
                    withContext(Dispatchers.IO) { repository.abandon(draft) }
                    message = reason ?: "No usable audio was captured. Record a little longer and retry."
                } else {
                    withContext(Dispatchers.IO) { repository.save(draft) }
                    message = if (reason == null) "Take saved." else "$reason Take saved."
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                withContext(Dispatchers.IO) { repository.abandon(draft) }
                message = "Take could not be saved. Check storage and retry. Earlier takes were kept."
            } finally { busy = false }
        }
    }

    private fun work(action: suspend () -> Unit) {
        if (!ready || busy || recording) return
        busy = true
        viewModelScope.launch {
            try { action() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { message = "Could not complete this recording action. Check storage and file access, then retry. Your other takes were kept." }
            finally { busy = false }
        }
    }
    fun play(take: BlockTake) = work {
        stopPlayback()
        val file = withContext(Dispatchers.IO) { repository.audioFile(take) }
        if (!foreground || visibleBlockId != take.blockId) return@work
        if (manager.requestAudioFocus(focus) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            message = "Audio output is in use. Try playback again."; return@work
        }
        try {
            val audio = MediaPlayer()
            player = audio; preparingId = take.id; message = null
            audio.setAudioAttributes(attributes); audio.setDataSource(file.absolutePath)
            audio.setOnPreparedListener {
                if (player === it && foreground && visibleBlockId == take.blockId) {
                    try { it.start(); preparingId = null; playingId = take.id }
                    catch (_: Exception) { stopPlayback(); message = "Playback could not start. Try again." }
                }
            }
            audio.setOnCompletionListener { if (player === it) stopPlayback() }
            audio.setOnErrorListener { source, _, _ ->
                if (player === source) { stopPlayback(); message = "This audio file could not be played. The saved take was kept." }; true
            }
            audio.prepareAsync()
        } catch (failure: Exception) { stopPlayback(); throw failure }
    }
    fun stopPlayback() {
        val audio = player; player = null; playingId = null; preparingId = null
        runCatching { audio?.release() }; runCatching { manager.abandonAudioFocusRequest(focus) }
    }
    fun rename(take: BlockTake, label: String) = work {
        withContext(Dispatchers.IO) { repository.rename(take.id, label) }; message = "Take renamed."
    }
    fun delete(take: BlockTake) = work {
        stopPlayback()
        val cleaned = withContext(Dispatchers.IO) { repository.delete(take.id) }
        message = if (cleaned) "Take deleted." else "Take deleted. File cleanup will retry after restart."
    }
    fun export(take: BlockTake, uri: Uri) = work {
        stopPlayback()
        try {
            withContext(Dispatchers.IO) {
                checkNotNull(app.contentResolver.openOutputStream(uri, "wt")).use { repository.export(take, it) }
            }
            message = "Audio exported. The saved take is still in this Block."
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { message = "Export failed. The original take was kept; the destination may be incomplete. Choose a new file and retry." }
    }
    override fun onCleared() {
        runCatching { recorder?.release() }; recorder = null // Pending capture is recovered on next cold start.
        pending?.let(repository::abandon); pending = null
        if (captureOwner === this) captureOwner = null
        stopPlayback(); runCatching { app.unregisterReceiver(noisy) }; super.onCleared()
    }
}
