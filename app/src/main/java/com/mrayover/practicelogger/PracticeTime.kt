package com.mrayover.practicelogger

import java.time.LocalDate
import java.time.ZoneId

internal fun weeklyPracticeMs(
    entries: List<PracticeTimeEntry>,
    weekStart: LocalDate,
    zone: ZoneId = ZoneId.systemDefault()
): Long {
    val interval = practiceWeekInterval(weekStart, zone)
    return entries.filter { it.startedAt in interval }.sumOf { it.activeMs }
}

internal fun practiceWeekInterval(weekStart: LocalDate, zone: ZoneId): LongRange =
    weekStart.atStartOfDay(zone).toInstant().toEpochMilli() until
        weekStart.plusWeeks(1).atStartOfDay(zone).toInstant().toEpochMilli()

internal fun practiceDuration(ms: Long): String = "${ms / 60_000} min ${ms / 1_000 % 60} sec"
