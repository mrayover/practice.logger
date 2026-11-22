package com.mrayover.practicelogger

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import java.io.FilterInputStream
import java.io.InputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

internal const val scanDocumentLabel = "Scan document"
internal const val scannedPdfMime = "application/pdf"
private const val scannerUnavailable = "Document scanner unavailable. Check Google Play services and try again, or choose another import option."
private const val missingScan = "The scanner did not return a readable PDF. Try scanning again."

internal class ScannedDocument(private val uri: Uri, time: LocalDateTime = LocalDateTime.now()) {
    init { require(uri.scheme == ContentResolver.SCHEME_CONTENT && !uri.authority.isNullOrBlank()) { missingScan } }

    val filename = "Scan-${time.format(DateTimeFormatter.ofPattern("uuuu-MM-dd-HHmmss", Locale.ROOT))}.pdf"

    fun open(resolver: ContentResolver): InputStream = limitScannedPdf(
        checkNotNull(resolver.openInputStream(uri)) { missingScan }
    )
}

// Apply the scanner-specific size message to actual bytes even if the provider omits metadata.
internal fun limitScannedPdf(input: InputStream, limit: Long = ImageReferenceRepository.MAX_BYTES.toLong()): InputStream =
    object : FilterInputStream(input) {
        private var total = 0L

        private fun count(bytes: Int) {
            total += bytes
            require(total <= limit) { "Scanned PDFs can be up to 20 MB. Scan fewer pages per document and try again." }
        }

        override fun read(): Int = super.read().also { if (it >= 0) count(1) }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
            super.read(buffer, offset, length).also { if (it > 0) count(it) }
    }

@Composable
internal fun rememberDocumentScanner(
    owner: String?,
    onDocument: (ScannedDocument) -> Unit,
    onError: (String) -> Unit
): () -> Unit {
    val context = LocalContext.current
    val currentOwner by rememberUpdatedState(owner)
    val deliver by rememberUpdatedState(onDocument)
    val report by rememberUpdatedState(onError)
    var pendingOwner by rememberSaveable { mutableStateOf<String?>(null) }
    var preparing by remember { mutableStateOf(false) }
    val attached = remember { mutableStateOf(true) }
    DisposableEffect(Unit) { onDispose { attached.value = false } }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        val requestedOwner = pendingOwner
        pendingOwner = null
        if (requestedOwner == null || requestedOwner != currentOwner) return@rememberLauncherForActivityResult
        if (result.data?.action == ActivityResultContracts.StartIntentSenderForResult.ACTION_INTENT_SENDER_REQUEST) {
            report(scannerUnavailable)
        } else if (result.resultCode == Activity.RESULT_OK) {
            val document = runCatching {
                val pdf = checkNotNull(GmsDocumentScanningResult.fromActivityResultIntent(result.data)?.pdf)
                check(pdf.pageCount > 0)
                ScannedDocument(pdf.uri)
            }.getOrNull()
            if (document == null) report(missingScan) else deliver(document)
        }
    }

    return launch@{
        val requestedOwner = owner ?: return@launch
        if (preparing || pendingOwner != null) return@launch
        val activity = context.scannerActivity()
        if (activity == null) {
            report(scannerUnavailable)
            return@launch
        }
        preparing = true
        try {
            val options = GmsDocumentScannerOptions.Builder()
                .setGalleryImportAllowed(true)
                .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_PDF)
                .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_BASE)
                .build()
            GmsDocumentScanning.getClient(options).getStartScanIntent(activity)
                .addOnSuccessListener { sender ->
                    preparing = false
                    if (attached.value && requestedOwner == currentOwner && !activity.isFinishing && !activity.isDestroyed) {
                        try {
                            pendingOwner = requestedOwner
                            launcher.launch(IntentSenderRequest.Builder(sender).build())
                        } catch (_: Exception) {
                            pendingOwner = null
                            report(scannerUnavailable)
                        }
                    }
                }
                .addOnFailureListener {
                    preparing = false
                    if (attached.value && requestedOwner == currentOwner) report(scannerUnavailable)
                }
        } catch (_: Exception) {
            preparing = false
            report(scannerUnavailable)
        }
    }
}

private tailrec fun Context.scannerActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.scannerActivity()
    else -> null
}
