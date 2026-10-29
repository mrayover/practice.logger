package com.mrayover.practicelogger

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import java.time.LocalDate

class LogViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = BlockDatabase.appInstance(app).logs()
    private val writer = LogWriter { dao.save(it) }
    val status = writer.status
    var date by mutableStateOf(LocalDate.now()); private set
    var text by mutableStateOf(""); private set
    var ready by mutableStateOf(false); private set
    var busy by mutableStateOf(false); private set
    var weekly by mutableStateOf(false); private set
    var week by mutableStateOf(logWeek(date)); private set
    var rows by mutableStateOf<List<DailyLog>>(emptyList()); private set
    var error by mutableStateOf<String?>(null); private set
    private var openedDay: LocalDate? = null
    private fun operation(action: suspend () -> Unit) {
        if (busy) return
        busy = true; error = null
        viewModelScope.launch {
            try { writer.flush(); action() }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { error = "Log could not be saved or loaded. Your current text is retained. Retry." }
            finally { busy = false }
        }
    }
    private suspend fun load(day: LocalDate) {
        val saved = dao.find(day.toString())
        date = day; text = saved?.text.orEmpty(); ready = true; weekly = false
    }
    fun open(today: LocalDate = LocalDate.now()) = operation { load(today); openedDay = today }
    fun resume(today: LocalDate = LocalDate.now()) {
        if (openedDay != today) open(today)
    }
    fun select(day: LocalDate) = operation { load(day) }
    fun edit(value: String) {
        if (!ready || busy || weekly) return
        text = value; writer.edit(date.toString(), value)
    }
    fun showWeek(first: LocalDate = logWeek(date)) = operation {
        val entries = dao.range(first.toString(), first.plusDays(6).toString())
        week = first; rows = entries; weekly = true
    }
    fun leave(onHome: () -> Unit) = operation { onHome() }
    fun retry() = operation { }
    // Android pauses the Activity before it becomes eligible for ordinary process reclamation.
    // Drain IO writes before returning from pause; no debounce timer or main-thread DB access.
    fun background() {
        try { runBlocking { writer.flush() } }
        catch (_: Exception) { error = "Log could not be saved. Your text is retained here; retry." }
    }
    override fun onCleared() { writer.close() }
}
