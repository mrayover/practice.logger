package com.mrayover.practicelogger

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.*
import java.util.Locale
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
internal fun StandaloneTranscribeScreen(
    media: MediaLibraryViewModel, timer: TranscribeViewModel, goals: List<GoalBundle>, onBack: () -> Unit
) {
    val assets by media.assets.collectAsStateWithLifecycle()
    val selectedId = timer.audioId
    var recordingReference by rememberSaveable { mutableStateOf(false) }
    val stopwatch: @Composable () -> Unit = { TranscribeStopwatch(timer, goals) }
    fun leave() {
        timer.leaveWorkspace()
        onBack()
    }
    var choosing by rememberSaveable { mutableStateOf(false) }
    val asset = assets.find { it.id == selectedId && it.category == "AUDIO" }
    if (asset != null) {
        key(asset.id) {
            LibraryAudioScreen(asset, ::leave, onChangeAudio = { choosing = true }, stopwatch = stopwatch,
                onRecordReference = { recordingReference = true })
        }
    } else {
        BackHandler(onBack = ::leave)
        Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = ::leave, modifier = Modifier.heightIn(min = 48.dp)) { Text("Back to Practice Now") }
            PageHeading("Transcribe", style = MaterialTheme.typography.headlineLarge)
            if (selectedId != null) {
                Text(when {
                    media.readError -> "The Library could not load."
                    !media.ready -> "Loading audio…"
                    else -> "This recording is unavailable. Choose another audio file."
                })
            }
            ActionButton("Choose Audio", { choosing = true })
            TextButton(onClick = { recordingReference = true }) { Text("Record Reference") }
            stopwatch()
        }
    }
    if (recordingReference) RecordReferenceDialog(null, null,
        onClose = { recordingReference = false }, onKept = { timer.selectAudio(it); recordingReference = false })
    if (choosing) LibraryMediaPicker(MediaRole.AUDIO, selectedId, onClose = { choosing = false },
        allowAudioImport = true, onChoose = { timer.selectAudio(it); choosing = false })
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TranscribeStopwatch(timer: TranscribeViewModel, goals: List<GoalBundle>) {
    var chooseGoal by remember { mutableStateOf(false) }
    val goal = goals.find { it.goal.id == timer.goalId }?.goal
    val seconds = timer.elapsedMs / 1000
    val time = String.format(Locale.ROOT, "%02d:%02d:%02d", seconds / 3600, seconds / 60 % 60, seconds % 60)
    Column(Modifier.fillMaxWidth()) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(time, style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(androidx.compose.ui.Alignment.CenterVertically))
            if (!timer.running && !timer.finishing) {
                TextButton(onClick = timer::start, enabled = timer.ready && !timer.busy) {
                    Text(if (timer.hasActivity) "Resume" else "Start")
                }
            }
            if (timer.hasActivity) {
                TextButton(onClick = timer::stop, enabled = !timer.busy) { Text("Stop & Save") }
            }
            Box {
                TextButton(onClick = { chooseGoal = true }, enabled = timer.ready && !timer.busy && !timer.finishing) {
                    Text(goal?.title?.let { "Goal · $it" } ?: "No Goal")
                }
                DropdownMenu(expanded = chooseGoal, onDismissRequest = { chooseGoal = false }) {
                    DropdownMenuItem(text = { Text("No Goal") }, onClick = { timer.selectGoal(null); chooseGoal = false })
                    activeGoals(goals).forEach { entry ->
                        DropdownMenuItem(text = { Text(entry.goal.title) },
                            onClick = { timer.selectGoal(entry.goal.id); chooseGoal = false })
                    }
                }
            }
        }
        timer.message?.let { Text(it, style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
        if (!timer.ready && !timer.busy) TextButton(onClick = timer::reload) { Text("Retry") }
    }
}
