package com.mrayover.practicelogger

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File

class MediaLibraryViewModel(app: Application) : AndroidViewModel(app) {
    val repository = MediaLibraryRepository(BlockDatabase.appInstance(app), File(app.filesDir, "media-library"))
    private val rows = MutableStateFlow<List<MediaAsset>>(emptyList())
    val assets = rows.asStateFlow()
    var ready by mutableStateOf(false); private set
    var readError by mutableStateOf(false); private set
    var unavailable by mutableStateOf(setOf<String>()); private set
    var busy by mutableStateOf(false); private set
    var importing by mutableStateOf(false); private set
    var message by mutableStateOf<String?>(null); private set
    private var reader: Job? = null
    private var operation: Job? = null
    init { retryRead() }
    fun retryRead() {
        reader?.cancel(); readError = false
        reader = viewModelScope.launch {
            try {
                repository.recover()
                repository.assets.collect { value ->
                    val missing = withContext(Dispatchers.IO) { value.filterNot(repository::available).map { it.id }.toSet() }
                    rows.value = value; unavailable = missing; ready = true
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { readError = true; ready = false }
        }
    }
    fun refreshFiles() { viewModelScope.launch {
        unavailable = withContext(Dispatchers.IO) { rows.value.filterNot(repository::available).map { it.id }.toSet() }
    } }
    private fun work(import: Boolean = false, action: suspend () -> String) {
        if (busy || !ready) return
        busy = true; importing = import; message = null
        operation = viewModelScope.launch {
            try { message = action() }
            catch (cancelled: CancellationException) { message = "Import canceled. Saved Library items were kept."; throw cancelled }
            catch (failure: IllegalArgumentException) { message = failure.message ?: "Choose a valid supported original." }
            catch (failure: IllegalStateException) { message = failure.message ?: "Could not update the Library. Try again." }
            catch (_: Exception) { message = "Could not update the Library. Check file access and free storage, then retry." }
            finally { busy = false; importing = false }
        }
    }
    fun import(uri: Uri, role: MediaRole? = null) = work(import = true) {
        withContext(Dispatchers.IO) {
            val resolver = getApplication<Application>().contentResolver
            val name = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                if (it.moveToFirst() && !it.isNull(0)) it.getString(0) else null
            } ?: throw IllegalArgumentException("The file provider did not supply a filename. Choose another original.")
            repository.import(name, resolver.getType(uri), role = role) { checkNotNull(resolver.openInputStream(uri)) { "The source file is unavailable." } }
        }
        "Imported. Library media is local and is not included in JSON backups."
    }
    fun cancelImport() { if (importing) operation?.cancel() }
    fun pickerUnavailable() { message = "Could not open the file picker. Try again." }
    fun rename(id: String, value: String) = work { repository.rename(id, value); "Library item renamed." }
    fun delete(id: String) = work {
        val cleaned = repository.delete(id)
        runCatching { AudioWaveformCache(repository, File(getApplication<Application>().cacheDir, "audio-waveforms")).remove(id) }
        if (cleaned) "Library item deleted."
        else "Library item deleted. Private file cleanup will retry after reopening."
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable fun MediaLibraryScreen(vm: MediaLibraryViewModel, onBack: () -> Unit, onWorkspaceChanged: (Boolean) -> Unit = {}, selectionRole: MediaRole? = null, selectedId: String? = null, allowAudioImport: Boolean = false, onChoose: ((String) -> Unit)? = null, destinationPills: (@Composable () -> Unit)? = null) {
    val assets by vm.assets.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var filterName by rememberSaveable { mutableStateOf(MediaFilter.ALL.name) }
    var byName by rememberSaveable { mutableStateOf(false) }
    var renameId by rememberSaveable { mutableStateOf<String?>(null) }
    var renameText by rememberSaveable { mutableStateOf("") }
    var deleteId by rememberSaveable { mutableStateOf<String?>(null) }
    var previewId by rememberSaveable { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) vm.import(uri, if (allowAudioImport) MediaRole.AUDIO else null) }
    val importAction = {
        try {
            picker.launch(if (allowAudioImport) libraryAudioMimeTypes else
                arrayOf("image/jpeg", "image/png", "image/webp", "application/pdf") + libraryAudioMimeTypes)
        }
        catch (_: Exception) { vm.pickerUnavailable() }
    }
    val lifecycle = LocalLifecycleOwner.current
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) vm.refreshFiles() }
        lifecycle.lifecycle.addObserver(observer)
        onDispose { lifecycle.lifecycle.removeObserver(observer) }
    }
    val enabled = vm.ready && !vm.busy
    val compatible = if (selectionRole == null) assets else assets.filter { selectionRole.accepts(it.category) }
    val filtered = libraryItems(compatible, query, MediaFilter.valueOf(filterName), byName)
    val preview = assets.find { it.id == previewId }
    DisposableEffect(preview?.id) {
        onWorkspaceChanged(preview != null)
        onDispose { onWorkspaceChanged(false) }
    }
    if (preview != null) {
        if (preview.category == "AUDIO") LibraryAudioScreen(preview, { previewId = null })
        else LibraryVisualPreview(preview, vm.repository) { previewId = null }
        return
    }
    LazyColumn(Modifier.fillMaxSize().safeDrawingPadding().imePadding().padding(horizontal = 24.dp).testTag("media-library-list"),
        state = listState, verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 12.dp)) {
        if (destinationPills != null) item { destinationPills() }
        item {
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onBack) { Text(if (selectionRole == null) "Back" else "Cancel selection") }
                Text(if (selectionRole == null) "Media Library" else if (selectionRole == MediaRole.VISUAL) "Choose visual reference" else "Choose audio reference", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(vertical = 8.dp))
                if (selectionRole == null || allowAudioImport) Button(onClick = importAction, enabled = enabled, modifier = Modifier.testTag("media-import")) { Text("Import") }
            }
        }
        item { Text("Library media and master annotations are stored locally and are not yet included in JSON backups. Keep your originals separately.", style = MaterialTheme.typography.bodySmall) }
        item {
            OutlinedTextField(query, { query = it }, label = { Text("Search title or filename") }, singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("media-search"))
        }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MediaFilter.entries.filter { selectionRole == null || it == MediaFilter.ALL || (it.category != null && selectionRole.accepts(it.category)) }.forEach { filter -> OutlinedSelectionChip(selected = filterName == filter.name, onClick = { filterName = filter.name }, label = { Text(filter.label) }) }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedSelectionChip(selected = !byName, onClick = { byName = false }, label = { Text("Most recent") })
                OutlinedSelectionChip(selected = byName, onClick = { byName = true }, label = { Text("Name") })
            }
        }
        if (vm.busy) item {
            Text(if (vm.importing) "Importing and validating…" else "Saving…")
            if (vm.importing) TextButton(onClick = vm::cancelImport) { Text("Cancel import") }
        }
        vm.message?.let { message -> item { Text(message, modifier = Modifier.testTag("media-message")) } }
        if (vm.readError) item {
            Text("Could not read the Library or finish storage recovery. Saved files were kept.")
            Button(onClick = vm::retryRead) { Text("Retry Library") }
        } else if (!vm.ready) item { Text("Loading Library…") }
        else if (filtered.isEmpty()) item {
            Text(if (compatible.isEmpty() && allowAudioImport) "No audio in the Library yet. Import an MP3, M4A or WAV." else if (compatible.isEmpty()) "No compatible files imported yet. Import an image, PDF or audio file to organize it here." else "No files match your search and filter.")
            if (selectionRole == null && assets.isEmpty()) OutlinedButton(onClick = importAction, enabled = enabled) { Text("Import a file") }
        }
        items(filtered, key = { it.id }) { asset ->
            OutlinedCard(Modifier.fillMaxWidth().testTag("media-item-${asset.id}")) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(asset.title, style = MaterialTheme.typography.titleMedium)
                    LibraryMediaMetadata(asset)
                    if (selectionRole != null && selectedId == asset.id) Text("Currently selected", modifier = Modifier.testTag("media-selected-${asset.id}"))
                    if (asset.id in vm.unavailable) Text("File missing or changed. Delete this item and import your original again.", color = MaterialTheme.colorScheme.error)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (onChoose != null) TextButton(onClick = { onChoose(asset.id) }, enabled = enabled && asset.id !in vm.unavailable, modifier = Modifier.testTag("media-choose-${asset.id}")) { Text(if (selectedId == asset.id) "Keep selected" else "Choose") }
                        if (selectionRole == null) TextButton(onClick = { previewId = asset.id }, enabled = enabled, modifier = Modifier.testTag("media-open-${asset.id}")) { Text("Open") }
                        if (selectionRole == null) TextButton(onClick = { renameId = asset.id; renameText = asset.title }, enabled = enabled, modifier = Modifier.testTag("media-rename-${asset.id}")) { Text("Rename") }
                        if (selectionRole == null) TextButton(onClick = { deleteId = asset.id }, enabled = enabled, modifier = Modifier.testTag("media-delete-${asset.id}")) { Text("Delete") }
                    }
                }
            }
        }
    }
    renameId?.let { id ->
        val valid = runCatching { MediaLibraryRepository.title(renameText) }.isSuccess
        AlertDialog(onDismissRequest = { renameId = null }, title = { Text("Rename Library item") },
            text = { OutlinedTextField(renameText, { renameText = it }, label = { Text("Title") }, isError = !valid, supportingText = { Text("1–255 characters") }, modifier = Modifier.testTag("media-rename-title")) },
            confirmButton = { TextButton(onClick = { vm.rename(id, renameText); renameId = null }, enabled = enabled && valid) { Text("Save title") } },
            dismissButton = { if (selectionRole == null) TextButton(onClick = { renameId = null }) { Text("Cancel") } })
    }
    deleteId?.let { id ->
        AlertDialog(onDismissRequest = { deleteId = null }, title = { Text("Delete Library item?") },
            text = { Text("Delete “${assets.find { it.id == id }?.title.orEmpty()}”, its local original and any master audio markers? This cannot be undone. Your source outside the app is kept.") },
            confirmButton = { TextButton(onClick = { vm.delete(id); deleteId = null }, enabled = enabled) { Text("Delete item") } },
            dismissButton = { if (selectionRole == null) TextButton(onClick = { deleteId = null }) { Text("Cancel") } })
    }
}

@Composable internal fun LibraryMediaMetadata(asset: MediaAsset) {
    Text(asset.originalName, style = MaterialTheme.typography.bodySmall)
    val type = when (asset.category) { "IMAGE" -> "Image"; "PDF" -> "PDF"; else -> "Audio" }
    Text("$type · ${asset.mimeType} · ${android.text.format.Formatter.formatShortFileSize(androidx.compose.ui.platform.LocalContext.current, asset.byteCount)}", style = MaterialTheme.typography.bodySmall)
    Text(when (asset.category) {
        "IMAGE" -> "${asset.width} × ${asset.height} pixels"
        "PDF" -> "${asset.pageCount} pages"
        else -> importedDuration(asset.durationMs ?: 0)
    }, style = MaterialTheme.typography.bodySmall)
}
