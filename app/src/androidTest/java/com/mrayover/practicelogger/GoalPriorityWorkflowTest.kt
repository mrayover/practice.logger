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

    private fun assertCap(row: SemanticsNodeInteraction, expected: Color) {
        val pixels = row.captureToImage().toPixelMap()
        assertTrue("Missing trailing cap fill $expected",
            (pixels.width * 3 / 4 until pixels.width).any { x ->
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
        assertCap(openRow(block), Color.Black)
        openRow(block).performClick()
        compose.onNodeWithTag("primary-goal-picker").performScrollTo().performClick()
        shown("Priority 3 · Goal C")
        compose.onNodeWithText("Priority 3 · Goal C").scrollIfNeeded().performClick()
        compose.onNodeWithText("Save").performScrollTo().performClick()
        shown("Block Library")
        openRow(block).assertTextContains("Priority 3 · Goal C")
        assertCap(openRow(block), LightPalette.colors.goalPriorities[2])

        runBlocking { goals.move(ids[2], -1); goals.move(ids[2], -1) }
        shown("Priority 1 · Goal C")
        assertCap(openRow(block), LightPalette.colors.goalPriorities[0])
        runBlocking { goals.setCompleted(ids[2], true) }
        shown("Goal C · Completed Goal")
        openRow(block).assertTextContains("Goal C · Completed Goal")
        runBlocking { goals.setCompleted(ids[2], false) }
        shown("Priority 5 · Goal C")

        for (theme in ThemeChoice.entries) {
            ThemePreferences(context).save(theme)
            compose.activityRule.scenario.recreate()
            shown("Block Library")
            assertCap(openRow(block), LightPalette.colors.goalPriorities[4])
            openRow(block).assertTextContains("Priority 5 · Goal C")
        }
        captureStage3("goal-priority-library")
        openRow(block).performClick()
        compose.onNodeWithTag("primary-goal-picker").performScrollTo().performClick()
        shown("No Goal")
        compose.onNodeWithText("No Goal").scrollIfNeeded().performClick()
        compose.onNodeWithText("Save").performScrollTo().performClick()
        shown("Block Library")
        assertCap(openRow(block), Color.Black)
        runBlocking {
            assertNull(db.goalAttribution().primaryGoalId(block))
            assertTrue(db.goals().find(ids[2])!!.blocks.any { it.blockId == block })
        }
    }
}
