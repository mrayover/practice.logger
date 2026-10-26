package com.mrayover.practicelogger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
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
        setContent { PracticeTheme { CatalogApp() } }
    }
}

@Composable fun CatalogApp(vm: CatalogViewModel = viewModel(), practice: PracticeViewModel = viewModel()) {
    val sessions by practice.catalog.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val view = LocalView.current
    DisposableEffect(lifecycle, practice) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) practice.setForeground(true)
            if (event == Lifecycle.Event.ON_PAUSE) practice.setForeground(false)
        }
        lifecycle.addObserver(observer)
        practice.setForeground(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
        onDispose { lifecycle.removeObserver(observer); practice.setForeground(false) }
    }
    DisposableEffect(practice.running, view) { view.keepScreenOn = practice.running; onDispose { view.keepScreenOn = false } }
    val catalog by vm.catalog.collectAsStateWithLifecycle()
    var page by rememberSaveable { mutableStateOf("home") }
    var editorReturn by rememberSaveable { mutableStateOf("library") }
    var summaryId by rememberSaveable { mutableStateOf<String?>(null) }
    var editorId by rememberSaveable { mutableStateOf<String?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var archived by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(vm.destinationArchived) {
        vm.destinationArchived?.let { destination ->
            archived = destination; query = ""; page = editorReturn
            if (editorReturn == "builder") vm.savedBlockId?.let { practice.addBlock(it) }
            vm.navigationHandled()
        }
    }
    LaunchedEffect(practice.destination) {
        practice.destination?.let { destination ->
            if (destination == "summary") summaryId = practice.completedId
            page = destination; practice.navigationHandled()
        }
    }
    fun openEditor(id: String?, returnTo: String = "library") { editorId = id; editorReturn = returnTo; page = "editor" }
    BackHandler(page == "library") { page = "home" }
    Surface(Modifier.fillMaxSize()) {
        if (catalog.error != null || sessions.error != null) {
            Column(Modifier.safeDrawingPadding().padding(24.dp)) { Text("practice.logger", style = MaterialTheme.typography.headlineLarge); Text(catalog.error ?: sessions.error!!) }
        } else if (!catalog.ready || !sessions.ready) {
            Box(Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp)) { Text("Loading Blocks…") }
        } else when (page) {
            "home" -> PracticeHome(catalog, sessions, onPractice = { page = "builder" }, onContinue = { page = "focus" },
                onLibrary = { page = "library" }, onNew = { openEditor(null) }, onSummary = { summaryId = it; page = "summary" })
            "builder" -> SessionBuilder(catalog.blocks, sessions.draft, practice, onHome = { page = "home" }, onCreate = { openEditor(null, "builder") })
            "focus" -> FocusedPractice(sessions.active, practice, onHome = { page = "home" })
            "summary" -> SessionSummary(sessions.bundles.find { it.session.id == summaryId }, practice, onHome = { page = "home" })
            "library" -> Column(Modifier.safeDrawingPadding().padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = { page = "home" }) { Text("Back to Home") }
                Text("Block Library", style = MaterialTheme.typography.headlineMedium)
                Button(onClick = { openEditor(null) }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("New Block") }
                OutlinedTextField(query, { query = it }, label = { Text("Search Blocks") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilterChip(selected = !archived, onClick = { archived = false }, label = { Text("Active") })
                    FilterChip(selected = archived, onClick = { archived = true }, label = { Text("Archived") })
                }
                val matches = matchingBlocks(catalog.blocks, query, archived)
                if (matches.isEmpty()) Text(if (query.isNotBlank()) "No matching Blocks." else if (archived) "No archived Blocks." else "No Blocks yet. Create your first Block.")
                LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 24.dp)) {
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
                    BlockEditor(block, vm.busy, onCancel = { page = editorReturn },
                        onSave = { draft -> vm.save(editorId, draft, block?.archived ?: false) },
                        onArchive = { value -> block?.let { vm.archive(it.id, value) } })
                }
            }
        }
        practice.error?.let { message ->
            AlertDialog(onDismissRequest = practice::dismissError, title = { Text("Practice paused") }, text = { Text(message) }, confirmButton = { TextButton(onClick = practice::dismissError) { Text("OK") } })
        }
        vm.error?.let { message ->
            AlertDialog(onDismissRequest = vm::dismissError, title = { Text("Changes not saved") }, text = { Text(message) }, confirmButton = { TextButton(onClick = vm::dismissError) { Text("OK") } })
        }
    }
}

@Composable private fun BlockEditor(block: Block?, busy: Boolean, onCancel: () -> Unit, onSave: (BlockDraft) -> Unit, onArchive: (Boolean) -> Unit) {
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
        EditorField("Name", name, { name = it }, errors["name"], busy, singleLine = true)
        EditorField("Action", action, { action = it }, errors["action"], busy)
        EditorField("Default duration (minutes)", duration, { duration = it }, errors["duration"], busy, singleLine = true, numeric = true)
        EditorField("Tempo reference (BPM, optional)", tempo, { tempo = it }, errors["tempo"], busy, singleLine = true, numeric = true)
        EditorField("Notes / source (optional)", notes, { notes = it }, null, busy)
        Button(onClick = { submitted = true; if (draft.errors().isEmpty()) onSave(draft) }, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text(if (busy) "Saving…" else "Save") }
        OutlinedButton(onClick = ::leave, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Cancel") }
        if (block != null) OutlinedButton(onClick = { archiveConfirm = true }, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text(if (block.archived) "Restore Block" else "Archive Block") }
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
