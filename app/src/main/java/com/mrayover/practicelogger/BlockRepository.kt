package com.mrayover.practicelogger

import androidx.room.withTransaction
import java.util.UUID

data class BlockDraft(val name: String = "", val action: String = "", val duration: String = "5", val tempo: String = "", val notes: String = "") {
    fun errors(): Map<String, String> = buildMap {
        if (name.isBlank()) put("name", "Enter a name.")
        if (action.isBlank()) put("action", "Describe what to practice.")
        if (duration.trim().toIntOrNull()?.let { it > 0 } != true) put("duration", "Enter positive whole minutes (up to 2147483647).")
        if (tempo.isNotBlank() && tempo.trim().toIntOrNull()?.let { it > 0 } != true) put("tempo", "Enter a positive whole BPM or leave blank.")
    }
    companion object {
        fun from(block: Block) = BlockDraft(block.name, block.action, block.durationMinutes.toString(), block.tempo?.toString() ?: "", block.notes)
    }
}

fun matchingBlocks(blocks: List<Block>, query: String, archived: Boolean): List<Block> {
    val term = query.trim()
    return blocks.filter { it.archived == archived && (term.isEmpty() ||
        it.name.contains(term, ignoreCase = true) || it.action.contains(term, ignoreCase = true) || it.notes.contains(term, ignoreCase = true)) }
}

// Archive-only policy: stable IDs remain available for future historical references.
// There is deliberately no permanent-delete API or destructive migration fallback.
class BlockRepository(private val db: BlockDatabase, private val now: () -> Long = System::currentTimeMillis) {
    val blocks = db.blocks().observeAll()
    suspend fun save(id: String?, draft: BlockDraft): String = db.withTransaction {
        require(draft.errors().isEmpty()) { "Check the highlighted fields." }
        val previous = id?.let { checkNotNull(db.blocks().find(it)) { "This Block is no longer available." } }
        val timestamp = now()
        val block = Block(
            id = previous?.id ?: UUID.randomUUID().toString(), name = draft.name.trim(), action = draft.action.trim(),
            durationMinutes = draft.duration.trim().toInt(), tempo = draft.tempo.trim().takeIf { it.isNotEmpty() }?.toInt(),
            notes = draft.notes.trim(), archived = previous?.archived ?: false,
            createdAt = previous?.createdAt ?: timestamp, updatedAt = timestamp
        )
        if (previous == null) db.blocks().insert(block) else check(db.blocks().update(block) == 1)
        block.id
    }
    suspend fun setArchived(id: String, archived: Boolean) = db.withTransaction {
        val previous = checkNotNull(db.blocks().find(id)) { "This Block is no longer available." }
        check(db.blocks().update(previous.copy(archived = archived, updatedAt = now())) == 1)
    }
}
