package com.mrayover.practicelogger

import android.app.Application
import android.os.SystemClock
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

class TranscribeViewModel(application: Application) : AndroidViewModel(application) {
    private val db = BlockDatabase.appInstance(application)
    private val dao = db.transcribeActivities()
    val practiceTime = dao.observePracticeTime().map<List<PracticeTimeEntry>, List<PracticeTimeEntry>?> { it }
        .catch { emit(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    private val writes = Mutex()
    private var clock = ToolClock(now = SystemClock::elapsedRealtime)
    private var committedMs = 0L
    private var draft by mutableStateOf<TranscribeActivity?>(null)
    private var selectedGoalId by mutableStateOf<String?>(null)
    var audioId by mutableStateOf<String?>(null); private set
    private var completionTime by mutableStateOf<Long?>(null)
    private var visible = false
    private var foreground = false
    private var contextVersion = 0

    val goalId get() = if (hasActivity) draft?.goalId else selectedGoalId
    val hasActivity get() = draft != null
    val finishing get() = completionTime != null
    var ready by mutableStateOf(false); private set
    var busy by mutableStateOf(false); private set
    var running by mutableStateOf(false); private set
    var elapsedMs by mutableLongStateOf(0); private set
    var message by mutableStateOf<String?>(null); private set

    init {
        reload()
        viewModelScope.launch {
            while (isActive) {
                delay(5_000)
                if (running) persist { if (running) checkpoint() }
            }
        }
        viewModelScope.launch {
            while (isActive) {
                delay(200)
                if (running) elapsedMs = clock.elapsed
            }
        }
    }

    fun reload() = command {
        if (ready) return@command
        draft = dao.unfinished()
        committedMs = draft?.activeMs ?: 0
        clock = ToolClock(committedMs, SystemClock::elapsedRealtime)
        elapsedMs = committedMs
        audioId = draft?.audioId
        ready = true
        message = if (hasActivity) "Activity paused. Resume or Stop & Save." else null
    }

    fun setForeground(value: Boolean) {
        foreground = value
        if (!value) interrupt()
    }

    fun setVisible(value: Boolean) {
        visible = value
        if (!value) interrupt()
    }

    private fun interrupt() {
        contextVersion++
        if (!running) return
        freeze()
        message = "Activity paused. Resume or Stop & Save."
        persist { if (hasActivity) checkpoint() }
    }

    fun selectGoal(id: String?) = command {
        if (finishing) return@command
        val current = draft
        if (current == null) selectedGoalId = id
        else {
            draft = current.copy(goalId = id)
            checkpoint()
        }
    }

    fun selectAudio(id: String) {
        audioId = id
        persist {
            val current = draft ?: return@persist
            if (finishing) return@persist
            draft = current.copy(audioId = id)
            checkpoint()
        }
    }

    fun start() {
        val version = contextVersion
        command {
            if (!ready || running || finishing || !canRun(version)) return@command
            if (!PracticeTimingOwner.acquire(this)) {
                message = "Pause Block practice before starting the Transcribe timer."
                return@command
            }
            try {
                if (draft == null) {
                    val newActivity = TranscribeActivity(UUID.randomUUID().toString(),
                        System.currentTimeMillis(), goalId = selectedGoalId, audioId = audioId)
                    draft = db.withTransaction {
                        dao.unfinished() ?: newActivity.copy(
                            audioId = newActivity.audioId?.takeIf { db.media().find(it)?.category == "AUDIO" }
                        ).also { dao.insert(it) }
                    }
                    selectedGoalId = null
                    committedMs = checkNotNull(draft).activeMs
                    clock = ToolClock(committedMs, SystemClock::elapsedRealtime)
                }
                // Retry a failed pause/metadata checkpoint before counting again.
                checkpoint()
                if (canRun(version)) {
                    clock.start()
                    running = true
                    message = null
                } else message = "Activity paused. Resume or Stop & Save."
            } finally {
                if (!running) PracticeTimingOwner.release(this)
            }
        }
    }

    fun stop() {
        if (busy || draft == null) return
        freeze()
        if (completionTime == null) completionTime = System.currentTimeMillis()
        command {
            checkpoint(completionTime)
            draft = null
            completionTime = null
            committedMs = 0
            clock.reset()
            elapsedMs = 0
            selectedGoalId = null
            message = "Activity saved."
        }
    }

    private fun canRun(version: Int) = visible && foreground && version == contextVersion

    private fun freeze() {
        clock.pause()
        elapsedMs = clock.elapsed
        running = false
        PracticeTimingOwner.release(this)
    }

    private suspend fun checkpoint(completedAt: Long? = null) {
        val current = checkNotNull(draft)
        val elapsed = clock.elapsed.coerceAtLeast(committedMs)
        val changed = dao.save(current.id, elapsed, current.goalId, current.audioId, completedAt)
        check(changed == 1 || (completedAt != null && dao.find(current.id)?.completedAt == completedAt))
        committedMs = elapsed
    }

    private fun command(action: suspend () -> Unit) {
        if (busy) return
        busy = true
        persist {
            try { action() }
            finally { busy = false }
        }
    }

    private fun persist(action: suspend () -> Unit) {
        viewModelScope.launch {
            writes.withLock {
                try { action() }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) {
                    freeze()
                    message = when {
                        finishing -> "Could not save this activity. Check storage and tap Stop & Save to retry."
                        hasActivity -> "Could not save the timer checkpoint. Activity paused. Check storage and retry."
                        else -> "Could not open the practice timer. Check storage and retry."
                    }
                }
            }
        }
    }

    override fun onCleared() {
        PracticeTimingOwner.release(this)
        super.onCleared()
    }
}
