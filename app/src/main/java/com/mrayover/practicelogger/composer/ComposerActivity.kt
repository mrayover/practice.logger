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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
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

class ComposerActivity : ComponentActivity() {
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
    init { load() }
    fun load() {
        if (busy) return
        busy = true; loading = true; error = null
        viewModelScope.launch {
            try {
                // A short restrained branded surface spans file readiness and orientation setup.
                val loaded = withContext(Dispatchers.IO) { store.read(requireNotNull(state.get<String>(ComposerActivity.PROJECT_ID))) }
                val draft = state.get<String>("draft")?.let(CompositionCodec::decode)
                require(draft == null || (draft.id == loaded.id && draft.revision == loaded.revision && draft.createdAt == loaded.createdAt)) {
                    "The saved composition changed while a draft was open. Return to the library and reopen it."
                }
                committed = loaded; project = draft ?: loaded
                delay(400)
                status = if (dirty) "Unsaved settings" else "Saved on this device"
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (failure: Exception) { error = failure.message ?: "Composition could not be opened." }
            finally { busy = false; loading = false }
        }
    }
    fun settings(title: String, tempo: Int, numerator: Int, denominator: Int) {
        if (busy) return
        val draft = requireNotNull(project).copy(title = title, tempoBpm = tempo, numerator = numerator, denominator = denominator)
        CompositionCodec.validate(draft)
        state["draft"] = CompositionCodec.encode(draft)
        project = draft; status = if (dirty) "Unsaved settings" else "Saved on this device"; error = null
    }
    fun save(afterSave: () -> Unit = {}) {
        if (busy) return
        val draft = project ?: return
        busy = true; error = null
        viewModelScope.launch {
            try {
                val saved = withContext(Dispatchers.IO) { store.save(draft) }
                committed = saved; project = saved; state["draft"] = null
                status = "Saved on this device"; afterSave()
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (failure: Exception) { error = failure.message ?: "Save failed. Your draft is retained; retry Save." }
            finally { busy = false }
        }
    }
}

@Composable internal fun BrandedTransition() {
    Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
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
    if (vm.loading) { BrandedTransition(); return }
    val project = vm.project
    if (project == null) {
        Column(Modifier.fillMaxSize().background(Color.Black).safeDrawingPadding().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("compose.logger", color = Color.White, style = MaterialTheme.typography.titleLarge)
            Text(vm.error ?: "Composition unavailable", color = Color.White)
            Button(onClick = vm::load, enabled = !vm.busy) { Text("Retry open") }
            TextButton(onClick = onBack) { Text("Back to composition library") }
        }
        return
    }
    val portraitFallback = LocalConfiguration.current.screenWidthDp < LocalConfiguration.current.screenHeightDp
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                Text("compose.logger", style = MaterialTheme.typography.titleLarge, modifier = Modifier.align(Alignment.CenterVertically))
                Button(onClick = { vm.save() }, enabled = !vm.busy) { Text("Save") }
                TextButton(onClick = {}, enabled = false) { Text("Undo") }
                TextButton(onClick = { back() }, enabled = !vm.busy) { Text("Back to composition library") }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(project.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.CenterVertically))
                Text("${project.tempoBpm} BPM · ${project.numerator}/${project.denominator}", modifier = Modifier.align(Alignment.CenterVertically))
                TextButton(onClick = { settings = true }, enabled = !vm.busy) { Text("Project settings") }
                Text(vm.status, style = MaterialTheme.typography.bodySmall, modifier = Modifier.align(Alignment.CenterVertically))
            }
            if (portraitFallback) Text("Landscape workspace: rotate your device or widen this window. All foundation controls remain available here.", style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Loop", "Arrange").forEach { value -> FilterChip(selected = mode == value, onClick = { mode = value }, label = { Text(value) }) }
            }
            Box(Modifier.fillMaxWidth().heightIn(min = if (portraitFallback) 180.dp else 100.dp).border(1.dp, MaterialTheme.colorScheme.outlineVariant).padding(20.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("$mode workspace · C1 foundation", style = MaterialTheme.typography.titleMedium)
                    Text(if (mode == "Loop") "An empty musical document. Instrument tracks, patterns and note editing arrive in later increments."
                        else "Arrangement timeline reserved. No clips, arrangement editing or playback in this build.")
                }
            }
            Text("Transport area reserved · playback and export are not available in C1", style = MaterialTheme.typography.bodySmall)
            vm.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
    if (settings) ProjectSettingsDialog("Project settings", project.title, project.tempoBpm, project.numerator, project.denominator, "Apply", { settings = false }) { title, tempo, n, d ->
        vm.settings(title, tempo, n, d); settings = false
    }
    if (leave) AlertDialog(onDismissRequest = { if (!vm.busy) leave = false }, title = { Text("Unsaved composition settings") },
        text = { Column { Text("Save your changes before returning to the composition library?"); vm.error?.let { Text(it, color = MaterialTheme.colorScheme.error) } } },
        confirmButton = { TextButton(onClick = { vm.save(onBack) }, enabled = !vm.busy) { Text("Save and return") } },
        dismissButton = {
            Column {
                TextButton(onClick = onBack, enabled = !vm.busy) { Text("Discard changes") }
                TextButton(onClick = { leave = false }, enabled = !vm.busy) { Text("Keep editing") }
            }
        })
}
