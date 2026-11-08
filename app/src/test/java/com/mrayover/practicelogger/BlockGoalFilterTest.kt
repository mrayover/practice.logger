package com.mrayover.practicelogger

import org.junit.Assert.assertEquals
import org.junit.Test

class BlockGoalFilterTest {
    @Test fun matchesOnlyPrimaryGoalAlongsideTextAndArchiveStatus() {
        fun block(id: String, archived: Boolean = false) =
            Block(id, "Block $id", "Play slowly", notes = "From Book_1", archived = archived, createdAt = 0, updatedAt = 0)

        val active = (1..5).map { block("$it") }
        val completed = block("completed")
        val unassigned = block("unassigned")
        val archived = block("archived", archived = true)
        val blocks = active + listOf(completed, unassigned, archived)
        val rows = active.mapIndexed { index, item ->
            val goal = Goal("g${item.id}", "Goal ${item.id}", priority = index, createdAt = 0, updatedAt = 0)
            val legacyLinks = if (index == 0) listOf(GoalBlock(goal.id, unassigned.id, 0), GoalBlock(goal.id, active[1].id, 1)) else emptyList()
            val primary = listOf(BlockPrimaryGoal(item.id, goal.id)) +
                if (index == 0) listOf(BlockPrimaryGoal(archived.id, goal.id)) else emptyList()
            GoalBundle(goal, legacyLinks, primary)
        } + GoalBundle(Goal("done", "Finished", completed = true, createdAt = 0, updatedAt = 0),
            emptyList(), listOf(BlockPrimaryGoal(completed.id, "done")))
        val goals = GoalIndicators(rows)

        active.forEach { item ->
            assertEquals(listOf(item), matchingBlocks(blocks, "  BOOK_1 ", false, goals, BlockGoalFilter.Assigned("g${item.id}")))
        }
        val firstGoal = BlockGoalFilter.Assigned("g1")
        assertEquals(listOf(archived), matchingBlocks(blocks, "slowly", true, goals, firstGoal))
        assertEquals(emptyList<Block>(), matchingBlocks(blocks, "Block 2", false, goals, firstGoal))
        assertEquals(listOf(unassigned), matchingBlocks(blocks, "", false, goals, BlockGoalFilter.NoGoal))
        assertEquals(emptyList<Block>(), matchingBlocks(blocks, "", true, goals, BlockGoalFilter.NoGoal))
        assertEquals(active + listOf(completed, unassigned), matchingBlocks(blocks, "", false, goals, BlockGoalFilter.All))
        assertEquals("Finished · Completed Goal", goals.block(completed.id)?.label)

        val reordered = GoalIndicators(rows.map {
            if (it.goal.id == "g1") it.copy(goal = it.goal.copy(priority = 5)) else it
        })
        assertEquals(listOf("g2", "g3", "g4", "g5", "g1"), reordered.active.map { it.goal.id })
        assertEquals(listOf(active.first()), matchingBlocks(blocks, "", false, reordered, firstGoal))

        val updated = GoalIndicators(rows.map {
            if (it.goal.id == "g1") it.copy(goal = it.goal.copy(completed = true)) else it
        })
        assertEquals(listOf("g2", "g3", "g4", "g5"), updated.active.map { it.goal.id })
        assertEquals(BlockGoalFilter.All, firstGoal.availableIn(updated))
        assertEquals(active + listOf(completed, unassigned), matchingBlocks(blocks, "", false, updated, firstGoal))
        assertEquals(listOf(unassigned), matchingBlocks(blocks, "", false, updated, BlockGoalFilter.NoGoal))
    }
}
