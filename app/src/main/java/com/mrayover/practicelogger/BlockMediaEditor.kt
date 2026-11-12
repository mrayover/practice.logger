package com.mrayover.practicelogger

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.*

@OptIn(ExperimentalLayoutApi::class)
@Composable fun BlockImageEditor(blockId: String?, vm: ImageReferenceViewModel, busy: Boolean) {
    val draft = vm.editor
    var options by rememberSaveable { mutableStateOf<String?>(null) }
    var confirm by rememberSaveable { mutableStateOf<String?>(null) }
    var selection by rememberSaveable { mutableStateOf<String?>(null) }
    var importRole by rememberSaveable { mutableStateOf(MediaRole.VISUAL.name) }
    var pendingPhoto by rememberSaveable { mutableStateOf<String?>(null) }
    var preview by remember { mutableStateOf<MediaAsset?>(null) }
    var audioPreview by remember { mutableStateOf<MediaAsset?>(null) }
    var legacyPreview by remember { mutableStateOf(false) }
    val visuals by vm.visuals.collectAsStateWithLifecycle()
    val legacy = visuals.find { it.blockId == blockId && it.legacy != null }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.editorImport(MediaRole.valueOf(importRole), uri)
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        pendingPhoto?.let { if (saved) vm.editorPhoto(it) else vm.photoCapture.discard(it) }
        pendingPhoto = null
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.editorImport(MediaRole.VISUAL, uri)
    }
    val enabled = draft.ready && !busy && !vm.busy && pendingPhoto == null
    Text("References", style = MaterialTheme.typography.titleMedium)
    for (role in MediaRole.entries) {
        val items = draft.references(role)
        Text(if (role == MediaRole.VISUAL) "Visual References" else "Audio References", style = MaterialTheme.typography.labelLarge)
        items.forEachIndexed { index, item -> key(item.identity) {
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth().testTag("reference-${role.name}-${item.identity}")) {
                Column(Modifier.padding(horizontal = 8.dp)) {
                    Text(item.title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth().testTag(if (index == 0) "block-${role.name}-title" else "reference-title-${item.identity}"))
                    Text("${index + 1} of ${items.size} · ${item.type}" + if (index == 0) " · Default" else "", style = MaterialTheme.typography.labelSmall)
                    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        TextButton(onClick = {
                            if (item.legacy != null) legacyPreview = true
                            else if (role == MediaRole.VISUAL) preview = item.asset else audioPreview = item.asset
                        }, enabled = enabled, contentPadding = PaddingValues(horizontal = 8.dp),
                            modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = "Preview ${item.title}" }.testTag("reference-preview-${item.identity}")) { Text("Preview") }
                        TextButton(onClick = { vm.editorMove(role, item.identity, -1) }, enabled = enabled && index > 0,
                            contentPadding = PaddingValues(horizontal = 8.dp), modifier = Modifier.heightIn(min = 48.dp)
                                .semantics { contentDescription = "Move ${item.title} up" }.testTag("reference-up-${item.identity}")) { Text("Up") }
                        TextButton(onClick = { vm.editorMove(role, item.identity, 1) }, enabled = enabled && index < items.lastIndex,
                            contentPadding = PaddingValues(horizontal = 8.dp), modifier = Modifier.heightIn(min = 48.dp)
                                .semantics { contentDescription = "Move ${item.title} down" }.testTag("reference-down-${item.identity}")) { Text("Down") }
                        TextButton(onClick = {
                            if (item.asset?.let(draft::isStaged) == true || blockId == null) vm.editorRemove(role, item.identity, false)
                            else { importRole = role.name; confirm = item.identity }
                        }, enabled = enabled, contentPadding = PaddingValues(horizontal = 8.dp), modifier = Modifier.heightIn(min = 48.dp)
                            .semantics { contentDescription = "Remove ${item.title}" }.testTag("reference-remove-${item.identity}")) { Text("Remove") }
                    }
                }
            }
        } }
        TextButton(onClick = { options = role.name }, enabled = enabled,
            modifier = Modifier.testTag("block-${role.name}-choose").heightIn(min = 48.dp)) { Text("Add ${if (role == MediaRole.VISUAL) "Visual" else "Audio"} Reference") }
    }
    Text("First in each list is the default. Changes apply with Save Block.", style = MaterialTheme.typography.bodySmall)
    if (vm.busy) Text("Preparing references…")
    vm.message?.let { Text(it, color = MaterialTheme.colorScheme.error,
        modifier = Modifier.testTag("block-media-message").semantics { liveRegion = LiveRegionMode.Polite }) }
    confirm?.let { identity ->
        AlertDialog(onDismissRequest = { confirm = null }, title = { Text("Remove this reference?") },
            text = { Text("Save will remove only this attachment and its Block markings or local audio settings and markers. Library originals, master markers and other attachments are kept. Review its preview before removing saved work.") },
            confirmButton = { TextButton(onClick = { confirm = null; vm.editorRemove(MediaRole.valueOf(importRole), identity, true) }) { Text("Remove reference") } },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Keep reference") } })
    }
    options?.let { value ->
        val role = MediaRole.valueOf(value)
        AlertDialog(onDismissRequest = { options = null }, title = { Text("${value.lowercase().replaceFirstChar { it.uppercase() }} reference") },
            text = { Column(Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())) {
                TextButton(onClick = { options = null; selection = value }) { Text("Choose from Library") }
                TextButton(onClick = {
                    options = null; importRole = value
                    try { picker.launch(if (role == MediaRole.VISUAL) arrayOf("image/jpeg", "image/png", "image/webp", "application/pdf") else libraryAudioMimeTypes) }
                    catch (_: Exception) { vm.photoUnavailable() }
                }, modifier = Modifier.testTag("block-${role.name}-import")) { Text("Import New into Library") }
                if (role == MediaRole.VISUAL) {
                    TextButton(onClick = {
                        options = null
                        try { val name = vm.photoCapture.create(); pendingPhoto = name; camera.launch(vm.photoCapture.uri(name)) }
                        catch (_: Exception) { pendingPhoto?.let(vm.photoCapture::discard); pendingPhoto = null; vm.photoUnavailable() }
                    }) { Text("Take photo") }
                    TextButton(onClick = {
                        options = null
                        try { gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                        catch (_: Exception) { vm.photoUnavailable() }
                    }) { Text("Choose from gallery") }
                }
            } }, confirmButton = { TextButton(onClick = { options = null }) { Text("Cancel") } })
    }
    selection?.let { value ->
        val role = MediaRole.valueOf(value)
        LibraryMediaPicker(role, (if (role == MediaRole.VISUAL) draft.visual else draft.audio)?.id,
            onClose = { selection = null }, onChoose = { selection = null; vm.editorChoose(role, it) })
    }
    preview?.let { asset -> EditorVisualPreview(asset, blockId, vm, onClose = { preview = null }) }
    audioPreview?.let { asset -> EditorAudioPreview(asset, vm, onClose = { audioPreview = null }) }
    if (legacyPreview && legacy != null) Dialog(onDismissRequest = { legacyPreview = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize()) { Column(Modifier.safeDrawingPadding().verticalScroll(androidx.compose.foundation.rememberScrollState()).padding(16.dp)) {
            TextButton(onClick = { legacyPreview = false }) { Text("Close preview") }
            CollapsibleImageReference(legacy, vm, embeddedExpanded = true)
        } }
    }
}

@Composable
private fun EditorAudioPreview(asset: MediaAsset, vm: ImageReferenceViewModel, onClose: () -> Unit) {
    val owner = LocalLifecycleOwner.current
    var player by remember(asset.id) { mutableStateOf<android.media.MediaPlayer?>(null) }
    var playing by remember(asset.id) { mutableStateOf(false) }
    var preparing by remember(asset.id) { mutableStateOf(false) }
    var error by remember(asset.id) { mutableStateOf<String?>(null) }
    fun stop() { player?.release(); player = null; playing = false; preparing = false }
    DisposableEffect(owner, asset.id) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) stop() }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer); stop() }
    }
    Dialog(onDismissRequest = { stop(); onClose() }) {
        Surface(shape = MaterialTheme.shapes.large) {
            Column(Modifier.padding(16.dp).verticalScroll(androidx.compose.foundation.rememberScrollState())) {
                Text(asset.originalName, maxLines = 2, overflow = TextOverflow.Ellipsis)
                TextButton(onClick = {
                    if (playing || preparing) stop() else try {
                        stop(); error = null
                        val preview = android.media.MediaPlayer()
                        player = preview; preparing = true
                        preview.setAudioAttributes(android.media.AudioAttributes.Builder().setUsage(android.media.AudioAttributes.USAGE_MEDIA).setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC).build())
                        preview.setDataSource(vm.editor.audioFile(asset).absolutePath)
                        preview.setOnPreparedListener { if (player === it) { it.start(); playing = true; preparing = false } }
                        preview.setOnCompletionListener { if (player === it) stop() }
                        preview.setOnErrorListener { failed, _, _ -> if (player === failed) { stop(); error = "Could not preview this audio. Retry or select the original again." }; true }
                        preview.prepareAsync()
                    } catch (_: Exception) { stop(); error = "Could not preview this audio. Retry or select the original again." }
                }, modifier = Modifier.testTag("editor-audio-play")) { Text(if (playing || preparing) "Stop preview" else "Play preview") }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                TextButton(onClick = { stop(); onClose() }) { Text("Close preview") }
            }
        }
    }
}

@Composable
private fun EditorVisualPreview(asset: MediaAsset, blockId: String?, vm: ImageReferenceViewModel, onClose: () -> Unit) {
    val visuals by vm.visuals.collectAsStateWithLifecycle()
    val savedReference = visuals.find { it.blockId == blockId && it.mediaId == asset.id }
    if (savedReference != null) {
        Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(Modifier.fillMaxSize()) { Column(Modifier.safeDrawingPadding().verticalScroll(androidx.compose.foundation.rememberScrollState()).padding(16.dp)) {
                TextButton(onClick = onClose) { Text("Close preview") }
                CollapsibleImageReference(savedReference, vm, embeddedExpanded = true)
            } }
        }
        return
    }
    var page by rememberSaveable(asset.id) { mutableIntStateOf(0) }
    var loaded by remember(asset.id, page) { mutableStateOf<ReferencePage?>(null) }
    var error by remember(asset.id, page) { mutableStateOf(false) }
    LaunchedEffect(asset.id, page) {
        val owner = coroutineContext
        withContext(NonCancellable + Dispatchers.IO) {
            try {
                val result = vm.editor.preview(asset, page)
                if (owner.isActive) loaded = result else result.bitmap.recycle()
            } catch (_: Exception) { if (owner.isActive) error = true }
        }
    }
    DisposableEffect(loaded) { val owned = loaded; onDispose { owned?.bitmap?.recycle() } }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.safeDrawingPadding().padding(16.dp)) {
                TextButton(onClick = onClose) { Text("Close preview") }
                Text(asset.originalName)
                if (error) Text("Could not open this reference. Select the original again.")
                loaded?.let { result ->
                    if (result.pageCount > 1) {
                        Text("Page ${page + 1} of ${result.pageCount}")
                        Row {
                            TextButton(onClick = { page-- }, enabled = page > 0) { Text("Previous page") }
                            TextButton(onClick = { page++ }, enabled = page + 1 < result.pageCount) { Text("Next page") }
                        }
                    }
                    ZoomableReference(result.bitmap, 0.dp, asset.mimeType == "application/pdf")
                }
            }
        }
    }
}

internal val libraryAudioMimeTypes = arrayOf("audio/mpeg", "audio/mp4", "audio/x-m4a", "audio/wav", "audio/x-wav")

@Composable
internal fun LibraryMediaPicker(role: MediaRole, selectedId: String?, onClose: () -> Unit, allowAudioImport: Boolean = false, onChoose: (String) -> Unit) {
    val libraryVm: MediaLibraryViewModel = viewModel()
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize()) {
            MediaLibraryScreen(libraryVm, onBack = onClose, selectionRole = role, selectedId = selectedId, allowAudioImport = allowAudioImport, onChoose = onChoose)
        }
    }
}

@Composable
internal fun ConfirmBlockAudioChange(blockId: String, vm: ImageReferenceViewModel) {
    val change = vm.pendingAudioChange?.takeIf { it.block == blockId } ?: return
    val action = if (change.next == null) "Remove" else "Replace"
    AlertDialog(onDismissRequest = vm::cancelAudioChange,
        title = { Text("$action audio reference?") },
        text = { Text("This removes this Block's saved playback settings and local marker work for the old recording. The Library original and master markers are kept.") },
        confirmButton = { TextButton(onClick = vm::confirmAudioChange) { Text("$action reference") } },
        dismissButton = { TextButton(onClick = vm::cancelAudioChange) { Text("Cancel") } })
}
