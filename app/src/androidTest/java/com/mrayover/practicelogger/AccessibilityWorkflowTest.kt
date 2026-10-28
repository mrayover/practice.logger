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
    private fun click(text: String) {
        compose.waitUntil(15000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
        visible(compose.onNodeWithText(text)).performClick()
    }
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
        captureStage3("00-large-home-top")
        readable("practice.logger"); readable("Goals"); readable("Practice Now"); readable("Blocks")
        readable("Routines"); readable("Tools"); readable("Recording"); readable("Data usage")
        captureStage3("00-large-home-actions")
        click("Edit"); readable("Manage Goals"); readable("New Goal"); readable("Active Goals"); readable("Completed Goals")
        click("2. Goal workflow edited · 37%"); readable("37% complete · set by you"); readable("Save progress")
        captureStage3("goal-07-large-detail")
        readable("Practice Goal"); readable("Manage Blocks"); readable("Edit Goal"); readable("Mark Goal completed")
        click("Edit Goal"); readable("Goal title"); readable("Desired outcome (optional)")
        readable("Completion percentage (0–100)"); readable("Goal notes (optional)"); readable("Save Goal")
        captureStage3("goal-08-large-editor")
        click("Cancel Goal editing"); click("Manage Blocks"); readable("Find Blocks")
        readable("Remove Goal active fixture"); readable("Remove Goal archived fixture (archived)")
        captureStage3("goal-09-large-blocks")
        click("Back to Goal"); click("Back to Goals"); click("Back to Home")
        openHomeTools(compose); readable("practice.logger")
        readable("Metronome"); readable("Tuner"); readable("Timer")
        click("Metronome"); readable("practice.logger"); readable("120"); readable("Start click")
        captureStage3("01-large-tool-metronome")
        click("Back to Tools"); click("Tuner"); readable("Start tuner")
        captureStage3("02-large-tool-tuner")
        click("Back to Tools"); click("Timer"); readable("Start stopwatch"); readable("Reset stopwatch")
        readable("Minute timer"); readable("Start timer"); readable("Reset timer")
        captureStage3("03-large-tool-timer")
        click("Back to Tools"); click("Back to Home")
        click("Recording"); readable("Free recording"); readable("Saved free recordings")
        click("Saved free recordings")
        compose.onNodeWithTag("take-list").performScrollToNode(hasText("Record take")); readable("Record take")
        compose.onNodeWithText("Record phrase").assertDoesNotExist()
        captureStage3("04-large-free-recordings")
        compose.onNodeWithTag("take-list").performScrollToNode(hasText("Close recordings"))
        click("Close recordings"); click("Back to Home")
        val db = BlockDatabase.open(compose.activity.applicationContext)
        try {
            runBlocking { BlockRepository(db).save(null, BlockDraft("Accessible fixture", "Play slowly with relaxed hands", "1")) }
            openHomeRoutines(compose); readable("New Routine"); click("New Routine")
            shown("Routine name"); readable("Routine name")
            compose.onNodeWithText("Routine name").performTextInput("Large text routine")
            click("Add Accessible fixture"); shown("1. Accessible fixture"); click("Add Accessible fixture"); shown("2. Accessible fixture")
            compose.onNodeWithContentDescription("Move Accessible fixture, Routine placement 2, up").performScrollTo().assertIsDisplayed().performClick()
            compose.onNode(hasText("Change duration") and hasAnyAncestor(hasTestTag("routine-placement-0"))).performScrollTo().performClick()
            readable("Minutes"); readable("Apply duration"); compose.onNodeWithText("Minutes").performTextReplacement("2")
            compose.onNodeWithText("Apply duration").performClick(); readable("Routine duration: 2:00")
            compose.waitForIdle(); captureStage3("05-large-routine-duration")
            readable("Save Routine"); captureStage3("06-large-routine-save"); click("Save Routine"); shown("Large text routine")
            readable("Practice Routine"); readable("Edit Routine"); readable("Delete Routine")
            click("Back to Home")
            click("Practice Now"); shown("Add Accessible fixture"); click("Add Accessible fixture"); shown("1. Accessible fixture"); compose.waitForIdle(); click("Add Accessible fixture")
            shown("2. Accessible fixture"); readable("Change duration")
            // Repeated controls identify their Block for assistive technology.
            compose.onNodeWithContentDescription("Move Accessible fixture, placement 2, up").assertExists()
            compose.onNode(hasText("Down") and hasAnyAncestor(hasTestTag("placement-0"))).performScrollTo().assertIsDisplayed().performClick()
            captureStage3("10-large-builder")
            readable("Save as Routine"); click("Save as Routine"); readable("Routine name"); readable("Save Routine"); captureStage3("10-large-save-as-routine"); compose.onNodeWithText("Cancel").performClick()
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
            readable("Click while recording")
            compose.onNodeWithTag("phrase-recording-click").performScrollTo().assertIsOn().performClick().assertIsOff()
            compose.onNodeWithTag("phrase-recording-click").performClick().assertIsOn()
            readable("Start 4-click count-in"); captureStage3("11-large-phrase-setup")
            compose.onNodeWithText("Cancel").performClick()
            compose.onNodeWithTag("take-list").performScrollToNode(hasText("Close recordings")); compose.onNodeWithText("Close recordings").performClick()
            readable("Complete"); captureStage3("11-large-focus"); click("Complete"); shown("Block 2 of 2"); click("Skip")
            shown("Session Summary"); readable("Session Summary"); captureStage3("12-large-summary")
            readable("Save note"); click("Home"); click("Blocks"); shown("Block Library"); readable("New Block")
            click("New Block"); shown("Default duration (minutes)"); readable("Cancel"); click("Cancel"); click("Back to Home")
            openHomeTools(compose); click("Backup and restore"); shown("Export backup"); readable("Export backup"); readable("Choose backup to restore")
            captureStage3("13-large-backup")
        } finally { db.close() }
    }
}
