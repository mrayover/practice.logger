package com.mrayover.practicelogger

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

internal const val OVERLAYS_PER_PAGE = 64
internal const val OVERLAYS_PER_BLOCK = 256
internal const val OVERLAYS_TOTAL = 10_000
internal const val OVERLAY_MAX_ORDER = 1_000_000_000

internal enum class OverlayTool { HIGHLIGHT, COVER, FOCUS }

@Entity(
    tableName = "practice_overlays",
    foreignKeys = [ForeignKey(
        entity = Block::class, parentColumns = ["id"], childColumns = ["blockId"],
        onDelete = ForeignKey.RESTRICT
    )],
    indices = [Index(value = ["blockId", "reference", "page", "drawOrder"], unique = true)]
)
data class PracticeOverlay(
    @androidx.room.PrimaryKey val id: String,
    val blockId: String,
    val reference: String,
    val page: Int,
    val type: String,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val appearance: Int,
    val drawOrder: Int
)

@Dao
interface PracticeOverlayDao {
    @Query("SELECT * FROM practice_overlays WHERE blockId = :block AND reference = :reference ORDER BY page, drawOrder")
    fun observe(block: String, reference: String): Flow<List<PracticeOverlay>>

    @Query("SELECT * FROM practice_overlays WHERE blockId = :block AND reference = :reference ORDER BY page, drawOrder")
    suspend fun read(block: String, reference: String): List<PracticeOverlay>

    @Query("SELECT * FROM practice_overlays WHERE id = :id")
    suspend fun find(id: String): PracticeOverlay?

    @Insert suspend fun insert(shape: PracticeOverlay)
    @Update suspend fun update(shape: PracticeOverlay): Int

    @Query("DELETE FROM practice_overlays WHERE id = :id")
    suspend fun delete(id: String): Int

    @Query("DELETE FROM practice_overlays WHERE blockId = :block AND reference = :reference")
    suspend fun discard(block: String, reference: String): Int

    @Query("SELECT COUNT(*) FROM practice_overlays")
    suspend fun count(): Int
}

internal fun validOverlayBounds(left: Number, top: Number, right: Number, bottom: Number): Boolean {
    val l = left.toDouble()
    val t = top.toDouble()
    val r = right.toDouble()
    val b = bottom.toDouble()
    return l >= 0 && t >= 0 && r <= 1 && b <= 1 && l < r && t < b
}

internal fun validateOverlay(shape: PracticeOverlay, pageCount: Int) {
    require(shape.id.isNotBlank() && shape.id.length <= 64)
    require(shape.page in 0 until pageCount && shape.page < 100)
    require(shape.drawOrder in 0..OVERLAY_MAX_ORDER)
    require(validOverlayBounds(shape.left, shape.top, shape.right, shape.bottom)) { "Choose a rectangle within the page." }
    require(when (shape.type) {
        "HIGHLIGHT" -> shape.appearance in 0..2
        "COVER", "FOCUS" -> shape.appearance == 0
        else -> false
    })
}

internal suspend fun BlockDatabase.visualIdentity(blockId: String): String? =
    blockMedia().find(blockId, MediaRole.VISUAL.name)?.let { "library:${it.mediaId}" }
        ?: images().find(blockId)?.let { "legacy:${it.fileName}" }

internal data class VisualChangeCheck(val identity: String?, val hasMarkings: Boolean)

internal suspend fun BlockDatabase.reviewVisualChange(block: String): VisualChangeCheck = withTransaction {
    val identity = visualIdentity(block)
    VisualChangeCheck(identity, identity != null && overlays().read(block, identity).isNotEmpty())
}

// Call inside the same transaction as the visual link change.
internal suspend fun BlockDatabase.discardVisualMarkings(block: String, current: String?, reviewed: VisualChangeCheck?) {
    check(reviewed == null || reviewed.identity == current) {
        "The visual reference changed. Review it and try again."
    }
    if (current != null && overlays().read(block, current).isNotEmpty()) {
        check(reviewed?.hasMarkings == true) { "Review and confirm discarding this Block’s saved markings first." }
        overlays().discard(block, current)
    }
}

/** A committed change can be inverted using the same compare-and-write path. */
internal data class OverlayEdit(val before: PracticeOverlay?, val after: PracticeOverlay?) {
    val shape get() = checkNotNull(after ?: before)
    fun inverse() = OverlayEdit(after, before)
}

internal class PracticeOverlayRepository(private val db: BlockDatabase) {
    fun observe(reference: BlockVisualReference) = db.overlays().observe(reference.blockId, reference.identity)

    suspend fun edit(change: OverlayEdit, verifiedPages: Int, appendOnPage: Boolean = false): OverlayEdit = db.withTransaction {
        val shape = change.shape
        val dao = db.overlays()
        check(db.visualIdentity(shape.blockId) == shape.reference) { "The reference changed. Reopen it to edit markings." }
        check(dao.find(shape.id) == change.before) { "This marking changed. Select it again." }
        val saved = change.after?.let {
            if (appendOnPage) append(listOf(it), shape.blockId, verifiedPages).single()
            else {
                validateOverlay(it, verifiedPages)
                if (change.before == null) {
                    checkCapacity(dao.read(it.blockId, it.reference), listOf(it))
                    dao.insert(it)
                } else {
                    require(change.before.copy(
                        left = it.left, top = it.top, right = it.right, bottom = it.bottom,
                        appearance = it.appearance
                    ) == it)
                    check(dao.update(it) == 1)
                }
                it
            }
        }
        if (saved == null) check(dao.delete(shape.id) == 1)
        OverlayEdit(change.before, saved)
    }

    suspend fun copy(source: BlockVisualReference, destination: String, verifiedPages: Int): Int = db.withTransaction {
        require(destination != source.blockId &&
            db.visualIdentity(source.blockId) == source.identity &&
            db.visualIdentity(destination) == source.identity) {
            "Choose another Block with the same visual original."
        }
        val originals = db.overlays().read(source.blockId, source.identity)
        require(originals.isNotEmpty()) { "There are no markings to copy." }
        append(originals.map { it.copy(id = java.util.UUID.randomUUID().toString(), blockId = destination) },
            destination, verifiedPages).size
    }

    private suspend fun append(shapes: List<PracticeOverlay>, block: String, verifiedPages: Int): List<PracticeOverlay> {
        val dao = db.overlays()
        val existing = dao.read(block, shapes.first().reference)
        checkCapacity(existing, shapes)
        val lastOrder = existing.groupBy { it.page }.mapValues { (_, page) -> page.last().drawOrder }.toMutableMap()
        return shapes.map { shape ->
            val order = (lastOrder[shape.page] ?: -1) + 1
            lastOrder[shape.page] = order
            shape.copy(drawOrder = order).also {
                validateOverlay(it, verifiedPages)
                dao.insert(it)
            }
        }
    }

    private suspend fun checkCapacity(existing: List<PracticeOverlay>, added: List<PracticeOverlay>) {
        require(existing.size + added.size <= OVERLAYS_PER_BLOCK &&
            db.overlays().count() + added.size <= OVERLAYS_TOTAL) { "This would exceed the saved markings limit." }
        require((existing + added).groupingBy { it.page }.eachCount().values.all { it <= OVERLAYS_PER_PAGE }) {
            "A page can have up to $OVERLAYS_PER_PAGE markings."
        }
    }
}

internal fun installPracticeOverlayConstraints(db: SupportSQLiteDatabase) {
    for (event in listOf("INSERT", "UPDATE")) db.execSQL("""
        CREATE TRIGGER IF NOT EXISTS practice_overlay_${event.lowercase()}
        BEFORE $event ON practice_overlays WHEN
            length(trim(NEW.id)) NOT BETWEEN 1 AND 64 OR
            typeof(NEW.page) != 'integer' OR NEW.page NOT BETWEEN 0 AND 99 OR
            typeof(NEW.drawOrder) != 'integer' OR NEW.drawOrder NOT BETWEEN 0 AND $OVERLAY_MAX_ORDER OR
            typeof(NEW.appearance) != 'integer' OR NOT (
                (NEW.type = 'HIGHLIGHT' AND NEW.appearance BETWEEN 0 AND 2) OR
                (NEW.type IN ('COVER', 'FOCUS') AND NEW.appearance = 0)
            ) OR NOT (
                NEW.left >= 0 AND NEW.left < NEW.right AND NEW.right <= 1 AND
                NEW.top >= 0 AND NEW.top < NEW.bottom AND NEW.bottom <= 1
            ) OR NOT (
                EXISTS (SELECT 1 FROM block_media_links link JOIN media_assets asset ON asset.id = link.mediaId
                    WHERE link.blockId = NEW.blockId AND link.role = 'VISUAL'
                    AND NEW.reference = 'library:' || asset.id
                    AND ((asset.category = 'IMAGE' AND NEW.page = 0) OR
                         (asset.category = 'PDF' AND NEW.page < asset.pageCount)))
                OR EXISTS (SELECT 1 FROM block_images image WHERE image.blockId = NEW.blockId
                    AND NEW.reference = 'legacy:' || image.fileName
                    AND (image.mimeType = 'application/pdf' OR NEW.page = 0)
                    AND NOT EXISTS (SELECT 1 FROM block_media_links
                        WHERE blockId = NEW.blockId AND role = 'VISUAL'))
            )
        BEGIN SELECT RAISE(ABORT, 'Invalid practice overlay'); END
    """.trimIndent())

    for (event in listOf("DELETE", "UPDATE OF blockId, role, mediaId")) db.execSQL("""
        CREATE TRIGGER IF NOT EXISTS practice_overlay_library_${event.substringBefore(' ').lowercase()}
        BEFORE $event ON block_media_links WHEN OLD.role = 'VISUAL' AND EXISTS (
            SELECT 1 FROM practice_overlays WHERE blockId = OLD.blockId AND reference = 'library:' || OLD.mediaId
        ) ${if (event.startsWith("UPDATE")) "AND (NEW.blockId != OLD.blockId OR NEW.role != OLD.role OR NEW.mediaId != OLD.mediaId)" else ""}
        BEGIN SELECT RAISE(ABORT, 'Discard markings before changing the visual reference'); END
    """.trimIndent())

    for (event in listOf("DELETE", "UPDATE OF blockId, fileName, mimeType")) db.execSQL("""
        CREATE TRIGGER IF NOT EXISTS practice_overlay_legacy_${event.substringBefore(' ').lowercase()}
        BEFORE $event ON block_images WHEN EXISTS (
            SELECT 1 FROM practice_overlays WHERE blockId = OLD.blockId AND reference = 'legacy:' || OLD.fileName
        ) ${if (event.startsWith("UPDATE")) "AND (NEW.blockId != OLD.blockId OR NEW.fileName != OLD.fileName OR NEW.mimeType != OLD.mimeType)" else ""}
        BEGIN SELECT RAISE(ABORT, 'Discard markings before changing the visual reference'); END
    """.trimIndent())

    db.execSQL("""
        CREATE TRIGGER IF NOT EXISTS practice_overlay_legacy_replacement
        BEFORE INSERT ON block_media_links WHEN NEW.role = 'VISUAL' AND EXISTS (
            SELECT 1 FROM practice_overlays WHERE blockId = NEW.blockId AND reference LIKE 'legacy:%'
        )
        BEGIN SELECT RAISE(ABORT, 'Discard markings before changing the visual reference'); END
    """.trimIndent())
}

internal val OVERLAY_MIGRATION_17_18 = object : Migration(17, 18) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS practice_overlays (
                id TEXT NOT NULL PRIMARY KEY, blockId TEXT NOT NULL, reference TEXT NOT NULL,
                page INTEGER NOT NULL, type TEXT NOT NULL, left REAL NOT NULL, top REAL NOT NULL,
                right REAL NOT NULL, bottom REAL NOT NULL, appearance INTEGER NOT NULL, drawOrder INTEGER NOT NULL,
                FOREIGN KEY(blockId) REFERENCES blocks(id) ON UPDATE NO ACTION ON DELETE RESTRICT
            )
        """.trimIndent())
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_practice_overlays_blockId_reference_page_drawOrder ON practice_overlays(blockId, reference, page, drawOrder)")
        installPracticeOverlayConstraints(db)
    }
}
