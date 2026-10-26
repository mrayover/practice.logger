package com.mrayover.practicelogger

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** CI runs at 200% system font size. Screenshots retained for actual visual review. */
class AccessibilityWorkflowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun shown(text: String) { compose.waitUntil(15000) { compose.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty() } }
    private fun click(text: String) { compose.onNodeWithText(text).performScrollTo().performClick() }
    private fun readable(text: String) {
        val node = compose.onAllNodesWithText(text, useUnmergedTree = true)[0].performScrollTo().assertIsDisplayed().fetchSemanticsNode()
        val results = mutableListOf<TextLayoutResult>()
        node.config[SemanticsActions.GetTextLayoutResult].action!!.invoke(results)
        if (results.isEmpty() || results.any { it.hasVisualOverflow }) {
            println("TEXT LAYOUT: " + results.map { "size=${it.size}, paragraph=${it.multiParagraph.width}x${it.multiParagraph.height}, lines=${it.lineCount}, widthOverflow=${it.didOverflowWidth}, heightOverflow=${it.didOverflowHeight}" })
            captureStage3("text-overflow")
        }
        assertTrue("Text was clipped: $text", results.isNotEmpty() && results.none { it.hasVisualOverflow })
    }
    @Test fun largeTextCatalogBuilderFocusSummaryAndBackupRemainUsable() {
        assertTrue(compose.activity.resources.configuration.fontScale >= 1.9f)
        shown("Practice Now")
        val db = BlockDatabase.open(compose.activity.applicationContext)
        try {
            runBlocking { BlockRepository(db).save(null, BlockDraft("Accessible fixture", "Play slowly with relaxed hands", "1")) }
            click("Practice Now"); shown("Add Accessible fixture"); click("Add Accessible fixture"); shown("1. Accessible fixture"); compose.waitForIdle(); click("Add Accessible fixture")
            shown("2. Accessible fixture"); readable("Change duration")
            // Repeated controls identify their Block for assistive technology.
            compose.onNodeWithContentDescription("Move Accessible fixture, placement 2, up").assertExists()
            compose.onNode(hasText("Down") and hasAnyAncestor(hasTestTag("placement-0"))).performScrollTo().assertIsDisplayed().performClick()
            captureStage3("10-large-builder")
            click("Start Session"); shown("Ready when you are")
            compose.onNodeWithTag("practice-timer").performScrollTo().assertIsDisplayed().assertContentDescriptionEquals("Time remaining 1:00")
            readable("Complete"); captureStage3("11-large-focus"); click("Complete"); shown("Block 2 of 2"); click("Skip")
            shown("Session Summary"); readable("Session Summary"); captureStage3("12-large-summary")
            readable("Save note"); click("Home"); click("Manage Blocks"); shown("Block Library"); readable("New Block")
            click("New Block"); shown("Default duration (minutes)"); readable("Cancel"); click("Cancel"); click("Back to Home")
            click("Backup and restore"); shown("Export backup"); readable("Export backup"); readable("Choose backup to restore")
            captureStage3("13-large-backup")
        } finally { db.close() }
    }
}
