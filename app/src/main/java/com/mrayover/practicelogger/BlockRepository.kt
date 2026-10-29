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

data class BlockDependencies(val goals: Int, val routines: Int, val recordings: Int,
    val sessionEntries: Int, val completedEntries: Int, val images: Int = 0) {
    val canDelete get() = goals == 0 && routines == 0 && recordings == 0 && sessionEntries == 0 && images == 0
    fun explanation(): String = buildList {
        add("It is associated with:")
        if (images > 0) add("• $images reference image. Remove the image first in this Block editor (confirmation required).")
        if (goals > 0) add("• $goals Goal associations")
        if (routines > 0) add("• $routines saved Routines")
        if (recordings > 0) add("• $recordings recordings")
        if (sessionEntries > 0) add("• $sessionEntries practice session entries (drafts, unfinished and completed)")
        if (completedEntries > 0) add("$completedEntries entries belong to completed practice history, which remains protected.")
        if (recordings > 0) add("Export any recordings you want to keep before deleting individual takes in Recordings.")
        add("Remove applicable associations or use Archive instead. No associated data will be removed automatically.")
    }.joinToString("\n\n")
}
data class BlockDeletionReview(val block: Block, val dependencies: BlockDependencies)
sealed interface BlockDeletionResult {
    data object Deleted : BlockDeletionResult
    data class Blocked(val review: BlockDeletionReview) : BlockDeletionResult
}

// Permanent deletion is limited to unreferenced Blocks; original FK restrictions remain.
class BlockRepository(private val db: BlockDatabase, private val now: () -> Long = System::currentTimeMillis) {
    val blocks = db.blocks().observeAll()
    suspend fun reviewDeletion(id: String): BlockDeletionReview = db.withTransaction {
        BlockDeletionReview(checkNotNull(db.blocks().find(id)) { "This Block is no longer available." }, db.blocks().dependencies(id))
    }
    suspend fun deletePermanently(id: String): BlockDeletionResult = db.withTransaction {
        // Recheck at confirmation: associations may have changed since the dialog opened.
        val review = reviewDeletion(id)
        if (!review.dependencies.canDelete) BlockDeletionResult.Blocked(review)
        else {
            check(db.blocks().delete(id) == 1) { "The Block could not be deleted." }
            BlockDeletionResult.Deleted
        }
    }
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
