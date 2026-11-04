package com.mrayover.practicelogger

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PracticeTransportModes(transcribing: Boolean, onRecordLoop: () -> Unit, onTranscribe: () -> Unit,
    onClose: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedSelectionChip(!transcribing, onRecordLoop, modifier = Modifier.testTag("practice-mode-record"),
            label = { Text("Record Loop") })
        OutlinedSelectionChip(transcribing, onTranscribe, modifier = Modifier.testTag("practice-mode-transcribe"),
            label = { Text("Transcribe") })
        if (onClose != null) TextButton(onClick = onClose, modifier = Modifier.heightIn(min = 48.dp)) { Text("Close") }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TranscribePanel(blockId: String, images: ImageReferenceViewModel, vm: LibraryAudioViewModel,
    onRecordLoop: () -> Unit, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val links by images.links.collectAsStateWithLifecycle()
    val assets by images.assets.collectAsStateWithLifecycle()
    val mediaId = links.find { it.blockId == blockId && it.role == MediaRole.AUDIO.name }?.mediaId
    val asset = assets.find { it.id == mediaId }
    val player = vm.controller
    val state by player.state.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var choice by rememberSaveable(blockId) { mutableStateOf(false) }
    var picker by rememberSaveable(blockId) { mutableStateOf(false) }
    var pickerError by rememberSaveable(blockId) { mutableStateOf(false) }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) images.importLibrary(blockId, MediaRole.AUDIO, uri)
    }
    DisposableEffect(lifecycle, player) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                player.setForeground(true)
                player.state.value.mediaId?.let { id ->
                    if (player.state.value.error == null) player.open(id)
                }
            }
            if (event == Lifecycle.Event.ON_PAUSE) player.setForeground(false)
        }
        lifecycle.addObserver(observer)
        player.setForeground(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
        onDispose {
            lifecycle.removeObserver(observer)
            player.setForeground(false)
        }
    }
    LaunchedEffect(asset?.id) {
        if (asset != null) vm.open(asset.id) else vm.close()
    }
    if (choice) AlertDialog(
        onDismissRequest = { choice = false },
        title = { Text(if (asset == null) "Choose audio" else "Change audio") },
        text = {
            Column {
                TextButton(onClick = { choice = false; picker = true }) { Text("Choose from Library") }
                TextButton(onClick = {
                    choice = false
                    try { importer.launch(blockAudioMimeTypes) }
                    catch (_: Exception) { pickerError = true }
                }) { Text("Import New into Library") }
            }
        },
        confirmButton = { TextButton(onClick = { choice = false }) { Text("Cancel") } }
    )
    if (picker) BlockMediaPicker(MediaRole.AUDIO, mediaId, onClose = { picker = false }, onChoose = {
        picker = false
        images.choose(blockId, MediaRole.AUDIO, it)
    })
    val title: @Composable () -> Unit = {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(asset?.title ?: if (mediaId == null) "No audio reference" else "Audio reference unavailable",
                Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleSmall)
            TextButton(onClick = { player.pause(); pickerError = false; choice = true },
                enabled = !images.busy, modifier = Modifier.heightIn(min = 48.dp).testTag("transcribe-choose-audio")) {
                Text(if (asset == null) "Choose Audio" else "Change Audio")
            }
        }
    }
    val messages: @Composable () -> Unit = {
        if (images.busy) Text("Saving reference…")
        images.message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        if (pickerError) Text("File picker unavailable. Try choosing audio from Library.", color = MaterialTheme.colorScheme.error)
    }
    val modes: @Composable () -> Unit = { PracticeTransportModes(true, onRecordLoop, {}, onClose) }
    if (asset == null) {
        PracticeScrollPanel(modifier, tag = "transcribe-panel") { modes(); title(); messages() }
        return
    }

    val duration = asset.durationMs ?: 1L
    val markerFlow = remember(asset.id) { vm.markers.observe(asset.id) }
    val markers by markerFlow.collectAsStateWithLifecycle(emptyList())
    var start by rememberSaveable(asset.id) { mutableDoubleStateOf(0.0) }
    var span by rememberSaveable(asset.id) { mutableDoubleStateOf(duration.toDouble()) }
    val timeline = AudioTimeline(duration, start, span)
    fun viewport(value: AudioTimeline) { start = value.startMs; span = value.spanMs }
    var rangeDialog by rememberSaveable(asset.id) { mutableStateOf(false) }
    var seekDialog by rememberSaveable(asset.id) { mutableStateOf(false) }
    var pianoExpanded by rememberSaveable(blockId) { mutableStateOf(false) }
    val ready = state.mediaId == asset.id && state.durationMs > 0 && !state.preparing

    val controls: @Composable () -> Unit = {
        AudioPlaybackControls(player, state, asset.id)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton(onClick = { seekDialog = true }, enabled = ready, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("${audioTimestamp(state.positionMs)} / ${audioTimestamp(duration)}")
            }
            OutlinedSelectionChip(state.repeatRange, { player.repeatRange(!state.repeatRange) }, enabled = ready,
                modifier = Modifier.testTag("transcribe-repeat"), label = { Text("Repeat A–B") })
        }
        Text("A ${audioTimestamp(state.aMs)} · B ${audioTimestamp(state.bMs)}", style = MaterialTheme.typography.bodySmall)
    }
    val waveform: @Composable () -> Unit = {
        AudioWaveformView(vm.waveform, state, markers, timeline, ::viewport, player::seek,
            { a, b -> player.range(a, b) })
    }
    val details: @Composable () -> Unit = {
        messages()
        if (state.preparing) Text("Preparing audio…")
        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
            TextButton(onClick = { player.open(asset.id) }) { Text("Retry playback") }
        }
        if (vm.waveform == null && vm.waveformError == null) Text("Analyzing waveform…", style = MaterialTheme.typography.bodySmall)
        vm.waveformError?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
            TextButton(onClick = vm::retryWaveform) { Text("Retry waveform") }
        }
        AudioZoomControls(timeline, state.positionMs, duration, ::viewport)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            OutlinedButton(onClick = { player.range(state.positionMs, state.bMs) }, enabled = ready && state.positionMs < state.bMs) { Text("Set A here") }
            OutlinedButton(onClick = { player.range(state.aMs, state.positionMs) }, enabled = ready && state.positionMs > state.aMs) { Text("Set B here") }
            OutlinedButton(onClick = { rangeDialog = true }, enabled = ready) { Text("Edit A–B") }
            OutlinedButton(onClick = player::playSelection, enabled = ready) { Text("Play phrase once") }
        }
        OutlinedSelectionChip(pianoExpanded, { pianoExpanded = !pianoExpanded },
            modifier = Modifier.testTag("transcribe-piano"), label = { Text("Piano") })
        if (pianoExpanded) EmbeddedReferencePiano(player)
        Text("Master markers and sections", style = MaterialTheme.typography.titleSmall)
        if (markers.isEmpty()) Text("No master markers yet.", style = MaterialTheme.typography.bodySmall)
        markers.forEach { marker ->
            TextButton(onClick = {
                player.repeatRange(false)
                marker.endMs?.let { player.range(marker.startMs, it) }
                player.seek(marker.startMs)
                viewport(timeline.copy(startMs = (marker.startMs - timeline.spanMs / 4)
                    .coerceIn(0.0, (duration - timeline.spanMs).coerceAtLeast(0.0))))
            }, enabled = ready, modifier = Modifier.heightIn(min = 48.dp).testTag("master-jump-${marker.id}")) {
                Text("${marker.label} · ${audioTimestamp(marker.startMs)}${marker.endMs?.let { "–${audioTimestamp(it)}" }.orEmpty()}")
            }
            if (marker.note.isNotBlank()) Text(marker.note, style = MaterialTheme.typography.bodySmall)
        }
    }
    BoxWithConstraints(modifier.testTag("transcribe-panel")) {
        if (maxWidth >= 560.dp && maxHeight < 370.dp) {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PracticeScrollPanel(Modifier.weight(1f).fillMaxHeight(), tag = "transcribe-controls") {
                    modes(); title(); controls()
                }
                PracticeScrollPanel(Modifier.weight(1f).fillMaxHeight(), tag = "transcribe-details") {
                    waveform(); details()
                }
            }
        } else if (maxHeight >= 370.dp && maxWidth >= 350.dp && LocalDensity.current.fontScale < 1.4f) {
            Column(Modifier.fillMaxSize()) {
                modes(); title(); controls(); waveform()
                PracticeScrollPanel(Modifier.weight(1f).fillMaxWidth(), tag = "transcribe-details") { details() }
            }
        } else {
            PracticeScrollPanel(Modifier.fillMaxSize(), tag = "transcribe-details") {
                modes(); title(); controls(); waveform(); details()
            }
        }
    }
    if (rangeDialog) AudioRangeDialog(state, { rangeDialog = false }) { a, b -> player.range(a, b); rangeDialog = false }
    if (seekDialog) AudioSeekDialog(state.positionMs, duration, { seekDialog = false }) { player.seek(it); seekDialog = false }
}

@Composable
private fun EmbeddedReferencePiano(player: LibraryAudioController) {
    val context = LocalContext.current
    var reset by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    val engine = remember(context, player) { PianoAudioEngine(context, player) { error = it; reset++ } }
    DisposableEffect(engine) { onDispose { engine.close() } }
    PianoKeyboard({ token, note -> engine.noteOn(token, note); Unit }, engine::noteOff, engine::stopAll, resetToken = reset)
    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
}
