package com.mrayover.practicelogger

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

internal data class GoalIndicator(val goal: Goal, val priority: Int?) {
    val label: String
        get() = if (goal.completed) "${goal.title} · Completed Goal" else "Priority $priority · ${goal.title}"
}

internal class GoalIndicators(rows: List<GoalBundle>) {
    val active = activeGoals(rows).mapIndexed { index, row -> GoalIndicator(row.goal, index + 1) }
    private val byGoal = (active + rows.filter { it.goal.completed }.map { GoalIndicator(it.goal, null) })
        .associateBy { it.goal.id }
    private val primary = rows.flatMap { it.primaryBlocks }.associate { it.blockId to it.goalId }

    fun goal(id: String?) = byGoal[id]
    fun block(id: String?) = goal(primary[id])
}

internal val LocalGoalIndicators = compositionLocalOf { GoalIndicators(emptyList()) }

internal fun goalAccent(colors: PracticeColors, indicator: GoalIndicator?): Color? = when {
    indicator == null -> colors.unassignedGoalAccent
    indicator.goal.completed -> null
    else -> indicator.priority?.let { colors.goalPriorities.getOrNull(it - 1) }
}

@Composable
internal fun Modifier.goalEndCap(indicator: GoalIndicator?): Modifier {
    val accent = goalAccent(PracticeTheme.colors, indicator)
    val outline = MaterialTheme.colorScheme.outline
    val hollow = MaterialTheme.colorScheme.surface
    return drawWithContent {
        drawContent()
        val width = 6.dp.toPx()
        val height = minOf(size.height * .55f, 34.dp.toPx())
        val inset = 8.dp.toPx()
        val x = if (layoutDirection == LayoutDirection.Ltr) size.width - inset - width else inset
        val y = (size.height - height) / 2
        val edge = 1.dp.toPx()
        drawRoundRect(outline, Offset(x, y), Size(width, height), CornerRadius(width / 2))
        drawRoundRect(accent ?: hollow, Offset(x + edge, y + edge), Size(width - 2 * edge, height - 2 * edge),
            CornerRadius((width - 2 * edge) / 2))
    }
}

@Composable
internal fun BlockGoalLabel(indicator: GoalIndicator?) {
    Text(indicator?.label ?: "No Goal", style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
internal fun PrimaryGoalPicker(selectedId: String?, enabled: Boolean, onSelect: (String?) -> Unit) {
    val goals = LocalGoalIndicators.current
    val selected = goals.goal(selectedId)
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Primary Goal", style = MaterialTheme.typography.titleSmall)
        Box {
            OutlinedButton(onClick = { expanded = true }, enabled = enabled,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("primary-goal-picker").goalEndCap(selected)) {
                Text(selected?.label ?: "No Goal", Modifier.weight(1f))
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                (listOf(null) + goals.active + listOfNotNull(selected?.takeIf { it.goal.completed })).forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option?.label ?: "No Goal") },
                        onClick = { onSelect(option?.goal?.id); expanded = false },
                        modifier = Modifier.goalEndCap(option),
                        contentPadding = PaddingValues(start = 12.dp, end = 24.dp)
                    )
                }
            }
        }
    }
}
