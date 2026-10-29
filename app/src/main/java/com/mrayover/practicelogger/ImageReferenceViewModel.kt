package com.mrayover.practicelogger

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File

class ImageReferenceViewModel(app: Application) : AndroidViewModel(app) {
    val repository = ImageReferenceRepository(BlockDatabase.appInstance(app), File(app.filesDir, "block-images"))
    val images = repository.images.catch { message = "Could not read references. Reopen the app to retry."; emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    var busy by mutableStateOf(false); private set
    var message by mutableStateOf<String?>(null); private set
    init { viewModelScope.launch { runCatching { repository.recover() }.onFailure { message = "Reference storage cleanup will be retried when the app reopens." } } }
    private fun work(operation: suspend () -> Unit) {
        if (busy) return
        busy = true; message = null
        viewModelScope.launch {
            try { operation() }
            catch (e: CancellationException) { throw e }
            catch (e: IllegalArgumentException) { message = e.message ?: "Choose a valid JPEG, PNG, WebP or unencrypted PDF." }
            catch (_: Exception) { message = "Could not update the reference. Your existing attachment was kept. Check the file, access and free storage, then retry." }
            finally { busy = false }
        }
    }
    fun import(blockId: String, uri: Uri) = work {
        repository.import(blockId) { checkNotNull(getApplication<Application>().contentResolver.openInputStream(uri)) }
        message = "Reference saved. Keep your original separately; JSON backups exclude images and PDFs."
    }
    fun remove(blockId: String) = work {
        message = if (repository.remove(blockId)) "Reference removed."
            else "Reference removed. Private file cleanup will be retried when the app reopens."
    }
}
