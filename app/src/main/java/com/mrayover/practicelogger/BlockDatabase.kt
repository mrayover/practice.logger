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
    @Insert suspend fun insert(block: Block)
    @Update suspend fun update(block: Block): Int
}
@Database(entities = [Block::class, PracticeSession::class, Placement::class, BlockTake::class, SavedRoutine::class, RoutinePlacement::class], version = 7, exportSchema = true)
abstract class BlockDatabase : RoomDatabase() {
    abstract fun blocks(): BlockDao
    abstract fun sessions(): SessionDao
    abstract fun routines(): RoutineDao
    abstract fun takes(): TakeDao
    companion object {
        @Volatile private var applicationDatabase: BlockDatabase? = null
        // One application-owned instance keeps catalog, practice and restore observers consistent.
        fun appInstance(context: Context): BlockDatabase = applicationDatabase ?: synchronized(this) {
            applicationDatabase ?: open(context.applicationContext).also { applicationDatabase = it }
        }
        fun open(context: Context, name: String = "practice.logger.db") =
            Room.databaseBuilder(context.applicationContext, BlockDatabase::class.java, name).addMigrations(SESSION_MIGRATION_1_2, METRONOME_MIGRATION_2_3, RECORDING_MIGRATION_3_4, PHRASE_MIGRATION_4_5, FREE_RECORDING_MIGRATION_5_6, ROUTINE_MIGRATION_6_7).enableMultiInstanceInvalidation().build()
    }
}
