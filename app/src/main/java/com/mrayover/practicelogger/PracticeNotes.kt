package com.mrayover.practicelogger

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

@Entity(tableName = "practice_notes", foreignKeys = [
    ForeignKey(entity = Block::class, parentColumns = ["id"], childColumns = ["blockId"], onDelete = ForeignKey.RESTRICT),
    ForeignKey(entity = PracticeSession::class, parentColumns = ["id"], childColumns = ["sessionId"], onDelete = ForeignKey.RESTRICT),
    ForeignKey(entity = Placement::class, parentColumns = ["id"], childColumns = ["placementId"], onDelete = ForeignKey.RESTRICT)
], indices = [Index("blockId"), Index("sessionId"), Index("placementId")])
data class PracticeNote(@PrimaryKey val id: String, val blockId: String, val sessionId: String,
    val placementId: String, val createdAt: Long, val text: String)

data class PracticeNoteEntry(val id: String, val blockId: String, val sessionId: String,
    val placementId: String, val createdAt: Long, val text: String, val legacy: Boolean)

@Dao interface PracticeNoteDao {
    @Query("SELECT * FROM practice_notes ORDER BY createdAt DESC, id DESC") suspend fun readAll(): List<PracticeNote>
    @Insert suspend fun insert(note: PracticeNote)
    @Query("UPDATE practice_notes SET text = :text WHERE id = :id AND blockId = :blockId AND sessionId = :sessionId AND placementId = :placementId")
    suspend fun updateText(id: String, blockId: String, sessionId: String, placementId: String, text: String): Int
    @Query("UPDATE placements SET note = :text WHERE id = :placementId AND sourceBlockId = :blockId AND sessionId = :sessionId")
    suspend fun updateLegacy(blockId: String, sessionId: String, placementId: String, text: String): Int
    // Legacy notes remain in their original placement. No copying/backfill or duplicate history rows.
    @Query("""SELECT 'note:' || id AS id, blockId, sessionId, placementId, createdAt, text, 0 AS legacy FROM practice_notes
        UNION ALL
        SELECT 'legacy:' || p.id AS id, p.sourceBlockId AS blockId, p.sessionId, p.id AS placementId,
            COALESCE(s.endedAt, s.startedAt) AS createdAt, p.note AS text, 1 AS legacy
        FROM placements p JOIN sessions s ON p.sessionId = s.id WHERE TRIM(p.note) != ''
        ORDER BY createdAt DESC, id DESC""")
    fun history(): Flow<List<PracticeNoteEntry>>
}

val NOTES_MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `practice_notes` (`id` TEXT NOT NULL, `blockId` TEXT NOT NULL, `sessionId` TEXT NOT NULL, `placementId` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `text` TEXT NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`blockId`) REFERENCES `blocks`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT, FOREIGN KEY(`sessionId`) REFERENCES `sessions`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT, FOREIGN KEY(`placementId`) REFERENCES `placements`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_practice_notes_blockId` ON `practice_notes` (`blockId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_practice_notes_sessionId` ON `practice_notes` (`sessionId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_practice_notes_placementId` ON `practice_notes` (`placementId`)")
    }
}

class PracticeNoteRepository(private val db: BlockDatabase, private val now: () -> Long = System::currentTimeMillis) {
    val history = db.practiceNotes().history()
    suspend fun update(entry: PracticeNoteEntry, text: String) = db.withTransaction {
        require(text.isNotBlank()) { "A saved note cannot be empty." }
        val changed = if (entry.legacy) db.practiceNotes().updateLegacy(entry.blockId, entry.sessionId, entry.placementId, text.trim())
            else db.practiceNotes().updateText(entry.id.removePrefix("note:"), entry.blockId, entry.sessionId, entry.placementId, text.trim())
        check(changed == 1) { "This note is no longer available." }
        // Only text changes. Creation timestamp, associations and practice timing remain intact.
    }
    suspend fun append(blockId: String, sessionId: String, placementId: String, text: String): PracticeNote = db.withTransaction {
        require(text.isNotBlank()) { "Write a note before saving." }
        val session = checkNotNull(db.sessions().find(sessionId))
        val placement = checkNotNull(session.current)
        check(session.session.status == "IN_PROGRESS" && placement.id == placementId && placement.sourceBlockId == blockId && placement.outcome == "PENDING")
        checkNotNull(db.blocks().find(blockId))
        PracticeNote(UUID.randomUUID().toString(), blockId, sessionId, placementId, now(), text.trim()).also { db.practiceNotes().insert(it) }
    }
}

class BlockNotesViewModel(app: Application, private val savedState: SavedStateHandle) : AndroidViewModel(app) {
    private val repository = PracticeNoteRepository(BlockDatabase.appInstance(app))
    var busy by mutableStateOf(false); private set
    var message by mutableStateOf<String?>(null); private set
    val history = repository.history.catch { message = "Could not load notes. Reopen the app to retry; saved notes are kept." }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val editingIds = savedState.getStateFlow("editingNoteIds", arrayListOf<String>())
    fun editDraft(entry: PracticeNoteEntry) = savedState.getStateFlow("noteEdit:${entry.id}", entry.text)
    fun startEdit(entry: PracticeNoteEntry) {
        if (entry.id !in editingIds.value) {
            savedState["noteEdit:${entry.id}"] = entry.text
            savedState["editingNoteIds"] = ArrayList(editingIds.value + entry.id)
        }
        message = null
    }
    fun changeEdit(entry: PracticeNoteEntry, text: String) { savedState["noteEdit:${entry.id}"] = text; message = null }
    fun cancelEdit(entry: PracticeNoteEntry) {
        savedState["editingNoteIds"] = ArrayList(editingIds.value - entry.id)
        savedState["noteEdit:${entry.id}"] = entry.text
    }
    fun hasUnsavedEdits(blockId: String) = history.value.any { it.blockId == blockId && it.id in editingIds.value && editDraft(it).value != it.text }
    fun discardEdits(blockId: String) { history.value.filter { it.blockId == blockId && it.id in editingIds.value }.forEach(::cancelEdit) }
    fun saveEdit(entry: PracticeNoteEntry) {
        if (busy) return
        val text = editDraft(entry).value
        if (text.isBlank()) return
        busy = true; message = null
        viewModelScope.launch {
            try {
                repository.update(entry, text)
                savedState["editingNoteIds"] = ArrayList(editingIds.value - entry.id)
                message = "Changes saved."
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { message = "Changes could not be saved. Your edit is kept; retry." }
            finally { busy = false }
        }
    }
    fun draft(placementId: String) = savedState.getStateFlow("noteDraft:$placementId", "")
    fun edit(placementId: String, text: String) { savedState["noteDraft:$placementId"] = text; message = null }
    fun save(blockId: String, sessionId: String, placementId: String) {
        if (busy) return
        val text = draft(placementId).value
        if (text.isBlank()) return
        busy = true; message = null
        viewModelScope.launch {
            try {
                repository.append(blockId, sessionId, placementId, text)
                if (draft(placementId).value == text) savedState["noteDraft:$placementId"] = ""
                message = "Note saved."
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { message = "Note could not be saved. Your draft is kept; check storage and retry." }
            finally { busy = false }
        }
    }
}
