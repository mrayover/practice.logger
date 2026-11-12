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

@Composable private fun GoalPage(back: () -> Unit, backLabel: String, title: String, content: @Composable ColumnScope.() -> Unit) {
    BackHandler(onBack = back)
    Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (backLabel != "Back to Home") TextButton(onClick = back) { Text(backLabel) }
        Text(title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
        content()
    }
}

@Composable fun GoalManagement(rows: List<GoalBundle>, vm: GoalViewModel, onHome: () -> Unit, onNew: () -> Unit, onGoal: (String) -> Unit) {
    var completed by rememberSaveable { mutableStateOf(false) }
    val active = activeGoals(rows)
    GoalPage(onHome, "Back to Home", "Manage Goals") {
        Text("${active.size} of 5 active Goals. Set progress yourself; time is measured separately.")
        Button(onClick = onNew, enabled = !vm.busy && active.size < 5, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("New Goal") }
        if (active.size == 5) Text("Complete an active Goal before adding or reopening another.")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedSelectionChip(!completed, { completed = false }, label = { Text("Active Goals") })
            OutlinedSelectionChip(completed, { completed = true }, label = { Text("Completed Goals") })
        }
        val visible = if (completed) rows.filter { it.goal.completed } else active
        if (visible.isEmpty()) Text(if (completed) "No completed Goals yet." else "Create a Goal to describe what you want to accomplish.")
        val indicators = LocalGoalIndicators.current
        visible.forEachIndexed { i, bundle ->
            val g = bundle.goal
            OutlinedButton(onClick = { onGoal(g.id) }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("open-goal-${g.id}").goalEndCap(indicators.goal(g.id))) {
                Text((if (completed) "" else "${i + 1}. ") + g.title + " · ${g.progress}%")
            }
            if (!completed) Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = { vm.move(g.id, -1) }, enabled = !vm.busy && i > 0, modifier = Modifier.semantics { contentDescription = "Move ${g.title} up" }) { Text("Up") }
                TextButton(onClick = { vm.move(g.id, 1) }, enabled = !vm.busy && i < active.lastIndex, modifier = Modifier.semantics { contentDescription = "Move ${g.title} down" }) { Text("Down") }
            }
            HorizontalDivider()
        }
    }
}

@Composable fun GoalDetail(bundle: GoalBundle?, allBlocks: List<Block>, actualMs: Long, vm: GoalViewModel, practice: PracticeViewModel,
    onGoals: () -> Unit, onEdit: () -> Unit, onBlocks: () -> Unit, onBlock: (String) -> Unit, onSession: (String) -> Unit = {}) {
    GoalPage(onGoals, "Back to Goals", bundle?.goal?.title ?: "Goal unavailable") {
        if (bundle == null) { Text("Return to Goals and select an available Goal."); return@GoalPage }
        val g = bundle.goal
        val indicators = LocalGoalIndicators.current
        val indicator = indicators.goal(g.id)
        if (indicator != null) {
            Text(indicator.priority?.let { "Priority $it" } ?: "Completed Goal",
                Modifier.fillMaxWidth().goalEndCap(indicator).padding(end = 16.dp))
        }
        if (g.description.isNotBlank()) Text(g.description)
        var assessProgress by rememberSaveable(g.id) { mutableStateOf(false) }
        TextButton(onClick = { assessProgress = !assessProgress }) {
            Text(if (assessProgress) "Hide manual assessment" else "Assess musical progress manually")
        }
        if (assessProgress) {
        Text("${g.progress}% · your manual assessment", style = MaterialTheme.typography.titleLarge)
        var progress by rememberSaveable(g.id, g.progress) { mutableStateOf(g.progress.toString()) }
        OutlinedTextField(progress, { progress = it }, label = { Text("Manual progress (0–100%)") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
        OutlinedButton(onClick = { vm.progress(g.id, progress) }, enabled = !vm.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Save progress") }
        }
        Text(practiceTime(actualMs) + " actual practice", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        Text("Committed practice attributed to this Goal, including unfinished sessions and completed standalone Transcribe. Saved placement Goals take precedence; explicit legacy Goal-session time is labeled below. Time does not change your assessment.")
        val activity by vm.activity.collectAsState()
        vm.error?.let { Text(it) }
        StatisticsHeading("Related practice (latest 12)")
        val related = activity?.filter { it.goalId == g.id && it.activeMs > 0 }
        if (related == null) Text("Loading practice history…")
        else if (related.isEmpty()) Text("No attributed practice yet.")
        related?.take(12)?.forEach { PracticeActivityRow(it, onSession) }
        Text("Associated Blocks", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        val blocks = allBlocks.associateBy { it.id }
        if (bundle.blocks.isEmpty()) Text("No Blocks yet. Add existing Blocks to practice toward this Goal.")
        bundle.ordered.forEach { link ->
            val b = blocks[link.blockId]
            if (b == null) Text("Unavailable Block (${link.blockId}) — remove it in Manage Blocks.")
            else {
                val primary = indicators.block(b.id)
                OutlinedButton(onClick = { onBlock(b.id) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).goalEndCap(primary)) {
                    Column(Modifier.fillMaxWidth()) {
                        Text((if (b.archived) "Archived: " else "") + b.name + " · ${b.durationMinutes} min")
                        BlockGoalLabel(primary)
                    }
                }
            }
        }
        if (bundle.blocks.any { blocks[it.blockId]?.archived != false }) Text("Archived and unavailable Blocks stay linked but are excluded from new Goal practice.")
        val usable = bundle.blocks.any { blocks[it.blockId]?.archived == false }
        if (usable && !g.completed) Button(onClick = { practice.startGoal(g.id) }, enabled = !practice.busy && !vm.busy,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Practice Goal") }
        else if (!g.completed) Text("Add an active Block to enable Practice Goal.")
        OutlinedButton(onClick = onBlocks, enabled = !vm.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Manage Blocks") }
        Text("Notes", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        Text(g.notes.ifBlank { "No Goal notes yet." })
        OutlinedButton(onClick = onEdit, enabled = !vm.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Edit Goal") }
        OutlinedButton(onClick = { vm.complete(g.id, !g.completed) }, enabled = !vm.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text(if (g.completed) "Reopen Goal" else "Mark Goal completed") }
    }
}

@Composable fun GoalEditor(goal: Goal?, vm: GoalViewModel, onCancel: () -> Unit, onNavigationCancelled: () -> Unit = {}) {
    var title by rememberSaveable { mutableStateOf(goal?.title ?: "") }
    var description by rememberSaveable { mutableStateOf(goal?.description ?: "") }
    var progress by rememberSaveable { mutableStateOf((goal?.progress ?: 0).toString()) }
    var notes by rememberSaveable { mutableStateOf(goal?.notes ?: "") }
    var discard by rememberSaveable { mutableStateOf(false) }
    fun leave() {
        if (!vm.busy) {
            if (title != (goal?.title ?: "") || description != (goal?.description ?: "") || progress != (goal?.progress ?: 0).toString() || notes != (goal?.notes ?: "")) discard = true
            else onCancel()
        }
    }
    GoalPage(::leave, "Cancel Goal editing", if (goal == null) "New Goal" else "Edit Goal") {
        OutlinedTextField(title, { title = it }, label = { Text("Goal title") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(description, { description = it }, label = { Text("Desired outcome (optional)") }, minLines = 3, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(progress, { progress = it }, label = { Text("Manual progress (0–100%)") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
        Text("Your assessment of musical progress. Practice time is tracked separately.")
        OutlinedTextField(notes, { notes = it }, label = { Text("Goal notes (optional)") }, minLines = 3, modifier = Modifier.fillMaxWidth())
        Button(onClick = { vm.save(goal?.id, title, description, progress, notes) }, enabled = !vm.busy,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Save Goal") }
    }
    if (discard) AlertDialog(onDismissRequest = { discard = false; onNavigationCancelled() }, title = { Text("Discard unsaved Goal changes?") },
        confirmButton = { TextButton(onClick = { discard = false; onCancel() }) { Text("Discard") } },
        dismissButton = { TextButton(onClick = { discard = false; onNavigationCancelled() }) { Text("Keep editing") } })
}

@Composable fun GoalBlocks(bundle: GoalBundle?, blocks: List<Block>, vm: GoalViewModel, onBack: () -> Unit, onLibrary: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    GoalPage(onBack, "Back to Goal", "Goal Blocks") {
        if (bundle == null) { Text("This Goal is unavailable."); return@GoalPage }
        Text("Choose existing Blocks. Removing a Block from its primary Goal clears the primary assignment.")
        val indicators = LocalGoalIndicators.current
        OutlinedTextField(query, { query = it }, label = { Text("Find Blocks") }, modifier = Modifier.fillMaxWidth())
        val selected = bundle.ordered.map { it.blockId }
        bundle.ordered.filter { link -> blocks.none { it.id == link.blockId } }.forEach { link ->
            Text("Unavailable Block (${link.blockId})")
            TextButton(onClick = { vm.blocks(bundle.goal.id, selected - link.blockId) }, enabled = !vm.busy) { Text("Remove unavailable Block") }
        }
        if (blocks.isEmpty()) Text("Create a Block in the Block Library, then return to this Goal.")
        blocks.filter { it.name.contains(query.trim(), ignoreCase = true) }.forEach { block ->
            val linked = block.id in selected
            val indicator = indicators.block(block.id)
            OutlinedButton(onClick = { vm.blocks(bundle.goal.id, if (linked) selected - block.id else selected + block.id) },
                enabled = !vm.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).semantics { stateDescription = if (linked) "Associated" else "Not associated" }.goalEndCap(indicator)) {
                Column(Modifier.fillMaxWidth()) {
                    Text((if (linked) "Remove " else "Add ") + block.name + (if (block.archived) " (archived)" else ""))
                    BlockGoalLabel(indicator)
                }
            }
        }
        OutlinedButton(onClick = onLibrary, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Open Block Library") }
    }
}
