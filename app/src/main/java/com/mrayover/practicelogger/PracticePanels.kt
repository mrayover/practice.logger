package com.mrayover.practicelogger

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Standard touch/keyboard scrolling with an always-visible thumb when content overflows. */
@Composable fun PracticeScrollPanel(modifier: Modifier = Modifier, state: ScrollState = rememberScrollState(),
    tag: String, content: @Composable ColumnScope.() -> Unit) {
    val color = MaterialTheme.colorScheme.primary
    Box(modifier.testTag(tag)) {
        Column(Modifier.fillMaxWidth().verticalScroll(state).padding(end = 12.dp), content = content)
        if (state.maxValue > 0 && state.maxValue != Int.MAX_VALUE) Canvas(Modifier.matchParentSize()
            .testTag("$tag-scrollbar").semantics { contentDescription = "Vertical scroll indicator" }) {
            val width = 4.dp.toPx()
            val total = size.height + state.maxValue
            val thumb = (size.height * size.height / total).coerceAtLeast(24.dp.toPx()).coerceAtMost(size.height)
            val top = (size.height - thumb) * state.value / state.maxValue.toFloat()
            drawRoundRect(color.copy(alpha = .16f), Offset(size.width - width, 0f), Size(width, size.height),
                androidx.compose.ui.geometry.CornerRadius(width / 2))
            drawRoundRect(color.copy(alpha = .8f), Offset(size.width - width, top), Size(width, thumb),
                androidx.compose.ui.geometry.CornerRadius(width / 2))
        }
    }
}

@Composable private fun WorkspaceHeader(label: String, expanded: Boolean, tag: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(role = Role.Button, onClick = onClick)
        .padding(horizontal = 16.dp).testTag(tag).semantics { stateDescription = if (expanded) "Expanded" else "Collapsed" },
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, Modifier.weight(1f).semantics { heading() }, style = MaterialTheme.typography.labelLarge,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(if (expanded) "Collapse" else "Expand", color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelMedium, maxLines = 1)
    }
}

@Composable fun FocusedWorkspace(current: Placement, images: ImageReferenceViewModel, modifier: Modifier = Modifier) {
    val attachments by images.images.collectAsStateWithLifecycle()
    val attached = attachments.any { it.blockId == current.sourceBlockId }
    // Null follows asynchronous attachment loading until the musician makes an explicit choice.
    var choice by rememberSaveable(current.id) { mutableStateOf<String?>(null) }
    val expanded = choice ?: if (attached) "reference" else "description"
    Column(modifier.testTag("practice-workspace")) {
        WorkspaceHeader("Practice Description", expanded == "description", "practice-description-toggle") {
            choice = if (expanded == "description") "none" else "description"
        }
        if (expanded == "description") PracticeScrollPanel(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
            tag = "practice-description-panel") {
            Text(current.action, style = MaterialTheme.typography.titleMedium, modifier = Modifier.fillMaxWidth().testTag("practice-description"))
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = .16f))
        WorkspaceHeader("Reference Material", expanded == "reference", "practice-reference-toggle") {
            choice = if (expanded == "reference") "none" else "reference"
        }
        if (expanded == "reference") {
            if (attached) BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().testTag("practice-reference-area")) {
                val available = maxHeight.coerceAtLeast(1.dp)
                PracticeScrollPanel(Modifier.fillMaxSize(), tag = "practice-reference-panel") {
                    PracticeImageReference(current.sourceBlockId, current.id, images, viewerMaxHeight = available,
                        embeddedExpanded = true, fitAvailableHeight = true)
                    if (current.referenceNotes.isNotBlank()) Text(current.referenceNotes, Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                Text(current.referenceNotes.ifBlank { "No reference attached to this Block." },
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable fun PracticeTakeSelection(choices: List<BlockTake>, selectedId: String?, loop: Boolean,
    onSelect: (BlockTake) -> Unit, onClose: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(if (loop) "Loop Recordings" else "Free Recordings", Modifier.weight(1f).semantics { heading() }, style = MaterialTheme.typography.labelLarge)
            TextButton(onClick = onClose, modifier = Modifier.heightIn(min = 48.dp)) { Text("Close") }
        }
        PracticeScrollPanel(Modifier.weight(1f, fill = false).fillMaxWidth(), tag = "practice-${if (loop) "loop" else "free"}-list") {
            choices.forEach { take ->
                Column(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(role = Role.Button) { onSelect(take) }
                    .padding(vertical = 6.dp).testTag("practice-select-${take.id}").semantics { selected = take.id == selectedId }) {
                    Text(compactPracticeTakeTitle(take), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
                    Text(practiceTakeDetails(take), style = MaterialTheme.typography.bodySmall)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = .12f))
            }
        }
    }
}

fun compactPracticeTakeTitle(take: BlockTake): String = take.label.ifBlank { java.time.format.DateTimeFormatter.ofPattern("MMM d, h:mm:ss a").withZone(java.time.ZoneId.systemDefault()).format(java.time.Instant.ofEpochMilli(take.createdAt)) }
private fun practiceTakeDetails(take: BlockTake): String = practiceTime(take.durationMs) +
    (if (take.label.isNotBlank()) " · " + sessionDate(take.createdAt) else "") +
    if (take.mode == "LOOP") " · ${take.recordedBpm} BPM · ${take.beatCount} beats" else ""

fun practiceTransportMessage(message: String?): String? = message?.takeUnless {
    it == "Take saved." || it.startsWith("Looping ·") || it.startsWith("Phrase saved. Looping") || it == "Complete phrase saved. Playback stopped."
}

