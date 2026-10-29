package com.mrayover.practicelogger

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "blocks")
data class Block(
    @PrimaryKey val id: String,
    val name: String,
    val action: String,
    val durationMinutes: Int = 5,
    val tempo: Int? = null,
    val notes: String = "",
    val archived: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
)
@Dao interface BlockDao {
    @Query("SELECT * FROM blocks ORDER BY name COLLATE NOCASE, id")
    fun observeAll(): Flow<List<Block>>
    @Query("SELECT * FROM blocks ORDER BY name COLLATE NOCASE, id")
    suspend fun readAll(): List<Block>
    @Query("SELECT * FROM blocks WHERE id = :id")
    suspend fun find(id: String): Block?
    // Scalar counts avoid loading history/audio and include every saved session state.
    @Query("""SELECT
        (SELECT COUNT(*) FROM goal_blocks WHERE blockId = :id) AS goals,
        (SELECT COUNT(DISTINCT routineId) FROM routine_placements WHERE sourceBlockId = :id) AS routines,
        (SELECT COUNT(*) FROM takes WHERE blockId = :id) AS recordings,
        (SELECT COUNT(*) FROM placements WHERE sourceBlockId = :id) AS sessionEntries,
        (SELECT COUNT(*) FROM placements p JOIN sessions s ON s.id = p.sessionId
            WHERE p.sourceBlockId = :id AND s.status = 'COMPLETED') AS completedEntries,
        (SELECT COUNT(*) FROM block_images WHERE blockId = :id) AS images""")
    suspend fun dependencies(id: String): BlockDependencies
    @Query("DELETE FROM blocks WHERE id = :id") suspend fun delete(id: String): Int
    @Insert suspend fun insert(block: Block)
    @Update suspend fun update(block: Block): Int
}
@Database(entities = [Block::class, PracticeSession::class, Placement::class, BlockTake::class, SavedRoutine::class, RoutinePlacement::class, Goal::class, GoalBlock::class, GoalSession::class, DailyLog::class, BlockImage::class], version = 10, exportSchema = true)
abstract class BlockDatabase : RoomDatabase() {
    abstract fun images(): BlockImageDao
    abstract fun logs(): LogDao
    abstract fun blocks(): BlockDao
    abstract fun sessions(): SessionDao
    abstract fun goals(): GoalDao
    abstract fun routines(): RoutineDao
    abstract fun takes(): TakeDao
    companion object {
        @Volatile private var applicationDatabase: BlockDatabase? = null
        // One application-owned instance keeps catalog, practice and restore observers consistent.
        fun appInstance(context: Context): BlockDatabase = applicationDatabase ?: synchronized(this) {
            applicationDatabase ?: open(context.applicationContext).also { applicationDatabase = it }
        }
        fun open(context: Context, name: String = "practice.logger.db") =
            Room.databaseBuilder(context.applicationContext, BlockDatabase::class.java, name).addMigrations(SESSION_MIGRATION_1_2, METRONOME_MIGRATION_2_3, RECORDING_MIGRATION_3_4, PHRASE_MIGRATION_4_5, FREE_RECORDING_MIGRATION_5_6, ROUTINE_MIGRATION_6_7, GOAL_MIGRATION_7_8, LOG_MIGRATION_8_9, IMAGE_MIGRATION_9_10).enableMultiInstanceInvalidation().build()
    }
}
