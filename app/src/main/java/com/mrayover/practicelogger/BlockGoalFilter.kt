package com.mrayover.practicelogger

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

internal sealed interface BlockGoalFilter {
    data object All : BlockGoalFilter
    data object NoGoal : BlockGoalFilter
    data class Assigned(val goalId: String) : BlockGoalFilter

    fun availableIn(goals: GoalIndicators): BlockGoalFilter =
        if (this is Assigned && goals.active.none { it.goal.id == goalId }) All else this

    fun matches(primaryGoalId: String?): Boolean = when (this) {
        All -> true
        NoGoal -> primaryGoalId == null
        is Assigned -> primaryGoalId == goalId
    }
}

@Composable
internal fun rememberBlockGoalFilter(goals: GoalIndicators, scope: Any? = Unit): MutableState<BlockGoalFilter> {
    val filter = remember(scope) { mutableStateOf<BlockGoalFilter>(BlockGoalFilter.All) }
    LaunchedEffect(goals, filter.value) {
        filter.value = filter.value.availableIn(goals)
    }
    return filter
}

@Composable
internal fun BlockGoalFilterPicker(filter: BlockGoalFilter, onSelect: (BlockGoalFilter) -> Unit) {
    val goals = LocalGoalIndicators.current
    val current = filter.availableIn(goals)
    val options = listOf(BlockGoalFilter.All) + goals.active.map { BlockGoalFilter.Assigned(it.goal.id) } + BlockGoalFilter.NoGoal
    fun label(option: BlockGoalFilter) = when (option) {
        BlockGoalFilter.All -> "All Goals"
        BlockGoalFilter.NoGoal -> "No Goal"
        is BlockGoalFilter.Assigned -> goals.goal(option.goalId)?.label ?: "All Goals"
    }

    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("block-goal-filter")) {
            Text("Goal filter · ${label(current)}")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(label(option)) },
                    onClick = { onSelect(option); expanded = false },
                    modifier = Modifier.semantics { selected = option == current }
                )
            }
        }
    }
}
