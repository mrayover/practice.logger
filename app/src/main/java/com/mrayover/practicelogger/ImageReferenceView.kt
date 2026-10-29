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
import kotlinx.coroutines.CancellationException

@Composable fun BlockImageEditor(blockId: String?, vm: ImageReferenceViewModel, busy: Boolean) {
    Text("Reference image", style = MaterialTheme.typography.titleMedium)
    Text("One JPEG, PNG or WebP, up to 20 MiB / 40 megapixels. Images are stored only in this app. Keep originals separately: JSON backups exclude images.", style = MaterialTheme.typography.bodySmall)
    if (blockId == null) { Text("Save this Block first, then reopen it to add an image."); return }
    val all by vm.images.collectAsStateWithLifecycle()
    val image = all.find { it.blockId == blockId }
    var confirmRemoval by rememberSaveable(blockId) { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.import(blockId, uri)
    }
    OutlinedButton(onClick = { picker.launch(arrayOf("image/jpeg", "image/png", "image/webp")) }, enabled = !busy && !vm.busy,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(if (image == null) "Add reference image" else "Replace reference image") }
    if (image != null) {
        key(blockId) { CollapsibleImageReference(image, vm.repository) }
        OutlinedButton(onClick = { confirmRemoval = true }, enabled = !busy && !vm.busy,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Remove reference image") }
    }
    if (vm.busy) Text("Saving reference…")
    vm.message?.let { Text(it, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
    if (confirmRemoval) AlertDialog(onDismissRequest = { confirmRemoval = false }, title = { Text("Remove reference image?") },
        text = { Text("This deletes the app's copy. Keep the original separately; images are not in JSON backups.") },
        confirmButton = { TextButton(onClick = { confirmRemoval = false; vm.remove(blockId) }, enabled = !busy && !vm.busy) { Text("Remove image") } },
        dismissButton = { TextButton(onClick = { confirmRemoval = false }) { Text("Cancel") } })
}

@Composable fun PracticeImageReference(blockId: String, placementId: String, vm: ImageReferenceViewModel, edgeToEdge: Boolean = false) {
    val all by vm.images.collectAsStateWithLifecycle()
    key(placementId) { all.find { it.blockId == blockId }?.let { CollapsibleImageReference(it, vm.repository, edgeToEdge) } }
}

@Composable private fun CollapsibleImageReference(image: BlockImage, repository: ImageReferenceRepository, edgeToEdge: Boolean = false) {
    val inset = if (edgeToEdge) 16.dp else 0.dp
    // Local UI state never calls practice/recording lifecycle or changes immutable session snapshots.
    var expanded by remember(image.fileName) { mutableStateOf(false) }
    var loaded by remember(image.fileName) { mutableStateOf(false) }
    var retry by remember(image.fileName) { mutableIntStateOf(0) }
    var bitmap by remember(image.fileName) { mutableStateOf<android.graphics.Bitmap?>(null) }
    var failure by remember(image.fileName) { mutableStateOf(false) }
    LaunchedEffect(image.fileName, loaded, retry) {
        if (loaded) {
            failure = false
            try { bitmap = repository.decode(image) }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { failure = true }
        }
    }
    OutlinedButton(onClick = { expanded = !expanded; if (expanded) loaded = true },
        modifier = Modifier.fillMaxWidth().padding(horizontal = inset).heightIn(min = 48.dp).testTag("reference-toggle")) { Text(if (expanded) "Hide reference" else "Show reference") }
    if (expanded) {
        when {
            failure -> {
                Text("Reference image is missing or unreadable. Replace or remove it in the Block editor.")
                TextButton(onClick = { retry++ }) { Text("Retry image") }
            }
            bitmap == null -> Text("Loading reference…")
            else -> ZoomableReference(bitmap!!, inset)
        }
    }
}

@Composable private fun ZoomableReference(bitmap: android.graphics.Bitmap, inset: androidx.compose.ui.unit.Dp) {
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
            .semantics { contentDescription = "Reference image viewer, zoom ${"%.1f".format(scale)}" }
            .transformable(state = transform, canPan = { scale > 1f })) {
            Image(bitmap.asImageBitmap(), contentDescription = "Block reference image", contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    scaleX = scale; scaleY = scale; translationX = offset.x; translationY = offset.y
                })
        }
        }
        if (scale > 1f) TextButton(modifier = Modifier.padding(horizontal = inset), onClick = { scale = 1f; offset = Offset.Zero }) { Text("Reset image zoom") }
    }
}
