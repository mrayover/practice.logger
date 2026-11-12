package com.mrayover.practicelogger

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.*
import kotlin.coroutines.coroutineContext

@Composable fun PracticeImageReference(blockId: String, placementId: String, vm: ImageReferenceViewModel, edgeToEdge: Boolean = false, viewerMaxHeight: androidx.compose.ui.unit.Dp = 640.dp, embeddedExpanded: Boolean = false, fitAvailableHeight: Boolean = false, active: Boolean = true) {
    val all by vm.visuals.collectAsStateWithLifecycle()
    val assets by vm.assets.collectAsStateWithLifecycle()
    key(placementId, blockId) {
        val references = all.filter { it.blockId == blockId }
        var selected by rememberSaveable { mutableStateOf<String?>(null) }
        val holder = rememberSaveableStateHolder()
        val current = references.find { it.identity == selected } ?: references.firstOrNull()
        val missing = selected != null && references.none { it.identity == selected }
        if (missing) Text("The selected visual was removed. Showing the first available reference.")
        if (current != null) {
            ReferenceSourceSelector("Visual reference", references.map { reference ->
                reference.identity to (assets.find { it.id == reference.mediaId }?.title
                    ?: reference.legacy?.let { "Legacy ${if (it.mimeType == "application/pdf") "PDF" else "image"}" }
                    ?: "Unavailable visual")
            }, current.identity, active, Modifier.padding(horizontal = if (edgeToEdge) 16.dp else 0.dp)) { selected = it }
            holder.SaveableStateProvider(current.identity) {
                CollapsibleImageReference(current, vm, edgeToEdge, viewerMaxHeight, embeddedExpanded, fitAvailableHeight, active)
            }
        }
    }
}

@Composable internal fun CollapsibleImageReference(image: BlockVisualReference, vm: ImageReferenceViewModel, edgeToEdge: Boolean = false, viewerMaxHeight: androidx.compose.ui.unit.Dp = 640.dp, embeddedExpanded: Boolean = false, fitAvailableHeight: Boolean = false, active: Boolean = true) {
    val inset = if (edgeToEdge) 16.dp else 0.dp
    // These states never enter/leave the sheet or touch any practice/audio model.
    var expanded by rememberSaveable(image.identity) { mutableStateOf(false) }
    var page by rememberSaveable(image.identity) { mutableIntStateOf(0) }
    var retry by remember(image.identity) { mutableIntStateOf(0) }
    val pages = rememberSaveableStateHolder()
    val scope = rememberCoroutineScope()
    val workspace = remember(image.blockId, image.identity) { OverlayWorkspace(image, vm, scope) }
    val shapes by remember(image.blockId, image.identity) { vm.overlays.observe(image) }
        .collectAsStateWithLifecycle(emptyList())
    if (!embeddedExpanded) OutlinedButton(onClick = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth().padding(horizontal = inset).heightIn(min = 48.dp).testTag("reference-toggle")) { Text(if (expanded) "Hide reference" else "Show reference") }
    if (embeddedExpanded || expanded) key(image.identity, page, retry) {
        var result by remember { mutableStateOf<ReferencePage?>(null) }
        var failure by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            val owner = coroutineContext
            // PdfRenderer is synchronous. Finish/close resources on IO even after disposal;
            // recycle a result that no longer has a UI owner instead of publishing stale pages.
            withContext(NonCancellable) {
                try {
                    val loaded = vm.loadPage(image, page)
                    if (owner.isActive) result = loaded else loaded.bitmap.recycle()
                } catch (_: Exception) { if (owner.isActive) failure = true }
            }
        }
        when {
            failure -> {
                Text("Reference is missing or unreadable. Retry or reopen the app, then replace or remove it in the Block editor if needed.", modifier = Modifier.padding(horizontal = inset))
                TextButton(onClick = { retry++ }) { Text("Retry reference") }
            }
            result == null -> Text("Loading reference…", modifier = Modifier.padding(horizontal = inset))
            else -> {
                val loaded = result!!
                val density = androidx.compose.ui.platform.LocalDensity.current
                var controlsHeight by remember { mutableStateOf(0.dp) }
                val pageShapes = remember(shapes, page) { shapes.filter { it.page == page } }
                Column(Modifier.fillMaxWidth().padding(horizontal = inset)
                    .onSizeChanged { controlsHeight = with(density) { it.height.toDp() } }) {
                    if (image.mimeType == "application/pdf") {
                        Text("Page ${page + 1} of ${loaded.pageCount}", modifier = Modifier.testTag("reference-page"))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { workspace.select(null); workspace.peek(false); page-- },
                                enabled = page > 0, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("Previous page") }
                            OutlinedButton(onClick = { workspace.select(null); workspace.peek(false); page++ },
                                enabled = page + 1 < loaded.pageCount, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("Next page") }
                        }
                    }
                    OverlayToolbar(workspace, shapes, pageShapes, loaded.pageCount, active)
                }
                // Keep the existing image/PDF painter and the same clipped viewport.
                pages.SaveableStateProvider(page) { ZoomableReference(loaded.bitmap, inset, image.mimeType == "application/pdf",
                    if (fitAvailableHeight) (viewerMaxHeight - controlsHeight).coerceAtLeast(1.dp) else viewerMaxHeight,
                    fitAvailableHeight, minimumHeight = if (fitAvailableHeight) 180.dp else 1.dp,
                    overlayEditing = active && workspace.editing) { transform ->
                    OverlayLayer(workspace, transform, pageShapes, page, loaded.pageCount, active)
                }
                }
            }
        }
    }
}

@Composable internal fun ZoomableReference(bitmap: android.graphics.Bitmap, inset: androidx.compose.ui.unit.Dp, pdf: Boolean = false, maxHeight: androidx.compose.ui.unit.Dp = 640.dp, fitAvailableHeight: Boolean = false, resetKey: Int = 0, minimumHeight: androidx.compose.ui.unit.Dp = 1.dp, overlayEditing: Boolean = false, overlay: (@Composable BoxScope.(ReferencePageTransform) -> Unit)? = null) {
    var scale by rememberSaveable(bitmap) { mutableFloatStateOf(1f) }
    var offset by rememberSaveable(bitmap, stateSaver = androidx.compose.runtime.saveable.Saver<Offset, List<Float>>(
        save = { listOf(it.x, it.y) }, restore = { Offset(it[0], it[1]) })) { mutableStateOf(Offset.Zero) }
    var lastReset by rememberSaveable { mutableIntStateOf(resetKey) }
    LaunchedEffect(resetKey) { if (lastReset != resetKey) { scale = 1f; offset = Offset.Zero; lastReset = resetKey } }
    var size by remember { mutableStateOf(IntSize.Zero) }
    fun bounded(value: Offset, zoom: Float): Offset {
        val fit = ReferencePageTransform(size, IntSize(bitmap.width, bitmap.height), zoom, Offset.Zero).bounds
        val x = ((fit.width - size.width) / 2).coerceAtLeast(0f)
        val y = ((fit.height - size.height) / 2).coerceAtLeast(0f)
        return Offset(value.x.coerceIn(-x, x), value.y.coerceIn(-y, y))
    }
    val transform = rememberTransformableState { zoom, pan, _ ->
        val next = (scale * zoom).coerceIn(1f, 6f)
        offset = if (next == 1f) Offset.Zero else bounded(offset * (next / scale) + pan, next)
        scale = next
    }
    val density = androidx.compose.ui.platform.LocalDensity.current
    var guideHeight by remember { mutableStateOf(0.dp) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (overlay == null) Text("Pinch to zoom; drag while enlarged.", style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = inset).onSizeChanged { guideHeight = with(density) { it.height.toDp() } })
        BoxWithConstraints(Modifier.fillMaxWidth()) {
        // PDF controls can exceed a large-text workspace. Keep a usable image inside
        // the existing scroll panel instead of collapsing the bitmap to one pixel.
        val cap = if (fitAvailableHeight) (maxHeight - guideHeight - 4.dp).coerceAtLeast(minimumHeight) else maxHeight
        val viewerHeight = (maxWidth * (bitmap.height.toFloat() / bitmap.width)).coerceIn(minOf(180.dp, cap), cap)
        Box(Modifier.fillMaxWidth().height(viewerHeight).clipToBounds().background(MaterialTheme.colorScheme.surfaceVariant)
            .onSizeChanged { size = it }.testTag("reference-viewer")
            .semantics {
                contentDescription = "Reference ${if (pdf) "PDF" else "image"} viewer, zoom ${"%.1f".format(scale)}"
                customActions = listOf(androidx.compose.ui.semantics.CustomAccessibilityAction("Reset zoom") { scale = 1f; offset = Offset.Zero; true })
            }
            .transformable(state = transform, enabled = !overlayEditing, canPan = { scale > 1f })) {
            Image(bitmap.asImageBitmap(), contentDescription = if (pdf) "Block reference PDF page" else "Block reference image", contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    scaleX = scale; scaleY = scale; translationX = offset.x; translationY = offset.y
                })
            if (overlay != null && size.width > 0 && size.height > 0) {
                val pageTransform = remember(size, bitmap, scale, offset) {
                    ReferencePageTransform(size, IntSize(bitmap.width, bitmap.height), scale, offset)
                }
                overlay(pageTransform)
            }
            if (scale > 1f && !fitAvailableHeight && maxHeight < 640.dp) {
                // Keep reset inside a compact viewport: enlarged-image pans must not
                // trap the only reset control below its scrolling parent's boundary.
                Surface(modifier = Modifier.align(androidx.compose.ui.Alignment.TopEnd).padding(4.dp), shape = MaterialTheme.shapes.small) {
                    TextButton(modifier = Modifier.heightIn(min = 48.dp), onClick = { scale = 1f; offset = Offset.Zero }) {
                        Text(if (pdf) "Reset PDF zoom" else "Reset image zoom")
                    }
                }
            }
        }
        }
        if (scale > 1f && !fitAvailableHeight && maxHeight >= 640.dp) TextButton(modifier = Modifier.padding(horizontal = inset), onClick = { scale = 1f; offset = Offset.Zero }) { Text(if (pdf) "Reset PDF zoom" else "Reset image zoom") }
    }
}
