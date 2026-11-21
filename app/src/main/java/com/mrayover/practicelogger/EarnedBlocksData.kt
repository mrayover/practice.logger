package com.mrayover.practicelogger

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.migration.Migration
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal const val BLOCK_CONSUMPTION_ID_LIMIT = 128

@Entity(
    tableName = "earned_block_spends",
    foreignKeys = [ForeignKey(
        entity = BlockCompletion::class, parentColumns = ["placementId"],
        childColumns = ["placementId"], onDelete = ForeignKey.RESTRICT
    )],
    indices = [Index("consumptionId", unique = true)]
)
data class EarnedBlockSpend(
    @PrimaryKey val placementId: String,
    val consumptionId: String,
    val spentAt: Long
)

data class EarnedBlockHistoryEntry(
    val placementId: String,
    val name: String,
    val completedAt: Long,
    val goalId: String?,
    val goalTitle: String?,
    val colorArgb: Int?
)

data class EarnedBlockBalance(val colorArgb: Int?, val hasGoal: Boolean, val earned: Int, val used: Int) {
    val available get() = earned - used
}

internal data class EarnedBlockInventory(val balances: List<EarnedBlockBalance>) {
    val earned = balances.sumOf { it.earned }
    val used = balances.sumOf { it.used }
    val available = earned - used
}

@Dao
interface EarnedBlocksDao {
    @Query("""
        SELECT c.placementId, p.name, c.completedAt, c.goalId, c.goalTitle, c.colorArgb
        FROM block_completions c JOIN placements p ON p.id = c.placementId
        ORDER BY c.completedAt DESC, c.placementId DESC
    """)
    fun observeHistory(): Flow<List<EarnedBlockHistoryEntry>>

    @Query("""
        SELECT c.colorArgb, c.goalId IS NOT NULL AS hasGoal,
            COUNT(*) AS earned, COUNT(s.placementId) AS used
        FROM block_completions c LEFT JOIN earned_block_spends s ON s.placementId = c.placementId
        GROUP BY c.colorArgb, hasGoal
    """)
    fun observeBalances(): Flow<List<EarnedBlockBalance>>

    @Query("""
        SELECT c.* FROM block_completions c JOIN earned_block_spends s ON s.placementId = c.placementId
        WHERE s.consumptionId = :consumptionId
    """)
    suspend fun spentBlock(consumptionId: String): BlockCompletion?

    @Query("""
        SELECT c.placementId FROM block_completions c
        LEFT JOIN earned_block_spends s ON s.placementId = c.placementId
        WHERE c.colorArgb IS :colorArgb AND s.placementId IS NULL
        ORDER BY c.completedAt, c.placementId LIMIT 1
    """)
    suspend fun nextAvailable(colorArgb: Int?): String?

    @Insert suspend fun insertSpend(spend: EarnedBlockSpend)
}

internal class EarnedBlocksRepository(
    private val db: BlockDatabase,
    private val now: () -> Long = System::currentTimeMillis
) {
    private val dao = db.earnedBlocks()
    val inventory = dao.observeBalances().map(::EarnedBlockInventory)
    val history = dao.observeHistory()

    /** Spend one Block of this saved color. Reuse the same ID when retrying a game action. */
    suspend fun consume(consumptionId: String, colorArgb: Int?): String? = db.withTransaction {
        require(consumptionId.isNotBlank() && consumptionId.length <= BLOCK_CONSUMPTION_ID_LIMIT)
        dao.spentBlock(consumptionId)?.let { previous ->
            require(previous.colorArgb == colorArgb) { "This consumption ID belongs to another color." }
            return@withTransaction previous.placementId
        }
        val placementId = dao.nextAvailable(colorArgb) ?: return@withTransaction null
        val spentAt = now()
        require(spentAt >= 0)
        dao.insertSpend(EarnedBlockSpend(placementId, consumptionId, spentAt))
        placementId
    }
}

internal val EARNED_BLOCKS_MIGRATION_29_30 = object : Migration(29, 30) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE block_completions ADD COLUMN goalTitle TEXT")
        db.execSQL("""
            CREATE TABLE earned_block_spends (
                placementId TEXT NOT NULL PRIMARY KEY, consumptionId TEXT NOT NULL, spentAt INTEGER NOT NULL,
                FOREIGN KEY(placementId) REFERENCES block_completions(placementId)
                    ON UPDATE NO ACTION ON DELETE RESTRICT
            )
        """.trimIndent())
        db.execSQL("CREATE UNIQUE INDEX index_earned_block_spends_consumptionId ON earned_block_spends(consumptionId)")
    }
}
