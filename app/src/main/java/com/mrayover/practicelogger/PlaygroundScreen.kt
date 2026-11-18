package com.mrayover.practicelogger

import android.app.Application
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.floor
import kotlin.math.roundToInt

class PlaygroundViewModel(app: Application) : AndroidViewModel(app) {
    internal val earnedCounts = BlockDatabase.appInstance(app).playground().observeEarnedCounts()
}

@Composable
internal fun PlaygroundScreen(onJournal: () -> Unit, vm: PlaygroundViewModel = viewModel()) {
    val earnedCounts by vm.earnedCounts.collectAsStateWithLifecycle(initialValue = null)
    val total = earnedCounts?.sumOf { it.count }
    var inventoryOpen by rememberSaveable { mutableStateOf(false) }
    var player by remember { mutableStateOf(PlaygroundHome.entrance) }
    var held by remember { mutableStateOf<HomeDirection?>(null) }
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    val canMove = lifecycle == Lifecycle.State.RESUMED && LocalWindowInfo.current.isWindowFocused && !inventoryOpen

    LaunchedEffect(held, canMove) {
        if (!canMove) {
            held = null
            return@LaunchedEffect
        }
        val direction = held ?: return@LaunchedEffect
        var previous = withFrameNanos { it }
        while (held == direction) {
            val now = withFrameNanos { it }
            if (held != direction) break
            // Bound catch-up after a stalled frame so resuming cannot cause a jump.
            val elapsed = ((now - previous) / 1_000_000_000f).coerceIn(0f, 0.05f)
            player = PlaygroundHome.move(player, direction, elapsed)
            previous = now
        }
    }

    Column(
        Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 12.dp).testTag("playground-home"),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onJournal, contentPadding = PaddingValues(horizontal = 6.dp)) { Text("Journal") }
            PageHeading("Playground", Modifier.weight(1f).padding(horizontal = 4.dp),
                style = MaterialTheme.typography.titleMedium, maxLines = 1)
            OutlinedButton(
                onClick = { held = null; inventoryOpen = true },
                modifier = Modifier.testTag("playground-inventory"),
                contentPadding = PaddingValues(horizontal = 10.dp)
            ) {
                Box(Modifier.size(12.dp).background(MaterialTheme.colorScheme.primary))
                Spacer(Modifier.width(6.dp))
                Text("Blocks ${total ?: "…"}", maxLines = 1)
            }
        }
        // This row indicates the current environment and previews inactive destinations.
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("Home", "Stats", "Workshop", "Kitchen").forEach { destination ->
                    val home = destination == "Home"
                    FilterChip(
                        selected = home, onClick = {}, enabled = home,
                        label = { Text(destination, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        modifier = Modifier.weight(1f).semantics { selected = home }
                    )
                }
            }
        }
        HomeViewport(Modifier.fillMaxWidth().weight(1f)) { player }
        OutlinedButton(
            onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth().testTag("playground-explore")
        ) { Text("Explore / Battle", maxLines = 1) }
        Row(
            Modifier.fillMaxWidth().padding(bottom = 4.dp).testTag("playground-controls"),
            horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically
        ) {
            val directionButton: @Composable (HomeDirection) -> Unit = { direction ->
                DirectionButton(direction, held == direction, canMove,
                    onHeld = { pressed ->
                        if (pressed) held = direction
                        else if (held == direction) held = null
                    },
                    onStep = { player = PlaygroundHome.move(player, direction, 0.10f) })
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                directionButton(HomeDirection.UP)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    directionButton(HomeDirection.LEFT)
                    directionButton(HomeDirection.DOWN)
                    directionButton(HomeDirection.RIGHT)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(PlaygroundHome::interact, Modifier.size(48.dp).testTag("playground-a"),
                    contentPadding = PaddingValues(0.dp)) { Text("A") }
                OutlinedButton({}, Modifier.size(48.dp).testTag("playground-b"), enabled = false,
                    contentPadding = PaddingValues(0.dp)) { Text("B") }
            }
        }
    }

    if (inventoryOpen) InventoryDialog(earnedCounts, total) { inventoryOpen = false }
}

@Composable
private fun DirectionButton(
    direction: HomeDirection, pressed: Boolean, enabled: Boolean, onHeld: (Boolean) -> Unit, onStep: () -> Unit
) {
    val updateHeld by rememberUpdatedState(onHeld)
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier.size(48.dp).testTag("playground-${direction.name.lowercase()}")
            .clip(RoundedCornerShape(10.dp))
            .background(if (pressed) colors.primary else colors.secondaryContainer)
            .semantics(mergeDescendants = true) {
                contentDescription = direction.label
                role = Role.Button
                if (enabled) onClick { onStep(); true }
            }.pointerInput(enabled, direction) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    awaitFirstDown().consume()
                    updateHeld(true)
                    try {
                        waitForUpOrCancellation()?.consume()
                    } finally {
                        updateHeld(false)
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(direction.arrow, style = MaterialTheme.typography.titleLarge,
            color = if (pressed) colors.onPrimary else colors.onSecondaryContainer)
    }
}

@Composable
private fun HomeViewport(modifier: Modifier, player: () -> HomePosition) {
    Canvas(modifier.clip(RoundedCornerShape(12.dp)).background(Color(0xFF293944)).testTag("playground-canvas")) {
        val room = PlaygroundHome
        val fit = minOf(size.width / room.width, size.height / room.height)
        val zoom = if (fit >= 1f) floor(fit) else fit
        val x = floor((size.width - room.width * zoom) / 2)
        val y = floor((size.height - room.height * zoom) / 2)
        translate(x, y) {
            scale(zoom, zoom, Offset.Zero) {
                val tile = room.tile.toFloat()
                for (row in 0 until room.rows) {
                    for (column in 0 until room.columns) {
                        val wall = row < 2 || row == room.rows - 1 || column == 0 || column == room.columns - 1
                        val alternate = (row + column) % 2 == 0
                        val color = when {
                            wall && alternate -> Color(0xFF546C78)
                            wall -> Color(0xFF607985)
                            alternate -> Color(0xFFD0B692)
                            else -> Color(0xFFDDC4A0)
                        }
                        drawRect(color, Offset(column * tile, row * tile), Size(tile, tile))
                    }
                }
                drawRect(Color(0xFF774C32), Offset(room.width / 2f - 12, 4f), Size(24f, 28f))
                drawRect(Color(0xFFAB7950), Offset(room.width / 2f - 9, 7f), Size(18f, 25f))
                val position = player()
                drawRect(Color(0xFF20323B), Offset(position.x.roundToInt().toFloat(), position.y.roundToInt().toFloat()),
                    Size(room.playerWidth.toFloat(), room.playerHeight.toFloat()))
            }
        }
    }
}

@Composable
private fun InventoryDialog(counts: List<EarnedBlockCount>?, total: Int?, onClose: () -> Unit) {
    val byColor = counts.orEmpty().associate { it.colorArgb to it.count }
    val colors = listOf("Red", "Orange", "Yellow", "Green", "Blue")
    val known = colors.indices.map { goalCompletionColor(it + 1) }
    val other = byColor.filterKeys { it != null && it !in known }.values.sum()
    Dialog(onDismissRequest = onClose) {
        Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().testTag("playground-inventory-panel")) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Block inventory", style = MaterialTheme.typography.titleLarge)
                colors.forEachIndexed { index, label ->
                    val color = checkNotNull(known[index])
                    InventoryRow(label, Color(color), total?.let { byColor[color] ?: 0 })
                }
                InventoryRow("Neutral", MaterialTheme.colorScheme.outline, total?.let { byColor[null] ?: 0 })
                if (other > 0) InventoryRow("Other saved colors", MaterialTheme.colorScheme.outline, other)
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                InventoryRow("Total available", null, total)
                TextButton(onClose, Modifier.align(Alignment.End)) { Text("Close") }
            }
        }
    }
}

@Composable
private fun InventoryRow(label: String, color: Color?, count: Int?) {
    Row(Modifier.fillMaxWidth().heightIn(min = 24.dp), verticalAlignment = Alignment.CenterVertically) {
        if (color != null) {
            Box(Modifier.size(12.dp).background(color))
            Spacer(Modifier.width(10.dp))
        }
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(count?.toString() ?: "…", Modifier.testTag("playground-count-$label"),
            style = MaterialTheme.typography.bodyMedium)
    }
}
