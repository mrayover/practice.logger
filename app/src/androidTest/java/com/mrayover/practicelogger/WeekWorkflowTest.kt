package com.mrayover.practicelogger

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class WeekWorkflowTest : WeekUiChecks() {

    @Test fun preparePracticeAndBuild() {
        val existing = runBlocking { BlockRepository(db).save(null, BlockDraft("Weekly scales", "Play slowly")) }
        val current = practiceWeek()
        val previous = current.minusWeeks(1)

        shown("Blocks")
        click("Blocks")
        click("Add to This Week", inList = "block-library-list")
        shown("In This Week")
        compose.onNodeWithTag("block-library-list").performScrollToNode(hasTestTag("library-block-$existing"))
        compose.onNodeWithTag("library-block-$existing").performClick()
        shown("Edit Block")
        shown("In This Week")
        goHome()

        openHomePractice(compose)
        click("This Week")
        click("Previous week", inList = "week-list")
        shown("No Blocks assigned to this week.")
        click("Prepare Week", inList = "week-list")
        click("Add Weekly scales", inList = "week-list")
        click("Create a new Block", inList = "week-list")
        compose.onNodeWithText("Name").scrollIfNeeded().performTextInput("Weekly chords")
        compose.onNodeWithText("Action").scrollIfNeeded().performTextInput("Change cleanly")
        click("Save")
        click("Done", inList = "week-list")
        val created = runBlocking {
            val rows = db.weekPlan().read(previous.toString())
            assertEquals(2, rows.size)
            assertEquals(existing, rows.first().blockId)
            rows.last().blockId
        }
        click("Next week", inList = "week-list")
        click("Prepare Week", inList = "week-list")
        click("Carry forward", inList = "week-list")
        compose.onNodeWithTag("carry-week-$previous").performClick()
        click("Remove Weekly scales from week", inList = "week-list")
        click("Add Weekly scales", inList = "week-list")
        click("Done", inList = "week-list")

        assertEquals(listOf(created, existing), runBlocking { db.weekPlan().read(current.toString()).map { it.blockId } })
        compose.activityRule.scenario.recreate()
        click("Select Blocks", inList = "week-list")
        clickTag("select-week-$existing")
        clickTag("select-week-$created")
        click("Build session (2)", inList = "week-list")
        shown("Build a Session")
        shown("1. Weekly chords")
        shown("2. Weekly scales")
        click("Start Session")
        shown("Weekly chords")
        compose.onNodeWithText("Start").assertExists()
        leavePracticeForLater(compose)

        openHomePractice(compose)
        click("This Week")
        clickTag("practice-week-$existing")
        shown("Weekly scales")
        compose.onNodeWithText("Start").assertExists()
        leavePracticeForLater(compose)
        openHomePractice(compose)
        click("In Progress")
        shown("Unfinished sessions")
        shown("Weekly scales")
        shown("Weekly chords")
        assertEquals(listOf(1, 2), runBlocking {
            db.sessions().readAll().filter { it.session.status == "IN_PROGRESS" }.map { it.ordered.size }.sorted()
        })
    }

    private fun goHome() = compose.onNodeWithTag("home-brand", useUnmergedTree = true).performClick()
}

class WeekReopenTest : WeekUiChecks() {
    @Test fun persistsAfterProcessRestart() {
        openHomePractice(compose)
        click("This Week")
        shown("Weekly chords")
        shown("Weekly scales")
        val current = practiceWeek()
        assertEquals(listOf("Weekly chords", "Weekly scales"), runBlocking {
            db.weekPlan().read(current.toString()).map { db.blocks().find(it.blockId)!!.name }
        })
        click("Previous week", inList = "week-list")
        assertEquals(listOf("Weekly scales", "Weekly chords"), runBlocking {
            db.weekPlan().read(current.minusWeeks(1).toString()).map { db.blocks().find(it.blockId)!!.name }
        })
    }

}

abstract class WeekUiChecks {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    protected val db get() = BlockDatabase.appInstance(compose.activity.applicationContext)

    protected fun shown(label: String) {
        compose.waitUntil(15_000) {
            compose.onAllNodesWithText(label).fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()
        }
    }
    protected fun click(label: String, inList: String? = null) {
        if (inList != null) {
            compose.onNodeWithTag(inList).performScrollToNode(hasText(label))
            compose.onNodeWithText(label).assertIsDisplayed().performClick()
        } else {
            shown(label)
            compose.onNodeWithText(label).scrollIfNeeded().performClick()
        }
    }
    protected fun clickTag(tag: String) {
        compose.onNodeWithTag("week-list").performScrollToNode(hasTestTag(tag))
        compose.onNodeWithTag(tag).assertIsDisplayed().performClick()
    }
}
