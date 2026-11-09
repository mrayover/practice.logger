package com.mrayover.practicelogger

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.UUID

class InspirationsWorkflowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val context get() = compose.activity.applicationContext
    private val vm get() = ViewModelProvider(compose.activity)[InspirationsViewModel::class.java]
    private val db get() = BlockDatabase.appInstance(context)

    @Test fun includedWorksRenderCompletelyAfterNavigationAndRecreation() {
        val works = mapOf(
            "Homeric Hymn 25 — To the Muses and Apollo" to "homeric-hymn-25.txt",
            "Theogony — Invocation to the Muses" to "theogony-invocation.txt",
            "Orphic Hymn 76 — To the Muses" to "orphic-hymn-76.txt",
            "A Song for St. Cecilia's Day, 1687" to "dryden-st-cecilia.txt",
            "Hail! Bright Cecilia, 1692" to "brady-hail-bright-cecilia.txt"
        )
        openInspirations()
        repeat(2) { pass ->
            if (pass == 1) {
                click("Media")
                click("Inspirations")
                compose.activityRule.scenario.recreate()
            }
            for ((title, file) in works) {
                click(title)
                var remaining = context.assets.open("inspirations/$file").bufferedReader()
                    .use { it.readText().removeSuffix("\n") }
                assertTrue(remaining.isNotBlank())
                while (remaining.isNotEmpty()) {
                    val firstLine = remaining.substringBefore('\n')
                    val section = SemanticsMatcher("starts with the next verse line") { node ->
                        node.config.getOrNull(SemanticsProperties.Text)
                            ?.any { it.text.startsWith(firstLine) } == true
                    }
                    compose.onNode(hasScrollToIndexAction()).performScrollToNode(section)
                    val rendered = compose.onNode(section).assertIsDisplayed().fetchSemanticsNode()
                        .config[SemanticsProperties.Text].single().text
                    assertTrue("Unexpected rendered text for $title", remaining == rendered ||
                        remaining.startsWith("$rendered\n"))
                    remaining = remaining.removePrefix(rendered).removePrefix("\n")
                }
                compose.onNodeWithText("Edit inspiration").assertDoesNotExist()
                click("Back to Inspirations")
            }
        }
    }

    @Test fun journalNavigationAndTextEditingProtectDrafts() {
        click("Log")
        waitFor(hasTestTag("log-editor") and isEnabled())
        compose.onNodeWithTag("log-editor").performTextInput("Keep this journal entry")
        androidx.test.uiautomator.UiDevice.getInstance(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()).pressBack()
        click("Playground")
        shown("Coming Soon")
        click("Back to Log")
        waitFor(hasText("Keep this journal entry", substring = true))
        // The banner returns to Home using the existing journal save barrier.
        compose.onNodeWithTag("home-brand", useUnmergedTree = true).performClick()
        openInspirations()
        click("Homeric Hymn 25 — To the Muses and Apollo")
        readBody("I will begin with the Muses")
        click("Back to Inspirations")
        click("Add Inspiration")
        click("Write / Paste")
        val body = "Start of a long document\n" + (1..230).joinToString("\n") { "Musical thought $it" }
        compose.onNode(hasSetTextAction() and hasText("Title (required)")).performTextInput("Notebook")
        compose.onNodeWithTag("inspiration-body").performScrollTo().performTextInput(body)
        click("Save inspiration")
        readBody("Start of a long document")
        val id = runBlocking { db.inspirations().observe().first().single { it.title == "Notebook" }.id }
        click("Edit inspiration")
        compose.onNode(hasSetTextAction() and hasText("Title (required)")).performTextReplacement("Unsaved title")
        click("Back")
        shown("Discard unsaved changes?")
        click("Cancel")
        click("Save inspiration")
        waitFor(hasText("Edit inspiration"))
        assertEquals("Unsaved title", runBlocking { db.inspirations().find(id)?.title })
        compose.activityRule.scenario.recreate()
        click("Back to Inspirations")
        click("Unsaved title")
        readBody("Start of a long document")
        click("Delete inspiration")
        shown("Delete this inspiration?")
        compose.onAllNodesWithText("Delete inspiration").onLast().performClick()
        compose.waitUntil(10_000) { runBlocking { db.inspirations().find(id) == null } }
        click("Media")
        compose.onNodeWithTag("media-library-list").assertExists()
    }

    @Test fun importsRenderAndCameraResultKeepsOrientation() {
        val folder = File(context.cacheDir, "inspiration-ui-" + UUID.randomUUID()).apply { mkdir() }
        try {
            openInspirations()
            val unsupported = File(folder, "wrong.docx").apply { writeText("not supported") }
            click("Add Inspiration")
            compose.runOnIdle { vm.openFile(Uri.fromFile(unsupported)) }
            waitFor(hasText("Choose a PDF, JPEG, PNG, WebP", substring = true))
            val markdown = File(folder, "Imported.md").apply { writeText("Imported text with café") }
            compose.runOnIdle { vm.openFile(Uri.fromFile(markdown)) }
            click("Save inspiration")
            waitFor(hasText("Imported text with café"))
            click("Edit inspiration")
            compose.onNodeWithTag("inspiration-body").performScrollTo().performTextReplacement("Updated café")
            click("Save inspiration")
            waitFor(hasText("Updated café"))
            click("Back to Inspirations")

            val pdf = File(folder, "Score.pdf")
            val document = PdfDocument()
            try {
                repeat(2) { i ->
                    val page = document.startPage(PdfDocument.PageInfo.Builder(160, 120, i + 1).create())
                    page.canvas.drawColor(Color.GREEN)
                    document.finishPage(page)
                }
                pdf.outputStream().use(document::writeTo)
            } finally { document.close() }
            click("Add Inspiration")
            compose.runOnIdle { vm.openFile(Uri.fromFile(pdf)) }
            click("Save inspiration")
            click("View PDF")
            waitFor(hasTestTag("reference-viewer"))
            click("Next page")
            shown("Page 2 of 2")
            waitFor(hasTestTag("reference-viewer"))
            click("Back to inspiration")
            click("Back to Inspirations")

            val jpg = File(folder, "Photo.jpg")
            val image = Bitmap.createBitmap(48, 24, Bitmap.Config.ARGB_8888)
            jpg.outputStream().use { assertTrue(image.compress(Bitmap.CompressFormat.JPEG, 90, it)) }
            image.recycle()
            ExifInterface(jpg).apply {
                setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
                saveAttributes()
            }
            click("Add Inspiration")
            compose.runOnIdle { vm.openFile(Uri.fromFile(jpg)) }
            click("Save inspiration")
            click("View image")
            waitFor(hasTestTag("reference-viewer"))
            click("Back to inspiration")
            click("Back to Inspirations")

            click("Add Inspiration")
            compose.runOnIdle {
                vm.launchCamera { vm.cameraResult(false) }
            }
            waitFor(hasText("No photo was captured.", substring = true))
            compose.runOnIdle {
                vm.launchCamera { uri ->
                    checkNotNull(context.contentResolver.openOutputStream(uri)).use { it.write(jpg.readBytes()) }
                    vm.cameraResult(true)
                }
            }
            compose.onNode(hasSetTextAction() and hasText("Title (required)")).performTextInput("Camera page")
            click("Save inspiration")
            click("View image")
            waitFor(hasTestTag("reference-viewer"))
            click("Reset zoom")
            val repository = InspirationRepository(db, context.filesDir)
            runBlocking {
                val row = db.inspirations().observe().first().single { it.title == "Camera page" }
                val rendered = repository.visualPage(row, 0).bitmap
                assertEquals(24, rendered.width)
                assertEquals(48, rendered.height)
                rendered.recycle()
            }
            click("Back to inspiration")
            click("Edit inspiration")
            click("Remove attachment")
            compose.onAllNodesWithText("Remove attachment").onLast().performClick()
            click("Save inspiration")
            compose.onNodeWithText("View image").assertDoesNotExist()
        } finally { folder.deleteRecursively() }
    }

    private fun openInspirations() {
        waitFor(hasTestTag("home-menu"))
        click("Library")
        click("Inspirations")
        shown("Included Inspirations")
    }

    private fun waitFor(matcher: SemanticsMatcher) {
        compose.waitUntil(15_000) { compose.onAllNodes(matcher).fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty() }
    }

    private fun readBody(text: String) {
        val matcher = hasText(text, substring = true)
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(matcher)
        compose.onNode(matcher).assertIsDisplayed()
    }

    private fun shown(label: String) {
        waitFor(hasText(label))
        compose.onNodeWithText(label).assertIsDisplayed()
    }

    private fun click(label: String) {
        val node = compose.onNodeWithText(label)
        if (compose.onAllNodesWithText(label).fetchSemanticsNodes(atLeastOneRootRequired = false).isEmpty() &&
            compose.onAllNodes(hasScrollToIndexAction()).fetchSemanticsNodes(atLeastOneRootRequired = false).size == 1) {
            compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(label))
        }
        waitFor(hasText(label))
        if (!node.isDisplayed()) node.performScrollTo()
        node.performClick()
    }
}
