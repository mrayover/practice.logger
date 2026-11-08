package com.mrayover.practicelogger

import android.app.Application
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.File
import java.util.UUID

fun readBackup(input: InputStream): ByteArray {
    val bytes = java.io.ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    while (true) {
        val count = input.read(buffer)
        if (count < 0) break
        require(bytes.size() + count <= BACKUP_LIMIT) { "Backup exceeds the 8 MiB limit." }
        bytes.write(buffer, 0, count)
    }
    return bytes.toByteArray()
}

class BackupViewModel(app: Application, private val db: BlockDatabase) : AndroidViewModel(app) {
    constructor(app: Application) : this(app, BlockDatabase.appInstance(app))
    private val repository = BackupRepository(db)
    private val storage = ExportStorage(app)
    private val portable = PortableZip(app, db)
    private val zipRestore = PortableRestore(app, db)
    private val staging = File(app.filesDir, "portable-backup")
    private var prepared: File? = null
    var zipReady by mutableStateOf(false); private set
    val hasSelectedFolder get() = storage.selected() != null
    val unfinishedCopy get() = storage.unfinishedZipCopy()
    var zipName by mutableStateOf(""); private set
    var busy by mutableStateOf(false); private set
    var message by mutableStateOf<String?>(null); private set
    var pending by mutableStateOf<BackupData?>(null); private set
    internal var pendingZip by mutableStateOf<PortableRestore.Staged?>(null); private set
    var recoveryRequired by mutableStateOf(false); private set
    val navigationBlocked get() = busy || recoveryRequired
    private fun work(reportReason: Boolean = false, operation: suspend () -> Unit) {
        if (busy) return
        busy = true; message = null
        viewModelScope.launch {
            try { operation() }
            catch (e: CancellationException) { throw e }
            catch (e: RestoreRecoveryRequired) { recoveryRequired = true; message = e.message }
            catch (e: IllegalArgumentException) { message = e.message ?: "Invalid backup data." }
            catch (_: NonEmptyRestoreException) { message = "Restore needs an empty app. Your existing Blocks and practice history were kept. Use this backup on a new phone or spare empty installation." }
            catch (failure: Exception) {
                message = if (reportReason && failure is IllegalStateException && !failure.message.isNullOrBlank()) failure.message
                    else "Could not complete this backup operation. Existing practice data was kept. Check the file, free space and access, then try again. Complete ZIP and legacy JSON restore need an empty app."
            }
            finally { busy = false }
        }
    }
    fun prepareZip() = work(reportReason = true) {
        discardPreparedZip()
        zipName = "practice.logger-complete-${System.currentTimeMillis()}.zip"
        val file = File(staging, "${UUID.randomUUID()}.zip")
        val verified = withContext(Dispatchers.IO) { portable.create(file) }
        prepared = file
        zipReady = true
        message = "Private ZIP verified with ${verified.originals.size} originals. Choose where to save it."
    }

    fun publishZip(uri: Uri?) {
        val file = prepared ?: return
        work(reportReason = true) {
            val copy = if (uri == null) storage.saveZipInSelectedFolder(zipName, file)
                else storage.saveZipAs(uri, zipName, file)
            message = if (copy.readBack) "Backup saved and read-back verified at ${copy.location}. Cloud synchronization depends on the provider."
                else "Backup copy written to ${copy.location}. The provider did not allow read-back verification. You can try another destination."
            if (copy.readBack) discardPreparedZip()
        }
    }

    fun cancelZip() {
        if (!busy) discardPreparedZip()
    }

    private fun discardPreparedZip() {
        prepared?.delete()
        prepared = null
        zipReady = false
    }

    override fun onCleared() {
        discardPreparedZip()
        super.onCleared()
    }

    fun export(uri: Uri) = work {
        withContext(Dispatchers.IO) {
            val bytes = BackupCodec.encode(repository.snapshot())
            checkNotNull(getApplication<Application>().contentResolver.openOutputStream(uri, "wt")).use { it.write(bytes); it.flush() }
        }
        message = "Backup saved. Keep this JSON file somewhere safe; it contains your Logs, Blocks, Goals, Routines and practice notes in plain text."
    }
    fun inspect(uri: Uri) = work {
        clearPreview()
        val data = withContext(Dispatchers.IO) {
            val bytes = checkNotNull(getApplication<Application>().contentResolver.openInputStream(uri)).use(::readBackup)
            BackupCodec.decode(bytes)
        }
        pending = data
    }
    fun inspectZip(uri: Uri) = work(reportReason = true) {
        clearPreview()
        pendingZip = zipRestore.stage {
            checkNotNull(getApplication<Application>().contentResolver.openInputStream(uri)) {
                "The selected ZIP could not be opened."
            }
        }
    }

    fun restoreZip(onRestored: () -> Unit) {
        val staged = pendingZip ?: return
        work(reportReason = true) {
            pendingZip = null
            try {
                zipRestore.restore(staged)
                onRestored()
                message = "Complete backup restored. Unfinished practice is paused."
            } finally {
                withContext(Dispatchers.IO + kotlinx.coroutines.NonCancellable) { zipRestore.discard(staged) }
            }
        }
    }

    fun retryRecovery(onRestored: () -> Unit) = work(reportReason = true) {
        val committed = zipRestore.recover()
        recoveryRequired = false
        onRestored()
        message = if (committed) "Complete backup restored. Unfinished practice is paused."
            else "Restore recovery finished. You can choose a backup again."
    }

    private fun clearPreview() {
        pending = null
        val staged = pendingZip
        pendingZip = null
        if (staged != null) viewModelScope.launch(Dispatchers.IO) { zipRestore.discard(staged) }
    }

    fun cancel() { if (!navigationBlocked) clearPreview() }
    fun restore() {
        val data = pending ?: return
        work {
            pending = null
            withContext(Dispatchers.IO) { repository.restore(data) }
            message = "Backup restored. Return Home to see your Blocks, Goals, Routines and history. Unfinished practice resumes paused."
        }
    }
    // The shared database belongs to the application, not an individual screen.
}

@Composable fun BackupScreen(onHome: () -> Unit, vm: BackupViewModel = viewModel(), onRestored: () -> Unit = {}) {
    val saveZip = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        uri?.let(vm::publishZip)
    }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> uri?.let(vm::export) }
    val openZip = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(vm::inspectZip) }
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(vm::inspect) }
    BackHandler { if (!vm.navigationBlocked) { vm.cancel(); vm.cancelZip(); onHome() } }
    Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Backup and restore", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
        Text("Complete ZIP backup", style = MaterialTheme.typography.titleLarge)
        Text("Includes practice history, settings, original media and recordings. Keep the archive somewhere private.")
        Button(onClick = vm::prepareZip, enabled = !vm.navigationBlocked && !vm.zipReady) { Text("Prepare complete ZIP") }
        if (vm.zipReady) {
            Button(onClick = { saveZip.launch(vm.zipName) }, enabled = !vm.navigationBlocked) { Text("Save ZIP as…") }
            if (vm.hasSelectedFolder) {
                OutlinedButton(onClick = { vm.publishZip(null) }, enabled = !vm.navigationBlocked) { Text("Save ZIP to selected folder") }
            }
            TextButton(onClick = vm::cancelZip, enabled = !vm.navigationBlocked) { Text("Discard prepared ZIP") }
        }
        OutlinedButton(onClick = { openZip.launch(arrayOf("application/zip", "application/octet-stream")) },
            enabled = !vm.navigationBlocked) { Text("Choose complete ZIP to restore") }
        Text("Restore needs an empty app, such as a new phone. Existing practice data is never erased. Do not clear your current app to test a restore.")
        if (vm.recoveryRequired) Button(onClick = { vm.retryRecovery(onRestored) }, enabled = !vm.busy) { Text("Retry restore recovery") }
        vm.unfinishedCopy?.let { Text("A previous destination copy at $it may be incomplete.") }
        HorizontalDivider()
        Text("Legacy JSON backup", style = MaterialTheme.typography.titleLarge)
        Text("Save your Logs, Blocks, Goals, Routines and practice history as a data-only JSON file.")
        Text("Backups are plain text and include your notes. Keep private files in a location you trust. A failed export may leave an incomplete file; use only a successfully saved backup.")
        Text("Media Library originals, metadata, Block media associations and master annotations, Block-local audio settings and markers, practice markings, reference images, PDFs, recordings and take labels are not included in JSON backups. Keep original images and PDFs separately; they cannot be restored from JSON. Export each take from its recordings list to keep a separate audio copy. Audio cannot be restored from this JSON file.")
        Text("Use a complete ZIP to preserve weekly plans, standalone Transcribe activities and their practice time.")
        Button(onClick = { export.launch("practice.logger-backup-v7-" + System.currentTimeMillis() + ".json") }, enabled = !vm.navigationBlocked, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Export backup") }
        Text("Restore is for an empty app, such as a new phone. It never replaces or merges existing data. Do not uninstall or clear your current app to try a restore. Supported limit: 8 MiB, 20,000 records per type.")
        OutlinedButton(onClick = { restore.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }, enabled = !vm.navigationBlocked, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Choose backup to restore") }
        if (vm.busy) Text("Working…")
        vm.message?.let { Text(it, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
    }
    vm.pendingZip?.let { staged ->
        val data = staged.verified
        val rows = data.records
        AlertDialog(onDismissRequest = vm::cancel, title = { Text("Restore this complete backup?") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text("Created by practice.logger ${data.appVersion}.")
                    Text("${rows["blocks"].size} Blocks, ${rows["sessions"].size} sessions, ${rows["routines"].size} Routines, ${rows["goals"].size} Goals and ${rows["daily_logs"].size} Logs.")
                    Text("${rows["media_assets"].size} Library assets, ${rows["takes"].size} saved recordings and ${rows["block_images"].size} legacy visual references. ${android.text.format.Formatter.formatFileSize(androidx.compose.ui.platform.LocalContext.current, data.mediaBytes)} of original media was verified.")
                    if (rows["block_primary_goals"].isNotEmpty() || rows["placement_goal_snapshots"].isNotEmpty()) {
                        Text("${rows["block_primary_goals"].size} primary Goal assignments and ${rows["placement_goal_snapshots"].size} practice attribution snapshots.")
                    }
                    if (rows["practice_overlays"].isNotEmpty()) Text("${rows["practice_overlays"].size} editable practice markings.")
                    Text("Restore includes saved notes, audio annotations, standalone practice history and portable settings. Unfinished practice opens paused. Only an empty app can be restored.")
                }
            },
            confirmButton = { TextButton(onClick = { vm.restoreZip(onRestored) }, enabled = !vm.navigationBlocked) { Text("Restore complete backup") } },
            dismissButton = { TextButton(onClick = vm::cancel, enabled = !vm.navigationBlocked) { Text("Cancel") } })
    }
    vm.pending?.let { data ->
        AlertDialog(onDismissRequest = vm::cancel, title = { Text("Restore this backup?") },
            text = { Text("${data.logs.size} Log entries, ${data.blocks.size} Blocks, ${data.sessions.size} sessions and ${data.placements.size} session placements, ${data.routines.size} Routines and ${data.routinePlacements.size} Routine placements, ${data.goals.size} Goals, ${data.goalBlocks.size} Goal Block links and ${data.sessionGoals.size} Goal session links validated. Restore will proceed only if this app is still empty. Existing data is never erased.") },
            confirmButton = { TextButton(onClick = vm::restore, enabled = !vm.navigationBlocked) { Text("Restore backup") } },
            dismissButton = { TextButton(onClick = vm::cancel, enabled = !vm.navigationBlocked) { Text("Cancel") } })
    }
}
