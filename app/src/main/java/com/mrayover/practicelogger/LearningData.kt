package com.mrayover.practicelogger

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.ZoneId
import java.time.LocalDate
import java.util.UUID

internal val learningOutcomes = listOf("HELD", "PARTIAL", "NOT_YET", "NOT_ASSESSED")
internal val learningContexts = listOf("FIRST_ATTEMPT", "LATER_RECOLLECTION", "UNKNOWN")
internal const val LEARNING_TEXT_LIMIT = 1000

@Entity(tableName = "learning_observations", foreignKeys = [
    ForeignKey(entity = Placement::class, parentColumns = ["id"], childColumns = ["placementId"], onDelete = ForeignKey.RESTRICT),
    ForeignKey(entity = LearningObservation::class, parentColumns = ["placementId"], childColumns = ["anchorId"], onDelete = ForeignKey.RESTRICT)
], indices = [Index("anchorId")])
data class LearningObservation(
    @PrimaryKey val placementId: String,
    val outcome: String,
    val observedAt: Long,
    val context: String,
    val criterionToken: String? = null,
    val criterion: String? = null,
    val anchorId: String? = null,
    val correctedAt: Long? = null
)

@Entity(tableName = "guidance_events", foreignKeys = [
    ForeignKey(entity = Block::class, parentColumns = ["id"], childColumns = ["blockId"], onDelete = ForeignKey.RESTRICT)
], indices = [Index(value = ["blockId", "revision"], unique = true)])
data class GuidanceEvent(
    @PrimaryKey val id: String,
    val blockId: String,
    val revision: Long,
    val recordedAt: Long,
    val action: String,
    val returnSoon: Boolean,
    val dueDate: String?,
    val baseDate: String?,
    val intervalDays: Int?,
    val origin: String,
    // Immutable accepted scheduling provenance; historical manual records remain unchanged.
    val offeredSuggestion: String? = null
)

@Dao interface LearningDao {
    @Query("SELECT * FROM learning_observations") suspend fun allObservations(): List<LearningObservation>
    @Query("SELECT * FROM guidance_events") suspend fun allEvents(): List<GuidanceEvent>
    @Query(PRACTICE_FACTS_SQL) suspend fun facts(): List<PracticeFact>
    @Query("""SELECT p.id AS placementId,p.sessionId,p.sourceBlockId AS blockId,s.startedAt,s.endedAt,s.status AS sessionStatus,
        p.activeMs,p.outcome,c.completedAt,c.sessionId AS completionSessionId,c.sourceBlockId AS completionBlockId
        FROM placements p JOIN sessions s ON s.id=p.sessionId LEFT JOIN block_completions c ON c.placementId=p.id
        WHERE s.status != 'DRAFT'""")
    suspend fun practices(): List<ReturnPractice>
    @Query("SELECT * FROM learning_observations WHERE placementId = :id")
    suspend fun observation(id: String): LearningObservation?
    @Query("SELECT o.* FROM learning_observations o JOIN placements p ON p.id=o.placementId WHERE p.sourceBlockId=:blockId ORDER BY o.observedAt DESC,o.placementId")
    fun observeHistory(blockId: String): Flow<List<LearningObservation>>
    @Query("SELECT o.* FROM learning_observations o JOIN placements p ON p.id=o.placementId WHERE p.sourceBlockId=:blockId ORDER BY o.observedAt DESC,o.placementId")
    suspend fun history(blockId: String): List<LearningObservation>
    @Query("SELECT * FROM guidance_events WHERE blockId=:blockId ORDER BY revision DESC")
    fun observeEvents(blockId: String): Flow<List<GuidanceEvent>>
    @Query("SELECT * FROM guidance_events WHERE blockId=:blockId ORDER BY revision DESC")
    suspend fun events(blockId: String): List<GuidanceEvent>
    @Query("SELECT * FROM guidance_events WHERE id=:id")
    suspend fun event(id: String): GuidanceEvent?
    @Insert suspend fun insert(observation: LearningObservation)
    @Update suspend fun update(observation: LearningObservation)
    @Insert suspend fun insert(event: GuidanceEvent)
    @Query("SELECT EXISTS(SELECT 1 FROM learning_observations) OR EXISTS(SELECT 1 FROM guidance_events)")
    suspend fun hasEntries(): Boolean
}

internal fun validateLearningObservation(o: LearningObservation) {
    require(o.placementId.isNotBlank() && o.outcome in learningOutcomes && o.context in learningContexts)
    require(o.observedAt >= 0 && (o.correctedAt == null || o.correctedAt >= o.observedAt))
    require((o.criterionToken == null) == (o.criterion == null))
    require(o.criterionToken == null || o.criterionToken.isNotBlank() && o.criterionToken.length <= 80)
    require(o.criterion == null || o.criterion.isNotBlank() && o.criterion.length <= LEARNING_TEXT_LIMIT)
    require(o.anchorId == null || o.anchorId.isNotBlank() && o.anchorId != o.placementId && o.criterionToken != null)
}

internal fun canonicalLearningDate(value: String): LocalDate =
    LocalDate.parse(value).also { require(it.toString() == value && it.year in 1..9999) }

internal fun validateGuidanceEvent(e: GuidanceEvent, adaptive: Boolean = true) {
    require(e.id.isNotBlank() && e.id.length <= 80 && e.blockId.isNotBlank() && e.revision > 0 && e.recordedAt >= 0)
    require(e.action in listOf("FLAG_SET", "FLAG_CLEAR", "DATE_SET", "DATE_CLEAR", "DECLINED"))
    if (!adaptive) require(e.offeredSuggestion == null) { "Suggestions are not supported in ZIP9." }
    if (e.dueDate == null) {
        require(e.baseDate == null && e.intervalDays == null && e.origin == "NONE" && e.offeredSuggestion == null)
    } else {
        val due = canonicalLearningDate(e.dueDate)
        val base = canonicalLearningDate(requireNotNull(e.baseDate))
        require(e.intervalDays != null && e.intervalDays in 0..36500 && base.plusDays(e.intervalDays.toLong()) == due)
        if (e.origin == "MANUAL") require(e.offeredSuggestion == null)
        else {
            require(adaptive && e.origin == "ACCEPTED_SUGGESTION")
            val s = readAcceptedSuggestion(requireNotNull(e.offeredSuggestion))
            require(s.blockId == e.blockId && s.baseDate == e.baseDate && s.intervalDays == e.intervalDays && s.dueDate == e.dueDate)
        }
    }
}

class LearningRepository(private val db: BlockDatabase, private val now: () -> Long = System::currentTimeMillis) {
    private val dao get() = db.learning()

    internal suspend fun inputs(): LearningInputs = db.withTransaction {
        LearningInputs(dao.allObservations(), dao.allEvents(), dao.facts(), dao.practices(), db.goals().readAll())
    }
    internal val inputsFlow get() = db.invalidationTracker.createFlow("learning_observations", "guidance_events", "placements", "sessions",
        "block_completions", "block_reflections", "placement_goal_snapshots", "session_goals", "transcribe_activities", "goals", "goal_blocks", "block_primary_goals")
        .map { inputs() }

    /** Recheck displayed evidence and manual state in the same transaction as explicit acceptance. */
    internal suspend fun accept(suggestion: ReturnSuggestion, expectedRevision: Long,
        operationId: String, zone: ZoneId = ZoneId.systemDefault()): GuidanceEvent = db.withTransaction {
        dao.event(operationId)?.let {
            check(it.blockId == suggestion.blockId && it.action == "DATE_SET" && it.offeredSuggestion == suggestion.provenance())
            return@withTransaction it
        }
        checkNotNull(db.blocks().find(suggestion.blockId)).also { check(!it.archived) }
        val inputs = inputs()
        val previous = inputs.events.filter { it.blockId == suggestion.blockId }.maxByOrNull { it.revision }
        check((previous?.revision ?: 0) == expectedRevision && expectedRevision < Long.MAX_VALUE) { "Revisit choices changed. Review the suggestion again." }
        val timestamp = now()
        val today = Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate()
        check(returnSuggestion(suggestion.blockId, inputs.observations, inputs.practices, today, timestamp) == suggestion) {
            "Evidence or date changed. Review the suggestion again."
        }
        val event = GuidanceEvent(operationId, suggestion.blockId, expectedRevision + 1, timestamp, "DATE_SET",
            previous?.returnSoon ?: false, suggestion.dueDate, suggestion.baseDate, suggestion.intervalDays,
            "ACCEPTED_SUGGESTION", suggestion.provenance())
        validateGuidanceEvent(event)
        dao.insert(event)
        event
    }

    suspend fun saveObservation(placementId: String, outcome: String, context: String,
        criterion: String?, reuseId: String? = null): LearningObservation = db.withTransaction {
        val placement = db.openHelper.writableDatabase.query(
            "SELECT sourceBlockId,s.status FROM placements p JOIN sessions s ON s.id=p.sessionId WHERE p.id=?",
            arrayOf(placementId)).use { c -> check(c.moveToFirst()) { "Practice placement is unavailable." }; c.getString(0) to c.getString(1) }
        check(placement.second != "DRAFT") { "Start this session before recording an observation." }
        val previous = dao.observation(placementId)
        val text = criterion?.trim()?.takeIf { it.isNotEmpty() }
        val anchor = reuseId?.let { id ->
            checkNotNull(dao.history(placement.first).find { it.placementId == id && it.placementId != placementId }) {
                "The earlier criterion is unavailable for this Block."
            }.also { check(it.criterionToken != null && it.criterion == text) { "Changed criteria need a new observation criterion." } }
        }
        val record = if (previous != null) {
            check(previous.criterion == text && previous.anchorId == reuseId) {
                "Saved criteria cannot be changed. Use a new practice placement for a changed target."
            }
            if (previous.outcome == outcome && previous.context == context) return@withTransaction previous
            previous.copy(outcome = outcome, context = context, correctedAt = maxOf(now(), previous.observedAt))
        } else LearningObservation(placementId, outcome, now(), context,
            text?.let { anchor?.criterionToken ?: UUID.randomUUID().toString() }, text, anchor?.placementId)
        validateLearningObservation(record)
        require(anchor == null || anchor.observedAt <= record.observedAt) { "The earlier observation has a later timestamp. Check the device clock." }
        if (previous == null) dao.insert(record) else dao.update(record)
        record
    }

    /** Stable operation ID makes a retry/recreation idempotent; revision serializes Block history. */
    suspend fun schedule(blockId: String, action: String, date: String? = null,
        base: String = LocalDate.now().toString(), operationId: String = UUID.randomUUID().toString()): GuidanceEvent = db.withTransaction {
        dao.event(operationId)?.let {
            check(it.blockId == blockId && it.action == action && (action != "DATE_SET" || it.dueDate == date))
            return@withTransaction it
        }
        checkNotNull(db.blocks().find(blockId)) { "This Block is unavailable." }
        val previous = dao.events(blockId).firstOrNull()
        check(previous?.revision != Long.MAX_VALUE)
        val due = if (action == "DATE_SET") canonicalLearningDate(requireNotNull(date)) else null
        val start = if (due != null) canonicalLearningDate(base) else null
        val days = if (due != null) java.time.temporal.ChronoUnit.DAYS.between(start, due).also { require(it in 0..36500) }.toInt() else null
        val clearDate = action == "DATE_CLEAR"
        val event = GuidanceEvent(operationId, blockId, (previous?.revision ?: 0) + 1, now(), action,
            when (action) { "FLAG_SET" -> true; "FLAG_CLEAR" -> false; else -> previous?.returnSoon ?: false },
            if (due != null) due.toString() else if (clearDate) null else previous?.dueDate,
            if (start != null) start.toString() else if (clearDate) null else previous?.baseDate,
            days ?: if (clearDate) null else previous?.intervalDays,
            if (due != null) "MANUAL" else if (clearDate) "NONE" else previous?.origin ?: "NONE",
            if (due != null || clearDate) null else previous?.offeredSuggestion)
        validateGuidanceEvent(event)
        dao.insert(event)
        event
    }
}

internal val LEARNING_MIGRATION_24_25 = object : Migration(24, 25) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE learning_observations (placementId TEXT NOT NULL, outcome TEXT NOT NULL, observedAt INTEGER NOT NULL, context TEXT NOT NULL, criterionToken TEXT, criterion TEXT, anchorId TEXT, correctedAt INTEGER, PRIMARY KEY(placementId), FOREIGN KEY(placementId) REFERENCES placements(id) ON UPDATE NO ACTION ON DELETE RESTRICT, FOREIGN KEY(anchorId) REFERENCES learning_observations(placementId) ON UPDATE NO ACTION ON DELETE RESTRICT)")
        db.execSQL("CREATE INDEX index_learning_observations_anchorId ON learning_observations(anchorId)")
        db.execSQL("CREATE TABLE guidance_events (id TEXT NOT NULL, blockId TEXT NOT NULL, revision INTEGER NOT NULL, recordedAt INTEGER NOT NULL, action TEXT NOT NULL, returnSoon INTEGER NOT NULL, dueDate TEXT, baseDate TEXT, intervalDays INTEGER, origin TEXT NOT NULL, offeredSuggestion TEXT, PRIMARY KEY(id), FOREIGN KEY(blockId) REFERENCES blocks(id) ON UPDATE NO ACTION ON DELETE RESTRICT)")
        db.execSQL("CREATE UNIQUE INDEX index_guidance_events_blockId_revision ON guidance_events(blockId,revision)")
    }
}

/** Shared strict validation for export/restore; unknown context is never inferred. */
internal fun validateLearningHistory(observations: List<LearningObservation>, placements: List<Placement>, sessions: List<PracticeSession>) {
    val byId = observations.associateBy { it.placementId }
    orderedLearningIds(observations)
    val byPlacement = placements.associateBy { it.id }
    val bySession = sessions.associateBy { it.id }
    observations.forEach { o ->
        validateLearningObservation(o)
        val p = requireNotNull(byPlacement[o.placementId]) { "Missing learning placement." }
        require(bySession[p.sessionId]?.status in listOf("IN_PROGRESS", "COMPLETED"))
        o.anchorId?.let { id ->
            val anchor = requireNotNull(byId[id]) { "Missing comparison anchor." }
            require(byPlacement[id]?.sourceBlockId == p.sourceBlockId && anchor.criterionToken == o.criterionToken &&
                anchor.criterion == o.criterion && anchor.observedAt <= o.observedAt) { "Incomparable evidence anchor." }
        }
    }
    observations.filter { it.criterionToken != null }.groupBy { it.criterionToken }.values.forEach { group ->
        require(group.map { it.criterion to byPlacement[it.placementId]?.sourceBlockId }.distinct().size == 1) {
            "A comparable criterion changed text or Block."
        }
    }
}

internal fun validateGuidanceHistory(events: List<GuidanceEvent>, adaptive: Boolean = true) {
    require(events.map { it.id }.distinct().size == events.size)
    events.groupBy { it.blockId }.values.forEach { group ->
        var previous: GuidanceEvent? = null
        group.sortedBy { it.revision }.forEachIndexed { index, e ->
            validateGuidanceEvent(e, adaptive)
            require(e.revision == index.toLong() + 1) { "Invalid guidance revision." }
            val old = previous
            if (e.action == "FLAG_SET") require(e.returnSoon)
            else if (e.action == "FLAG_CLEAR") require(!e.returnSoon)
            else require(e.returnSoon == (old?.returnSoon ?: false))
            if (e.action == "DATE_CLEAR") require(e.dueDate == null)
            else if (e.action == "DATE_SET") require(e.dueDate != null)
            else require(listOf(e.dueDate, e.baseDate, e.intervalDays, e.origin, e.offeredSuggestion) ==
                listOf(old?.dueDate, old?.baseDate, old?.intervalDays, old?.origin ?: "NONE", old?.offeredSuggestion)) {
                "Unrelated scheduling changes."
            }
            previous = e
        }
    }
}

/** Linear ordering avoids quadratic scans for long chains of comparable returns. */
internal fun orderedLearningIds(observations: List<LearningObservation>): List<String> {
    val byId = observations.associateBy { it.placementId }
    require(byId.size == observations.size) { "Duplicate learning observation." }
    require(observations.all { it.anchorId == null || it.anchorId in byId }) { "Missing comparison anchor." }
    val children = observations.filter { it.anchorId != null }.groupBy { it.anchorId }
    val queue = java.util.ArrayDeque(observations.filter { it.anchorId == null }.map { it.placementId })
    val ordered = ArrayList<String>(observations.size)
    while (queue.isNotEmpty()) {
        val id = queue.removeFirst()
        ordered.add(id)
        children[id].orEmpty().forEach { queue.addLast(it.placementId) }
    }
    require(ordered.size == observations.size) { "Cyclic learning evidence." }
    return ordered
}
