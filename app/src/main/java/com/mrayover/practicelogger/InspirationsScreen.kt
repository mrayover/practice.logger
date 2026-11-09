package com.mrayover.practicelogger

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

internal data class IncludedInspiration(
    val title: String, val author: String, val source: String, val file: String
)

private val included = listOf(
    IncludedInspiration("Homeric Hymn 25 — To the Muses and Apollo", "Anonymous · translated by Hugh G. Evelyn-White",
        "Hesiod, the Homeric Hymns and Homerica (Loeb Classical Library, 1914), Hymn 25", "homeric-hymn-25.txt"),
    IncludedInspiration("Theogony — Invocation to the Muses", "Hesiod · translated by Hugh G. Evelyn-White",
        "Hesiod, the Homeric Hymns and Homerica (Loeb Classical Library, 1914), lines 1–115", "theogony-invocation.txt"),
    IncludedInspiration("Orphic Hymn 76 — To the Muses", "Attributed to Orpheus · translated by Thomas Taylor",
        "The Hymns of Orpheus (1792), LXXV in Taylor’s numbering", "orphic-hymn-76.txt"),
    IncludedInspiration("A Song for St. Cecilia's Day, 1687", "John Dryden",
        "The Poetical Works of John Dryden, vol. II, ed. George Gilfillan (1855) · Project Gutenberg", "dryden-st-cecilia.txt"),
    IncludedInspiration("Hail! Bright Cecilia, 1692", "Nicholas Brady · set by Henry Purcell",
        "A Select Collection of Poems, vol. V, ed. John Nichols (1782), pp. 302–306", "brady-hail-bright-cecilia.txt")
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DestinationPills(first: String, second: String, secondSelected: Boolean, select: (Boolean) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedSelectionChip(selected = !secondSelected, onClick = { select(false) }, label = { Text(first) })
        OutlinedSelectionChip(selected = secondSelected, onClick = { select(true) }, label = { Text(second) })
    }
}

@Composable
internal fun InspirationsScreen(vm: InspirationsViewModel, onMedia: () -> Unit, onHome: () -> Unit) {
    val entries by vm.entries.collectAsStateWithLifecycle(initialValue = null)
    val context = LocalContext.current
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    var reading by remember { mutableStateOf<IncludedInspiration?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmRemoval by remember { mutableStateOf(false) }
    fun back() {
        focus.clearFocus()
        keyboard?.hide()
        if (reading != null) reading = null else vm.back(onMedia)
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(vm::openFile) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture(), vm::cameraResult)
    BackHandler { back() }

    val current = vm.current
    if (current != null && vm.preview) {
        VisualDocumentPreview(current.id, current.title, current.pageCount, "Back to inspiration",
            load = { vm.visualPage(current, it) }, close = { vm.preview = false })
        return
    }

    val selected = reading
    val bundledText by produceState("", selected) {
        if (selected != null) value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            context.assets.open("inspirations/" + selected.file).bufferedReader().use { it.readText() }
        }
    }
    val draft = vm.draft

    if (selected == null && draft == null && current == null) {
        LazyColumn(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 24.dp).testTag("inspirations-list"),
            verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 12.dp)) {
            item {
                TextButton(onClick = { vm.leave(onHome) }, enabled = !vm.busy) { Text("Back") }
                DestinationPills("Media", "Inspirations", true) { if (!it) back() }
                Text("Inspirations", style = MaterialTheme.typography.headlineMedium)
                Button(onClick = vm::add, enabled = !vm.busy) { Text("Add Inspiration") }
                if (vm.busy) Text("Opening…")
                vm.message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
            item { Text("Included Inspirations", style = MaterialTheme.typography.titleLarge) }
            items(included, key = { it.file }) { row ->
                InspirationCard(row.title, row.author, !vm.busy) { reading = row }
            }
            item { Text("My Inspirations", style = MaterialTheme.typography.titleLarge) }
            val rows = entries
            if (rows == null) item { Text("Loading…") }
            else if (rows.isEmpty()) item { Text("Your writings and documents will appear here.") }
            else items(rows, key = { it.id }) { row -> InspirationCard(row.title, row.author, !vm.busy) { vm.open(row) } }
        }
    } else if (draft != null) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding().padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()).padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = { back() }, enabled = !vm.busy) { Text("Back") }
            Text(if (current == null) "Add Inspiration" else "Edit inspiration", style = MaterialTheme.typography.headlineSmall)
            if (vm.chooseMethod) {
                Button(onClick = vm::write, enabled = !vm.busy) { Text("Write / Paste") }
                OutlinedButton(onClick = { vm.launchCamera { camera.launch(it) } }, enabled = !vm.busy) { Text("Camera") }
                OutlinedButton(onClick = {
                    try { picker.launch(arrayOf("*/*")) } catch (_: Exception) { vm.pickerUnavailable() }
                }, enabled = !vm.busy) { Text("Choose File") }
                Text("PDF, JPEG, PNG, WebP and UTF-8 TXT or MD", style = MaterialTheme.typography.bodySmall)
            } else {
                val enabled = !vm.busy
                val details = draft.details
                InspirationField("Title (required)", details.title, enabled) { vm.draft = draft.copy(details = details.copy(title = it)) }
                if (draft.hasVisual) {
                    val attachment = draft.input?.name ?: if (details.mimeType == "application/pdf") "PDF" else "Image"
                    Text("Attachment · $attachment")
                    OutlinedButton(onClick = { confirmRemoval = true }, enabled = enabled) { Text("Remove attachment") }
                } else {
                    OutlinedTextField(draft.body, { vm.draft = draft.copy(body = it) }, label = { Text("Text") },
                        enabled = enabled, minLines = 6, maxLines = 12, modifier = Modifier.fillMaxWidth().testTag("inspiration-body"))
                }
                InspirationField("Author", details.author, enabled) { vm.draft = draft.copy(details = details.copy(author = it)) }
                InspirationField("Source", details.source, enabled) { vm.draft = draft.copy(details = details.copy(source = it)) }
                InspirationCategory(details.category, enabled) { vm.draft = draft.copy(details = details.copy(category = it)) }
                OutlinedTextField(details.note, { vm.draft = draft.copy(details = details.copy(note = it)) },
                    label = { Text("Personal note") }, enabled = enabled, maxLines = 5, modifier = Modifier.fillMaxWidth())
                Button(onClick = {
                    focus.clearFocus()
                    keyboard?.hide()
                    vm.save()
                }, enabled = enabled && runCatching { MediaLibraryRepository.title(details.title) }.isSuccess) { Text("Save inspiration") }
            }
            if (vm.busy) Text("Working…")
            vm.message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    } else {
        val text = if (selected != null) bundledText else vm.body
        val sections = remember(text) { inspirationSections(text) }
        LazyColumn(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 12.dp)) {
            item {
                TextButton(onClick = { back() }, enabled = !vm.busy) { Text("Back to Inspirations") }
                Text(selected?.title ?: current?.title.orEmpty(), style = MaterialTheme.typography.headlineSmall)
                (selected?.author ?: current?.author)?.takeIf(String::isNotBlank)?.let { Text(it) }
                (selected?.source ?: current?.source)?.takeIf(String::isNotBlank)?.let { Text(it) }
                current?.category?.takeIf(String::isNotBlank)?.let { Text(it) }
                current?.note?.takeIf(String::isNotBlank)?.let { Text("Personal note\n$it") }
                if (current != null) {
                    if (current.mimeType != null && current.mimeType != "text/plain") {
                        Button(onClick = { vm.preview = true }, enabled = !vm.busy) {
                            Text(if (current.pageCount != null) "View PDF" else "View image")
                        }
                    }
                    TextButton(onClick = vm::edit, enabled = !vm.busy) { Text("Edit inspiration") }
                    TextButton(onClick = { confirmDelete = true }, enabled = !vm.busy) { Text("Delete inspiration") }
                }
                vm.message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
            items(sections.size) { index -> Text(sections[index].ifEmpty { " " }) }
        }
    }

    if (vm.confirmDiscard != null) InspirationConfirmation("Discard unsaved changes?",
        "Your changes to this inspiration will be lost.", "Discard changes", { vm.discard(false) }) { vm.discard(true) }
    if (confirmDelete) InspirationConfirmation("Delete this inspiration?",
        "This permanently removes your entry and any original owned by it.", "Delete inspiration",
        { confirmDelete = false }) { confirmDelete = false; vm.delete() }
    if (confirmRemoval) InspirationConfirmation("Remove the attachment?",
        "The attachment will be removed when you save.", "Remove attachment",
        { confirmRemoval = false }) { confirmRemoval = false; vm.removeOriginal() }
}

private fun inspirationSections(text: String): List<String> = buildList {
    var start = 0
    while (start < text.length) {
        val limit = (start + 4096).coerceAtMost(text.length)
        val newline = if (limit < text.length) text.lastIndexOf('\n', limit - 1) else -1
        val end = if (newline >= start) newline + 1
            else if (limit < text.length && text[limit - 1].isHighSurrogate()) limit - 1 else limit
        add(text.substring(start, end).removeSuffix("\n"))
        start = end
    }
}

@Composable
private fun InspirationCard(title: String, author: String, enabled: Boolean, open: () -> Unit) {
    OutlinedCard(onClick = open, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (author.isNotBlank()) Text(author, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun InspirationField(label: String, value: String, enabled: Boolean, change: (String) -> Unit) {
    OutlinedTextField(value, change, label = { Text(label) }, singleLine = true, enabled = enabled,
        modifier = Modifier.fillMaxWidth())
}

@Composable
private fun InspirationCategory(category: String, enabled: Boolean, select: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, enabled = enabled) {
            Text(if (category.isEmpty()) "Category (optional)" else category)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            for (option in listOf("", "Invocations", "Musical Philosophy", "Practical Wisdom", "Other")) {
                DropdownMenuItem(text = { Text(option.ifEmpty { "No category" }) }, onClick = { select(option); expanded = false })
            }
        }
    }
}

@Composable
private fun InspirationConfirmation(title: String, body: String, action: String, cancel: () -> Unit, confirm: () -> Unit) {
    AlertDialog(onDismissRequest = cancel, title = { Text(title) }, text = { Text(body) },
        dismissButton = { TextButton(onClick = cancel) { Text("Cancel") } },
        confirmButton = { TextButton(onClick = confirm) { Text(action) } })
}
