package com.mrayover.practicelogger

/** Shared process-wide lease. Call from the main thread alongside clock transitions. */
internal object PracticeTimingOwner {
    private var owner: Any? = null

    fun acquire(clock: Any): Boolean {
        if (owner != null && owner !== clock) return false
        owner = clock
        return true
    }

    fun release(clock: Any) {
        if (owner === clock) owner = null
    }
}
