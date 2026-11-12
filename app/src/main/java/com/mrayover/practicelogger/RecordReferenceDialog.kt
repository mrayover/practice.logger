package com.mrayover.practicelogger

import android.Manifest
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun RecordReferenceDialog(
    blockId: String?,
    previousAudioId: String?,
    onClose: () -> Unit,
    onKept: (String) -> Unit,
    reviewedOrder: List<String>? = null,
    recorder: RecordingViewModel = viewModel(key = "reference-recorder")
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val activity = LocalContext.current as? Activity
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        recorder.referencePermissionResult(it)
    }
    DisposableEffect(lifecycle, blockId, recorder) {
        recorder.enterReference(blockId)
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) recorder.setForeground(true)
            if (event == Lifecycle.Event.ON_PAUSE) recorder.setForeground(false)
        }
        lifecycle.addObserver(observer)
        recorder.setForeground(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
        onDispose {
            lifecycle.removeObserver(observer)
            recorder.setForeground(false)
            if (activity?.isChangingConfigurations != true) recorder.leaveReference()
        }
    }
    LaunchedEffect(recorder.keptReferenceId) {
        val id = recorder.keptReferenceId ?: return@LaunchedEffect
        recorder.referenceHandled()
        onKept(id)
    }
    val review = recorder.reviewTake
    val canClose = !recorder.busy && !recorder.recording && review == null
    Dialog(onDismissRequest = { if (canClose) onClose() },
        properties = DialogProperties(dismissOnClickOutside = false, dismissOnBackPress = canClose)) {
        val view = LocalView.current
        DisposableEffect(view, recorder.recording) {
            val previous = view.keepScreenOn
            if (recorder.recording) view.keepScreenOn = true
            onDispose { view.keepScreenOn = previous }
        }
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (review != null) "Review Reference" else "Record Reference",
                    style = MaterialTheme.typography.titleLarge)
                if (recorder.recording) Text("Recording · ${practiceDuration(recorder.elapsedMs)}")
                if (review != null) Text("Duration · ${practiceDuration(review.durationMs)}")
                if (recorder.busy) Text("Preparing reference…")
                recorder.message?.let { Text(it, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    when {
                        recorder.recording -> Button(onClick = { recorder.finishCapture() }) { Text("Stop and Review") }
                        review != null -> {
                            OutlinedButton(enabled = !recorder.busy, onClick = {
                                if (recorder.playingId == review.id) recorder.stopPlayback() else recorder.previewReview()
                            }) { Text(if (recorder.playingId == review.id) "Stop Preview" else "Play Preview") }
                            Button(enabled = !recorder.busy, onClick = { recorder.keepReference(previousAudioId, reviewedOrder = reviewedOrder, append = blockId != null) }) { Text("Keep") }
                            TextButton(enabled = !recorder.busy, onClick = {
                                if (recorder.hasMicrophonePermission()) recorder.rerecordReview()
                                else permission.launch(Manifest.permission.RECORD_AUDIO)
                            }) { Text("Rerecord") }
                            TextButton(enabled = !recorder.busy, onClick = { recorder.discardReview(); onClose() }) { Text("Discard") }
                        }
                        else -> {
                            Button(enabled = recorder.ready && !recorder.busy, onClick = {
                                if (recorder.hasMicrophonePermission()) recorder.startCapture(blockId, reference = true)
                                else permission.launch(Manifest.permission.RECORD_AUDIO)
                            }) { Text("Start Recording") }
                            TextButton(enabled = canClose, onClick = onClose) { Text("Cancel") }
                        }
                    }
                }
            }
        }
    }
    recorder.referenceReplacement?.let { previous ->
        AlertDialog(onDismissRequest = recorder::dismissReferenceReplacement,
            title = { Text("Replace this Block's audio?") },
            text = { Text("Replacing this reference removes the Block's local playback settings and markers for the previous audio. The Library original and its master markers stay available.") },
            confirmButton = { TextButton(onClick = { recorder.keepReference(previousAudioId, previous, reviewedOrder) }) { Text("Replace reference") } },
            dismissButton = { TextButton(onClick = recorder::dismissReferenceReplacement) { Text("Cancel") } })
    }
}
