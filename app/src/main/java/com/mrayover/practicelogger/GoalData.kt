package com.mrayover.practicelogger

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Entity(tableName = "goals")
data class Goal(@PrimaryKey val id: String, val title: String, val description: String = "",
    val progress: Int = 0, val notes: String = "", val completed: Boolean = false,
    val priority: Int = 0, val createdAt: Long, val updatedAt: Long)

@Entity(tableName = "goal_blocks", primaryKeys = ["goalId", "blockId"], foreignKeys = [
    ForeignKey(entity = Goal::class, parentColumns = ["id"], childColumns = ["goalId"], onDelete = ForeignKey.RESTRICT),
    ForeignKey(entity = Block::class, parentColumns = ["id"], childColumns = ["blockId"], onDelete = ForeignKey.RESTRICT)
], indices = [Index("blockId")])
data class GoalBlock(val goalId: String, val blockId: String, val position: Int)

@Entity(tableName = "session_goals", foreignKeys = [
    ForeignKey(entity = PracticeSession::class, parentColumns = ["id"], childColumns = ["sessionId"], onDelete = ForeignKey.CASCADE),
    ForeignKey(entity = Goal::class, parentColumns = ["id"], childColumns = ["goalId"], onDelete = ForeignKey.RESTRICT)
], indices = [Index("goalId")])
data class GoalSession(@PrimaryKey val sessionId: String, val goalId: String)

data class GoalBundle(@Embedded val goal: Goal,
    @Relation(parentColumn = "id", entityColumn = "goalId") val blocks: List<GoalBlock>) {
    val ordered get() = blocks.sortedBy { it.position }
}
data class GoalTime(val goalId: String, val activeMs: Long)
fun activeGoals(goals: List<GoalBundle>) = goals.filter { !it.goal.completed }.sortedWith(compareBy({ it.goal.priority }, { it.goal.id }))
fun homeGoals(goals: List<GoalBundle>) = activeGoals(goals).take(2)

@Dao interface GoalDao {
    @Transaction @Query("SELECT * FROM goals ORDER BY completed, priority, updatedAt DESC, id")
    fun observeAll(): Flow<List<GoalBundle>>
    @Transaction @Query("SELECT * FROM goals ORDER BY completed, priority, updatedAt DESC, id")
    suspend fun readAll(): List<GoalBundle>
    @Transaction @Query("SELECT * FROM goals WHERE id = :id") suspend fun find(id: String): GoalBundle?
    @Insert suspend fun insert(goal: Goal)
    @Update suspend fun update(goal: Goal): Int
    @Insert suspend fun insert(link: GoalBlock)
    @Insert suspend fun insert(link: GoalSession)
    @Query("DELETE FROM goal_blocks WHERE goalId = :id") suspend fun clearBlocks(id: String)
    @Query("SELECT * FROM session_goals") suspend fun readSessions(): List<GoalSession>
    @Query("SELECT * FROM session_goals") fun observeSessions(): Flow<List<GoalSession>>
    @Query("SELECT sg.goalId, COALESCE(SUM(p.activeMs),0) AS activeMs FROM session_goals sg JOIN placements p ON p.sessionId = sg.sessionId GROUP BY sg.goalId")
    fun observeTime(): Flow<List<GoalTime>>
}

val GOAL_MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS goals (id TEXT NOT NULL, title TEXT NOT NULL, description TEXT NOT NULL, progress INTEGER NOT NULL, notes TEXT NOT NULL, completed INTEGER NOT NULL, priority INTEGER NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, PRIMARY KEY(id))")
        db.execSQL("CREATE TABLE IF NOT EXISTS goal_blocks (goalId TEXT NOT NULL, blockId TEXT NOT NULL, position INTEGER NOT NULL, PRIMARY KEY(goalId, blockId), FOREIGN KEY(goalId) REFERENCES goals(id) ON UPDATE NO ACTION ON DELETE RESTRICT, FOREIGN KEY(blockId) REFERENCES blocks(id) ON UPDATE NO ACTION ON DELETE RESTRICT)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_goal_blocks_blockId ON goal_blocks(blockId)")
        db.execSQL("CREATE TABLE IF NOT EXISTS session_goals (sessionId TEXT NOT NULL, goalId TEXT NOT NULL, PRIMARY KEY(sessionId), FOREIGN KEY(sessionId) REFERENCES sessions(id) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(goalId) REFERENCES goals(id) ON UPDATE NO ACTION ON DELETE RESTRICT)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_session_goals_goalId ON session_goals(goalId)")
    }
}

class GoalRepository(private val db: BlockDatabase, private val now: () -> Long = System::currentTimeMillis) {
    val goals = db.goals().observeAll()
    private fun stamp(goal: Goal) = maxOf(now(), goal.updatedAt, goal.createdAt)
    suspend fun save(id: String?, title: String, description: String, progress: Int, notes: String): String = db.withTransaction {
        require(title.isNotBlank()) { "Enter a Goal title." }
        require(progress in 0..100) { "Enter progress from 0 to 100%." }
        val old = id?.let { checkNotNull(db.goals().find(it)?.goal) { "This Goal is unavailable." } }
        val active = activeGoals(db.goals().readAll())
        require(old != null || active.size < 5) { "Five Goals are active. Complete one before adding another." }
        val timestamp = now().coerceAtLeast(0)
        val goal = old?.copy(title = title.trim(), description = description.trim(), progress = progress,
            notes = notes.trim(), updatedAt = stamp(old)) ?: Goal(UUID.randomUUID().toString(), title.trim(),
            description.trim(), progress, notes.trim(), priority = active.size, createdAt = timestamp, updatedAt = timestamp)
        if (old == null) db.goals().insert(goal) else check(db.goals().update(goal) == 1)
        goal.id
    }
    suspend fun setCompleted(id: String, completed: Boolean) = db.withTransaction {
        val old = checkNotNull(db.goals().find(id)).goal
        if (old.completed != completed) {
            val active = activeGoals(db.goals().readAll())
            require(completed || active.size < 5) { "Five Goals are active. Complete one before reopening this Goal." }
            check(db.goals().update(old.copy(completed = completed, priority = if (completed) -1 else active.size, updatedAt = stamp(old))) == 1)
            activeGoals(db.goals().readAll()).forEachIndexed { i, b -> if (b.goal.priority != i) db.goals().update(b.goal.copy(priority = i, updatedAt = stamp(b.goal))) }
        }
    }
    suspend fun move(id: String, direction: Int) = db.withTransaction {
        require(direction == -1 || direction == 1)
        val active = activeGoals(db.goals().readAll()); val i = active.indexOfFirst { it.goal.id == id }
        require(i >= 0 && i + direction in active.indices)
        val a = active[i].goal; val b = active[i + direction].goal
        db.goals().update(a.copy(priority = b.priority, updatedAt = stamp(a)))
        db.goals().update(b.copy(priority = a.priority, updatedAt = stamp(b)))
    }
    suspend fun setBlocks(id: String, blockIds: List<String>) = db.withTransaction {
        val goal = checkNotNull(db.goals().find(id)).goal
        require(blockIds.size <= 20_000 && blockIds.distinct().size == blockIds.size)
        blockIds.forEach { require(db.blocks().find(it) != null) { "A selected Block is unavailable." } }
        db.goals().clearBlocks(id)
        blockIds.forEachIndexed { i, blockId -> db.goals().insert(GoalBlock(id, blockId, i)) }
        db.goals().update(goal.copy(updatedAt = stamp(goal)))
    }
}
