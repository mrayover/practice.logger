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
    val enabled = draft.ready && !busy && pendingPhoto == null
    Text("References", style = MaterialTheme.typography.titleMedium)
    for (role in MediaRole.entries) {
        val asset = if (role == MediaRole.VISUAL) draft.visual else draft.audio
        val label = if (role == MediaRole.VISUAL) "Visual" else "Audio"
        val oldVisual = role == MediaRole.VISUAL && legacy != null && !draft.visualChanged
        Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelMedium)
                if (asset != null && role == MediaRole.AUDIO && draft.isStaged(asset)) Text("${asset.originalName} · audio",
                    modifier = Modifier.testTag("block-AUDIO-title"), style = MaterialTheme.typography.bodyMedium)
                else if (asset != null) TextButton(onClick = { if (role == MediaRole.VISUAL) preview = asset else audioPreview = asset }, enabled = enabled,
                    contentPadding = PaddingValues(0.dp), modifier = Modifier.testTag("block-${role.name}-title")) {
                    Text("${asset.originalName} · ${asset.category.lowercase()}")
                } else if (oldVisual) TextButton(onClick = { legacyPreview = true }, enabled = enabled) {
                    Text("Block-owned ${if (legacy?.mimeType == "application/pdf") "PDF" else "image"}")
                } else Text("None selected", style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = {
                if (asset != null || oldVisual) confirm = role.name else options = role.name
            }, enabled = enabled, modifier = Modifier.testTag("block-${role.name}-choose")) { Text(if (asset != null || oldVisual) "Change" else "Add") }
        }
    }
    Text("One image or PDF, and one audio. Changes apply with Save Block.", style = MaterialTheme.typography.bodySmall)
    if (vm.busy) Text("Preparing references…")
    vm.message?.let { Text(it, color = MaterialTheme.colorScheme.error,
        modifier = Modifier.testTag("block-media-message").semantics { liveRegion = LiveRegionMode.Polite }) }
    confirm?.let { role ->
        AlertDialog(onDismissRequest = { confirm = null }, title = { Text("Change ${role.lowercase()} reference?") },
            text = { Text("Replacing or removing this reference on Save discards this Block’s markings or local playback settings for it. Shared Library originals and other Blocks are kept. An old Block-owned original is retired only after Save succeeds.") },
            confirmButton = { TextButton(onClick = { confirm = null; options = role }) { Text("Continue") } },
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
                TextButton(onClick = { options = null; vm.editorRemove(role) }, modifier = Modifier.testTag("block-${role.name}-remove")) { Text("Remove reference") }
            } }, confirmButton = { TextButton(onClick = { options = null }) { Text("Cancel") } })
    }
    selection?.let { value ->
        val role = MediaRole.valueOf(value)
        LibraryMediaPicker(role, (if (role == MediaRole.VISUAL) draft.visual else draft.audio)?.id,
            onClose = { selection = null }, onChoose = { selection = null; vm.editorChoose(role, it) })
    }
    preview?.let { asset -> EditorVisualPreview(asset, vm, onClose = { preview = null }) }
    audioPreview?.let { asset ->
        Dialog(onDismissRequest = { audioPreview = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(Modifier.fillMaxSize()) { LibraryAudioScreen(asset, { audioPreview = null }) }
        }
    }
    if (legacyPreview && legacy != null) Dialog(onDismissRequest = { legacyPreview = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize()) { Column(Modifier.safeDrawingPadding().verticalScroll(androidx.compose.foundation.rememberScrollState()).padding(16.dp)) {
            TextButton(onClick = { legacyPreview = false }) { Text("Close preview") }
            CollapsibleImageReference(legacy, vm, embeddedExpanded = true, active = false)
        } }
    }
}

@Composable
private fun EditorVisualPreview(asset: MediaAsset, vm: ImageReferenceViewModel, onClose: () -> Unit) {
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
