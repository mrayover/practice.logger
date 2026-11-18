package com.mrayover.practicelogger

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import kotlinx.coroutines.flow.Flow

internal fun practiceWeek(date: LocalDate = LocalDate.now()): LocalDate =
    date.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))

@Entity(tableName = "week_blocks", primaryKeys = ["weekStart", "blockId"],
    foreignKeys = [ForeignKey(entity = Block::class, parentColumns = ["id"], childColumns = ["blockId"],
        onDelete = ForeignKey.RESTRICT)], indices = [Index("blockId")])
data class WeekBlock(val weekStart: String, val blockId: String, val position: Int,
    @ColumnInfo(defaultValue = "1") val plannedRepetitions: Int = 3)

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

    @Query("UPDATE week_blocks SET plannedRepetitions = :repetitions WHERE weekStart = :week AND blockId = :id")
    suspend fun setRepetitions(week: String, id: String, repetitions: Int): Int
}

// Existing membership represented one intended practice. Never backfill a new three-practice plan.
internal val WEEK_REPETITIONS_MIGRATION_27_28 = object : Migration(27, 28) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE week_blocks ADD COLUMN plannedRepetitions INTEGER NOT NULL DEFAULT 1")
    }
}

internal data class WeekRepetitionProgress(val planned: Int, val completed: Int) {
    init { require(planned >= 1 && completed >= 0) }
    val fulfilled get() = completed >= planned
    val dots get() = minOf(planned, completed)
    val remaining get() = maxOf(0, planned - completed)
}

internal fun plannedWeekMinutesByBlock(plans: List<WeekBlock>, blocks: List<Block>, week: LocalDate): Map<String, Long> {
    val durations = blocks.associate { it.id to it.durationMinutes }
    val weekKey = week.toString()
    return plans.filter { it.weekStart == weekKey }.associate {
        it.blockId to ((durations[it.blockId] ?: 0).toLong() * it.plannedRepetitions)
    }
}

internal fun plannedWeekMinutes(plans: List<WeekBlock>, blocks: List<Block>, week: LocalDate): Long =
    plannedWeekMinutesByBlock(plans, blocks, week).values.sum()

internal fun weekCompletionCounts(sessions: List<SessionBundle>, week: LocalDate,
    zone: ZoneId = ZoneId.systemDefault()): Map<String, Int> {
    val start = week.atStartOfDay(zone).toInstant().toEpochMilli()
    val end = week.plusWeeks(1).atStartOfDay(zone).toInstant().toEpochMilli()
    return sessions.asSequence().flatMap { it.completions.asSequence() }.map { it.completion }
        .filter { it.completedAt >= start && it.completedAt < end }
        .distinctBy { it.placementId }.groupingBy { it.sourceBlockId }.eachCount()
}

internal val WEEK_PLAN_MIGRATION_19_20 = object : Migration(19, 20) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""CREATE TABLE IF NOT EXISTS week_blocks (
            weekStart TEXT NOT NULL, blockId TEXT NOT NULL, position INTEGER NOT NULL,
            PRIMARY KEY(weekStart, blockId), FOREIGN KEY(blockId) REFERENCES blocks(id) ON DELETE RESTRICT)""")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_week_blocks_blockId ON week_blocks(blockId)")
    }
}

internal fun weekCompletions(
    sessions: List<SessionBundle>,
    week: LocalDate,
    zone: ZoneId = ZoneId.systemDefault()
): Map<String, BlockCompletion> {
    val start = week.atStartOfDay(zone).toInstant().toEpochMilli()
    val end = week.plusWeeks(1).atStartOfDay(zone).toInstant().toEpochMilli()
    return buildMap {
        for (session in sessions) {
            for ((completion) in session.completions) {
                if (completion.completedAt >= start && completion.completedAt < end &&
                    completion.completedAt >= (get(completion.sourceBlockId)?.completedAt ?: start)) {
                    put(completion.sourceBlockId, completion)
                }
            }
        }
    }
}
