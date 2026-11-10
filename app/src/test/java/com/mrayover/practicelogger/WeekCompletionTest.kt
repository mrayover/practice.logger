package com.mrayover.practicelogger

import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class WeekCompletionTest {
    @Test fun latestTimestampedOccurrenceWinsWithinLocalWeek() {
        val week = LocalDate.of(2026, 11, 1)
        val zone = ZoneId.of("America/Los_Angeles")
        val start = week.atStartOfDay(zone).toInstant().toEpochMilli()
        val next = week.plusWeeks(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val first = BlockCompletion("first", "session", "source", start, "goal-a", 1, 1)
        val latest = first.copy(placementId = "latest", completedAt = next - 1,
            goalId = "goal-b", priority = 2, colorArgb = 2)
        val before = first.copy(placementId = "before", sourceBlockId = "previous", completedAt = start - 1)
        val after = first.copy(placementId = "after", sourceBlockId = "next", completedAt = next)
        val legacy = Placement("legacy", "session", "old-source", "Old Block", "Play", null, "", 0,
            60_000, outcome = "COMPLETED")
        val skipped = legacy.copy(id = "skipped", sourceBlockId = "skipped-source", outcome = "SKIPPED")
        val records = SessionBundle(PracticeSession("session", start - 1), listOf(legacy, skipped),
            listOf(latest, after, first, before).map { CompletedPlacement(it, null) })

        assertEquals(mapOf("source" to latest), weekCompletions(listOf(records), week, zone))
        assertEquals(mapOf("next" to after), weekCompletions(listOf(records), week.plusWeeks(1), zone))
    }
}
