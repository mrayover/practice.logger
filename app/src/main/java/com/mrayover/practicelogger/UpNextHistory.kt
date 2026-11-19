package com.mrayover.practicelogger

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

@Composable
internal fun UpNextHistory(current: Placement, practice: PracticeViewModel, recordings: RecordingViewModel) {
    val catalog by practice.catalog.collectAsStateWithLifecycle()
    val (reflection, rating) = remember(catalog.bundles, current.id, current.sourceBlockId) {
        latestBlockReviews(catalog.bundles, current)
    }
    reflection?.let { entry ->
        UpNextCard("Last Reflection") {
            UpNextDate(entry.completion.completedAt)
            entry.reflection?.let { BlockReflectionAnswers(it) }
        }
    }
    rating?.let { entry ->
        UpNextCard("Last Session Ratings") {
            UpNextDate(entry.completion.completedAt)
            entry.reflection?.security?.let { BlockSecurityRating(it) }
        }
    }

    val takes by recordings.takes.collectAsStateWithLifecycle()
    var latest by remember(recordings) { mutableStateOf<BlockTake?>(null) }
    LaunchedEffect(recordings, takes, recordings.foregroundActive) {
        if (recordings.foregroundActive) latest = recordings.latestAccessibleFreeRecording(takes)
    }
    // Reflect metadata/deletions immediately while the latest file selection runs off the UI thread.
    val take = latest?.let { selected -> takes.find { it.id == selected.id } }
    if (take != null) {
        DisposableEffect(recordings, take.id) {
            recordings.enterBlock(null)
            onDispose { recordings.leaveBlock() }
        }
        var playbackRequested by remember(take.id) { mutableStateOf(false) }
        val scope = rememberCoroutineScope()
        val playing = recordings.playingId == take.id
        val preparing = recordings.preparingId == take.id
        UpNextCard("Last Free Recording") {
            Text(takeTitle(take), style = MaterialTheme.typography.titleSmall)
            Text("${sessionDate(take.createdAt)} · ${takeDuration(take)}",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(
                onClick = {
                    if (playing) recordings.stopPlayback()
                    else scope.launch {
                        val available = recordings.latestAccessibleFreeRecording(recordings.takes.value)
                        latest = available
                        if (available?.id == take.id) {
                            playbackRequested = true
                            recordings.play(available)
                        }
                    }
                },
                enabled = recordings.ready && recordings.foregroundActive && !recordings.busy && !preparing,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text(when {
                    playing -> "Stop"
                    preparing -> "Preparing recording…"
                    else -> "Play recording"
                })
            }
            if (playbackRequested) recordings.message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
private fun UpNextCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
            content()
        }
    }
}

@Composable
private fun UpNextDate(time: Long) {
    Text(sessionDate(time), style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
}
