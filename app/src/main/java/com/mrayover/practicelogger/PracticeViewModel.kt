package com.mrayover.practicelogger

import android.app.Application
import android.os.SystemClock
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class SessionCatalog(val bundles: List<SessionBundle> = emptyList(), val ready: Boolean = false, val error: String? = null, val selectedId: String? = null) {
    val draft get() = bundles.find { it.session.status == "DRAFT" }
    val unfinished get() = bundles.filter { it.session.status == "IN_PROGRESS" }
    val active get() = unfinished.find { it.session.id == selectedId } ?: unfinished.firstOrNull()
    val history get() = bundles.filter { it.session.status == "COMPLETED" }
}

class PracticeViewModel(application: Application, private val savedState: SavedStateHandle) : AndroidViewModel(application) {
    private val db = BlockDatabase.appInstance(application)
    private val repository = SessionRepository(db)
    val catalog = combine(repository.sessions, savedState.getStateFlow<String?>("selectedSession", null)) { bundles, selected ->
        SessionCatalog(bundles, ready = true, selectedId = selected)
    }
        .catch { emit(SessionCatalog(error = "Could not open practice sessions. Close and reopen the app to retry.")) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, SessionCatalog())
    var running by mutableStateOf(false)
        private set
    var busy by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var destination by mutableStateOf<String?>(null)
        private set
    var completedId by mutableStateOf<String?>(null)
        private set
    var noteSaved by mutableStateOf(false)
        private set
    var blockNoteSavedFor by mutableStateOf<String?>(null)
        private set
    private var foreground = false
    private var focusedPlacementId: String? = null
    var clicking by mutableStateOf(false)
        private set
    var workingBpm by mutableIntStateOf(120)
        private set
    var clickError by mutableStateOf<String?>(null)
        private set
    var metronomeConfig by mutableStateOf(MetronomeConfig()); private set
    var metronomePulse by mutableStateOf(MetronomePulse()); private set
    private val metronome = MetronomeAudio(application, { metronomePulse = it }) { message -> clicking = false; clickError = message }
    fun configureMetronome(value: MetronomeConfig) { metronomeConfig = value; metronome.configure(value) }
    internal fun metronomeFramesWritten() = metronome.sampleFramesWritten
    internal fun metronomeNonZeroFramesWritten() = metronome.nonZeroClickFramesWritten

    fun enterFocusedPlacement(placement: Placement) {
        if (focusedPlacementId != placement.id) {
            stopClick(); focusedPlacementId = placement.id; workingBpm = initialWorkingBpm(placement)
        }
    }
    fun leaveFocusedPlacement() { stopClick(); focusedPlacementId = null }
    fun stopClick() { metronome.stop(); clicking = false }
    fun changeWorkingBpm(bpm: Int) {
        require(bpm in 30..300)
        workingBpm = bpm
        metronome.changeTempo(bpm)
        if (clicking) persistUsedBpm(bpm)
    }
    private fun persistUsedBpm(bpm: Int) {
        val bundle = catalog.value.active ?: return
        val placementId = focusedPlacementId ?: return
        viewModelScope.launch {
            mutex.withLock {
                try { repository.saveUsedBpm(bundle.session.id, placementId, bpm) }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { stopClick(); clickError = "Click stopped because its BPM could not be saved. Check storage and retry." }
            }
        }
    }
    fun startClick() {
        if (!foreground || busy || focusedPlacementId == null || focusedPlacementId != catalog.value.active?.current?.id) return
        clickError = null
        if (metronome.start(workingBpm)) { clicking = true; persistUsedBpm(workingBpm) }
    }
    private var completionArmedFor: String? = null
    internal var completionSignals by mutableIntStateOf(0); private set
    internal fun completionFramesWritten() = metronome.completionFramesWritten
    private var anchor: Long? = null
    private var timingSessionId: String? = null
    private val mutex = Mutex()

    init {
        viewModelScope.launch {
            while (isActive) {
                delay(250)
                if (running) mutex.withLock {
                    if (running) try { flush(SystemClock.elapsedRealtime()) }
                    catch (cancelled: CancellationException) { throw cancelled }
                    catch (_: Exception) { running = false; anchor = null; error = "Timer paused because practice time could not be saved. Check available storage and retry." }
                }
            }
        }
    }
    fun navigationHandled() { destination = null }
    fun dismissError() { error = null }
    fun noteChanged() { noteSaved = false }
    private suspend fun flush(until: Long) {
        val start = anchor ?: return
        val id = timingSessionId ?: return
        val updated = repository.checkpoint(id, maxOf(0L, until - start))
        anchor = until
        if (!updated.overtimeAccepted && updated.activeMs >= updated.configuredMs) {
            val alert = running && foreground && completionArmedFor == updated.id
            completionArmedFor = null
            running = false; anchor = null
            if (alert) {
                completionSignals++
                // A committed running crossing is the only source of this one-shot cue.
                // Audio failure must never undo the checkpoint or prevent the visual zero state.
                runCatching { metronome.playCompletion() }
            }
        }
    }
    private fun operation(action: suspend () -> Unit) {
        if (busy) return
        busy = true
        viewModelScope.launch {
            try { mutex.withLock { action() } }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (e: RoutineUnavailableException) { running = false; anchor = null; error = e.message }
            catch (_: Exception) { running = false; anchor = null; error = "Changes could not be saved. Your stored session is retained. Close and reopen if the problem continues." }
            finally { busy = false }
        }
    }
    fun setForeground(value: Boolean) {
        foreground = value
        if (!value) { stopClick(); pause() }
    }
    fun pause() {
        val until = SystemClock.elapsedRealtime()
        running = false
        viewModelScope.launch {
            mutex.withLock {
                try { flush(until); anchor = null }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { anchor = null; error = "Could not save the last timer checkpoint. Previously saved practice time is retained." }
            }
        }
    }
    fun resume(overtime: Boolean = false) = operation {
        check(foreground)
        val id = checkNotNull(catalog.value.active).session.id
        val current = checkNotNull(db.sessions().find(id)?.current)
        if (overtime) repository.continueOvertime(id)
        else check(current.overtimeAccepted || current.activeMs < current.configuredMs)
        completionArmedFor = current.id.takeIf { !overtime && !current.overtimeAccepted && current.activeMs < current.configuredMs }
        check(foreground)
        timingSessionId = id; anchor = SystemClock.elapsedRealtime(); running = true
    }
    fun addBlock(id: String) = operation { repository.addBlock(id) }
    fun move(id: String, direction: Int) = operation { repository.move(id, direction) }
    fun remove(id: String) = operation { repository.remove(id) }
    fun duration(id: String, minutes: String) = operation {
        val value = minutes.trim().toIntOrNull(); require(value != null && value > 0)
        repository.setDuration(id, value.toLong() * 60_000L)
    }
    fun discardDraft() = operation { repository.discardDraft(); destination = "home" }
    private suspend fun selectSession(id: String) {
        check(db.sessions().find(id)?.session?.status == "IN_PROGRESS")
        savedState["selectedSession"] = id
        catalog.first { it.active?.session?.id == id }
        destination = "focus"
    }
    fun openSession(id: String) {
        if (busy) return
        leaveFocusedPlacement()
        val until = SystemClock.elapsedRealtime()
        running = false
        operation { flush(until); anchor = null; selectSession(id) }
    }
    fun start() = operation { selectSession(repository.start()) }
    fun startRoutine(id: String) {
        if (busy) return
        leaveFocusedPlacement()
        val until = SystemClock.elapsedRealtime()
        running = false
        operation { flush(until); anchor = null; selectSession(repository.startRoutine(id)) }
    }
    fun startGoal(id: String) {
        if (busy) return
        // Goal detail is outside Focused Practice; never interrupt a running session.
        if (running || clicking) { error = "Save your current practice for later before starting a Goal session."; return }
        operation { selectSession(repository.startGoal(id)) }
    }
    fun finish(outcome: String, note: String, preserveNote: Boolean = false) {
        if (busy) return
        stopClick()
        val until = SystemClock.elapsedRealtime()
        running = false
        operation {
            flush(until); anchor = null
            val id = checkNotNull(catalog.value.active).session.id
            if (repository.finishBlock(id, outcome, note, preserveNote)) { completedId = id; destination = "summary" }
        }
    }
    fun saveBlockNote(sessionId: String, placementId: String, note: String) = operation {
        repository.saveBlockNote(sessionId, placementId, note)
        blockNoteSavedFor = placementId
    }
    fun saveForLater(sessionId: String, placementId: String, note: String, preserveNote: Boolean = false) {
        if (busy) return
        stopClick()
        val until = SystemClock.elapsedRealtime()
        running = false
        operation {
            flush(until); anchor = null
            if (!preserveNote) repository.saveBlockNote(sessionId, placementId, note)
            destination = "home"
        }
    }
    fun saveNote(id: String, note: String) = operation { repository.saveSessionNote(id, note); noteSaved = true }
    override fun onCleared() { metronome.stop(); super.onCleared() }
    // The shared database belongs to the application, not an individual screen.
}
