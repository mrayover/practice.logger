package com.mrayover.practicelogger

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun SessionBuilder(blocks: List<Block>, draft: SessionBundle?, vm: PracticeViewModel,
    routines: RoutineViewModel, onBack: () -> Unit, onCreate: () -> Unit) {
    var picking by rememberSaveable { mutableStateOf(false) }
    var suggested by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var changeId by rememberSaveable { mutableStateOf<String?>(null) }
    var minutes by rememberSaveable { mutableStateOf("") }
    var validation by rememberSaveable { mutableStateOf(false) }
    var discard by rememberSaveable { mutableStateOf(false) }
    val entries = draft?.ordered.orEmpty()
    val busy = vm.busy || routines.busy
    val indicators = LocalGoalIndicators.current
    var goalFilter by rememberBlockGoalFilter(indicators)
    val lineupScroll = rememberLazyListState()
    val pickerScroll = rememberLazyListState()
    val total = entries.sumOf { it.configuredMs }
    val duration = if (total % 60_000L == 0L) "${total / 60_000L} min" else practiceTime(total)
    val summary = "${entries.size} ${if (entries.size == 1) "Block" else "Blocks"} · $duration planned"
    BackHandler { if (picking) picking = false else onBack() }

    Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
        if (picking) {
            val recommendations = rememberBlockRecommendations(blocks)
            LazyColumn(Modifier.weight(1f).fillMaxWidth().testTag("builder-picker"), state = pickerScroll,
                contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    Text("Add Blocks", style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.semantics { heading() })
                }
                item {
                    OutlinedTextField(query, { query = it }, label = { Text("Find a Block") },
                        singleLine = true, modifier = Modifier.fillMaxWidth())
                }
                item { BlockGoalFilterPicker(goalFilter) { goalFilter = it } }
                item { SuggestedViewPicker(suggested) { suggested = it } }
                item {
                    OutlinedButton(onClick = { picking = false; onCreate() }, enabled = !busy,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Create a new Block") }
                }
                val matching = matchingBlocks(blocks, query, false, indicators, goalFilter)
                val reasons = recommendations.orEmpty().associateBy { it.blockId }
                val available = if (suggested) recommendations.orEmpty().mapNotNull { r -> matching.find { it.id == r.blockId } } else matching
                if (available.isEmpty()) item { Text(if (suggested) if (recommendations == null) "Suggestions unavailable. All Blocks remains available." else "No matching suggestions. All Blocks remains available." else "No matching active Blocks.") }
                items(available, key = { it.id }) { block ->
                    Column {
                        if (suggested) Text(reasons[block.id]!!.reasons.joinToString(" · "), style = MaterialTheme.typography.bodySmall)
                        BlockPill(block, indicators.block(block.id), emptyList(),
                            modifier = Modifier.testTag("builder-block-${block.id}"),
                            selection = {
                                TextButton(onClick = { vm.addBlock(block.id) }, enabled = !busy,
                                    modifier = Modifier.heightIn(min = 48.dp).testTag("builder-add-${block.id}")
                                        .semantics { contentDescription = "Add ${block.name}" }) { Text("Add") }
                            }, actions = {})
                        val count = entries.count { it.sourceBlockId == block.id }
                        if (count > 0) Text("Added $count to session", style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 16.dp).semantics { liveRegion = LiveRegionMode.Polite })
                    }
                }
            }
            Surface(shadowElevation = 4.dp) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(summary, style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                    ActionButton("Done adding Blocks", { picking = false }, enabled = !busy)
                }
            }
        } else {
            LazyColumn(Modifier.weight(1f).fillMaxWidth().testTag("builder-lineup"), state = lineupScroll,
                contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    Column {
                        Text("Build Session", style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.semantics { heading() })
                        Text(summary, modifier = Modifier.testTag("builder-summary"), style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (entries.isEmpty()) item { Text("Add Blocks to build your session.") }
                itemsIndexed(entries, key = { _, placement -> placement.id }) { index, placement ->
                    SessionPlacementPill(placement, index, entries.lastIndex, indicators.block(placement.sourceBlockId), busy,
                        onDuration = {
                            changeId = placement.id
                            minutes = (placement.configuredMs / 60_000L).toString()
                            validation = false
                        }, onMove = { vm.move(placement.id, it) }, onRemove = { vm.remove(placement.id) })
                }
                item {
                    OutlinedButton(onClick = { picking = true }, enabled = !busy,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("+ Add Blocks") }
                }
                item { SaveAsRoutine(draft, vm.busy, routines) }
                if (entries.isNotEmpty()) item {
                    TextButton(onClick = { discard = true }, enabled = !busy,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Discard draft session") }
                }
                item {
                    TextButton(onClick = onBack, enabled = !busy,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Back") }
                }
            }
            Surface(shadowElevation = 4.dp) {
                Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    ActionButton("Start Session", vm::start, enabled = !busy && entries.isNotEmpty())
                }
            }
        }
    }
    if (changeId != null) AlertDialog(onDismissRequest = { changeId = null }, title = { Text("Session duration") }, text = {
        Column {
            Text("Set the duration for this placement.")
            OutlinedTextField(minutes, { minutes = it }, label = { Text("Minutes") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), isError = validation,
                supportingText = { if (validation) Text("Enter positive whole minutes (up to 2147483647).") })
        }
    }, confirmButton = { TextButton(onClick = {
        if (minutes.trim().toIntOrNull()?.let { it > 0 } == true) {
            vm.duration(changeId!!, minutes)
            changeId = null
        } else validation = true
    }, enabled = !busy) { Text("Apply duration") } }, dismissButton = { TextButton(onClick = { changeId = null }) { Text("Cancel") } })
    if (discard) AlertDialog(onDismissRequest = { discard = false }, title = { Text("Discard this draft session?") },
        text = { Text("Saved Blocks and practice history are retained.") },
        confirmButton = { TextButton(onClick = { discard = false; vm.discardDraft() }, enabled = !busy) { Text("Discard draft") } },
        dismissButton = { TextButton(onClick = { discard = false }) { Text("Keep draft") } })
}

/** A saved session snapshot. Repeated source Blocks have independent placement IDs and state. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SessionPlacementPill(placement: Placement, index: Int, lastIndex: Int, goal: GoalIndicator?, busy: Boolean,
    onDuration: () -> Unit, onMove: (Int) -> Unit, onRemove: () -> Unit) {
    var expanded by rememberSaveable(placement.id) { mutableStateOf(false) }
    Surface(Modifier.fillMaxWidth().testTag("placement-$index"), shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
        Column {
            Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).testTag("placement-${placement.id}")
                .semantics { stateDescription = if (expanded) "Expanded" else "Collapsed" }
                .clickable(role = Role.Button, onClickLabel = if (expanded) "Collapse placement" else "Expand placement") { expanded = !expanded }
                .padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(8.dp).background(goalAccent(PracticeTheme.colors, goal) ?: MaterialTheme.colorScheme.outline)
                    .testTag("placement-goal-marker"))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("${index + 1}. ${placement.name}", style = MaterialTheme.typography.titleMedium,
                        maxLines = if (expanded) Int.MAX_VALUE else 2, overflow = TextOverflow.Ellipsis)
                    if (!expanded) Text(placement.action.lineSequence().first(), style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(practiceTime(placement.configuredMs) + (placement.tempo?.let { " · $it BPM" } ?: ""),
                        style = MaterialTheme.typography.bodySmall)
                }
                Text(if (expanded) "−" else "+", style = MaterialTheme.typography.titleMedium)
            }
            if (expanded) Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(placement.action)
                BlockGoalLabel(goal)
                if (placement.referenceNotes.isNotBlank()) {
                    Text("Notes / Source", style = MaterialTheme.typography.titleSmall)
                    Text(placement.referenceNotes)
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = onDuration, enabled = !busy, modifier = Modifier.heightIn(min = 48.dp)) { Text("Change duration") }
                    TextButton(onClick = { onMove(-1) }, enabled = !busy && index > 0,
                        modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = "Move ${placement.name}, placement ${index + 1}, up" }) { Text("Up") }
                    TextButton(onClick = { onMove(1) }, enabled = !busy && index < lastIndex,
                        modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = "Move ${placement.name}, placement ${index + 1}, down" }) { Text("Down") }
                    TextButton(onClick = onRemove, enabled = !busy, modifier = Modifier.heightIn(min = 48.dp)) { Text("Remove from Session") }
                }
            }
        }
    }
}
