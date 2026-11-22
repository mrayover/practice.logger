package com.mrayover.practicelogger

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import kotlinx.coroutines.CancellationException
import java.io.File
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

internal const val scanDocumentLabel = "Scan document"
internal const val scannedPdfMime = "application/pdf"
private const val scannerUnavailable = "Document scanner unavailable. Check Google Play services and try again, or choose another import option."
private const val scannerTag = "PracticeDocScan"
private const val unreadableScan = "The scanner returned a PDF, but its file could not be read. Try scanning again."
private const val invalidScan = "The scan is not a readable, supported PDF. Try scanning again."

private class UnreadableScan : IllegalStateException(unreadableScan)
private class OversizedScan : IllegalArgumentException("Scanned PDFs can be up to 20 MB. Scan fewer pages per document and try again.")

private fun Uri.isScanFile(): Boolean = when (scheme) {
    ContentResolver.SCHEME_CONTENT -> !authority.isNullOrBlank()
    ContentResolver.SCHEME_FILE -> authority.isNullOrEmpty() && path?.startsWith("/") == true
    else -> false
}

internal class ScannedDocument(private val uri: Uri, time: LocalDateTime = LocalDateTime.now()) {
    init { require(uri.isScanFile()) { unreadableScan } }

    val filename = "Scan-${time.format(DateTimeFormatter.ofPattern("uuuu-MM-dd-HHmmss", Locale.ROOT))}.pdf"

    fun open(context: Context): InputStream {
        val input = readScanBytes {
            // ML Kit copies its PDF into the calling app's cache and returns Uri.fromFile.
            if (uri.scheme == ContentResolver.SCHEME_FILE) {
                val file = File(checkNotNull(uri.path)).canonicalFile
                if (!file.toPath().startsWith(context.cacheDir.canonicalFile.toPath()) || !file.isFile) {
                    throw UnreadableScan()
                }
            }
            context.contentResolver.openInputStream(uri) ?: throw UnreadableScan()
        }
        val buffered = limitScannedPdf(input).buffered()
        try {
            buffered.mark(5)
            if (!"%PDF-".all { buffered.read() == it.code }) throw InvalidPdfOriginal(invalidScan)
            buffered.reset()
            return buffered
        } catch (failure: Exception) {
            runCatching { buffered.close() }
            throw failure
        }
    }
}

private inline fun <T> readScanBytes(read: () -> T): T = try {
    read()
} catch (_: IOException) {
    throw UnreadableScan()
} catch (_: SecurityException) {
    throw UnreadableScan()
}

// Apply the scanner-specific size message to actual bytes even if the provider omits metadata.
internal fun limitScannedPdf(input: InputStream, limit: Long = ImageReferenceRepository.MAX_BYTES.toLong()): InputStream =
    object : FilterInputStream(input) {
        private var total = 0L

        private fun count(bytes: Int) {
            total += bytes
            if (total > limit) throw OversizedScan()
        }

        override fun read(): Int = readScanBytes { super.read() }.also { if (it >= 0) count(1) }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
            readScanBytes { super.read(buffer, offset, length) }.also { if (it > 0) count(it) }
    }

internal suspend fun <T> importScannedPdf(import: suspend () -> T): T = try {
    import()
} catch (canceled: CancellationException) {
    Log.i(scannerTag, "scan_import_canceled")
    throw canceled
} catch (failure: Exception) {
    Log.w(scannerTag, when (failure) {
        is UnreadableScan -> "scan_uri_unreadable"
        is InvalidPdfOriginal -> "scan_pdf_invalid"
        is OversizedScan -> "scan_pdf_oversized"
        else -> "scan_import_failed"
    })
    throw failure
}

internal fun handleDocumentScanResult(
    result: ActivityResult,
    onDocument: (ScannedDocument) -> Unit,
    onError: (String) -> Unit
) {
    fun fail(event: String, message: String) {
        Log.w(scannerTag, event)
        onError(message)
    }

    val data = result.data
    if (data?.action == ActivityResultContracts.StartIntentSenderForResult.ACTION_INTENT_SENDER_REQUEST) {
        return fail("scan_launch_failed", scannerUnavailable)
    }
    if (result.resultCode == Activity.RESULT_CANCELED) {
        Log.i(scannerTag, "scan_canceled")
        return
    }
    if (result.resultCode != Activity.RESULT_OK) {
        return fail("scan_result_unexpected", "The scanner could not finish. Try scanning again.")
    }
    if (data == null) return fail("scan_intent_missing", "The scanner finished without returning a result. Try scanning again.")
    val pdf = try {
        GmsDocumentScanningResult.fromActivityResultIntent(data)?.pdf
    } catch (_: RuntimeException) {
        return fail("scan_result_invalid", "The scanner returned an invalid result. Try scanning again.")
    } ?: return fail("scan_pdf_missing", "The scanner finished without returning a PDF. Try scanning again.")
    if (pdf.pageCount <= 0) return fail("scan_page_count_invalid", "The scanner reported no pages in its PDF. Try scanning again.")
    val uri = pdf.uri
    if (!uri.isScanFile()) return fail("scan_uri_invalid", unreadableScan)
    onDocument(ScannedDocument(uri))
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
    fun launchFailed() {
        Log.w(scannerTag, "scan_launch_failed")
        report(scannerUnavailable)
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        val requestedOwner = pendingOwner
        pendingOwner = null
        if (requestedOwner == null || requestedOwner != currentOwner) return@rememberLauncherForActivityResult
        handleDocumentScanResult(result, deliver, report)
    }

    return launch@{
        val requestedOwner = owner ?: return@launch
        if (preparing || pendingOwner != null) return@launch
        val activity = context.scannerActivity()
        if (activity == null) {
            launchFailed()
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
                            launchFailed()
                        }
                    }
                }
                .addOnFailureListener {
                    preparing = false
                    if (attached.value && requestedOwner == currentOwner) launchFailed()
                }
        } catch (_: Exception) {
            preparing = false
            launchFailed()
        }
    }
}

private tailrec fun Context.scannerActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.scannerActivity()
    else -> null
}
