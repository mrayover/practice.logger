package com.mrayover.practicelogger

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*

@Composable fun LibraryVisualPreview(asset: MediaAsset, repository: MediaLibraryRepository, close: () -> Unit) {
    VisualDocumentPreview(asset.id, asset.title, asset.pageCount, "Back to Library",
        load = { repository.visualPage(asset.id, it) }, close = close)
}

@Composable
internal fun VisualDocumentPreview(
    id: String, title: String, pageCount: Int?, closeLabel: String, load: suspend (Int) -> ReferencePage, close: () -> Unit
) {
    BackHandler(onBack = close)
    var page by rememberSaveable(id) { mutableIntStateOf(0) }
    var retry by remember { mutableIntStateOf(0) }
    var reset by remember { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(8.dp)) {
        Row(Modifier.fillMaxWidth()) {
            TextButton(onClick = close, modifier = Modifier.heightIn(min = 48.dp)) { Text(closeLabel) }

        }
        PageHeading(title, style = MaterialTheme.typography.titleMedium, maxLines = 2)
        Column {
            if (pageCount != null) {
                Text("Page ${page + 1} of $pageCount")
                Row(Modifier.fillMaxWidth()) {
                    TextButton(onClick = { page-- }, enabled = page > 0, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("Previous page") }
                    TextButton(onClick = { page++ }, enabled = page + 1 < pageCount, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("Next page") }
                }
            }
        }
        TextButton(onClick = { reset++ }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Reset zoom") }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val height = maxHeight
            key(id, page, retry) {
                var result by remember { mutableStateOf<ReferencePage?>(null) }
                var error by remember { mutableStateOf<String?>(null) }
                LaunchedEffect(Unit) {
                    val owner = coroutineContext
                    withContext(NonCancellable) {
                        try {
                            val loaded = load(page)
                            if (owner.isActive) result = loaded else loaded.bitmap.recycle()
                        } catch (_: Exception) { if (owner.isActive) error = "Original is missing or unreadable." }
                    }
                }
                when {
                    error != null -> Column { Text(error!!); Button(onClick = { retry++ }) { Text("Retry preview") } }
                    result == null -> Text("Loading preview…")
                    else -> ZoomableReference(result!!.bitmap, 0.dp, pageCount != null, height, true, reset)
                }
            }
        }
    }
}
