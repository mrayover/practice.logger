package com.mrayover.practicelogger

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
internal fun WeeklyPlanSummary(progress: PracticeMinutes?, editable: Boolean, enabled: Boolean, onEdit: () -> Unit) {
    Column {
        Column(Modifier.fillMaxWidth().testTag("weekly-target-summary"), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("Weekly practice", style = MaterialTheme.typography.labelLarge)
            if (progress == null) Text("Loading weekly practice…", style = MaterialTheme.typography.bodySmall)
            else {
                Text(progress.label, style = MaterialTheme.typography.bodyMedium)
                if (progress.plannedMinutes > 0) {
                    LinearProgressIndicator(progress = { progress.fill }, modifier = Modifier.fillMaxWidth().height(4.dp),
                        color = MaterialTheme.colorScheme.primary, trackColor = PracticeTheme.colors.scrollTrack)
                    Text(progress.status, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        if (editable) TextButton(onClick = onEdit, enabled = enabled, modifier = Modifier.testTag("edit-weekly-plan")) {
            Text("Edit weekly plan")
        } else Text("Past week", style = MaterialTheme.typography.bodySmall)
    }
}
