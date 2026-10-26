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
    fun navigationHandled() { destinationArchived = null }
    fun dismissError() { error = null }
    private fun mutate(action: suspend () -> Unit, done: () -> Unit) {
        if (busy) return
        busy = true
        viewModelScope.launch {
            try { action(); done() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { error = "Could not save changes. Your draft is still here. Check available storage and try again." }
            finally { busy = false }
        }
    }
    fun save(id: String?, draft: BlockDraft, archived: Boolean) = mutate({ savedBlockId = repository.save(id, draft) }, { destinationArchived = archived })
    fun archive(id: String, archived: Boolean) = mutate({ savedBlockId = null; repository.setArchived(id, archived) }, { destinationArchived = archived })
    // The shared database belongs to the application, not an individual screen.
}
