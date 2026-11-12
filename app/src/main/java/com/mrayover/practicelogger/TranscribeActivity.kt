package com.mrayover.practicelogger

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "transcribe_activities",
    foreignKeys = [
        ForeignKey(entity = Goal::class, parentColumns = ["id"], childColumns = ["goalId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = MediaAsset::class, parentColumns = ["id"], childColumns = ["audioId"], onDelete = ForeignKey.SET_NULL)
    ],
    indices = [Index("goalId"), Index("audioId")]
)
data class TranscribeActivity(
    @PrimaryKey val id: String,
    val startedAt: Long,
    val completedAt: Long? = null,
    val activeMs: Long = 0,
    val goalId: String? = null,
    val audioId: String? = null
)

@Dao
interface TranscribeActivityDao {
    @Query(PRACTICE_FACTS_SQL + " ORDER BY startedAt DESC, id")
    fun observeFacts(): Flow<List<PracticeFact>>

    // An unfinished row has no durable running anchor and always reopens paused.
    @Query("SELECT * FROM transcribe_activities WHERE completedAt IS NULL LIMIT 1")
    suspend fun unfinished(): TranscribeActivity?

    @Query("SELECT * FROM transcribe_activities WHERE id = :id")
    suspend fun find(id: String): TranscribeActivity?

    @Insert
    suspend fun insert(activity: TranscribeActivity)

    @Query("""
        UPDATE transcribe_activities
        SET activeMs = :activeMs, goalId = :goalId, completedAt = :completedAt,
            audioId = (SELECT id FROM media_assets WHERE id = :audioId AND category = 'AUDIO')
        WHERE id = :id AND completedAt IS NULL
    """)
    suspend fun save(id: String, activeMs: Long, goalId: String?, audioId: String?, completedAt: Long?): Int

    @Query("SELECT EXISTS(SELECT 1 FROM transcribe_activities)")
    suspend fun hasEntries(): Boolean

    @Query("SELECT startedAt, SUM(activeMs) AS activeMs FROM (" + PRACTICE_FACTS_SQL + ") GROUP BY startedAt")
    fun observePracticeTime(): Flow<List<PracticeTimeEntry>>
}

data class PracticeTimeEntry(val startedAt: Long, val activeMs: Long)

internal val TRANSCRIBE_MIGRATION_15_16 = object : Migration(15, 16) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS transcribe_activities (
                id TEXT NOT NULL PRIMARY KEY, startedAt INTEGER NOT NULL,
                completedAt INTEGER, activeMs INTEGER NOT NULL, goalId TEXT, audioId TEXT,
                FOREIGN KEY(goalId) REFERENCES goals(id) ON UPDATE NO ACTION ON DELETE RESTRICT,
                FOREIGN KEY(audioId) REFERENCES media_assets(id) ON UPDATE NO ACTION ON DELETE SET NULL
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS index_transcribe_activities_goalId ON transcribe_activities(goalId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_transcribe_activities_audioId ON transcribe_activities(audioId)")
    }
}
