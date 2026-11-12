package com.mrayover.practicelogger

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.LocalDate

@Composable
internal fun WeeklyTargetSummary(progress: WeeklyTargetProgress?, title: String = "This Week") {
    Column(Modifier.fillMaxWidth().testTag("weekly-target-summary"), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge)
        if (progress == null) Text("Loading weekly practice…", style = MaterialTheme.typography.bodySmall)
        else {
            Text(if (progress.targetMinutes == null) "${targetMinuteText(progress.actualMs)} min practiced"
                else "${targetMinuteText(progress.actualMs)} / ${progress.targetMinutes} min planned",
                style = MaterialTheme.typography.bodyMedium)
            if (progress.targetMinutes != null) {
                LinearProgressIndicator(progress = { progress.fill }, modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = MaterialTheme.colorScheme.primary, trackColor = PracticeTheme.colors.scrollTrack)
                val difference = progress.differenceMs!!
                Text("${progress.percentage} of target · " + when {
                    difference > 0 -> "${targetMinuteText(difference)} minutes extra"
                    difference < 0 -> "${targetMinuteText(-difference)} minutes remaining"
                    else -> "Target reached"
                }, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
internal fun WeeklyTargetEditor(week: LocalDate, editable: Boolean, target: WeeklyPracticeTarget?,
    progress: WeeklyTargetProgress?, ready: Boolean, vm: WeeklyTargetViewModel) {
    var editing by rememberSaveable(week.toString()) { mutableStateOf(false) }
    var input by rememberSaveable(week.toString()) { mutableStateOf("") }
    Column {
        WeeklyTargetSummary(progress, "Weekly practice")
        if (editable) TextButton(onClick = { input = target?.targetMinutes?.toString().orEmpty(); editing = true },
            enabled = ready && !vm.busy, modifier = Modifier.testTag("edit-weekly-target")) {
            Text(if (target == null) "Set weekly target" else "Edit weekly target")
        } else Text("Historical target · read-only", style = MaterialTheme.typography.bodySmall)
    }
    if (editing) AlertDialog(
        onDismissRequest = { if (!vm.busy) editing = false },
        title = { Text(if (target == null) "Set weekly target" else "Edit weekly target") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("One target for this Sunday–Saturday week. Measures practice time, not musical mastery.")
                OutlinedTextField(input, { input = it }, label = { Text("Minutes per week") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = input.isNotEmpty() && parseWeeklyTarget(input) == null,
                    supportingText = { Text("1–10,080 whole minutes") }, enabled = !vm.busy,
                    modifier = Modifier.fillMaxWidth().testTag("weekly-target-input"))
                vm.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (target != null) TextButton(onClick = { vm.save(week, null) { editing = false } },
                    enabled = !vm.busy, modifier = Modifier.testTag("clear-weekly-target")) { Text("Clear target") }
            }
        },
        confirmButton = { TextButton(onClick = { parseWeeklyTarget(input)?.let { minutes -> vm.save(week, minutes) { editing = false } } },
            enabled = editable && !vm.busy && parseWeeklyTarget(input) != null,
            modifier = Modifier.testTag("save-weekly-target")) { Text(if (vm.busy) "Saving…" else "Save") } },
        dismissButton = { TextButton(onClick = { editing = false }, enabled = !vm.busy) { Text("Cancel") } }
    )
}
