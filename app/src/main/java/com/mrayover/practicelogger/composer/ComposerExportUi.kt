package com.mrayover.practicelogger.composer

import android.app.Application
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.*
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.*
import java.io.File

internal class ComposerExportViewModel(app: Application, private val state: SavedStateHandle) : AndroidViewModel(app) {
    var active by mutableStateOf(false); private set
    var rendering by mutableStateOf(false); private set
    var progress by mutableIntStateOf(0); private set
    var message by mutableStateOf<String?>(null); private set
    var request by mutableStateOf<ComposerExportSnapshot?>(null); private set
    var format by mutableStateOf(ComposerExportFormat.M4A); private set
    var pickerLaunched=false
    val midiPickerOpen get() = pickerLaunched && format == ComposerExportFormat.MIDI
    val audioExportActive get() = active && format != ComposerExportFormat.MIDI
    private var job: Job?=null
    private var recoveryError: String?=null
    private val recovery: Job
    init {
        // A killed process cannot finish its export. Recover only export-owned staging/destination.
        recovery=viewModelScope.launch {
            try {
                val interrupted=state.get<String>("exportDestination")
                val removed=withContext(Dispatchers.IO) {
                    ComposerOfflineExport.directory(app).listFiles()?.filter { it.name.startsWith("render-") }?.forEach {
                        check(it.delete() || !it.exists()) { "Could not remove an interrupted export's temporary file." }
                    }
                    interrupted==null || runCatching { DocumentsContract.deleteDocument(app.contentResolver,Uri.parse(interrupted)) }.getOrDefault(false)
                }
                state["exportDestination"]=null
                if(!removed) message="An interrupted export could not be removed from its destination. Delete the incomplete file from the chosen folder."
            } catch(cancelled: CancellationException) { throw cancelled }
            catch(failure: Exception) {
                recoveryError="Export recovery failed: ${failure.message}. Free temporary storage and reopen Composer."
                message=recoveryError
            }
        }
    }
    fun prepare(project: CompositionProject, selections: List<String>, arrangement: Boolean, format: ComposerExportFormat) {
        if(active) return
        active=true; this.format=format; message=null; progress=0; pickerLaunched=false
        job=viewModelScope.launch {
            try {
                recovery.join()
                check(recoveryError==null) { recoveryError.orEmpty() }
                val captured=withContext(Dispatchers.Default) { ComposerExportSnapshot.capture(project,selections,arrangement,format) }
                val needed = if (format == ComposerExportFormat.MIDI) ComposerMidiExport.MAX_FILE_BYTES
                    else ComposerOfflineExport.estimatedBytes(captured, format)
                withContext(Dispatchers.IO) { ComposerOfflineExport.checkStorage(ComposerOfflineExport.directory(getApplication()),needed) }
                request=captured
            } catch(cancelled: CancellationException) { active=false; throw cancelled }
            catch(failure: Exception) { message=failure.message ?: "Could not prepare export."; active=false }
        }
    }
    fun destination(uri: Uri?, beforeRender: suspend () -> Unit) {
        val snapshot=request
        request=null
        pickerLaunched=false
        if(uri==null || snapshot==null) {
            if(uri!=null) viewModelScope.launch(Dispatchers.IO) { runCatching { DocumentsContract.deleteDocument(getApplication<Application>().contentResolver,uri) } }
            active=false; message="Export cancelled."; return
        }
        state["exportDestination"]=uri.toString()
        rendering=true
        job=viewModelScope.launch {
            var stage: File?=null
            var success=false
            var cleanupFailed=false
            try {
                val midi = format == ComposerExportFormat.MIDI
                if (!midi) beforeRender()
                val task=currentCoroutineContext()
                withContext(Dispatchers.IO) {
                    task.ensureActive()
                    val app=getApplication<Application>()
                    stage=File.createTempFile("render-",".${format.extension}",ComposerOfflineExport.directory(app))
                    if (midi) {
                        stage!!.outputStream().buffered().use { ComposerMidiExport.write(snapshot, it) { task.ensureActive() } }
                    } else {
                        ComposerOfflineExport.render(app,snapshot,format,stage!!,{ task.ensureActive() },{ value ->
                            viewModelScope.launch { progress=value }
                        })
                    }
                    task.ensureActive()
                    // Destination sees only finalized files. All copying is bounded and cancellation-aware.
                    app.contentResolver.openOutputStream(uri,"wt")?.use { output ->
                        stage!!.inputStream().use { input ->
                            val buffer=ByteArray(65536)
                            while(true) { task.ensureActive(); val count=input.read(buffer); if(count<0) break; output.write(buffer,0,count) }
                        }
                        output.flush()
                    } ?: error("Could not open the chosen destination.")
                    task.ensureActive()
                }
                success=true; message=if (midi) "MIDI exported · MID" else "Audio exported · ${format.extension.uppercase()}"
            } catch(cancelled: CancellationException) { message="Export cancelled." }
            catch(failure: Exception) { message="Export failed: ${failure.message ?: "Could not write the file."}" }
            finally {
                withContext(NonCancellable) {
                    withContext(Dispatchers.IO) {
                        stage?.delete()
                        if(!success) cleanupFailed=runCatching { DocumentsContract.deleteDocument(getApplication<Application>().contentResolver,uri) }.getOrDefault(false).not()
                    }
                    state["exportDestination"]=null
                    if(cleanupFailed) message="${message.orEmpty()} The provider could not remove the incomplete destination; delete it from the chosen folder."
                    rendering=false; active=false
                }
            }
        }
    }
    fun cancel() { job?.cancel(); if(!rendering) { request=null; pickerLaunched=false; active=false; message="Export cancelled." } }
    fun pickerFailed() {
        cancel()
        message = "Export failed: Could not open the Android file picker."
    }
    fun dismissMessage() { message=null }
}

@Composable internal fun ComposerExportUi(project: CompositionProject, selections: List<String>, audio: ComposerAudio,
    requested: Boolean, dismiss: () -> Unit, busy: (Boolean) -> Unit, vm: ComposerExportViewModel = viewModel()) {
    var arrangement by rememberSaveable { mutableStateOf(false) }
    var formatName by rememberSaveable { mutableStateOf(ComposerExportFormat.M4A.name) }
    val format=ComposerExportFormat.valueOf(formatName)
    SideEffect { busy(vm.active) }
    val launcher=rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        vm.destination(if(result.resultCode==Activity.RESULT_OK) result.data?.data else null) { audio.pauseForExport() }
    }
    val pending=vm.request
    LaunchedEffect(pending) {
        if(pending!=null && !vm.pickerLaunched) {
            vm.pickerLaunched=true
            try { launcher.launch(Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE); type=vm.format.mime
                putExtra(Intent.EXTRA_TITLE,exportFilename(pending.project.title,vm.format))
            }) } catch(failure: Exception) { vm.pickerFailed() }
        }
    }
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    DisposableEffect(vm,lifecycle) {
        val observer=LifecycleEventObserver { _,event -> if(event==Lifecycle.Event.ON_STOP && vm.rendering) vm.cancel() }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    if(requested && !vm.active) AlertDialog(onDismissRequest=dismiss,title={ Text("Export composition") },
        text={ Column(Modifier.verticalScroll(rememberScrollState())) {
            Text("Source")
            ComposerButton("Current Loop",selected=!arrangement) { arrangement=false }
            ComposerButton("Full Arrangement",selected=arrangement) { arrangement=true }
            Text("Format")
            ComposerButton("M4A / AAC",selected=format==ComposerExportFormat.M4A) { formatName=ComposerExportFormat.M4A.name }
            ComposerButton("WAV — uncompressed",selected=format==ComposerExportFormat.WAV) { formatName=ComposerExportFormat.WAV.name }
            ComposerButton("MIDI (.mid) — editable",selected=format==ComposerExportFormat.MIDI) { formatName=ComposerExportFormat.MIDI.name }
            Text(format.description)
            if (format != ComposerExportFormat.MIDI) {
                Text("Stereo · 48 kHz · includes mixer and master volume",style=MaterialTheme.typography.bodySmall)
            }
        } }, confirmButton={ TextButton(onClick={
            dismiss(); vm.prepare(project,selections,arrangement,format)
        }) { Text("Choose destination") } },dismissButton={ TextButton(onClick=dismiss) { Text("Cancel") } })
    if(vm.active && (vm.request==null || vm.rendering)) AlertDialog(onDismissRequest={},title={ Text(when {
        !vm.rendering -> "Preparing export"
        vm.format == ComposerExportFormat.MIDI -> "Saving MIDI"
        else -> "Rendering audio"
    }) },
        text={ Text(when {
            !vm.rendering -> "Checking composition and temporary storage…"
            vm.format == ComposerExportFormat.MIDI -> "Keep Composer open until saving finishes."
            else -> "${vm.progress}% · Keep Composer open until saving finishes."
        },Modifier.testTag("composer-export-progress")) },
        confirmButton={ TextButton(onClick=vm::cancel) { Text("Cancel") } })
    vm.message?.let { message -> AlertDialog(onDismissRequest=vm::dismissMessage,title={ Text("Export composition") },
        text={ Text(message,Modifier.testTag("composer-export-result")) },confirmButton={ TextButton(onClick=vm::dismissMessage) { Text("OK") } }) }
}
