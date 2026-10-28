package com.mrayover.practicelogger

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch
import kotlin.math.abs
import java.util.Locale

@Composable fun ToolScreen(tool: String, vm: ToolsViewModel, onTools: () -> Unit) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(tool, lifecycle, vm) {
        vm.enter(tool)
        vm.setForeground(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) vm.setForeground(true)
            if (event == Lifecycle.Event.ON_PAUSE) vm.setForeground(false)
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); vm.leave(); vm.setForeground(false) }
    }
    BackHandler(onBack = onTools)
    Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TextButton(onClick = onTools, modifier = Modifier.align(Alignment.Start)) { Text("Back to Tools") }
        Text(tool.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() })
        when (tool) {
            "metronome" -> {
                Text(vm.bpm.toString(), Modifier.fillMaxWidth().testTag("tool-bpm"), textAlign = TextAlign.Center,
                    fontSize = 64.sp, lineHeight = 72.sp, fontWeight = FontWeight.Bold, color = PracticeGreen)
                Text("BPM")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    TempoButton("−", "Decrease BPM by 1", vm.bpm > 30, Modifier.weight(1f)) { vm.changeBpm(-1) }
                    TempoButton("+", "Increase BPM by 1", vm.bpm < 300, Modifier.weight(1f)) { vm.changeBpm(1) }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    TempoButton("−5", "Decrease BPM by 5", vm.bpm > 30, Modifier.weight(1f)) { vm.changeBpm(-5) }
                    TempoButton("+5", "Increase BPM by 5", vm.bpm < 300, Modifier.weight(1f)) { vm.changeBpm(5) }
                }
                HomeButton(if (vm.clicking) "Stop click" else "Start click", vm::toggleClick)
                Text("30–300 BPM · media volume")
            }
            "tuner" -> TunerControls(vm)
            "timer" -> TimerControls(vm)
        }
        vm.message?.let { Text(it, Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
    }
}

@Composable private fun TempoButton(label: String, description: String, enabled: Boolean, modifier: Modifier, action: () -> Unit) {
    Button(onClick = action, enabled = enabled, modifier = modifier.heightIn(min = 56.dp).semantics { contentDescription = description }) {
        Text(label, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable private fun TunerControls(vm: ToolsViewModel) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        // Explicit second tap prevents permission/lifecycle transitions from unexpectedly opening the mic.
        if (!granted) vm.permissionDenied()
    }
    val pitch = vm.pitch
    Text(pitch?.note ?: "—", Modifier.fillMaxWidth().testTag("tuner-note"), textAlign = TextAlign.Center,
        fontSize = 56.sp, lineHeight = 64.sp, fontWeight = FontWeight.Bold, color = PracticeGreen)
    Text(if (pitch == null) "Play one steady note" else String.format(Locale.US, "%.1f Hz", pitch.frequency))
    Text(if (pitch == null) "A4 = 440 Hz" else String.format(Locale.US, "%+.0f cents", pitch.cents),
        style = MaterialTheme.typography.headlineSmall)
    Text(when {
        pitch == null && vm.listening -> "Listening…"
        pitch == null -> "Chromatic tuner · 50–1600 Hz"
        abs(pitch.cents) <= 5 -> "In tune"
        pitch.cents < 0 -> "Flat — tune up"
        else -> "Sharp — tune down"
    }, modifier = Modifier.testTag("tuner-status"))
    HomeButton(if (vm.listening) "Stop tuner" else "Start tuner") {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) vm.toggleTuner()
        else launcher.launch(Manifest.permission.RECORD_AUDIO)
    }
    Text("Allow microphone access, then tap Start tuner. Play one note at a time in a quiet room.")
}

@Composable private fun TimerControls(vm: ToolsViewModel) {
    Text("Stopwatch", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
    Text(practiceTime(vm.stopwatchMs), Modifier.fillMaxWidth().testTag("stopwatch-value"),
        textAlign = TextAlign.Center, style = MaterialTheme.typography.displayMedium)
    HomeButton(if (vm.stopwatchRunning) "Stop stopwatch" else "Start stopwatch", vm::toggleStopwatch)
    TextButton(onClick = vm::resetStopwatch) { Text("Reset stopwatch") }
    HorizontalDivider()
    Text("Minute timer", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
    MinuteWheel(vm.minutes, !vm.countdownRunning, vm::selectMinutes)
    Text("${vm.minutes} ${if (vm.minutes == 1) "minute" else "minutes"}")
    Text(practiceTime(vm.remainingMs, roundUp = true), Modifier.fillMaxWidth().testTag("countdown-value"),
        textAlign = TextAlign.Center, style = MaterialTheme.typography.displayMedium)
    if (vm.remainingMs == 0L) Text("Time’s up", Modifier.semantics { liveRegion = LiveRegionMode.Polite })
    HomeButton(if (vm.countdownRunning) "Stop timer" else "Start timer", vm::toggleCountdown)
    TextButton(onClick = vm::resetCountdown) { Text("Reset timer") }
    Text("Roll to choose 1–180 whole minutes. Timers pause when you leave this screen.")
}

@Composable private fun MinuteWheel(minutes: Int, enabled: Boolean, changed: (Int) -> Unit) {
    val state = rememberLazyListState(initialFirstVisibleItemIndex = minutes - 1)
    val scope = rememberCoroutineScope()
    val rowHeight = with(LocalDensity.current) { 32.sp.toDp() + 24.dp }
    val latestChanged by rememberUpdatedState(changed)
    LaunchedEffect(state) {
        snapshotFlow { if (state.isScrollInProgress) null else state.layoutInfo.let { layout ->
            val center = (layout.viewportStartOffset + layout.viewportEndOffset) / 2
            layout.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2 - center) }?.index
        } }.collect { index -> if (index != null) latestChanged(index + 1) }
    }
    Box(Modifier.width(160.dp).height(rowHeight * 3).testTag("minute-wheel").semantics {
        contentDescription = "Minutes"
        stateDescription = minutes.toString()
        progressBarRangeInfo = ProgressBarRangeInfo(minutes.toFloat(), 1f..180f, 178)
        if (enabled) setProgress { value ->
            val target = value.toInt().coerceIn(1, 180)
            scope.launch { state.scrollToItem(target - 1) }
            true
        }
    }) {
        Box(Modifier.fillMaxWidth().height(rowHeight).align(Alignment.Center).border(1.dp, PracticeGreen))
        LazyColumn(state = state, flingBehavior = rememberSnapFlingBehavior(state), userScrollEnabled = enabled,
            contentPadding = PaddingValues(vertical = rowHeight), modifier = Modifier.fillMaxSize()) {
            items(180) { index ->
                Box(Modifier.fillMaxWidth().height(rowHeight), contentAlignment = Alignment.Center) {
                    Text((index + 1).toString(), fontSize = 24.sp, fontWeight = if (minutes == index + 1) FontWeight.Bold else FontWeight.Normal,
                        color = PracticeGreen.copy(alpha = if (minutes == index + 1) 1f else 0.5f))
                }
            }
        }
    }
}
