package com.mrayover.practicelogger

import android.app.Application
import android.os.SystemClock
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ToolsViewModel(app: Application) : AndroidViewModel(app) {
    var bpm by mutableIntStateOf(120); private set
    var clicking by mutableStateOf(false); private set
    var listening by mutableStateOf(false); private set
    internal var pitch by mutableStateOf<TuningPitch?>(null); private set
    var message by mutableStateOf<String?>(null); private set
    var stopwatchMs by mutableLongStateOf(0); private set
    var stopwatchRunning by mutableStateOf(false); private set
    var minutes by mutableIntStateOf(5); private set
    var remainingMs by mutableLongStateOf(300_000); private set
    var countdownRunning by mutableStateOf(false); private set
    private val stopwatch = ToolClock(SystemClock::elapsedRealtime)
    private val countdown = ToolClock(SystemClock::elapsedRealtime)
    private val click = MetronomeAudio(app) { error -> clicking = false; message = error }
    private val tuner = TunerAudio(app, { pitch = it }, { error -> listening = false; pitch = null; message = error })
    internal val tunerFramesRead get() = tuner.framesRead
    private var foreground = false
    private var screen: String? = null

    init {
        viewModelScope.launch {
            while (true) { delay(100); updateClocks() }
        }
    }
    fun enter(value: String) { stopAll(); screen = value; message = null }
    fun leave() { stopAll(); screen = null }
    fun setForeground(value: Boolean) { foreground = value; if (!value) stopAll() }
    fun changeBpm(delta: Int) { bpm = (bpm + delta).coerceIn(30, 300); click.changeTempo(bpm) }
    fun toggleClick() {
        message = null
        if (clicking) { click.stop(); clicking = false }
        else if (foreground && screen == "metronome") clicking = click.start(bpm)
    }
    fun toggleTuner() {
        message = null
        if (listening) { tuner.stop(); listening = false; pitch = null }
        else if (foreground && screen == "tuner") { pitch = null; listening = tuner.start() }
    }
    fun permissionDenied() { message = "Microphone access is needed for tuning. You can allow it in Android app settings." }
    fun toggleStopwatch() {
        if (stopwatch.running) stopwatch.pause() else if (foreground && screen == "timer") stopwatch.start()
        updateClocks()
    }
    fun resetStopwatch() { stopwatch.reset(); updateClocks() }
    fun selectMinutes(value: Int) {
        if (!countdown.running && value.coerceIn(1, 180) != minutes) { minutes = value.coerceIn(1, 180); countdown.reset(); updateClocks() }
    }
    fun toggleCountdown() {
        if (countdown.running) countdown.pause()
        else if (foreground && screen == "timer") {
            if (countdown.elapsed >= minutes * 60_000L) countdown.reset()
            countdown.start()
        }
        updateClocks()
    }
    fun resetCountdown() { countdown.reset(); updateClocks() }
    private fun updateClocks() {
        stopwatchMs = stopwatch.elapsed; stopwatchRunning = stopwatch.running
        remainingMs = (minutes * 60_000L - countdown.elapsed).coerceAtLeast(0)
        if (remainingMs == 0L) countdown.pause()
        countdownRunning = countdown.running
    }
    private fun stopAll() {
        click.stop(); clicking = false; tuner.stop(); listening = false; pitch = null
        stopwatch.pause(); countdown.pause(); updateClocks()
    }
    override fun onCleared() { stopAll() }
}
