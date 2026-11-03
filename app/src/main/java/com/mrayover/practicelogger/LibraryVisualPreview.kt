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
    BackHandler(onBack = close)
    var page by rememberSaveable(asset.id) { mutableIntStateOf(0) }
    var retry by remember { mutableIntStateOf(0) }
    var reset by remember { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(8.dp)) {
        Row(Modifier.fillMaxWidth()) {
            TextButton(onClick = close, modifier = Modifier.heightIn(min = 48.dp)) { Text("Back to Library") }
            Text(asset.title, modifier = Modifier.weight(1f).padding(8.dp), maxLines = 2, style = MaterialTheme.typography.titleMedium)
        }
        Column {
            if (asset.category == "PDF") {
                Text("Page ${page + 1} of ${asset.pageCount}")
                Row(Modifier.fillMaxWidth()) {
                    TextButton(onClick = { page-- }, enabled = page > 0, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("Previous page") }
                    TextButton(onClick = { page++ }, enabled = page + 1 < (asset.pageCount ?: 1), modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("Next page") }
                }
            }
        }
        TextButton(onClick = { reset++ }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Reset zoom") }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val height = maxHeight
            key(asset.id, page, retry) {
                var result by remember { mutableStateOf<ReferencePage?>(null) }
                var error by remember { mutableStateOf<String?>(null) }
                LaunchedEffect(Unit) {
                    val owner = coroutineContext
                    withContext(NonCancellable) {
                        try {
                            val loaded = repository.visualPage(asset.id, page)
                            if (owner.isActive) result = loaded else loaded.bitmap.recycle()
                        } catch (_: Exception) { if (owner.isActive) error = "Original is missing or unreadable." }
                    }
                }
                when {
                    error != null -> Column { Text(error!!); Button(onClick = { retry++ }) { Text("Retry preview") } }
                    result == null -> Text("Loading preview…")
                    else -> ZoomableReference(result!!.bitmap, 0.dp, asset.category == "PDF", height, true, reset)
                }
            }
        }
    }
}
