package com.mrayover.practicelogger

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
    OutlinedButton(onClick = { picker.launch(arrayOf("image/jpeg", "image/png", "image/webp", "application/pdf")) }, enabled = !busy && !vm.busy,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(if (image == null) "Add image or PDF" else "Replace reference $kind") }
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

@Composable fun PracticeImageReference(blockId: String, placementId: String, vm: ImageReferenceViewModel, edgeToEdge: Boolean = false) {
    val all by vm.images.collectAsStateWithLifecycle()
    key(placementId) { all.find { it.blockId == blockId }?.let { CollapsibleImageReference(it, vm.repository, edgeToEdge) } }
}

@Composable private fun CollapsibleImageReference(image: BlockImage, repository: ImageReferenceRepository, edgeToEdge: Boolean = false) {
    val inset = if (edgeToEdge) 16.dp else 0.dp
    // These states never enter/leave the sheet or touch any practice/audio model.
    var expanded by rememberSaveable(image.fileName) { mutableStateOf(false) }
    var page by rememberSaveable(image.fileName) { mutableIntStateOf(0) }
    var retry by remember(image.fileName) { mutableIntStateOf(0) }
    OutlinedButton(onClick = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth().padding(horizontal = inset).heightIn(min = 48.dp).testTag("reference-toggle")) { Text(if (expanded) "Hide reference" else "Show reference") }
    if (expanded) key(image.fileName, page, retry) {
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
                Text("Reference is missing or unreadable. Replace or remove it in the Block editor.", modifier = Modifier.padding(horizontal = inset))
                TextButton(onClick = { retry++ }) { Text("Retry reference") }
            }
            result == null -> Text("Loading reference…", modifier = Modifier.padding(horizontal = inset))
            else -> {
                val loaded = result!!
                if (image.mimeType == "application/pdf") {
                    // Wrapping controls remain reachable at enlarged text sizes.
                    Column(Modifier.fillMaxWidth().padding(horizontal = inset)) {
                        Text("Page ${page + 1} of ${loaded.pageCount}", modifier = Modifier.testTag("reference-page"))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { page-- }, enabled = page > 0, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("Previous page") }
                            OutlinedButton(onClick = { page++ }, enabled = page + 1 < loaded.pageCount, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("Next page") }
                        }
                    }
                }
                // One displayed bitmap per viewer. Dropped pages are left to Android GC rather
                // than recycling a bitmap that the Compose render thread may still be drawing.
                ZoomableReference(loaded.bitmap, inset, image.mimeType == "application/pdf")
            }
        }
    }
}

@Composable private fun ZoomableReference(bitmap: android.graphics.Bitmap, inset: androidx.compose.ui.unit.Dp, pdf: Boolean = false) {
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
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Pinch to zoom; drag while enlarged.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = inset))
        BoxWithConstraints(Modifier.fillMaxWidth()) {
        val viewerHeight = (maxWidth * (bitmap.height.toFloat() / bitmap.width)).coerceIn(180.dp, 640.dp)
        Box(Modifier.fillMaxWidth().height(viewerHeight).clipToBounds().background(MaterialTheme.colorScheme.surfaceVariant)
            .onSizeChanged { size = it }.testTag("reference-viewer")
            .semantics { contentDescription = "Reference ${if (pdf) "PDF" else "image"} viewer, zoom ${"%.1f".format(scale)}" }
            .transformable(state = transform, canPan = { scale > 1f })) {
            Image(bitmap.asImageBitmap(), contentDescription = if (pdf) "Block reference PDF page" else "Block reference image", contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    scaleX = scale; scaleY = scale; translationX = offset.x; translationY = offset.y
                })
        }
        }
        if (scale > 1f) TextButton(modifier = Modifier.padding(horizontal = inset), onClick = { scale = 1f; offset = Offset.Zero }) { Text(if (pdf) "Reset PDF zoom" else "Reset image zoom") }
    }
}
