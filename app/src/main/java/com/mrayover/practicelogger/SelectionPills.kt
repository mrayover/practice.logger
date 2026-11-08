package com.mrayover.practicelogger

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable fun OutlinedSelectionChip(selected: Boolean, onClick: () -> Unit,
    label: @Composable () -> Unit, enabled: Boolean = true, modifier: Modifier = Modifier) {
    FilterChip(selected = selected, onClick = onClick, label = label, enabled = enabled, modifier = modifier,
        border = BorderStroke(if (selected) 2.dp else 1.dp,
            if (!enabled) MaterialTheme.colorScheme.outlineVariant else if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline))
}

@Composable fun CatalogPill(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable ColumnScope.() -> Unit) {
    Surface(onClick = onClick, enabled = enabled, modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
        Row(Modifier.heightIn(min = 64.dp)) {
            Column(Modifier.weight(1f).padding(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp), content = content)
        }
    }
}
