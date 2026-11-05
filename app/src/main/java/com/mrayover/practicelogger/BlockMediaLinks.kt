package com.mrayover.practicelogger

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow

enum class MediaRole { VISUAL, AUDIO;
    fun accepts(category: String) = when (this) { VISUAL -> category in setOf("IMAGE", "PDF"); AUDIO -> category == "AUDIO" }
}

@Entity(tableName = "block_media_links", primaryKeys = ["blockId", "role"],
    indices = [Index("mediaId"), Index(value = ["blockId", "role", "mediaId"], unique = true)], foreignKeys = [
        ForeignKey(entity = Block::class, parentColumns = ["id"], childColumns = ["blockId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = MediaAsset::class, parentColumns = ["id"], childColumns = ["mediaId"], onDelete = ForeignKey.RESTRICT)])
data class BlockMediaLink(val blockId: String, val role: String, val mediaId: String)

@Dao interface BlockMediaLinkDao {
    @Query("SELECT * FROM block_media_links") fun observeAll(): Flow<List<BlockMediaLink>>
    @Query("SELECT * FROM block_media_links") suspend fun readAll(): List<BlockMediaLink>
    @Query("SELECT * FROM block_media_links WHERE blockId = :blockId AND role = :role") suspend fun find(blockId: String, role: String): BlockMediaLink?
    @Upsert suspend fun save(link: BlockMediaLink)
    @Query("DELETE FROM block_media_links WHERE blockId = :blockId AND role = :role") suspend fun remove(blockId: String, role: String): Int
}

// Room exports the table/FKs. These triggers enforce role/type compatibility on raw writes too.
internal fun installBlockMediaConstraints(db: SupportSQLiteDatabase) {
    for (event in listOf("INSERT", "UPDATE")) db.execSQL("""CREATE TRIGGER IF NOT EXISTS block_media_type_${event.lowercase()}
        BEFORE $event ON block_media_links WHEN NOT EXISTS (
            SELECT 1 FROM media_assets WHERE id = NEW.mediaId AND
            ((NEW.role = 'VISUAL' AND category IN ('IMAGE','PDF')) OR (NEW.role = 'AUDIO' AND category = 'AUDIO')))
        BEGIN SELECT RAISE(ABORT, 'Incompatible Block media role'); END""")
    db.execSQL("""CREATE TRIGGER IF NOT EXISTS linked_media_type_update BEFORE UPDATE OF category ON media_assets
        WHEN EXISTS (SELECT 1 FROM block_media_links WHERE mediaId = OLD.id AND
        NOT ((role = 'VISUAL' AND NEW.category IN ('IMAGE','PDF')) OR (role = 'AUDIO' AND NEW.category = 'AUDIO')))
        BEGIN SELECT RAISE(ABORT, 'Media type is linked to a Block'); END""")
}
val BLOCK_MEDIA_MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""CREATE TABLE IF NOT EXISTS block_media_links (blockId TEXT NOT NULL, role TEXT NOT NULL, mediaId TEXT NOT NULL,
            PRIMARY KEY(blockId, role), FOREIGN KEY(blockId) REFERENCES blocks(id) ON UPDATE NO ACTION ON DELETE RESTRICT,
            FOREIGN KEY(mediaId) REFERENCES media_assets(id) ON UPDATE NO ACTION ON DELETE RESTRICT)""")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_block_media_links_mediaId ON block_media_links(mediaId)")
        installBlockMediaConstraints(db)
    }
}

internal class ConfirmAudioLoss(val mediaId: String) : IllegalStateException()

/** Only associations live here. Canonical storage and all playback remain in the existing Library. */
class BlockMediaRepository(private val db: BlockDatabase, private val library: MediaLibraryRepository,
    private val legacy: ImageReferenceRepository) {
    val links = db.blockMedia().observeAll()
    suspend fun link(blockId: String, role: MediaRole, mediaId: String, replaceLegacy: Boolean = false, confirmedAudioId: String? = null) =
        linkChecked(blockId, role, mediaId, replaceLegacy, confirmedAudioId)

    internal suspend fun linkChecked(blockId: String, role: MediaRole, mediaId: String,
        replaceLegacy: Boolean = false, confirmedAudioId: String? = null, reviewedVisual: VisualChangeCheck? = null
    ) = withContext(Dispatchers.IO) {
        // Share the legacy file mutex so retirement cannot race a legacy import or recovery.
        legacy.commitCanonicalLink(blockId, role == MediaRole.VISUAL, replaceLegacy) {
            checkNotNull(db.blocks().find(blockId)) { "This Block is no longer available." }
            val asset = checkNotNull(db.media().find(mediaId)) { "This Library item is no longer available." }
            require(role.accepts(asset.category)) { "Choose a compatible Library item." }
            check(library.available(asset)) { "The original is missing or changed. Import your original again." }
            val previous = db.blockMedia().find(blockId, role.name)
            if (role == MediaRole.AUDIO) check(confirmedAudioId == null || confirmedAudioId == previous?.mediaId) {
                "The audio reference changed. Review the current reference and try again."
            }
            if (role == MediaRole.AUDIO) saveAudioLink(blockId, mediaId, previous?.mediaId, confirmedAudioId)
            else {
                val current = db.visualIdentity(blockId)
                check(reviewedVisual == null || reviewedVisual.identity == current) {
                    "The visual reference changed. Review it and try again."
                }
                if (current != "library:$mediaId") {
                    db.discardVisualMarkings(blockId, current, reviewedVisual)
                    db.blockMedia().save(BlockMediaLink(blockId, role.name, mediaId))
                }
            }
        }
    }
    suspend fun unlink(blockId: String, role: MediaRole, confirmedAudioId: String? = null) =
        unlinkChecked(blockId, role, confirmedAudioId)

    internal suspend fun unlinkChecked(blockId: String, role: MediaRole, confirmedAudioId: String? = null,
        reviewedVisual: VisualChangeCheck? = null
    ) = withContext(Dispatchers.IO) {
        db.withTransaction {
            val previous = db.blockMedia().find(blockId, role.name)
            if (role == MediaRole.AUDIO) check(confirmedAudioId == null || confirmedAudioId == previous?.mediaId) {
                "The audio reference changed. Review the current reference and try again."
            }
            if (role == MediaRole.AUDIO && previous != null) removeAudio(blockId, previous.mediaId, confirmedAudioId)
            else {
                db.discardVisualMarkings(blockId, previous?.let { "library:${it.mediaId}" }, reviewedVisual)
                db.blockMedia().remove(blockId, role.name)
            }
        }
    }

    // Runs inside the Library import transaction. Only the audio link owns local audio state.
    internal suspend fun attachRecordedAudio(blockId: String, mediaId: String, expectedAudioId: String?, confirmedAudioId: String?) {
        checkNotNull(db.blocks().find(blockId)) { "This Block is no longer available." }
        val previous = db.blockMedia().find(blockId, MediaRole.AUDIO.name)?.mediaId
        check(previous == expectedAudioId) { "The audio reference changed. Close recording and review the current reference." }
        saveAudioLink(blockId, mediaId, previous, confirmedAudioId)
    }

    private suspend fun saveAudioLink(blockId: String, mediaId: String, previous: String?, confirmedAudioId: String?) {
        if (previous != null && previous != mediaId) removeAudio(blockId, previous, confirmedAudioId)
        db.blockMedia().save(BlockMediaLink(blockId, MediaRole.AUDIO.name, mediaId))
    }

    private suspend fun removeAudio(block: String, media: String, confirmed: String?): Int {
        val saved = db.blockAudio().state(block, media)
        val duration = db.media().find(media)?.durationMs
        val customized = saved != null && (saved.positionMs != 0L || saved.speedPercent != 100 ||
            saved.aMs != 0L || saved.bMs != duration || saved.repeat || saved.selectedMarkerId != null)
        if (confirmed != media && (customized || db.blockAudio().hasMarkers(block, media))) throw ConfirmAudioLoss(media)
        return db.blockMedia().remove(block, MediaRole.AUDIO.name)
    }
}

data class BlockVisualReference(val blockId: String, val identity: String, val mimeType: String,
    val mediaId: String? = null, val legacy: BlockImage? = null)

/** Canonical when linked; otherwise the existing legacy attachment. No startup copying/adoption. */
fun resolveBlockVisuals(legacy: List<BlockImage>, links: List<BlockMediaLink>, assets: List<MediaAsset>): List<BlockVisualReference> {
    val canonical = links.filter { it.role == MediaRole.VISUAL.name }
    val linkedBlocks = canonical.map { it.blockId }.toSet()
    return canonical.map { link -> BlockVisualReference(link.blockId, "library:${link.mediaId}",
        assets.find { it.id == link.mediaId }?.mimeType ?: "", mediaId = link.mediaId) } +
        legacy.filter { it.blockId !in linkedBlocks }.map { BlockVisualReference(it.blockId, "legacy:${it.fileName}", it.mimeType, legacy = it) }
}
