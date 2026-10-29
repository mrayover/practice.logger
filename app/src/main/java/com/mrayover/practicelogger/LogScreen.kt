package com.mrayover.practicelogger

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun LogScreen(vm: LogViewModel, onHome: () -> Unit) {
    val status by vm.status.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    var picker by remember { mutableStateOf(false) }
    fun hideKeyboard() { focus.clearFocus(); keyboard?.hide() }
    fun home() { hideKeyboard(); vm.leave(onHome) }
    BackHandler { if (vm.weekly) vm.select(vm.date) else home() }
    DisposableEffect(lifecycle, vm) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) vm.background()
            if (event == Lifecycle.Event.ON_RESUME) vm.resume()
        }
        lifecycle.addObserver(observer); vm.resume()
        onDispose { lifecycle.removeObserver(observer) }
    }
    val format = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL) }
    Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.fillMaxWidth().heightIn(max = 260.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            BackToHomeButton(onClick = ::home, enabled = !vm.busy)
            Text(if (vm.weekly) "Weekly View" else "Log", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
            vm.error?.let { Text(it, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
            if (vm.error != null || status.startsWith("Could not")) TextButton(onClick = vm::retry, enabled = !vm.busy) { Text("Retry") }
            if (!vm.weekly) {
                Text(vm.date.format(format), style = MaterialTheme.typography.titleLarge, modifier = Modifier.testTag("log-date"))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { hideKeyboard(); picker = true }, enabled = !vm.busy) { Text("Choose date") }
                    OutlinedButton(onClick = { hideKeyboard(); vm.showWeek() }, enabled = !vm.busy && vm.ready) { Text("Weekly View") }
                }
            } else {
                Text("${vm.week} – ${vm.week.plusDays(6)}", style = MaterialTheme.typography.titleLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { vm.showWeek(vm.week.minusWeeks(1)) }, enabled = !vm.busy) { Text("Previous week") }
                    OutlinedButton(onClick = { vm.showWeek(vm.week.plusWeeks(1)) }, enabled = !vm.busy) { Text("Next week") }
                }
            }
        }
        if (vm.weekly) {
            LazyColumn(Modifier.weight(1f).testTag("log-week"), contentPadding = PaddingValues(bottom = 24.dp)) {
                items((0L..6L).map { vm.week.plusDays(it) }, key = { it.toString() }) { day ->
                    val entry = vm.rows.find { it.date == day.toString() }
                    Column(Modifier.fillMaxWidth().clickable(enabled = !vm.busy, onClickLabel = "Edit $day") { vm.select(day) }
                        .padding(vertical = 16.dp).testTag("log-day-$day"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(day.format(format), style = MaterialTheme.typography.titleMedium)
                        Text(entry?.text?.takeIf { it.isNotEmpty() } ?: "No entry")
                    }
                    HorizontalDivider()
                }
            }
        } else {
            if (!vm.ready) Text("Loading Log…")
            OutlinedTextField(vm.text, vm::edit, enabled = vm.ready && !vm.busy,
                modifier = Modifier.fillMaxWidth().weight(1f).testTag("log-editor"),
                textStyle = MaterialTheme.typography.bodyLarge)
            Text(if (vm.busy) "Loading…" else status, style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 12.dp).semantics { liveRegion = LiveRegionMode.Polite })
        }
    }
    if (picker) {
        // Material DatePicker uses UTC milliseconds to encode a calendar date, not an entry boundary.
        val state = rememberDatePickerState(initialSelectedDateMillis = vm.date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(), yearRange = 1..9999)
        DatePickerDialog(onDismissRequest = { picker = false }, confirmButton = {
            TextButton(onClick = { state.selectedDateMillis?.let { vm.select(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }; picker = false }) { Text("OK") }
        }, dismissButton = { TextButton(onClick = { picker = false }) { Text("Cancel") } }) { DatePicker(state) }
    }
}
