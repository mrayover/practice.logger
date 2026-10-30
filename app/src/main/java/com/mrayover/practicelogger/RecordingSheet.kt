package com.mrayover.practicelogger

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle

fun takeTitle(take: BlockTake): String = take.label.ifBlank { "Take " + sessionDate(take.createdAt) + " · " + take.id.take(4) }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable fun BlockRecordings(blockId: String?, blockName: String, vm: RecordingViewModel,
    practice: PracticeViewModel, timer: SessionBundle?, initialBpm: Int, onBeforePlayback: () -> Unit, onDismiss: () -> Unit, startOnOpen: Boolean = false, references: ImageReferenceViewModel? = null, manageLifecycle: Boolean = true) {
    val keyboard = LocalSoftwareKeyboardController.current
    val all by vm.takes.collectAsStateWithLifecycle()
    val takes = all.filter { it.blockId == blockId }
    var renameId by rememberSaveable { mutableStateOf<String?>(null) }
    var label by rememberSaveable { mutableStateOf("") }
    var deleteId by rememberSaveable { mutableStateOf<String?>(null) }
    var exportId by rememberSaveable { mutableStateOf<String?>(null) }
    var phraseDialog by rememberSaveable { mutableStateOf(false) }
    var pendingStart by rememberSaveable { mutableStateOf(startOnOpen) }
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        vm.permissionResult(granted)
        if (granted && blockId == null) pendingStart = true
    }
    val exportResult: (android.net.Uri?) -> Unit = { uri ->
        val selected = all.find { it.id == exportId }
        if (uri != null) {
            if (selected != null) vm.export(selected, uri) else vm.expiredExport()
        }
        exportId = null
    }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("audio/mp4"), exportResult)
    val exportWave = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("audio/wav"), exportResult)
    if (manageLifecycle) DisposableEffect(blockId, vm) { vm.enterBlock(blockId); onDispose { vm.leaveBlock() } }
    LaunchedEffect(pendingStart, vm.ready, vm.busy, lifecycleState) {
        if (pendingStart && vm.ready && !vm.busy && lifecycleState == Lifecycle.State.RESUMED) {
            pendingStart = false // Recreation/background must never restart capture automatically.
            if (vm.hasMicrophonePermission()) vm.startCapture(blockId)
            else permission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
      Column(Modifier.fillMaxWidth().fillMaxHeight(0.9f).imePadding().padding(horizontal = 24.dp)) {
        vm.message?.let { Text(it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }.padding(bottom = 8.dp)) }
        if (vm.recording) {
            // Always reachable, including when a tall reference or take list is scrolled.
            Text(if (vm.phrasing && vm.countInBeat > 0) "Count-in · ${vm.countInBeat} of 4"
                else "Recording · " + practiceTime(vm.elapsedMs) + if (vm.phrasing) " / " + practiceTime(vm.phraseDurationMs) else "",
                style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.error,
                modifier = Modifier.testTag("recording-elapsed"))
            if (vm.phrasing && vm.countInBeat == 0) Text(if (vm.phraseClickEnabled) "Recording click on" else "Recording click off", modifier = Modifier.testTag("phrase-click-status"))
            Button(onClick = { vm.finishCapture() }, enabled = vm.ready && !vm.busy,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text(if (vm.phrasing) "Cancel phrase" else "Stop and save take") }
        }
        LazyColumn(Modifier.fillMaxWidth().weight(1f).testTag("take-list"),
            contentPadding = PaddingValues(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text(if (blockId == null) "Free recordings" else "Block recordings", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
                Text(blockName, style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text("Close recordings") }
                timer?.current?.let { placement ->
                    val awaiting = placement.activeMs >= placement.configuredMs && !placement.overtimeAccepted
                    val value = if (placement.overtimeAccepted) "+" + practiceTime(placement.activeMs - placement.configuredMs)
                        else practiceTime(placement.configuredMs - placement.activeMs, roundUp = true)
                    Text("Practice timer · $value · " + if (practice.running) "Running" else "Paused", style = MaterialTheme.typography.bodyMedium)
                    OutlinedButton(onClick = { if (practice.running) practice.pause() else practice.resume(overtime = awaiting) }, enabled = !practice.busy,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Text(if (practice.running) "Pause timer" else if (awaiting) "Continue timer" else if (placement.activeMs == 0L) "Start timer" else "Resume timer")
                    }
                }
            }
            item {
                if (!vm.recording) {
                Button(onClick = {
                    if (vm.recording) vm.finishCapture()
                    else if (vm.hasMicrophonePermission()) vm.startCapture(blockId)
                    else permission.launch(Manifest.permission.RECORD_AUDIO)
                }, enabled = vm.ready && !vm.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                    Text(if (vm.phrasing) "Cancel phrase" else if (vm.recording) "Stop and save take" else "Record take")
                }
                Text("Free recording · no BPM required. A running click may be captured. Leaving stops and saves usable audio.", style = MaterialTheme.typography.bodySmall)
                if (blockId != null) {
                OutlinedButton(onClick = { keyboard?.hide(); phraseDialog = true }, enabled = vm.ready && !vm.busy && !vm.recording,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Record phrase") }
                Text("Four clicks, then one fixed phrase. It saves and repeats until stopped. Leaving cancels an incomplete phrase.", style = MaterialTheme.typography.bodySmall)
                }
                if (takes.any { it.id == vm.playingId && it.mode == "LOOP" }) {
                    Button(onClick = vm::stopPlayback, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Stop loop") }
                }
                if (vm.busy) Text("Working…")
                }
            }
            item(key = "block-reference") {
                if (blockId != null && references != null) PracticeImageReference(blockId, "recording-$blockId", references)
            }
            item {
                Text("JSON backups exclude recordings. Export each take to keep a copy outside this app. Uninstalling or clearing the app loses unexported audio.", style = MaterialTheme.typography.bodySmall)
                Text("Saved takes (${takes.size})", style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
                if (takes.isEmpty()) Text(if (blockId == null) "No saved free recordings yet." else "No saved takes for this Block yet.")
            }
            items(takes, key = { it.id }) { take ->
                Column(Modifier.fillMaxWidth().testTag("take-" + take.id), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val title = takeTitle(take)
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text((if (take.mode == "LOOP") "Loop · ${take.recordedBpm} BPM · ${take.beatCount} beats · " else "Free · ") + practiceTime(take.durationMs) + " · " + sessionDate(take.createdAt), style = MaterialTheme.typography.bodySmall)
                    val playing = vm.playingId == take.id || vm.preparingId == take.id
                    if (vm.preparingId == take.id) Text("Preparing playback…")
                    OutlinedButton(onClick = {
                        if (playing) vm.stopPlayback() else { onBeforePlayback(); vm.play(take) }
                    }, enabled = !vm.busy && !vm.recording, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .testTag("play-" + take.id).semantics { contentDescription = (if (playing) "Stop " else "Play ") + title }) {
                        Text(if (playing) "Stop playback" else if (take.mode == "LOOP") "Play loop" else "Play take")
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = { renameId = take.id; label = take.label }, enabled = !vm.busy && !vm.recording,
                            modifier = Modifier.heightIn(min = 48.dp).testTag("rename-" + take.id).semantics { contentDescription = "Rename $title" }) { Text("Rename") }
                        TextButton(onClick = { exportId = take.id; if (take.mode == "LOOP") exportWave.launch("practice-loop-${take.id}.wav") else export.launch("practice-take-${take.id}.m4a") }, enabled = !vm.busy && !vm.recording,
                            modifier = Modifier.heightIn(min = 48.dp).testTag("export-" + take.id).semantics { contentDescription = "Export $title" }) { Text("Export audio") }
                        TextButton(onClick = { deleteId = take.id }, enabled = !vm.busy && !vm.recording,
                            modifier = Modifier.heightIn(min = 48.dp).testTag("delete-" + take.id).semantics { contentDescription = "Delete $title" }) { Text("Delete") }
                    }
                    HorizontalDivider()
                }
            }
        }
      }
    }
    if (phraseDialog) {
        blockId?.let { PhraseSetupDialog(it, vm, initialBpm, onBeforePlayback, { permission.launch(Manifest.permission.RECORD_AUDIO) }, { phraseDialog = false }) }
    }

    all.find { it.id == renameId }?.let { take ->
        AlertDialog(onDismissRequest = { renameId = null }, title = { Text("Rename take") }, text = {
            OutlinedTextField(label, { label = it }, label = { Text("Take label (optional)") }, isError = label.trim().length > 120,
                supportingText = { Text("Up to 120 characters. Leave blank to use the date.") })
        }, confirmButton = { TextButton(onClick = { vm.rename(take, label); renameId = null }, enabled = !vm.busy && label.trim().length <= 120) { Text("Save take label") } },
            dismissButton = { TextButton(onClick = { renameId = null }) { Text("Cancel") } })
    }
    all.find { it.id == deleteId }?.let { take ->
        AlertDialog(onDismissRequest = { deleteId = null }, title = { Text("Delete this take?") },
            text = { Text("Delete " + takeTitle(take) + " and its audio? This cannot be undone. Other takes and practice history are kept.") },
            confirmButton = { TextButton(onClick = { vm.delete(take); deleteId = null }, enabled = !vm.busy) { Text("Delete take permanently") } },
            dismissButton = { TextButton(onClick = { deleteId = null }) { Text("Keep take") } })
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable private fun PhraseSetupDialog(blockId: String, vm: RecordingViewModel, initialBpm: Int, onBeforePlayback: () -> Unit, requestPermission: () -> Unit, onDismiss: () -> Unit) {
    val keyboard = LocalSoftwareKeyboardController.current
    var phraseBpm by rememberSaveable(blockId) { mutableStateOf(initialBpm.coerceIn(30, 300).toString()) }
    var phraseBeats by rememberSaveable(blockId) { mutableIntStateOf(8) }
    var phraseClick by rememberSaveable(blockId) { mutableStateOf(true) }
        val bpm = phraseBpm.toIntOrNull()
        val spec = bpm?.takeIf { it in 30..300 }?.let { PhraseSpec(it, phraseBeats) }
        AlertDialog(onDismissRequest = { onDismiss() }, title = { Text("Record one phrase") }, text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(phraseBpm, { phraseBpm = it }, label = { Text("Phrase BPM") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { keyboard?.hide() }), supportingText = { Text("30–300") },
                    modifier = Modifier.padding(top = 8.dp), isError = spec == null, singleLine = true)
                Text("Phrase length in beats")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(4, 8, 16, 32).forEach { beats ->
                        FilterChip(selected = phraseBeats == beats, onClick = { phraseBeats = beats }, label = { Text("$beats beats") })
                    }
                }
                Text(spec?.let { "One phrase: " + String.format(java.util.Locale.getDefault(), "%.2f seconds", it.durationMs / 1000.0) } ?: "Enter a whole BPM from 30 to 300.")
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("phrase-recording-click")
                    .toggleable(value = phraseClick, role = Role.Checkbox, onValueChange = { phraseClick = it }),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Checkbox(checked = phraseClick, onCheckedChange = null)
                    Text("Click while recording", modifier = Modifier.weight(1f))
                }
                Text("The click follows the phrase BPM and may be heard in the microphone recording.", style = MaterialTheme.typography.bodySmall)
                Text("Listen to four clicks, then play on the next beat. Recording ends automatically and loops at this fixed tempo. A new tempo needs a new take.")
            }
        }, confirmButton = { TextButton(onClick = {
            if (spec != null) {
                keyboard?.hide(); onDismiss()
                if (vm.hasMicrophonePermission()) { onBeforePlayback(); vm.startPhrase(blockId, spec, phraseClick) }
                else requestPermission()
            }
        }, enabled = spec != null && vm.ready && !vm.busy && !vm.recording) { Text("Start 4-click count-in") } },
            dismissButton = { TextButton(onClick = { onDismiss() }) { Text("Cancel") } })
}

@Composable fun CompactRecordingControls(blockId: String, initialBpm: Int, vm: RecordingViewModel, onBeforePlayback: () -> Unit) {
    val all by vm.takes.collectAsStateWithLifecycle()
    val takes = all.filter { it.blockId == blockId }
    var phraseDialog by rememberSaveable(blockId) { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission(), vm::permissionResult)
    val enabled = vm.ready && !vm.busy
    Column {
        if (vm.recording) Text(if (vm.countInBeat > 0) "Count-in · ${vm.countInBeat} of 4" else "Recording · " + practiceTime(vm.elapsedMs),
            color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.testTag("compact-recording-status").semantics { liveRegion = LiveRegionMode.Polite })
        listOf(false, true).forEach { loop ->
            val latest = takes.filter { (it.mode == "LOOP") == loop }.maxByOrNull { it.createdAt }
            val playing = latest != null && (vm.playingId == latest.id || vm.preparingId == latest.id)
            Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(if (loop) "Loop recording" else "Free recording", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                OutlinedButton(onClick = {
                    if (vm.recording) vm.finishCapture()
                    else if (loop) phraseDialog = true
                    else if (vm.hasMicrophonePermission()) vm.startCapture(blockId)
                    else permission.launch(Manifest.permission.RECORD_AUDIO)
                }, enabled = enabled && !vm.recording, contentPadding = PaddingValues(horizontal = 8.dp), modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(if (vm.recording && vm.phrasing == loop) "Recording…" else if (loop) "Record Loop" else "Record", style = MaterialTheme.typography.bodyMedium)
                }
                TextButton(onClick = { if (playing) vm.stopPlayback() else latest?.let { onBeforePlayback(); vm.play(it) } }, enabled = enabled && !vm.recording && latest != null,
                    contentPadding = PaddingValues(horizontal = 8.dp), modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = (if (playing) "Stop" else "Play") + if (loop) " latest loop" else " latest free take" }) { Text(if (playing) "Stop" else "Play") }
            }
        }
    }
    if (phraseDialog) PhraseSetupDialog(blockId, vm, initialBpm, onBeforePlayback, { permission.launch(Manifest.permission.RECORD_AUDIO) }, { phraseDialog = false })
}
