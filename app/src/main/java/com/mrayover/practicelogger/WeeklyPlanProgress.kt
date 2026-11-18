package com.mrayover.practicelogger

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.ZoneId

internal data class PracticeMinutes(val actualMs: Long, val plannedMinutes: Long) {
    private val actualWholeMinutes get() = actualMs / 60_000 + if (actualMs % 60_000 >= 30_000) 1 else 0
    val label get() = "$actualWholeMinutes / $plannedMinutes min"
    val fill: Float get() = if (plannedMinutes == 0L) 0f
        else (actualMs / (plannedMinutes * 60_000.0)).coerceIn(0.0, 1.0).toFloat()
    val status: String get() {
        if (plannedMinutes == 0L) return "No plan"
        val difference = BigDecimal.valueOf(plannedMinutes).multiply(BigDecimal.valueOf(60_000))
            .subtract(BigDecimal.valueOf(actualMs))
        if (difference.signum() == 0) return "Plan met"
        val whole = difference.abs().divide(BigDecimal.valueOf(60_000), 0, RoundingMode.HALF_UP)
        val minutes = if (whole.signum() == 0) "<1" else whole.toPlainString()
        return if (difference.signum() > 0) "$minutes min left" else "$minutes min over"
    }
}

internal data class WeeklyPlanProgress(val total: PracticeMinutes, val byGoal: Map<String, PracticeMinutes>)

internal fun weeklyPlanProgress(
    plannedByBlock: Map<String, Long>, goals: List<GoalBundle>, facts: List<PracticeFact>,
    week: LocalDate, zone: ZoneId
): WeeklyPlanProgress {
    val interval = practiceWeekInterval(week, zone)
    val actual = facts.filter { it.activeMs > 0 && it.startedAt in interval }
    val actualByGoal = actual.groupBy { it.goalId }
    return WeeklyPlanProgress(
        PracticeMinutes(actual.sumOf { it.activeMs }, plannedByBlock.values.sum()),
        goals.associate { bundle ->
            val goal = bundle.goal.id
            val planned = bundle.blocks.sumOf { plannedByBlock[it.blockId] ?: 0 }
            goal to PracticeMinutes(actualByGoal[goal].orEmpty().sumOf { it.activeMs }, planned)
        }
    )
}
