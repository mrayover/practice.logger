package com.mrayover.practicelogger

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import kotlinx.coroutines.flow.Flow

internal fun practiceWeek(date: LocalDate = LocalDate.now()): LocalDate =
    date.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))

@Entity(tableName = "week_blocks", primaryKeys = ["weekStart", "blockId"],
    foreignKeys = [ForeignKey(entity = Block::class, parentColumns = ["id"], childColumns = ["blockId"],
        onDelete = ForeignKey.RESTRICT)], indices = [Index("blockId")])
data class WeekBlock(val weekStart: String, val blockId: String, val position: Int)

@Dao
interface WeekPlanDao {
    @Query("SELECT * FROM week_blocks ORDER BY weekStart, position")
    fun observeAll(): Flow<List<WeekBlock>>

    @Query("SELECT * FROM week_blocks WHERE weekStart = :week ORDER BY position")
    suspend fun read(week: String): List<WeekBlock>

    @Query("SELECT COUNT(*) FROM week_blocks")
    suspend fun count(): Int

    @Insert
    suspend fun insert(rows: List<WeekBlock>)

    @Query("DELETE FROM week_blocks WHERE weekStart = :week AND blockId = :id")
    suspend fun remove(week: String, id: String)

    @Query("UPDATE week_blocks SET position = position - 1 WHERE weekStart = :week AND position > :position")
    suspend fun closeGap(week: String, position: Int)
}

internal val WEEK_PLAN_MIGRATION_19_20 = object : Migration(19, 20) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""CREATE TABLE IF NOT EXISTS week_blocks (
            weekStart TEXT NOT NULL, blockId TEXT NOT NULL, position INTEGER NOT NULL,
            PRIMARY KEY(weekStart, blockId), FOREIGN KEY(blockId) REFERENCES blocks(id) ON DELETE RESTRICT)""")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_week_blocks_blockId ON week_blocks(blockId)")
    }
}
