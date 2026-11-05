package com.mrayover.practicelogger

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

internal class OverlayWorkspace(
    val reference: BlockVisualReference,
    private val vm: ImageReferenceViewModel,
    private val scope: CoroutineScope
) {
    var visible by mutableStateOf(true)
    var editing by mutableStateOf(false)
    var tool by mutableStateOf<OverlayTool?>(null)
    var selected by mutableStateOf<String?>(null)
    var peeking by mutableStateOf(false); private set
    var lastEdit by mutableStateOf<OverlayEdit?>(null); private set
    var busy by mutableStateOf(false); private set
    var message by mutableStateOf<String?>(null); private set
    var copyTargets by mutableStateOf<List<Block>?>(null); private set
    private var peekTimer: Job? = null

    fun endEdit() {
        editing = false
        tool = null
        selected = null
    }

    fun startTool(next: OverlayTool) {
        tool = next
        selected = null
    }

    fun select(id: String?) {
        tool = null
        selected = id
    }

    fun peek(on: Boolean) {
        peekTimer?.cancel()
        peekTimer = null
        peeking = on
    }

    fun accessiblePeek() {
        peek(true)
        peekTimer = scope.launch {
            delay(3000)
            peeking = false
        }
    }

    fun newShape(page: Int, kind: OverlayTool, rect: Rect) = PracticeOverlay(
        UUID.randomUUID().toString(), reference.blockId, reference.identity, page, kind.name,
        rect.left, rect.top, rect.right, rect.bottom, 0, 0
    )

    fun save(before: PracticeOverlay?, after: PracticeOverlay?, pages: Int) {
        if (before == after) return
        perform {
            val change = vm.overlays.edit(OverlayEdit(before, after), pages, appendOnPage = before == null)
            lastEdit = change
            select(change.after?.id)
        }
    }

    fun undo(pages: Int) {
        val change = lastEdit ?: return
        perform {
            val restored = vm.overlays.edit(change.inverse(), pages)
            lastEdit = null
            select(restored.after?.id)
        }
    }

    fun chooseCopyTarget() = perform { copyTargets = vm.overlayCopyTargets(reference) }
    fun closeCopy() { copyTargets = null }
    fun copyTo(block: String) {
        closeCopy()
        perform {
            val copied = vm.copyOverlays(reference, block)
            message = "Copied $copied markings."
        }
    }

    private fun perform(action: suspend () -> Unit) {
        if (busy) return
        busy = true
        message = null
        scope.launch {
            try { action() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: IllegalArgumentException) { message = failure.message ?: "Could not save markings." }
            catch (failure: IllegalStateException) { message = failure.message ?: "Could not save markings." }
            catch (_: Exception) { message = "Could not save markings. Try again." }
            finally { busy = false }
        }
    }
}

private fun OverlayTool.label() = name.lowercase().replaceFirstChar { it.uppercase() }

@OptIn(ExperimentalLayoutApi::class)
@Composable internal fun OverlayToolbar(workspace: OverlayWorkspace, shapes: List<PracticeOverlay>,
    pageShapes: List<PracticeOverlay>, pages: Int, active: Boolean) {
    var menu by remember { mutableStateOf(false) }
    val selection = pageShapes.find { it.id == workspace.selected }
    val canPeek = active && workspace.visible && pageShapes.any { it.type == OverlayTool.COVER.name }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { workspace.peek(false) }
    LaunchedEffect(canPeek) { if (!canPeek) workspace.peek(false) }
    LaunchedEffect(active) {
        if (!active) {
            menu = false
            workspace.closeCopy()
        }
    }
    DisposableEffect(Unit) { onDispose { workspace.peek(false) } }

    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box {
            TextButton(onClick = { menu = true }, enabled = active, modifier = Modifier.testTag("overlay-tools")
                .semantics {
                    stateDescription = when {
                        !workspace.visible -> "Hidden"
                        workspace.tool != null -> "${workspace.tool!!.label()} tool selected"
                        workspace.editing -> "Editing"
                        else -> "Visible"
                    }
                }) { Text(if (workspace.editing) workspace.tool?.label() ?: "Markings" else "Markings") }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                @Composable fun Item(label: String, enabled: Boolean = true, action: () -> Unit) {
                    DropdownMenuItem(text = { Text(label) }, enabled = enabled && !workspace.busy,
                        onClick = { menu = false; action() })
                }
                if (workspace.editing) {
                    for (tool in OverlayTool.entries) Item(tool.label()) { workspace.startTool(tool) }
                    if (selection != null) {
                        if (selection.type == OverlayTool.HIGHLIGHT.name) {
                            val colors = listOf("Yellow", "Blue", "Pink")
                            for ((index, color) in colors.withIndex()) if (index != selection.appearance) {
                                Item("Highlight color · $color") { workspace.save(selection, selection.copy(appearance = index), pages) }
                            }
                        }
                        Item("Delete marking") { workspace.save(selection, null, pages) }
                    }
                } else Item("Edit markings") { workspace.visible = true; workspace.editing = true }
                if (workspace.lastEdit != null) Item("Undo last edit") { workspace.undo(pages) }
                if (shapes.isNotEmpty()) {
                    Item(if (workspace.visible) "Hide markings" else "Show markings") {
                        workspace.visible = !workspace.visible
                        if (!workspace.visible) workspace.endEdit()
                    }
                    Item("Copy overlays to Block") { workspace.chooseCopyTarget() }
                }
                if (workspace.editing && pageShapes.isNotEmpty()) {
                    HorizontalDivider()
                    for ((index, shape) in pageShapes.withIndex()) {
                        val label = "${OverlayTool.valueOf(shape.type).label()} ${index + 1}"
                        Item(if (selection?.id == shape.id) "$label · selected" else "Select $label") { workspace.select(shape.id) }
                    }
                }
            }
        }
        if (workspace.editing) TextButton(onClick = workspace::endEdit) { Text("Done") }
        if (canPeek) PeekButton(workspace)
    }
    workspace.message?.let {
        Text(it, Modifier.semantics { liveRegion = LiveRegionMode.Polite }, style = MaterialTheme.typography.bodySmall)
    }
    workspace.copyTargets?.takeIf { active }?.let { targets ->
        AlertDialog(
            onDismissRequest = workspace::closeCopy,
            title = { Text("Copy overlays to Block") },
            text = {
                if (targets.isEmpty()) Text("No other Block uses this exact visual original.")
                else LazyColumn(Modifier.heightIn(max = 280.dp)) {
                    items(targets, key = { it.id }) { block ->
                        TextButton(onClick = { workspace.copyTo(block.id) }, modifier = Modifier.fillMaxWidth()) {
                            Text(if (block.archived) "${block.name} · archived" else block.name)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = workspace::closeCopy) { Text("Cancel") } }
        )
    }
}

@Composable private fun PeekButton(workspace: OverlayWorkspace) {
    Box(Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
        .testTag("overlay-peek")
        .semantics(mergeDescendants = true) {
            role = Role.Button
            stateDescription = if (workspace.peeking) "Revealing covers" else "Covers visible"
            onClick("Reveal covers for three seconds") { workspace.accessiblePeek(); true }
        }
        .focusable()
        .pointerInput(workspace) {
            detectTapGestures(onPress = {
                workspace.peek(true)
                try { tryAwaitRelease() } finally { workspace.peek(false) }
            })
        }
        .padding(horizontal = 12.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
        Text("Peek", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
    }
}

private enum class ResizeCorner { TOP_LEFT, BOTTOM_RIGHT }

private val highlights = listOf(Color(0xFFFFD54F), Color(0xFF42D8FA), Color(0xFFF783B9))

@Composable internal fun BoxScope.OverlayLayer(workspace: OverlayWorkspace, transform: ReferencePageTransform,
    shapes: List<PracticeOverlay>, page: Int, pages: Int, active: Boolean) {
    var draft by remember { mutableStateOf<PracticeOverlay?>(null) }
    val currentShapes by rememberUpdatedState(shapes)
    val enabled = active && workspace.visible && workspace.editing && !workspace.busy
    val selected = shapes.find { it.id == workspace.selected }
    val input = if (!enabled) Modifier else Modifier.pointerInput(workspace, transform, page, pages) {
        awaitEachGesture {
            val down = awaitFirstDown()
            if (down.position !in transform.bounds) return@awaitEachGesture
            val tool = workspace.tool
            val start = transform.toPage(down.position)
            val radius = 24.dp.toPx()
            val selectedShape = currentShapes.find { it.id == workspace.selected }
            val selectedBounds = selectedShape?.let { transform.toScreen(it.rectangle()) }
            val corner = if (tool != null || selectedBounds == null) null else when {
                (down.position - selectedBounds.topLeft).getDistance() <= radius -> ResizeCorner.TOP_LEFT
                (down.position - selectedBounds.bottomRight).getDistance() <= radius -> ResizeCorner.BOTTOM_RIGHT
                else -> null
            }
            val before = when {
                tool != null -> null
                corner != null -> selectedShape
                else -> currentShapes.lastOrNull { start in it.rectangle() }
            }
            if (tool == null) workspace.select(before?.id)
            val newShape = tool?.let { workspace.newShape(page, it, Rect.Zero) }
            down.consume()
            var committed: PracticeOverlay? = null
            try {
                var dragged = false
                while (true) {
                    val event = awaitPointerEvent()
                    val touch = event.changes.singleOrNull()?.takeIf { it.id == down.id && !it.isConsumed } ?: break
                    dragged = dragged || (touch.position - down.position).getDistance() >= viewConfiguration.touchSlop
                    if (dragged) {
                        val point = transform.toPage(touch.position)
                        draft = if (before != null) {
                            val rect = before.rectangle()
                            val delta = point - start
                            val minWidth = minOf(rect.width, 10.dp.toPx() / transform.bounds.width)
                            val minHeight = minOf(rect.height, 10.dp.toPx() / transform.bounds.height)
                            val edited = when (corner) {
                                ResizeCorner.TOP_LEFT -> rect.copy(
                                    left = (rect.left + delta.x).coerceIn(0f, (rect.right - minWidth).coerceAtLeast(0f)),
                                    top = (rect.top + delta.y).coerceIn(0f, (rect.bottom - minHeight).coerceAtLeast(0f))
                                )
                                ResizeCorner.BOTTOM_RIGHT -> rect.copy(
                                    right = (rect.right + delta.x).coerceIn((rect.left + minWidth).coerceAtMost(1f), 1f),
                                    bottom = (rect.bottom + delta.y).coerceIn((rect.top + minHeight).coerceAtMost(1f), 1f)
                                )
                                null -> moveOverlay(rect, delta)
                            }
                            before.withRectangle(edited)
                        } else newShape?.withRectangle(overlayRectangle(start, point))
                    }
                    touch.consume()
                    if (!touch.pressed) {
                        committed = draft?.takeIf {
                            val bounds = transform.toScreen(it.rectangle())
                            validOverlayBounds(it.left, it.top, it.right, it.bottom) &&
                                (before != null || (bounds.width >= 10.dp.toPx() && bounds.height >= 10.dp.toPx()))
                        }
                        break
                    }
                }
            } finally { draft = null }
            committed?.let { workspace.save(before, it, pages) }
        }
    }
    Canvas(Modifier.matchParentSize().testTag("overlay-layer").then(input).semantics {
        stateDescription = when {
            !workspace.visible -> "Markings hidden"
            workspace.peeking -> "Covers revealed"
            else -> "${shapes.size} markings"
        }
        if (enabled) customActions = buildList {
            workspace.tool?.let { tool ->
                add(CustomAccessibilityAction("Place ${tool.label()} in the center") {
                    workspace.save(null, workspace.newShape(page, tool, Rect(.3f, .35f, .7f, .65f)), pages)
                    true
                })
            }
            if (selected != null) {
                val rect = selected.rectangle()
                for ((label, delta) in listOf(
                    "left" to Offset(-.025f, 0f), "right" to Offset(.025f, 0f),
                    "up" to Offset(0f, -.025f), "down" to Offset(0f, .025f)
                )) add(CustomAccessibilityAction("Move marking $label") {
                    workspace.save(selected, selected.withRectangle(moveOverlay(rect, delta)), pages)
                    true
                })
                for ((label, factor) in listOf("Enlarge" to 1.1f, "Shrink" to .9f)) {
                    add(CustomAccessibilityAction("$label marking") {
                        val resized = resizeOverlayFromCenter(rect, factor)
                        workspace.save(selected, selected.withRectangle(resized), pages)
                        true
                    })
                }
            }
        }
    }) {
        if (!workspace.visible) return@Canvas
        val bounds = transform.bounds
        val edit = draft
        val drawn = if (edit == null) shapes else shapes.filterNot { it.id == edit.id } + edit
        clipRect(bounds.left, bounds.top, bounds.right, bounds.bottom) {
            val focus = drawn.filter { it.type == OverlayTool.FOCUS.name }
            if (focus.isNotEmpty()) {
                // All saved focus openings remain clear, including overlapping rectangles.
                val pagePath = Path().apply { addRect(bounds) }
                val dark = focus.fold(pagePath) { path, item ->
                    Path.combine(PathOperation.Difference, path, Path().apply { addRect(transform.toScreen(item.rectangle())) })
                }
                drawPath(dark, Color.Black.copy(alpha = .64f))
            }
            for (shape in drawn.sortedBy { it.drawOrder }) {
                val rect = transform.toScreen(shape.rectangle())
                when (shape.type) {
                    OverlayTool.HIGHLIGHT.name -> drawRect(highlights[shape.appearance].copy(alpha = .34f), rect.topLeft, rect.size)
                    OverlayTool.COVER.name -> if (!workspace.peeking) drawRect(Color(0xFF222526), rect.topLeft, rect.size)
                }
            }
            if (workspace.editing) (edit ?: selected)?.let { shape ->
                val rect = transform.toScreen(shape.rectangle())
                drawRect(Color.Black, rect.topLeft, rect.size, style = Stroke(4.dp.toPx()))
                drawRect(Color.White, rect.topLeft, rect.size, style = Stroke(2.dp.toPx()))
                for (handle in listOf(rect.topLeft, rect.bottomRight)) {
                    drawCircle(Color.Black, 7.dp.toPx(), handle)
                    drawCircle(Color.White, 5.dp.toPx(), handle)
                }
            }
        }
    }
}

private fun resizeOverlayFromCenter(rect: Rect, factor: Float): Rect {
    val center = rect.center
    val x = (rect.width * factor / 2).coerceAtMost(minOf(center.x, 1f - center.x))
    val y = (rect.height * factor / 2).coerceAtMost(minOf(center.y, 1f - center.y))
    return Rect(center.x - x, center.y - y, center.x + x, center.y + y)
}
