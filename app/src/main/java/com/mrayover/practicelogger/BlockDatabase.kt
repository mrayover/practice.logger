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
    @Query("SELECT * FROM blocks WHERE id = :id")
    suspend fun find(id: String): Block?
    @Insert suspend fun insert(block: Block)
    @Update suspend fun update(block: Block): Int
}
@Database(entities = [Block::class], version = 1, exportSchema = true)
abstract class BlockDatabase : RoomDatabase() {
    abstract fun blocks(): BlockDao
    companion object {
        fun open(context: Context, name: String = "practice.logger.db") =
            Room.databaseBuilder(context.applicationContext, BlockDatabase::class.java, name).build()
    }
}
