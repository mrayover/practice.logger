package com.mrayover.practicelogger.composer

import kotlin.math.roundToLong

enum class RollGrid(val label: String, val ticks: Long) {
    QUARTER("1/4",960), EIGHTH("1/8",480), SIXTEENTH("1/16",240), THIRTY_SECOND("1/32",120),
    EIGHTH_TRIPLET("1/8T",320), SIXTEENTH_TRIPLET("1/16T",160), THIRTY_SECOND_TRIPLET("1/32T",80)
}
internal fun CompositionProject.barTicks() = numerator * 3840L / denominator
internal fun CompositionProject.patternTicks(pattern: CompositionPattern) = barTicks() * pattern.lengthBars
internal fun snapTick(value: Double, grid: RollGrid, snap: Boolean): Long =
    if (snap) (value / grid.ticks).roundToLong() * grid.ticks else value.roundToLong()
/** Clamp to a valid grid point, rather than clamping a snapped value off the grid at an edge. */
internal fun boundedTick(value: Double, minimum: Long, maximum: Long, grid: RollGrid, snap: Boolean, fallback: Long = minimum): Long {
    if (!snap) return value.roundToLong().coerceIn(minimum,maximum)
    val low = ((minimum + grid.ticks - 1) / grid.ticks) * grid.ticks
    val high = (maximum / grid.ticks) * grid.ticks
    return if (low <= high) snapTick(value,grid,true).coerceIn(low,high) else fallback.coerceIn(minimum,maximum)
}
internal fun CompositionProject.replacePattern(pattern: CompositionPattern) = copy(tracks = tracks.map { track ->
    track.copy(patterns = track.patterns.map { if (it.id == pattern.id) pattern else it })
})

/** Clipboard stores values, never source ownership; each paste receives fresh identities. */
internal fun pastedNotes(notes: List<CompositionNote>, destination: Long): List<CompositionNote> {
    val origin = notes.minOfOrNull { it.startTick } ?: return emptyList()
    return notes.map { it.copy(id = newCompositionId(), startTick = destination + it.startTick - origin) }
}
internal fun CompositionProject.withNotes(pattern: CompositionPattern, notes: List<CompositionNote>): CompositionProject {
    val end = notes.maxOfOrNull { it.startTick + it.durationTick } ?: 0L
    val bars = listOf(1,2,4,8,16).firstOrNull { it * barTicks() >= end && it >= pattern.lengthBars }
        ?: error("Notes would exceed the 16-bar pattern limit. Move the cursor earlier.")
    return replacePattern(pattern.copy(lengthBars = bars, notes = notes)).also(CompositionCodec::validate)
}
