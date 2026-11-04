package com.mrayover.practicelogger

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import java.util.UUID

@Entity(tableName = "block_audio_state", primaryKeys = ["blockId", "role", "mediaId"],
    foreignKeys = [ForeignKey(entity = BlockMediaLink::class, parentColumns = ["blockId", "role", "mediaId"],
        childColumns = ["blockId", "role", "mediaId"], onDelete = ForeignKey.CASCADE)])
data class BlockAudioState(val blockId: String, val mediaId: String, val positionMs: Long, val speedPercent: Int,
    val aMs: Long, val bMs: Long, val repeat: Boolean, val selectedMarkerId: String?, val role: String = "AUDIO")

@Entity(tableName = "block_audio_markers", primaryKeys = ["blockId", "role", "mediaId", "id"],
    foreignKeys = [ForeignKey(entity = BlockMediaLink::class, parentColumns = ["blockId", "role", "mediaId"],
        childColumns = ["blockId", "role", "mediaId"], onDelete = ForeignKey.CASCADE)])
data class BlockAudioMarker(val blockId: String, val mediaId: String, val id: String,
    val startMs: Long, val endMs: Long?, val label: String, val note: String,
    val originalMaster: String?, val hidden: Boolean = false, val role: String = "AUDIO") {
    fun presentation() = MasterAudioMarker(id, mediaId, if (endMs == null) "POINT" else "SECTION", startMs, endMs, label, note)
}

@Dao
interface BlockAudioDao {
    @Query("SELECT * FROM block_audio_state WHERE blockId = :block AND role = 'AUDIO' AND mediaId = :media")
    suspend fun state(block: String, media: String): BlockAudioState?
    @Upsert suspend fun save(state: BlockAudioState)
    @Query("SELECT * FROM block_audio_markers WHERE blockId = :block AND role = 'AUDIO' AND mediaId = :media")
    suspend fun read(block: String, media: String): List<BlockAudioMarker>
    @Query("SELECT * FROM block_audio_markers WHERE blockId = :block AND role = 'AUDIO' AND mediaId = :media AND id = :id")
    suspend fun marker(block: String, media: String, id: String): BlockAudioMarker?
    @Query("SELECT EXISTS(SELECT 1 FROM block_audio_markers WHERE blockId = :block AND role = 'AUDIO' AND mediaId = :media)")
    suspend fun hasMarkers(block: String, media: String): Boolean
    @Upsert suspend fun save(marker: BlockAudioMarker)
    @Delete suspend fun remove(marker: BlockAudioMarker)
}

internal fun installBlockAudioConstraints(db: SupportSQLiteDatabase) {
    // A customized version keeps its ID and becomes local-only when the master is deleted.
    db.execSQL("""CREATE TRIGGER IF NOT EXISTS retire_block_master AFTER DELETE ON media_markers BEGIN
        DELETE FROM block_audio_markers WHERE mediaId = OLD.mediaId AND id = OLD.id AND hidden = 1 AND originalMaster IS NOT NULL;
        UPDATE block_audio_markers SET originalMaster = NULL
            WHERE mediaId = OLD.mediaId AND id = OLD.id AND originalMaster IS NOT NULL;
    END""")
}

val BLOCK_AUDIO_MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_block_media_links_blockId_role_mediaId ON block_media_links (blockId, role, mediaId)")
        db.execSQL("""CREATE TABLE IF NOT EXISTS block_audio_state (
            blockId TEXT NOT NULL, mediaId TEXT NOT NULL, positionMs INTEGER NOT NULL, speedPercent INTEGER NOT NULL,
            aMs INTEGER NOT NULL, bMs INTEGER NOT NULL, repeat INTEGER NOT NULL, selectedMarkerId TEXT, role TEXT NOT NULL,
            PRIMARY KEY (blockId, role, mediaId),
            FOREIGN KEY (blockId, role, mediaId) REFERENCES block_media_links (blockId, role, mediaId) ON UPDATE NO ACTION ON DELETE CASCADE)""")
        db.execSQL("""CREATE TABLE IF NOT EXISTS block_audio_markers (
            blockId TEXT NOT NULL, mediaId TEXT NOT NULL, id TEXT NOT NULL, startMs INTEGER NOT NULL, endMs INTEGER,
            label TEXT NOT NULL, note TEXT NOT NULL, originalMaster TEXT, hidden INTEGER NOT NULL, role TEXT NOT NULL,
            PRIMARY KEY (blockId, role, mediaId, id),
            FOREIGN KEY (blockId, role, mediaId) REFERENCES block_media_links (blockId, role, mediaId) ON UPDATE NO ACTION ON DELETE CASCADE)""")
        installBlockAudioConstraints(db)
    }
}

enum class BlockMarkerOwnership(val label: String) {
    MASTER("Master"), CUSTOMIZED("Customized in This Block"), LOCAL("Local Only"), HIDDEN("Hidden in This Block")
}

data class BlockMarker(val marker: MasterAudioMarker, val ownership: BlockMarkerOwnership,
    val local: BlockAudioMarker? = null)

data class BlockMarkerSet(val visible: List<BlockMarker> = emptyList(), val hidden: List<BlockMarker> = emptyList())

internal fun effectiveBlockMarkers(masters: List<MasterAudioMarker>, locals: List<BlockAudioMarker>): BlockMarkerSet {
    val overrides = locals.filter { it.originalMaster != null }.associateBy { it.id }
    val masterIds = masters.mapTo(hashSetOf()) { it.id }
    val inherited = masters.map { master ->
        val local = overrides[master.id]
        when {
            local == null -> BlockMarker(master, BlockMarkerOwnership.MASTER)
            local.hidden -> BlockMarker(master, BlockMarkerOwnership.HIDDEN, local)
            else -> BlockMarker(local.presentation(), BlockMarkerOwnership.CUSTOMIZED, local)
        }
    }
    val independent = locals.filter { !it.hidden && (it.originalMaster == null || it.id !in masterIds) }
        .map { BlockMarker(it.presentation(), BlockMarkerOwnership.LOCAL, it) }
    val (hidden, visible) = (inherited + independent).sortedWith(
        compareBy({ it.marker.startMs }, { it.marker.id })).partition { it.ownership == BlockMarkerOwnership.HIDDEN }
    return BlockMarkerSet(visible, hidden)
}

private fun MasterAudioMarker.snapshot(): String = JSONArray()
    .put(type).put(startMs).put(endMs ?: org.json.JSONObject.NULL).put(label).put(note).toString()

class MasterMarkerConflict(val current: MasterAudioMarker) : IllegalStateException("The master marker changed. Review its current version before publishing.")

class BlockAudioRepository(private val db: BlockDatabase) {
    private val audio = db.blockAudio()
    private val masters = db.markers()

    fun observe(block: String, media: String) =
        db.invalidationTracker.createFlow("media_markers", "block_audio_markers").map {
            db.withTransaction { effectiveBlockMarkers(masters.read(media), audio.read(block, media)) }
        }

    suspend fun state(block: String, media: String) = audio.state(block, media)

    suspend fun saveState(state: BlockAudioState) = db.withTransaction {
        if (linked(state.blockId, state.mediaId) && audio.state(state.blockId, state.mediaId) != state) audio.save(state)
    }

    private suspend fun linked(block: String, media: String) =
        db.blockMedia().find(block, MediaRole.AUDIO.name)?.mediaId == media

    suspend fun saveMarker(block: String, media: String, existing: BlockMarker?, draft: MasterAudioMarker,
        hide: Boolean = false): String = db.withTransaction {
        check(linked(block, media)) { "This Block’s audio reference changed." }
        val id = existing?.marker?.id ?: UUID.randomUUID().toString()
        val local = audio.marker(block, media, id)
        check(local == existing?.local) { "This Block’s marker changed. Open it again to edit." }
        val origin = if (local != null) local.originalMaster else if (existing != null) {
            check(masters.find(id)?.mediaId == media) { "The master marker was removed." }
            existing.marker.snapshot()
        } else null
        val marker = draft.copy(id = id, mediaId = media, label = MediaLibraryRepository.title(draft.label), note = draft.note.trim())
        validateMasterMarker(marker, checkNotNull(db.media().find(media)?.durationMs))
        audio.save(BlockAudioMarker(block, media, id, marker.startMs, marker.endMs, marker.label, marker.note, origin, hide))
        id
    }

    suspend fun discard(local: BlockAudioMarker, reset: Boolean) = db.withTransaction {
        check(linked(local.blockId, local.mediaId) && audio.marker(local.blockId, local.mediaId, local.id) == local) {
            "This Block’s marker changed. Open it again."
        }
        if (reset) check(local.originalMaster != null && masters.find(local.id)?.mediaId == local.mediaId) {
            "The master marker was removed."
        }
        audio.remove(local)
    }

    suspend fun publish(local: BlockAudioMarker, reviewed: MasterAudioMarker? = null): String = db.withTransaction {
        check(linked(local.blockId, local.mediaId) && audio.marker(local.blockId, local.mediaId, local.id) == local && !local.hidden) {
            "This Block’s marker changed. Open it again to publish."
        }
        val published = if (local.originalMaster == null) {
            local.presentation().copy(id = UUID.randomUUID().toString())
        } else {
            val current = checkNotNull(masters.find(local.id)?.takeIf { it.mediaId == local.mediaId }) {
                "The master marker was removed. Reopen this marker to add it to Master."
            }
            if (current.snapshot() != (reviewed?.snapshot() ?: local.originalMaster)) throw MasterMarkerConflict(current)
            local.presentation()
        }
        validateMasterMarker(published, checkNotNull(db.media().find(local.mediaId)?.durationMs))
        if (local.originalMaster == null) masters.insert(published) else check(masters.update(published) == 1)
        audio.state(local.blockId, local.mediaId)?.let { state ->
            if (state.selectedMarkerId == local.id) audio.save(state.copy(selectedMarkerId = published.id))
        }
        audio.remove(local)
        published.id
    }
}
