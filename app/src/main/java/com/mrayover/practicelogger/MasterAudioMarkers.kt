package com.mrayover.practicelogger

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

@Entity(tableName = "media_markers", foreignKeys = [ForeignKey(entity = MediaAsset::class,
    parentColumns = ["id"], childColumns = ["mediaId"], onDelete = ForeignKey.RESTRICT)],
    indices = [Index(value = ["mediaId", "startMs"])])
data class MasterAudioMarker(@PrimaryKey val id: String, val mediaId: String, val type: String,
    val startMs: Long, val endMs: Long?, val label: String, val note: String)

@Dao interface MasterAudioMarkerDao {
    @Query("SELECT * FROM media_markers WHERE mediaId = :mediaId ORDER BY startMs, id")
    fun observe(mediaId: String): kotlinx.coroutines.flow.Flow<List<MasterAudioMarker>>
    @Query("SELECT * FROM media_markers WHERE mediaId = :mediaId ORDER BY startMs, id")
    suspend fun read(mediaId: String): List<MasterAudioMarker>
    @Query("SELECT * FROM media_markers WHERE id = :id") suspend fun find(id: String): MasterAudioMarker?
    @Insert suspend fun insert(marker: MasterAudioMarker)
    @Update suspend fun update(marker: MasterAudioMarker): Int
    @Query("DELETE FROM media_markers WHERE id = :id AND mediaId = :mediaId") suspend fun remove(id: String, mediaId: String): Int
    @Query("DELETE FROM media_markers WHERE mediaId = :mediaId") suspend fun removeOwned(mediaId: String)
}

val MARKER_MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `media_markers` (`id` TEXT NOT NULL, `mediaId` TEXT NOT NULL, `type` TEXT NOT NULL, `startMs` INTEGER NOT NULL, `endMs` INTEGER, `label` TEXT NOT NULL, `note` TEXT NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`mediaId`) REFERENCES `media_assets`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_media_markers_mediaId_startMs` ON `media_markers` (`mediaId`, `startMs`)")
    }
}

fun validAudioRange(start: Long, end: Long, duration: Long) = start >= 0 && start < end && end <= duration
fun validateMasterMarker(marker: MasterAudioMarker, duration: Long) {
    require(marker.startMs in 0..duration && duration > 0) { "Position must be inside the recording." }
    require((marker.type == "POINT" && marker.endMs == null) || (marker.type == "SECTION" && marker.endMs != null && validAudioRange(marker.startMs, marker.endMs, duration))) { "A section must end after its start, inside the recording." }
    MediaLibraryRepository.title(marker.label)
    require(marker.note.length <= 8000) { "Use notes up to 8000 characters." }
}

class MasterAudioMarkerRepository(private val db: BlockDatabase) {
    fun observe(mediaId: String) = db.markers().observe(mediaId)
    suspend fun save(mediaId: String, id: String?, startMs: Long, endMs: Long?, label: String, note: String): MasterAudioMarker = withContext(Dispatchers.IO) {
        db.withTransaction {
            val asset = checkNotNull(db.media().find(mediaId)) { "Recording no longer exists." }
            require(asset.category == "AUDIO")
            val marker = MasterAudioMarker(id ?: UUID.randomUUID().toString(), mediaId, if (endMs == null) "POINT" else "SECTION", startMs, endMs, MediaLibraryRepository.title(label), note.trim())
            validateMasterMarker(marker, checkNotNull(asset.durationMs))
            if (id == null) db.markers().insert(marker) else {
                check(db.markers().find(id)?.mediaId == mediaId) { "Marker no longer exists." }
                check(db.markers().update(marker) == 1)
            }
            marker
        }
    }
    suspend fun delete(mediaId: String, id: String) = db.markers().remove(id, mediaId)
}
