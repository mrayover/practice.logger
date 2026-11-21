package com.mrayover.practicelogger

import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class EarnedBlocksViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = EarnedBlocksRepository(BlockDatabase.appInstance(app))
    internal val inventory = repository.inventory
    internal val history = repository.history
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LogDestinations(
    earnedSelected: Boolean,
    onJournal: () -> Unit,
    onEarnedBlocks: (() -> Unit)?,
    onPlayground: (() -> Unit)?,
    enabled: Boolean = true
) {
    if (onEarnedBlocks == null && onPlayground == null) return
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedSelectionChip(!earnedSelected, onJournal, { Text("Journal") }, enabled)
        onEarnedBlocks?.let {
            OutlinedSelectionChip(earnedSelected, it, { Text("Earned Blocks") }, enabled)
        }
        onPlayground?.let {
            OutlinedSelectionChip(false, it, { Text("Playground") }, enabled)
        }
    }
}

@Composable
internal fun EarnedBlocksScreen(
    onJournal: () -> Unit,
    onPlayground: () -> Unit,
    vm: EarnedBlocksViewModel = viewModel()
) {
    val inventory by vm.inventory.collectAsStateWithLifecycle(initialValue = null)
    val history by vm.history.collectAsStateWithLifecycle(initialValue = null)
    val dateFormat = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }
    BackHandler(onBack = onJournal)
    LazyColumn(
        Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 24.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            LogDestinations(true, onJournal, {}, onPlayground)
            PageHeading("Earned Blocks", style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.testTag("earned-blocks-heading").semantics { heading() })
            Column(Modifier.padding(vertical = 12.dp)) {
                BlockCounter("Lifetime Earned", inventory?.earned, "earned")
                BlockColorCounts(inventory?.balances, available = false)
            }
            HorizontalDivider()
            Text("Earned History", Modifier.padding(vertical = 12.dp).semantics { heading() },
                style = MaterialTheme.typography.titleLarge)
            if (history?.isEmpty() == true) Text("No Blocks earned yet.")
        }
        items(history.orEmpty(), key = { it.placementId }) { entry ->
            val earnedDate = Instant.ofEpochMilli(entry.completedAt).atZone(ZoneId.systemDefault()).format(dateFormat)
            val goal = entry.goalTitle ?: if (entry.goalId == null) "No Goal" else "Goal title unavailable"
            val color = entry.colorArgb?.let(::earnedColorName)
            Column(Modifier.fillMaxWidth().padding(vertical = 10.dp).testTag("earned-block-${entry.placementId}")) {
                Text(entry.name, style = MaterialTheme.typography.titleMedium)
                Text(earnedDate, style = MaterialTheme.typography.bodySmall)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    entry.colorArgb?.let { BlockColorSwatch(Color(it)) }
                    Text(if (color != null) "$goal · $color" else goal, style = MaterialTheme.typography.bodyMedium)
                }
            }
            HorizontalDivider()
        }
    }
}

@Composable
internal fun InventoryDialog(inventory: EarnedBlockInventory?, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose) {
        Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().testTag("playground-inventory-panel")) {
            Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Block inventory", style = MaterialTheme.typography.titleLarge)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BlockCounter("Lifetime Earned", inventory?.earned, "playground", Modifier.weight(1f))
                    BlockCounter("Used", inventory?.used, "playground", Modifier.weight(1f))
                    BlockCounter("Available", inventory?.available, "playground", Modifier.weight(1f))
                }
                HorizontalDivider()
                Text("Available by color", style = MaterialTheme.typography.titleSmall)
                BlockColorCounts(inventory?.balances, available = true)
                TextButton(onClose, Modifier.align(Alignment.End)) { Text("Close") }
            }
        }
    }
}

private val earnedColorNames = listOf("Red", "Orange", "Yellow", "Green", "Blue")
    .mapIndexed { index, name -> checkNotNull(goalCompletionColor(index + 1)) to name }.toMap()

private fun earnedColorName(colorArgb: Int) =
    earnedColorNames[colorArgb] ?: "Saved color #${(colorArgb and 0xFFFFFF).toString(16).uppercase().padStart(6, '0')}"

@Composable
private fun BlockCounter(label: String, count: Int?, screen: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(count?.toString() ?: "…", Modifier.testTag("$screen-count-$label"),
            style = MaterialTheme.typography.headlineSmall)
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun BlockColorCounts(balances: List<EarnedBlockBalance>?, available: Boolean) {
    val byColor = balances.orEmpty().associateBy { it.colorArgb to it.hasGoal }
    val savedColors = balances.orEmpty().mapNotNull { it.colorArgb }.distinct()
    val screen = if (available) "playground" else "earned"
    fun count(colorArgb: Int?, hasGoal: Boolean): Int? {
        if (balances == null) return null
        val row = byColor[colorArgb to hasGoal] ?: return 0
        return if (available) row.available else row.earned
    }
    for (color in (earnedColorNames.keys + savedColors).distinct()) {
        BlockColorCount(earnedColorName(color), Color(color), count(color, true), screen)
    }
    BlockColorCount("No Goal", MaterialTheme.colorScheme.outline, count(null, false), screen)
    if ((null to true) in byColor) {
        BlockColorCount("Goal without a color", MaterialTheme.colorScheme.outline, count(null, true), screen)
    }
}

@Composable
private fun BlockColorCount(label: String, color: Color, count: Int?, screen: String) {
    Row(Modifier.fillMaxWidth().heightIn(min = 24.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        BlockColorSwatch(color)
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(count?.toString() ?: "…", Modifier.testTag("$screen-count-$label"),
            style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun BlockColorSwatch(color: Color) {
    Box(Modifier.size(12.dp).background(color))
}
