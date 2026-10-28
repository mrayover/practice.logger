package com.mrayover.practicelogger

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class GoalViewModel(application: Application) : AndroidViewModel(application) {
    private val db = BlockDatabase.appInstance(application)
    private val repository = GoalRepository(db)
    var ready by mutableStateOf(false); private set
    val goals = repository.goals.onEach { ready = true }.catch { error = "Goals could not be loaded. Close and reopen to retry." }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val times = db.goals().observeTime().catch { error = "Goal practice time could not be loaded." }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val links = db.goals().observeSessions().catch { error = "Goal session links could not be loaded." }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    var busy by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    var savedId by mutableStateOf<String?>(null); private set
    fun dismissError() { error = null }
    fun navigationHandled() { savedId = null }
    private fun operation(action: suspend () -> Unit) {
        if (busy) return
        busy = true
        viewModelScope.launch {
            try { action() }
            catch (e: CancellationException) { throw e }
            catch (e: IllegalArgumentException) { error = e.message }
            catch (_: Exception) { error = "Changes could not be saved. Your stored Goals are retained. Close and reopen to retry." }
            finally { busy = false }
        }
    }
    fun save(id: String?, title: String, description: String, progress: String, notes: String) = operation {
        val value = progress.trim().toIntOrNull()
        require(value != null && value in 0..100) { "Enter progress from 0 to 100%." }
        val saved = repository.save(id, title, description, value, notes)
        goals.first { rows -> rows.any { it.goal.id == saved && it.goal.title == title.trim() && it.goal.progress == value && it.goal.notes == notes.trim() && it.goal.description == description.trim() } }
        savedId = saved
    }
    fun progress(id: String, progress: String) = operation {
        val value = progress.trim().toIntOrNull()
        require(value != null && value in 0..100) { "Enter progress from 0 to 100%." }
        val g = checkNotNull(db.goals().find(id)).goal
        repository.save(id, g.title, g.description, value, g.notes)
    }
    fun complete(id: String, completed: Boolean) = operation { repository.setCompleted(id, completed) }
    fun move(id: String, direction: Int) = operation { repository.move(id, direction) }
    fun blocks(id: String, blockIds: List<String>) = operation { repository.setBlocks(id, blockIds) }
}
