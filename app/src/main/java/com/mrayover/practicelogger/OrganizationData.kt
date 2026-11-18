package com.mrayover.practicelogger

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * Organization is metadata, not ownership. A Block has one optional category and
 * primary Source. A Source can group many canonical Library assets without copying them.
 */
@Entity(tableName = "block_categories", indices = [Index("name")])
data class BlockCategory(@PrimaryKey val id: String, val name: String, val createdAt: Long)

@Entity(tableName = "study_sources", indices = [Index("title")])
data class StudySource(
    @PrimaryKey val id: String,
    val title: String,
    val creator: String = "",
    val kind: String = "BOOK",
    val createdAt: Long
)

@Entity(
    tableName = "study_source_media",
    primaryKeys = ["sourceId", "mediaId"],
    foreignKeys = [
        ForeignKey(entity = StudySource::class, parentColumns = ["id"], childColumns = ["sourceId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = MediaAsset::class, parentColumns = ["id"], childColumns = ["mediaId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("mediaId")]
)
data class StudySourceMedia(val sourceId: String, val mediaId: String)

enum class SourceKind(val label: String) {
    BOOK("Book"), VIDEO("Video"), LESSON("Lesson"), RECORDING("Recording"), WEBSITE("Website"), ORIGINAL("Original material"), OTHER("Other")
}

@Dao
interface OrganizationDao {
    @Query("SELECT * FROM block_categories ORDER BY name COLLATE NOCASE, id")
    fun categories(): Flow<List<BlockCategory>>
    @Query("SELECT * FROM study_sources ORDER BY title COLLATE NOCASE, id")
    fun sources(): Flow<List<StudySource>>
    @Query("SELECT * FROM study_source_media ORDER BY sourceId, mediaId")
    fun sourceMedia(): Flow<List<StudySourceMedia>>
    @Query("SELECT * FROM block_categories WHERE id = :id")
    suspend fun category(id: String): BlockCategory?
    @Query("SELECT * FROM study_sources WHERE id = :id")
    suspend fun source(id: String): StudySource?
    @Query("SELECT id FROM block_categories WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun categoryWithName(name: String): String?
    @Insert suspend fun addCategory(category: BlockCategory)
    @Insert suspend fun addSource(source: StudySource)
    @Query("UPDATE block_categories SET name = :name WHERE id = :id")
    suspend fun renameCategory(id: String, name: String): Int
    @Query("UPDATE study_sources SET title = :title, creator = :creator, kind = :kind WHERE id = :id")
    suspend fun updateSource(id: String, title: String, creator: String, kind: String): Int
    @Query("UPDATE blocks SET categoryId = NULL WHERE categoryId = :id")
    suspend fun clearCategory(id: String)
    @Query("UPDATE blocks SET sourceId = NULL, sourceLocation = '' WHERE sourceId = :id")
    suspend fun clearSource(id: String)
    @Query("DELETE FROM block_categories WHERE id = :id")
    suspend fun removeCategory(id: String): Int
    @Query("DELETE FROM study_sources WHERE id = :id")
    suspend fun removeSource(id: String): Int
    @Query("SELECT sourceId FROM study_source_media WHERE mediaId = :mediaId")
    suspend fun mediaSourceIds(mediaId: String): List<String>
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun attachMedia(link: StudySourceMedia): Long
    @Query("DELETE FROM study_source_media WHERE sourceId = :sourceId AND mediaId = :mediaId")
    suspend fun detachMedia(sourceId: String, mediaId: String): Int
}

class OrganizationRepository(private val db: BlockDatabase, private val now: () -> Long = System::currentTimeMillis) {
    val categories = db.organization().categories()
    val sources = db.organization().sources()
    val mediaLinks = db.organization().sourceMedia()
    private fun title(value: String): String = value.trim().also { require(it.length in 1..120) { "Enter a name of 1–120 characters." } }
    private fun kind(value: String) = SourceKind.valueOf(value).name

    suspend fun createCategory(name: String): BlockCategory = db.withTransaction {
        val clean = title(name)
        require(db.organization().categoryWithName(clean) == null) { "This category already exists." }
        BlockCategory(UUID.randomUUID().toString(), clean, now()).also { db.organization().addCategory(it) }
    }
    suspend fun renameCategory(id: String, name: String) = db.withTransaction {
        val clean = title(name)
        require(db.organization().categoryWithName(clean)?.let { it != id } != true) { "This category already exists." }
        check(db.organization().renameCategory(id, clean) == 1) { "Category no longer exists." }
    }
    suspend fun deleteCategory(id: String) = db.withTransaction {
        db.organization().clearCategory(id)
        check(db.organization().removeCategory(id) == 1) { "Category no longer exists." }
    }
    suspend fun createSource(title: String, creator: String = "", kind: String = "BOOK"): StudySource = db.withTransaction {
        val clean = title(title)
        require(creator.length <= 160) { "Creator is too long." }
        StudySource(UUID.randomUUID().toString(), clean, creator.trim(), kind(kind), now()).also {
            db.organization().addSource(it)
        }
    }
    suspend fun updateSource(id: String, title: String, creator: String, kind: String) = db.withTransaction {
        val clean = title(title)
        require(creator.length <= 160) { "Creator is too long." }
        check(db.organization().updateSource(id, clean, creator.trim(), kind(kind)) == 1) { "Source no longer exists." }
    }
    suspend fun deleteSource(id: String) = db.withTransaction {
        // Deletes organizational links only, never canonical media, recordings or Blocks.
        db.organization().clearSource(id)
        check(db.organization().removeSource(id) == 1) { "Source no longer exists." }
    }
    suspend fun setMediaSources(mediaId: String, sources: Set<String>) = db.withTransaction {
        requireNotNull(db.media().find(mediaId)) { "Library file no longer exists." }
        val current = db.organization().mediaSourceIds(mediaId).toSet()
        val next = sources.toSet()
        for (id in next) requireNotNull(db.organization().source(id)) { "Source no longer exists." }
        for (id in current - next) db.organization().detachMedia(id, mediaId)
        for (id in next - current) db.organization().attachMedia(StudySourceMedia(id, mediaId))
    }
}

val ORGANIZATION_MIGRATION_28_29 = object : Migration(28, 29) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE blocks ADD COLUMN categoryId TEXT")
        db.execSQL("ALTER TABLE blocks ADD COLUMN sourceId TEXT")
        db.execSQL("ALTER TABLE blocks ADD COLUMN sourceLocation TEXT NOT NULL DEFAULT ''")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_blocks_categoryId ON blocks(categoryId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_blocks_sourceId ON blocks(sourceId)")
        db.execSQL("CREATE TABLE IF NOT EXISTS block_categories (id TEXT NOT NULL, name TEXT NOT NULL, createdAt INTEGER NOT NULL, PRIMARY KEY(id))")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_block_categories_name ON block_categories(name)")
        db.execSQL("CREATE TABLE IF NOT EXISTS study_sources (id TEXT NOT NULL, title TEXT NOT NULL, creator TEXT NOT NULL, kind TEXT NOT NULL, createdAt INTEGER NOT NULL, PRIMARY KEY(id))")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_study_sources_title ON study_sources(title)")
        db.execSQL("""CREATE TABLE IF NOT EXISTS study_source_media (
            sourceId TEXT NOT NULL, mediaId TEXT NOT NULL, PRIMARY KEY(sourceId, mediaId),
            FOREIGN KEY(sourceId) REFERENCES study_sources(id) ON UPDATE NO ACTION ON DELETE CASCADE,
            FOREIGN KEY(mediaId) REFERENCES media_assets(id) ON UPDATE NO ACTION ON DELETE CASCADE
        )""")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_study_source_media_mediaId ON study_source_media(mediaId)")
    }
}
