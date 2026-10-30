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
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
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

@Composable private fun WorkspaceHeader(label: String, expanded: Boolean, tag: String, enabled: Boolean = true, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(enabled = enabled, role = Role.Button, onClick = onClick)
        .padding(horizontal = 16.dp).testTag(tag).semantics { stateDescription = if (expanded) "Expanded" else "Collapsed" },
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, Modifier.weight(1f).semantics { heading() }, style = MaterialTheme.typography.labelLarge,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(if (expanded) "Collapse" else "Expand", color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelMedium, maxLines = 1)
    }
}

@Composable fun FocusedWorkspace(current: Placement, images: ImageReferenceViewModel, modifier: Modifier = Modifier, suppressed: Boolean = false) {
    val attachments by images.images.collectAsStateWithLifecycle()
    val attached = attachments.any { it.blockId == current.sourceBlockId }
    // Null follows asynchronous attachment loading until the musician makes an explicit choice.
    var choice by rememberSaveable(current.id) { mutableStateOf<String?>(null) }
    val expanded = choice ?: if (attached) "reference" else "description"
    var referenceOpened by rememberSaveable(current.id) { mutableStateOf(false) }
    LaunchedEffect(expanded, attached) { if (attached && expanded == "reference") referenceOpened = true }
    Column(modifier.testTag("practice-workspace")) {
        WorkspaceHeader("Practice Description", !suppressed && expanded == "description", "practice-description-toggle", enabled = !suppressed) {
            choice = if (expanded == "description") "none" else "description"
        }
        if (expanded == "description") RetainedWorkspaceContent(suppressed,
            if (suppressed) Modifier.fillMaxWidth() else Modifier.weight(1f).fillMaxWidth()) {
            PracticeScrollPanel(Modifier.fillMaxSize().padding(horizontal = 16.dp), tag = "practice-description-panel") {
                Text(current.action, style = MaterialTheme.typography.titleMedium, modifier = Modifier.fillMaxWidth().testTag("practice-description"))
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = .16f))
        WorkspaceHeader("Reference Material", !suppressed && expanded == "reference", "practice-reference-toggle", enabled = !suppressed) {
            choice = if (expanded == "reference") "none" else "reference"
        }
        if (expanded == "reference" || (referenceOpened && attached)) RetainedWorkspaceContent(suppressed || expanded != "reference",
            if (suppressed || expanded != "reference" || !attached) Modifier.fillMaxWidth() else Modifier.weight(1f).fillMaxWidth()) {
            if (attached) BoxWithConstraints(Modifier.fillMaxSize().testTag("practice-reference-area")) {
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

@Composable private fun RetainedWorkspaceContent(hidden: Boolean, modifier: Modifier, content: @Composable () -> Unit) {
    val previous = remember { arrayOfNulls<Constraints>(1) }
    Box(modifier.then(if (hidden) Modifier.clearAndSetSemantics { } else Modifier).layout { measurable, constraints ->
        val child = measurable.measure(if (hidden) previous[0] ?: constraints else constraints)
        if (!hidden) previous[0] = constraints
        layout(child.width.coerceIn(constraints.minWidth, constraints.maxWidth), if (hidden) 0 else child.height) {
            if (!hidden) child.place(0, 0)
        }
    }) { content() }
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



@OptIn(ExperimentalLayoutApi::class)
@Composable fun BlockNotesPanel(entries: List<PracticeNoteEntry>, draft: String, onDraft: (String) -> Unit,
    busy: Boolean, message: String?, onSave: () -> Unit, onReturn: () -> Unit, editModel: BlockNotesViewModel, modifier: Modifier = Modifier) {
    val editingIds by editModel.editingIds.collectAsStateWithLifecycle()
    Column(modifier.testTag("practice-notes-panel")) {
        PracticeScrollPanel(Modifier.weight(1f).fillMaxWidth(), tag = "practice-notes-content") {
            Text("Add New Note", style = MaterialTheme.typography.labelLarge, modifier = Modifier.semantics { heading() })
            OutlinedTextField(draft, onDraft, label = { Text("New note") }, enabled = !busy, minLines = 2, maxLines = 5,
                modifier = Modifier.fillMaxWidth().testTag("practice-new-note"))
            message?.let { Text(it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
            if (entries.isEmpty()) Text("No saved practice notes for this Block.", style = MaterialTheme.typography.bodySmall)
            entries.forEach { entry -> key(entry.id) {
                var expanded by rememberSaveable(entry.id) { mutableStateOf(false) }
                val editing = entry.id in editingIds
                val editText by editModel.editDraft(entry).collectAsStateWithLifecycle()
                HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = .16f))
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(role = Role.Button) { if (!editing) expanded = !expanded }
                    .testTag("practice-note-${entry.id}").semantics { stateDescription = if (expanded || editing) "Expanded" else "Collapsed" },
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val date = java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy · h:mm:ss a")
                        .withZone(java.time.ZoneId.systemDefault()).format(java.time.Instant.ofEpochMilli(entry.createdAt))
                    Text(date + if (entry.legacy) " · Earlier session" else "", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                    Text(if (expanded || editing) "Collapse" else "Expand", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
                if (editing) {
                    OutlinedTextField(editText, { editModel.changeEdit(entry, it) }, label = { Text("Edit note") }, enabled = !busy,
                        minLines = 2, maxLines = 5, modifier = Modifier.fillMaxWidth().testTag("practice-note-editor-${entry.id}"))
                    FlowRow {
                        TextButton(onClick = { editModel.saveEdit(entry); expanded = true }, enabled = !busy && editText.isNotBlank(),
                            modifier = Modifier.heightIn(min = 48.dp).testTag("practice-note-save-${entry.id}")) { Text("Save Changes") }
                        TextButton(onClick = { editModel.cancelEdit(entry); expanded = true }, enabled = !busy,
                            modifier = Modifier.heightIn(min = 48.dp).testTag("practice-note-cancel-${entry.id}")) { Text("Cancel Changes") }
                    }
                } else if (expanded) {
                    Text(entry.text, Modifier.fillMaxWidth().testTag("practice-note-text-${entry.id}"), style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = { editModel.startEdit(entry) }, enabled = !busy,
                        modifier = Modifier.heightIn(min = 48.dp).testTag("practice-note-edit-${entry.id}")) { Text("Edit") }
                }
            } }
        }
        // Pinned note actions stay reachable even when the editor/history scrolls or IME opens.
        androidx.compose.foundation.layout.FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onSave, enabled = !busy && draft.isNotBlank(), modifier = Modifier.heightIn(min = 48.dp)) { Text("Save Note") }
            TextButton(onClick = onReturn, enabled = !busy, modifier = Modifier.heightIn(min = 48.dp)) { Text("Return to Practice") }
        }
    }
}

