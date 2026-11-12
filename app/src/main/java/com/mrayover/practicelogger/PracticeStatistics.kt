package com.mrayover.practicelogger

import java.time.LocalDate
import java.time.ZoneId

/** One row per persisted placement or completed standalone activity. All joins are to unique keys.
 * Session start labels placement dates: we cannot reconstruct midnight splits from activeMs.
 * Never join current goal_blocks, Routines, weekly plans or note collections into time totals.
 */
internal const val PRACTICE_FACTS_SQL = """
    SELECT 'placement:' || p.id AS id, p.id AS placementId, p.sessionId,
        p.sourceBlockId AS blockId, p.name AS name, s.startedAt, c.completedAt,
        p.activeMs, p.outcome, s.status AS sessionStatus,
        CASE WHEN snap.placementId IS NOT NULL THEN snap.goalId ELSE sg.goalId END AS goalId,
        CASE WHEN snap.placementId IS NOT NULL THEN
            CASE WHEN snap.goalId IS NULL THEN 'NO_GOAL' ELSE 'SNAPSHOT' END
            WHEN sg.goalId IS NOT NULL THEN 'LEGACY_SESSION' ELSE 'UNKNOWN' END AS attribution,
        r.security, p.note
    FROM placements p JOIN sessions s ON s.id = p.sessionId
    LEFT JOIN placement_goal_snapshots snap ON snap.placementId = p.id
    LEFT JOIN session_goals sg ON sg.sessionId = p.sessionId
    LEFT JOIN block_completions c ON c.placementId = p.id
    LEFT JOIN block_reflections r ON r.placementId = p.id
    WHERE s.status != 'DRAFT'
    UNION ALL
    SELECT 'transcribe:' || id, NULL, NULL, NULL, 'Standalone Transcribe', startedAt,
        completedAt, activeMs, 'COMPLETED', 'COMPLETED', goalId,
        CASE WHEN goalId IS NULL THEN 'NO_GOAL' ELSE 'EXPLICIT' END, NULL, ''
    FROM transcribe_activities WHERE completedAt IS NOT NULL
"""

data class PracticeFact(
    val id: String, val placementId: String?, val sessionId: String?, val blockId: String?,
    val name: String, val startedAt: Long, val completedAt: Long?, val activeMs: Long,
    val outcome: String, val sessionStatus: String, val goalId: String?, val attribution: String,
    val security: Int?, val note: String
)

internal fun statisticsWeekMs(facts: List<PracticeFact>, date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Long =
    weeklyPracticeMs(facts.map { PracticeTimeEntry(it.startedAt, it.activeMs) }, logWeek(date), zone)

internal fun practiceOutcome(fact: PracticeFact): String = when (fact.outcome) {
    "SKIPPED" -> if (fact.activeMs > 0) "Skipped after practice" else "Skipped without practice"
    "COMPLETED" -> "Completed"
    else -> "Unfinished"
}
