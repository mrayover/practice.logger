package com.mrayover.practicelogger

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
internal fun StandaloneTranscribeScreen(media: MediaLibraryViewModel, onBack: () -> Unit) {
    val assets by media.assets.collectAsStateWithLifecycle()
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var choosing by rememberSaveable { mutableStateOf(false) }
    val asset = assets.find { it.id == selectedId && it.category == "AUDIO" }
    if (asset != null) {
        key(asset.id) {
            LibraryAudioScreen(asset, onBack, onChangeAudio = { choosing = true })
        }
    } else {
        BackHandler(onBack = onBack)
        Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) { Text("Back to Practice Now") }
            Text("Transcribe", style = MaterialTheme.typography.headlineLarge)
            if (selectedId != null) {
                Text(when {
                    media.readError -> "The Library could not load."
                    !media.ready -> "Loading audio…"
                    else -> "This recording is unavailable. Choose another audio file."
                })
            }
            ActionButton("Choose Audio", { choosing = true })
        }
    }
    if (choosing) LibraryMediaPicker(MediaRole.AUDIO, selectedId, onClose = { choosing = false },
        allowAudioImport = true, onChoose = { selectedId = it; choosing = false })
}
