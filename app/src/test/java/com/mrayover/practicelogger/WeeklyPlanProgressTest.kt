package com.mrayover.practicelogger

import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class WeeklyPlanProgressTest {
    private val sunday = LocalDate.of(2026, 3, 8)
    private val zone = ZoneId.of("America/Los_Angeles")
    private val interval = practiceWeekInterval(sunday, zone)

    @Test fun oneWeeklyTotalWithLinkedPlansAndHistoricalActuals() {
        val goals = listOf(
            goal("original"), goal("current", "shared"), goal("other", "shared", "extra")
        )
        val facts = listOf(
            fact("legacy", interval.first, 45, "original"),
            fact("transcribe", interval.last, 20, "current"),
            fact("unassigned", interval.first + 1, 31),
            fact("previous", interval.first - 1, 50, "current"),
            fact("next", interval.last + 1, 60, "current")
        )
        val progress = weeklyPlanProgress(mapOf("shared" to 120L, "extra" to 60L), goals, facts, sunday, zone)
        assertEquals("96 / 180 min", progress.total.label)
        assertEquals("84 min left", progress.total.status)
        assertEquals("45 / 0 min", progress.byGoal.getValue("original").label)
        assertEquals("20 / 120 min", progress.byGoal.getValue("current").label)
        assertEquals("0 / 180 min", progress.byGoal.getValue("other").label)
        val next = weeklyPlanProgress(emptyMap(), goals, facts, sunday.plusWeeks(1), zone)
        assertEquals("60 / 0 min", next.total.label)
        assertEquals("60 / 0 min", next.byGoal.getValue("current").label)
    }

    @Test fun roundingKeepsTimePrecisionAndCapsOnlyTheBar() {
        val empty = PracticeMinutes(95 * 60_000 + 42_600, 0)
        assertEquals("96 / 0 min", empty.label)
        assertEquals("No plan", empty.status)
        assertEquals(0f, empty.fill, 0f)
        assertEquals(0f, PracticeMinutes(0, 120).fill, 0f)
        assertEquals("Plan met", PracticeMinutes(120 * 60_000, 120).status)
        assertEquals("<1 min left", PracticeMinutes(120 * 60_000 - 1, 120).status)
        assertEquals("<1 min over", PracticeMinutes(120 * 60_000 + 1, 120).status)
        val over = PracticeMinutes(155 * 60_000, 120)
        assertEquals("155 / 120 min", over.label)
        assertEquals("35 min over", over.status)
        assertEquals(1f, over.fill, 0f)
    }

    private fun goal(id: String, vararg blocks: String) = GoalBundle(
        Goal(id, id, createdAt = 1, updatedAt = 1),
        blocks.mapIndexed { index, block -> GoalBlock(id, block, index) }
    )

    private fun fact(id: String, startedAt: Long, minutes: Long, goal: String? = null) =
        PracticeFact(id, null, null, null, id, startedAt, null, minutes * 60_000, "COMPLETED",
            "COMPLETED", goal, if (goal == null) "NO_GOAL" else "SNAPSHOT", null, "")
}
