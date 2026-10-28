package com.mrayover.practicelogger

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Embedded above Recent Practice, preserving the existing history destination. */
@Composable fun SavedRoutines(catalog: Catalog, vm: RoutineViewModel, practice: PracticeViewModel,
    onNew: () -> Unit, onEdit: (String) -> Unit) {
    val routines by vm.catalog.collectAsStateWithLifecycle()
    var deleteId by rememberSaveable { mutableStateOf<String?>(null) }
    OutlinedButton(onClick = onNew, enabled = !vm.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("New Routine") }
    when {
        routines.error != null -> Text(routines.error!!)
        !routines.ready -> Text("Loading Routines…")
        routines.bundles.isEmpty() -> Text("No saved Routines. Create one here or save a session selection as a Routine.")
    }
    val blocksById = remember(catalog.blocks) { catalog.blocks.associateBy { it.id } }
    routines.bundles.forEach { bundle ->
        val id = bundle.routine.id
        val unavailable = routineUnavailable(bundle.ordered.map { RoutineSelection(it.sourceBlockId, it.configuredMs) }, blocksById)
        Column(Modifier.fillMaxWidth().testTag("routine-" + id), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(bundle.routine.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
            Text("${bundle.placements.size} Block placements")
            if (unavailable.isNotEmpty()) Text(unavailable.joinToString("\n") + "\nEdit the Routine or restore its archived Blocks before starting.")
            Button(onClick = { practice.startRoutine(id) }, enabled = unavailable.isEmpty() && bundle.placements.isNotEmpty() && !practice.busy && !vm.busy,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("start-routine-" + id)
                    .semantics { contentDescription = "Practice " + bundle.routine.name }) { Text("Practice Routine") }
            OutlinedButton(onClick = { onEdit(id) }, enabled = !vm.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                .testTag("edit-routine-" + id).semantics { contentDescription = "Edit " + bundle.routine.name }) { Text("Edit Routine") }
            TextButton(onClick = { deleteId = id }, enabled = !vm.busy, modifier = Modifier.heightIn(min = 48.dp)
                .testTag("delete-routine-" + id).semantics { contentDescription = "Delete " + bundle.routine.name }) { Text("Delete Routine") }
            HorizontalDivider()
        }
    }
    vm.message?.let { Text(it, Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
    routines.bundles.find { it.routine.id == deleteId }?.let { bundle ->
        AlertDialog(onDismissRequest = { deleteId = null }, title = { Text("Delete this Routine?") },
            text = { Text("Delete ${bundle.routine.name}? Only the saved Routine is removed. Blocks, recordings and practice history are kept.") },
            confirmButton = { TextButton(onClick = { deleteId = null; vm.delete(bundle.routine.id) }, enabled = !vm.busy) { Text("Delete Routine permanently") } },
            dismissButton = { TextButton(onClick = { deleteId = null }) { Text("Keep Routine") } })
    }
}

@Composable fun RoutineEditor(blocks: List<Block>, vm: RoutineViewModel, onRoutines: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var changing by rememberSaveable { mutableStateOf<Int?>(null) }
    var minutes by rememberSaveable { mutableStateOf("") }
    var discard by rememberSaveable { mutableStateOf(false) }
    fun leave() { if (!vm.busy) { if (vm.dirty) discard = true else onRoutines() } }
    BackHandler { leave() }
    Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TextButton(onClick = ::leave, enabled = !vm.busy) { Text("Back to Routines") }
        Text(if (vm.editorId == null) "New Routine" else "Edit Routine", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
        OutlinedTextField(vm.name, vm::rename, enabled = !vm.busy, label = { Text("Routine name") }, singleLine = true,
            isError = vm.name.isNotEmpty() && !routineNameValid(vm.name), supportingText = { Text("1–120 characters") }, modifier = Modifier.fillMaxWidth())
        Text("Saved durations belong to this Routine. Practice uses the current Block details and starts paused.")
        if (vm.entries.isEmpty()) Text("Add a Block below to begin.")
        vm.entries.forEachIndexed { index, row ->
            val block = blocks.find { it.id == row.blockId }
            val title = block?.name ?: "Unavailable Block (${row.blockId})"
            Column(Modifier.fillMaxWidth().testTag("routine-placement-$index"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${index + 1}. $title", style = MaterialTheme.typography.titleLarge)
                block?.let { Text(it.action) }
                if (block == null || block.archived) Text(if (block == null) "Unavailable — remove or replace this Block before practicing." else "Archived — restore or replace this Block before practicing.")
                Text("Routine duration: " + practiceTime(row.configuredMs))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { vm.move(index, -1) }, enabled = !vm.busy && index > 0,
                        modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = "Move $title, Routine placement ${index + 1}, up" }) { Text("Up") }
                    OutlinedButton(onClick = { vm.move(index, 1) }, enabled = !vm.busy && index < vm.entries.lastIndex,
                        modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = "Move $title, Routine placement ${index + 1}, down" }) { Text("Down") }
                }
                TextButton(onClick = { changing = index; minutes = (row.configuredMs / 60_000L).toString() }, enabled = !vm.busy,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Change duration") }
                TextButton(onClick = { vm.remove(index) }, enabled = !vm.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Remove") }
                HorizontalDivider()
            }
        }
        Button(onClick = vm::saveEditor, enabled = !vm.busy && routineNameValid(vm.name) && vm.entries.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text(if (vm.busy) "Saving…" else "Save Routine") }
        Text("Add Blocks", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(query, { query = it }, label = { Text("Find a Block") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        val available = matchingBlocks(blocks, query, false)
        if (available.isEmpty()) Text("No matching active Blocks. Create or restore Blocks in the Block Library.")
        available.forEach { block ->
            OutlinedButton(onClick = { vm.add(block) }, enabled = !vm.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                Column(Modifier.fillMaxWidth()) { Text("Add " + block.name); Text("${block.durationMinutes} min · ${block.action}", style = MaterialTheme.typography.bodyMedium) }
            }
        }
    }
    changing?.let { index ->
        val valid = minutes.trim().toIntOrNull()?.let { it > 0 } == true
        AlertDialog(onDismissRequest = { changing = null }, title = { Text("Routine duration") },
            text = { OutlinedTextField(minutes, { minutes = it }, label = { Text("Minutes") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), isError = !valid,
                supportingText = { Text("Positive whole minutes. Saved Block duration stays unchanged.") }) },
            confirmButton = { TextButton(onClick = { vm.duration(index, minutes); changing = null }, enabled = valid && !vm.busy) { Text("Apply duration") } },
            dismissButton = { TextButton(onClick = { changing = null }) { Text("Cancel") } })
    }
    if (discard) AlertDialog(onDismissRequest = { discard = false }, title = { Text("Discard Routine edits?") },
        text = { Text("Your saved Routine and Blocks stay unchanged.") },
        confirmButton = { TextButton(onClick = { discard = false; onRoutines() }) { Text("Discard edits") } },
        dismissButton = { TextButton(onClick = { discard = false }) { Text("Keep editing") } })
}

@Composable fun SaveAsRoutine(draft: SessionBundle?, practiceBusy: Boolean, vm: RoutineViewModel) {
    var open by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(vm.draftSaveResult) { if (vm.draftSaveResult != null) { open = false; vm.draftResultHandled() } }
    OutlinedButton(onClick = { name = ""; open = true }, enabled = !practiceBusy && !vm.busy && draft?.placements?.isNotEmpty() == true,
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Save as Routine") }
    vm.message?.let { Text(it, Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
    if (open) AlertDialog(onDismissRequest = { if (!vm.busy) open = false }, title = { Text("Save as Routine") },
        text = { Column {
            Text("Save this ordered selection and its durations. Your session draft and Blocks stay unchanged.")
            OutlinedTextField(name, { name = it }, label = { Text("Routine name") }, enabled = !vm.busy, singleLine = true,
                isError = name.isNotEmpty() && !routineNameValid(name), supportingText = { Text("1–120 characters") })
        } }, confirmButton = { TextButton(onClick = { vm.saveDraft(name) }, enabled = !vm.busy && !practiceBusy && routineNameValid(name)) { Text("Save Routine") } },
        dismissButton = { TextButton(onClick = { open = false }, enabled = !vm.busy) { Text("Cancel") } })
}
