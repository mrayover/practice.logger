package com.mrayover.practicelogger

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class OrganizationSnapshot(
    val categories: List<BlockCategory> = emptyList(),
    val sources: List<StudySource> = emptyList(),
    val mediaLinks: List<StudySourceMedia> = emptyList()
) {
    fun categoryName(id: String?): String? = categories.firstOrNull { it.id == id }?.name
    fun sourceName(id: String?): String? = sources.firstOrNull { it.id == id }?.title
    fun mediaSources(id: String): Set<String> = mediaLinks.filter { it.mediaId == id }.mapTo(linkedSetOf()) { it.sourceId }
    fun matches(block: Block, query: String): Boolean {
        val term = query.trim()
        return term.isEmpty() || listOfNotNull(
            block.name, block.action, block.notes, block.sourceLocation,
            categoryName(block.categoryId), sourceName(block.sourceId),
            sources.firstOrNull { it.id == block.sourceId }?.creator
        ).any { it.contains(term, ignoreCase = true) }
    }
}

class OrganizationViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = OrganizationRepository(BlockDatabase.appInstance(app))
    val snapshot = combine(repository.categories, repository.sources, repository.mediaLinks) { c, s, m ->
        OrganizationSnapshot(c, s, m)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), OrganizationSnapshot())
    var busy by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    fun dismissError() { error = null }
    private fun mutate(action: suspend () -> Unit) {
        if (busy) return
        busy = true
        viewModelScope.launch {
            try { action() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (e: Exception) { error = e.message ?: "Organization could not be saved." }
            finally { busy = false }
        }
    }
    fun addCategory(name: String) = mutate { repository.createCategory(name) }
    fun renameCategory(id: String, name: String) = mutate { repository.renameCategory(id, name) }
    fun removeCategory(id: String) = mutate { repository.deleteCategory(id) }
    fun addSource(title: String, creator: String, kind: String) = mutate { repository.createSource(title, creator, kind) }
    fun editSource(id: String, title: String, creator: String, kind: String) = mutate { repository.updateSource(id, title, creator, kind) }
    fun removeSource(id: String) = mutate { repository.deleteSource(id) }
    fun assignMedia(mediaId: String, sourceIds: Set<String>) = mutate { repository.setMediaSources(mediaId, sourceIds) }
}

internal val LocalOrganization = compositionLocalOf { OrganizationSnapshot() }

/** Shared, subdued taxonomy line. Goal colors are reserved for the Goal system. */
@Composable
internal fun BlockOrganizationLabel(block: Block) {
    val org = LocalOrganization.current
    val labels = listOfNotNull(org.categoryName(block.categoryId), org.sourceName(block.sourceId))
    if (labels.isNotEmpty()) Text(labels.joinToString(" · "), style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
}

/** Reusable organization picker for Block editor; creating names is handled in its parent Library. */
@Composable
internal fun BlockOrganizationEditor(
    categoryId: String?, sourceId: String?, sourceLocation: String,
    onCategory: (String?) -> Unit, onSource: (String?) -> Unit, onLocation: (String) -> Unit,
    enabled: Boolean = true
) {
    val org = LocalOrganization.current
    var categoryMenu by remember { mutableStateOf(false) }
    var sourceMenu by remember { mutableStateOf(false) }
    Text("Organization", style = MaterialTheme.typography.titleMedium)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.weight(1f)) {
            OutlinedButton(onClick = { categoryMenu = true }, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
                Text(org.categoryName(categoryId) ?: "Category", maxLines = 1)
            }
            DropdownMenu(expanded = categoryMenu, onDismissRequest = { categoryMenu = false }) {
                DropdownMenuItem(text = { Text("No category") }, onClick = { onCategory(null); categoryMenu = false })
                org.categories.forEach { item ->
                    DropdownMenuItem(text = { Text(item.name) }, onClick = { onCategory(item.id); categoryMenu = false })
                }
            }
        }
        Box(Modifier.weight(1f)) {
            OutlinedButton(onClick = { sourceMenu = true }, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
                Text(org.sourceName(sourceId) ?: "Source", maxLines = 1)
            }
            DropdownMenu(expanded = sourceMenu, onDismissRequest = { sourceMenu = false }) {
                DropdownMenuItem(text = { Text("No source") }, onClick = { onSource(null); sourceMenu = false })
                org.sources.forEach { item ->
                    DropdownMenuItem(text = { Text(item.title) }, onClick = { onSource(item.id); sourceMenu = false })
                }
            }
        }
    }
    if (sourceId != null) OutlinedTextField(
        sourceLocation, onLocation, label = { Text("Chapter / page / section (optional)") },
        singleLine = true, enabled = enabled, modifier = Modifier.fillMaxWidth(),
        supportingText = { Text("Optional location within this Source") }
    )
}

internal enum class BlockGroupMode(val label: String) {
    ALL("All"), CATEGORY("Category"), SOURCE("Source")
}

@Composable
internal fun BlockGroupPicker(mode: BlockGroupMode, onMode: (BlockGroupMode) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        BlockGroupMode.entries.forEach { option ->
            OutlinedSelectionChip(selected = mode == option, onClick = { onMode(option) },
                label = { Text(option.label) })
        }
    }
}

@Composable
internal fun OrganizationFilterPicker(
    categoryId: String?, sourceId: String?,
    onCategory: (String?) -> Unit, onSource: (String?) -> Unit
) {
    val org = LocalOrganization.current
    var categoryOpen by remember { mutableStateOf(false) }
    var sourceOpen by remember { mutableStateOf(false) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box {
            OutlinedButton(onClick = { categoryOpen = true }) {
                Text(org.categoryName(categoryId) ?: "All Categories", maxLines = 1)
            }
            DropdownMenu(expanded = categoryOpen, onDismissRequest = { categoryOpen = false }) {
                DropdownMenuItem(text = { Text("All Categories") }, onClick = { onCategory(null); categoryOpen = false })
                org.categories.forEach { category ->
                    DropdownMenuItem(text = { Text(category.name) }, onClick = { onCategory(category.id); categoryOpen = false })
                }
            }
        }
        Box {
            OutlinedButton(onClick = { sourceOpen = true }) { Text(org.sourceName(sourceId) ?: "All Sources", maxLines = 1) }
            DropdownMenu(expanded = sourceOpen, onDismissRequest = { sourceOpen = false }) {
                DropdownMenuItem(text = { Text("All Sources") }, onClick = { onSource(null); sourceOpen = false })
                org.sources.forEach { source ->
                    DropdownMenuItem(text = { Text(source.title) }, onClick = { onSource(source.id); sourceOpen = false })
                }
            }
        }
    }
}

/** Group headers generated from saved associations; never maintain duplicate folders. */
internal fun LazyListScope.organizedBlockItems(
    blocks: List<Block>, org: OrganizationSnapshot, mode: BlockGroupMode,
    expanded: Set<String>, onToggle: (String) -> Unit,
    render: @Composable (Block) -> Unit
) {
    if (mode == BlockGroupMode.ALL) {
        items(blocks.size, key = { blocks[it].id }) { index -> render(blocks[index]) }
        return
    }
    val grouped = blocks.groupBy { block ->
        if (mode == BlockGroupMode.CATEGORY) block.categoryId ?: "" else block.sourceId ?: ""
    }.toList().sortedWith(compareBy<Pair<String, List<Block>>> {
        if (mode == BlockGroupMode.CATEGORY) org.categoryName(it.first) ?: "Uncategorized"
        else org.sourceName(it.first) ?: "No source"
    }.thenBy { it.first })
    for ((key, rows) in grouped) {
        val id = mode.name + ":" + key
        val label = if (mode == BlockGroupMode.CATEGORY) org.categoryName(key) ?: "Uncategorized"
                    else org.sourceName(key) ?: "No source"
        item(key = "group-$id") {
            OutlinedButton(onClick = { onToggle(id) }, modifier = Modifier.fillMaxWidth()) {
                Text(if (id in expanded) "⌄ " else "› ")
                Text(label, modifier = Modifier.weight(1f))
                Text(rows.size.toString())
            }
        }
        if (id in expanded) for (block in rows) {
            item(key = block.id) { render(block) }
        }
    }
}

/** Source labels and grouped media coexist with the original file search and type filters. */
internal fun LazyListScope.organizedMediaItems(
    assets: List<MediaAsset>, org: OrganizationSnapshot, grouped: Boolean,
    expanded: Set<String>, onToggle: (String) -> Unit, render: @Composable (MediaAsset) -> Unit
) {
    if (!grouped) {
        items(assets.size, key = { assets[it].id }) { render(assets[it]) }
        return
    }
    // A media item may be attached to several Sources. Render it under each one,
    // retaining a single canonical original and master annotations.
    val memberships = assets.flatMap { asset ->
        val ids = org.mediaSources(asset.id)
        if (ids.isEmpty()) listOf("" to asset) else ids.map { it to asset }
    }
    val groups = memberships.groupBy({ it.first }, { it.second })
    for (id in groups.keys.sortedBy { org.sourceName(it) ?: "Unassigned" }) {
        val groupId = "media:$id"
        item(key = "media-header:$id") {
            OutlinedButton(onClick = { onToggle(groupId) }, modifier = Modifier.fillMaxWidth()) {
                Text(if (groupId in expanded) "⌄ " else "› ")
                Text(org.sourceName(id) ?: "Unassigned", modifier = Modifier.weight(1f))
                Text(groups.getValue(id).size.toString())
            }
        }
        if (groupId in expanded) groups.getValue(id).forEach { media ->
            item(key = "media:$id:${media.id}") { render(media) }
        }
    }
}

@Composable
internal fun OrganizationManager(vm: OrganizationViewModel, onBack: () -> Unit) {
    val org by vm.snapshot.collectAsStateWithLifecycle()
    var name by rememberSaveable { mutableStateOf("") }
    var creator by rememberSaveable { mutableStateOf("") }
    var kind by rememberSaveable { mutableStateOf(SourceKind.BOOK.name) }
    var tab by rememberSaveable { mutableStateOf(0) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf<String?>(null) }
    var kindMenu by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = onBack) { Text("Back") }
        PageHeading("Organize", style = MaterialTheme.typography.headlineSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedSelectionChip(tab == 0, { tab = 0; name = ""; creator = ""; editingId = null }, { Text("Categories") })
            OutlinedSelectionChip(tab == 1, { tab = 1; name = ""; creator = ""; editingId = null }, { Text("Sources") })
        }
        OutlinedTextField(name, { name = it }, label = { Text(if (tab == 0) "Category name" else "Source title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        if (tab == 1) {
            OutlinedTextField(creator, { creator = it }, label = { Text("Author / creator (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Box {
                OutlinedButton(onClick = { kindMenu = true }) { Text(SourceKind.valueOf(kind).label) }
                DropdownMenu(expanded = kindMenu, onDismissRequest = { kindMenu = false }) {
                    SourceKind.entries.forEach { option -> DropdownMenuItem(text = { Text(option.label) }, onClick = { kind = option.name; kindMenu = false }) }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                if (tab == 0) {
                    if (editingId == null) vm.addCategory(name) else vm.renameCategory(checkNotNull(editingId), name)
                } else {
                    if (editingId == null) vm.addSource(name, creator, kind) else vm.editSource(checkNotNull(editingId), name, creator, kind)
                }
                name = ""; creator = ""; editingId = null
            }, enabled = !vm.busy && name.trim().isNotBlank()) { Text(if (editingId == null) "Add" else "Save") }
            if (editingId != null) TextButton(onClick = { name = ""; creator = ""; editingId = null }) { Text("Cancel edit") }
        }
        HorizontalDivider()
        androidx.compose.foundation.lazy.LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (tab == 0) items(org.categories.size) { index ->
                val category = org.categories[index]
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(category.name, modifier = Modifier.weight(1f))
                    TextButton(onClick = { editingId = category.id; name = category.name }) { Text("Edit") }
                    TextButton(onClick = { confirmDelete = category.id }) { Text("Remove") }
                }
            } else items(org.sources.size) { index ->
                val source = org.sources[index]
                Column {
                    Text(source.title, style = MaterialTheme.typography.titleMedium)
                    Text(listOf(source.creator, SourceKind.valueOf(source.kind).label).filter { it.isNotBlank() }.joinToString(" · "), style = MaterialTheme.typography.bodySmall)
                    Row {
                        TextButton(onClick = { editingId = source.id; name = source.title; creator = source.creator; kind = source.kind }) { Text("Edit") }
                        TextButton(onClick = { confirmDelete = source.id }) { Text("Remove") }
                    }
                }
            }
        }
    }
    confirmDelete?.let { id ->
        AlertDialog(onDismissRequest = { confirmDelete = null }, title = { Text("Remove organization?") },
            text = { Text("Only this category or Source and its associations are removed. Blocks, recordings and original Library files are retained.") },
            confirmButton = { TextButton(onClick = { if (tab == 0) vm.removeCategory(id) else vm.removeSource(id); confirmDelete = null }) { Text("Remove") } },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Cancel") } })
    }
}
