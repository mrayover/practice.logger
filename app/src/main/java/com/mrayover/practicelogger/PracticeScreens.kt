package com.mrayover.practicelogger

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
@Composable fun FocusedPractice(bundle: SessionBundle?, vm: PracticeViewModel, images: ImageReferenceViewModel, onHome: () -> Unit, onRecordings: (String) -> Unit, recordingMessage: String?, goalName: String? = null, recordings: RecordingViewModel, notes: BlockNotesViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    if (bundle == null || bundle.current == null) {
        Column(Modifier.safeDrawingPadding().padding(24.dp)) { Text("Session saved.") }
        return
    }
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val current = bundle.current!!
    DisposableEffect(current.id, vm, recordings) {
        vm.enterFocusedPlacement(current); recordings.enterBlock(current.sourceBlockId)
        onDispose { vm.leaveFocusedPlacement(); recordings.leaveBlock() }
    }
    val loopPermission = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission(), recordings::permissionResult)
    val note = current.note // Preserve the legacy placement note in existing session actions.
    val noteDraft by notes.draft(current.id).collectAsStateWithLifecycle()
    val noteHistory by notes.history.collectAsStateWithLifecycle()
    var transportPanel by rememberSaveable(current.id) { mutableStateOf<String?>(null) }
    // Only presentation state: a workspace tap must never discard/stop a temporary take.
    var reviewCollapsed by rememberSaveable(current.id, recordings.reviewTake?.id) { mutableStateOf(false) }
    var bpmDialog by rememberSaveable(current.id) { mutableStateOf(false) }
    var pendingNoteAction by rememberSaveable(current.id) { mutableStateOf<String?>(null) }
    val transportScroll = rememberScrollState()
    LaunchedEffect(transportPanel) { transportScroll.scrollTo(0) }
    val allTakes by recordings.takes.collectAsStateWithLifecycle()
    val takes = allTakes.filter { it.blockId == current.sourceBlockId }.sortedWith(compareByDescending<BlockTake> { it.createdAt }.thenBy { it.id })
    val freeTakes = takes.filter { it.mode != "LOOP" }
    val loopTakes = takes.filter { it.mode == "LOOP" }
    var freeId by rememberSaveable(current.id) { mutableStateOf<String?>(null) }
    var loopId by rememberSaveable(current.id) { mutableStateOf<String?>(null) }
    LaunchedEffect(freeTakes.map { it.id }, loopTakes.map { it.id }) {
        if (freeTakes.none { it.id == freeId }) freeId = freeTakes.firstOrNull()?.id
        if (loopTakes.none { it.id == loopId }) loopId = loopTakes.firstOrNull()?.id
    }
    val freeTake = freeTakes.find { it.id == freeId } ?: freeTakes.firstOrNull()
    val loopTake = loopTakes.find { it.id == loopId } ?: loopTakes.firstOrNull()
    var leaving by rememberSaveable(current.id) { mutableStateOf(false) }
    val awaiting = current.activeMs >= current.configuredMs && !current.overtimeAccepted
    val audioBusy = recordings.recording || recordings.busy || recordings.reviewTake != null
    fun performPracticeAction(action: String) {
        if (action == "leave") leaving = true else vm.finish(action, note, preserveNote = true)
    }
    fun requestPracticeAction(action: String) {
        if (vm.busy || notes.busy) return
        if (noteDraft.isNotBlank() || notes.hasUnsavedEdits(current.sourceBlockId)) pendingNoteAction = action else performPracticeAction(action)
    }
    fun requestLeave() { requestPracticeAction("leave") }
    BackHandler { requestLeave() }
    BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
    val panelOpen = transportPanel != null || (recordings.reviewTake != null && !reviewCollapsed)
    val loopPanelOpen = (recordings.reviewTake == null && transportPanel == "loop") || (recordings.reviewTake?.mode == "LOOP" && !reviewCollapsed)
    val transportMaxHeight = if (panelOpen) minOf(maxHeight * .42f,
        (maxHeight - 97.dp - 56.dp - 48.dp - 128.dp).coerceAtLeast(48.dp)) else maxHeight * .42f
    Column(Modifier.fillMaxSize().padding(top = 4.dp)) {
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
        Box(Modifier.fillMaxWidth().then(if (panelOpen) Modifier else Modifier.weight(1f)).testTag("practice-workspace-viewport")) {
            FocusedWorkspace(current, images, if (panelOpen) Modifier.fillMaxWidth() else Modifier.fillMaxSize(), suppressed = panelOpen || bpmDialog, onReveal = {
                focusManager.clearFocus(force = true); keyboard?.hide()
                transportPanel = null
                reviewCollapsed = recordings.reviewTake != null
            })
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = .22f))
        Surface(color = androidx.compose.ui.graphics.lerp(PracticeCream, PracticeGreen, .085f), modifier = Modifier.fillMaxWidth().then(if (panelOpen) Modifier.weight(1f) else Modifier).testTag("practice-transport-surface")) {
        Column(if (panelOpen) Modifier.fillMaxSize() else Modifier.fillMaxWidth()) {
            if (recordings.reviewTake == null && transportPanel == "notes") BlockNotesPanel(noteHistory.filter { it.blockId == current.sourceBlockId }, noteDraft,
                { notes.edit(current.id, it) }, notes.busy, notes.message,
                { notes.save(current.sourceBlockId, bundle.session.id, current.id) },
                { focusManager.clearFocus(force = true); keyboard?.hide(); transportPanel = null }, notes,
                Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp))
            if (loopPanelOpen) BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp)) {
                val loopHeight = maxHeight
                PracticeScrollPanel(Modifier.fillMaxSize(), tag = "practice-loop-setup-content") {
                    PhraseSetupDialog(current.sourceBlockId, recordings, vm.workingBpm, vm::stopClick,
                        { loopPermission.launch(android.Manifest.permission.RECORD_AUDIO) }, { transportPanel = null },
                        inline = true, minimumHeight = loopHeight)
                }
            }
            if (recordings.reviewTake == null && (transportPanel == "free-list" || transportPanel == "loop-list")) Box(Modifier.fillMaxWidth()
                .weight(1f).padding(horizontal = 16.dp).testTag("practice-selection-panel")) {
                val loop = transportPanel == "loop-list"
                PracticeTakeSelection(if (loop) loopTakes else freeTakes, if (loop) loopId else freeId, loop,
                    onSelect = { if (loop) loopId = it.id else freeId = it.id; transportPanel = null }, onClose = { transportPanel = null })
            }
        if (recordings.reviewTake == null && transportPanel == "metronome") PracticeScrollPanel(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal=16.dp),tag="practice-metronome-panel") {
            MetronomeOptions(vm.metronomeConfig,vm.metronomePulse,vm.workingBpm,vm::configureMetronome,vm::changeWorkingBpm,!vm.busy,compact=true)
        }
        if (recordings.reviewTake != null && recordings.reviewTake?.mode != "LOOP" && !reviewCollapsed) PracticeScrollPanel(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp), tag = "practice-review-content") {
            TakeReviewControls(recordings, vm::stopClick)
        }
        PracticeScrollPanel(Modifier.fillMaxWidth().heightIn(max = transportMaxHeight).padding(horizontal = 16.dp),
            state = transportScroll, tag = "practice-transport") {
            PracticeMetronome(vm, expanded = transportPanel == "metronome", blocked = recordings.phrasing,
                onExpand = { transportPanel = if (transportPanel == "metronome") null else "metronome" },
                onEditingChanged = { open -> bpmDialog = open; if (open) transportPanel = null })
            CompactRecordingControls(current.sourceBlockId, takes, freeTake, loopTake, recordings, vm::stopClick,
                { reviewCollapsed = false; transportPanel = if (transportPanel == "loop") null else "loop" },
                { loop -> val panel = if (loop) "loop-list" else "free-list"; transportPanel = if (transportPanel == panel) null else panel })
            practiceTransportMessage(recordingMessage)?.takeUnless { loopPanelOpen && it == "Temporary take discarded. Saved recordings were kept." }?.let { Text(it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
        }
        if (recordings.reviewTake != null && reviewCollapsed) Row(Modifier.fillMaxWidth().padding(horizontal=16.dp),
            verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
            TextButton(onClick={ reviewCollapsed=false; transportPanel=if(recordings.reviewTake?.mode=="LOOP") "loop" else null }, modifier=Modifier.weight(1f).heightIn(min=48.dp)
                .testTag("practice-review-expand")) { Text("Review Take · Not saved") }
            Button(onClick=recordings::saveReview,enabled=!recordings.busy,
                modifier=Modifier.heightIn(min=48.dp).testTag("review-save")) { Text("Save") }
        }
        if (recordings.recording && !panelOpen) {
            val status: @Composable (Modifier) -> Unit = { modifier ->
                Text(if (recordings.countInBeat > 0) "Count-in · ${recordings.countInBeat} of ${recordings.countInTotal}" else "Recording · " + practiceTime(recordings.elapsedMs),
                    color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, modifier = modifier)
            }
            val stop: @Composable (Modifier) -> Unit = { modifier ->
                Button(onClick = { recordings.finishCapture() }, enabled = recordings.ready && !recordings.busy,
                    modifier = modifier.heightIn(min = 48.dp).testTag("practice-recording-stop")) { Text(if (recordings.phrasing) "Cancel phrase" else "Stop and review take") }
            }
            if (androidx.compose.ui.platform.LocalDensity.current.fontScale >= 1.5f) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    status(Modifier.fillMaxWidth()); stop(Modifier.fillMaxWidth())
                }
            } else Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                status(Modifier.weight(1f)); stop(Modifier)
            }
        }
        if (awaiting && !loopPanelOpen) Text("Time reached. Continue for overtime or complete this Block.",
            style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 16.dp).semantics { liveRegion = LiveRegionMode.Polite })
        FlowRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp).testTag("practice-bottom-actions"),
            maxItemsInEachRow = if (androidx.compose.ui.platform.LocalDensity.current.fontScale >= 1.5f) 2 else 4,
            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(onClick = { if (vm.running) vm.pause() else vm.resume(overtime = awaiting) }, enabled = !vm.busy,
                contentPadding = PaddingValues(horizontal = 4.dp), modifier = Modifier.weight(1.35f).heightIn(min = 48.dp).semantics { stateDescription = if (vm.running) "Practicing" else "Paused" }) {
                Text(if (awaiting) "Continue" else if (vm.running) "Pause" else if (current.activeMs == 0L) "Start" else "Resume")
            }
            Button(onClick = { requestPracticeAction("COMPLETED") }, enabled = !vm.busy && !notes.busy && !audioBusy,
                contentPadding = PaddingValues(horizontal = 4.dp), modifier = Modifier.weight(1.35f).heightIn(min = 48.dp)) { Text("Complete") }
            OutlinedButton(onClick = { transportPanel = if (recordings.reviewTake == null && transportPanel == "notes") null else "notes" },
                contentPadding = PaddingValues(horizontal = 4.dp), modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                    .testTag("practice-notes-toggle").semantics { stateDescription = if (recordings.reviewTake == null && transportPanel == "notes") "Expanded" else "Collapsed" }) { Text("Notes") }
            OutlinedButton(onClick = { requestPracticeAction("SKIPPED") }, enabled = !vm.busy && !notes.busy && !audioBusy,
                contentPadding = PaddingValues(horizontal = 4.dp), modifier = Modifier.weight(.7f).heightIn(min = 48.dp)) { Text("Skip") }
        }
        }
        }
    }
    }
    if (pendingNoteAction != null) AlertDialog(onDismissRequest = { pendingNoteAction = null }, title = { Text("Unsaved note text") },
        text = { Text("Return to Notes to save your new note or edits, or discard unsaved text before continuing. Saved notes stay kept.") },
        confirmButton = { TextButton(onClick = {
            val action = pendingNoteAction!!; pendingNoteAction = null; notes.edit(current.id, ""); notes.discardEdits(current.sourceBlockId); performPracticeAction(action)
        }) { Text("Discard draft and continue") } },
        dismissButton = { TextButton(onClick = { pendingNoteAction = null; transportPanel = "notes" }) { Text("Keep editing") } })
    if (leaving) AlertDialog(onDismissRequest = { if (!vm.busy) leaving = false }, title = { Text("Leave current session?") }, text = {
        Text(if (audioBusy) "Stop your recording, then Save or Discard the temporary take, or cancel an incomplete phrase, before leaving. Your current practice note is kept here."
            else "Your current session and note will be saved for later, and practice will pause.")
    }, confirmButton = { TextButton(onClick = { vm.saveForLater(bundle.session.id, current.id, note, preserveNote = true) }, enabled = !vm.busy && !audioBusy) { Text("Back to Main") } },
        dismissButton = { TextButton(onClick = { leaving = false }, enabled = !vm.busy) { Text("Continue") } })
}

@Composable fun SessionSummary(bundle: SessionBundle?, vm: PracticeViewModel, onHome: () -> Unit, goalName: String? = null) {
    if (bundle == null) {
        Column(Modifier.safeDrawingPadding().padding(24.dp)) { Text("This session is not available.") }
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
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable private fun PracticeMetronome(vm: PracticeViewModel, expanded: Boolean, blocked: Boolean, onExpand: () -> Unit, onEditingChanged: (Boolean) -> Unit = {}) {
    var editing by rememberSaveable { mutableStateOf(false) }
    var entered by rememberSaveable { mutableStateOf("") }
    val largeText = androidx.compose.ui.platform.LocalDensity.current.fontScale >= 1.5f
    val bpmControl: @Composable (Modifier) -> Unit = { modifier ->
        TextButton(onClick = { entered = vm.workingBpm.toString(); editing = true; onEditingChanged(true) }, enabled = !vm.busy,
            contentPadding = PaddingValues(horizontal = 2.dp), modifier = modifier.heightIn(min = 48.dp).testTag("metronome-bpm").semantics { contentDescription = "Set BPM" }) {
            Column {
                Text("Metronome",style=MaterialTheme.typography.labelSmall)
                Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                    Text("${vm.workingBpm} BPM",style=MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
    val controls: @Composable () -> Unit = {
        TextButton(onClick = { vm.changeWorkingBpm((vm.workingBpm - 1).coerceAtLeast(30)) }, enabled = vm.workingBpm > 30 && !vm.busy,
            contentPadding = PaddingValues(0.dp), modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp).width(48.dp).semantics { contentDescription = "Decrease click BPM by 1" }) { Text("−1") }
        TextButton(onClick = { vm.changeWorkingBpm((vm.workingBpm + 1).coerceAtMost(300)) }, enabled = vm.workingBpm < 300 && !vm.busy,
            contentPadding = PaddingValues(0.dp), modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp).width(48.dp).semantics { contentDescription = "Increase click BPM by 1" }) { Text("+1") }
        OutlinedButton(onClick = { if (vm.clicking) vm.stopClick() else vm.startClick() }, enabled = !vm.busy && !blocked,
            colors = metronomeButtonColors(vm.metronomePulse, vm.clicking, outlined = true),
            contentPadding = PaddingValues(horizontal = 8.dp), modifier = Modifier.heightIn(min = 48.dp)
                .metronomeBeat(vm.metronomePulse, vm.clicking, "practice-metronome-pulse")) { Text(if (vm.clicking) "Stop click" else "Start click", style = MaterialTheme.typography.bodyMedium) }
    }
        val expand: @Composable () -> Unit = {
            TextButton(onClick=onExpand,contentPadding=PaddingValues(0.dp),modifier=Modifier.sizeIn(minWidth=48.dp,minHeight=48.dp)
                .testTag("practice-metronome-expand").semantics { contentDescription=if(expanded) "Collapse metronome" else "Expand metronome" }) {
                Column(horizontalAlignment=androidx.compose.ui.Alignment.CenterHorizontally) {
                    if(vm.clicking) Text((if(vm.metronomePulse.sequence>0) vm.metronomePulse.config else vm.metronomeConfig).meter.label,
                        style=MaterialTheme.typography.labelSmall,modifier=Modifier.testTag("practice-metronome-meter"))
                    Text(if(expanded) "▴" else "▾")
                }
            }
        }
    if (largeText) FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        bpmControl(Modifier.fillMaxWidth())
        controls(); expand()
    } else Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        bpmControl(Modifier.weight(1f))
        controls(); expand()
    }
    vm.clickError?.let { Text(it, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
    if (editing) AlertDialog(onDismissRequest = { editing = false; onEditingChanged(false) }, title = { Text("Working BPM") }, text = {
        OutlinedTextField(entered, { entered = it }, label = { Text("BPM (30–300)") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), isError = parseWorkingBpm(entered) == null,
            supportingText = { Text("Enter a whole number from 30 to 300.") })
    }, confirmButton = { TextButton(onClick = { parseWorkingBpm(entered)?.let { vm.changeWorkingBpm(it); editing = false; onEditingChanged(false) } },
        enabled = parseWorkingBpm(entered) != null && !vm.busy) { Text("Apply BPM") } },
        dismissButton = { TextButton(onClick = { editing = false; onEditingChanged(false) }) { Text("Cancel") } })
}

@Composable private fun ActionButton(label: String, click: () -> Unit, enabled: Boolean = true) {
    Button(onClick = click, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text(label) }
}
