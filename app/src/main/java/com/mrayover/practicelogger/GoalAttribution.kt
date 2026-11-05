package com.mrayover.practicelogger

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** A primary assignment is always backed by an existing Goal/Block link. */
@Entity(tableName = "block_primary_goals", foreignKeys = [ForeignKey(
    entity = GoalBlock::class, parentColumns = ["goalId", "blockId"],
    childColumns = ["goalId", "blockId"], onDelete = ForeignKey.CASCADE
)], indices = [Index("goalId", "blockId")])
data class BlockPrimaryGoal(@PrimaryKey val blockId: String, val goalId: String)

/** An absent row means no snapshot. A row with a null Goal means explicit absence. */
@Entity(tableName = "placement_goal_snapshots", foreignKeys = [
    ForeignKey(entity = Placement::class, parentColumns = ["id"], childColumns = ["placementId"],
        onDelete = ForeignKey.CASCADE),
    ForeignKey(entity = Goal::class, parentColumns = ["id"], childColumns = ["goalId"],
        onDelete = ForeignKey.RESTRICT)
], indices = [Index("goalId")])
data class PlacementGoalSnapshot(@PrimaryKey val placementId: String, val goalId: String?, val priority: Int?)

@Dao interface GoalAttributionDao {
    @Query("SELECT goalId FROM block_primary_goals WHERE blockId = :blockId")
    suspend fun primaryGoalId(blockId: String): String?
    @Upsert suspend fun setPrimary(assignment: BlockPrimaryGoal)
    @Query("DELETE FROM block_primary_goals WHERE blockId = :blockId")
    suspend fun clearPrimary(blockId: String)
    @Insert suspend fun insert(snapshot: PlacementGoalSnapshot)
    @Query("SELECT * FROM placement_goal_snapshots WHERE placementId = :placementId")
    suspend fun snapshot(placementId: String): PlacementGoalSnapshot?
}

val GOAL_ATTRIBUTION_MIGRATION_18_19 = object : Migration(18, 19) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""CREATE TABLE IF NOT EXISTS block_primary_goals (
            blockId TEXT NOT NULL, goalId TEXT NOT NULL, PRIMARY KEY(blockId),
            FOREIGN KEY(goalId, blockId) REFERENCES goal_blocks(goalId, blockId)
                ON UPDATE NO ACTION ON DELETE CASCADE)""")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_block_primary_goals_goalId_blockId ON block_primary_goals(goalId, blockId)")
        db.execSQL("""INSERT INTO block_primary_goals (blockId, goalId)
            SELECT link.blockId, MIN(link.goalId) FROM goal_blocks link
            JOIN blocks b ON b.id = link.blockId JOIN goals g ON g.id = link.goalId
            GROUP BY link.blockId HAVING COUNT(*) = 1""")
        db.execSQL("""CREATE TABLE IF NOT EXISTS placement_goal_snapshots (
            placementId TEXT NOT NULL, goalId TEXT, priority INTEGER, PRIMARY KEY(placementId),
            FOREIGN KEY(placementId) REFERENCES placements(id) ON UPDATE NO ACTION ON DELETE CASCADE,
            FOREIGN KEY(goalId) REFERENCES goals(id) ON UPDATE NO ACTION ON DELETE RESTRICT)""")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_placement_goal_snapshots_goalId ON placement_goal_snapshots(goalId)")
    }
}
