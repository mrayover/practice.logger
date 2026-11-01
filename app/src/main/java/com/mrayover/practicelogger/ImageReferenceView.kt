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

@Composable fun BlockImageEditor(blockId: String?, vm: ImageReferenceViewModel, busy: Boolean) {
    Text("Reference image or PDF", style = MaterialTheme.typography.titleMedium)
    Text("One image (JPEG, PNG, WebP; up to 40 megapixels) or PDF (1–100 pages), up to 20 MiB. Stored only in this app. Keep originals separately: JSON backups exclude images and PDFs.", style = MaterialTheme.typography.bodySmall)
    if (blockId == null) { Text("Save this Block first, then reopen it to add a reference."); return }
    val all by vm.images.collectAsStateWithLifecycle()
    val image = all.find { it.blockId == blockId }
    val kind = if (image?.mimeType == "application/pdf") "PDF" else "image"
    var confirmRemoval by rememberSaveable(blockId) { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.import(blockId, uri)
    }
    var photoOptions by rememberSaveable(blockId) { mutableStateOf(false) }
    // Save the private filename across camera activity recreation, never a bitmap in saved state.
    var pendingPhoto by rememberSaveable(blockId) { mutableStateOf<String?>(null) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        pendingPhoto?.let { name ->
            if (saved) vm.importPhoto(blockId, name) else vm.photoCapture.discard(name)
        }
        pendingPhoto = null
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.import(blockId, uri)
    }
    val enabled = !busy && !vm.busy && pendingPhoto == null
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val stacked = maxWidth / fontScale < 250.dp
        val files: @Composable (Modifier) -> Unit = { modifier ->
            OutlinedButton(onClick = { picker.launch(arrayOf("image/jpeg", "image/png", "image/webp", "application/pdf")) }, enabled = enabled,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp), modifier = modifier.heightIn(min = 48.dp)) {
                Text(if (image == null) "Add image or PDF" else "Replace reference $kind", textAlign = TextAlign.Center)
            }
        }
        val photos: @Composable (Modifier) -> Unit = { modifier ->
            OutlinedButton(onClick = { photoOptions = true }, enabled = enabled,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp), modifier = modifier.heightIn(min = 48.dp)) {
                Text("Take or Add a photo", textAlign = TextAlign.Center)
            }
        }
        if (stacked) Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            files(Modifier.fillMaxWidth()); Text("Or", style = MaterialTheme.typography.bodySmall); photos(Modifier.fillMaxWidth())
        } else Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            files(Modifier.weight(1f).fillMaxHeight()); Text("Or", style = MaterialTheme.typography.bodySmall); photos(Modifier.weight(1f).fillMaxHeight())
        }
    }
    if (photoOptions) AlertDialog(onDismissRequest = { photoOptions = false }, title = { Text("Take or Add a photo") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {
                photoOptions = false
                try {
                    val name = vm.photoCapture.create()
                    pendingPhoto = name
                    camera.launch(vm.photoCapture.uri(name))
                } catch (_: Exception) {
                    pendingPhoto?.let { vm.photoCapture.discard(it) }; pendingPhoto = null; vm.photoUnavailable()
                }
            }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Take photo") }
            OutlinedButton(onClick = {
                photoOptions = false
                try { gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                catch (_: Exception) { vm.photoUnavailable() }
            }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Choose from gallery") }
        } }, confirmButton = { TextButton(onClick = { photoOptions = false }) { Text("Cancel") } })
    if (image != null) {
        key(blockId) { CollapsibleImageReference(image, vm.repository) }
        OutlinedButton(onClick = { confirmRemoval = true }, enabled = !busy && !vm.busy,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Remove reference $kind") }
    }
    if (vm.busy) Text("Saving reference…")
    vm.message?.let { Text(it, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
    if (confirmRemoval) AlertDialog(onDismissRequest = { confirmRemoval = false }, title = { Text("Remove reference $kind?") },
        text = { Text("This deletes the app's copy. Keep the original separately; images and PDFs are not in JSON backups.") },
        confirmButton = { TextButton(onClick = { confirmRemoval = false; vm.remove(blockId) }, enabled = !busy && !vm.busy) { Text("Remove $kind") } },
        dismissButton = { TextButton(onClick = { confirmRemoval = false }) { Text("Cancel") } })
}

@Composable fun PracticeImageReference(blockId: String, placementId: String, vm: ImageReferenceViewModel, edgeToEdge: Boolean = false, viewerMaxHeight: androidx.compose.ui.unit.Dp = 640.dp, embeddedExpanded: Boolean = false, fitAvailableHeight: Boolean = false) {
    val all by vm.images.collectAsStateWithLifecycle()
    key(placementId) { all.find { it.blockId == blockId }?.let { CollapsibleImageReference(it, vm.repository, edgeToEdge, viewerMaxHeight, embeddedExpanded, fitAvailableHeight) } }
}

@Composable private fun CollapsibleImageReference(image: BlockImage, repository: ImageReferenceRepository, edgeToEdge: Boolean = false, viewerMaxHeight: androidx.compose.ui.unit.Dp = 640.dp, embeddedExpanded: Boolean = false, fitAvailableHeight: Boolean = false) {
    val inset = if (edgeToEdge) 16.dp else 0.dp
    // These states never enter/leave the sheet or touch any practice/audio model.
    var expanded by rememberSaveable(image.fileName) { mutableStateOf(false) }
    var page by rememberSaveable(image.fileName) { mutableIntStateOf(0) }
    var retry by remember(image.fileName) { mutableIntStateOf(0) }
    if (!embeddedExpanded) OutlinedButton(onClick = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth().padding(horizontal = inset).heightIn(min = 48.dp).testTag("reference-toggle")) { Text(if (expanded) "Hide reference" else "Show reference") }
    if (embeddedExpanded || expanded) key(image.fileName, page, retry) {
        var result by remember { mutableStateOf<ReferencePage?>(null) }
        var failure by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            val owner = coroutineContext
            // PdfRenderer is synchronous. Finish/close resources on IO even after disposal;
            // recycle a result that no longer has a UI owner instead of publishing stale pages.
            withContext(NonCancellable) {
                try {
                    val loaded = repository.loadPage(image, page)
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
                if (image.mimeType == "application/pdf") {
                    // Wrapping controls remain reachable at enlarged text sizes.
                    Column(Modifier.fillMaxWidth().padding(horizontal = inset).onSizeChanged { controlsHeight = with(density) { it.height.toDp() } }) {
                        Text("Page ${page + 1} of ${loaded.pageCount}", modifier = Modifier.testTag("reference-page"))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { page-- }, enabled = page > 0, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("Previous page") }
                            OutlinedButton(onClick = { page++ }, enabled = page + 1 < loaded.pageCount, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("Next page") }
                        }
                    }
                }
                // One displayed bitmap per viewer. Dropped pages are left to Android GC rather
                // than recycling a bitmap that the Compose render thread may still be drawing.
                ZoomableReference(loaded.bitmap, inset, image.mimeType == "application/pdf",
                    if (fitAvailableHeight) (viewerMaxHeight - controlsHeight).coerceAtLeast(1.dp) else viewerMaxHeight, fitAvailableHeight)
            }
        }
    }
}

@Composable private fun ZoomableReference(bitmap: android.graphics.Bitmap, inset: androidx.compose.ui.unit.Dp, pdf: Boolean = false, maxHeight: androidx.compose.ui.unit.Dp = 640.dp, fitAvailableHeight: Boolean = false) {
    var scale by remember(bitmap) { mutableFloatStateOf(1f) }
    var offset by remember(bitmap) { mutableStateOf(Offset.Zero) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    fun bounded(value: Offset, zoom: Float): Offset {
        val fit = minOf(size.width.toFloat() / bitmap.width, size.height.toFloat() / bitmap.height)
        val x = ((bitmap.width * fit * zoom - size.width) / 2).coerceAtLeast(0f)
        val y = ((bitmap.height * fit * zoom - size.height) / 2).coerceAtLeast(0f)
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
        Text("Pinch to zoom; drag while enlarged.", style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = inset).onSizeChanged { guideHeight = with(density) { it.height.toDp() } })
        BoxWithConstraints(Modifier.fillMaxWidth()) {
        val cap = if (fitAvailableHeight) (maxHeight - guideHeight - 4.dp).coerceAtLeast(1.dp) else maxHeight
        val viewerHeight = (maxWidth * (bitmap.height.toFloat() / bitmap.width)).coerceIn(minOf(180.dp, cap), cap)
        Box(Modifier.fillMaxWidth().height(viewerHeight).clipToBounds().background(MaterialTheme.colorScheme.surfaceVariant)
            .onSizeChanged { size = it }.testTag("reference-viewer")
            .semantics {
                contentDescription = "Reference ${if (pdf) "PDF" else "image"} viewer, zoom ${"%.1f".format(scale)}"
                customActions = listOf(androidx.compose.ui.semantics.CustomAccessibilityAction("Reset zoom") { scale = 1f; offset = Offset.Zero; true })
            }
            .transformable(state = transform, canPan = { scale > 1f })) {
            Image(bitmap.asImageBitmap(), contentDescription = if (pdf) "Block reference PDF page" else "Block reference image", contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    scaleX = scale; scaleY = scale; translationX = offset.x; translationY = offset.y
                })
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
