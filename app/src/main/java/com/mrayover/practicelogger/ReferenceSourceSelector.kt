package com.mrayover.practicelogger

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** Chooses a view of existing links. Never attaches, reorders or removes an association. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ReferenceSourceSelector(label: String, sources: List<Pair<String, String>>, selected: String?,
    enabled: Boolean, modifier: Modifier = Modifier, choose: (String) -> Unit) {
    if (sources.isEmpty()) return
    var dialog by remember { mutableStateOf(false) }
    val index = sources.indexOfFirst { it.first == selected }.coerceAtLeast(0)
    Column(modifier.fillMaxWidth()) {
        Text("$label: ${sources[index].second}", maxLines = 2, overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag("$label-title"))
        if (sources.size > 1) FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton(onClick = { choose(sources[index - 1].first) }, enabled = enabled && index > 0,
                modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = "Previous $label" }) { Text("‹") }
            Text("${index + 1} of ${sources.size}", Modifier.padding(vertical = 12.dp))
            TextButton(onClick = { choose(sources[index + 1].first) }, enabled = enabled && index < sources.lastIndex,
                modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = "Next $label" }) { Text("›") }
            TextButton(onClick = { dialog = true }, enabled = enabled,
                modifier = Modifier.heightIn(min = 48.dp).testTag("$label-choose")) { Text("Choose") }
        }
    }
    if (dialog) AlertDialog(onDismissRequest = { dialog = false }, title = { Text("Choose $label") },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) {
            sources.forEach { (id, name) -> TextButton(onClick = { if (enabled) { dialog = false; choose(id) } },
                enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("$label-$id")) {
                Text(name)
            } }
        } }, confirmButton = { TextButton(onClick = { dialog = false }) { Text("Cancel") } })
}
