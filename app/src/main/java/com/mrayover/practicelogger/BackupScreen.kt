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
    var busy by mutableStateOf(false); private set
    var message by mutableStateOf<String?>(null); private set
    var pending by mutableStateOf<BackupData?>(null); private set
    private fun work(operation: suspend () -> Unit) {
        if (busy) return
        busy = true; message = null
        viewModelScope.launch {
            try { operation() }
            catch (e: CancellationException) { throw e }
            catch (e: IllegalArgumentException) { message = e.message ?: "Invalid backup data." }
            catch (_: NonEmptyRestoreException) { message = "Restore needs an empty app. Your existing Blocks and practice history were kept. Use this backup on a new phone or spare empty installation." }
            catch (_: Exception) { message = "Could not complete this backup operation. Existing practice data was kept. Check the file, free space and access, then try again. Restore supports version 1, 2, 3, 4, 5, 6 and 7 backups up to 8 MiB and needs an empty app." }
            finally { busy = false }
        }
    }
    fun export(uri: Uri) = work {
        withContext(Dispatchers.IO) {
            val bytes = BackupCodec.encode(repository.snapshot())
            checkNotNull(getApplication<Application>().contentResolver.openOutputStream(uri, "wt")).use { it.write(bytes); it.flush() }
        }
        message = "Backup saved. Keep this JSON file somewhere safe; it contains your Logs, Blocks, Goals, Routines and practice notes in plain text."
    }
    fun inspect(uri: Uri) = work {
        pending = null
        val data = withContext(Dispatchers.IO) {
            val bytes = checkNotNull(getApplication<Application>().contentResolver.openInputStream(uri)).use(::readBackup)
            BackupCodec.decode(bytes)
        }
        pending = data
    }
    fun cancel() { if (!busy) pending = null }
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

@Composable fun BackupScreen(onHome: () -> Unit, vm: BackupViewModel = viewModel()) {
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> uri?.let(vm::export) }
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(vm::inspect) }
    BackHandler { if (!vm.busy) { vm.cancel(); onHome() } }
    Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Backup and restore", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
        Text("Save your Logs, Blocks, archived Blocks, Goals, Routines, practice history and unfinished sessions as a versioned JSON file. Choose a safe location using Android's file picker.")
        Text("Backups are plain text and include your notes. Keep private files in a location you trust. A failed export may leave an incomplete file; use only a successfully saved backup.")
        Text("Reference images, PDFs, recordings and take labels are not included in JSON backups. Keep original images and PDFs separately; they cannot be restored from JSON. Export each take from its recordings list to keep a separate audio copy. Audio cannot be restored from this JSON file.")
        Button(onClick = { export.launch("practice.logger-backup-v6-" + System.currentTimeMillis() + ".json") }, enabled = !vm.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Export backup") }
        Text("Restore is for an empty app, such as a new phone. It never replaces or merges existing data. Do not uninstall or clear your current app to try a restore. Supported limit: 8 MiB, 20,000 records per type.")
        OutlinedButton(onClick = { restore.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }, enabled = !vm.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Choose backup to restore") }
        if (vm.busy) Text("Working…")
        vm.message?.let { Text(it, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
    }
    vm.pending?.let { data ->
        AlertDialog(onDismissRequest = vm::cancel, title = { Text("Restore this backup?") },
            text = { Text("${data.logs.size} Log entries, ${data.blocks.size} Blocks, ${data.sessions.size} sessions and ${data.placements.size} session placements, ${data.routines.size} Routines and ${data.routinePlacements.size} Routine placements, ${data.goals.size} Goals, ${data.goalBlocks.size} Goal Block links and ${data.sessionGoals.size} Goal session links validated. Restore will proceed only if this app is still empty. Existing data is never erased.") },
            confirmButton = { TextButton(onClick = vm::restore, enabled = !vm.busy) { Text("Restore backup") } },
            dismissButton = { TextButton(onClick = vm::cancel, enabled = !vm.busy) { Text("Cancel") } })
    }
}
