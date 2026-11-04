package com.mrayover.practicelogger

import java.time.LocalDate
import java.time.ZoneId

internal fun weeklyPracticeMs(
    entries: List<PracticeTimeEntry>,
    monday: LocalDate,
    zone: ZoneId = ZoneId.systemDefault()
): Long {
    val from = monday.atStartOfDay(zone).toInstant().toEpochMilli()
    val end = monday.plusWeeks(1).atStartOfDay(zone).toInstant().toEpochMilli()
    return entries.filter { it.startedAt in from until end }.sumOf { it.activeMs }
}

internal fun practiceDuration(ms: Long): String = "${ms / 60_000} min ${ms / 1_000 % 60} sec"
