package com.mrayover.practicelogger

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.*
import kotlin.math.*

fun audioTimestamp(ms: Long): String = java.lang.String.format(java.util.Locale.ROOT, "%d:%02d.%03d", ms / 60000, ms / 1000 % 60, ms % 1000)
fun audioSeconds(ms: Long): String = java.lang.String.format(java.util.Locale.ROOT, "%.3f", ms / 1000.0)
fun parseAudioSeconds(text: String): Long? = text.toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0 && it <= 900 }?.let { (it * 1000).roundToLong() }

/** Reusable presentation: viewport + source timestamps, no Library or practice ownership. */
@Composable fun AudioWaveformView(waveform: AudioWaveform?, state: LibraryAudioState, markers: List<MasterAudioMarker>,
    timeline: AudioTimeline, onTimeline: (AudioTimeline) -> Unit, seek: (Long) -> Unit, range: (Long, Long) -> Unit,
    modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outline
    val tertiary = MaterialTheme.colorScheme.tertiary
    val cursor = MaterialTheme.colorScheme.onSurface
    val latestTimeline by rememberUpdatedState(timeline)
    val latestState by rememberUpdatedState(state)
    val latestRange by rememberUpdatedState(range)
    val latestSeek by rememberUpdatedState(seek)
    var viewportWidth by remember { mutableFloatStateOf(1f) }
    val transform = rememberTransformableState { zoom, pan, _ ->
        onTimeline(latestTimeline.transform(zoom, pan.x / viewportWidth))
    }
    BoxWithConstraints(modifier.fillMaxWidth().height(112.dp).onSizeChanged { viewportWidth = it.width.toFloat().coerceAtLeast(1f) }.testTag("audio-waveform")) {
        val width = constraints.maxWidth.toFloat()
        val density = LocalDensity.current
        Canvas(Modifier.fillMaxSize().transformable(transform)
            .pointerInput(Unit) { detectTapGestures { latestSeek(latestTimeline.timeAt(it.x / size.width)) } }
            .semantics {
                contentDescription = "Audio waveform, ${audioTimestamp(timeline.timeAt(0f))} to ${audioTimestamp(timeline.timeAt(1f))}"
                customActions = listOf(CustomAccessibilityAction("Seek forward one second") { seek((state.positionMs + 1000).coerceAtMost(state.durationMs)); true },
                    CustomAccessibilityAction("Seek back one second") { seek((state.positionMs - 1000).coerceAtLeast(0)); true })
            }) {
            val a = timeline.fraction(state.aMs) * size.width
            val b = timeline.fraction(state.bMs) * size.width
            drawRect(primary.copy(alpha = .12f), Offset(a.coerceIn(0f, size.width), 0f), androidx.compose.ui.geometry.Size((b.coerceIn(0f, size.width) - a.coerceIn(0f, size.width)).coerceAtLeast(0f), size.height))
            drawLine(outline, Offset(0f, size.height / 2), Offset(size.width, size.height / 2))
            waveform?.let { wave ->
                for (x in 0 until size.width.toInt() step 2) {
                    val start = (timeline.timeAt(x / size.width) / AudioWaveform.BIN_MS).toInt().coerceIn(wave.peaks.indices)
                    val end = (timeline.timeAt((x + 2) / size.width) / AudioWaveform.BIN_MS).toInt().coerceIn(wave.peaks.indices)
                    var peak = 0f
                    for (i in start..end) peak = max(peak, wave.peaks[i])
                    val height = peak * size.height * .43f
                    drawLine(primary, Offset(x.toFloat(), size.height / 2 - height), Offset(x.toFloat(), size.height / 2 + height), 1.5f)
                }
            }
            markers.forEach { marker ->
                val x = timeline.fraction(marker.startMs) * size.width
                if (x in 0f..size.width) drawLine(tertiary, Offset(x, 0f), Offset(x, size.height), 2f)
                marker.endMs?.let { end ->
                    val endX = timeline.fraction(end) * size.width
                    if (endX in 0f..size.width) drawLine(tertiary, Offset(endX, 0f), Offset(endX, size.height), 1f)
                }
            }
            for (x in listOf(a, b)) if (x in 0f..size.width) drawLine(primary, Offset(x, 0f), Offset(x, size.height), 3f)
            val x = timeline.fraction(state.positionMs) * size.width
            if (x in 0f..size.width) drawLine(cursor, Offset(x, 0f), Offset(x, size.height), 2.5f)
        }
        // Separate vertical lanes keep adjacent A/B handles independently reachable.
        for (isA in listOf(true, false)) {
            val time = if (isA) state.aMs else state.bMs
            val fraction = timeline.fraction(time)
            if (fraction in 0f..1f) {
                val handlePx = with(density) { 48.dp.toPx() }
                Box(Modifier.offset { IntOffset((fraction * width - handlePx / 2).coerceIn(0f, (width - handlePx).coerceAtLeast(0f)).roundToInt(), if (isA) 0 else with(density) { 64.dp.roundToPx() }) }
                    .size(48.dp).testTag(if (isA) "audio-handle-a" else "audio-handle-b")
                    .pointerInput(isA, width) {
                        var timeAtStart = 0L; var distance = 0f
                        detectDragGestures(onDragStart = { timeAtStart = if (isA) latestState.aMs else latestState.bMs; distance = 0f }) { change, drag ->
                            change.consume(); distance += drag.x
                            val next = (timeAtStart + distance / width * latestTimeline.spanMs).roundToLong().coerceIn(0, latestState.durationMs)
                            if (isA) latestRange(next, latestState.bMs) else latestRange(latestState.aMs, next)
                        }
                    }.semantics { contentDescription = "${if (isA) "A start" else "B end"} ${audioTimestamp(time)}; use Edit range for precise adjustment" }) {
                    Surface(color = primary, contentColor = MaterialTheme.colorScheme.onPrimary, shape = MaterialTheme.shapes.small) {
                        Text(if (isA) "A" else "B", Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                }
            }
        }
    }
}
