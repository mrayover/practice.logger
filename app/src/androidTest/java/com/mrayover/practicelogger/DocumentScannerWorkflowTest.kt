package com.mrayover.practicelogger

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Parcel
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import android.provider.MediaStore
import java.io.File
import java.io.RandomAccessFile
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.UUID

class DocumentScannerWorkflowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val app get() = compose.activity.application
    private val db get() = BlockDatabase.appInstance(app)

    @Test fun libraryKeepsOnePdfAndPageOrderAcrossRecreation() = withScan(Color.BLUE, Color.RED, Color.GREEN) { document ->
        click("Library")
        val vm = ViewModelProvider(compose.activity)[MediaLibraryViewModel::class.java]
        click("Add to Library")
        compose.onNodeWithText(scanDocumentLabel).assertExists()
        click("Cancel")
        val baseline = runBlocking { db.media().readAll().map { it.id }.toSet() }
        compose.runOnIdle { vm.importScan(document) }
        compose.waitUntil(15_000) { !vm.busy && vm.assets.value.any { it.id !in baseline } }
        compose.activityRule.scenario.recreate()
        click("PDFs")
        val pdf = runBlocking { db.media().readAll().single { it.id !in baseline } }
        try {
            assertEquals(scannedPdfMime, pdf.mimeType)
            assertEquals(document.filename, pdf.originalName)
            assertEquals(3, pdf.pageCount)
            listOf(Color.BLUE, Color.RED, Color.GREEN).forEachIndexed { index, expected ->
                val page = runBlocking { vm.repository.visualPage(pdf.id, index) }
                try { assertEquals(expected, page.bitmap.getPixel(12, 12)) } finally { page.bitmap.recycle() }
            }
            compose.onNodeWithTag("media-library-list").performScrollToNode(hasTestTag("media-item-${pdf.id}"))
            compose.onNodeWithTag("media-open-${pdf.id}").performScrollTo().performClick()
            click("Next page")
            compose.onNodeWithText("Page 2 of 3").assertExists()
        } finally { runBlocking { vm.repository.delete(pdf.id) } }
    }

    @Test fun blockStagesUntilSaveAndDiscardsWithoutLibraryInsert() = withScan(Color.MAGENTA) { document ->
        val vm = ViewModelProvider(compose.activity)[ImageReferenceViewModel::class.java]
        val editor = vm.editor
        runBlocking {
            val baseline = db.media().readAll().map { it.id }.toSet()
            editor.open(UUID.randomUUID().toString(), null)
            assertTrue(editor.scan(document))
            assertEquals(1, editor.visual?.pageCount)
            assertFalse(editor.scan(document))
            assertEquals(baseline, db.media().readAll().map { it.id }.toSet())
            editor.discard()
            assertEquals(baseline, db.media().readAll().map { it.id }.toSet())

            editor.open(UUID.randomUUID().toString(), null)
            assertTrue(editor.scan(document))
            val id = editor.save(null, BlockDraft(name = "Scan fixture", action = "Read the score"), null)
            val pdf = db.media().readAll().single { it.id !in baseline }
            val identity = "library:${pdf.id}"
            try {
                assertEquals(listOf(identity), db.referenceOrder(id, MediaRole.VISUAL))
                assertEquals(1, pdf.pageCount)
            } finally {
                editor.discard()
                vm.associations.removeReference(id, MediaRole.VISUAL, identity, listOf(identity))
                BlockRepository(db).deletePermanently(id)
                ViewModelProvider(compose.activity)[MediaLibraryViewModel::class.java].repository.delete(pdf.id)
            }
        }
    }

    @Test fun inspirationCopiesScanAndKeepsMetadataOnReplacement() = withScan(Color.BLACK, Color.CYAN) { document ->
        click("Library")
        click("Inspirations")
        click("Add Inspiration")
        compose.onNodeWithText(scanDocumentLabel).assertExists()
        val vm = ViewModelProvider(compose.activity)[InspirationsViewModel::class.java]
        compose.runOnIdle { vm.openScan(document) }
        compose.waitUntil(15_000) { vm.draft?.scanPages == 2 }
        compose.runOnIdle {
            val draft = checkNotNull(vm.draft)
            vm.draft = draft.copy(details = draft.details.copy(title = "Scanner fixture", author = "Author", source = "Source",
                category = "Scores", note = "Notes"))
        }
        click("Save inspiration")
        compose.waitUntil(15_000) { vm.current?.pageCount == 2 }
        click("Edit inspiration")
        compose.onNodeWithText(scanDocumentLabel).assertExists()
        compose.runOnIdle { vm.openScan(document) }
        compose.waitUntil(15_000) { vm.draft?.scanPages == 2 }
        compose.activityRule.scenario.recreate()
        click("Save inspiration")
        compose.waitUntil(15_000) { !vm.busy && vm.draft == null }
        val row = checkNotNull(vm.current)
        assertEquals(listOf("Scanner fixture", "Author", "Source", "Scores", "Notes"),
            listOf(row.title, row.author, row.source, row.category, row.note))
        click("View PDF")
        click("Next page")
        compose.onNodeWithText("Page 2 of 2").assertExists()
        click("Back to inspiration")
        compose.runOnIdle { vm.delete() }
        compose.waitUntil(15_000) { vm.current == null }
    }

    @Test fun invalidOversizedAndMissingScanLeaveLibraryIntact() {
        val vm = ViewModelProvider(compose.activity)[MediaLibraryViewModel::class.java]
        val baseline = runBlocking { db.media().readAll().map { it.id }.toSet() }
        val capture = ReferencePhotoCapture(app)
        val name = capture.create()
        val document = ScannedDocument(capture.uri(name))
        fun expectFailure(messagePart: String) {
            compose.runOnIdle { vm.importScan(document) }
            compose.waitUntil(15_000) { !vm.busy && vm.message?.contains(messagePart, ignoreCase = true) == true }
            assertEquals(baseline, runBlocking { db.media().readAll().map { it.id }.toSet() })
        }
        try {
            capture.file(name).writeText("%PDF-1.7\nbroken scan")
            expectFailure("PDF")
            RandomAccessFile(capture.file(name), "rw").use { it.setLength(20L * 1024 * 1024 + 1) }
            expectFailure("Scan fewer pages")
            capture.discard(name)
            expectFailure("its file could not be read")
        } finally { capture.discard(name) }
    }

    @Test fun resultCallbackSeparatesCancellationLaunchAndInvalidResults() {
        fun handle(code: Int, data: Intent?, expected: String?) {
            val errors = mutableListOf<String>()
            handleDocumentScanResult(ActivityResult(code, data), { fail("Unexpected document") }, errors::add)
            if (expected == null) assertTrue(errors.isEmpty())
            else assertTrue(errors.single().contains(expected, ignoreCase = true))
        }

        handle(Activity.RESULT_CANCELED, null, null)
        val senderFailure = Intent(ActivityResultContracts.StartIntentSenderForResult.ACTION_INTENT_SENDER_REQUEST)
        handle(Activity.RESULT_CANCELED, senderFailure, "scanner unavailable")
        handle(Activity.RESULT_FIRST_USER, Intent(), "could not finish")
        handle(Activity.RESULT_OK, null, "without returning a result")
        handle(Activity.RESULT_OK, Intent(), "without returning a PDF")
        handle(Activity.RESULT_OK, scannerIntent(null, 0), "without returning a PDF")
        handle(Activity.RESULT_OK, scannerIntent(Uri.fromFile(File(app.cacheDir, "scan.pdf")), 0), "no pages")
        handle(Activity.RESULT_OK, scannerIntent(Uri.parse("https://example.invalid/scan.pdf"), 1), "could not be read")
    }

    @SdkSuppress(minSdkVersion = 29)
    @Test fun regularUploadStillUsesAndroidFilesAndStoresTheOriginalPdf() {
        val name = "scanner-regression-upload.pdf"
        val bytes = l1Fixture("references/score.pdf")
        val source = checkNotNull(app.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, scannedPdfMime)
        }))
        val vm = ViewModelProvider(compose.activity)[MediaLibraryViewModel::class.java]
        try {
            checkNotNull(app.contentResolver.openOutputStream(source)).use { it.write(bytes) }
            click("Library")
            compose.waitUntil(15_000) { vm.ready }
            click("Add to Library")
            click("Upload file")
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            checkNotNull(device.wait(Until.findObject(By.desc("Show roots")), 10_000)).click()
            assertTrue(device.wait(Until.hasObject(By.text("Downloads")), 10_000))
            device.findObjects(By.text("Downloads")).last().click()
            checkNotNull(device.wait(Until.findObject(By.text(name)), 10_000)).click()
            compose.waitUntil(15_000) { !vm.busy && vm.assets.value.any { it.originalName == name } }
            val asset = vm.assets.value.single { it.originalName == name }
            assertEquals(2, asset.pageCount)
            assertArrayEquals(bytes, File(app.filesDir, "media-library/${asset.fileName}").readBytes())
        } finally {
            app.contentResolver.delete(source, null, null)
            vm.assets.value.firstOrNull { it.originalName == name }?.let { asset ->
                runBlocking { vm.repository.delete(asset.id) }
            }
        }
    }

    private fun click(label: String) {
        compose.waitUntil(15_000) { compose.onAllNodesWithText(label).fetchSemanticsNodes().isNotEmpty() }
        val button = compose.onNodeWithText(label)
        if (!button.isDisplayed()) button.performScrollTo()
        button.performClick()
    }

    // Exercise the SDK callback with the private file URI the Google delegate returns after a scan.
    private fun withScan(vararg colors: Int, test: (ScannedDocument) -> Unit) {
        val file = File.createTempFile("scan-callback-", ".pdf", app.cacheDir)
        try {
            val pdf = PdfDocument()
            try {
                colors.forEachIndexed { index, color ->
                    val page = pdf.startPage(PdfDocument.PageInfo.Builder(96, 112, index + 1).create())
                    page.canvas.drawColor(color)
                    pdf.finishPage(page)
                }
                file.outputStream().use(pdf::writeTo)
            } finally { pdf.close() }
            val data = scannerIntent(Uri.fromFile(file), colors.size)
            val documents = mutableListOf<ScannedDocument>()
            handleDocumentScanResult(ActivityResult(Activity.RESULT_OK, data), documents::add, ::fail)
            test(documents.single())
        } finally { file.delete() }
    }

    private fun scannerIntent(uri: Uri?, pages: Int): Intent {
        // ML Kit 16.0.0 does not expose a fixture builder. Match its delegate's parcelable
        // envelope here so production uses the public fromActivityResultIntent parser.
        val fixture = GmsDocumentScanningResult::class.java.getDeclaredMethod(
            "zza", List::class.java, List::class.java, Uri::class.java, Int::class.javaPrimitiveType
        ).invoke(null, emptyList<Uri>(), emptyList<Uri>(), uri, pages) as GmsDocumentScanningResult
        val data = Intent().putExtra("extra_scanning_result", fixture)
        val parcel = Parcel.obtain()
        return try {
            data.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)
            Intent.CREATOR.createFromParcel(parcel).apply { setExtrasClassLoader(fixture.javaClass.classLoader) }
        } finally { parcel.recycle() }
    }
}
