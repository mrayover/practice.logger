package com.mrayover.practicelogger

import android.graphics.Color
import android.graphics.pdf.PdfDocument
import java.io.RandomAccessFile
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.time.LocalDateTime
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
            assertEquals(listOf("library:${pdf.id}"), db.referenceOrder(id, MediaRole.VISUAL))
            assertEquals(1, pdf.pageCount)
            editor.discard()
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
            vm.draft = draft.copy(details = draft.details.copy(title = "Scanner fixture", author = "Author", source = "Source", note = "Notes"))
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
        assertEquals(listOf("Scanner fixture", "Author", "Source", "Notes"), listOf(row.title, row.author, row.source, row.note))
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
            capture.file(name).writeText("broken scan")
            expectFailure("PDF")
            RandomAccessFile(capture.file(name), "rw").use { it.setLength(20L * 1024 * 1024 + 1) }
            expectFailure("Scan fewer pages")
            capture.discard(name)
            expectFailure("Could not update the Library")
        } finally { capture.discard(name) }
    }

    private fun click(label: String) {
        compose.waitUntil(15_000) { compose.onAllNodesWithText(label).fetchSemanticsNodes().isNotEmpty() }
        val button = compose.onNodeWithText(label)
        if (!button.isDisplayed()) button.performScrollTo()
        button.performClick()
    }

    // Use a content URI whose provider filename ends in .jpg, with distinct page colors to verify order.
    private fun withScan(vararg colors: Int, test: (ScannedDocument) -> Unit) {
        val capture = ReferencePhotoCapture(app)
        val name = capture.create()
        try {
            val pdf = PdfDocument()
            try {
                colors.forEachIndexed { index, color ->
                    val page = pdf.startPage(PdfDocument.PageInfo.Builder(96, 112, index + 1).create())
                    page.canvas.drawColor(color)
                    pdf.finishPage(page)
                }
                capture.file(name).outputStream().use(pdf::writeTo)
            } finally { pdf.close() }
            test(ScannedDocument(capture.uri(name), LocalDateTime.of(2026, 11, 22, 9, 8, 7)))
        } finally { capture.discard(name) }
    }
}
