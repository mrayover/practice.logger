package com.mrayover.practicelogger

import androidx.compose.ui.test.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class BlockGoalFilterWorkflowTest : WeekUiChecks() {

    @Test fun filtersAllFourPickersWithoutChangingSelections() {
        val goals = GoalRepository(db)
        val blocks = BlockRepository(db)
        val sessions = SessionRepository(db)
        val (fixture, archived) = runBlocking {
            val a = goals.save(null, "Timing and articulation", "", 0, "")
            val b = goals.save(null, "Tone", "", 0, "")
            val alpha = blocks.save(null, BlockDraft("G2 Alpha", "Use a click", primaryGoalId = a, primaryGoalChanged = true))
            val beta = blocks.save(null, BlockDraft("G2 Beta", "Listen closely", primaryGoalId = b, primaryGoalChanged = true))
            val none = blocks.save(null, BlockDraft("G2 Unassigned", "Play evenly"))
            val old = blocks.save(null, BlockDraft("G2 Archived", "Play evenly", primaryGoalId = a, primaryGoalChanged = true))
            blocks.setArchived(old, true)
            listOf(a, b, alpha, beta, none) to old
        }

        val (goalA, goalB, blockA, blockB, unassigned) = fixture

        click("Blocks")
        choose("Priority 1 · Timing and articulation", "block-library-list")
        libraryBlock(blockA)
        click("Archived", inList = "block-library-list")
        libraryBlock(archived)
        click("Active", inList = "block-library-list")
        runBlocking { goals.move(goalB, -1) }
        click("Goal filter · Priority 2 · Timing and articulation", inList = "block-library-list")
        option("Priority 2 · Timing and articulation").assertIsSelected().performClick()
        libraryBlock(blockA)
        runBlocking { goals.setCompleted(goalA, true) }
        click("Goal filter · All Goals", inList = "block-library-list")
        option("Priority 2 · Timing and articulation").assertDoesNotExist()
        option("No Goal").scrollIfNeeded().performClick()
        libraryBlock(unassigned)
        choose("All Goals", "block-library-list")
        libraryBlock(blockA)
        openBlockPill(compose, "block-library-list", "library-block-$blockA")
        compose.onNodeWithTag("block-library-list").performScrollToNode(hasText("Timing and articulation · Completed Goal"))

        home()
        openHomePractice(compose)
        click("Add Blocks")
        choose("Priority 1 · Tone")
        compose.onNodeWithText("Add G2 Alpha").assertDoesNotExist()
        click("Add G2 Beta")
        shown("1. G2 Beta")
        choose("No Goal")
        compose.onNodeWithText("Add G2 Beta").assertDoesNotExist()
        click("Add G2 Unassigned")
        shown("2. G2 Unassigned")
        val draft = runBlocking { db.sessions().withStatus("DRAFT")!!.id }
        val order = listOf(blockB, unassigned)
        choose("All Goals")
        shown("1. G2 Beta")
        shown("2. G2 Unassigned")
        assertEquals(order, runBlocking { db.sessions().find(draft)!!.ordered.map { it.sourceBlockId } })
        home()
        runBlocking { sessions.discardDraft() }

        val routineId = runBlocking {
            RoutineRepository(db).save(null, "G2 existing routine", listOf(RoutineSelection(blockA, 60_000)))
        }
        openHomeRoutines(compose)
        waitForTag("edit-routine-$routineId")
        compose.onNodeWithTag("edit-routine-$routineId").scrollIfNeeded().performClick()
        shown("1. G2 Alpha")
        choose("Priority 1 · Tone")
        click("Add G2 Beta")
        choose("No Goal")
        shown("2. G2 Beta")
        click("Save Routine")
        shown("G2 existing routine")
        assertEquals(listOf(blockA, blockB), runBlocking { db.routines().find(routineId)!!.ordered.map { it.sourceBlockId } })

        home()
        openHomePractice(compose)
        click("This Week")
        click("Prepare Week", inList = "week-list")
        choose("Priority 1 · Tone", "week-list")
        openBlockPill(compose, "week-list", "available-week-block-$blockB")
        click("Add to Week", inList = "week-list")
        compose.onNodeWithTag("week-list").performScrollToNode(hasText("No matching active Blocks to add."))
        val week = practiceWeek().toString()
        assertEquals(listOf(blockB), runBlocking { db.weekPlan().read(week).map { it.blockId } })
        openBlockPill(compose, "week-list", "week-block-$blockB")
        click("Remove from Week", inList = "week-list")
    }

    private fun choose(option: String, list: String? = null) {
        if (list != null) compose.onNodeWithTag(list).performScrollToNode(hasTestTag("block-goal-filter"))
        compose.onNodeWithTag("block-goal-filter").scrollIfNeeded().performClick()
        option(option).scrollIfNeeded().performClick()
    }

    private fun option(label: String) = compose.onNode(hasText(label) and hasAnyAncestor(isPopup()))

    private fun libraryBlock(id: String) {
        val tag = "library-block-$id"
        compose.onNodeWithTag("block-library-list").performScrollToNode(hasTestTag(tag))
        compose.onNodeWithTag(tag).assertIsDisplayed()
    }

    private fun home() = compose.onNodeWithTag("home-brand", useUnmergedTree = true).performClick()
}
