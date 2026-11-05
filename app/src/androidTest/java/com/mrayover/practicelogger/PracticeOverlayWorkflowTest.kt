package com.mrayover.practicelogger

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class PracticeOverlayWorkflowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val app get() = compose.activity.application
    private val db get() = BlockDatabase.appInstance(app)
    private val vm get() = ViewModelProvider(compose.activity)[ImageReferenceViewModel::class.java]

    @Test fun editCopyAndRemoveSharedPdfMarkings() {
        val original = runBlocking {
            db.blocks().insert(Block("score-source", "Overlay score", "Read", createdAt = 1, updatedAt = 1))
            db.blocks().insert(Block("score-copy", "Copy destination", "Read", createdAt = 1, updatedAt = 1))
            vm.library.import("Overlay score.pdf", "application/pdf") {
                l1Fixture("references/score.pdf").inputStream()
            }.also {
                vm.associations.link("score-source", MediaRole.VISUAL, it.id)
                vm.associations.link("score-copy", MediaRole.VISUAL, it.id)
            }
        }
        val file = File(app.filesDir, "media-library/${original.fileName}")
        val hash = l1Hash(file)
        fun shapes(block: String = "score-source") = runBlocking { db.overlays().read(block, "library:${original.id}") }
        fun count(expected: Int) = compose.waitUntil(15_000) { shapes().size == expected }
        click("Blocks")
        compose.onNodeWithTag("block-library-list").performScrollToNode(hasText("Overlay score"))
        click("Overlay score")
        click("Show reference")
        tools("Edit markings")
        tools("Highlight")
        val layer = compose.onNodeWithTag("overlay-layer")
        layer.scrollIfNeeded().performTouchInput {
            swipe(Offset(width * .2f, height * .23f), Offset(width * .74f, height * .52f), 400)
        }
        count(1)
        val highlight = shapes().single()
        assertEquals("HIGHLIGHT", highlight.type)
        assertEquals(0, highlight.page)
        layer.scrollIfNeeded().performTouchInput {
            swipe(Offset(width * .44f, height * .37f), Offset(width * .51f, height * .50f), 400)
        }
        compose.waitUntil(15_000) { shapes().single().top > highlight.top }
        val moved = shapes().single()
        compose.overlayAction("Shrink marking")
        compose.waitUntil(15_000) { shapes().single().right - shapes().single().left < moved.right - moved.left }
        tools("Highlight color · Blue")
        compose.waitUntil(15_000) { shapes().single().appearance == 1 }
        tools("Undo last edit")
        compose.waitUntil(15_000) { shapes().single().appearance == 0 }
        tools("Cover")
        place("Cover")
        count(2)
        val cover = shapes().single { it.type == "COVER" }
        val peek = compose.onNodeWithTag("overlay-peek").scrollIfNeeded().assertIsDisplayed()
        peek.performTouchInput { down(center) }
        peek.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Revealing covers"))
        peek.performTouchInput { up() }
        peek.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Covers visible"))
        assertEquals(cover, shapes().single { it.type == "COVER" })
        click("Next page")
        tools("Focus")
        place("Focus")
        count(3)
        assertEquals(1, shapes().single { it.type == "FOCUS" }.page)
        tools("Delete marking")
        count(2)
        tools("Undo last edit")
        count(3)
        click("Done")
        tools("Hide markings")
        layer.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Markings hidden"))
        tools("Show markings")
        layer.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "1 markings"))
        tools("Copy overlays to Block")
        click("Copy destination")
        compose.waitUntil(15_000) { shapes("score-copy").size == 3 }
        assertEquals(shapes().map { it.copy(id = "", blockId = "") }, shapes("score-copy").map { it.copy(id = "", blockId = "") })
        assertTrue(shapes().map { it.id }.intersect(shapes("score-copy").map { it.id }.toSet()).isEmpty())

        layer.scrollIfNeeded().performTouchInput {
            pinch(Offset(width * .35f, height * .4f), Offset(width * .65f, height * .6f),
                Offset(width * .12f, height * .22f), Offset(width * .88f, height * .78f))
        }
        compose.onNodeWithText("Reset PDF zoom").assertExists()
        compose.runOnIdle { compose.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
        compose.waitUntil(15_000) { compose.activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE }
        compose.waitUntil(15_000) { compose.onAllNodesWithText("Page 2 of 2").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(3, shapes().size)
        compose.onNodeWithTag("block-VISUAL-remove").scrollIfNeeded().performClick()
        compose.waitUntil(15_000) { compose.onAllNodesWithText("This discards this Block’s saved markings", substring = true)
            .fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(hasText("Cancel") and hasAnyAncestor(isDialog())).performClick()
        assertEquals(3, shapes().size)
        compose.onNodeWithTag("block-VISUAL-remove").scrollIfNeeded().performClick()
        click("Remove reference")
        count(0)
        assertEquals(3, shapes("score-copy").size)
        assertEquals(hash, l1Hash(file))
    }

    private fun place(tool: String) {
        shown(hasTestTag("overlay-layer"))
        compose.overlayAction("Place $tool in the center")
    }

    private fun tools(action: String) {
        shown(hasTestTag("overlay-tools"))
        compose.onNodeWithTag("overlay-tools").scrollIfNeeded().assertIsDisplayed().performClick()
        click(action)
    }

    private fun shown(matcher: SemanticsMatcher) = compose.waitUntil(15_000) {
        compose.onAllNodes(matcher).fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()
    }

    private fun click(label: String) {
        shown(hasText(label))
        compose.onNodeWithText(label).scrollIfNeeded().assertIsDisplayed().performClick()
    }
}

internal fun ComposeTestRule.overlayAction(label: String) {
    val action = onNodeWithTag("overlay-layer").fetchSemanticsNode()
        .config[SemanticsActions.CustomActions].single { it.label == label }
    runOnIdle { assertTrue(action.action()) }
}
