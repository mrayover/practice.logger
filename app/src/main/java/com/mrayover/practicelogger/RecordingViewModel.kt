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
    companion object {
        @Volatile private var captureOwner: RecordingViewModel? = null
        internal val isCapturingAudio get() = captureOwner != null
    }
    private val app = application
    private val db = BlockDatabase.appInstance(application)
    private val repository = TakeRepository(db, application)
    val takes = repository.takes.catch { message = "Could not load recordings. Close and reopen to retry."; emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    var ready by mutableStateOf(false); private set
    var busy by mutableStateOf(false); private set
    var recording by mutableStateOf(false); private set
    var phrasing by mutableStateOf(false); private set
    var countInBeat by mutableIntStateOf(0); private set
    var countInTotal by mutableLongStateOf(4L); private set
    private val countInPreferences = CountInPreferences(app)
    var defaultCountIn by mutableLongStateOf(countInPreferences.read()); private set
    internal fun reloadCountIn() { defaultCountIn = countInPreferences.read() }
    fun saveDefaultCountIn(text: String) = work {
        val value = text.trim().toLongOrNull()
        if (value == null || value <= 0) { message = "Enter a positive whole count-in beat count."; return@work }
        try { PhraseSpec(300, 1, value) } catch (failure: IllegalArgumentException) { message = failure.message; return@work }
        withContext(Dispatchers.IO) { countInPreferences.save(value) }
        defaultCountIn = value; message = "Default count-in saved."
    }
    var phraseDurationMs by mutableLongStateOf(0L); private set
    var phraseClickEnabled by mutableStateOf(false); private set
    var elapsedMs by mutableLongStateOf(0L); private set
    var playingId by mutableStateOf<String?>(null); private set
    var preparingId by mutableStateOf<String?>(null); private set
    var message by mutableStateOf<String?>(null); private set
    var backingTrack by mutableStateOf<PendingTake?>(null); private set
    var importedSelection by mutableStateOf<BlockTake?>(null); private set
    private var importJob: Job? = null
    private var importGeneration = 0
    fun importedSelectionHandled(id: String) { if (importedSelection?.id == id) importedSelection = null }
    fun chooseBackingTrack(blockId: String, uri: Uri?) {
        if (uri == null || !ready || busy || recording || reviewTake != null || !recordingScreenOpen || visibleBlockId != blockId) return
        val generation = ++importGeneration
        busy = true; message = "Validating audio…"
        importJob = viewModelScope.launch {
            var staged: PendingTake? = null
            try {
                withContext(Dispatchers.IO) {
                    val name = app.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) cursor.getString(0) else null
                    } ?: error("The selected file has no filename.")
                    staged = AudioImport(repository).prepare(blockId, name) { checkNotNull(app.contentResolver.openInputStream(uri)) }
                }
                if (generation == importGeneration && recordingScreenOpen && visibleBlockId == blockId) {
                    backingTrack?.let(repository::abandon); backingTrack = staged; staged = null
                    message = null
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) { message = "Audio not imported. ${failure.message ?: "Check the file and retry."} Saved takes and selection were kept." }
            finally {
                withContext(NonCancellable + Dispatchers.IO) { staged?.let(repository::abandon) }
                busy = false; importJob = null
            }
        }
    }
    fun saveBackingTrack(blockId: String) = work {
        val draft = backingTrack?.takeIf { it.blockId == blockId } ?: return@work
        if (reviewTake != null) return@work
        val saved = withContext(NonCancellable + Dispatchers.IO) { repository.save(draft) }
        stopPlayback(); backingTrack = null; importedSelection = saved; message = "Backing track saved."
    }
    private fun clearImport() {
        importGeneration++; importJob?.cancel()
        backingTrack?.let(repository::abandon); backingTrack = null
    }
    private var visibleBlockId: String? = null
    private var recordingScreenOpen = false
    private var foreground = false
    private var recorder: MediaRecorder? = null
    private var pending: PendingTake? = null
    var reviewTake by mutableStateOf<BlockTake?>(null); private set
    private var reviewPending: PendingTake? = null
    private var captureStart = 0L
    private var referenceCapture = false
    private var captureDevices: Set<Int>? = null
    private fun audioDevices() = manager.getDevices(AudioManager.GET_DEVICES_ALL).map { it.id }.toSet()
    internal var keptReferenceId by mutableStateOf<String?>(null); private set
    internal var referenceReplacement by mutableStateOf<String?>(null); private set
    private val referenceLibrary by lazy { MediaLibraryRepository(db, java.io.File(app.filesDir, "media-library")) }
    private val referenceLinks by lazy {
        BlockMediaRepository(db, referenceLibrary, ImageReferenceRepository(db, java.io.File(app.filesDir, "block-images")))
    }
    private var player: MediaPlayer? = null
    private var loopTrack: AudioTrack? = null
    private var phraseCapture: PhraseCapture? = null
    private var phraseJob: Job? = null
    private var audioGeneration = 0
    private val manager = app.getSystemService(AudioManager::class.java)
    private val attributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
    private val focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(attributes)
        .setWillPauseWhenDucked(true).setOnAudioFocusChangeListener({ change ->
            if (change != AudioManager.AUDIOFOCUS_GAIN) {
                stopPlayback()
                if (recording) finishCapture("Audio interrupted. Take discarded; saved recordings were kept.", discard = true)
            }
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
                if (recording && !phrasing) {
                    elapsedMs = SystemClock.elapsedRealtime() - captureStart
                    try {
                        when {
                            !hasMicrophonePermission() -> finishCapture("Microphone permission was removed. Take not saved.", discard = true)
                            Build.VERSION.SDK_INT >= 29 && recorder?.activeRecordingConfiguration?.isClientSilenced == true ->
                                finishCapture("The microphone is in use or was silenced. Take not saved. Try again when it is available.", discard = true)
                            referenceCapture && elapsedMs >= AudioImport.MAX_DURATION_MS - 2_000 ->
                                finishCapture("Reference recording reached its time limit.")
                            captureDevices?.let { it != audioDevices() } == true ->
                                finishCapture("Audio devices changed. Recording stopped.")
                            repository.availableBytes() < 512 * 1024 -> finishCapture("Storage is low. Recording stopped.")
                        }
                    } catch (_: Exception) { finishCapture("Microphone failed. Take not saved. Try again.", discard = true) }
                }
            }
        }
    }

    fun enterBlock(id: String?) {
        if (visibleBlockId != id) clearImport()
        if (reviewTake != null && reviewTake?.blockId != id) clearReview()
        if (!recordingScreenOpen || visibleBlockId != id) { stopPlayback(); finishCapture(); visibleBlockId = id }; recordingScreenOpen = true
    }
    fun leaveBlock() { clearImport(); recordingScreenOpen = false; visibleBlockId = null; stopPlayback(); finishCapture("Recording interrupted before review. Temporary audio discarded.", discard = true) }
    fun setForeground(value: Boolean) {
        foreground = value
        if (!value) { stopPlayback(); finishCapture("Recording stopped when the app left the foreground.") }
    }
    fun hasMicrophonePermission() = app.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    fun permissionResult(granted: Boolean) {
        message = if (granted) "Microphone allowed. Tap Record take or Record phrase to begin." else
            "Microphone permission was not allowed. Tap Record take to retry; if Android no longer asks, allow Microphone in this app's Android settings."
    }
    internal fun referencePermissionResult(granted: Boolean) {
        message = if (granted) "Microphone allowed. Tap Start Recording or Rerecord to begin." else
            "Microphone permission was not allowed. Try again or enable Microphone in Android settings."
    }
    @Suppress("DEPRECATION")
    fun startCapture(blockId: String?, reference: Boolean = false, alongsideReference: Boolean = false) {
        if (!ready || busy || recording || reviewTake != null || !foreground || !recordingScreenOpen || visibleBlockId != blockId) return
        if (!hasMicrophonePermission()) { permissionResult(false); return }
        if (captureOwner != null) { message = "Another app window is recording. Stop that take first."; return }
        captureOwner = this
        stopPlayback(); message = null
        var created: MediaRecorder? = null
        var draft: PendingTake? = null
        try {
            val devices = if (alongsideReference) audioDevices() else null
            draft = repository.prepare(blockId)
            val audio = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(app) else MediaRecorder()
            created = audio
            audio.setAudioSource(MediaRecorder.AudioSource.MIC)
            audio.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            audio.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            audio.setAudioChannels(1); audio.setAudioSamplingRate(44_100); audio.setAudioEncodingBitRate(128_000)
            audio.setOutputFile(draft.file.absolutePath)
            if (reference) {
                audio.setMaxDuration(AudioImport.MAX_DURATION_MS.toInt() - 1000)
                audio.setMaxFileSize(AudioImport.MAX_BYTES)
                audio.setOnInfoListener { source, what, _ ->
                    if (source === recorder && (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED ||
                            what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED)) {
                        finishCapture("Reference recording reached its limit.")
                    }
                }
            }
            audio.setOnErrorListener { source, _, _ ->
                if (recorder === source) finishCapture("Microphone or storage failed. Take not saved. Try again.", discard = true)
            }
            audio.prepare(); audio.start()
            referenceCapture = reference
            captureDevices = devices
            recorder = audio; pending = draft; captureStart = SystemClock.elapsedRealtime(); elapsedMs = 0; recording = true
        } catch (_: Exception) {
            captureOwner = null
            runCatching { created?.release() }; draft?.let(repository::abandon)
            message = "Could not record. Check microphone permission, other apps using the microphone, and free storage, then retry."
        }
    }
    /** Microphone stop/release is synchronous; usable bytes stay temporary until explicit Save. */
    fun finishCapture(reason: String? = null, discard: Boolean = false) {
        audioGeneration++
        if (phrasing) {
            phraseCapture?.cancel(); recording = false; busy = true
            message = reason ?: "Incomplete phrase cancelled. Earlier takes were kept."
            return
        }
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
                    val inspected = withContext(Dispatchers.IO) { repository.inspect(draft) }
                    if (recordingScreenOpen && visibleBlockId == draft.blockId) {
                        reviewPending = draft; reviewTake = inspected
                        message = reason?.let { "$it Review the temporary take before saving." }
                    } else withContext(Dispatchers.IO) { repository.abandon(draft) }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                withContext(NonCancellable + Dispatchers.IO) { repository.abandon(draft) }
                message = "Temporary take could not be prepared. Check storage and retry. Earlier takes were kept."
            } finally {
                withContext(NonCancellable + Dispatchers.IO) { if (reviewPending !== draft) repository.abandon(draft) }
                busy = false
            }
        }
    }

    fun startPhrase(blockId: String, spec: PhraseSpec) {
        if (!ready || busy || recording || reviewTake != null || !foreground || !recordingScreenOpen || visibleBlockId != blockId) return
        if (!hasMicrophonePermission()) { permissionResult(false); return }
        if (captureOwner != null) { message = "Another app window is recording. Stop that take first."; return }
        stopPlayback()
        if (manager.requestAudioFocus(focus) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            message = "Audio output is in use. Try the phrase again."; return
        }
        val generation = ++audioGeneration
        val capture = PhraseCapture(); phraseCapture = capture; captureOwner = this
        phrasing = true; recording = true; countInTotal = spec.countInBeats; countInBeat = 1; elapsedMs = 0; phraseDurationMs = spec.durationMs
        phraseClickEnabled = true; message = null
        phraseJob = viewModelScope.launch {
            var draft: PendingTake? = null
            var reviewed = false
            try {
                val inspected = withContext(Dispatchers.IO) {
                    val pending = repository.prepare(blockId, spec); draft = pending
                    capture.capture(spec, pending.file, true,
                        onCount = { beat -> viewModelScope.launch { if (generation == audioGeneration) countInBeat = beat } },
                        onFrames = { frames -> viewModelScope.launch { if (generation == audioGeneration) elapsedMs = frames * 1000L / PhraseSpec.RATE } },
                        hasSpaceAndPermission = { repository.availableBytes() >= 512 * 1024 && hasMicrophonePermission() })
                    currentCoroutineContext().ensureActive()
                    repository.inspect(pending)
                }
                if (generation == audioGeneration && recordingScreenOpen && visibleBlockId == blockId) {
                    reviewPending = draft; reviewTake = inspected; reviewed = true
                    message = null
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) {
                android.util.Log.e("PhraseCapture", "Phrase capture or commit failed", failure)
                if (generation == audioGeneration) message = "Phrase not saved. Check microphone, audio output and storage, then retry. Earlier takes were kept."
            } finally {
                withContext(NonCancellable + Dispatchers.IO) { if (!reviewed) draft?.let(repository::abandon) }
                capture.cancel(); phraseCapture = null; phraseJob = null
                if (captureOwner === this@RecordingViewModel) captureOwner = null
                phrasing = false; recording = false; busy = false; countInBeat = 0; phraseClickEnabled = false
                runCatching { manager.abandonAudioFocusRequest(focus) }
            }
            // Audition only this successful completion, after capture/focus cleanup.
            // Background/cancellation invalidates the generation; UI recomposition never starts it.
            if (reviewed && generation == audioGeneration && foreground && recordingScreenOpen && visibleBlockId == blockId) {
                previewReview()
            }
        }
    }

    private fun clearReview() {
        stopPlayback()
        reviewPending?.let(repository::abandon)
        reviewPending = null; reviewTake = null
    }
    fun discardReview() {
        if (busy || recording || reviewPending == null) return
        clearReview(); message = "Temporary take discarded. Saved recordings were kept."
    }
    fun rerecordReview() {
        if (busy || recording || !foreground || !recordingScreenOpen) return
        val draft = reviewPending ?: return
        if (!hasMicrophonePermission()) { permissionResult(false); return }
        clearReview()
        if (draft.phrase != null && draft.blockId != null) startPhrase(draft.blockId, draft.phrase)
        else startCapture(draft.blockId, referenceCapture, alongsideReference = captureDevices != null)
    }
    internal fun keepReference(expectedAudioId: String?, confirmedAudioId: String? = null, reviewedOrder: List<String>? = null) {
        val draft = reviewPending ?: return
        if (!referenceCapture || !ready || busy || recording) return
        stopPlayback()
        busy = true
        message = null
        referenceReplacement = null
        viewModelScope.launch {
            try {
                withContext(NonCancellable) {
                    val name = "Recorded reference " + java.text.SimpleDateFormat("yyyy-MM-dd HH-mm-ss", java.util.Locale.ROOT)
                        .format(java.util.Date(draft.createdAt)) + ".m4a"
                    val asset = referenceLibrary.importRecordedReference(name, draft.file) {
                        draft.blockId?.let { block -> referenceLinks.attachRecordedAudio(block, it.id, expectedAudioId, confirmedAudioId, reviewedOrder) }
                    }
                    clearReview()
                    keptReferenceId = asset.id
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (loss: ConfirmAudioLoss) { referenceReplacement = loss.mediaId }
            catch (failure: Exception) {
                message = "Reference could not be kept. ${failure.message ?: "Check storage and retry."}"
            } finally { busy = false }
        }
    }

    internal fun enterReference(blockId: String?) {
        if (!recordingScreenOpen) message = null
        enterBlock(blockId)
    }

    internal fun dismissReferenceReplacement() { referenceReplacement = null }
    internal fun referenceHandled() { keptReferenceId = null }
    internal fun leaveReference() {
        leaveBlock()
        clearReview()
        dismissReferenceReplacement()
    }

    fun saveReview() {
        if (!ready || busy || recording || referenceCapture) return
        val draft = reviewPending ?: return
        stopPlayback(); busy = true; message = null
        viewModelScope.launch {
            try {
                withContext(NonCancellable + Dispatchers.IO) { repository.save(draft) }
                reviewPending = null; reviewTake = null; message = "Take saved."
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                message = "Take could not be saved. Check storage and retry Save, or discard the temporary take. Saved recordings were kept."
            } finally { busy = false }
        }
    }
    fun previewReview() { reviewTake?.let(::play) }

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
        if (reviewTake != null && reviewTake?.id != take.id) return@work
        stopPlayback()
        val generation = audioGeneration
        val file = withContext(Dispatchers.IO) { repository.audioFile(take) }
        val samples = if (take.kind == TakeKind.RECORDED_LOOP) withContext(Dispatchers.IO) {
            val spec = PhraseSpec(checkNotNull(take.recordedBpm), checkNotNull(take.beatCount))
            PhraseWave.read(file).also { check(it.size == spec.frames) }
        } else null
        if (!foreground || !recordingScreenOpen || visibleBlockId != take.blockId || generation != audioGeneration) return@work
        if (manager.requestAudioFocus(focus) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            message = "Audio output is in use. Try playback again."; return@work
        }
        try {
            if (samples != null) {
                loopTrack = phraseTrack(samples, true)
                loopTrack!!.play(); playingId = take.id; message = "Looping · ${take.recordedBpm} BPM · ${take.beatCount} beats."
                return@work
            }
            val audio = MediaPlayer()
            player = audio; preparingId = take.id; message = null
            audio.setAudioAttributes(attributes); audio.setDataSource(file.absolutePath)
            audio.isLooping = take.kind == TakeKind.IMPORTED_LOOP
            audio.setOnPreparedListener {
                if (player === it && foreground && recordingScreenOpen && visibleBlockId == take.blockId) {
                    try { it.start(); preparingId = null; playingId = take.id; if (take.kind == TakeKind.IMPORTED_LOOP) message = "Looping imported audio · " + takeDuration(take) }
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
        audioGeneration++
        val loop = loopTrack; loopTrack = null
        runCatching { loop?.pause(); loop?.flush(); loop?.release() }
        val audio = player; player = null; playingId = null; preparingId = null
        runCatching { audio?.release() }; runCatching { manager.abandonAudioFocusRequest(focus) }
    }
    internal fun importedRepeatActive(): Boolean = player?.let { it.isLooping && it.isPlaying } == true
    internal fun importedPlaybackPosition(): Int = runCatching { player?.currentPosition ?: 0 }.getOrDefault(0)
    internal fun loopFramesPlayed(): Long = loopTrack?.let { it.playbackHeadPosition.toLong() and 0xffffffffL } ?: 0L
    internal fun phraseGuideFramesPlayed(): Long = phraseCapture?.outputFramesPlayed() ?: 0L
    fun rename(take: BlockTake, label: String) = work {
        withContext(Dispatchers.IO) { repository.rename(take.id, label) }; message = "Take renamed."
    }
    fun delete(take: BlockTake) = work {
        stopPlayback()
        val cleaned = withContext(Dispatchers.IO) { repository.delete(take.id) }
        message = if (cleaned) "Take deleted." else "Take deleted. File cleanup will retry after restart."
    }
    fun expiredExport() { message = "Export selection is no longer available. The source was kept; choose the take and export again." }
    internal val exportStorage = ExportStorage(app)
    internal var failedOrganizedExport by mutableStateOf<String?>(null)


    fun export(take: BlockTake, uri: Uri) = exportAudio(take) { source, copy ->
        exportStorage.saveAs(uri, exportFileName(take), source, copy)
    }

    internal fun exportOrganized(take: BlockTake, destination: ExportStorage.Destination, blockName: String) =
        exportAudio(take, organized = true) { source, copy -> exportStorage.export(destination, take, blockName, source, copy) }

    private fun exportAudio(take: BlockTake, organized: Boolean = false,
        send: suspend (java.io.File, suspend (java.io.OutputStream) -> Long) -> String) {
        if (!ready || busy || recording) {
            message = "Export could not start while recordings are busy or loading. The source was kept."
            return
        }
        work {
            stopPlayback()
            message = "Exporting audio…"
            try {
                val path = withContext(Dispatchers.IO) {
                    repository.withSavedAudio(take) { source ->
                        send(source) { output ->
                            val context = currentCoroutineContext()
                            repository.export(take, output, context::ensureActive)
                        }
                    }
                }
                message = "Audio exported. Saved to $path. The original is still in your recordings."
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                val detail = if (failure is IllegalStateException) failure.message.orEmpty() else "Check available storage and provider access."
                message = "Export failed. $detail The original was kept. Choose a new file with Save As or retry."
                if (organized) failedOrganizedExport = take.id
            }
        }
    }
    override fun onCleared() {
        audioGeneration++; phraseCapture?.cancel(); phraseJob?.cancel()
        runCatching { recorder?.release() }; recorder = null // Pending capture is recovered on next cold start.
        pending?.let(repository::abandon); pending = null
        clearImport(); clearReview()
        if (captureOwner === this) captureOwner = null
        stopPlayback(); runCatching { app.unregisterReceiver(noisy) }; super.onCleared()
    }
}
