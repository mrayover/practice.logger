package com.mrayover.practicelogger

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray

data class RoutineCatalog(val bundles: List<RoutineBundle> = emptyList(), val ready: Boolean = false, val error: String? = null)

class RoutineViewModel(app: Application, private val state: SavedStateHandle) : AndroidViewModel(app) {
    private val repository = RoutineRepository(BlockDatabase.appInstance(app))
    val catalog = repository.routines.map { RoutineCatalog(it, ready = true) }
        .catch { emit(RoutineCatalog(error = "Could not load Routines. Close and reopen to retry.")) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, RoutineCatalog())
    var busy by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    var message by mutableStateOf<String?>(null); private set
    var editorSaved by mutableStateOf(false); private set
    var draftSaveResult by mutableStateOf<String?>(null); private set
    var editorId: String? = state["routineEditorId"]; private set
    var name by mutableStateOf(state.get<String>("routineName") ?: ""); private set
    var entries by mutableStateOf(decode(state["routineEntries"])); private set
    private var originalName = state.get<String>("routineOriginalName") ?: ""
    private var originalEntries = decode(state["routineOriginalEntries"])
    val dirty get() = name != originalName || entries != originalEntries

    private fun encode(rows: List<RoutineSelection>): String = JSONArray().apply {
        rows.forEach { put(JSONArray().put(it.blockId).put(it.configuredMs)) }
    }.toString()
    private fun decode(value: String?): List<RoutineSelection> = runCatching {
        val rows = JSONArray(value ?: "[]")
        (0 until rows.length()).map { rows.getJSONArray(it).let { row -> RoutineSelection(row.getString(0), row.getLong(1)) } }
    }.getOrDefault(emptyList())
    private fun persist() {
        state["routineEditorId"] = editorId; state["routineName"] = name; state["routineEntries"] = encode(entries)
        state["routineOriginalName"] = originalName; state["routineOriginalEntries"] = encode(originalEntries)
    }
    fun beginEditor(id: String?): Boolean {
        if (busy) return false
        val bundle = id?.let { key -> catalog.value.bundles.find { it.routine.id == key } }
        if (id != null && bundle == null) { error = "This Routine is unavailable. Reopen the Routines list."; return false }
        editorId = id; name = bundle?.routine?.name ?: ""
        entries = bundle?.ordered?.map { RoutineSelection(it.sourceBlockId, it.configuredMs) } ?: emptyList()
        originalName = name; originalEntries = entries; error = null; message = null; persist()
        return true
    }
    fun rename(value: String) { if (!busy) { name = value; persist() } }
    fun add(block: Block) { if (!busy && !block.archived) { entries = entries + RoutineSelection(block.id, block.durationMinutes.toLong() * 60_000L); persist() } }
    fun remove(index: Int) { if (!busy && index in entries.indices) { entries = entries.filterIndexed { i, _ -> i != index }; persist() } }
    fun move(index: Int, direction: Int) {
        if (busy || direction !in listOf(-1, 1) || index !in entries.indices || index + direction !in entries.indices) return
        entries = entries.toMutableList().apply { val other = this[index + direction]; this[index + direction] = this[index]; this[index] = other }
        persist()
    }
    fun duration(index: Int, minutes: String) {
        val value = minutes.trim().toIntOrNull()
        if (!busy && index in entries.indices && value != null && value > 0) {
            entries = entries.mapIndexed { i, row -> if (i == index) row.copy(configuredMs = value.toLong() * 60_000L) else row }; persist()
        }
    }
    private fun work(action: suspend () -> Unit) {
        if (busy) return
        busy = true; error = null; message = null
        viewModelScope.launch {
            try { action() }
            catch (e: CancellationException) { throw e }
            catch (e: RoutineUnavailableException) { error = e.message }
            catch (e: IllegalArgumentException) { error = e.message ?: "Check the Routine name and Block durations." }
            catch (_: Exception) { error = "Routine could not be saved. Existing Routines and practice data were kept. Check storage and retry." }
            finally { busy = false }
        }
    }
    fun saveEditor() {
        val id = editorId; val title = name; val rows = entries.toList()
        work { repository.save(id, title, rows); originalName = title; originalEntries = rows; persist(); editorSaved = true; message = "Routine saved." }
    }
    fun saveDraft(title: String) = work { draftSaveResult = repository.saveDraft(title); message = "Routine saved. Your session draft is unchanged." }
    fun delete(id: String) = work { repository.delete(id); message = "Routine deleted. Blocks and practice history were kept." }
    fun dismissError() { error = null }
    fun navigationHandled() { editorSaved = false }
    fun draftResultHandled() { draftSaveResult = null }
}
