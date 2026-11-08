package com.mrayover.practicelogger

import androidx.room.withTransaction
import java.time.LocalDate
import java.util.UUID

data class BlockDraft(val name: String = "", val action: String = "", val duration: String = "5", val tempo: String = "", val notes: String = "",
    val primaryGoalId: String? = null, val primaryGoalChanged: Boolean = false) {
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
    val sessionEntries: Int, val completedEntries: Int, val images: Int = 0, val mediaLinks: Int = 0, val weeks: Int = 0) {
    val canDelete get() = goals == 0 && routines == 0 && recordings == 0 && sessionEntries == 0 && images == 0 && mediaLinks == 0 && weeks == 0
    fun explanation(): String = buildList {
        add("It is associated with:")
        if (mediaLinks > 0) add("• $mediaLinks Library media links. Remove the visual/audio associations in this Block editor first. Library originals are retained.")
        if (images > 0) add("• $images reference attachment (image or PDF). Remove the reference first in this Block editor (confirmation required).")
        if (weeks > 0) add("• $weeks weekly plans. Remove the Block in Prepare Week first.")
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
    suspend fun save(id: String?, draft: BlockDraft, week: LocalDate? = null): String = db.withTransaction {
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
        if (draft.primaryGoalChanged) {
            val attribution = db.goalAttribution()
            val goalId = draft.primaryGoalId
            if (goalId == null) {
                attribution.clearPrimary(block.id)
            } else if (goalId != attribution.primaryGoalId(block.id)) {
                val goal = requireNotNull(db.goals().find(goalId)) { "This Goal is no longer available." }
                require(!goal.goal.completed) { "Reopen this Goal before assigning it." }
                if (goal.blocks.none { it.blockId == block.id }) {
                    require(goal.blocks.size < 20_000) { "This Goal has too many linked Blocks." }
                    db.goals().insert(GoalBlock(goalId, block.id, goal.blocks.size))
                }
                attribution.setPrimary(BlockPrimaryGoal(block.id, goalId))
            }
        }
        if (week != null) addToWeek(week, block.id)
        block.id
    }

    suspend fun addToWeek(week: LocalDate, id: String) = db.withTransaction {
        check(db.blocks().find(id)?.archived == false)
        appendWeek(week, listOf(id))
    }

    suspend fun carryWeek(from: LocalDate, to: LocalDate) = db.withTransaction {
        require(from < to)
        val activeIds = db.weekPlan().read(from.toString()).map { it.blockId }
            .filter { db.blocks().find(it)?.archived == false }
        appendWeek(to, activeIds)
    }

    private suspend fun appendWeek(week: LocalDate, ids: List<String>) {
        require(practiceWeek(week) == week)
        val dao = db.weekPlan()
        val existing = dao.read(week.toString())
        val present = existing.mapTo(hashSetOf()) { it.blockId }
        val additions = ids.filter(present::add)
        if (additions.isEmpty()) return
        check(dao.count() + additions.size <= ZIP_RECORD_LIMIT)
        dao.insert(additions.mapIndexed { index, id -> WeekBlock(week.toString(), id, existing.size + index) })
    }

    suspend fun removeFromWeek(week: LocalDate, id: String) = db.withTransaction {
        val dao = db.weekPlan()
        val row = dao.read(week.toString()).find { it.blockId == id } ?: return@withTransaction
        dao.remove(row.weekStart, id)
        dao.closeGap(row.weekStart, row.position)
    }
    suspend fun setArchived(id: String, archived: Boolean) = db.withTransaction {
        val previous = checkNotNull(db.blocks().find(id)) { "This Block is no longer available." }
        check(db.blocks().update(previous.copy(archived = archived, updatedAt = now())) == 1)
    }
}
