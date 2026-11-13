package com.mrayover.practicelogger

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import kotlin.math.*

/** Small process-only LRU. No media derivative or persistent playback checkpoint. */
internal object RecordingWaveforms {
    private val mutex = Mutex()
    private val cache = linkedMapOf<String, AudioWaveform>()
    suspend fun load(file: File, duration: Long): AudioWaveform = withContext(Dispatchers.IO) {
        mutex.withLock {
            require(duration in 1..Int.MAX_VALUE.toLong() && file.isFile && file.length() > 0)
            val key = "${file.absolutePath}:${file.length()}:${file.lastModified()}:$duration"
            cache.remove(key)?.let { cache[key] = it; return@withLock it }
            val binMs = maxOf(AudioWaveform.BIN_MS, (duration + AudioWaveform.MAX_BINS - 2) / (AudioWaveform.MAX_BINS - 1))
            val peaks = FloatArray((duration / binMs + 1).toInt())
            val peak: (Long, Float) -> Unit = { time, amplitude ->
                val index = (time / (binMs * 1000)).toInt()
                if (index in peaks.indices && amplitude.isFinite()) peaks[index] = max(peaks[index], abs(amplitude).coerceAtMost(1f))
            }
            if (file.extension.equals("wav", true)) wavePeaks(file, peak) else decodedPeaks(file, duration, peak)
            currentCoroutineContext().ensureActive()
            AudioWaveform(duration, peaks, binMs).also {
                cache[key] = it
                while (cache.size > 4) cache.remove(cache.keys.first())
            }
        }
    }
}

internal fun recordingFrame(ms: Long, frames: Int): Int =
    (ms.coerceAtLeast(0).coerceAtMost(900000) * PhraseSpec.RATE / 1000).coerceIn(0, (frames - 1).coerceAtLeast(0).toLong()).toInt()

@Composable internal fun RecordingScrubber(take: BlockTake?, slot: String, vm: RecordingViewModel, enabled: Boolean,
    reviewing: Boolean = false, bindSelection: Boolean = true) {
    if (!reviewing && bindSelection) DisposableEffect(slot, take?.id) {
        vm.bindScrubber(slot, take?.id)
        onDispose { vm.unbindScrubber(slot, take?.id) }
    }
    if (take == null) return
    var wave by remember(take.id, take.fileName) { mutableStateOf<AudioWaveform?>(null) }
    var failed by remember(take.id) { mutableStateOf(false) }
    var position by remember(take.id) { mutableLongStateOf(0) }
    LaunchedEffect(take.id, take.fileName, vm.foregroundActive) {
        if (reviewing && !vm.foregroundActive) return@LaunchedEffect
        failed = false
        try { wave = if (reviewing) vm.reviewWaveform(take) else vm.waveform(take) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { failed = true }
    }
    LaunchedEffect(take.id, vm.playingId, vm.preparingId) {
        while (isActive) { position = vm.playbackPosition(take); delay(50) }
    }
    CompactRecordingWaveform(take.id, wave, position, take.durationMs, enabled && vm.foregroundActive,
        if (failed) "Waveform unavailable" else if (wave == null) "Loading waveform…" else null,
        Modifier.testTag("practice-$slot-waveform")) {
            if (reviewing) vm.seekReview(take, it) else vm.seekSaved(slot, take, it)
        }
}

/** One review surface for Free, Loop and both Reference workflows. */
@Composable internal fun ReviewRecordingScrubber(vm: RecordingViewModel) {
    val take = vm.reviewTake ?: return
    RecordingScrubber(take, "review", vm, !vm.busy && !vm.recording, reviewing = true)
}

/** Gesture owns only a preview; release submits exactly one source-bound seek. */
@Composable internal fun CompactRecordingWaveform(identity: String, waveform: AudioWaveform?, positionMs: Long,
    durationMs: Long, enabled: Boolean, status: String?, modifier: Modifier = Modifier, seek: (Long) -> Unit) {
    var preview by remember(identity) { mutableStateOf<Long?>(null) }
    val latestSeek by rememberUpdatedState(seek)
    val latestEnabled by rememberUpdatedState(enabled)
    val timeline = AudioTimeline(durationMs.coerceAtLeast(1))
    val primary = MaterialTheme.colorScheme.primary
    val cursor = MaterialTheme.colorScheme.onSurface
    val outline = MaterialTheme.colorScheme.outline
    val shown = preview ?: positionMs
    Column(Modifier.fillMaxWidth()) {
        Canvas(modifier.fillMaxWidth().height(48.dp)
            .pointerInput(identity, durationMs, enabled) {
                if (!enabled || durationMs <= 0) return@pointerInput
                try {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val start = down.position
                        var horizontal = false
                        var cancelled = false
                        preview = timeline.timeAt(start.x / size.width.coerceAtLeast(1))
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (change.isConsumed) { cancelled = true; break }
                            val delta = change.position - start
                            if (!horizontal && abs(delta.y) > viewConfiguration.touchSlop && abs(delta.y) > abs(delta.x)) { cancelled = true; break }
                            if (abs(delta.x) > viewConfiguration.touchSlop) horizontal = true
                            preview = timeline.timeAt(change.position.x / size.width.coerceAtLeast(1))
                            if (horizontal) change.consume()
                            if (!change.pressed) {
                                if (!cancelled && latestEnabled) latestSeek(checkNotNull(preview))
                                break
                            }
                        }
                        preview = null
                    }
                } finally { preview = null }
            }.semantics {
                contentDescription = status ?: "Recording waveform"
                stateDescription = "${audioTimestamp(shown)} of ${audioTimestamp(durationMs)}"
                if (enabled && durationMs > 0) {
                    progressBarRangeInfo = ProgressBarRangeInfo(shown.toFloat(), 0f..durationMs.toFloat())
                    setProgress { latestSeek(it.toLong().coerceIn(0, durationMs)); true }
                    customActions = listOf(
                        CustomAccessibilityAction("Seek forward one second") { latestSeek((shown + 1000).coerceAtMost(durationMs)); true },
                        CustomAccessibilityAction("Seek back one second") { latestSeek((shown - 1000).coerceAtLeast(0)); true })
                } else disabled()
            }) {
            drawLine(outline, Offset(0f, size.height / 2), Offset(size.width, size.height / 2))
            waveform?.let { drawAudioEnvelope(it, timeline, primary) }
            val x = timeline.fraction(shown).coerceIn(0f, 1f) * size.width
            drawLine(cursor, Offset(x, 0f), Offset(x, size.height), 2.5f)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(audioTimestamp(shown), style = MaterialTheme.typography.labelSmall)
            Text(audioTimestamp(durationMs), style = MaterialTheme.typography.labelSmall)
        }
        if (status != null) Text(status, style = MaterialTheme.typography.labelSmall)
    }
}
