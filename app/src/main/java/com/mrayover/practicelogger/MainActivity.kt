package com.mrayover.practicelogger

import android.os.Bundle
import android.os.Build
import android.app.Activity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(PracticeGreen.toArgb()),
            navigationBarStyle = SystemBarStyle.light(PracticeCream.toArgb(), android.graphics.Color.BLACK))
        setContent { PracticeTheme { CatalogApp() } }
    }
}

@Suppress("DEPRECATION")
@Composable fun CatalogApp(vm: CatalogViewModel = viewModel(), practice: PracticeViewModel = viewModel(), recordings: RecordingViewModel = viewModel(), tools: ToolsViewModel = viewModel(), routines: RoutineViewModel = viewModel(), goals: GoalViewModel = viewModel(), log: LogViewModel = viewModel(), images: ImageReferenceViewModel = viewModel()) {
    val goalRows by goals.goals.collectAsStateWithLifecycle()
    val goalLinks by goals.links.collectAsStateWithLifecycle()
    var goalId by rememberSaveable { mutableStateOf<String?>(null) }
    var goalEditorId by rememberSaveable { mutableStateOf<String?>(null) }
    val sessions by practice.catalog.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val view = LocalView.current
    val focus = androidx.compose.ui.platform.LocalFocusManager.current
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    var freeRecordingStart by rememberSaveable { mutableStateOf(false) }
    var recordingsBlockId by rememberSaveable { mutableStateOf<String?>(null) }
    DisposableEffect(lifecycle, recordings) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) recordings.setForeground(true)
            if (event == Lifecycle.Event.ON_PAUSE) recordings.setForeground(false)
        }
        lifecycle.addObserver(observer)
        recordings.setForeground(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
        onDispose { lifecycle.removeObserver(observer); recordings.setForeground(false) }
    }
    DisposableEffect(lifecycle, practice) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) practice.setForeground(true)
            if (event == Lifecycle.Event.ON_PAUSE) practice.setForeground(false)
        }
        lifecycle.addObserver(observer)
        practice.setForeground(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
        onDispose { lifecycle.removeObserver(observer); practice.setForeground(false) }
    }
    DisposableEffect(practice.running, recordings.recording, tools.clicking, tools.listening, tools.stopwatchRunning, tools.countdownRunning, view) { view.keepScreenOn = practice.running || recordings.recording || tools.clicking || tools.listening || tools.stopwatchRunning || tools.countdownRunning; onDispose { view.keepScreenOn = false } }
    val catalog by vm.catalog.collectAsStateWithLifecycle()
    var page by rememberSaveable { mutableStateOf("home") }
    SideEffect {
        (view.context as? Activity)?.window?.let { window ->
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            if (Build.VERSION.SDK_INT < 35) window.statusBarColor = PracticeGreen.toArgb()
        }
    }
    var editorReturn by rememberSaveable { mutableStateOf("library") }
    var summaryId by rememberSaveable { mutableStateOf<String?>(null) }
    var editorId by rememberSaveable { mutableStateOf<String?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var archived by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(vm.destinationArchived) {
        vm.destinationArchived?.let { destination ->
            focus.clearFocus(force = true); keyboard?.hide()
            archived = destination; query = ""; page = if (vm.deleted) "library" else editorReturn
            if (!vm.deleted && editorReturn == "builder") vm.savedBlockId?.let { practice.addBlock(it) }
            vm.navigationHandled()
        }
    }
    LaunchedEffect(practice.destination) {
        practice.destination?.let { destination ->
            if (destination == "summary") summaryId = practice.completedId
            page = destination; practice.navigationHandled()
        }
    }
    LaunchedEffect(routines.editorSaved) {
        if (routines.editorSaved) { focus.clearFocus(force = true); keyboard?.hide(); page = "routines"; routines.navigationHandled() }
    }
    LaunchedEffect(goals.savedId) {
        goals.savedId?.let { id ->
            focus.clearFocus(force = true); keyboard?.hide(); goalId = id; page = "goal-detail"; goals.navigationHandled()
        }
    }
    fun goalName(sessionId: String?): String? = goalLinks.find { it.sessionId == sessionId }?.let { link -> goalRows.find { it.goal.id == link.goalId }?.goal?.title }
    fun openRoutine(id: String?) { if (routines.beginEditor(id)) page = "routine-editor" }
    fun openEditor(id: String?, returnTo: String = "library") { editorId = id; editorReturn = returnTo; page = "editor" }
    BackHandler(page == "library") { page = "home" }
    Surface(Modifier.fillMaxSize()) {
      Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))) {
        PracticeBanner()
        Box(Modifier.weight(1f).fillMaxWidth().consumeWindowInsets(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))) {
        if (catalog.error != null || sessions.error != null) {
            Column(Modifier.safeDrawingPadding().padding(24.dp)) { Text(catalog.error ?: sessions.error!!) }
        } else if (!catalog.ready || !sessions.ready || !goals.ready) {
            Box(Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp)) { Text("Loading Blocks…") }
        } else when (page) {
            "home" -> PracticeHome(onPractice = { page = "builder" }, onBlocks = { page = "library" },
                onRoutines = { page = "routines" }, onTools = { page = "tools" }, onRecording = { page = "recording" }, goals = goalRows,
                onLog = { log.open(); page = "log" }, onGoals = { page = "goals" }, onGoal = { goalId = it; page = "goal-detail" })
            "log" -> LogScreen(log, onHome = { page = "home" })
            "goals" -> GoalManagement(goalRows, goals, onHome = { page = "home" },
                onNew = { goalEditorId = null; page = "goal-editor" }, onGoal = { goalId = it; page = "goal-detail" })
            "goal-detail" -> {
                val goalTimes by goals.times.collectAsStateWithLifecycle()
                GoalDetail(goalRows.find { it.goal.id == goalId }, catalog.blocks, goalTimes.find { it.goalId == goalId }?.activeMs ?: 0L,
                goals, practice, onGoals = { page = "goals" }, onEdit = { goalEditorId = goalId; page = "goal-editor" },
                onBlocks = { page = "goal-blocks" }, onBlock = { openEditor(it, "goal-detail") })
            }
            "goal-editor" -> key(goalEditorId) { GoalEditor(goalRows.find { it.goal.id == goalEditorId }?.goal, goals,
                onCancel = { page = if (goalEditorId == null) "goals" else "goal-detail" }) }
            "goal-blocks" -> GoalBlocks(goalRows.find { it.goal.id == goalId }, catalog.blocks, goals,
                onBack = { page = "goal-detail" }, onLibrary = { page = "library" })
            "routines" -> PracticeOverview(catalog, sessions, onPractice = { page = "builder" },
                onBackup = { page = "backup" }, onLibrary = { page = "library" }, onNew = { openEditor(null) },
                onSummary = { summaryId = it; page = "summary" }, onHome = { page = "home" }, routines = routines, practice = practice, onNewRoutine = { openRoutine(null) }, onEditRoutine = { openRoutine(it) })
            "routine-editor" -> RoutineEditor(catalog.blocks, routines, onRoutines = { page = "routines" })
            "tools" -> ToolsHome(onHome = { page = "home" }, onBackup = { page = "backup" }, onTool = { page = it })
            "metronome", "tuner", "timer" -> ToolScreen(page, tools, onTools = { page = "tools" })
            "recording" -> RecordingHome(catalog.blocks, onHome = { page = "home" }, onBlock = { recordingsBlockId = it }, onFree = { freeRecordingStart = it; page = "free-recording" })
            "free-recording" -> BlockRecordings(null, "Free recording", recordings, practice, null, 120,
                onBeforePlayback = practice::stopClick, onDismiss = { page = "recording" }, startOnOpen = freeRecordingStart)
            "backup" -> BackupScreen(onHome = { page = "home" })
            "builder" -> SessionBuilder(catalog.blocks, sessions.draft, sessions, practice, routines, onHome = { page = "home" }, onCreate = { openEditor(null, "builder") })
            "focus" -> FocusedPractice(sessions.active, practice, images, onHome = { page = "home" }, onRecordings = { recordingsBlockId = it }, recordingMessage = recordings.message, goalName = goalName(sessions.active?.session?.id))
            "summary" -> SessionSummary(sessions.bundles.find { it.session.id == summaryId }, practice, onHome = { page = "home" }, goalName = goalName(summaryId))
            "library" -> {
                val libraryScroll = rememberLazyListState()
                var searchFocused by remember { mutableStateOf(false) }
                LaunchedEffect(query, archived, searchFocused) {
                    libraryScroll.scrollToItem(if (searchFocused || query.isNotBlank()) 3 else 0)
                }
                val matches = matchingBlocks(catalog.blocks, query, archived)
                LazyColumn(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 24.dp).testTag("block-library-list"), state = libraryScroll,
                    verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                    item { BackToHomeButton(onClick = { page = "home" }) }
                    item { Text("Block Library", style = MaterialTheme.typography.headlineMedium) }
                    item { Button(onClick = { openEditor(null) }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("New Block") } }
                    item { OutlinedTextField(query, { query = it }, label = { Text("Search Blocks") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth().onFocusChanged { searchFocused = it.isFocused }) }
                    item { Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FilterChip(selected = !archived, onClick = { archived = false }, label = { Text("Active") })
                        FilterChip(selected = archived, onClick = { archived = true }, label = { Text("Archived") })
                    } }
                    if (matches.isEmpty()) item { Text(if (query.isNotBlank()) "No matching Blocks." else if (archived) "No archived Blocks." else "No Blocks yet. Create your first Block.") }
                    items(matches, key = { it.id }) { block ->
                        Column(Modifier.fillMaxWidth().clickable(onClickLabel = "Edit " + block.name) { openEditor(block.id) }.heightIn(min = 64.dp).padding(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(block.name, style = MaterialTheme.typography.titleLarge)
                            Text(block.action)
                            Text(block.durationMinutes.toString() + " min" + (block.tempo?.let { " · " + it + " BPM reference" } ?: ""), style = MaterialTheme.typography.bodyMedium)
                        }
                        HorizontalDivider()
                    }
                }
            }
            "editor" -> {
                val block = editorId?.let { id -> catalog.blocks.find { it.id == id } }
                if (editorId != null && block == null) {
                    Column(Modifier.safeDrawingPadding().padding(24.dp)) {
                        Text("This Block is no longer available.")
                        Button(onClick = { page = "library" }) { Text("Back to Library") }
                    }
                } else key(editorId) {
                    BlockEditor(block, vm.busy || images.busy, images, onDelete = { block?.let { vm.reviewDeletion(it.id) } }, onCancel = { page = editorReturn },
                        onSave = { draft -> vm.save(editorId, draft, block?.archived ?: false) },
                        onArchive = { value -> block?.let { vm.archive(it.id, value) } }, onRecordings = { block?.let { recordingsBlockId = it.id } }, recordingMessage = recordings.message)
                }
            }
        }
        }
      }
        recordingsBlockId?.let { id ->
            catalog.blocks.find { it.id == id }?.let { block ->
                BlockRecordings(id, block.name, recordings, practice, if (page == "focus") sessions.active else null,
                    initialBpm = if (page == "focus") practice.workingBpm else block.tempo?.takeIf { it in 30..300 } ?: 120,
                    onBeforePlayback = practice::stopClick, onDismiss = { recordingsBlockId = null })
            }
        }
        practice.error?.let { message ->
            AlertDialog(onDismissRequest = practice::dismissError, title = { Text("Practice paused") }, text = { Text(message) }, confirmButton = { TextButton(onClick = practice::dismissError) { Text("OK") } })
        }
        goals.error?.let { message ->
            AlertDialog(onDismissRequest = goals::dismissError, title = { Text("Goal could not be updated") }, text = { Text(message) },
                confirmButton = { TextButton(onClick = goals::dismissError) { Text("OK") } })
        }
        routines.error?.let { message ->
            AlertDialog(onDismissRequest = routines::dismissError, title = { Text("Routine could not be updated") }, text = { Text(message) },
                confirmButton = { TextButton(onClick = routines::dismissError) { Text("OK") } })
        }
        vm.deletionReview?.let { review ->
            val eligible = review.dependencies.canDelete
            AlertDialog(onDismissRequest = vm::dismissDeletion,
                title = { Text(if (eligible) "Delete \"${review.block.name}\" permanently?" else "This Block cannot be deleted yet.") },
                text = { Text(if (eligible) "This removes the saved Block from your library and cannot be undone. Unsaved edits will be discarded."
                    else review.dependencies.explanation(), modifier = Modifier.verticalScroll(rememberScrollState())) },
                confirmButton = {
                    TextButton(onClick = if (eligible) vm::confirmDeletion else vm::dismissDeletion, enabled = !vm.busy,
                        modifier = Modifier.testTag("block-delete-confirm")) { Text(if (eligible) "Delete Permanently" else "OK") }
                },
                dismissButton = { if (eligible) TextButton(onClick = vm::dismissDeletion, enabled = !vm.busy) { Text("Cancel") } })
        }
        vm.error?.let { message ->
            AlertDialog(onDismissRequest = vm::dismissError, title = { Text("Changes not saved") }, text = { Text(message) }, confirmButton = { TextButton(onClick = vm::dismissError) { Text("OK") } })
        }
    }
}

@Composable private fun BlockEditor(block: Block?, busy: Boolean, images: ImageReferenceViewModel, onDelete: () -> Unit, onCancel: () -> Unit, onSave: (BlockDraft) -> Unit, onArchive: (Boolean) -> Unit, onRecordings: () -> Unit, recordingMessage: String?) {
    val initial = remember { block?.let(BlockDraft::from) ?: BlockDraft() }
    var name by rememberSaveable { mutableStateOf(initial.name) }
    var action by rememberSaveable { mutableStateOf(initial.action) }
    var duration by rememberSaveable { mutableStateOf(initial.duration) }
    var tempo by rememberSaveable { mutableStateOf(initial.tempo) }
    var notes by rememberSaveable { mutableStateOf(initial.notes) }
    var submitted by rememberSaveable { mutableStateOf(false) }
    var discard by rememberSaveable { mutableStateOf(false) }
    var archiveConfirm by rememberSaveable { mutableStateOf(false) }
    val draft = BlockDraft(name, action, duration, tempo, notes)
    val errors = if (submitted) draft.errors() else emptyMap()
    fun leave() { if (!busy) { if (draft != initial) discard = true else onCancel() } }
    BackHandler { leave() }
    Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(if (block == null) "New Block" else "Edit Block", style = MaterialTheme.typography.headlineMedium)
        if (block?.archived == true) Text("Archived — restore to return this Block to your active library.")
        if (block != null) OutlinedButton(onClick = onRecordings, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Recordings") }
        if (block != null) recordingMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        BlockImageEditor(block?.id, images, busy)
        EditorField("Name", name, { name = it }, errors["name"], busy, singleLine = true)
        EditorField("Action", action, { action = it }, errors["action"], busy)
        EditorField("Default duration (minutes)", duration, { duration = it }, errors["duration"], busy, singleLine = true, numeric = true)
        EditorField("Tempo reference (BPM, optional)", tempo, { tempo = it }, errors["tempo"], busy, singleLine = true, numeric = true)
        EditorField("Notes / source (optional)", notes, { notes = it }, null, busy)
        Button(onClick = { submitted = true; if (draft.errors().isEmpty()) onSave(draft) }, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text(if (busy) "Saving…" else "Save") }
        OutlinedButton(onClick = ::leave, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Cancel") }
        if (block != null) OutlinedButton(onClick = { archiveConfirm = true }, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text(if (block.archived) "Restore Block" else "Archive Block") }
        if (block != null) OutlinedButton(onClick = onDelete, enabled = !busy,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("block-delete-action")) { Text("Delete Permanently") }
    }
    if (discard) AlertDialog(onDismissRequest = { discard = false }, title = { Text("Discard unsaved changes?") }, text = { Text("Your saved Block will stay unchanged.") },
        confirmButton = { TextButton(onClick = { discard = false; onCancel() }) { Text("Discard") } },
        dismissButton = { TextButton(onClick = { discard = false }) { Text("Keep editing") } })
    if (archiveConfirm && block != null) AlertDialog(onDismissRequest = { archiveConfirm = false },
        title = { Text(if (block.archived) "Restore this Block?" else "Archive this Block?") },
        text = { Text(if (draft != initial) "Unsaved edits will be discarded. The saved Block is retained and can be restored; no history is deleted." else "The saved Block is retained and can be restored; no history is deleted.") },
        confirmButton = { TextButton(onClick = { archiveConfirm = false; onArchive(!block.archived) }) { Text(if (block.archived) "Restore" else "Archive") } },
        dismissButton = { TextButton(onClick = { archiveConfirm = false }) { Text("Cancel") } })
}

@Composable private fun EditorField(label: String, value: String, changed: (String) -> Unit, error: String?, busy: Boolean, singleLine: Boolean = false, numeric: Boolean = false) {
    OutlinedTextField(value, changed, label = { Text(label) }, enabled = !busy, isError = error != null,
        supportingText = { if (error != null) Text(error) }, singleLine = singleLine, minLines = if (singleLine) 1 else 3,
        keyboardOptions = KeyboardOptions(keyboardType = if (numeric) KeyboardType.Number else KeyboardType.Text),
        modifier = Modifier.fillMaxWidth().testTag(label))
}
