package com.mrayover.practicelogger

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle

fun takeTitle(take: BlockTake): String = take.label.ifBlank { "Take " + sessionDate(take.createdAt) + " · " + take.id.take(4) }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable fun BlockRecordings(blockId: String?, blockName: String, vm: RecordingViewModel,
    practice: PracticeViewModel, timer: SessionBundle?, initialBpm: Int, onBeforePlayback: () -> Unit, onDismiss: () -> Unit, startOnOpen: Boolean = false, references: ImageReferenceViewModel? = null, manageLifecycle: Boolean = true) {
    val keyboard = LocalSoftwareKeyboardController.current
    val all by vm.takes.collectAsStateWithLifecycle()
    val takes = all.filter { it.blockId == blockId }
    var renameId by rememberSaveable { mutableStateOf<String?>(null) }
    var label by rememberSaveable { mutableStateOf("") }
    var deleteId by rememberSaveable { mutableStateOf<String?>(null) }
    var exportId by rememberSaveable { mutableStateOf<String?>(null) }
    var exportOptionsId by rememberSaveable { mutableStateOf<String?>(null) }
    var destination by remember { mutableStateOf(vm.exportStorage.selected()) }
    var folderFeedback by rememberSaveable { mutableStateOf<String?>(null) }
    var recoveredWarning by rememberSaveable { mutableStateOf(vm.exportStorage.unfinishedCopy()) }
    val chooseFolder = exportFolderPicker(vm.exportStorage) { error ->
        destination = vm.exportStorage.selected()
        folderFeedback = error
    }
    LaunchedEffect(vm.failedOrganizedExport, blockId) {
        val failed = vm.failedOrganizedExport
        if (failed != null && takes.any { it.id == failed }) {
            exportOptionsId = failed
            folderFeedback = vm.message
            vm.failedOrganizedExport = null
        }
    }
    var phraseDialog by rememberSaveable { mutableStateOf(false) }
    var pendingStart by rememberSaveable { mutableStateOf(startOnOpen) }
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    fun requestClose() {
        if (vm.busy) return
        if (vm.recording) vm.finishCapture()
        else if (vm.reviewTake != null) confirmDiscard = true
        else onDismiss()
    }
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        vm.permissionResult(granted)
        if (granted && blockId == null) pendingStart = true
    }
    val exportResult: (android.net.Uri?) -> Unit = { uri ->
        val selected = all.find { it.id == exportId }
        if (uri != null) {
            if (selected != null) vm.export(selected, uri) else vm.expiredExport()
        }
        exportId = null
    }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("audio/mp4"), exportResult)
    val exportMp3 = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("audio/mpeg"), exportResult)
    val exportWave = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("audio/wav"), exportResult)
    fun saveAs(take: BlockTake) {
        exportId = take.id
        val name = exportFileName(take)
        when (take.audioMime) {
            "audio/wav" -> exportWave.launch(name)
            "audio/mpeg" -> exportMp3.launch(name)
            else -> export.launch(name)
        }
    }
    if (manageLifecycle) DisposableEffect(blockId, vm) { vm.enterBlock(blockId); onDispose { vm.leaveBlock() } }
    LaunchedEffect(pendingStart, vm.ready, vm.busy, lifecycleState) {
        if (pendingStart && vm.ready && !vm.busy && lifecycleState == Lifecycle.State.RESUMED) {
            pendingStart = false // Recreation/background must never restart capture automatically.
            if (vm.hasMicrophonePermission()) vm.startCapture(blockId)
            else permission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
    ModalBottomSheet(onDismissRequest = ::requestClose, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true, confirmValueChange = { state ->
        state != SheetValue.Hidden || (!vm.recording && !vm.busy && vm.reviewTake == null)
    })) {
      Column(Modifier.fillMaxWidth().fillMaxHeight(0.9f).imePadding().padding(horizontal = 24.dp)) {
        vm.message?.let { Text(it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }.padding(bottom = 8.dp)) }
        recoveredWarning?.let { path ->
            Text("A previous export may be incomplete at $path.", style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = { recoveredWarning = null }) { Text("Dismiss export notice") }
        }
        if (vm.recording) {
            // Always reachable, including when a tall reference or take list is scrolled.
            Text(if (vm.phrasing && vm.countInBeat > 0) "Count-in · ${vm.countInBeat} of ${vm.countInTotal}"
                else "Recording · " + practiceTime(vm.elapsedMs) + if (vm.phrasing) " / " + practiceTime(vm.phraseDurationMs) else "",
                style = MaterialTheme.typography.titleLarge, color = PracticeTheme.colors.recording,
                modifier = Modifier.testTag("recording-elapsed"))
            if (vm.phrasing && vm.countInBeat == 0) Text(if (vm.phraseClickEnabled) "Recording click on" else "Recording click off", modifier = Modifier.testTag("phrase-click-status"))
            Button(onClick = { vm.finishCapture() }, enabled = vm.ready && !vm.busy,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text(if (vm.phrasing) "Cancel phrase" else "Stop and review take") }
        }
        if (vm.reviewTake != null) TakeReviewControls(vm, onBeforePlayback)
        LazyColumn(Modifier.fillMaxWidth().weight(1f).testTag("take-list"),
            contentPadding = PaddingValues(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text(if (blockId == null) "Free recordings" else "Block recordings", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
                Text(blockName, style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = ::requestClose, modifier = Modifier.heightIn(min = 48.dp)) { Text("Close recordings") }
                timer?.current?.let { placement ->
                    val awaiting = placement.activeMs >= placement.configuredMs && !placement.overtimeAccepted
                    val value = if (placement.overtimeAccepted) "+" + practiceTime(placement.activeMs - placement.configuredMs)
                        else practiceTime(placement.configuredMs - placement.activeMs, roundUp = true)
                    val timerState = if (practice.running) "Running" else if (practice.preparing) "Prep" else "Paused"
                    Text("Practice timer · $value · $timerState", style = MaterialTheme.typography.bodyMedium)
                    OutlinedButton(onClick = { if (practice.running) practice.pause() else practice.resume(overtime = awaiting) }, enabled = !practice.busy,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Text(if (practice.preparing) "Start Practice" else if (practice.running) "Pause timer" else if (awaiting) "Continue timer" else if (placement.activeMs == 0L) "Start timer" else "Resume timer")
                    }
                }
            }
            item {
                if (!vm.recording && vm.reviewTake == null) {
                Button(onClick = {
                    if (vm.recording) vm.finishCapture()
                    else if (vm.hasMicrophonePermission()) vm.startCapture(blockId)
                    else permission.launch(Manifest.permission.RECORD_AUDIO)
                }, enabled = vm.ready && !vm.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                    Text(if (vm.phrasing) "Cancel phrase" else if (vm.recording) "Stop and review take" else "Record take")
                }
                Text("Free recording · no BPM required. A running click may be captured. Stop to review, then Save to keep the take. Unsaved temporary audio is removed after process loss.", style = MaterialTheme.typography.bodySmall)
                if (blockId != null) {
                OutlinedButton(onClick = { keyboard?.hide(); phraseDialog = true }, enabled = vm.ready && !vm.busy && !vm.recording && vm.reviewTake == null,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Record phrase") }
                Text("Your selected count-in, then one fixed-tempo phrase. A complete phrase opens Review Take before saving. Leaving cancels an incomplete phrase.", style = MaterialTheme.typography.bodySmall)
                }
                if (takes.any { it.id == vm.playingId && it.isLoop }) {
                    Button(onClick = vm::stopPlayback, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Stop loop") }
                }
                if (vm.busy) Text("Working…")
                }
            }
            item(key = "block-reference") {
                if (blockId != null && references != null) PracticeImageReference(blockId, "recording-$blockId", references)
            }
            item {
                Text("JSON backups exclude recordings. Export each take to keep a copy outside this app. Uninstalling or clearing the app loses unexported audio.", style = MaterialTheme.typography.bodySmall)
                Text("Saved takes (${takes.size})", style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
                if (takes.isEmpty()) Text(if (blockId == null) "No saved free recordings yet." else "No saved takes for this Block yet.")
            }
            items(takes, key = { it.id }) { take ->
                Column(Modifier.fillMaxWidth().testTag("take-" + take.id), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val title = takeTitle(take)
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text((takeDescription(take) + " · ") + takeDuration(take) + " · " + sessionDate(take.createdAt), style = MaterialTheme.typography.bodySmall)
                    val playing = vm.playingId == take.id || vm.preparingId == take.id
                    if (vm.preparingId == take.id) Text("Preparing playback…")
                    OutlinedButton(onClick = {
                        if (playing) vm.stopPlayback() else { onBeforePlayback(); vm.play(take) }
                    }, enabled = !vm.busy && !vm.recording && vm.reviewTake == null, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .testTag("play-" + take.id).semantics { contentDescription = (if (playing) "Stop " else "Play ") + title }) {
                        Text(if (playing) "Stop playback" else if (take.isLoop) "Play loop" else "Play take")
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = { renameId = take.id; label = take.label }, enabled = !vm.busy && !vm.recording && vm.reviewTake == null,
                            modifier = Modifier.heightIn(min = 48.dp).testTag("rename-" + take.id).semantics { contentDescription = "Rename $title" }) { Text("Rename") }
                        TextButton(onClick = {
                            destination = vm.exportStorage.selected()
                            folderFeedback = null
                            if (destination == null) saveAs(take) else exportOptionsId = take.id
                        }, enabled = !vm.busy && !vm.recording && vm.reviewTake == null,
                            modifier = Modifier.heightIn(min = 48.dp).testTag("export-" + take.id).semantics { contentDescription = "Export $title" }) { Text("Export audio") }
                        TextButton(onClick = { deleteId = take.id }, enabled = !vm.busy && !vm.recording && vm.reviewTake == null,
                            modifier = Modifier.heightIn(min = 48.dp).testTag("delete-" + take.id).semantics { contentDescription = "Delete $title" }) { Text("Delete") }
                    }
                    HorizontalDivider()
                }
            }
        }
      }
    }
    BackHandler { requestClose() }
    takes.find { it.id == exportOptionsId }?.let { take ->
        AlertDialog(
            onDismissRequest = { exportOptionsId = null },
            title = { Text("Export audio") },
            text = {
                Column {
                    Text(destination?.label ?: "No export folder selected.")
                    folderFeedback?.let { Text(it) }
                    TextButton(onClick = chooseFolder) { Text("Choose Folder") }
                    TextButton(onClick = {
                        exportOptionsId = null
                        saveAs(take)
                    }) { Text("Save As…") }
                }
            },
            confirmButton = {
                val selected = destination
                if (selected != null) TextButton(enabled = !vm.busy, onClick = {
                    exportOptionsId = null
                    vm.exportOrganized(take, selected, blockName)
                }) { Text("Export to Selected Folder") }
            },
            dismissButton = { TextButton(onClick = { exportOptionsId = null }) { Text("Cancel") } }
        )
    }
    if (confirmDiscard) AlertDialog(onDismissRequest = { confirmDiscard = false }, title = { Text("Discard temporary take?") },
        text = { Text("This take has not been saved. Return to review it, or discard only this temporary audio before closing.") },
        confirmButton = { TextButton(onClick = { vm.discardReview(); confirmDiscard = false; onDismiss() }, enabled = !vm.busy) { Text("Discard and close") } },
        dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Return to review") } })
    if (phraseDialog) {
        blockId?.let { PhraseSetupDialog(it, vm, initialBpm, onBeforePlayback, { permission.launch(Manifest.permission.RECORD_AUDIO) }, { phraseDialog = false }) }
    }

    all.find { it.id == renameId }?.let { take ->
        AlertDialog(onDismissRequest = { renameId = null }, title = { Text("Rename take") }, text = {
            OutlinedTextField(label, { label = it }, label = { Text("Take label (optional)") }, isError = label.trim().length > 120,
                supportingText = { Text("Up to 120 characters. Leave blank to use the date.") })
        }, confirmButton = { TextButton(onClick = { vm.rename(take, label); renameId = null }, enabled = !vm.busy && label.trim().length <= 120) { Text("Save take label") } },
            dismissButton = { TextButton(onClick = { renameId = null }) { Text("Cancel") } })
    }
    all.find { it.id == deleteId }?.let { take ->
        AlertDialog(onDismissRequest = { deleteId = null }, title = { Text("Delete this take?") },
            text = { Text("Delete " + takeTitle(take) + " and its audio? This cannot be undone. Other takes and practice history are kept.") },
            confirmButton = { TextButton(onClick = { vm.delete(take); deleteId = null }, enabled = !vm.busy) { Text("Delete take permanently") } },
            dismissButton = { TextButton(onClick = { deleteId = null }) { Text("Keep take") } })
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable fun PhraseSetupDialog(blockId: String, vm: RecordingViewModel, initialBpm: Int, onBeforePlayback: () -> Unit, requestPermission: () -> Unit, onDismiss: () -> Unit, inline: Boolean = false, minimumHeight: androidx.compose.ui.unit.Dp = 0.dp) {
    val keyboard = LocalSoftwareKeyboardController.current
    var phraseBpm by rememberSaveable(blockId) { mutableStateOf(initialBpm.coerceIn(30, 300).toString()) }
    var phraseBeats by rememberSaveable(blockId) { mutableStateOf("8") }
    var countIn by rememberSaveable(blockId) { mutableStateOf(vm.defaultCountIn.toString()) }
    val bpm = phraseBpm.toIntOrNull()
    val length = phraseBeats.toIntOrNull()
    val intro = countIn.toLongOrNull()
    val result = runCatching {
        require(bpm != null) { "Enter a whole BPM from 30 to 300." }
        require(length != null) { "Enter a whole loop length from 1 to 64 beats." }
        require(intro != null) { "Enter a positive whole count-in beat count." }
        PhraseSpec(bpm, length, intro)
    }
    val spec = result.getOrNull()
    val review = vm.reviewTake?.takeIf { it.kind == TakeKind.RECORDED_LOOP }
    val largeText = androidx.compose.ui.platform.LocalDensity.current.fontScale >= 1.5f
    var headingHeight by remember { mutableIntStateOf(0) }
    LaunchedEffect(review?.id) {
        review?.let {
            phraseBpm = it.recordedBpm.toString(); phraseBeats = it.beatCount.toString()
            countIn = vm.countInTotal.toString()
        }
    }
    var backing by rememberSaveable(blockId) { mutableStateOf(false) }
    val importMode = inline && backing && review == null
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> vm.chooseBackingTrack(blockId, uri) }
    val editable = !vm.recording && review == null && !vm.busy
    val musicalEditable = editable && !importMode
    val form: @Composable BoxWithConstraintsScope.() -> Unit = {
        val availableWidth = maxWidth
        Column(if (inline) Modifier.heightIn(min = (minimumHeight - with(androidx.compose.ui.platform.LocalDensity.current) {
            headingHeight.toDp()
        }).coerceAtLeast(0.dp)) else Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = if (inline) Arrangement.SpaceBetween else Arrangement.Top) {
            val labelStyle = MaterialTheme.typography.labelSmall
            val measurer = rememberTextMeasurer()
            val density = LocalDensity.current
            val fieldWidth = with(density) {
                maxOf(measurer.measure("Loop beats", labelStyle).size.width,
                    measurer.measure("27000", MaterialTheme.typography.bodyMedium).size.width).toDp()
            } + 36.dp
            val fieldColumns = ((availableWidth + 6.dp) / (fieldWidth + 6.dp)).toInt().coerceIn(1, 3)
            val fieldCellWidth = with(density) {
                ((availableWidth.roundToPx() - 6.dp.roundToPx() * (fieldColumns - 1)) / fieldColumns).toDp()
            }
            FlowRow(Modifier.fillMaxWidth().testTag("phrase-fields"), maxItemsInEachRow = fieldColumns,
                horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                PhraseNumberField(phraseBpm, { phraseBpm = it }, "BPM", "Phrase BPM", "phrase-bpm",
                    bpm == null || bpm !in 30..300, musicalEditable, Modifier.width(fieldCellWidth))
                PhraseNumberField(phraseBeats, { phraseBeats = it }, "Loop beats", "Loop beats", "phrase-beats",
                    length == null || length !in 1..64, musicalEditable, Modifier.width(fieldCellWidth))
                PhraseNumberField(countIn, { countIn = it }, "Count-in", "Count-in beats", "phrase-count-in",
                    intro == null || intro <= 0, musicalEditable, Modifier.width(fieldCellWidth))
            }
            val shortcutWidth = with(density) { measurer.measure("48 beats", labelStyle).size.width.toDp() } + 24.dp
            val shortcutColumns = ((availableWidth + 6.dp) / (shortcutWidth + 6.dp)).toInt().coerceIn(1, 4)
            listOf(listOf(4, 8, 16, 32), listOf(6, 12, 24, 48)).forEach { row ->
                FlowRow(Modifier.fillMaxWidth(), maxItemsInEachRow = shortcutColumns,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { beats ->
                        FilterChip(selected = phraseBeats == beats.toString(), onClick = { phraseBeats = beats.toString() },
                            enabled = musicalEditable, label = { Text("$beats beats", style = labelStyle) },
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("phrase-shortcut-$beats"))
                    }
                }
            }
            Row(Modifier.fillMaxWidth().testTag("phrase-duration"), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text(if (importMode) vm.backingTrack?.imported?.let { "Duration: " + importedDuration(it.durationMs) } ?: "Choose audio to validate its duration."
                    else spec?.let { "One phrase: " + String.format(java.util.Locale.getDefault(), "%.2f seconds", it.durationMs / 1000.0) }
                        ?: (result.exceptionOrNull()?.message ?: "Check loop settings."),
                    color = if (!importMode && spec == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                if (inline && review == null) Row(Modifier.weight(1f, fill = false).heightIn(min = 48.dp)
                    .toggleable(value = backing, enabled = editable, role = Role.Checkbox, onValueChange = { backing = it })
                    .testTag("backing-track-toggle").semantics { contentDescription = "Add Backing Track" },
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("Add Backing Track", Modifier.weight(1f, fill = false),
                        style = labelStyle, color = MaterialTheme.colorScheme.primary)
                    Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                        Checkbox(checked = backing, onCheckedChange = null, enabled = editable)
                    }
                }
            }
            // Keep recording/review and its metadata together against the transport boundary.
            Column(Modifier.fillMaxWidth().testTag("phrase-footer")) {
                if (importMode) {
                    Column(Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(onClick = { keyboard?.hide(); picker.launch(arrayOf("audio/mpeg", "audio/mp4", "audio/x-m4a", "audio/wav", "audio/x-wav")) },
                                enabled = editable && vm.ready, modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("backing-track-choose")) { Text(vm.backingTrack?.imported?.name ?: "Choose file…", maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.testTag("backing-track-filename")) }
                            Button(onClick = { vm.saveBackingTrack(blockId) }, enabled = editable && vm.ready && vm.backingTrack?.blockId == blockId,
                                modifier = Modifier.heightIn(min = 48.dp).testTag("backing-track-save")) { Text("Save") }
                        }
                    }
                } else if (inline && review != null) {
                    Row(Modifier.fillMaxWidth().testTag("phrase-primary-actions"), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = vm::saveReview, enabled = !vm.busy,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("review-save")) { Text("Keep") }
                        OutlinedButton(onClick = { if (vm.hasMicrophonePermission()) vm.rerecordReview() else requestPermission() }, enabled = !vm.busy,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("review-rerecord")) { Text("Re-record") }
                    }
                } else Button(onClick = {
                    if (vm.recording) vm.finishCapture()
                    else if (spec != null) {
                        keyboard?.hide()
                        if (!inline) onDismiss()
                        if (vm.hasMicrophonePermission()) { onBeforePlayback(); vm.startPhrase(blockId, spec) }
                        else requestPermission()
                    }
                }, enabled = vm.ready && !vm.busy && (vm.phrasing || (editable && spec != null)),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("phrase-start")) {
                    Text(if (vm.phrasing) {
                        if (vm.countInBeat > 0) "Cancel · Count-in ${vm.countInBeat} of ${vm.countInTotal}"
                        else "Cancel · Recording " + practiceTime(vm.elapsedMs)
                    } else if (inline) "Record Loop" else "Start count-in")
                }
                FlowRow(Modifier.fillMaxWidth().heightIn(min = 24.dp).testTag("phrase-budget-row"),
                    itemVerticalAlignment = Alignment.CenterVertically) {
                    Text(if (importMode) "MP3 / M4A / PCM16 WAV · 50 MiB / 15 min" else "Execution budget: 15 minutes.", style = labelStyle, modifier = if (largeText) Modifier.fillMaxWidth() else Modifier.weight(1f))
                    if (inline && review != null) {
                        val playing = vm.playingId == review.id || vm.preparingId == review.id
                        TextButton(onClick = { if (playing) vm.stopPlayback() else { onBeforePlayback(); vm.previewReview() } }, enabled = !vm.busy,
                            contentPadding = PaddingValues(horizontal = 4.dp), modifier = Modifier.heightIn(min = 48.dp).testTag("review-preview")) {
                            Text(if (playing) "Stop" else "Preview", style = MaterialTheme.typography.labelSmall)
                        }
                        TextButton(onClick = vm::discardReview, enabled = !vm.busy, contentPadding = PaddingValues(horizontal = 4.dp),
                            modifier = Modifier.heightIn(min = 48.dp).testTag("review-discard")) { Text("Discard", style = MaterialTheme.typography.labelSmall) }
                    }
                }
            }
        }
    }
    if (inline) Column(Modifier.fillMaxWidth().testTag("practice-loop-panel")) {
        Text("Loop setup", style = MaterialTheme.typography.titleSmall, modifier = Modifier.onSizeChanged { headingHeight = it.height }.semantics { heading() })
        BoxWithConstraints(Modifier.fillMaxWidth(), content = form)
    } else AlertDialog(onDismissRequest = onDismiss, title = { Text("Record one phrase") },
        text = { BoxWithConstraints(Modifier.fillMaxWidth(), content = form) },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable private fun PhraseNumberField(value: String, onChange: (String) -> Unit, label: String,
    description: String, tag: String, error: Boolean, enabled: Boolean, modifier: Modifier) {
    val keyboard = LocalSoftwareKeyboardController.current
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().testTag("$tag-label"))
        OutlinedTextField(value, onChange, enabled = enabled, singleLine = true, isError = error,
            textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { keyboard?.hide() }),
            modifier = Modifier.fillMaxWidth().testTag(tag).semantics { contentDescription = description })
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable fun CompactRecordingControls(blockId: String, takes: List<BlockTake>, freeTake: BlockTake?, loopTake: BlockTake?,
    vm: RecordingViewModel, onBeforePlayback: () -> Unit, onLoopSetup: () -> Unit, onSelectType: (Boolean) -> Unit) {
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission(), vm::permissionResult)
    val action = MaterialTheme.colorScheme.primary
    val colors = PracticeTheme.colors
    listOf(false, true).forEach { loop ->
        val selected = if (loop) loopTake else freeTake
        val type = if (loop) "loop" else "free"
        val activeCapture = vm.recording && vm.phrasing == loop
        val playing = takes.any { it.isLoop == loop && (it.id == vm.playingId || it.id == vm.preparingId) }
        val enabled = vm.ready && !vm.busy && vm.reviewTake == null
        val selector: @Composable (Modifier) -> Unit = { modifier ->
            Column(modifier.heightIn(min = 48.dp).clickable(enabled = enabled && !vm.recording && selected != null,
                role = Role.Button, onClick = { onSelectType(loop) }).padding(vertical = 4.dp)
                .testTag("practice-$type-selector").semantics { contentDescription = "Select $type recording" },
                verticalArrangement = Arrangement.Center) {
                Text(if (loop) "Loop Recording" else "Free Recording", style = MaterialTheme.typography.labelSmall, color = action)
                Row(Modifier.padding(end = 16.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text(if (activeCapture) { if (vm.countInBeat > 0) "Count-in · ${vm.countInBeat} of ${vm.countInTotal}" else "Recording · " + practiceTime(vm.elapsedMs) } else selected?.let(::compactPracticeTakeTitle) ?: "No recordings", Modifier.weight(1f),
                        maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = action)
                    Icon(painterResource(R.drawable.ic_expand_more), contentDescription = null,
                        modifier = Modifier.size(24.dp).testTag("practice-$type-chevron"), tint = action)
                }
            }
        }
        val actions: @Composable () -> Unit = {
            OutlinedButton(onClick = {
                if (activeCapture) vm.finishCapture()
                else if (loop) onLoopSetup()
                else if (vm.hasMicrophonePermission()) vm.startCapture(blockId)
                else permission.launch(Manifest.permission.RECORD_AUDIO)
            }, enabled = enabled && (!vm.recording || activeCapture), contentPadding = PaddingValues(horizontal = 6.dp),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = if (activeCapture) colors.recording else androidx.compose.ui.graphics.Color.Transparent,
                    contentColor = if (activeCapture) colors.onRecording else action),
                modifier = Modifier.then(if (androidx.compose.ui.platform.LocalDensity.current.fontScale >= 1.5f) Modifier.widthIn(min = 104.dp) else Modifier.width(104.dp)).heightIn(min = 48.dp).testTag("practice-$type-record")) {
                Text(if (activeCapture) { if (loop) "Cancel" else "Stop" } else if (loop) "Loop Setup" else "Record")
            }
            TextButton(onClick = { if (playing) vm.stopPlayback() else selected?.let { onBeforePlayback(); vm.play(it) } },
                enabled = enabled && !vm.recording && selected != null, contentPadding = PaddingValues(horizontal = 6.dp),
                colors = ButtonDefaults.textButtonColors(containerColor = if (playing) action else androidx.compose.ui.graphics.Color.Transparent,
                    contentColor = if (playing) MaterialTheme.colorScheme.onPrimary else action),
                modifier = Modifier.then(if (androidx.compose.ui.platform.LocalDensity.current.fontScale >= 1.5f) Modifier.widthIn(min = 56.dp) else Modifier.width(56.dp)).heightIn(min = 48.dp).testTag("practice-$type-play").semantics {
                    contentDescription = (if (playing) "Stop" else "Play") + " selected $type recording"
                    stateDescription = if (playing) "Playing" else "Stopped"
                }) { Text(if (playing) "Stop" else "Play") }
        }
        if (androidx.compose.ui.platform.LocalDensity.current.fontScale >= 1.5f) Column(Modifier.fillMaxWidth()) {
            selector(Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp, androidx.compose.ui.Alignment.End)) { actions() }
        } else Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)) { selector(Modifier.weight(1f)); actions() }
        RecordingScrubber(selected, type, vm, enabled && !vm.recording)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable fun TakeReviewControls(vm: RecordingViewModel, onBeforePlayback: () -> Unit) {
    val take = vm.reviewTake ?: return
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission(), vm::permissionResult)
    val playing = vm.playingId == take.id || vm.preparingId == take.id
    Column(Modifier.fillMaxWidth().testTag("take-review"), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Review Take", style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        Text(takeDescription(take) + " · " + practiceTime(take.durationMs) + " · Not saved", style = MaterialTheme.typography.bodySmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { if (playing) vm.stopPlayback() else { onBeforePlayback(); vm.previewReview() } }, enabled = !vm.busy,
                modifier = Modifier.heightIn(min = 48.dp).testTag("review-preview")) { Text(if (playing) "Stop Preview" else "Play Preview") }
            Button(onClick = vm::saveReview, enabled = !vm.busy, modifier = Modifier.heightIn(min = 48.dp).testTag("review-save")) { Text("Save") }
            OutlinedButton(onClick = { if (vm.hasMicrophonePermission()) vm.rerecordReview() else permission.launch(Manifest.permission.RECORD_AUDIO) },
                enabled = !vm.busy, modifier = Modifier.heightIn(min = 48.dp).testTag("review-rerecord")) { Text("Re-record") }
            TextButton(onClick = vm::discardReview, enabled = !vm.busy, modifier = Modifier.heightIn(min = 48.dp).testTag("review-discard")) { Text("Discard") }
        }
        Text("Save keeps this take. Re-record replaces only the temporary take. Discard keeps all earlier recordings.", style = MaterialTheme.typography.bodySmall)
    }
}
