package com.mrayover.practicelogger

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.ceil
import kotlin.math.floor
import org.json.JSONObject

internal const val LGE2_POLICY = "LGE2_INTERVAL_V1"
internal const val RETURN_DAY_MS = 86_400_000L

internal data class LearningInputs(val observations: List<LearningObservation>, val events: List<GuidanceEvent>,
    val facts: List<PracticeFact>, val practices: List<ReturnPractice>, val goals: List<GoalBundle>)

/** Exact endpoint identity/timestamps, independent of G3 time attribution and confidence. */
data class ReturnPractice(val placementId: String, val sessionId: String, val blockId: String,
    val startedAt: Long, val endedAt: Long?, val sessionStatus: String, val activeMs: Long, val outcome: String,
    val completedAt: Long?, val completionSessionId: String?, val completionBlockId: String?)

/** Pure policy inputs are persisted facts, never confidence ratings or inferred practice time. */
internal data class ReturnSuggestion(
    val blockId: String, val baseDate: String, val intervalDays: Int, val reason: String,
    val observationId: String? = null, val anchorId: String? = null,
    val criterionToken: String? = null, val criterion: String? = null,
    val elapsedMs: Long? = null, val outcome: String? = null,
    val insufficient: String? = null
) {
    val dueDate: String get() = LocalDate.parse(baseDate).plusDays(intervalDays.toLong()).toString()
    val explanation: String get() {
        val delay = elapsedMs?.let { java.math.BigDecimal.valueOf(it).divide(java.math.BigDecimal.valueOf(RETURN_DAY_MS), 2, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() }
        return when (reason) {
            "HELD" -> "You met this target on your first attempt after $delay days. Consider returning in $intervalDays days."
            "PARTIAL" -> "This target only partially held up after $delay days. Consider returning in $intervalDays days."
            "NOT_YET" -> "This target didn't hold up yet after $delay days. Consider returning in $intervalDays days."
            else -> "There isn't enough comparable return evidence yet. Three days is a provisional starting suggestion. $insufficient"
        }
    }
    fun provenance(): String = JSONObject().put("policy", LGE2_POLICY).put("accepted", true)
        .put("blockId", blockId).put("reason", reason).put("baseDate", baseDate)
        .put("intervalDays", intervalDays).put("dueDate", dueDate)
        .put("observationId", observationId ?: JSONObject.NULL).put("anchorId", anchorId ?: JSONObject.NULL)
        .put("criterionToken", criterionToken ?: JSONObject.NULL).put("criterion", criterion ?: JSONObject.NULL)
        .put("elapsedMs", elapsedMs ?: JSONObject.NULL).put("outcome", outcome ?: JSONObject.NULL)
        .put("insufficient", insufficient ?: JSONObject.NULL).toString()
}

internal fun adjustedReturnDays(elapsedMs: Long, outcome: String): Int {
    require(elapsedMs >= RETURN_DAY_MS)
    val days = elapsedMs.toDouble() / RETURN_DAY_MS
    return when (outcome) {
        "HELD" -> ceil(days * 1.5)
        "PARTIAL" -> floor(days * .75)
        "NOT_YET" -> floor(days * .5)
        else -> error("Unassessed returns cannot adapt an interval.")
    }.coerceIn(1.0, 30.0).toInt()
}

/** The explicit anchor must also be the last known practice endpoint for this Block.
 * Unlocated practice is deliberately disqualifying, even if another endpoint looks usable.
 */
internal fun returnSuggestion(blockId: String, observations: List<LearningObservation>, facts: List<ReturnPractice>,
    today: LocalDate, now: Long, observationId: String? = null): ReturnSuggestion {
    val blockFacts = facts.filter { it.blockId == blockId }
    val byPlacement = blockFacts.associateBy { it.placementId }
    val history = observations.filter { it.placementId in byPlacement }
    val o = if (observationId == null) history.sortedWith(compareByDescending<LearningObservation> { it.observedAt }.thenBy { it.placementId }).firstOrNull()
        else history.find { it.placementId == observationId }
    fun insufficient(detail: String) = ReturnSuggestion(blockId, today.toString(), 3, "STARTING",
        o?.placementId, criterionToken = o?.criterionToken, criterion = o?.criterion, insufficient = detail)
    if (o == null) return insufficient("No assessment is required to practice or set a manual revisit.")
    val p = byPlacement[o.placementId] ?: return insufficient("The observation's practice placement is unavailable.")
    if (o.context != "FIRST_ATTEMPT" || o.outcome !in listOf("HELD", "PARTIAL", "NOT_YET"))
        return insufficient("A musician-confirmed first attempt with an assessed outcome is needed.")
    val anchor = history.find { it.placementId == o.anchorId }
        ?: return insufficient("An earlier comparable observation must be explicitly confirmed.")
    val a = byPlacement[anchor.placementId] ?: return insufficient("The earlier practice endpoint is unavailable.")
    if (o.criterionToken == null || anchor.criterionToken != o.criterionToken || anchor.criterion != o.criterion ||
        anchor.context != "FIRST_ATTEMPT" || anchor.outcome !in listOf("HELD", "PARTIAL", "NOT_YET"))
        return insufficient("The earlier criterion or first-attempt context is not comparable in its current corrected state.")
    if (a.sessionId == p.sessionId) return insufficient("Repeated Blocks in one session are not delayed returns.")
    val end = a.completedAt
    val laterEnd = p.completedAt
    if (a.sessionStatus != "COMPLETED" || p.sessionStatus != "COMPLETED" || a.outcome != "COMPLETED" || p.outcome != "COMPLETED" ||
        a.activeMs <= 0 || p.activeMs <= 0 || end == null || laterEnd == null)
        return insufficient("Both practice sessions need recorded completions and positive actual practice time; skipped or unfinished work cannot reinforce an interval.")
    if (a.completionSessionId != a.sessionId || p.completionSessionId != p.sessionId ||
        a.completionBlockId != blockId || p.completionBlockId != blockId || a.endedAt == null || p.endedAt == null ||
        end > a.endedAt || laterEnd > p.endedAt || a.endedAt > now || p.endedAt > now ||
        a.startedAt < 0 || p.startedAt < 0 || anchor.observedAt < a.startedAt || anchor.observedAt > end ||
        end > o.observedAt || o.observedAt < p.startedAt || o.observedAt > laterEnd || laterEnd > now ||
        (o.correctedAt ?: o.observedAt) > now || (anchor.correctedAt ?: anchor.observedAt) > now || p.startedAt < end)
        return insufficient("Recorded timestamps are contradictory or in the future. Check the device clock.")
    if (blockFacts.any { f -> f.placementId != a.placementId && f.placementId != p.placementId && f.activeMs > 0 &&
            f.startedAt <= o.observedAt && (f.completedAt == null || f.completedAt > end || f.completedAt < f.startedAt) })
        return insufficient("Other recorded practice intervenes, or its endpoint cannot be located.")
    val delay = o.observedAt - end
    if (delay < RETURN_DAY_MS) return insufficient("The return was earlier than the minimum 24-hour delay.")
    return ReturnSuggestion(blockId, today.toString(), adjustedReturnDays(delay, o.outcome), o.outcome,
        o.placementId, anchor.placementId, o.criterionToken, o.criterion, delay, o.outcome)
}

/** Strict, bounded snapshot validation. Corrections must not rewrite earlier accepted decisions. */
internal fun readAcceptedSuggestion(raw: String): ReturnSuggestion {
    require(raw.length <= 4096)
    val j = JSONObject(raw)
    require(j.keys().asSequence().toSet() == setOf("policy", "accepted", "blockId", "reason", "baseDate", "intervalDays", "dueDate",
        "observationId", "anchorId", "criterionToken", "criterion", "elapsedMs", "outcome", "insufficient"))
    require(j.get("policy") == LGE2_POLICY && j.get("accepted") == true)
    fun text(key: String, limit: Int = 80): String? = if (j.isNull(key)) null else (j.get(key) as String).also { require(it.isNotBlank() && it.length <= limit) }
    val interval = j.get("intervalDays").also { require(it is Int && it in 1..30) } as Int
    val elapsed = if (j.isNull("elapsedMs")) null else j.get("elapsedMs").also { require(it is Int || it is Long) }.let { (it as Number).toLong() }
    val s = ReturnSuggestion(requireNotNull(text("blockId")), canonicalLearningDate(requireNotNull(text("baseDate"))).toString(), interval,
        requireNotNull(text("reason")), text("observationId"), text("anchorId"), text("criterionToken"), text("criterion", LEARNING_TEXT_LIMIT),
        elapsed, text("outcome"), text("insufficient", 500))
    require(s.dueDate == j.get("dueDate") && canonicalLearningDate(s.dueDate).toString() == s.dueDate)
    require((s.criterionToken == null) == (s.criterion == null))
    if (s.reason == "STARTING") require(s.intervalDays == 3 && s.anchorId == null && s.elapsedMs == null && s.outcome == null && s.insufficient != null)
    else require(s.reason in listOf("HELD", "PARTIAL", "NOT_YET") && s.reason == s.outcome && s.observationId != null &&
        s.anchorId != null && s.anchorId != s.observationId && s.criterionToken != null && s.insufficient == null &&
        s.elapsedMs != null && adjustedReturnDays(s.elapsedMs, s.reason) == s.intervalDays)
    return s
}

internal data class BlockRecommendation(val blockId: String, val reasons: List<String>, val tier: Int,
    val order: Long, val goalPriority: Int, val title: String, val blockOrder: Int)

internal fun recommendedBlocks(blocks: List<Block>, goals: List<GoalBundle>, events: List<GuidanceEvent>,
    observations: List<LearningObservation>, practices: List<ReturnPractice>, facts: List<PracticeFact>, today: LocalDate, now: Long,
    zone: ZoneId): List<BlockRecommendation> {
    val indicators = GoalIndicators(goals)
    val active = activeGoals(goals)
    val practicesByBlock = practices.groupBy { it.blockId }
    val blockByPlacement = practices.associate { it.placementId to it.blockId }
    val observationsByBlock = observations.groupBy { blockByPlacement[it.placementId] }
    val eventsByBlock = events.groupBy { it.blockId }
    // Previous seven complete local dates. G3 retains its own unchanged accounting rules.
    val attended = facts.filter { it.activeMs > 0 && it.attribution in listOf("SNAPSHOT", "LEGACY_SESSION", "EXPLICIT") }.filter {
        val date = Instant.ofEpochMilli(it.startedAt).atZone(zone).toLocalDate()
        date >= today.minusDays(7) && date < today
    }.mapNotNullTo(hashSetOf()) { it.goalId }
    return blocks.distinctBy { it.id }.filter { !it.archived }.mapIndexedNotNull { index, b ->
        val history = eventsByBlock[b.id].orEmpty().sortedByDescending { it.revision }
        val state = history.firstOrNull()
        val flag = if (state?.returnSoon == true) history.firstOrNull { it.action == "FLAG_SET" } else null
        val due = state?.dueDate?.let(LocalDate::parse)?.takeIf { it <= today }
        val blockObservations = observationsByBlock[b.id].orEmpty()
        val blockPractices = practicesByBlock[b.id].orEmpty()
        val difficulty = blockObservations.filter { o ->
            val date = Instant.ofEpochMilli(o.observedAt).atZone(zone).toLocalDate()
            date >= today.minusDays(13) && date <= today && o.outcome in listOf("PARTIAL", "NOT_YET")
        }.sortedWith(compareByDescending<LearningObservation> { it.observedAt }.thenBy { it.placementId }).firstOrNull {
            returnSuggestion(b.id, blockObservations, blockPractices, today, now, it.placementId).elapsedMs != null
        }
        val goal = active.firstOrNull { row -> row.goal.id !in attended && row.blocks.any { it.blockId == b.id } }
        val reasons = buildList {
            if (flag != null) add("Return Soon · chosen by you")
            if (due != null) add("Revisit date: $due")
            if (difficulty != null) add("Recent comparable return: " + if (difficulty.outcome == "PARTIAL") "partially held up" else "didn't hold up yet")
            if (goal != null) add("${goal.goal.title}: No recorded practice this week (previous seven days)")
        }
        if (reasons.isEmpty()) null else {
            val priority = indicators.block(b.id)?.takeUnless { it.goal.completed }?.priority ?: Int.MAX_VALUE
            when {
                flag != null -> BlockRecommendation(b.id, reasons, 0, -flag.recordedAt, priority, b.name, index)
                due != null -> BlockRecommendation(b.id, reasons, 1, due.toEpochDay(), priority, b.name, index)
                difficulty != null -> BlockRecommendation(b.id, reasons, 2, -difficulty.observedAt, priority, b.name, index)
                else -> BlockRecommendation(b.id, reasons, 3, active.indexOf(goal).toLong(), priority, b.name, index)
            }
        }
    }.sortedWith { a, b ->
        compareValues(a.tier, b.tier).takeIf { it != 0 } ?: when (a.tier) {
            0 -> compareValues(a.order, b.order).takeIf { it != 0 } ?: compareValues(a.goalPriority, b.goalPriority).takeIf { it != 0 }
                ?: compareValues(a.title, b.title).takeIf { it != 0 } ?: compareValues(a.blockId, b.blockId)
            1 -> compareValues(a.order, b.order).takeIf { it != 0 } ?: compareValues(a.goalPriority, b.goalPriority).takeIf { it != 0 } ?: compareValues(a.blockId, b.blockId)
            2 -> compareValues(a.order, b.order).takeIf { it != 0 } ?: compareValues(a.blockId, b.blockId)
            else -> compareValues(a.order, b.order).takeIf { it != 0 } ?: compareValues(a.blockOrder, b.blockOrder).takeIf { it != 0 } ?: compareValues(a.blockId, b.blockId)
        }
    }
}
