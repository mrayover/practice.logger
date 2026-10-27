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
    private fun visible(node: SemanticsNodeInteraction): SemanticsNodeInteraction = try { node.assertIsDisplayed() } catch (_: AssertionError) { node.performScrollTo().assertIsDisplayed() }
    private fun click(text: String) { visible(compose.onNodeWithText(text)).performClick() }
    private fun readable(text: String) {
        val node = visible(compose.onAllNodesWithText(text, useUnmergedTree = true)[0])
        compose.waitForIdle()
        val results = mutableListOf<TextLayoutResult>()
        node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action -> action(results) }
        // A cached MultiParagraph can retain a wider allocation than the drawn Text node.
        // Check the actual drawn lines, not hasVisualOverflow's allocation comparison.
        val fits = results.isNotEmpty() && results.all { result ->
            (0 until result.lineCount).all { line ->
                !result.isLineEllipsized(line) && result.getLineLeft(line) >= -1f &&
                    result.getLineRight(line) <= result.size.width + 1f &&
                    result.getLineBottom(line) <= result.size.height + 1f
            }
        }
        if (!fits) captureStage3("text-overflow")
        assertTrue("Drawn text exceeded its bounds: $text; " + results.map { r ->
            "size=${r.size}, lines=" + (0 until r.lineCount).map { "${r.getLineLeft(it)}..${r.getLineRight(it)}, bottom=${r.getLineBottom(it)}" }
        }, fits)
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
            captureStage3("11-large-timer")
            readable("Metronome · 120 BPM"); readable("Set BPM"); readable("Start click")
            click("Set BPM"); readable("Apply BPM")
            compose.onNodeWithText("BPM (30–300)").performTextReplacement("300")
            click("Apply BPM"); readable("Metronome · 300 BPM")
            captureStage3("11-large-metronome")
            click("Recordings")
            compose.onNodeWithTag("take-list").performScrollToNode(hasText("Record take")); readable("Record take")
            captureStage3("11-large-recordings")
            compose.onNodeWithTag("take-list").performScrollToNode(hasText("Record phrase")); readable("Record phrase")
            compose.onNodeWithText("Record phrase").performClick(); readable("Phrase BPM"); readable("30–300"); captureStage3("11-large-phrase-input"); readable("32 beats")
            readable("Start 4-click count-in"); captureStage3("11-large-phrase-setup")
            compose.onNodeWithText("Cancel").performClick()
            compose.onNodeWithTag("take-list").performScrollToNode(hasText("Close recordings")); compose.onNodeWithText("Close recordings").performClick()
            readable("Complete"); captureStage3("11-large-focus"); click("Complete"); shown("Block 2 of 2"); click("Skip")
            shown("Session Summary"); readable("Session Summary"); captureStage3("12-large-summary")
            readable("Save note"); click("Home"); click("Manage Blocks"); shown("Block Library"); readable("New Block")
            click("New Block"); shown("Default duration (minutes)"); readable("Cancel"); click("Cancel"); click("Back to Home")
            click("Backup and restore"); shown("Export backup"); readable("Export backup"); readable("Choose backup to restore")
            captureStage3("13-large-backup")
        } finally { db.close() }
    }
}
