package com.mrayover.practicelogger

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun WeekWorkspace(
    week: LocalDate, isCurrent: Boolean, plans: List<WeekBlock>, catalog: Catalog,
    preparing: Boolean, vm: CatalogViewModel, practice: PracticeViewModel, hasDraft: Boolean,
    onNavigate: (Long) -> Unit, onBack: () -> Unit, onPrepare: () -> Unit, onCreate: () -> Unit
) {
    var selecting by rememberSaveable(week.toString()) { mutableStateOf(false) }
    var selected by rememberSaveable(week.toString()) { mutableStateOf(arrayListOf<String>()) }
    var carrying by rememberSaveable(week.toString()) { mutableStateOf(false) }
    var query by rememberSaveable(week.toString()) { mutableStateOf("") }
    val weekKey = week.toString()
    val blocks = catalog.blocks.associateBy { it.id }
    val assigned = plans.filter { it.weekStart == weekKey }.mapNotNull { blocks[it.blockId] }
    val assignedIds = assigned.mapTo(hashSetOf()) { it.id }
    val selectedIds = assigned.filter { !it.archived && it.id in selected }.map { it.id }
    val earlier = plans.asSequence()
        .filter { it.weekStart < weekKey && blocks[it.blockId]?.archived == false }
        .map { it.weekStart }.distinct().toList().asReversed()
    val indicators = LocalGoalIndicators.current
    var goalFilter by rememberBlockGoalFilter(indicators, week to preparing)
    val enabled = !vm.busy && !practice.busy

    BackHandler {
        if (selecting) { selecting = false; selected = arrayListOf() } else onBack()
    }
    LazyColumn(
        Modifier.fillMaxSize().safeDrawingPadding().imePadding().padding(horizontal = 24.dp).testTag("week-list"),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(if (preparing) "Prepare Week" else "This Week", style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() })
            Text(weekRange(week) + if (isCurrent) " · Current week" else "")
            if (!preparing) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onNavigate(-1) }, enabled = enabled) { Text("Previous week") }
                OutlinedButton(onClick = { onNavigate(1) }, enabled = enabled) { Text("Next week") }
            }
        }
        if (preparing) {
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onCreate, enabled = enabled) { Text("Create a new Block") }
                    OutlinedButton(onClick = { carrying = true }, enabled = enabled && earlier.isNotEmpty()) {
                        Text("Carry forward")
                    }
                    TextButton(onClick = onBack) { Text("Done") }
                }
                Text("Assigned Blocks", style = MaterialTheme.typography.titleLarge)
            }
        } else {
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onPrepare, enabled = enabled) { Text("Prepare Week") }
                    OutlinedButton(onClick = { selecting = !selecting; selected = arrayListOf() },
                        enabled = enabled && (selecting || assigned.any { !it.archived })) {
                        Text(if (selecting) "Cancel selection" else "Select Blocks")
                    }
                }
                if (selecting) ActionButton(
                    if (hasDraft) "Add to draft session (${selectedIds.size})" else "Build session (${selectedIds.size})",
                    click = { practice.addWeekBlocks(selectedIds) }, enabled = enabled && selectedIds.isNotEmpty()
                )
            }
        }
        if (assigned.isEmpty()) item { Text("No Blocks assigned to this week.") }
        items(assigned, key = { "assigned-${it.id}" }) { block ->
            val indicator = indicators.block(block.id)
            when {
                preparing -> Column(Modifier.fillMaxWidth().goalEndCap(indicator).padding(end = 12.dp)) {
                    WeekBlockText(block, indicator)
                    TextButton(onClick = { vm.removeFromWeek(week, block.id) }, enabled = enabled) {
                        Text("Remove ${block.name} from week")
                    }
                    HorizontalDivider()
                }
                block.archived -> Column(Modifier.fillMaxWidth().goalEndCap(indicator).padding(end = 12.dp)) {
                    WeekBlockText(block, indicator)
                }
                selecting -> OutlinedSelectionChip(selected = block.id in selected, onClick = {
                    selected = ArrayList(if (block.id in selected) selected - block.id else selected + block.id)
                }, label = { Column { WeekBlockText(block, indicator) } }, enabled = enabled,
                    modifier = Modifier.fillMaxWidth().goalEndCap(indicator).testTag("select-week-${block.id}"))
                else -> CatalogPill(onClick = { practice.startWeekBlock(block.id) },
                    modifier = Modifier.goalEndCap(indicator).testTag("practice-week-${block.id}"), enabled = enabled) {
                    WeekBlockText(block, indicator)
                }
            }
        }
        if (preparing) {
            item {
                Text("Add existing Blocks", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(query, { query = it }, label = { Text("Find a Block") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth())
            }
            item { BlockGoalFilterPicker(goalFilter) { goalFilter = it } }
            val available = matchingBlocks(catalog.blocks, query, false, indicators, goalFilter)
                .filter { it.id !in assignedIds }
            if (available.isEmpty()) item { Text("No matching active Blocks to add.") }
            items(available, key = { "available-${it.id}" }) { block ->
                val indicator = indicators.block(block.id)
                OutlinedButton(onClick = { vm.addToWeek(week, block.id) }, enabled = enabled,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).goalEndCap(indicator)) {
                    Column(Modifier.fillMaxWidth()) {
                        Text("Add ${block.name}")
                        BlockGoalLabel(indicator)
                    }
                }
            }
        }
    }

    if (carrying) AlertDialog(
        onDismissRequest = { carrying = false },
        title = { Text("Carry forward") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Choose an earlier week to add its active Blocks in order.")
                LazyColumn(Modifier.heightIn(max = 240.dp)) {
                    items(earlier) { date ->
                        TextButton(onClick = {
                            vm.carryWeek(LocalDate.parse(date), week)
                            carrying = false
                        }, enabled = enabled, modifier = Modifier.fillMaxWidth().testTag("carry-week-$date")) {
                            Text(weekRange(LocalDate.parse(date)))
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { carrying = false }) { Text("Cancel") } }
    )
}

private fun weekRange(week: LocalDate): String {
    val format = DateTimeFormatter.ofPattern("MMM d, uuuu")
    return "${week.format(format)} – ${week.plusDays(6).format(format)}"
}

@Composable
private fun WeekBlockText(block: Block, indicator: GoalIndicator?) {
    Text(block.name, style = MaterialTheme.typography.titleMedium)
    Text(if (block.archived) "Archived" else "${block.durationMinutes} min · ${block.action}",
        style = MaterialTheme.typography.bodyMedium)
    BlockGoalLabel(indicator)
}

@Composable
internal fun AddToThisWeek(included: Boolean, archived: Boolean, busy: Boolean, onAdd: () -> Unit) {
    if (included) Text("In This Week", style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(vertical = 8.dp))
    else if (!archived) TextButton(onClick = onAdd, enabled = !busy) { Text("Add to This Week") }
}
