package com.mrayover.practicelogger

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
internal fun exportFolderPicker(storage: ExportStorage, onResult: (String?) -> Unit): () -> Unit {
    val scope = rememberCoroutineScope()
    val callback by rememberUpdatedState(onResult)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data = result.data
        val uri = data?.data
        if (result.resultCode == Activity.RESULT_OK && data != null && uri != null) {
            scope.launch {
                try {
                    storage.choose(uri, data.flags)
                    callback(null)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    callback("Could not use that folder. Choose a readable, writable folder or use Save As.")
                }
            }
        }
    }
    return {
        try {
            launcher.launch(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).addFlags(
                ExportStorage.MODES or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION))
        } catch (_: android.content.ActivityNotFoundException) {
            callback("Android’s folder picker is unavailable. Try Save As.")
        }
    }
}

@Composable
internal fun StorageSettings() {
    val context = LocalContext.current
    val storage = remember(context) { ExportStorage(context) }
    val scope = rememberCoroutineScope()
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    var destination by remember { mutableStateOf(storage.selected()) }
    var accessible by remember { mutableStateOf<Boolean?>(null) }
    var feedback by rememberSaveable { mutableStateOf<String?>(null) }
    var changing by remember { mutableStateOf(false) }
    val choose = exportFolderPicker(storage) { error ->
        destination = storage.selected()
        feedback = error ?: "Export folder saved."
    }
    LaunchedEffect(destination, lifecycle, feedback) {
        accessible = null
        if (lifecycle == Lifecycle.State.RESUMED) {
            accessible = destination?.let { storage.accessible(it) }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Storage", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        Text("Export destination", style = MaterialTheme.typography.titleMedium)
        val selected = destination
        if (selected == null) {
            Text("No export folder selected. Recordings can still be exported with Save As.")
        } else {
            Text(selected.label)
            if (accessible == false) Text("This folder is unavailable or access was lost. Choose a folder again or use Save As.")
        }
        OutlinedButton(onClick = choose, enabled = !changing) {
            Text(if (selected == null) "Choose Folder" else "Change Folder")
        }
        if (selected != null) TextButton(enabled = !changing, onClick = {
            changing = true
            scope.launch {
                try {
                    storage.forget()
                    destination = storage.selected()
                    feedback = "Export folder forgotten."
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    feedback = "Could not forget the folder. Try again."
                } finally {
                    changing = false
                }
            }
        }) { Text("Forget Folder") }
        feedback?.let { Text(it, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
    }
}