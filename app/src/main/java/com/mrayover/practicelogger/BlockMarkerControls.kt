package com.mrayover.practicelogger

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private enum class MarkerAction(val title: String) {
    RESET("Reset to Master"), DELETE("Delete Local Marker"), ADD("Add to Master"), UPDATE("Update Master")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun BlockMarkerControls(block: String, media: String, duration: Long, ready: Boolean,
    markers: BlockMarkerSet, vm: LibraryAudioViewModel, jump: (MasterAudioMarker) -> Unit,
    content: @Composable (@Composable () -> Unit) -> Unit) {
    val scope = rememberCoroutineScope()
    var menu by remember { mutableStateOf<String?>(null) }
    var editor by remember { mutableStateOf<MasterAudioMarker?>(null) }
    var editing by remember { mutableStateOf<BlockMarker?>(null) }
    var confirmation by remember { mutableStateOf<Pair<MarkerAction, BlockMarker>?>(null) }
    var reviewed by remember { mutableStateOf<MasterAudioMarker?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    fun runChange(operation: suspend () -> Unit) {
        if (saving) return
        error = null
        saving = true
        scope.launch {
            try { operation() }
            catch (conflict: MasterMarkerConflict) { reviewed = conflict.current; error = conflict.message }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) { error = failure.message ?: "Could not save the marker. Try again." }
            finally { saving = false }
        }
    }

    fun create(section: Boolean) {
        val audio = vm.controller.state.value
        editing = null
        error = null
        vm.controller.pause()
        editor = MasterAudioMarker("", media, if (section) "SECTION" else "POINT",
            if (section) audio.aMs else audio.positionMs, audio.bMs.takeIf { section }, "", "")
    }

    content {
        Text("Markers and sections", style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            OutlinedButton(onClick = { create(false) }, enabled = ready && !saving) { Text("Add Marker at Cursor") }
            OutlinedButton(onClick = { create(true) }, enabled = ready && !saving) { Text("Save A–B as Section") }
        }
        if (markers.visible.isEmpty()) Text("No visible markers yet.", style = MaterialTheme.typography.bodySmall)
        if (editor == null && confirmation == null) error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        for (item in markers.visible + markers.hidden) key(item.marker.id) {
            val marker = item.marker
            val hidden = item.ownership == BlockMarkerOwnership.HIDDEN
            val selected = !hidden && vm.selectedMarkerId == marker.id
            Row(Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    if (hidden) Text(markerSummary(marker))
                    else TextButton(onClick = { jump(marker) }, enabled = ready,
                        modifier = Modifier.heightIn(min = 48.dp).testTag("master-jump-${marker.id}")) {
                        Text(markerSummary(marker))
                    }
                    Text(item.ownership.label + if (selected) " · Selected" else "", style = MaterialTheme.typography.bodySmall,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    if (!hidden && selected && marker.note.isNotBlank()) Text(marker.note, style = MaterialTheme.typography.bodySmall)
                }
                Box {
                    TextButton(onClick = { menu = marker.id }, enabled = !saving,
                        modifier = Modifier.testTag("block-marker-actions-${marker.id}")) { Text("Actions") }
                    DropdownMenu(menu == marker.id, onDismissRequest = { menu = null }) {
                        if (!hidden) {
                            marker.endMs?.let { end ->
                                DropdownMenuItem(text = { Text("Load section into A–B") }, enabled = ready, onClick = {
                                    menu = null
                                    if (vm.controller.range(marker.startMs, end)) vm.selectBlockMarker(marker.id)
                                })
                            }
                            val editLabel = when (item.ownership) {
                                BlockMarkerOwnership.MASTER -> "Edit for This Block"
                                BlockMarkerOwnership.CUSTOMIZED -> "Edit Local Version"
                                else -> "Edit"
                            }
                            DropdownMenuItem(text = { Text(editLabel) }, onClick = {
                                menu = null; error = null; vm.controller.pause(); editing = item; editor = marker
                            })
                        }
                        if (item.ownership == BlockMarkerOwnership.MASTER || item.ownership == BlockMarkerOwnership.CUSTOMIZED) {
                            DropdownMenuItem(text = { Text("Hide in This Block") }, onClick = {
                                menu = null
                                runChange { vm.blockAudio.saveMarker(block, media, item, marker, hide = true) }
                            })
                        }
                        val actions = when (item.ownership) {
                            BlockMarkerOwnership.MASTER -> emptyList()
                            BlockMarkerOwnership.CUSTOMIZED -> listOf(MarkerAction.RESET, MarkerAction.UPDATE)
                            BlockMarkerOwnership.LOCAL -> listOf(MarkerAction.DELETE, MarkerAction.ADD)
                            BlockMarkerOwnership.HIDDEN -> listOf(MarkerAction.RESET)
                        }
                        for (action in actions) DropdownMenuItem(text = { Text(action.title) }, onClick = {
                            menu = null; error = null; vm.controller.pause(); reviewed = null; confirmation = action to item
                        })
                    }
                }
            }
        }
    }
    editor?.let { draft ->
        AudioMarkerDialog(draft, LibraryAudioState(durationMs = duration), draft.endMs != null, "This Block", saving, error,
            close = { editor = null; error = null }) { a, b, label, note, done ->
            runChange {
                val id = vm.blockAudio.saveMarker(block, media, editing, draft.copy(startMs = a, endMs = b, label = label, note = note))
                vm.selectBlockMarker(id)
                done()
            }
        }
    }
    confirmation?.let { (action, item) ->
        val local = checkNotNull(item.local)
        val explanation = when (action) {
            MarkerAction.RESET -> "Discard this Block's customization and show the current Master."
            MarkerAction.DELETE -> "Permanently remove this marker from this Block."
            MarkerAction.ADD -> "Publish this marker to the Library and all Blocks using this recording."
            MarkerAction.UPDATE -> "Replace the Master with this Block's version. Other Blocks inheriting it will receive the update."
        }
        fun close() { confirmation = null; reviewed = null; error = null }
        AlertDialog(onDismissRequest = { if (!saving) close() }, title = { Text(action.title) }, text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(explanation)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                reviewed?.let { Text("Current Master\n${markerSummary(it)}\n${it.note}\n\nProposed Block version") }
                Text(markerSummary(item.marker))
                if (item.marker.note.isNotBlank()) Text(item.marker.note)
            }
        }, confirmButton = {
            TextButton(enabled = !saving, onClick = {
                runChange {
                    when (action) {
                        MarkerAction.RESET -> vm.blockAudio.discard(local, reset = true)
                        MarkerAction.DELETE -> {
                            vm.blockAudio.discard(local, reset = false)
                            if (vm.selectedMarkerId == local.id) vm.selectBlockMarker(null)
                        }
                        MarkerAction.ADD, MarkerAction.UPDATE -> vm.selectBlockMarker(vm.blockAudio.publish(local, reviewed))
                    }
                    close()
                }
            }) { Text(if (reviewed == null) action.title else "Replace reviewed Master") }
        }, dismissButton = { TextButton(onClick = ::close, enabled = !saving) { Text("Cancel") } })
    }
}

private fun markerSummary(marker: MasterAudioMarker): String =
    "${marker.label} · ${audioTimestamp(marker.startMs)}" + marker.endMs?.let { "–${audioTimestamp(it)}" }.orEmpty()
