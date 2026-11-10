package com.mrayover.practicelogger

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun BlockPill(
    block: Block,
    goal: GoalIndicator?,
    references: List<String>,
    modifier: Modifier = Modifier,
    completion: BlockCompletion? = null,
    selection: (@Composable () -> Unit)? = null,
    actions: @Composable FlowRowScope.() -> Unit
) {
    var expanded by rememberSaveable(block.id) { mutableStateOf(false) }
    val goalColor = goalAccent(PracticeTheme.colors, goal)
    val completionColor = completion?.colorArgb?.let(::Color)
    val status = if (completion == null) "Needs work" else "Completed"
    val chevron = MaterialTheme.colorScheme.onSurfaceVariant
    val duration = "${block.durationMinutes} min" + (block.tempo?.let { " · $it BPM" } ?: "")

    Surface(
        modifier = Modifier.fillMaxWidth().testTag("block-pill-${block.id}"),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    modifier.weight(1f).heightIn(min = 64.dp)
                        .semantics { stateDescription = "${if (expanded) "Expanded" else "Collapsed"} · $status" }
                        .clickable(role = Role.Button, onClickLabel = if (expanded) "Collapse Block" else "Expand Block") {
                            expanded = !expanded
                        }
                        .padding(start = 14.dp, top = 10.dp, end = 12.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (completion == null && goal != null) {
                        Box(Modifier.size(8.dp).background(goalColor ?: MaterialTheme.colorScheme.outline)
                            .testTag("block-needs-work-marker"))
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(block.name, style = MaterialTheme.typography.titleMedium,
                            maxLines = if (expanded) Int.MAX_VALUE else 2, overflow = TextOverflow.Ellipsis)
                        if (!expanded) Text(block.action.lineSequence().first(), style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(duration + if (block.archived) " · Archived" else "", style = MaterialTheme.typography.bodySmall)
                        if (completion != null) Text("Completed", style = MaterialTheme.typography.bodySmall)
                    }
                    Canvas(Modifier.size(14.dp)) {
                        val edge = size.height * if (expanded) .65f else .35f
                        val center = size.height - edge
                        drawLine(chevron, Offset(0f, edge), Offset(size.width / 2, center), 2.dp.toPx())
                        drawLine(chevron, Offset(size.width / 2, center), Offset(size.width, edge), 2.dp.toPx())
                    }
                    when {
                        completionColor != null -> BlockCompletionSlit(completionColor)
                        completion != null || goal == null -> Box(Modifier.size(22.dp, 40.dp).goalEndCap(null))
                    }
                }
                selection?.invoke()
            }
            if (expanded) {
                Column(
                    Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(block.action, style = MaterialTheme.typography.bodyMedium)
                    BlockGoalLabel(goal)
                    if (block.notes.isNotBlank()) {
                        Text("Notes / Source", style = MaterialTheme.typography.titleSmall)
                        Text(block.notes, style = MaterialTheme.typography.bodyMedium)
                    }
                    if (references.isNotEmpty()) {
                        Text("References", style = MaterialTheme.typography.titleSmall)
                        references.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), content = actions)
                }
            }
        }
    }
}

@Composable
internal fun BlockCompletionSlit(color: Color) {
    Box(Modifier.size(4.dp, 24.dp).background(color, RoundedCornerShape(2.dp))
        .testTag("block-completed-marker"))
}

@Composable
internal fun blockReferenceSummaries(vm: ImageReferenceViewModel): Map<String, List<String>> {
    val visuals by vm.visuals.collectAsStateWithLifecycle()
    val links by vm.links.collectAsStateWithLifecycle()
    val assets by vm.assets.collectAsStateWithLifecycle()
    return remember(visuals, links, assets) {
        val byId = assets.associateBy { it.id }
        buildList {
            visuals.forEach { visual ->
                val title = byId[visual.mediaId]?.title
                val summary = when {
                    title != null -> "Visual · $title"
                    visual.legacy != null -> if (visual.mimeType == "application/pdf") "Block-owned PDF" else "Block-owned image"
                    else -> "Visual reference unavailable"
                }
                add(visual.blockId to summary)
            }
            links.filter { it.role == MediaRole.AUDIO.name }.forEach { link ->
                val title = byId[link.mediaId]?.title
                add(link.blockId to (title?.let { "Audio · $it" } ?: "Audio reference unavailable"))
            }
        }.groupBy({ it.first }, { it.second })
    }
}
