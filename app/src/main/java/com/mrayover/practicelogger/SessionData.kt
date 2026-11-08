package com.mrayover.practicelogger

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Entity(tableName = "sessions")
data class PracticeSession(
    @PrimaryKey val id: String,
    val startedAt: Long,
    val endedAt: Long? = null,
    val status: String = "DRAFT",
    val currentIndex: Int = 0,
    val note: String = ""
)

@Entity(tableName = "placements", foreignKeys = [ForeignKey(
    entity = PracticeSession::class, parentColumns = ["id"], childColumns = ["sessionId"], onDelete = ForeignKey.CASCADE
)], indices = [Index("sessionId")])
data class Placement(
    @PrimaryKey val id: String,
    val sessionId: String,
    val sourceBlockId: String,
    val name: String,
    val action: String,
    val tempo: Int?,
    val referenceNotes: String,
    val position: Int,
    val configuredMs: Long,
    val activeMs: Long = 0,
    val outcome: String = "PENDING",
    val overtimeAccepted: Boolean = false,
    val note: String = "",
    val lastUsedBpm: Int? = null,
    @ColumnInfo(defaultValue = "0") val prepMs: Long = 0
)

data class SessionBundle(
    @Embedded val session: PracticeSession,
    @Relation(parentColumn = "id", entityColumn = "sessionId") val placements: List<Placement>
) {
    val ordered: List<Placement> get() = placements.sortedBy { it.position }
    val current: Placement? get() = ordered.getOrNull(session.currentIndex)
    val activeMs: Long get() = placements.sumOf { it.activeMs }
}

@Dao interface SessionDao {
    @Transaction @Query("SELECT * FROM sessions ORDER BY startedAt DESC, id")
    fun observeAll(): Flow<List<SessionBundle>>
    @Transaction @Query("SELECT * FROM sessions ORDER BY startedAt DESC, id")
    suspend fun readAll(): List<SessionBundle>
    @Transaction @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun find(id: String): SessionBundle?
    @Query("SELECT * FROM sessions WHERE status = :status LIMIT 1")
    suspend fun withStatus(status: String): PracticeSession?
    @Insert suspend fun insert(session: PracticeSession)
    @Update suspend fun update(session: PracticeSession)
    @Insert suspend fun insert(placement: Placement)
    @Update suspend fun update(placement: Placement)
    @Query("DELETE FROM placements WHERE id = :id") suspend fun removePlacement(id: String)
    @Query("DELETE FROM sessions WHERE id = :id AND status = 'DRAFT'") suspend fun discardDraft(id: String)
}

// Conservative recovery: only committed foreground time is restored, always paused.
// Wall-clock timestamps label history; they are never used to calculate practice time.
fun advancePractice(placement: Placement, deltaMs: Long): Placement {
    require(deltaMs >= 0)
    require(placement.configuredMs > 0 && placement.activeMs >= 0)
    if (placement.outcome != "PENDING") return placement
    val sum = if (Long.MAX_VALUE - placement.activeMs < deltaMs) Long.MAX_VALUE else placement.activeMs + deltaMs
    return placement.copy(activeMs = if (placement.overtimeAccepted) sum else minOf(sum, placement.configuredMs))
}

val SESSION_MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS sessions (id TEXT NOT NULL, startedAt INTEGER NOT NULL, endedAt INTEGER, status TEXT NOT NULL, currentIndex INTEGER NOT NULL, note TEXT NOT NULL, PRIMARY KEY(id))")
        db.execSQL("CREATE TABLE IF NOT EXISTS placements (id TEXT NOT NULL, sessionId TEXT NOT NULL, sourceBlockId TEXT NOT NULL, name TEXT NOT NULL, action TEXT NOT NULL, tempo INTEGER, referenceNotes TEXT NOT NULL, position INTEGER NOT NULL, configuredMs INTEGER NOT NULL, activeMs INTEGER NOT NULL, outcome TEXT NOT NULL, overtimeAccepted INTEGER NOT NULL, note TEXT NOT NULL, PRIMARY KEY(id), FOREIGN KEY(sessionId) REFERENCES sessions(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_placements_sessionId ON placements(sessionId)")
    }
}

val METRONOME_MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE placements ADD COLUMN lastUsedBpm INTEGER")
    }
}

internal val PREP_MIGRATION_20_21 = object : Migration(20, 21) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE placements ADD COLUMN prepMs INTEGER NOT NULL DEFAULT 0")
    }
}

class SessionRepository(private val db: BlockDatabase, private val now: () -> Long = System::currentTimeMillis) {
    private val dao get() = db.sessions()
    val sessions = dao.observeAll()
    private suspend fun draft(): SessionBundle {

        val record = dao.withStatus("DRAFT") ?: PracticeSession(UUID.randomUUID().toString(), now()).also { dao.insert(it) }
        return checkNotNull(dao.find(record.id))
    }
    suspend fun addBlock(blockId: String) = addBlocks(listOf(blockId))

    suspend fun addBlocks(blockIds: List<String>) = db.withTransaction {
        require(blockIds.isNotEmpty())
        val bundle = draft()
        blockIds.forEachIndexed { index, id ->
            insertBlock(bundle.session.id, activeBlock(id), bundle.placements.size + index)
        }
    }

    suspend fun startBlock(blockId: String): String = db.withTransaction {
        val block = activeBlock(blockId)
        val session = PracticeSession(UUID.randomUUID().toString(), now(), status = "IN_PROGRESS")
        dao.insert(session)
        insertBlock(session.id, block, 0)
        session.id
    }

    private suspend fun activeBlock(id: String): Block =
        checkNotNull(db.blocks().find(id)).also { check(!it.archived) }

    private suspend fun insertBlock(sessionId: String, block: Block, position: Int) {
        dao.insert(Placement(UUID.randomUUID().toString(), sessionId, block.id, block.name, block.action,
            block.tempo, block.notes, position, block.durationMinutes.toLong() * 60_000L))
    }
    suspend fun setDuration(placementId: String, milliseconds: Long) = db.withTransaction {
        require(milliseconds > 0)
        val bundle = draft()
        val placement = checkNotNull(bundle.placements.find { it.id == placementId })
        dao.update(placement.copy(configuredMs = milliseconds))
    }
    suspend fun move(placementId: String, direction: Int) = db.withTransaction {
        require(direction == -1 || direction == 1)
        val bundle = draft()
        val ordered = bundle.ordered
        val index = ordered.indexOfFirst { it.id == placementId }
        require(index >= 0 && index + direction in ordered.indices)
        val first = ordered[index]; val second = ordered[index + direction]
        dao.update(first.copy(position = second.position)); dao.update(second.copy(position = first.position))
    }
    suspend fun remove(placementId: String) = db.withTransaction {
        val bundle = draft()
        check(bundle.placements.any { it.id == placementId })
        dao.removePlacement(placementId)
        bundle.ordered.filter { it.id != placementId }.forEachIndexed { index, placement -> dao.update(placement.copy(position = index)) }
    }
    suspend fun discardDraft() = db.withTransaction { dao.withStatus("DRAFT")?.let { dao.discardDraft(it.id) } }
    suspend fun start(): String = db.withTransaction {
        val bundle = draft()
        check(bundle.placements.isNotEmpty())
        dao.update(bundle.session.copy(status = "IN_PROGRESS", startedAt = now()))
        bundle.session.id
    }
    /** Fresh independent session; the builder draft and template remain untouched. */
    suspend fun startRoutine(routineId: String): String = db.withTransaction {
        val routine = db.routines().find(routineId) ?: throw RoutineUnavailableException("This Routine no longer exists. Return to Routines.")
        val rows = routine.ordered
        if (rows.isEmpty() || rows.map { it.position } != rows.indices.toList() || rows.any { it.configuredMs <= 0 })
            throw RoutineUnavailableException("This Routine has invalid placements. Edit and save it before practicing.")
        val blocks = rows.map { db.blocks().find(it.sourceBlockId) }
        val unavailable = routineUnavailable(rows.map { RoutineSelection(it.sourceBlockId, it.configuredMs) }, blocks.filterNotNull().associateBy { it.id })
        if (unavailable.isNotEmpty()) throw RoutineUnavailableException("Routine cannot start. ${unavailable.joinToString("; ")}. Restore or replace these Blocks first.")
        val session = PracticeSession(UUID.randomUUID().toString(), now(), status = "IN_PROGRESS")
        dao.insert(session)
        rows.forEachIndexed { index, row ->
            val block = checkNotNull(blocks[index])
            dao.insert(Placement(UUID.randomUUID().toString(), session.id, block.id, block.name, block.action,
                block.tempo, block.notes, index, row.configuredMs))
        }
        session.id
    }
    /** Explicit attribution only; ignores archived links with a visible explanation in Goal detail. */
    suspend fun startGoal(goalId: String): String = db.withTransaction {
        val goal = checkNotNull(db.goals().find(goalId)) { "This Goal is unavailable." }
        require(!goal.goal.completed) { "Reopen this Goal before practicing." }
        val blocks = goal.ordered.mapNotNull { db.blocks().find(it.blockId)?.takeUnless { b -> b.archived } }
        require(blocks.isNotEmpty()) { "Add an active Block to this Goal before practicing." }
        val session = PracticeSession(UUID.randomUUID().toString(), now(), status = "IN_PROGRESS")
        dao.insert(session)
        db.goals().insert(GoalSession(session.id, goalId))
        blocks.forEachIndexed { i, block ->
            dao.insert(Placement(UUID.randomUUID().toString(), session.id, block.id, block.name, block.action,
                block.tempo, block.notes, i, block.durationMinutes.toLong() * 60_000L))
        }
        session.id
    }
    private suspend fun active(id: String): SessionBundle = checkNotNull(dao.find(id)).also { check(it.session.status == "IN_PROGRESS") }
    suspend fun checkpoint(id: String, deltaMs: Long, placementId: String? = null): Placement = db.withTransaction {
        val placement = checkNotNull(active(id).current)
        check(placementId == null || placement.id == placementId)
        val updated = advancePractice(placement, deltaMs)
        dao.update(updated)
        if (placement.activeMs == 0L && updated.activeMs > 0L) {
            val attribution = db.goalAttribution()
            val goalId = attribution.primaryGoalId(placement.sourceBlockId)
            val priority = goalId?.let { id ->
                (activeGoals(db.goals().readAll()).indexOfFirst { it.goal.id == id } + 1)
                    .takeIf { it in 1..5 }
            }
            attribution.insert(PlacementGoalSnapshot(placement.id, goalId, priority))
        }
        updated
    }
    suspend fun checkpointPrep(id: String, placementId: String, deltaMs: Long) = db.withTransaction {
        require(deltaMs >= 0)
        val current = checkNotNull(active(id).current)
        check(current.id == placementId)
        val prepMs = current.prepMs + minOf(deltaMs, Long.MAX_VALUE - current.prepMs)
        dao.update(current.copy(prepMs = prepMs))
    }
    suspend fun continueOvertime(id: String) = db.withTransaction {
        val current = checkNotNull(active(id).current)
        check(current.activeMs >= current.configuredMs)
        dao.update(current.copy(overtimeAccepted = true))
    }
    suspend fun finishBlock(id: String, outcome: String, note: String, preserveNote: Boolean = false): Boolean = db.withTransaction {
        require(outcome == "COMPLETED" || outcome == "SKIPPED")
        val bundle = active(id)
        val current = checkNotNull(bundle.current)
        check(current.outcome == "PENDING")
        dao.update(current.copy(outcome = outcome, note = if (preserveNote) current.note else note.trim()))
        val finished = bundle.session.currentIndex + 1 == bundle.placements.size
        dao.update(if (finished) bundle.session.copy(status = "COMPLETED", endedAt = now()) else bundle.session.copy(currentIndex = bundle.session.currentIndex + 1))
        finished
    }
    suspend fun saveBlockNote(id: String, placementId: String, note: String) = db.withTransaction {
        val bundle = checkNotNull(dao.find(id))
        check(bundle.session.status == "IN_PROGRESS")
        val current = checkNotNull(bundle.current)
        check(current.id == placementId && current.outcome == "PENDING")
        dao.update(current.copy(note = note.trim()))
    }
    suspend fun saveUsedBpm(id: String, placementId: String, bpm: Int) = db.withTransaction {
        require(bpm in 30..300)
        val current = checkNotNull(active(id).current)
        check(current.id == placementId && current.outcome == "PENDING")
        dao.update(current.copy(lastUsedBpm = bpm))
    }
    suspend fun saveSessionNote(id: String, note: String) = db.withTransaction {
        val bundle = checkNotNull(dao.find(id))
        check(bundle.session.status == "COMPLETED")
        dao.update(bundle.session.copy(note = note.trim()))
    }
}
