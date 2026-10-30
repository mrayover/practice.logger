package com.mrayover.practicelogger

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Independent fixture permits targeted 200% Goal checks before the full regression matrix. */
class GoalAccessibilityTest {
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
    @Test fun goalNavigationFieldsAndActionsRemainReadableAtLargeText() {
        assertTrue(compose.activity.resources.configuration.fontScale >= 1.9f)
        val db = BlockDatabase.open(compose.activity.applicationContext)
        try {
            runBlocking {
                if (db.goals().readAll().none { it.goal.title == "Goal workflow edited" }) {
                    val b = BlockRepository(db).save(null, BlockDraft("Goal active fixture", "Play melody", "2"))
                    val archived = BlockRepository(db).save(null, BlockDraft("Goal archived fixture", "Play changes", "1"))
                    val repo = GoalRepository(db)
                    repo.save(null, "Second Goal", "", 0, "")
                    val g = repo.save(null, "Goal workflow edited", "Memorize melody and changes", 37, "Listen and assess")
                    repo.setBlocks(g, listOf(b, archived)); BlockRepository(db).setArchived(archived, true)
                    val s = SessionRepository(db); val id = s.startGoal(g); s.checkpoint(id, 1000); s.finishBlock(id, "COMPLETED", "")
                }
            }
            shown("2. Goal workflow edited · 37%")
            readable("practice.logger"); readable("Goals"); readable("2. Goal workflow edited · 37%")
            captureStage3("goal-07-large-home")
            click("Edit"); readable("Manage Goals"); readable("New Goal"); readable("Active Goals"); readable("Completed Goals"); captureStage3("goal-07-large-management")
            click("2. Goal workflow edited · 37%"); readable("37% complete · set by you"); readable("Save progress")
            captureStage3("goal-07-large-detail")
            readable("Practice Goal"); readable("Manage Blocks"); readable("Edit Goal"); readable("Mark Goal completed")
            click("Edit Goal"); readable("Goal title"); readable("Desired outcome (optional)")
            readable("Completion percentage (0–100)"); readable("Goal notes (optional)"); readable("Save Goal")
            captureStage3("goal-08-large-editor")
            click("Cancel Goal editing"); click("Manage Blocks"); readable("Find Blocks")
            readable("Remove Goal active fixture"); readable("Remove Goal archived fixture (archived)")
            captureStage3("goal-09-large-blocks")
            click("Back to Goal"); click("Back to Goals"); compose.onNodeWithTag("home-brand", useUnmergedTree = true).performClick()
        } finally { db.close() }
    }
}
