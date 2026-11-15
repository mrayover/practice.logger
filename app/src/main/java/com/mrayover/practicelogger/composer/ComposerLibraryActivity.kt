package com.mrayover.practicelogger.composer

import android.app.Application
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mrayover.practicelogger.*
import com.mrayover.practicelogger.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.DateFormat
import java.util.Date

internal fun Application.compositionStore() = CompositionStore(File(filesDir, "composer/projects"))

class ComposerLibraryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val choice = ThemePreferences(this).choice.value
        setTheme(when (choice) {
            ThemeChoice.LIGHT -> R.style.Theme_PracticeLogger_Light
            ThemeChoice.SMOOTH -> R.style.Theme_PracticeLogger_Smooth
            ThemeChoice.SUNNY -> R.style.Theme_PracticeLogger_Sunny
            ThemeChoice.ORCHID -> R.style.Theme_PracticeLogger_Orchid
            ThemeChoice.SUBMARINE -> R.style.Theme_PracticeLogger_Submarine
            ThemeChoice.COSMIC -> R.style.Theme_PracticeLogger_Cosmic
            ThemeChoice.SYSTEM -> if (resources.configuration.uiMode and 0x30 == 0x20) R.style.Theme_PracticeLogger_Dark else R.style.Theme_PracticeLogger_Light
            else -> R.style.Theme_PracticeLogger_Dark
        })
        super.onCreate(savedInstanceState)
        setContent {
            PracticeTheme(choice) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    ComposerLibrary(onBack = { finish() })
                }
            }
        }
    }
}

class ComposerLibraryViewModel(app: Application, private val state: SavedStateHandle) : AndroidViewModel(app) {
    private val store = app.compositionStore()
    var listing by mutableStateOf(CompositionListing(emptyList(), 0)); private set
    var busy by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    var openId by mutableStateOf<String?>(null); private set
    var transitioning by mutableStateOf(state.get<String>("pendingOpen") != null); private set
    init {
        val pending = state.get<String>("pendingCreate")
        val open = state.get<String>("pendingOpen")
        if (pending != null) createEncoded(pending)
        else if (open != null) open(open)
        else refresh()
    }
    private fun work(action: suspend () -> Unit) {
        if (busy) return
        busy = true; error = null
        viewModelScope.launch {
            try { action() }
            catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (failure: Exception) {
                error = failure.message ?: "Composer storage is unavailable. Please retry."
                transitioning = false
            } finally { busy = false }
        }
    }
    fun refresh() = work { listing = withContext(Dispatchers.IO) { store.list() } }
    fun create(title: String, tempo: Int, numerator: Int, denominator: Int) {
        if (busy || transitioning) return
        val project = CompositionProject.empty(title, tempo, numerator, denominator)
        val encoded = CompositionCodec.encode(project)
        state["pendingCreate"] = encoded
        createEncoded(encoded)
    }
    private fun createEncoded(encoded: String) = work {
        transitioning = true
        val project = withContext(Dispatchers.IO) { store.create(CompositionCodec.decode(encoded)) }
        state["pendingOpen"] = project.id
        state["pendingCreate"] = null
        openId = project.id
    }
    fun open(id: String) {
        if (busy) return
        state["pendingOpen"] = id
        work {
            transitioning = true
            withContext(Dispatchers.IO) { store.read(id) }
            openId = id
        }
    }
    fun returned() {
        openId = null; transitioning = false
        state["pendingOpen"] = null; state["pendingCreate"] = null
        refresh()
    }
    fun rename(project: CompositionProject, title: String) = work {
        withContext(Dispatchers.IO) { store.rename(project, title) }
        listing = withContext(Dispatchers.IO) { store.list() }
    }
    fun delete(project: CompositionProject) = work {
        withContext(Dispatchers.IO) { store.delete(project) }
        listing = withContext(Dispatchers.IO) { store.list() }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable private fun ComposerLibrary(onBack: () -> Unit, vm: ComposerLibraryViewModel = viewModel()) {
    var setup by rememberSaveable { mutableStateOf(false) }
    var renameId by rememberSaveable { mutableStateOf<String?>(null) }
    var deleteId by rememberSaveable { mutableStateOf<String?>(null) }
    var renameTitle by rememberSaveable { mutableStateOf("") }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { vm.returned() }
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(vm.openId) {
        vm.openId?.let { id ->
            // Let the library draw black before the separate Activity rotates the display.
            withFrameNanos { }; withFrameNanos { }
            launcher.launch(Intent(context, ComposerActivity::class.java).putExtra(ComposerActivity.PROJECT_ID, id))
        }
    }
    BackHandler { if (!vm.busy && !vm.transitioning) onBack() }
    if (vm.transitioning) { BrandedTransition(); return }
    LazyColumn(Modifier.fillMaxSize().safeDrawingPadding().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { TextButton(onClick = onBack, enabled = !vm.busy) { Text("Back to Tools") } }
        item { PageHeading("compose.logger", style = MaterialTheme.typography.headlineMedium) }
        item { Button(onClick = { setup = true }, enabled = !vm.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Create New Composition") } }
        item { Text("Development build: compositions are stored only on this device. Practice ZIP and JSON backups do not include them yet. Uninstalling or clearing app data removes them.", style = MaterialTheme.typography.bodySmall) }
        vm.error?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error); TextButton(onClick = vm::refresh, enabled = !vm.busy) { Text("Refresh compositions") } } }
        if (vm.listing.unreadableCount > 0) item { Text("${vm.listing.unreadableCount} unreadable or newer-version composition file(s) preserved. They cannot be opened by this build.", color = MaterialTheme.colorScheme.error) }
        if (vm.listing.projects.isEmpty() && !vm.busy) item { Text("No saved compositions. Create an empty project to begin.") }
        items(vm.listing.projects, key = { it.id }) { project ->
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(project.title, style = MaterialTheme.typography.titleMedium)
                    Text("Modified ${DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(project.modifiedAt))}", style = MaterialTheme.typography.bodySmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { vm.open(project.id) }, enabled = !vm.busy) { Text("Open") }
                        TextButton(onClick = { renameId = project.id; renameTitle = project.title }, enabled = !vm.busy) { Text("Rename") }
                        TextButton(onClick = { deleteId = project.id }, enabled = !vm.busy) { Text("Delete") }
                    }
                }
            }
        }
    }
    if (setup) ProjectSettingsDialog("New composition", "", 120, 4, 4, "Create", onDismiss = { setup = false }) { title, tempo, n, d ->
        setup = false; vm.create(title, tempo, n, d)
    }
    renameId?.let { id ->
        val project = vm.listing.projects.find { it.id == id }
        if (project != null) AlertDialog(onDismissRequest = { renameId = null }, title = { Text("Rename composition") },
            text = { OutlinedTextField(renameTitle, { renameTitle = it }, label = { Text("Composition title") }, keyboardOptions = ProseKeyboardOptions, singleLine = true) },
            confirmButton = { TextButton(onClick = { vm.rename(project, renameTitle); renameId = null }, enabled = validTitle(renameTitle)) { Text("Rename") } },
            dismissButton = { TextButton(onClick = { renameId = null }) { Text("Cancel") } })
    }
    deleteId?.let { id ->
        val project = vm.listing.projects.find { it.id == id }
        if (project != null) AlertDialog(onDismissRequest = { deleteId = null }, title = { Text("Delete composition?") },
            text = { Text("Permanently delete “${project.title}”? Only this composition will be removed. This cannot be undone.") },
            confirmButton = { TextButton(onClick = { vm.delete(project); deleteId = null }) { Text("Delete composition") } },
            dismissButton = { TextButton(onClick = { deleteId = null }) { Text("Cancel") } })
    }
}

internal fun validTitle(title: String) = title.trim().let { it.isNotEmpty() && it.length <= 120 && it.none(Char::isISOControl) }

@Composable internal fun ProjectSettingsDialog(heading: String, initialTitle: String, initialTempo: Int,
    initialN: Int, initialD: Int, action: String, onDismiss: () -> Unit, onApply: (String, Int, Int, Int) -> Unit) {
    var title by rememberSaveable { mutableStateOf(initialTitle) }
    var tempo by rememberSaveable { mutableStateOf(initialTempo.toString()) }
    var meter by rememberSaveable { mutableStateOf("$initialN/$initialD") }
    val pair = Regex("([1-9][0-9]?)/([1-9][0-9]?)").matchEntire(meter.trim())?.destructured
    val n = pair?.component1()?.toIntOrNull(); val d = pair?.component2()?.toIntOrNull()
    val bpm = tempo.toIntOrNull()
    val valid = validTitle(title) && bpm in 30..300 && n in 1..32 && d in setOf(1, 2, 4, 8, 16, 32)
    AlertDialog(onDismissRequest = onDismiss, title = { Text(heading) }, text = {
        Column(Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(title, { title = it }, label = { Text("Composition title") }, singleLine = true, keyboardOptions = ProseKeyboardOptions)
            OutlinedTextField(tempo, { tempo = it }, label = { Text("Tempo (30–300 BPM)") }, singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number))
            OutlinedTextField(meter, { meter = it }, label = { Text("Time signature (e.g. 4/4)") }, singleLine = true)
            if (!valid) Text("Enter a title (1–120 characters), whole-number tempo and valid meter. Meter: 1–32 beats over 1, 2, 4, 8, 16 or 32.", style = MaterialTheme.typography.bodySmall)
        }
    }, confirmButton = { TextButton(onClick = { onApply(title.trim(), bpm!!, n!!, d!!) }, enabled = valid) { Text(action) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}
