package com.mrayover.practicelogger

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Entity(tableName = "routines")
data class SavedRoutine(@PrimaryKey val id: String, val name: String, val createdAt: Long, val updatedAt: Long)

@Entity(tableName = "routine_placements", foreignKeys = [
    ForeignKey(entity = SavedRoutine::class, parentColumns = ["id"], childColumns = ["routineId"], onDelete = ForeignKey.CASCADE),
    ForeignKey(entity = Block::class, parentColumns = ["id"], childColumns = ["sourceBlockId"], onDelete = ForeignKey.RESTRICT)
], indices = [Index("routineId"), Index("sourceBlockId")])
data class RoutinePlacement(@PrimaryKey val id: String, val routineId: String, val sourceBlockId: String,
    val position: Int, val configuredMs: Long)

data class RoutineBundle(@Embedded val routine: SavedRoutine,
    @Relation(parentColumn = "id", entityColumn = "routineId") val placements: List<RoutinePlacement>) {
    val ordered get() = placements.sortedBy { it.position }
}

data class RoutineSelection(val blockId: String, val configuredMs: Long)

@Dao interface RoutineDao {
    @Transaction @Query("SELECT * FROM routines ORDER BY name COLLATE NOCASE, id")
    fun observeAll(): Flow<List<RoutineBundle>>
    @Transaction @Query("SELECT * FROM routines ORDER BY name COLLATE NOCASE, id")
    suspend fun readAll(): List<RoutineBundle>
    @Transaction @Query("SELECT * FROM routines WHERE id = :id") suspend fun find(id: String): RoutineBundle?
    @Insert suspend fun insert(routine: SavedRoutine)
    @Update suspend fun update(routine: SavedRoutine): Int
    @Insert suspend fun insert(placement: RoutinePlacement)
    @Query("DELETE FROM routine_placements WHERE routineId = :id") suspend fun clearPlacements(id: String)
    @Query("DELETE FROM routines WHERE id = :id") suspend fun delete(id: String): Int
}

val ROUTINE_MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS routines (id TEXT NOT NULL, name TEXT NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, PRIMARY KEY(id))")
        db.execSQL("CREATE TABLE IF NOT EXISTS routine_placements (id TEXT NOT NULL, routineId TEXT NOT NULL, sourceBlockId TEXT NOT NULL, position INTEGER NOT NULL, configuredMs INTEGER NOT NULL, PRIMARY KEY(id), FOREIGN KEY(routineId) REFERENCES routines(id) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(sourceBlockId) REFERENCES blocks(id) ON UPDATE NO ACTION ON DELETE RESTRICT)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_routine_placements_routineId ON routine_placements(routineId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_routine_placements_sourceBlockId ON routine_placements(sourceBlockId)")
    }
}

class RoutineUnavailableException(message: String) : IllegalStateException(message)

fun routineNameValid(name: String) = name.trim().length in 1..120

fun routineUnavailable(entries: List<RoutineSelection>, blocks: Map<String, Block>): List<String> = entries.mapNotNull {
    val block = blocks[it.blockId]
    when { block == null -> "Unavailable Block (${it.blockId})"; block.archived -> "Archived: ${block.name}"; else -> null }
}.distinct()

class RoutineRepository(private val db: BlockDatabase, private val now: () -> Long = System::currentTimeMillis) {
    val routines = db.routines().observeAll()
    suspend fun save(id: String?, name: String, selections: List<RoutineSelection>): String = db.withTransaction {
        require(routineNameValid(name)) { "Enter a Routine name (1–120 characters)." }
        require(selections.isNotEmpty() && selections.size <= 20_000) { "Add at least one Block to the Routine." }
        selections.forEach {
            require(it.configuredMs > 0) { "Each duration must be positive." }
            if (db.blocks().find(it.blockId) == null) throw RoutineUnavailableException("A Block is unavailable. Remove or replace it before saving the Routine.")
        }
        val previous = id?.let { db.routines().find(it)?.routine ?: throw RoutineUnavailableException("This Routine no longer exists. Return to Routines.") }
        val timestamp = now().coerceAtLeast(0)
        val routine = previous?.copy(name = name.trim(), updatedAt = maxOf(timestamp, previous.createdAt))
            ?: SavedRoutine(UUID.randomUUID().toString(), name.trim(), timestamp, timestamp)
        if (previous == null) db.routines().insert(routine) else check(db.routines().update(routine) == 1)
        db.routines().clearPlacements(routine.id)
        selections.forEachIndexed { position, selection ->
            db.routines().insert(RoutinePlacement(UUID.randomUUID().toString(), routine.id, selection.blockId, position, selection.configuredMs))
        }
        routine.id
    }
    suspend fun saveDraft(name: String): String = db.withTransaction {
        val draft = db.sessions().withStatus("DRAFT")?.let { db.sessions().find(it.id) }
        require(draft != null && draft.placements.isNotEmpty()) { "Add Blocks to the session before saving a Routine." }
        save(null, name, draft.ordered.map { RoutineSelection(it.sourceBlockId, it.configuredMs) })
    }
    suspend fun delete(id: String) = db.withTransaction { check(db.routines().delete(id) == 1) }
}
