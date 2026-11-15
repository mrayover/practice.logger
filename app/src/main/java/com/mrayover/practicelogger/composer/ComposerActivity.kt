package com.mrayover.practicelogger.composer

import android.app.Application
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mrayover.practicelogger.LatoTypography
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption.*

class ComposerActivity : ComponentActivity() {
    internal var brandFirstDrawMs = 0L
    internal var workspaceFirstDrawMs = 0L
    internal var auditionEngine: com.mrayover.practicelogger.PianoAudioEngine? = null
    companion object { const val PROJECT_ID = "compositionProjectId" }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawable(ColorDrawable(android.graphics.Color.BLACK))
        enableEdgeToEdge(SystemBarStyle.dark(android.graphics.Color.BLACK), SystemBarStyle.dark(android.graphics.Color.BLACK))
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(background = Color(0xFF101216), surface = Color(0xFF171A20)), typography = LatoTypography) {
                ComposerScreen(onBack = { finish() })
            }
        }
    }
}

class ComposerViewModel(app: Application, private val state: SavedStateHandle) : AndroidViewModel(app) {
    private val store = app.compositionStore()
    var project by mutableStateOf<CompositionProject?>(null); private set
    private var committed: CompositionProject? = null
    var busy by mutableStateOf(false); private set
    var loading by mutableStateOf(true); private set
    var error by mutableStateOf<String?>(null); private set
    var status by mutableStateOf(""); private set
    val dirty get() = project != committed
    private val history = ArrayDeque<CompositionProject>()
    var undoCount by mutableIntStateOf(0); private set
    private val draftLock = Mutex()
    private val draftId = state.get<String>("draftFile")?.takeIf(::validCompositionId) ?: newCompositionId().also { state["draftFile"] = it }
    private val draftFile = File(app.filesDir, "composer/drafts/$draftId.json")
    var entranceRemaining by mutableLongStateOf(state.get<Long>("entranceRemaining") ?: 3000L); private set
    fun entranceFrame(elapsed: Long) { entranceRemaining = (entranceRemaining - elapsed).coerceAtLeast(0); state["entranceRemaining"] = entranceRemaining }
    fun edit(value: CompositionProject) {
        if (busy || value == project) return
        try {
            CompositionCodec.validate(value)
            project?.let { history.addLast(it); if (history.size > 100) history.removeFirst() }
            undoCount = history.size
            updateDraft(value)
        } catch (failure: Exception) { error = failure.message }
    }
    private fun updateDraft(value: CompositionProject) {
        project = value; status = if (dirty) "Unsaved changes" else "Saved on this device"; error = null
        // Note arrays stay out of the Activity bundle; serialize ordered atomic checkpoints on IO.
        viewModelScope.launch {
            try { draftLock.withLock { withContext(Dispatchers.IO) {
                val bytes = CompositionCodec.encode(value).toByteArray(Charsets.UTF_8)
                check(draftFile.parentFile!!.isDirectory || draftFile.parentFile!!.mkdirs())
                val stage = File(draftFile.path + ".new")
                FileOutputStream(stage).use { it.write(bytes); it.fd.sync() }
                Files.move(stage.toPath(), draftFile.toPath(), ATOMIC_MOVE, REPLACE_EXISTING)
            } } } catch (failure: Exception) { error = "Draft checkpoint failed. Use Save: ${failure.message}" }
        }
    }
    fun undo() {
        if (!busy && history.isNotEmpty()) { updateDraft(history.removeLast().copy(revision = project!!.revision, modifiedAt = project!!.modifiedAt)); undoCount = history.size }
    }
    fun discard() { viewModelScope.launch { draftLock.withLock { withContext(Dispatchers.IO) { draftFile.delete() } } } }
    init { load() }
    fun load() {
        if (busy) return
        busy = true; loading = true; error = null
        viewModelScope.launch {
            try {
                // A short restrained branded surface spans file readiness and orientation setup.
                val loaded = withContext(Dispatchers.IO) { store.read(requireNotNull(state.get<String>(ComposerActivity.PROJECT_ID))) }
                val draft = withContext(Dispatchers.IO) { if (draftFile.isFile) CompositionCodec.decode(draftFile.readText()) else state.get<String>("draft")?.let(CompositionCodec::decode) }
                require(draft == null || (draft.id == loaded.id && draft.revision == loaded.revision && draft.createdAt == loaded.createdAt)) {
                    "The saved composition changed while a draft was open. Return to the library and reopen it."
                }
                committed = loaded; project = draft ?: loaded
                status = if (dirty) "Unsaved settings" else "Saved on this device"
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (failure: Exception) { error = failure.message ?: "Composition could not be opened." }
            finally { busy = false; loading = false }
        }
    }
    fun settings(title: String, tempo: Int, numerator: Int, denominator: Int) {
        if (busy) return
        val draft = requireNotNull(project).copy(title = title, tempoBpm = tempo, numerator = numerator, denominator = denominator)
        try { CompositionCodec.validate(draft); edit(draft); status = if (dirty) "Unsaved settings" else "Saved on this device" }
        catch (failure: Exception) { error = "Settings would exclude notes. Keep the meter or move notes first." }
    }
    fun save(afterSave: () -> Unit = {}) {
        if (busy) return
        val draft = project ?: return
        busy = true; error = null
        viewModelScope.launch {
            try {
                val saved = draftLock.withLock { withContext(Dispatchers.IO) { store.save(draft).also { draftFile.delete() } } }
                committed = saved; project = saved; state["draft"] = null
                status = "Saved on this device"; afterSave()
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (failure: Exception) { error = failure.message ?: "Save failed. Your draft is retained; retry Save." }
            finally { busy = false }
        }
    }
}

@Composable internal fun BrandedTransition() {
    val activity = LocalContext.current as? ComposerActivity
    Box(Modifier.fillMaxSize().background(Color.Black).drawWithContent {
        drawContent()
        if (activity != null && activity.brandFirstDrawMs == 0L) activity.brandFirstDrawMs = android.os.SystemClock.uptimeMillis()
    }, contentAlignment = Alignment.Center) {
        Text("compose.logger", color = Color.White, style = MaterialTheme.typography.headlineMedium)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable private fun ComposerScreen(onBack: () -> Unit, vm: ComposerViewModel = viewModel()) {
    var mode by rememberSaveable { mutableStateOf("Loop") }
    var settings by rememberSaveable { mutableStateOf(false) }
    var leave by rememberSaveable { mutableStateOf(false) }
    fun back() { if (!vm.busy) { if (vm.dirty && vm.project != null) leave = true else onBack() } }
    BackHandler { back() }
    if (vm.entranceRemaining > 0) {
        BrandedTransition()
        val lifecycle = LocalLifecycleOwner.current.lifecycle
        val entranceActivity = LocalContext.current as? ComposerActivity
        LaunchedEffect(vm, lifecycle) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                withFrameNanos { }
                withFrameNanos { }; var previous = android.os.SystemClock.uptimeMillis()
                while (vm.entranceRemaining > 0) {
                    delay(32); val now = android.os.SystemClock.uptimeMillis()
                    if (entranceActivity?.hasWindowFocus() != false) vm.entranceFrame(now - previous)
                    previous = now
                }
            }
        }
        return
    }
    if (vm.loading) { BrandedTransition(); return }
    val project = vm.project
    if (project == null) {
        Column(Modifier.fillMaxSize().background(Color.Black).safeDrawingPadding().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("compose.logger", color = Color.White, style = MaterialTheme.typography.titleLarge)
            Text(vm.error ?: "Composition unavailable", color = Color.White)
            Button(onClick = vm::load, enabled = !vm.busy) { Text("Retry open") }
            TextButton(onClick = onBack) { Text("Back") }
        }
        return
    }
    val portraitFallback = LocalConfiguration.current.screenWidthDp < LocalConfiguration.current.screenHeightDp
    val activity = LocalContext.current as? ComposerActivity
    Surface(Modifier.fillMaxSize().drawWithContent {
        drawContent()
        if (activity != null && activity.workspaceFirstDrawMs == 0L) activity.workspaceFirstDrawMs = android.os.SystemClock.uptimeMillis()
    }) {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
                Text("compose.logger", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 8.dp))
                TextButton(onClick = { vm.save() }, enabled = !vm.busy) { Text("Save") }
                TextButton(onClick = vm::undo, enabled = !vm.busy && vm.undoCount > 0) { Text("Undo") }
                TextButton(onClick = { back() }, enabled = !vm.busy) { Text("Back") }
                listOf("Loop", "Arrange").forEach { value -> FilterChip(selected = mode == value, onClick = { mode = value }, label = { Text(value) }) }
                TextButton(onClick = { settings = true }, enabled = !vm.busy) { Text("Settings") }

            }
            if (portraitFallback) Text("Rotate or widen for the piano roll.", style = MaterialTheme.typography.bodySmall)
            if (mode == "Loop") PianoRollWorkspace(project, !vm.busy, vm::edit, Modifier.weight(1f))
            else Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column { Text("Arrange workspace · C1 foundation"); Text("Arrangement editing and playback arrive later.") }
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp)) {
                Text(project.title, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(end = 10.dp))
                Text("${project.tempoBpm} BPM · ${project.numerator}/${project.denominator}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(end = 10.dp))
                Text(vm.error ?: vm.status, color = if (vm.error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodySmall)
                Text("  ·  Audition only · loop playback arrives in C3", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
    if (settings) ProjectSettingsDialog("Project settings", project.title, project.tempoBpm, project.numerator, project.denominator, "Apply", { settings = false }) { title, tempo, n, d ->
        vm.settings(title, tempo, n, d); settings = false
    }
    if (leave) AlertDialog(onDismissRequest = { if (!vm.busy) leave = false }, title = { Text("Unsaved composition changes") },
        text = { Column { Text("Save your changes before returning to the composition library?"); vm.error?.let { Text(it, color = MaterialTheme.colorScheme.error) } } },
        confirmButton = { TextButton(onClick = { vm.save(onBack) }, enabled = !vm.busy) { Text("Save and return") } },
        dismissButton = {
            Column {
                TextButton(onClick = { vm.discard(); onBack() }, enabled = !vm.busy) { Text("Discard changes") }
                TextButton(onClick = { leave = false }, enabled = !vm.busy) { Text("Keep editing") }
            }
        })
}
