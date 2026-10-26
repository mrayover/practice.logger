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
    val note: String = ""
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

class SessionRepository(private val db: BlockDatabase, private val now: () -> Long = System::currentTimeMillis) {
    private val dao get() = db.sessions()
    val sessions = dao.observeAll()
    private suspend fun draft(): SessionBundle {
        check(dao.withStatus("IN_PROGRESS") == null) { "Resume the unfinished session first." }
        val record = dao.withStatus("DRAFT") ?: PracticeSession(UUID.randomUUID().toString(), now()).also { dao.insert(it) }
        return checkNotNull(dao.find(record.id))
    }
    suspend fun addBlock(blockId: String) = db.withTransaction {
        val block = checkNotNull(db.blocks().find(blockId))
        check(!block.archived)
        val bundle = draft()
        dao.insert(Placement(UUID.randomUUID().toString(), bundle.session.id, block.id, block.name, block.action,
            block.tempo, block.notes, bundle.placements.size, block.durationMinutes.toLong() * 60_000L))
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
    private suspend fun active(id: String): SessionBundle = checkNotNull(dao.find(id)).also { check(it.session.status == "IN_PROGRESS") }
    suspend fun checkpoint(id: String, deltaMs: Long): Placement = db.withTransaction {
        val placement = checkNotNull(active(id).current)
        val updated = advancePractice(placement, deltaMs)
        dao.update(updated)
        updated
    }
    suspend fun continueOvertime(id: String) = db.withTransaction {
        val current = checkNotNull(active(id).current)
        check(current.activeMs >= current.configuredMs)
        dao.update(current.copy(overtimeAccepted = true))
    }
    suspend fun finishBlock(id: String, outcome: String, note: String): Boolean = db.withTransaction {
        require(outcome == "COMPLETED" || outcome == "SKIPPED")
        val bundle = active(id)
        val current = checkNotNull(bundle.current)
        check(current.outcome == "PENDING")
        dao.update(current.copy(outcome = outcome, note = note.trim()))
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
    suspend fun saveSessionNote(id: String, note: String) = db.withTransaction {
        val bundle = checkNotNull(dao.find(id))
        check(bundle.session.status == "COMPLETED")
        dao.update(bundle.session.copy(note = note.trim()))
    }
}
