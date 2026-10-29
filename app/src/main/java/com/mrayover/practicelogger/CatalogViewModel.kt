package com.mrayover.practicelogger

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class Catalog(val blocks: List<Block> = emptyList(), val ready: Boolean = false, val error: String? = null)

class CatalogViewModel(application: Application) : AndroidViewModel(application) {
    private val db = BlockDatabase.appInstance(application)
    private val repository = BlockRepository(db)
    val catalog = repository.blocks.map { Catalog(it, ready = true) }
        .catch { emit(Catalog(error = "Could not open saved Blocks. Close and reopen the app to retry.")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Catalog())
    var busy by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var savedBlockId: String? = null
        private set
    var destinationArchived by mutableStateOf<Boolean?>(null)
        private set
    var deletionReview by mutableStateOf<BlockDeletionReview?>(null)
        private set
    var deleted by mutableStateOf(false)
        private set
    fun navigationHandled() { destinationArchived = null; deleted = false }
    fun dismissDeletion() { if (!busy) deletionReview = null }
    fun reviewDeletion(id: String) = mutate({ deletionReview = repository.reviewDeletion(id) }, {},
        "Could not check this Block's dependencies. Nothing was deleted. Try again.")
    fun confirmDeletion() {
        val review = deletionReview ?: return
        if (!review.dependencies.canDelete) return
        mutate({
            when (val result = repository.deletePermanently(review.block.id)) {
                BlockDeletionResult.Deleted -> {
                    deletionReview = null; savedBlockId = null; deleted = true
                    destinationArchived = review.block.archived
                }
                is BlockDeletionResult.Blocked -> deletionReview = result.review
            }
        }, {}, "Could not delete this Block. Nothing was deleted. Check available storage and try again.")
    }
    fun dismissError() { error = null }
    private fun mutate(action: suspend () -> Unit, done: () -> Unit, failureMessage: String = "Could not save changes. Your draft is still here. Check available storage and try again.") {
        if (busy) return
        busy = true
        viewModelScope.launch {
            try { action(); done() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { error = failureMessage }
            finally { busy = false }
        }
    }
    fun save(id: String?, draft: BlockDraft, archived: Boolean) = mutate({ savedBlockId = repository.save(id, draft) }, { destinationArchived = archived })
    fun archive(id: String, archived: Boolean) = mutate({ savedBlockId = null; repository.setArchived(id, archived) }, { destinationArchived = archived })
    // The shared database belongs to the application, not an individual screen.
}
