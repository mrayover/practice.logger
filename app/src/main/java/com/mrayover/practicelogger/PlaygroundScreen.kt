package com.mrayover.practicelogger

import android.content.res.AssetManager
import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.roundToInt

@Composable
internal fun PlaygroundScreen(onJournal: () -> Unit, vm: EarnedBlocksViewModel = viewModel()) {
    val inventory by vm.inventory.collectAsStateWithLifecycle(initialValue = null)
    val total = inventory?.available
    var inventoryOpen by rememberSaveable { mutableStateOf(false) }
    var player by remember { mutableStateOf(PlaygroundHome.entrance) }
    var held by remember { mutableStateOf<HomeDirection?>(null) }
    var walkElapsedNanos by remember { mutableLongStateOf(-1L) }
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    val canMove = lifecycle == Lifecycle.State.RESUMED && LocalWindowInfo.current.isWindowFocused && !inventoryOpen

    val moving = held != null
    fun updateDirection(direction: HomeDirection?) {
        held = direction
        if (direction == null) walkElapsedNanos = -1L
    }
    LaunchedEffect(moving, canMove) {
        if (!canMove) {
            updateDirection(null)
            return@LaunchedEffect
        }
        if (!moving) return@LaunchedEffect
        var previous = withFrameNanos { it }
        var walkStart = previous
        while (true) {
            val now = withFrameNanos { it }
            // Bound catch-up after a stalled frame so resuming cannot cause a jump.
            val elapsed = ((now - previous) / 1_000_000_000f).coerceIn(0f, 0.05f)
            held?.let {
                val next = PlaygroundHome.move(player, it, elapsed)
                if (next == player) {
                    walkElapsedNanos = -1L
                } else {
                    if (walkElapsedNanos < 0) walkStart = now
                    walkElapsedNanos = now - walkStart
                    player = next
                }
            }
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
                onClick = { updateDirection(null); inventoryOpen = true },
                modifier = Modifier.testTag("playground-inventory"),
                contentPadding = PaddingValues(horizontal = 10.dp)
            ) {
                Box(Modifier.size(12.dp).background(MaterialTheme.colorScheme.primary))
                Spacer(Modifier.width(6.dp))
                Text("Blocks ${total ?: "…"}", maxLines = 1)
            }
        }
        val colors = MaterialTheme.colorScheme
        val destinationShape = MaterialTheme.shapes.small
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("Home", "Stats", "Workshop", "Kitchen").forEach { destination ->
                val home = destination == "Home"
                Box(
                    Modifier.weight(1f).height(32.dp)
                        .background(if (home) colors.primaryContainer else Color.Transparent, destinationShape)
                        .border(1.dp, if (home) colors.primary else colors.outlineVariant, destinationShape)
                        .semantics(mergeDescendants = true) {
                            role = Role.Tab
                            selected = home
                            if (!home) disabled()
                        }.padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(destination, style = MaterialTheme.typography.labelSmall, maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (home) colors.onPrimaryContainer else colors.onSurfaceVariant.copy(alpha = 0.65f))
                }
            }
        }
        HomeViewport(Modifier.fillMaxWidth().weight(1f), { player }) {
            if (canMove) walkElapsedNanos else -1L
        }
        OutlinedButton(
            onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth().testTag("playground-explore")
        ) { Text("Explore / Battle", maxLines = 1) }
        Row(
            Modifier.fillMaxWidth().padding(bottom = 4.dp).testTag("playground-controls"),
            horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically
        ) {
            DirectionPad(canMove, onHeld = ::updateDirection) { direction, target ->
                DirectionButton(direction, held?.includes(direction) == true, canMove,
                    onStep = { player = PlaygroundHome.move(player, direction, 0.10f) },
                    modifier = target)
            }
            Box(Modifier.size(104.dp, 96.dp)) {
                OutlinedButton(PlaygroundHome::interact,
                    Modifier.align(Alignment.TopEnd).offset(y = 12.dp).size(48.dp).testTag("playground-a"),
                    contentPadding = PaddingValues(0.dp)) { Text("A") }
                OutlinedButton({},
                    Modifier.align(Alignment.BottomStart).offset(y = (-12).dp).size(48.dp).testTag("playground-b"),
                    enabled = false,
                    contentPadding = PaddingValues(0.dp)) { Text("B") }
            }
        }
    }

    if (inventoryOpen) InventoryDialog(inventory) { inventoryOpen = false }
}

@Composable
private fun DirectionPad(
    enabled: Boolean,
    onHeld: (HomeDirection?) -> Unit,
    button: @Composable (HomeDirection, Modifier) -> Unit
) {
    val updateHeld by rememberUpdatedState(onHeld)
    Box(Modifier.size(152.dp, 96.dp).testTag("playground-dpad").pointerInput(enabled) {
        if (!enabled) return@pointerInput
        val deadZone = 8.dp.toPx()
        awaitEachGesture {
            val pointers = mutableMapOf<PointerId, HomeDirection?>()
            try {
                do {
                    val event = awaitPointerEvent()
                    for (change in event.changes) {
                        val position = change.position
                        val inside = position.x >= 0f && position.x < size.width &&
                            position.y >= 0f && position.y < size.height
                        if (change.changedToDown() && inside) pointers[change.id] = null
                        if (change.id !in pointers) continue
                        if (change.isConsumed) {
                            pointers.remove(change.id)
                            continue
                        }
                        if (change.pressed) {
                            pointers[change.id] = if (inside) HomeDirection.fromTouch(
                                position.x - size.width / 2f,
                                position.y - size.height / 2f,
                                deadZone,
                                pointers[change.id]
                            ) else null
                        } else {
                            pointers.remove(change.id)
                        }
                        change.consume()
                    }
                    updateHeld(HomeDirection.combine(pointers.values))
                } while (event.changes.any { it.pressed })
            } finally {
                updateHeld(null)
            }
        }
    }) {
        Canvas(Modifier.matchParentSize()) {
            val fill = Color(0xFF463953)
            val corner = CornerRadius(5.dp.toPx())
            val bar = 42.dp.toPx()
            val inset = 12.dp.toPx()
            drawRoundRect(fill, Offset(center.x - bar / 2, 0f), Size(bar, size.height), corner)
            drawRoundRect(fill, Offset(inset, center.y - bar / 2), Size(size.width - 2 * inset, bar), corner)
        }
        button(HomeDirection.UP, Modifier.align(Alignment.TopCenter).size(48.dp, 40.dp))
        button(HomeDirection.DOWN, Modifier.align(Alignment.BottomCenter).size(48.dp, 40.dp))
        button(HomeDirection.LEFT, Modifier.align(Alignment.CenterStart).offset(x = 3.dp).size(48.dp))
        button(HomeDirection.RIGHT, Modifier.align(Alignment.CenterEnd).offset(x = (-3).dp).size(48.dp))
    }
}

@Composable
private fun DirectionButton(
    direction: HomeDirection, pressed: Boolean, enabled: Boolean, onStep: () -> Unit,
    modifier: Modifier
) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier.testTag("playground-${direction.name.lowercase()}")
            .semantics(mergeDescendants = true) {
                contentDescription = direction.label
                role = Role.Button
                if (enabled) onClick { onStep(); true }
            },
        contentAlignment = Alignment.Center
    ) {
        if (pressed) {
            Box(Modifier.size(30.dp).background(colors.primary, CircleShape))
        }
        Text(direction.arrow, style = MaterialTheme.typography.titleLarge,
            color = if (pressed) colors.onPrimary else Color(0xFFF0E8FF))
    }
}

@Composable
private fun HomeViewport(modifier: Modifier, player: () -> HomePosition, walkElapsedNanos: () -> Long) {
    val assets = LocalContext.current.assets
    val sprite = remember(assets) { PlaygroundSprite.load(assets) }
    val floorTile = remember(assets) { assets.loadHomeTile("FloorTile1.png") }
    val wallTile = remember(assets) { assets.loadHomeTile("Wall1.png") }
    val door = remember(assets) { assets.loadHomeTile("Door1.png") }
    val cornerRadius = 12.dp
    Canvas(modifier.clip(RoundedCornerShape(cornerRadius)).background(Color(0xFF293944)).testTag("playground-canvas")) {
        val room = PlaygroundHome
        val border = 1.dp.toPx()
        val zoom = minOf(
            (size.width - 2 * border) / room.width,
            (size.height - 2 * border) / room.height,
            // Keep the rectangular room inside the chord of each rounded viewport corner.
            (size.width + size.height - 2 * cornerRadius.toPx()) / (room.width + room.height)
        ).coerceAtLeast(0f)
        if (zoom == 0f) return@Canvas
        val x = (size.width - room.width * zoom) / 2
        val y = (size.height - room.height * zoom) / 2
        fun drawTile(image: ImageBitmap, left: Int, top: Int, tileSize: Int, sideWall: Boolean = false) {
            // Snap shared edges to the same screen pixels so fractional zoom leaves no tile seams.
            val pixelLeft = (x + left * zoom).roundToInt()
            val pixelTop = (y + top * zoom).roundToInt()
            val pixelRight = (x + (left + tileSize) * zoom).roundToInt()
            val pixelBottom = (y + (top + tileSize) * zoom).roundToInt()
            val width = pixelRight - pixelLeft
            val height = pixelBottom - pixelTop
            if (sideWall) {
                // Rotate from the snapped top-right corner and swap dimensions before drawing.
                rotate(90f, Offset(pixelRight.toFloat(), pixelTop.toFloat())) {
                    drawImage(image, dstOffset = IntOffset(pixelRight, pixelTop),
                        dstSize = IntSize(height, width), filterQuality = FilterQuality.None)
                }
            } else {
                drawImage(image, dstOffset = IntOffset(pixelLeft, pixelTop),
                    dstSize = IntSize(width, height), filterQuality = FilterQuality.None)
            }
        }
        for (row in 0 until room.rows) {
            for (column in 0 until room.columns) {
                val horizontalWall = row < 2 || row == room.rows - 1
                val sideWall = !horizontalWall && (column == 0 || column == room.columns - 1)
                val tile = if (horizontalWall || sideWall) wallTile else floorTile
                drawTile(tile, column * room.tile, row * room.tile, room.tile, sideWall)
            }
        }
        // Scale the door art around the original doorway center at (room.width / 2, 18).
        val doorSize = 36
        drawTile(door, (room.width - doorSize) / 2, 18 - doorSize / 2, doorSize)
        val position = player()
        val frame = sprite.frameAt(walkElapsedNanos())
        val left = position.x + (room.playerWidth - frame.size.width) / 2f
        val top = position.y + room.playerHeight - frame.feet
        drawImage(
            image = sprite.image,
            srcOffset = frame.offset,
            srcSize = frame.size,
            dstOffset = IntOffset((x + left * zoom).roundToInt(), (y + top * zoom).roundToInt()),
            dstSize = IntSize((frame.size.width * zoom).roundToInt(), (frame.size.height * zoom).roundToInt()),
            filterQuality = FilterQuality.None
        )
    }
}

private fun AssetManager.loadHomeTile(filename: String): ImageBitmap =
    open("playground/$filename").use { stream ->
        val bitmap = checkNotNull(BitmapFactory.decodeStream(stream))
        require(bitmap.width == bitmap.height) { "Expected square Playground artwork: $filename" }
        bitmap.asImageBitmap()
    }
