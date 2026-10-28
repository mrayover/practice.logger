package com.mrayover.practicelogger

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun practiceTime(ms: Long, roundUp: Boolean = false): String {
    val seconds = (ms.coerceAtLeast(0) / 1000L) + if (roundUp && ms % 1000L > 0) 1 else 0
    val minutes = seconds / 60L
    return minutes.toString() + ":" + (seconds % 60L).toString().padStart(2, '0')
}
fun sessionDate(ms: Long): String = DateTimeFormatter.ofPattern("MMM d, h:mm a").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(ms))

@Composable fun PracticeOverview(catalog: Catalog, sessions: SessionCatalog, onPractice: () -> Unit, onLibrary: () -> Unit, onNew: () -> Unit, onSummary: (String) -> Unit, onBackup: () -> Unit, onHome: () -> Unit) {
    BackHandler(onBack = onHome)
    Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TextButton(onClick = onHome) { Text("Back to Home") }
        Text("Routines", style = MaterialTheme.typography.headlineLarge)
        ActionButton("Practice Now", onPractice)
        OutlinedButton(onClick = onLibrary, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Blocks") }
        Text(catalog.blocks.count { !it.archived }.toString() + " active Blocks")
        TextButton(onClick = onNew) { Text("New Block") }
        TextButton(onClick = onBackup) { Text("Backup and restore") }
        Text("Recent Practice", style = MaterialTheme.typography.titleLarge)
        if (sessions.history.isEmpty()) Text("Completed sessions will appear here.")
        sessions.history.forEach { bundle ->
            Column(Modifier.fillMaxWidth().clickable(onClickLabel = "Open session summary") { onSummary(bundle.session.id) }.heightIn(min = 64.dp).padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(sessionDate(bundle.session.startedAt), style = MaterialTheme.typography.titleMedium)
                Text(bundle.ordered.count { it.outcome == "COMPLETED" }.toString() + " completed · " + bundle.ordered.count { it.outcome == "SKIPPED" } + " skipped · " + practiceTime(bundle.activeMs) + " active")
            }
            HorizontalDivider()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable fun SessionBuilder(blocks: List<Block>, draft: SessionBundle?, sessions: SessionCatalog, vm: PracticeViewModel, onHome: () -> Unit, onCreate: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var changeId by rememberSaveable { mutableStateOf<String?>(null) }
    var minutes by rememberSaveable { mutableStateOf("") }
    var validation by rememberSaveable { mutableStateOf(false) }
    var discard by rememberSaveable { mutableStateOf(false) }
    val entries = draft?.ordered ?: emptyList()
    BackHandler { onHome() }
    Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TextButton(onClick = onHome) { Text("Back to Home") }
        if (sessions.unfinished.isNotEmpty()) {
            Text("Unfinished sessions", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
            Text("Continue saved practice below, or build a new session. All sessions resume paused.")
            sessions.active?.let { active ->
                ActionButton("Continue Session", { vm.openSession(active.session.id) })
                Text(sessionDate(active.session.startedAt) + " · " + (active.current?.name ?: "Session") + " · Block " + (active.session.currentIndex + 1) + " of " + active.placements.size)
            }
            sessions.unfinished.filter { it.session.id != sessions.active?.session?.id }.forEach { bundle ->
                OutlinedButton(onClick = { vm.openSession(bundle.session.id) }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("continue-session-" + bundle.session.id)) {
                    Text("Continue: " + (bundle.ordered.firstOrNull()?.name ?: "Session") + " · " + sessionDate(bundle.session.startedAt))
                }
            }
        }
        Text("Build a Session", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
        Text("Arrange Blocks for this session. Duration changes here leave your saved Blocks unchanged.")
        if (entries.isEmpty()) Text("Add a Block below to begin.")
        entries.forEachIndexed { index, placement ->
            Column(Modifier.fillMaxWidth().testTag("placement-" + index), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text((index + 1).toString() + ". " + placement.name, style = MaterialTheme.typography.titleLarge)
                Text(placement.action)
                Text("Session duration: " + practiceTime(placement.configuredMs))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedButton(onClick = { vm.move(placement.id, -1) }, enabled = !vm.busy && index > 0, modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = "Move " + placement.name + ", placement " + (index + 1) + ", up" }) { Text("Up") }
                    OutlinedButton(onClick = { vm.move(placement.id, 1) }, enabled = !vm.busy && index < entries.lastIndex, modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = "Move " + placement.name + ", placement " + (index + 1) + ", down" }) { Text("Down") }
                }
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { changeId = placement.id; minutes = (placement.configuredMs / 60_000L).toString(); validation = false }, enabled = !vm.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Change duration") }
                    TextButton(onClick = { vm.remove(placement.id) }, enabled = !vm.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Remove") }
                }
                HorizontalDivider()
            }
        }
        ActionButton("Start Session", vm::start, enabled = !vm.busy && entries.isNotEmpty())
        if (entries.isNotEmpty()) TextButton(onClick = { discard = true }, enabled = !vm.busy) { Text("Discard draft session") }
        Text("Add Blocks", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(query, { query = it }, label = { Text("Find a Block") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedButton(onClick = onCreate, enabled = !vm.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Create a new Block") }
        val available = matchingBlocks(blocks, query, false)
        if (available.isEmpty()) Text("No matching active Blocks. Create a Block to add it here.")
        available.forEach { block ->
            OutlinedButton(onClick = { vm.addBlock(block.id) }, enabled = !vm.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Text("Add " + block.name)
                    Text(block.durationMinutes.toString() + " min · " + block.action, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
    if (changeId != null) AlertDialog(onDismissRequest = { changeId = null }, title = { Text("Session duration") }, text = {
        Column {
            Text("Only this placement changes; the saved Block stays unchanged.")
            OutlinedTextField(minutes, { minutes = it }, label = { Text("Minutes") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = validation, supportingText = { if (validation) Text("Enter positive whole minutes (up to 2147483647).") })
        }
    }, confirmButton = { TextButton(onClick = {
        if (minutes.trim().toIntOrNull()?.let { it > 0 } == true) { vm.duration(changeId!!, minutes); changeId = null } else validation = true
    }, enabled = !vm.busy) { Text("Apply duration") } }, dismissButton = { TextButton(onClick = { changeId = null }) { Text("Cancel") } })
    if (discard) AlertDialog(onDismissRequest = { discard = false }, title = { Text("Discard this draft session?") }, text = { Text("Saved Blocks and practice history are retained.") },
        confirmButton = { TextButton(onClick = { discard = false; vm.discardDraft() }, enabled = !vm.busy) { Text("Discard draft") } },
        dismissButton = { TextButton(onClick = { discard = false }) { Text("Keep draft") } })
}

@Composable fun FocusedPractice(bundle: SessionBundle?, vm: PracticeViewModel, onHome: () -> Unit, onRecordings: (String) -> Unit, recordingMessage: String?) {

    if (bundle == null || bundle.current == null) {
        Column(Modifier.safeDrawingPadding().padding(24.dp)) { Text("Session saved."); ActionButton("Home", onHome) }
        return
    }
    val current = bundle.current!!
    DisposableEffect(current.id, vm) {
        vm.enterFocusedPlacement(current)
        onDispose { vm.leaveFocusedPlacement() }
    }
    var note by rememberSaveable(current.id) { mutableStateOf(current.note) }
    fun leave() { vm.saveForLater(bundle.session.id, current.id, note) }
    BackHandler { leave() }
    val awaiting = current.activeMs >= current.configuredMs && !current.overtimeAccepted
    Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text("Block " + (bundle.session.currentIndex + 1) + " of " + bundle.placements.size, style = MaterialTheme.typography.titleMedium)
        Text(current.name, style = MaterialTheme.typography.headlineLarge)
        Text(current.action, style = MaterialTheme.typography.titleLarge)
        current.tempo?.let { Text(it.toString() + " BPM reference") }
        if (current.referenceNotes.isNotBlank()) Text(current.referenceNotes)
        Text(if (current.overtimeAccepted) "+" + practiceTime(current.activeMs - current.configuredMs) else practiceTime(current.configuredMs - current.activeMs, roundUp = true),
            style = MaterialTheme.typography.displayLarge, modifier = Modifier.fillMaxWidth().testTag("practice-timer").semantics { contentDescription = if (current.overtimeAccepted) "Overtime " + practiceTime(current.activeMs - current.configuredMs) else "Time remaining " + practiceTime(current.configuredMs - current.activeMs, roundUp = true) })
        Text(when { awaiting -> "Time reached. Continue for overtime or complete this Block."; vm.running -> "Practicing"; current.activeMs == 0L -> "Ready when you are"; else -> "Paused — time away from this screen is not counted" })
        if (awaiting) ActionButton("Continue", { vm.resume(overtime = true) }, !vm.busy)
        else ActionButton(if (vm.running) "Pause" else if (current.activeMs == 0L) "Start" else "Resume", { if (vm.running) vm.pause() else vm.resume() }, !vm.busy)
        ActionButton("Complete", { vm.finish("COMPLETED", note) }, !vm.busy)
        OutlinedButton(onClick = { vm.finish("SKIPPED", note) }, enabled = !vm.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Skip") }
        PracticeMetronome(vm)
        OutlinedButton(onClick = { onRecordings(current.sourceBlockId) }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Recordings") }
        recordingMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
        OutlinedTextField(note, { note = it }, label = { Text("Quick note (optional)") }, modifier = Modifier.fillMaxWidth())
        OutlinedButton(onClick = { vm.saveBlockNote(bundle.session.id, current.id, note) }, enabled = !vm.busy,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Save Block note") }
        if (vm.blockNoteSavedFor == current.id && note.trim() == current.note) Text("Block note saved", modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        Text("Notes save with Save Block note, Save for later, Back, Complete or Skip.")
        TextButton(onClick = ::leave, enabled = !vm.busy) { Text("Save for later") }
    }
}

@Composable fun SessionSummary(bundle: SessionBundle?, vm: PracticeViewModel, onHome: () -> Unit) {
    BackHandler { onHome() }
    if (bundle == null) {
        Column(Modifier.safeDrawingPadding().padding(24.dp)) { Text("This session is not available."); ActionButton("Home", onHome) }
        return
    }
    var note by rememberSaveable(bundle.session.id) { mutableStateOf(bundle.session.note) }
    Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Session Summary", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
        Text(sessionDate(bundle.session.startedAt))
        Text(practiceTime(bundle.activeMs) + " active practice", style = MaterialTheme.typography.headlineSmall)
        bundle.ordered.forEach { placement ->
            Text(placement.name, style = MaterialTheme.typography.titleLarge)
            Text(placement.action)
            Text((if (placement.outcome == "COMPLETED") "Completed" else "Skipped") + " · " + practiceTime(placement.activeMs) + " active · " + practiceTime(placement.configuredMs) + " planned")
            placement.lastUsedBpm?.let { Text("Last used click: $it BPM") }
            if (placement.note.isNotBlank()) Text(placement.note)
            HorizontalDivider()
        }
        OutlinedTextField(note, { note = it; vm.noteChanged() }, label = { Text("Session note (optional)") }, modifier = Modifier.fillMaxWidth())
        OutlinedButton(onClick = { vm.saveNote(bundle.session.id, note) }, enabled = !vm.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Save note") }
        if (vm.noteSaved && note.trim() == bundle.session.note) Text("Note saved")
        ActionButton("Home", onHome)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable private fun PracticeMetronome(vm: PracticeViewModel) {
    var editing by rememberSaveable { mutableStateOf(false) }
    var entered by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Metronome · ${vm.workingBpm} BPM", style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.testTag("metronome-bpm").semantics { heading() })
        Text("Working tempo · saved Block reference stays unchanged. Media volume controls the click.", style = MaterialTheme.typography.bodySmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            OutlinedButton(onClick = { vm.changeWorkingBpm((vm.workingBpm - 1).coerceAtLeast(30)) },
                enabled = vm.workingBpm > 30 && !vm.busy, modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = "Decrease click BPM by 1" }) { Text("−1") }
            OutlinedButton(onClick = { vm.changeWorkingBpm((vm.workingBpm + 1).coerceAtMost(300)) },
                enabled = vm.workingBpm < 300 && !vm.busy, modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = "Increase click BPM by 1" }) { Text("+1") }
            TextButton(onClick = { entered = vm.workingBpm.toString(); editing = true }, enabled = !vm.busy,
                modifier = Modifier.heightIn(min = 48.dp)) { Text("Set BPM") }
        }
        OutlinedButton(onClick = { if (vm.clicking) vm.stopClick() else vm.startClick() }, enabled = !vm.busy,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text(if (vm.clicking) "Stop click" else "Start click") }
        vm.clickError?.let { Text(it, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
    }
    if (editing) AlertDialog(onDismissRequest = { editing = false }, title = { Text("Working BPM") }, text = {
        OutlinedTextField(entered, { entered = it }, label = { Text("BPM (30–300)") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), isError = parseWorkingBpm(entered) == null,
            supportingText = { Text("Enter a whole number from 30 to 300.") })
    }, confirmButton = { TextButton(onClick = { parseWorkingBpm(entered)?.let { vm.changeWorkingBpm(it); editing = false } },
        enabled = parseWorkingBpm(entered) != null && !vm.busy) { Text("Apply BPM") } },
        dismissButton = { TextButton(onClick = { editing = false }) { Text("Cancel") } })
}

@Composable private fun ActionButton(label: String, click: () -> Unit, enabled: Boolean = true) {
    Button(onClick = click, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text(label) }
}
