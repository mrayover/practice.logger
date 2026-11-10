package com.mrayover.practicelogger

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.text.TextLayoutResult
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class BlockPillWorkflowTest : WeekUiChecks() {
    private val library = "block-library-list"
    private val weekList = "week-list"

    @Test fun collapsedTextIsBoundedAndActionsAreExplicit() {
        val name = "Long title for compact Block display ".repeat(5).trim()
        val firstLine = "Play the entire phrase with a steady pulse ".repeat(4).trim()
        val action = "$firstLine\nListen to the release before repeating"
        val block = runBlocking {
            BlockRepository(db).save(null, BlockDraft(name, action, "12", "88", "Source notes for the phrase"))
        }
        val images = ViewModelProvider(compose.activity)[ImageReferenceViewModel::class.java]
        runBlocking { images.repository.import(block) { l1Png().inputStream() } }
        val tag = "library-block-$block"

        click("Blocks")
        showPill(library, tag)
        assertLayout(name, 2)
        assertLayout(firstLine, 1)
        compose.onNodeWithText("12 min · 88 BPM").assertExists()
        compose.onNodeWithText("Notes / Source").assertDoesNotExist()
        compose.onNodeWithText("Edit").assertDoesNotExist()
        compose.onNodeWithTag(tag).performClick()
        assertState(tag, "Expanded · Needs work")
        compose.onNodeWithText("Edit Block").assertDoesNotExist()
        compose.onNodeWithTag(library).performScrollToNode(hasText(action))
        compose.onNodeWithText(action).assertExists()
        compose.onNodeWithTag(library).performScrollToNode(hasText("Block-owned image"))
        compose.onNodeWithText("Block-owned image").assertIsDisplayed()
        showPill(library, tag).performClick()
        assertState(tag, "Collapsed · Needs work")
        compose.onNodeWithText(action).assertDoesNotExist()

        openLibraryBlockEditor(compose, name)
        shown("Edit Block")
        compose.onNodeWithTag("Action").assertTextContains(action)
        click("Cancel")
        openBlockPill(compose, library, tag)
        click("Add to Practice", inList = library)
        shown("Build a Session")
        assertEquals(block, runBlocking {
            val draft = db.sessions().withStatus("DRAFT")!!
            db.sessions().find(draft.id)!!.placements.single().sourceBlockId
        })
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        shown("Block Library")
    }

    @Test fun weeklyMarkerUsesRecordedColorAndDoesNotCarryIntoAnotherWeekOrLibrary() {
        val current = practiceWeek()
        val repo = BlockRepository(db)
        val goals = GoalRepository(db)
        val sessions = SessionRepository(db)
        val (block, goal, otherGoal) = runBlocking {
            val first = goals.save(null, "Pill Goal A", "", 0, "")
            val second = goals.save(null, "Pill Goal B", "", 0, "")
            val block = repo.save(null, BlockDraft("Weekly completion fixture", "Practice the phrase",
                primaryGoalId = first, primaryGoalChanged = true))
            repo.addToWeek(current, block)
            repo.addToWeek(current.plusWeeks(1), block)
            Triple(block, first, second)
        }
        openHomePractice(compose)
        click("This Week")
        val tag = "week-block-$block"
        showPill(weekList, tag)
        assertState(tag, "Collapsed · Needs work")
        assertMarker("block-needs-work-marker", tag, leading = true, LightPalette.colors.goalPriorities[0])
        compose.onNodeWithTag("block-completed-marker", useUnmergedTree = true).assertDoesNotExist()

        val color = runBlocking {
            val id = sessions.startBlock(block)
            val placement = db.sessions().find(id)!!.current!!
            sessions.finishBlock(id, "COMPLETED", "", placementId = placement.id)
            goals.move(otherGoal, -1)
            val completion = db.sessions().find(id)!!.completions.single().completion
            assertEquals(goal, completion.goalId)
            Color(checkNotNull(completion.colorArgb))
        }
        compose.waitUntil(15_000) {
            compose.onAllNodesWithTag("block-completed-marker", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
        assertState(tag, "Collapsed · Completed")
        assertMarker("block-completed-marker", tag, leading = false, color)
        compose.onNodeWithTag("block-needs-work-marker", useUnmergedTree = true).assertDoesNotExist()
        click("Next week", inList = weekList)
        showPill(weekList, tag)
        assertState(tag, "Collapsed · Needs work")
        assertMarker("block-needs-work-marker", tag, leading = true, LightPalette.colors.goalPriorities[1])

        compose.onNodeWithTag("home-brand", useUnmergedTree = true).performClick()
        click("Blocks")
        showPill(library, "library-block-$block")
        assertState("library-block-$block", "Collapsed · Needs work")
        assertMarker("block-needs-work-marker", "library-block-$block", leading = true, LightPalette.colors.goalPriorities[1])
        runBlocking { repo.setArchived(block, true) }
        click("Archived", inList = library)
        openBlockPill(compose, library, "library-block-$block")
        compose.onNodeWithText("Add to Practice").assertDoesNotExist()
        compose.onNodeWithText("Add to This Week").assertDoesNotExist()
        click("Restore Block", inList = library)
        shown("Restore this Block?")
        compose.onNode(hasText("Restore") and hasAnyAncestor(isDialog())).performClick()
        showPill(library, "library-block-$block")
        assertFalse(runBlocking { db.blocks().find(block)!!.archived })
        assertEquals(listOf(block), runBlocking { db.weekPlan().read(current.toString()).map { it.blockId } })
    }

    private fun showPill(list: String, tag: String): SemanticsNodeInteraction {
        compose.onNodeWithTag(list).performScrollToNode(hasTestTag(tag))
        return compose.onNodeWithTag(tag).assertIsDisplayed()
    }

    private fun assertState(tag: String, state: String) {
        compose.onNodeWithTag(tag).assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, state))
    }

    private fun assertLayout(text: String, lines: Int) {
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText(text, useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        val layout = layouts.single()
        assertEquals(lines, layout.lineCount)
        assertTrue(layout.isLineEllipsized(lines - 1))
    }

    private fun assertMarker(marker: String, header: String, leading: Boolean, color: Color) {
        val row = compose.onNodeWithTag(header).getUnclippedBoundsInRoot()
        val node = compose.onNodeWithTag(marker, useUnmergedTree = true).assertIsDisplayed()
        val bounds = node.getUnclippedBoundsInRoot()
        val rowCenter = row.left + (row.right - row.left) / 2
        val width = bounds.right - bounds.left
        val height = bounds.bottom - bounds.top
        if (leading) {
            assertTrue(bounds.right < rowCenter)
            assertEquals(width.value, height.value, 0.5f)
        } else {
            assertTrue(bounds.left > rowCenter)
            assertTrue(height > width)
        }
        val pixels = node.captureToImage().toPixelMap()
        assertEquals(color, pixels[pixels.width / 2, pixels.height / 2])
    }
}
