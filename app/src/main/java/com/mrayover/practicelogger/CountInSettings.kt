package com.mrayover.practicelogger

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

class CountInPreferences(context: Context) {
    private val preferences = context.getSharedPreferences("audio-settings", Context.MODE_PRIVATE)
    fun read(): Long = runCatching { preferences.getLong("count-in-beats",4).also { PhraseSpec(300,1,it) } }.getOrDefault(4)
    fun save(value: Long) { PhraseSpec(300,1,value); check(preferences.edit().putLong("count-in-beats",value).commit()) { "Default count-in could not be saved." } }
}

@Composable fun CountInSettings(vm: RecordingViewModel, onTools: () -> Unit, fromHome: Boolean = false) {
    BackHandler(onBack = onTools)
    var value by rememberSaveable(vm.defaultCountIn) { mutableStateOf(vm.defaultCountIn.toString()) }
    val parsed = value.toLongOrNull()
    val context = LocalContext.current
    val preferences = remember { AudioPreferences(context) }
    var sound by remember { mutableStateOf(preferences.sound) }
    var alarm by remember { mutableStateOf(preferences.alarm) }
    var mode by remember { mutableStateOf(preferences.mode) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var previewToken by remember { mutableIntStateOf(0) }
    val preview = remember { MetronomeAudio(context) { feedback = it } }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner,preview) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) preview.stop() }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer); preview.stop() }
    }
    LaunchedEffect(previewToken) { if (previewToken > 0) { delay(1500); preview.stop() } }
    fun save(action: () -> Unit) { runCatching(action).onSuccess { feedback = "Saved" }.onFailure { feedback = it.message } }
    fun startPreview(action: () -> Unit) {
        preview.stop()
        if (RecordingViewModel.isCapturingAudio || vm.playingId != null) feedback = "Stop recording or playback before previewing."
        else { previewToken++; action() }
    }
    PracticeScrollPanel(Modifier.fillMaxSize().safeDrawingPadding().imePadding().padding(horizontal=16.dp), tag="audio-settings-scroll") {
    Column(Modifier.fillMaxWidth().padding(vertical=24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
        AppearanceSettings()
        HorizontalDivider()
        Text("Audio Settings", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        Text("Metronome sound", style = MaterialTheme.typography.titleMedium)
        ClickSound.entries.forEach { choice ->
            FilterChip(selected=sound == choice, onClick={ save { preferences.saveSound(choice); sound=choice } }, label={ Text(choice.label) })
        }
        Button(onClick={ startPreview { preview.configure(MetronomeConfig(mode=MetronomeMode.SOUND_ONLY,sound=sound)); preview.start(120) } }) { Text("Preview metronome sound") }
        Text("Default output mode", style = MaterialTheme.typography.titleMedium)
        MetronomeMode.entries.forEach { choice ->
            FilterChip(selected=mode == choice, onClick={ save { preferences.saveMode(choice); mode=choice } }, label={ Text(choice.label) })
        }
        Text("Completion alarm", style = MaterialTheme.typography.titleMedium)
        AlarmSound.entries.forEach { choice ->
            FilterChip(selected=alarm == choice, onClick={ save { preferences.saveAlarm(choice); alarm=choice } }, label={ Text(choice.label) })
        }
        Button(onClick={ startPreview { preview.playCompletion(alarm) } }) { Text("Preview completion alarm") }
        TextButton(onClick={ preview.stop() }) { Text("Stop preview") }
        Text("Sounds are shared by practice and Tools. Alarms play once when a foreground timer reaches zero; Off is silent. Alarms are suppressed during capture.")
        feedback?.let { Text(it) }
        HorizontalDivider()
        Text("Default loop count-in")
        OutlinedTextField(value, { value = it }, label = { Text("Default count-in beats") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), isError = parsed == null || parsed <= 0,
            modifier = Modifier.testTag("default-count-in"))
        Text("Positive whole beats. Each loop can override this default. Count-ins must fit a 15-minute execution budget at the selected BPM.")
        Button(onClick = { vm.saveDefaultCountIn(value) }, enabled = !vm.busy && parsed != null && parsed > 0) { Text("Save default count-in") }
        vm.message?.let { Text(it) }
        if (!fromHome) TextButton(onClick = onTools) { Text("Back to Tools") }
    }
    }
}

@Composable private fun AppearanceSettings() {
    val preferences = LocalThemePreferences.current
    val choice by preferences.choice.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var saving by remember { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    Text("Appearance", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
    Text("Theme", style = MaterialTheme.typography.titleMedium)
    ThemeChoice.entries.forEach { option ->
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("theme-${option.id}")
            .selectable(selected = choice == option, enabled = !saving, role = Role.RadioButton, onClick = {
                saving = true
                scope.launch {
                    runCatching { withContext(Dispatchers.IO) { preferences.save(option) } }
                        .onSuccess { error = null }.onFailure { error = it.message }
                    saving = false
                }
            }), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            RadioButton(selected = choice == option, onClick = null, enabled = !saving)
            Text(option.label)
        }
    }
    Text("System Default follows your device appearance. Light keeps the original cream and green palette.",
        style = MaterialTheme.typography.bodySmall)
    error?.let { Text(it, color = MaterialTheme.colorScheme.error,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
}
