package com.mrayover.practicelogger

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/** Thin Tools wrapper. T1 embeds PianoKeyboard with a borrowed-focus PianoAudioEngine. */
@Composable fun ReferencePianoScreen(onTools: () -> Unit) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var reset by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf<String?>(null) }
    var foreground by remember { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    val engine = remember(context) { PianoAudioEngine(context) { message = it; reset++ } }
    DisposableEffect(engine, lifecycle) {
        engine.setForeground(foreground)
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME || event == Lifecycle.Event.ON_PAUSE) {
                foreground = event == Lifecycle.Event.ON_RESUME; engine.setForeground(foreground)
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); engine.close() }
    }
    BackHandler(onBack = onTools)
    Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onTools) { Text("Back to Tools") }
        PageHeading("Reference Piano", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
        PianoKeyboard({ id, note -> engine.noteOn(id, note); Unit }, engine::noteOff, engine::stopAll,
            enabled = foreground, resetToken = reset)
        message?.let { Text(it) }
    }
}
