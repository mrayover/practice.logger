package com.mrayover.practicelogger

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class GoalPriorityWorkflowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun shown(text: String) {
        compose.waitUntil(15000) {
            compose.onAllNodesWithText(text).fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()
        }
    }

    private fun openRow(id: String): SemanticsNodeInteraction {
        val tag = "library-block-$id"
        compose.onNodeWithTag("block-library-list").performScrollToNode(hasTestTag(tag))
        return compose.onNodeWithTag(tag).assertIsDisplayed()
    }

    private fun assertMarker(row: SemanticsNodeInteraction, expected: Color, leading: Boolean) {
        val pixels = row.captureToImage().toPixelMap()
        val edge = if (leading) 0 until pixels.width / 4 else pixels.width * 3 / 4 until pixels.width
        assertTrue("Missing Goal marker fill $expected",
            edge.any { x ->
                (pixels.height / 3 until pixels.height * 2 / 3).any { y -> pixels[x, y] == expected }
            })
    }

    @Test fun editorAndLibraryShowCurrentPriorityThroughReorderCompletionAndThemes() {
        val context = compose.activity.applicationContext
        val db = BlockDatabase.appInstance(context)
        val goals = GoalRepository(db)
        val (block, ids) = runBlocking {
            val id = BlockRepository(db).save(null, BlockDraft("Priority study", "Play a melody"))
            id to ('A'..'E').map { goals.save(null, "Goal $it", "", 0, "") }
        }
        shown("Blocks")
        compose.onNodeWithText("Blocks").scrollIfNeeded().performClick()
        shown("Block Library")
        assertMarker(openRow(block), Color.Black, leading = false)
        openLibraryBlockEditor(compose, "Priority study")
        compose.onNodeWithTag("primary-goal-picker").performScrollTo().performClick()
        shown("Priority 3 · Goal C")
        compose.onNodeWithText("Priority 3 · Goal C").scrollIfNeeded().performClick()
        compose.onNodeWithText("Save").performScrollTo().performClick()
        shown("Block Library")
        openBlockPill(compose, "block-library-list", "library-block-$block")
        shown("Priority 3 · Goal C")
        assertMarker(openRow(block), LightPalette.colors.goalPriorities[2], leading = true)

        runBlocking { goals.move(ids[2], -1); goals.move(ids[2], -1) }
        shown("Priority 1 · Goal C")
        assertMarker(openRow(block), LightPalette.colors.goalPriorities[0], leading = true)
        runBlocking { goals.setCompleted(ids[2], true) }
        shown("Goal C · Completed Goal")
        runBlocking { goals.setCompleted(ids[2], false) }
        shown("Priority 5 · Goal C")

        for (theme in ThemeChoice.entries) {
            ThemePreferences(context).save(theme)
            compose.activityRule.scenario.recreate()
            shown("Block Library")
            assertMarker(openRow(block), LightPalette.colors.goalPriorities[4], leading = true)
            openBlockPill(compose, "block-library-list", "library-block-$block")
            shown("Priority 5 · Goal C")
        }
        captureStage3("goal-priority-library")
        openLibraryBlockEditor(compose, "Priority study")
        compose.onNodeWithTag("primary-goal-picker").performScrollTo().performClick()
        shown("No Goal")
        compose.onNodeWithText("No Goal").scrollIfNeeded().performClick()
        compose.onNodeWithText("Save").performScrollTo().performClick()
        shown("Block Library")
        assertMarker(openRow(block), Color.Black, leading = false)
        runBlocking {
            assertNull(db.goalAttribution().primaryGoalId(block))
            assertTrue(db.goals().find(ids[2])!!.blocks.any { it.blockId == block })
        }
    }
}
