package com.mrayover.practicelogger

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
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

@OptIn(ExperimentalLayoutApi::class)
@Composable fun BlockImageEditor(blockId: String?, vm: ImageReferenceViewModel, busy: Boolean) {
    Text("Library references", style = MaterialTheme.typography.titleMedium)
    Text("One image or PDF and one audio reference, independently. Library originals can be shared by Blocks. Media and associations are not included in JSON backups; keep your originals separately.", style = MaterialTheme.typography.bodySmall)
    if (blockId == null) { Text("Save this Block first, then reopen it to add references."); return }
    val links by vm.links.collectAsStateWithLifecycle()
    val assets by vm.assets.collectAsStateWithLifecycle()
    val visuals by vm.visuals.collectAsStateWithLifecycle()
    val visual = visuals.find { it.blockId == blockId }
    var selection by rememberSaveable(blockId) { mutableStateOf<String?>(null) }
    var importRole by rememberSaveable(blockId) { mutableStateOf(MediaRole.VISUAL.name) }
    var removal by rememberSaveable(blockId) { mutableStateOf<String?>(null) }
    var photoOptions by rememberSaveable(blockId) { mutableStateOf(false) }
    // Reuse the accepted private camera staging/provider and gallery contract.
    var pendingPhoto by rememberSaveable(blockId) { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.importLibrary(blockId, MediaRole.valueOf(importRole), uri)
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        pendingPhoto?.let { name -> if (saved) vm.importLibraryPhoto(blockId, name) else vm.photoCapture.discard(name) }
        pendingPhoto = null
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.importLibrary(blockId, MediaRole.VISUAL, uri)
    }
    val enabled = !busy && !vm.busy && pendingPhoto == null && vm.pendingVisualChange == null
    for (role in MediaRole.entries) {
        val label = if (role == MediaRole.VISUAL) "Visual" else "Audio"
        val link = links.find { it.blockId == blockId && it.role == role.name }
        val asset = assets.find { it.id == link?.mediaId }
        Text("$label reference", style = MaterialTheme.typography.titleMedium)
        if (asset != null) {
            Text(asset.title, modifier = Modifier.testTag("block-${role.name}-title"))
            LibraryMediaMetadata(asset)
        } else if (link != null) Text("Library reference unavailable. Replace or remove this association.", color = MaterialTheme.colorScheme.error)
        else if (role == MediaRole.VISUAL && visual?.legacy != null) Text("Existing Block-owned image/PDF. Kept until you confirm replacement or removal.")
        else Text("No ${label.lowercase()} reference attached.")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { selection = role.name }, enabled = enabled,
                modifier = Modifier.heightIn(min = 48.dp).testTag("block-${role.name}-choose")) { Text("Choose from Library") }
            OutlinedButton(onClick = {
                importRole = role.name
                try { picker.launch(if (role == MediaRole.VISUAL) arrayOf("image/jpeg", "image/png", "image/webp", "application/pdf")
                    else libraryAudioMimeTypes) }
                catch (_: Exception) { vm.photoUnavailable() }
            }, enabled = enabled, modifier = Modifier.heightIn(min = 48.dp).testTag("block-${role.name}-import")) { Text("Import New into Library") }
        }
        if (role == MediaRole.VISUAL) {
            OutlinedButton(onClick = { photoOptions = true }, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Take or Add a photo") }
            visual?.let { CollapsibleImageReference(it, vm) }
        }
        if (link != null || (role == MediaRole.VISUAL && visual != null)) OutlinedButton(onClick = { if (role == MediaRole.VISUAL) vm.unlink(blockId, role) else removal = role.name }, enabled = enabled,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("block-${role.name}-remove")) { Text("Remove ${label.lowercase()} reference") }
    }
    if (vm.busy) Text("Saving reference…")
    vm.message?.let { Text(it, modifier = Modifier.testTag("block-media-message").semantics { liveRegion = LiveRegionMode.Polite }) }
    selection?.let { name ->
        val role = MediaRole.valueOf(name)
        LibraryMediaPicker(role, links.find { it.blockId == blockId && it.role == name }?.mediaId,
            onClose = { selection = null }, onChoose = { id -> selection = null; vm.choose(blockId, role, id) })
    }
    vm.pendingVisualChange?.takeIf { it.block == blockId }?.let { change ->
        val replacing = change.next != null
        val title = when {
            !replacing -> "Remove visual reference?"
            change.legacy -> "Replace existing Block-owned reference?"
            else -> "Replace visual reference?"
        }
        val detail = if (change.legacy) {
            if (replacing) "Attaching the Library item retires the old Block-owned original. The Library item is kept if you cancel."
            else "Removing this reference deletes the old Block-owned original."
        } else "The Library original and other Blocks’ references are kept."
        AlertDialog(onDismissRequest = vm::cancelVisualChange, title = { Text(title) },
            text = { Text(if (change.reviewed.hasMarkings) "This discards this Block’s saved markings for the reference. $detail" else detail) },
            confirmButton = { TextButton(onClick = vm::confirmVisualChange, enabled = !vm.busy) {
                Text(if (replacing) "Replace old reference" else "Remove reference")
            } },
            dismissButton = { TextButton(onClick = vm::cancelVisualChange) {
                Text(if (replacing) "Keep old reference" else "Cancel")
            } })
    }
    ConfirmBlockAudioChange(blockId, vm)
    removal?.let { name ->
        val role = MediaRole.valueOf(name)
        AlertDialog(onDismissRequest = { removal = null }, title = { Text("Remove ${name.lowercase()} reference?") },
            text = { Text("Remove this Block's association? The Library original, master markers and other Blocks' links are kept.") },
            confirmButton = { TextButton(onClick = { removal = null; vm.unlink(blockId, role) }, enabled = !vm.busy) { Text("Remove reference") } },
            dismissButton = { TextButton(onClick = { removal = null }) { Text("Cancel") } })
    }
    if (photoOptions) AlertDialog(onDismissRequest = { photoOptions = false }, title = { Text("Take or Add a photo") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {
                photoOptions = false
                try { val name = vm.photoCapture.create(); pendingPhoto = name; camera.launch(vm.photoCapture.uri(name)) }
                catch (_: Exception) { pendingPhoto?.let { vm.photoCapture.discard(it) }; pendingPhoto = null; vm.photoUnavailable() }
            }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Take photo") }
            OutlinedButton(onClick = {
                photoOptions = false
                try { gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                catch (_: Exception) { vm.photoUnavailable() }
            }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Choose from gallery") }
        } }, confirmButton = { TextButton(onClick = { photoOptions = false }) { Text("Cancel") } })
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
