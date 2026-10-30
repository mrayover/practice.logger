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

@Composable fun PracticeOverview(catalog: Catalog, sessions: SessionCatalog, onPractice: () -> Unit, onLibrary: () -> Unit, onNew: () -> Unit, onSummary: (String) -> Unit, onBackup: () -> Unit, onHome: () -> Unit, routines: RoutineViewModel, practice: PracticeViewModel, onNewRoutine: () -> Unit, onEditRoutine: (String) -> Unit) {
    BackHandler(onBack = onHome)
    Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        BackToHomeButton(onClick = onHome)
        Text("Routines", style = MaterialTheme.typography.headlineLarge)
        SavedRoutines(catalog, routines, practice, onNewRoutine, onEditRoutine)
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
@Composable fun SessionBuilder(blocks: List<Block>, draft: SessionBundle?, sessions: SessionCatalog, vm: PracticeViewModel, routines: RoutineViewModel, onHome: () -> Unit, onCreate: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var changeId by rememberSaveable { mutableStateOf<String?>(null) }
    var minutes by rememberSaveable { mutableStateOf("") }
    var validation by rememberSaveable { mutableStateOf(false) }
    var discard by rememberSaveable { mutableStateOf(false) }
    val entries = draft?.ordered ?: emptyList()
    BackHandler { onHome() }
    Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        BackToHomeButton(onClick = onHome)
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
                    OutlinedButton(onClick = { vm.move(placement.id, -1) }, enabled = !vm.busy && !routines.busy && index > 0, modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = "Move " + placement.name + ", placement " + (index + 1) + ", up" }) { Text("Up") }
                    OutlinedButton(onClick = { vm.move(placement.id, 1) }, enabled = !vm.busy && !routines.busy && index < entries.lastIndex, modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = "Move " + placement.name + ", placement " + (index + 1) + ", down" }) { Text("Down") }
                }
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { changeId = placement.id; minutes = (placement.configuredMs / 60_000L).toString(); validation = false }, enabled = !vm.busy && !routines.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Change duration") }
                    TextButton(onClick = { vm.remove(placement.id) }, enabled = !vm.busy && !routines.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Remove") }
                }
                HorizontalDivider()
            }
        }
        ActionButton("Start Session", vm::start, enabled = !vm.busy && !routines.busy && entries.isNotEmpty())
        SaveAsRoutine(draft, vm.busy, routines)
        if (entries.isNotEmpty()) TextButton(onClick = { discard = true }, enabled = !vm.busy && !routines.busy) { Text("Discard draft session") }
        Text("Add Blocks", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(query, { query = it }, label = { Text("Find a Block") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedButton(onClick = onCreate, enabled = !vm.busy && !routines.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Create a new Block") }
        val available = matchingBlocks(blocks, query, false)
        if (available.isEmpty()) Text("No matching active Blocks. Create a Block to add it here.")
        available.forEach { block ->
            OutlinedButton(onClick = { vm.addBlock(block.id) }, enabled = !vm.busy && !routines.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
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
    }, enabled = !vm.busy && !routines.busy) { Text("Apply duration") } }, dismissButton = { TextButton(onClick = { changeId = null }) { Text("Cancel") } })
    if (discard) AlertDialog(onDismissRequest = { discard = false }, title = { Text("Discard this draft session?") }, text = { Text("Saved Blocks and practice history are retained.") },
        confirmButton = { TextButton(onClick = { discard = false; vm.discardDraft() }, enabled = !vm.busy && !routines.busy) { Text("Discard draft") } },
        dismissButton = { TextButton(onClick = { discard = false }) { Text("Keep draft") } })
}

@OptIn(ExperimentalLayoutApi::class)
@Composable fun FocusedPractice(bundle: SessionBundle?, vm: PracticeViewModel, images: ImageReferenceViewModel, onHome: () -> Unit, onRecordings: (String) -> Unit, recordingMessage: String?, goalName: String? = null, recordings: RecordingViewModel) {
    if (bundle == null || bundle.current == null) {
        Column(Modifier.safeDrawingPadding().padding(24.dp)) { Text("Session saved."); ActionButton("Home", onHome) }
        return
    }
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val current = bundle.current!!
    DisposableEffect(current.id, vm, recordings) {
        vm.enterFocusedPlacement(current); recordings.enterBlock(current.sourceBlockId)
        onDispose { vm.leaveFocusedPlacement(); recordings.leaveBlock() }
    }
    var note by rememberSaveable(current.id) { mutableStateOf(current.note) }
    var transportPanel by rememberSaveable(current.id) { mutableStateOf<String?>(null) }
    var savingNote by remember(current.id) { mutableStateOf(false) }
    LaunchedEffect(savingNote, vm.busy, current.note, vm.blockNoteSavedFor) {
        if (savingNote && !vm.busy) {
            if (vm.blockNoteSavedFor == current.id && current.note == note.trim()) {
                if (transportPanel == "notes") transportPanel = null
                focusManager.clearFocus(force = true); keyboard?.hide()
                savingNote = false
            } else if (vm.error != null) savingNote = false
            // Room can deliver the saved placement after the operation finishes; wait for it.
        }
    }
    val transportScroll = rememberScrollState()
    LaunchedEffect(transportPanel) { transportScroll.scrollTo(0) }
    var referenceOpen by rememberSaveable(current.id) { mutableStateOf(false) }
    var leaving by rememberSaveable(current.id) { mutableStateOf(false) }
    val awaiting = current.activeMs >= current.configuredMs && !current.overtimeAccepted
    val audioBusy = recordings.recording || recordings.busy
    fun requestLeave() { if (!vm.busy) leaving = true }
    BackHandler { requestLeave() }
    BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
    val transportMaxHeight = maxHeight * 0.60f
    val panelMaxHeight = maxHeight * 0.28f
    Column(Modifier.fillMaxSize().padding(vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp).testTag("practice-block-heading"), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Block " + (bundle.session.currentIndex + 1) + " of " + bundle.placements.size, style = MaterialTheme.typography.labelLarge)
                goalName?.let { Text("Goal: " + it, style = MaterialTheme.typography.bodySmall) }
                Text(current.name, style = MaterialTheme.typography.titleMedium)
            }
            Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceVariant) {
                Text(if (current.overtimeAccepted) "+" + practiceTime(current.activeMs - current.configuredMs) else practiceTime(current.configuredMs - current.activeMs, roundUp = true),
                    style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp).testTag("practice-timer").semantics {
                        contentDescription = if (current.overtimeAccepted) "Overtime " + practiceTime(current.activeMs - current.configuredMs) else "Time remaining " + practiceTime(current.configuredMs - current.activeMs, roundUp = true)

                    })
            }
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().testTag("practice-workspace-viewport")) {
            val workspaceHeight = maxHeight
            // Use spare portrait space for references; short screens, notes and large text scroll naturally.
            val stretchReference = workspaceHeight >= 420.dp &&
                androidx.compose.ui.platform.LocalDensity.current.fontScale < 1.5f
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .heightIn(min = workspaceHeight).testTag("practice-workspace"), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Practice Description", style = MaterialTheme.typography.labelLarge, modifier = Modifier.semantics { heading() })
                        Text(current.action, style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 32.dp, max = 80.dp).verticalScroll(rememberScrollState()).testTag("practice-description"))
                    }
                }
                OutlinedCard(Modifier.fillMaxWidth().then(if (stretchReference && referenceOpen) Modifier.weight(1f) else Modifier)
                    .testTag("practice-reference-area")) {
                    TextButton(onClick = { referenceOpen = !referenceOpen },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("practice-reference-toggle")
                            .semantics { stateDescription = if (referenceOpen) "Expanded" else "Collapsed" }) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Text("Reference Material", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f).semantics { heading() })
                            Text(if (referenceOpen) "Collapse" else "Expand", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    if (referenceOpen) {
                        Column(Modifier.fillMaxWidth().then(if (stretchReference) Modifier.fillMaxHeight() else Modifier)
                            .heightIn(min = 52.dp, max = if (stretchReference) workspaceHeight else 252.dp)
                            .verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (current.referenceNotes.isNotBlank()) Text(current.referenceNotes, style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(horizontal = 12.dp).heightIn(max = 80.dp).verticalScroll(rememberScrollState()))
                            current.tempo?.let { Text("$it BPM reference", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 12.dp)) }
                            PracticeImageReference(current.sourceBlockId, current.id, images, viewerMaxHeight = 220.dp, embeddedExpanded = true)
                            Spacer(Modifier.height(4.dp))
                        }
                    }
                }
                if (stretchReference && !referenceOpen) Spacer(Modifier.weight(1f))
            }
        }
        // A sibling of the weighted workspace: panel height takes space from the reference.
        // On short displays/large text the transport itself scrolls, while primary actions stay pinned.
        Column(Modifier.fillMaxWidth().heightIn(max = transportMaxHeight)
            .verticalScroll(transportScroll).padding(horizontal = 16.dp).testTag("practice-transport")) {
            if (transportPanel == "notes") Column(Modifier.fillMaxWidth().heightIn(max = panelMaxHeight)
                .verticalScroll(rememberScrollState()).testTag("practice-notes-panel")) {
                OutlinedTextField(note, { note = it }, label = { Text("Quick note (optional)") }, modifier = Modifier.fillMaxWidth())
                TextButton(onClick = {
                    vm.saveBlockNote(bundle.session.id, current.id, note); savingNote = true
                }, enabled = !vm.busy, modifier = Modifier.heightIn(min = 48.dp)) { Text("Save and return") }
                vm.error?.let { Text(it, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
            }
            if (transportPanel == "loop") Column(Modifier.fillMaxWidth().heightIn(max = panelMaxHeight)
                .verticalScroll(rememberScrollState())) {
                val permission = androidx.activity.compose.rememberLauncherForActivityResult(
                    androidx.activity.result.contract.ActivityResultContracts.RequestPermission(), recordings::permissionResult)
                PhraseSetupDialog(current.sourceBlockId, recordings, vm.workingBpm, vm::stopClick,
                    { permission.launch(android.Manifest.permission.RECORD_AUDIO) }, { transportPanel = null }, inline = true)
            }
            PracticeMetronome(vm)
            CompactRecordingControls(current.sourceBlockId, current.id, recordings, vm::stopClick,
                { transportPanel = if (transportPanel == "loop") null else "loop" })
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { transportPanel = if (transportPanel == "notes") null else "notes" },
                    modifier = Modifier.heightIn(min = 48.dp).semantics { stateDescription = if (transportPanel == "notes") "Expanded" else "Collapsed" }) { Text("Notes") }
                TextButton(onClick = ::requestLeave, enabled = !vm.busy, modifier = Modifier.heightIn(min = 48.dp)) { Text("Save for later") }
            }
            recordingMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
        }
        if (recordings.recording) {
            val status: @Composable (Modifier) -> Unit = { modifier ->
                Text(if (recordings.countInBeat > 0) "Count-in · ${recordings.countInBeat} of 4" else "Recording · " + practiceTime(recordings.elapsedMs),
                    color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, modifier = modifier)
            }
            val stop: @Composable (Modifier) -> Unit = { modifier ->
                Button(onClick = { recordings.finishCapture() }, enabled = recordings.ready && !recordings.busy,
                    modifier = modifier.heightIn(min = 48.dp).testTag("practice-recording-stop")) { Text(if (recordings.phrasing) "Cancel phrase" else "Stop and save take") }
            }
            if (androidx.compose.ui.platform.LocalDensity.current.fontScale >= 1.5f) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    status(Modifier.fillMaxWidth()); stop(Modifier.fillMaxWidth())
                }
            } else Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                status(Modifier.weight(1f)); stop(Modifier)
            }
        }
        Text(when { awaiting -> "Time reached. Continue for overtime or complete this Block."; vm.running -> "Practicing"; current.activeMs == 0L -> "Ready when you are"; else -> "Paused — time away from this screen is not counted" },
            style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 16.dp).semantics { liveRegion = LiveRegionMode.Polite })
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp).testTag("practice-bottom-actions"), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(onClick = { if (vm.running) vm.pause() else vm.resume(overtime = awaiting) }, enabled = !vm.busy,
                contentPadding = PaddingValues(horizontal = 4.dp), modifier = Modifier.weight(1.35f).heightIn(min = 48.dp)) {
                Text(if (awaiting) "Continue" else if (vm.running) "Pause" else if (current.activeMs == 0L) "Start" else "Resume")
            }
            Button(onClick = { vm.finish("COMPLETED", note) }, enabled = !vm.busy && !audioBusy,
                contentPadding = PaddingValues(horizontal = 4.dp), modifier = Modifier.weight(1.35f).heightIn(min = 48.dp)) { Text("Complete") }
            OutlinedButton(onClick = { vm.finish("SKIPPED", note) }, enabled = !vm.busy && !audioBusy,
                contentPadding = PaddingValues(horizontal = 4.dp), modifier = Modifier.weight(.7f).heightIn(min = 48.dp)) { Text("Skip") }
        }
    }
    }
    if (leaving) AlertDialog(onDismissRequest = { if (!vm.busy) leaving = false }, title = { Text("Leave current session?") }, text = {
        Text(if (audioBusy) "Stop and save your recording, or cancel the phrase, before leaving. Your current practice note is kept here."
            else "Your current session and note will be saved for later, and practice will pause.")
    }, confirmButton = { TextButton(onClick = { vm.saveForLater(bundle.session.id, current.id, note) }, enabled = !vm.busy && !audioBusy) { Text("Back to Main") } },
        dismissButton = { TextButton(onClick = { leaving = false }, enabled = !vm.busy) { Text("Continue") } })
}

@Composable fun SessionSummary(bundle: SessionBundle?, vm: PracticeViewModel, onHome: () -> Unit, goalName: String? = null) {
    if (bundle == null) {
        Column(Modifier.safeDrawingPadding().padding(24.dp)) { Text("This session is not available."); ActionButton("Home", onHome) }
        return
    }
    var note by rememberSaveable(bundle.session.id) { mutableStateOf(bundle.session.note) }
    var discard by rememberSaveable(bundle.session.id) { mutableStateOf(false) }
    fun leave() { if (!vm.busy) { if (note.trim() != bundle.session.note) discard = true else onHome() } }
    BackHandler { leave() }
    if (discard) AlertDialog(onDismissRequest = { discard = false }, title = { Text("Discard unsaved session note?") },
        text = { Text("Save note first to keep these changes.") },
        confirmButton = { TextButton(onClick = { discard = false; onHome() }) { Text("Discard") } },
        dismissButton = { TextButton(onClick = { discard = false }) { Text("Keep editing") } })
    Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Session Summary", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
        goalName?.let { Text("Goal: " + it) }
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
        ActionButton("Home", ::leave)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable private fun PracticeMetronome(vm: PracticeViewModel) {
    var editing by rememberSaveable { mutableStateOf(false) }
    var entered by rememberSaveable { mutableStateOf("") }
    val largeText = androidx.compose.ui.platform.LocalDensity.current.fontScale >= 1.5f
    val bpmControl: @Composable (Modifier) -> Unit = { modifier ->
        TextButton(onClick = { entered = vm.workingBpm.toString(); editing = true }, enabled = !vm.busy,
            contentPadding = PaddingValues(horizontal = 2.dp), modifier = modifier.heightIn(min = 48.dp).testTag("metronome-bpm").semantics { contentDescription = "Set BPM" }) {
            Text("Metronome · ${vm.workingBpm} BPM", style = MaterialTheme.typography.bodyMedium)
        }
    }
    val controls: @Composable () -> Unit = {
        TextButton(onClick = { vm.changeWorkingBpm((vm.workingBpm - 1).coerceAtLeast(30)) }, enabled = vm.workingBpm > 30 && !vm.busy,
            contentPadding = PaddingValues(0.dp), modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp).width(48.dp).semantics { contentDescription = "Decrease click BPM by 1" }) { Text("−1") }
        TextButton(onClick = { vm.changeWorkingBpm((vm.workingBpm + 1).coerceAtMost(300)) }, enabled = vm.workingBpm < 300 && !vm.busy,
            contentPadding = PaddingValues(0.dp), modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp).width(48.dp).semantics { contentDescription = "Increase click BPM by 1" }) { Text("+1") }
        OutlinedButton(onClick = { if (vm.clicking) vm.stopClick() else vm.startClick() }, enabled = !vm.busy,
            contentPadding = PaddingValues(horizontal = 8.dp), modifier = Modifier.heightIn(min = 48.dp)) { Text(if (vm.clicking) "Stop click" else "Start click", style = MaterialTheme.typography.bodyMedium) }
    }
    if (largeText) FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        bpmControl(Modifier.fillMaxWidth())
        controls()
    } else Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        bpmControl(Modifier.weight(1f))
        controls()
    }
    vm.clickError?.let { Text(it, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
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
