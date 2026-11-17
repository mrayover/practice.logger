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
    var format=ComposerExportFormat.M4A; private set
    var pickerLaunched=false
    private var job: Job?=null
    private val recovery: Job
    init {
        // A killed process cannot finish its export. Recover only export-owned staging/destination.
        recovery=viewModelScope.launch(Dispatchers.IO) {
            ComposerOfflineExport.directory(app).listFiles()?.filter { it.name.startsWith("render-") }?.forEach { it.delete() }
            state.get<String>("exportDestination")?.let { uri ->
                runCatching { DocumentsContract.deleteDocument(app.contentResolver,Uri.parse(uri)) }
                state["exportDestination"]=null
            }
        }
    }
    fun prepare(project: CompositionProject, selections: List<String>, arrangement: Boolean, format: ComposerExportFormat) {
        if(active) return
        active=true; this.format=format; message=null; progress=0; pickerLaunched=false
        job=viewModelScope.launch {
            try {
                recovery.join()
                val captured=withContext(Dispatchers.Default) { ComposerExportSnapshot.capture(project,selections,arrangement) }
                withContext(Dispatchers.IO) { ComposerOfflineExport.checkStorage(ComposerOfflineExport.directory(getApplication()),ComposerOfflineExport.estimatedBytes(captured,format)) }
                request=captured
            } catch(cancelled: CancellationException) { active=false; throw cancelled }
            catch(failure: Exception) { message=failure.message ?: "Could not prepare export."; active=false }
        }
    }
    fun destination(uri: Uri?, beforeRender: suspend () -> Unit) {
        val snapshot=request
        request=null
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
                beforeRender()
                val task=currentCoroutineContext()
                withContext(Dispatchers.IO) {
                    task.ensureActive()
                    val app=getApplication<Application>()
                    stage=File.createTempFile("render-",".${format.extension}",ComposerOfflineExport.directory(app))
                    ComposerOfflineExport.render(app,snapshot,format,stage!!,{ task.ensureActive() },{ value ->
                        viewModelScope.launch { progress=value }
                    })
                    task.ensureActive()
                    // Destination sees only finalized audio. All copying is bounded and cancellation-aware.
                    app.contentResolver.openOutputStream(uri,"wt")?.use { output ->
                        stage!!.inputStream().use { input ->
                            val buffer=ByteArray(65536)
                            while(true) { task.ensureActive(); val count=input.read(buffer); if(count<0) break; output.write(buffer,0,count) }
                        }
                        output.flush()
                    } ?: error("Could not open the chosen destination.")
                    task.ensureActive()
                }
                success=true; message="Audio exported · ${format.extension.uppercase()}"
            } catch(cancelled: CancellationException) { message="Export cancelled." }
            catch(failure: Exception) { message="Export failed: ${failure.message ?: "Could not write audio."}" }
            finally {
                withContext(NonCancellable+Dispatchers.IO) {
                    stage?.delete()
                    if(!success) cleanupFailed=runCatching { DocumentsContract.deleteDocument(getApplication<Application>().contentResolver,uri) }.getOrDefault(false).not()
                }
                state["exportDestination"]=null
                if(cleanupFailed) message="${message.orEmpty()} The provider could not remove the incomplete destination; delete it from the chosen folder."
                rendering=false; active=false
            }
        }
    }
    fun cancel() { job?.cancel(); if(!rendering) { request=null; active=false; message="Export cancelled." } }
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
            }) } catch(failure: Exception) { vm.cancel() }
        }
    }
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    DisposableEffect(vm,lifecycle) {
        val observer=LifecycleEventObserver { _,event -> if(event==Lifecycle.Event.ON_STOP && vm.rendering) vm.cancel() }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    if(requested && !vm.active) AlertDialog(onDismissRequest=dismiss,title={ Text("Export Audio") },
        text={ Column(Modifier.verticalScroll(rememberScrollState())) {
            Text("Source")
            ComposerButton("Current Loop",selected=!arrangement) { arrangement=false }
            ComposerButton("Full Arrangement",selected=arrangement) { arrangement=true }
            Text("Format")
            ComposerButton("M4A / AAC",selected=format==ComposerExportFormat.M4A) { formatName=ComposerExportFormat.M4A.name }
            ComposerButton("WAV — uncompressed",selected=format==ComposerExportFormat.WAV) { formatName=ComposerExportFormat.WAV.name }
            Text(format.description)
            Text("Stereo · 48 kHz · includes mixer and master volume",style=MaterialTheme.typography.bodySmall)
        } }, confirmButton={ TextButton(onClick={
            dismiss(); vm.prepare(project,selections,arrangement,format)
        }) { Text("Choose destination") } },dismissButton={ TextButton(onClick=dismiss) { Text("Cancel") } })
    if(vm.active && (vm.request==null || vm.rendering)) AlertDialog(onDismissRequest={},title={ Text(if(vm.rendering) "Rendering audio" else "Preparing export") },
        text={ Text(if(vm.rendering) "${vm.progress}% · Keep Composer open until saving finishes." else "Checking composition and temporary storage…",Modifier.testTag("composer-export-progress")) },
        confirmButton={ TextButton(onClick=vm::cancel) { Text("Cancel") } })
    vm.message?.let { message -> AlertDialog(onDismissRequest=vm::dismissMessage,title={ Text("Export Audio") },
        text={ Text(message,Modifier.testTag("composer-export-result")) },confirmButton={ TextButton(onClick=vm::dismissMessage) { Text("OK") } }) }
}
