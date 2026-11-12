package com.mrayover.practicelogger

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow

enum class MediaRole { VISUAL, AUDIO;
    fun accepts(category: String) = when (this) { VISUAL -> category in setOf("IMAGE", "PDF"); AUDIO -> category == "AUDIO" }
}

@Entity(tableName = "block_media_links", primaryKeys = ["blockId", "role", "mediaId"],
    indices = [Index("mediaId"), Index(value = ["blockId", "role", "mediaId"], unique = true),
        Index(value = ["blockId", "role", "position", "mediaId"])], foreignKeys = [
        ForeignKey(entity = Block::class, parentColumns = ["id"], childColumns = ["blockId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = MediaAsset::class, parentColumns = ["id"], childColumns = ["mediaId"], onDelete = ForeignKey.RESTRICT)])
data class BlockMediaLink(val blockId: String, val role: String, val mediaId: String,
    @ColumnInfo(defaultValue = "0") val position: Int = 0)

@Dao interface BlockMediaLinkDao {
    @Query("SELECT * FROM block_media_links ORDER BY blockId, role, position, mediaId") fun observeAll(): Flow<List<BlockMediaLink>>
    @Query("SELECT * FROM block_media_links ORDER BY blockId, role, position, mediaId") suspend fun readAll(): List<BlockMediaLink>
    @Query("SELECT * FROM block_media_links WHERE blockId = :blockId AND role = :role ORDER BY position, mediaId LIMIT 1") suspend fun find(blockId: String, role: String): BlockMediaLink?
    @Query("SELECT * FROM block_media_links WHERE blockId = :blockId AND role = :role ORDER BY position, mediaId") suspend fun list(blockId: String, role: String): List<BlockMediaLink>
    @Query("SELECT EXISTS(SELECT 1 FROM block_media_links WHERE blockId = :blockId AND role = :role AND mediaId = :mediaId)") suspend fun contains(blockId: String, role: String, mediaId: String): Boolean
    @Upsert suspend fun save(link: BlockMediaLink)
    @Query("DELETE FROM block_media_links WHERE blockId = :blockId AND role = :role AND mediaId = :mediaId") suspend fun removeExact(blockId: String, role: String, mediaId: String): Int
    @Query("UPDATE block_media_links SET position = :position WHERE blockId = :blockId AND role = :role AND mediaId = :mediaId") suspend fun move(blockId: String, role: String, mediaId: String, position: Int)
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
    suspend fun ordered(block: String, role: MediaRole): List<String> = db.withTransaction { db.referenceOrder(block, role) }
    suspend fun contains(block: String, role: MediaRole, media: String): Boolean = db.blockMedia().contains(block, role.name, media)

    suspend fun add(block: String, role: MediaRole, media: String, reviewed: List<String>? = null) =
        withContext(Dispatchers.IO) { withPortableMediaLocks { db.withTransaction {
            val order = db.referenceOrder(block, role)
            check(reviewed == null || reviewed == order) { "References changed. Review them and try again." }
            val asset = checkNotNull(db.media().find(media)) { "This Library item is unavailable." }
            require(role.accepts(asset.category)) { "Choose a compatible Library item." }
            check(library.available(asset)) { "The original is missing or changed." }
            appendChecked(block, role, media)
        } } }

    internal suspend fun appendChecked(block: String, role: MediaRole, media: String) {
        checkNotNull(db.blocks().find(block)) { "This Block is unavailable." }
        val asset = checkNotNull(db.media().find(media))
        require(role.accepts(asset.category))
        if (!db.blockMedia().contains(block, role.name, media)) {
            db.blockMedia().save(BlockMediaLink(block, role.name, media, db.referenceOrder(block, role).size))
        }
    }

    suspend fun reorder(block: String, role: MediaRole, reviewed: List<String>, desired: List<String>) = db.withTransaction {
        check(db.referenceOrder(block, role) == reviewed) { "References changed. Review them and try again." }
        require(desired.size == reviewed.size && desired.toSet().size == desired.size && desired.toSet() == reviewed.toSet()) {
            "Reorder must contain every reference exactly once."
        }
        db.writeReferenceOrder(block, role, desired)
    }

    internal suspend fun removeReference(block: String, role: MediaRole, identity: String, reviewed: List<String>,
        confirmedAudioId: String? = null, reviewedVisual: VisualChangeCheck? = null) =
        withContext(Dispatchers.IO) { withPortableMediaLocks { db.withTransaction {
            check(db.referenceOrder(block, role) == reviewed && identity in reviewed) { "References changed. Review them and try again." }
            if (role == MediaRole.AUDIO) {
                require(identity.startsWith("library:"))
                removeAudio(block, identity.removePrefix("library:"), confirmedAudioId)
            } else {
                db.discardVisualMarkings(block, identity, reviewedVisual)
                if (identity.startsWith("library:")) db.blockMedia().removeExact(block, role.name, identity.removePrefix("library:"))
                else { require(identity.startsWith("legacy:")); db.images().remove(block) }
            }
            db.writeReferenceOrder(block, role, reviewed.filter { it != identity })
        } } }
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
                    db.replaceVisual(blockId, mediaId, reviewedVisual ?: VisualChangeCheck(current, false))
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
            if (role == MediaRole.AUDIO) {
                if (previous != null) removeAudio(blockId, previous.mediaId, confirmedAudioId)
            } else {
                db.replaceVisual(blockId, null, reviewedVisual ?: VisualChangeCheck(db.visualIdentity(blockId), false))
            }
        }
    }

    // Runs inside the Library import transaction. Only the audio link owns local audio state.
    internal suspend fun attachRecordedAudio(blockId: String, mediaId: String, expectedAudioId: String?, confirmedAudioId: String?) {
        checkNotNull(db.blocks().find(blockId)) { "This Block is no longer available." }
        val previous = db.blockMedia().find(blockId, MediaRole.AUDIO.name)?.mediaId
        check(previous == expectedAudioId) { "The audio reference changed. Close recording and review the current reference." }
        appendChecked(blockId, MediaRole.AUDIO, mediaId)
    }

    internal suspend fun saveAudioLink(blockId: String, mediaId: String, previous: String?, confirmedAudioId: String?) {
        val order = db.referenceOrder(blockId, MediaRole.AUDIO)
        val index = order.indexOf("library:$previous")
        if (previous != null && previous != mediaId) removeAudio(blockId, previous, confirmedAudioId)
        if (!db.blockMedia().contains(blockId, "AUDIO", mediaId)) {
            val order = db.referenceOrder(blockId, MediaRole.AUDIO).toMutableList()
            order.add("library:$mediaId")
            db.blockMedia().save(BlockMediaLink(blockId, "AUDIO", mediaId, order.lastIndex))
        }
        if (index >= 0 && previous != mediaId && "library:$mediaId" !in order) {
            val updated = db.referenceOrder(blockId, MediaRole.AUDIO).toMutableList()
            updated.remove("library:$mediaId"); updated.add(index, "library:$mediaId")
            db.writeReferenceOrder(blockId, MediaRole.AUDIO, updated)
        }
    }

    internal suspend fun removeAudio(block: String, media: String, confirmed: String?): Int {
        val saved = db.blockAudio().state(block, media)
        val duration = db.media().find(media)?.durationMs
        val customized = saved != null && (saved.positionMs != 0L || saved.speedPercent != 100 ||
            saved.aMs != 0L || saved.bMs != duration || saved.repeat || saved.selectedMarkerId != null)
        if (confirmed != media && (customized || db.blockAudio().hasMarkers(block, media))) throw ConfirmAudioLoss(media)
        val removed = db.blockMedia().removeExact(block, "AUDIO", media)
        db.writeReferenceOrder(block, MediaRole.AUDIO, db.referenceOrder(block, MediaRole.AUDIO))
        return removed
    }
}

data class BlockVisualReference(val blockId: String, val identity: String, val mimeType: String,
    val mediaId: String? = null, val legacy: BlockImage? = null, val position: Int = 0)

/** Canonical when linked; otherwise the existing legacy attachment. No startup copying/adoption. */
fun resolveBlockVisuals(legacy: List<BlockImage>, links: List<BlockMediaLink>, assets: List<MediaAsset>): List<BlockVisualReference> {
    val canonical = links.filter { it.role == MediaRole.VISUAL.name }
    return (canonical.map { link -> BlockVisualReference(link.blockId, "library:${link.mediaId}",
        assets.find { it.id == link.mediaId }?.mimeType ?: "", mediaId = link.mediaId, position = link.position) } +
        legacy.map { BlockVisualReference(it.blockId, "legacy:${it.fileName}", it.mimeType, legacy = it, position = it.position) }).sortedWith(compareBy({ it.blockId }, { it.position }, { it.identity }))
}
