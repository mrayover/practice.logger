package com.mrayover.practicelogger.composer

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal data class ComposerControl(
    val label: String,
    val priority: Int = 0,
    val width: Int? = null,
    val content: @Composable () -> Unit
)

@Composable internal fun ComposerButton(label: String, enabled: Boolean = true, selected: Boolean = false, modifier: Modifier = Modifier, click: () -> Unit) {
    TextButton(onClick = click, enabled = enabled,
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
        colors = ButtonDefaults.textButtonColors(containerColor = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = .22f) else androidx.compose.ui.graphics.Color.Transparent),
        modifier = modifier.heightIn(min = 40.dp).defaultMinSize(minWidth = 0.dp)) {
        Text(label, fontSize = 13.sp, maxLines = 1, softWrap = false, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
    }
}

/** Measure labels at the real font scale before choosing overflow; no off-screen controls or scroll strip. */
@Composable internal fun ComposerToolbar(tag: String, controls: List<ComposerControl>) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp)
    var overflow by remember { mutableStateOf(false) }
    BoxWithConstraints(Modifier.fillMaxWidth().testTag(tag)) {
        val widths = controls.map { item ->
            item.width?.let { (it * density.fontScale).dp } ?: with(density) { measurer.measure(item.label, style, softWrap = false).size.width.toDp() + 14.dp }.coerceAtLeast(40.dp)
        }
        val visible = controls.indices.toMutableSet()
        val overflowWidth = with(density) { measurer.measure("More ▾", style).size.width.toDp() + 16.dp }.coerceAtLeast(48.dp)
        var used = widths.fold(0.dp) { a,b -> a+b }
        if (used > maxWidth) {
            used += overflowWidth
            controls.indices.sortedWith(compareBy<Int> { controls[it].priority }.thenByDescending { it }).forEach { i ->
                if (used > maxWidth) { visible.remove(i); used -= widths[i] }
            }
        }
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 40.dp) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            controls.forEachIndexed { i,item -> if(i in visible) Box(Modifier.width(widths[i]), contentAlignment = Alignment.CenterStart) { item.content() } }
            if (visible.size < controls.size) Box {
                ComposerButton("More ▾", click = { overflow = true })
                DropdownMenu(expanded = overflow, onDismissRequest = { overflow = false }) {
                    controls.forEachIndexed { i,item -> if(i !in visible) Box(Modifier.padding(horizontal = 8.dp)) { item.content() } }
                }
            }
        }
        }
    }
}
