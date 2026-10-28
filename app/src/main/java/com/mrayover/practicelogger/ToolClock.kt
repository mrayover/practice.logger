package com.mrayover.practicelogger

/** Foreground stopwatch: only intervals explicitly started are counted. */
internal class ToolClock(private val now: () -> Long) {
    private var committed = 0L
    private var anchor = 0L
    var running = false
        private set
    val elapsed: Long get() = committed + if (running) (now() - anchor).coerceAtLeast(0) else 0L
    fun start() { if (!running) { anchor = now(); running = true } }
    fun pause() { if (running) { committed = elapsed; running = false } }
    fun reset() { running = false; committed = 0L }
}
