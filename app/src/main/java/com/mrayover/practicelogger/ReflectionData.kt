package com.mrayover.practicelogger

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Entity(tableName = "block_completions", foreignKeys = [ForeignKey(
    entity = Placement::class, parentColumns = ["id"], childColumns = ["placementId"], onDelete = ForeignKey.CASCADE
)], indices = [Index("sessionId")])
data class BlockCompletion(
    @PrimaryKey val placementId: String,
    val sessionId: String,
    val sourceBlockId: String,
    val completedAt: Long,
    val goalId: String?,
    val priority: Int?,
    val colorArgb: Int?
)

@Entity(tableName = "block_reflections", foreignKeys = [ForeignKey(
    entity = BlockCompletion::class, parentColumns = ["placementId"], childColumns = ["placementId"], onDelete = ForeignKey.CASCADE
)])
data class BlockReflection(
    @PrimaryKey val placementId: String,
    val security: Int? = null,
    val focus: String = "",
    val difficulty: String = "",
    val progress: String = "",
    val revisit: String = ""
) {
    val hasAnswers get() = security != null || listOf(focus, difficulty, progress, revisit).any { it.isNotBlank() }
}

@Entity(tableName = "session_reflections", foreignKeys = [ForeignKey(
    entity = PracticeSession::class, parentColumns = ["id"], childColumns = ["sessionId"], onDelete = ForeignKey.CASCADE
)])
data class SessionReflection(
    @PrimaryKey val sessionId: String,
    val experience: String = "",
    val focus: String = "",
    val progress: String = "",
    val difficulty: String = "",
    val energy: String = ""
) {
    val hasAnswers get() = listOf(experience, focus, progress, difficulty, energy).any { it.isNotBlank() }
}

data class CompletedPlacement(
    @Embedded val completion: BlockCompletion,
    @Relation(parentColumn = "placementId", entityColumn = "placementId") val reflection: BlockReflection?
)

internal const val REFLECTION_ANSWER_LIMIT = 1000

internal val REFLECTION_MIGRATION_21_22 = object : Migration(21, 22) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE block_completions (placementId TEXT NOT NULL, sessionId TEXT NOT NULL, sourceBlockId TEXT NOT NULL, completedAt INTEGER NOT NULL, goalId TEXT, priority INTEGER, colorArgb INTEGER, PRIMARY KEY(placementId), FOREIGN KEY(placementId) REFERENCES placements(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
        db.execSQL("CREATE INDEX index_block_completions_sessionId ON block_completions(sessionId)")
        db.execSQL("CREATE TABLE block_reflections (placementId TEXT NOT NULL, security INTEGER, focus TEXT NOT NULL, difficulty TEXT NOT NULL, progress TEXT NOT NULL, revisit TEXT NOT NULL, PRIMARY KEY(placementId), FOREIGN KEY(placementId) REFERENCES block_completions(placementId) ON UPDATE NO ACTION ON DELETE CASCADE)")
        db.execSQL("CREATE TABLE session_reflections (sessionId TEXT NOT NULL, experience TEXT NOT NULL, focus TEXT NOT NULL, progress TEXT NOT NULL, difficulty TEXT NOT NULL, energy TEXT NOT NULL, PRIMARY KEY(sessionId), FOREIGN KEY(sessionId) REFERENCES sessions(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
    }
}
