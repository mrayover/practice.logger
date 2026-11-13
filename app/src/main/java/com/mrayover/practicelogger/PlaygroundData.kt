package com.mrayover.practicelogger

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "playground_layouts", foreignKeys = [ForeignKey(
    entity = BlockCompletion::class, parentColumns = ["placementId"], childColumns = ["placementId"], onDelete = ForeignKey.CASCADE
)])
data class PlaygroundLayout(@PrimaryKey val placementId: String, val x: Double, val y: Double, val rotationDegrees: Double)

@Dao interface PlaygroundDao {
    @Query("SELECT * FROM playground_layouts ORDER BY placementId") suspend fun readAll(): List<PlaygroundLayout>
    @Query("SELECT * FROM playground_layouts ORDER BY placementId") fun observeAll(): Flow<List<PlaygroundLayout>>
    @Query("SELECT * FROM block_completions ORDER BY completedAt, placementId") suspend fun completions(): List<BlockCompletion>
    @Upsert suspend fun save(row: PlaygroundLayout)
}

internal class PlaygroundRepository(private val db: BlockDatabase) {
    suspend fun ensureLayouts() = db.withTransaction {
        val saved = db.playground().readAll()
        val all = initialPlaygroundLayouts(db.playground().completions(), saved)
        val ids = saved.mapTo(hashSetOf()) { it.placementId }
        all.filter { it.placementId !in ids }.forEach { db.playground().save(it) }
        all
    }
    suspend fun save(row: PlaygroundLayout) = db.withTransaction {
        val all = db.playground().readAll().filter { it.placementId != row.placementId } + row
        validatePlaygroundLayouts(all, db.playground().completions().mapTo(hashSetOf()) { it.placementId })
        db.playground().save(row)
    }
}

internal val PLAYGROUND_MIGRATION_26_27 = object : Migration(26, 27) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE playground_layouts (placementId TEXT NOT NULL, x REAL NOT NULL, y REAL NOT NULL, rotationDegrees REAL NOT NULL, PRIMARY KEY(placementId), FOREIGN KEY(placementId) REFERENCES block_completions(placementId) ON UPDATE NO ACTION ON DELETE CASCADE)")
    }
}
