package com.mrayover.practicelogger

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
internal fun rememberAudioDisplayState(player: LibraryAudioController): State<LibraryAudioState> {
    // Keep the 15 ms playback loop independent of the Compose cursor refresh rate.
    val displayState = remember(player) {
        player.state.distinctUntilChanged { previous, next ->
            previous.playing && next.playing && previous.copy(positionMs = next.positionMs) == next &&
                previous.positionMs / 100 == next.positionMs / 100
        }
    }
    return displayState.collectAsStateWithLifecycle(player.state.value)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable fun LibraryAudioScreen(asset: MediaAsset, close: () -> Unit, vm: LibraryAudioViewModel = viewModel(), onChangeAudio: (() -> Unit)? = null) {
    val standalone = onChangeAudio != null
    val player = vm.controller
    val state by rememberAudioDisplayState(player)
    val markerFlow = remember(asset.id) { vm.markers.observe(asset.id) }
    val markers by markerFlow.collectAsStateWithLifecycle(emptyList())
    val duration = asset.durationMs ?: 1
    var start by rememberSaveable(asset.id) { mutableDoubleStateOf(0.0) }
    var span by rememberSaveable(asset.id) { mutableDoubleStateOf(duration.toDouble()) }
    val timeline = AudioTimeline(duration, start, span)
    fun viewport(value: AudioTimeline) { start = value.startMs; span = value.spanMs }
    var rangeDialog by rememberSaveable { mutableStateOf(false) }
    var seekDialog by rememberSaveable { mutableStateOf(false) }
    var editId by rememberSaveable { mutableStateOf<String?>(null) }
    var newType by rememberSaveable { mutableStateOf<String?>(null) }
    var deleteId by rememberSaveable { mutableStateOf<String?>(null) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val activity = LocalContext.current as? Activity
    val compactHeight = LocalConfiguration.current.screenHeightDp / LocalDensity.current.fontScale < 480
    var pianoExpanded by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(asset.id) { vm.open(asset.id) }
    DisposableEffect(lifecycle, asset.id) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                player.setForeground(true)
                if (standalone && player.state.value.mediaId == asset.id && player.state.value.error == null) {
                    player.open(asset.id)
                }
            }
            if (standalone && event == Lifecycle.Event.ON_PAUSE) player.setForeground(false)
            if (event == Lifecycle.Event.ON_STOP && activity?.isChangingConfigurations != true) player.setForeground(false)
        }
        lifecycle.addObserver(observer)
        // A replacement Activity can compose before STARTED during rotation. Only an actual
        // non-configuration ON_STOP marks background; attachment must not stop a retained player.
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) player.setForeground(true)
        onDispose {
            lifecycle.removeObserver(observer)
            if (standalone || activity?.isChangingConfigurations != true) vm.close()
        }
    }
    BackHandler(onBack = close)
    val ready = state.mediaId == asset.id && state.durationMs > 0 && !state.preparing && state.error == null
    val transport: @Composable () -> Unit = {
        // Transport and source-time/range readouts remain independent of the long marker list.
        AudioPlaybackControls(player, state, asset.id, enabled = !standalone || ready)
        Row(Modifier.fillMaxWidth()) {
            TextButton(onClick = { seekDialog = true }, enabled = !standalone || ready, modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("audio-position")) {
                Text(if (compactHeight) "${audioTimestamp(state.positionMs).substringBeforeLast('.')} / ${audioTimestamp(duration).substringBeforeLast('.')}" else "${audioTimestamp(state.positionMs)} / ${audioTimestamp(duration)}")
            }
            TextButton(onClick = { rangeDialog = true }, enabled = !standalone || ready, modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("audio-edit-range")) {
                Text(if (compactHeight) "Edit A–B" else "A ${audioTimestamp(state.aMs)} · B ${audioTimestamp(state.bMs)}")
            }
        }
        if (state.preparing) Text("Preparing audio…")
        state.error?.let { error -> Text(error, color = MaterialTheme.colorScheme.error); TextButton(onClick = { player.open(asset.id) }) { Text("Retry playback") } }
    }
    val details: @Composable () -> Unit = {
        AudioWaveformView(vm.waveform, state, markers, timeline, ::viewport, player::seek, { a, b -> player.range(a, b) })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(audioTimestamp(timeline.timeAt(0f)), style = MaterialTheme.typography.bodySmall)
            Text(audioTimestamp(timeline.timeAt(1f)), style = MaterialTheme.typography.bodySmall)
        }
        if (vm.waveform == null && vm.waveformError == null) Text("Analyzing waveform…", style = MaterialTheme.typography.bodySmall)
        vm.waveformError?.let { Text(it); TextButton(onClick = vm::retryWaveform) { Text("Retry waveform") } }
        AudioZoomControls(timeline, state.positionMs, duration, ::viewport)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            OutlinedButton(onClick = { player.range(state.positionMs, state.bMs) }, enabled = state.positionMs < state.bMs) { Text("Set A here") }
            OutlinedButton(onClick = { player.range(state.aMs, state.positionMs) }, enabled = state.positionMs > state.aMs) { Text("Set B here") }
            OutlinedButton(onClick = player::playSelection, enabled = !state.preparing) { Text("Play selection") }
        }
        Row { Checkbox(state.repeatRange, { player.repeatRange(it) }, Modifier.testTag("audio-repeat-range")); Text("Repeat A–B", Modifier.padding(top = 12.dp)) }
        if (!standalone) Row { Checkbox(state.repeatFile, { player.repeatFile(it) }); Text("Repeat whole file", Modifier.padding(top = 12.dp)) }
    }
    val annotationControls: @Composable () -> Unit = {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton(onClick = { newType = "POINT" }, enabled = state.durationMs > 0, contentPadding = PaddingValues(horizontal = 4.dp), modifier = Modifier.testTag("audio-add-point")) { Text("+ Point") }
            TextButton(onClick = { newType = "SECTION" }, enabled = validAudioRange(state.aMs, state.bMs, duration), contentPadding = PaddingValues(horizontal = 4.dp), modifier = Modifier.testTag("audio-add-section")) { Text("+ Section") }
        }
        vm.message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
    val markerRow: @Composable (MasterAudioMarker) -> Unit = { marker ->
        OutlinedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(8.dp)) {
                TextButton(onClick = {
                    if (marker.endMs != null) player.range(marker.startMs, marker.endMs)
                    else player.repeatRange(false)
                    player.seek(marker.startMs)
                    viewport(AudioTimeline(duration).copy(spanMs = minOf(10000.0, duration.toDouble())).let {
                        it.copy(startMs = (marker.startMs - it.spanMs / 4).coerceIn(0.0, duration - it.spanMs))
                    })
                }, modifier = Modifier.fillMaxWidth().testTag("master-jump-${marker.id}")) {
                    Text("${marker.label} · ${audioTimestamp(marker.startMs)}${marker.endMs?.let { "–${audioTimestamp(it)}" }.orEmpty()}")
                }
                if (marker.note.isNotBlank()) Text(marker.note, maxLines = 3, style = MaterialTheme.typography.bodySmall)
                Row {
                    TextButton(onClick = { editId = marker.id }) { Text("Edit") }
                    TextButton(onClick = { deleteId = marker.id }) { Text("Delete") }
                }
            }
        }
    }
    val noMarkers: @Composable () -> Unit = {
        Text("No master markers yet.", style = MaterialTheme.typography.bodySmall)
    }
    if (onChangeAudio != null) {
        val pianoHeight = if (compactHeight) 104.dp else 144.dp
        LazyColumn(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 12.dp)
            .testTag("standalone-transcribe"), verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(bottom = 16.dp)) {
            item {
                TextButton(onClick = close, modifier = Modifier.heightIn(min = 48.dp).testTag("audio-close")) {
                    Text("Back to Practice Now")
                }
                Text(asset.title, style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = onChangeAudio, modifier = Modifier.heightIn(min = 48.dp)) { Text("Change Audio") }
                transport()
                if (compactHeight) Text("A ${audioTimestamp(state.aMs)} · B ${audioTimestamp(state.bMs)}",
                    style = MaterialTheme.typography.bodySmall)
                details()
            }
            item {
                OutlinedSelectionChip(pianoExpanded, { pianoExpanded = !pianoExpanded },
                    modifier = Modifier.testTag("transcribe-piano"), label = { Text("Reference Piano") })
                if (pianoExpanded) EmbeddedReferencePiano(player, pianoHeight)
            }
            item {
                Text("Master Markers — shared with Blocks", style = MaterialTheme.typography.titleMedium)
                annotationControls()
                if (markers.isEmpty()) noMarkers()
            }
            items(markers, key = { it.id }) { markerRow(it) }
        }
    } else {
        Column(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 8.dp)) {
            Row(Modifier.fillMaxWidth()) {
                TextButton(onClick = close, modifier = Modifier.heightIn(min = 48.dp).testTag("audio-close")) { Text(if (compactHeight) "Back" else "Back to Library") }
                Text(asset.title, Modifier.weight(1f).padding(8.dp), maxLines = if (compactHeight) 1 else 2, style = MaterialTheme.typography.titleMedium)
            }
            transport()
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                val scrollDetails: @Composable (Modifier) -> Unit = { modifier ->
                    Column(modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (compactHeight) Text("A ${audioTimestamp(state.aMs)} · B ${audioTimestamp(state.bMs)}", style = MaterialTheme.typography.bodySmall)
                        details()
                    }
                }
                val annotationPanel: @Composable (Modifier) -> Unit = { modifier ->
                    Column(modifier) {
                        annotationControls()
                        LazyColumn(Modifier.weight(1f).fillMaxWidth().testTag("master-markers"), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (markers.isEmpty()) item { noMarkers() }
                            items(markers, key = { it.id }) { markerRow(it) }
                        }
                    }
                }
                if (maxWidth > 600.dp && maxWidth > maxHeight * 1.4f) Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    scrollDetails(Modifier.weight(1f).fillMaxHeight()); annotationPanel(Modifier.weight(1f).fillMaxHeight())
                } else Column {
                    scrollDetails(Modifier.weight(.5f).fillMaxWidth()); HorizontalDivider(); annotationPanel(Modifier.weight(.5f).fillMaxWidth())
                }
            }
        }
    }
    if (rangeDialog) AudioRangeDialog(state, { rangeDialog = false }) { a, b -> player.range(a, b); rangeDialog = false }
    if (seekDialog) AudioSeekDialog(state.positionMs, duration, { seekDialog = false }) { player.seek(it); seekDialog = false }
    val editing = markers.find { it.id == editId }
    if (newType != null || editing != null) key(editId, newType) {
        AudioMarkerDialog(editing, state, newType == "SECTION", "Master", vm.saving, vm.message,
            close = { editId = null; newType = null }) { a, b, label, note, done ->
            vm.save(editing?.id, a, b, label, note, done)
        }
    }
    deleteId?.let { id -> AlertDialog(onDismissRequest = { if (!vm.saving) deleteId = null }, title = { Text("Delete master marker?") },
        text = { Text("Delete the marker and its note? The recording is kept.") },
        confirmButton = { TextButton(onClick = { vm.delete(id) { deleteId = null } }, enabled = !vm.saving) { Text("Delete marker") } },
        dismissButton = { TextButton(onClick = { deleteId = null }, enabled = !vm.saving) { Text("Cancel") } }) }
}

@Composable internal fun AudioSeekDialog(position: Long, duration: Long, close: () -> Unit, seek: (Long) -> Unit) {
    var text by rememberSaveable { mutableStateOf(audioSeconds(position)) }
    val time = parseAudioSeconds(text)
    AlertDialog(onDismissRequest = close, title = { Text("Seek to source time") }, text = {
        OutlinedTextField(text, { text = it }, label = { Text("Seconds") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), isError = time == null || time > duration, modifier = Modifier.testTag("seek-seconds"))
    }, confirmButton = { TextButton(onClick = { seek(time!!) }, enabled = time != null && time <= duration) { Text("Seek") } }, dismissButton = { TextButton(onClick = close) { Text("Cancel") } })
}

@Composable internal fun AudioRangeDialog(state: LibraryAudioState, close: () -> Unit, save: (Long, Long) -> Unit) {
    var a by rememberSaveable { mutableStateOf(audioSeconds(state.aMs)) }
    var b by rememberSaveable { mutableStateOf(audioSeconds(state.bMs)) }
    val start = parseAudioSeconds(a); val end = parseAudioSeconds(b)
    val valid = start != null && end != null && validAudioRange(start, end, state.durationMs)
    AlertDialog(onDismissRequest = close, title = { Text("Edit A–B range") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Source seconds; A must precede B.", style = MaterialTheme.typography.bodySmall)
            for (isA in listOf(true, false)) {
                OutlinedTextField(if (isA) a else b, { if (isA) a = it else b = it }, label = { Text(if (isA) "A seconds" else "B seconds") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.testTag(if (isA) "range-a" else "range-b"))
                Row {
                    TextButton(onClick = { val value = parseAudioSeconds(if (isA) a else b) ?: 0; val next = audioSeconds((value - 10).coerceAtLeast(0)); if (isA) a = next else b = next }) { Text("−10 ms") }
                    TextButton(onClick = { val value = parseAudioSeconds(if (isA) a else b) ?: 0; val next = audioSeconds((value + 10).coerceAtMost(state.durationMs)); if (isA) a = next else b = next }) { Text("+10 ms") }
                }
            }
            if (!valid) Text("Choose a nonempty range inside the recording.", color = MaterialTheme.colorScheme.error)
        }
    }, confirmButton = { TextButton(onClick = { save(start!!, end!!) }, enabled = valid) { Text("Apply range") } }, dismissButton = { TextButton(onClick = close) { Text("Cancel") } })
}

@Composable
internal fun AudioMarkerDialog(marker: MasterAudioMarker?, state: LibraryAudioState, section: Boolean,
    owner: String, saving: Boolean, message: String?, close: () -> Unit,
    save: (Long, Long?, String, String, () -> Unit) -> Unit) {
    var label by rememberSaveable { mutableStateOf(marker?.label ?: "") }
    var note by rememberSaveable { mutableStateOf(marker?.note ?: "") }
    val isSection = marker?.let { it.endMs != null } ?: section
    var start by rememberSaveable { mutableStateOf(audioSeconds(marker?.startMs ?: if (isSection) state.aMs else state.positionMs)) }
    var end by rememberSaveable { mutableStateOf(audioSeconds(marker?.endMs ?: state.bMs)) }
    val a = parseAudioSeconds(start); val b = if (isSection) parseAudioSeconds(end) else null
    val valid = a != null && a <= state.durationMs && (!isSection || b != null && validAudioRange(a, b, state.durationMs)) && runCatching { MediaLibraryRepository.title(label) }.isSuccess && note.length <= 8000
    val kind = if (isSection) "section" else "marker"
    val title = when {
        owner == "Master" && marker == null -> "New master $kind"
        owner == "Master" -> "Edit master marker"
        marker?.id.isNullOrEmpty() -> "New $kind for This Block"
        else -> "Edit marker for This Block"
    }
    AlertDialog(onDismissRequest = { if (!saving) close() }, title = { Text(title) }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(label, { label = it }, label = { Text("Label") }, modifier = Modifier.testTag("marker-label"))
            OutlinedTextField(note, { note = it }, label = { Text("Note (optional)") }, modifier = Modifier.testTag("marker-note"))
            OutlinedTextField(start, { start = it }, label = { Text("Start seconds") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.testTag("marker-start"))
            if (isSection) OutlinedTextField(end, { end = it }, label = { Text("End seconds") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.testTag("marker-end"))
            message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }, confirmButton = { TextButton(onClick = { save(a!!, b, label, note, close) }, enabled = valid && !saving) { Text("Save marker") } },
        dismissButton = { TextButton(onClick = close, enabled = !saving) { Text("Cancel") } })
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AudioPlaybackControls(player: LibraryAudioController, state: LibraryAudioState, mediaId: String, enabled: Boolean = true) {
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Button(onClick = { if (state.playing) player.pause() else player.play() }, enabled = enabled && !state.preparing && state.mediaId == mediaId, modifier = Modifier.heightIn(min = 48.dp).testTag("audio-play")) { Text(if (state.playing) "Pause" else "Play") }
        OutlinedButton(onClick = player::stop, enabled = enabled && !state.preparing, modifier = Modifier.heightIn(min = 48.dp)) { Text("Stop") }
        TextButton(onClick = { player.speed(state.speedPercent - 5) }, enabled = enabled && state.speedPercent > 50, modifier = Modifier.heightIn(min = 48.dp).testTag("audio-slower")) { Text("−5%") }
        Text("${state.speedPercent}%", Modifier.padding(vertical = 12.dp).testTag("audio-speed"))
        TextButton(onClick = { player.speed(state.speedPercent + 5) }, enabled = enabled && state.speedPercent < 100, modifier = Modifier.heightIn(min = 48.dp)) { Text("+5%") }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AudioZoomControls(timeline: AudioTimeline, positionMs: Long, duration: Long, viewport: (AudioTimeline) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        TextButton(onClick = { viewport(timeline.transform(2f, 0f, timeline.fraction(positionMs).coerceIn(0f, 1f))) }) { Text("Zoom in") }
        TextButton(onClick = { viewport(timeline.transform(.5f, 0f)) }) { Text("Zoom out") }
        TextButton(onClick = { viewport(timeline.transform(1f, .5f)) }, enabled = timeline.startMs > 0) { Text("Earlier") }
        TextButton(onClick = { viewport(timeline.transform(1f, -.5f)) }, enabled = timeline.startMs + timeline.spanMs < duration) { Text("Later") }
        TextButton(onClick = { viewport(AudioTimeline(duration)) }) { Text("Fit track") }
    }
}
