package com.mrayover.practicelogger

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File

class ImageReferenceViewModel(app: Application) : AndroidViewModel(app) {
    val photoCapture = ReferencePhotoCapture(app)
    val repository = ImageReferenceRepository(BlockDatabase.appInstance(app), File(app.filesDir, "block-images"))
    val images = repository.images.catch { message = "Could not read references. Reopen the app to retry."; emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val library = MediaLibraryRepository(BlockDatabase.appInstance(app), File(app.filesDir, "media-library"))
    val associations = BlockMediaRepository(BlockDatabase.appInstance(app), library, repository)
    val links = associations.links.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val assets = library.assets.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val visuals = combine(images, links, assets, ::resolveBlockVisuals)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    var pendingVisualMedia by mutableStateOf<String?>(null); private set
    private var pendingBlock: String? = null
    suspend fun loadPage(reference: BlockVisualReference, page: Int): ReferencePage =
        reference.mediaId?.let { library.visualPage(it, page) } ?: repository.loadPage(checkNotNull(reference.legacy), page)
    fun choose(blockId: String, role: MediaRole, mediaId: String) {
        if (busy) return
        if (role == MediaRole.VISUAL && images.value.any { it.blockId == blockId }) {
            pendingBlock = blockId; pendingVisualMedia = mediaId
        } else work { associations.link(blockId, role, mediaId); message = "${roleLabel(role)} attached from Library." }
    }
    fun cancelReplacement() { pendingVisualMedia = null; pendingBlock = null }
    fun confirmReplacement() {
        val block = pendingBlock ?: return; val media = pendingVisualMedia ?: return
        cancelReplacement()
        work { associations.link(block, MediaRole.VISUAL, media, replaceLegacy = true); message = "Library visual attached. The old Block-owned reference was retired." }
    }
    fun unlink(blockId: String, role: MediaRole) = work {
        associations.unlink(blockId, role); message = "${roleLabel(role)} removed from this Block. The Library original was kept."
    }
    private fun roleLabel(role: MediaRole) = if (role == MediaRole.VISUAL) "Visual reference" else "Audio reference"
    private suspend fun attachImported(blockId: String, role: MediaRole, asset: MediaAsset) {
        try {
            require(role.accepts(asset.category)) { "Choose a compatible media type." }
            if (role == MediaRole.VISUAL && repository.images.first().any { it.blockId == blockId }) {
                pendingBlock = blockId; pendingVisualMedia = asset.id
                message = "Imported into Library. Confirm replacement to attach it."
            } else {
                associations.link(blockId, role, asset.id)
                message = "Imported into Library and attached. JSON backups exclude media and associations."
            }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { message = "Imported into Library, but attachment did not complete. The Library item and previous Block reference were kept. Choose it from Library to retry." }
    }
    fun importLibrary(blockId: String, role: MediaRole, uri: Uri) = work {
        val asset = withContext(Dispatchers.IO) {
            val resolver = getApplication<Application>().contentResolver
            val mime = resolver.getType(uri)
            val name = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                if (it.moveToFirst() && !it.isNull(0)) it.getString(0) else null
            } ?: when (mime) { "image/jpeg" -> "Photo.jpg"; "image/png" -> "Photo.png"; "image/webp" -> "Photo.webp"; else -> throw IllegalArgumentException("Choose an original with a supported filename.") }
            val extension = name.substringAfterLast('.', "").lowercase(java.util.Locale.ROOT)
            require(if (role == MediaRole.AUDIO) extension in setOf("mp3", "m4a", "wav") else extension in setOf("jpg", "jpeg", "png", "webp", "pdf")) { "Choose a compatible media type." }
            library.import(name, mime) { checkNotNull(resolver.openInputStream(uri)) }
        }
        attachImported(blockId, role, asset)
    }
    fun importLibraryPhoto(blockId: String, name: String) = work {
        try {
            val asset = library.import("Camera photo.jpg", "image/jpeg") { photoCapture.file(name).inputStream() }
            attachImported(blockId, MediaRole.VISUAL, asset)
        } finally { withContext(NonCancellable + Dispatchers.IO) { photoCapture.discard(name) } }
    }
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
            catch (e: Exception) { message = e.message ?: "Could not update the reference. Your existing attachment was kept. Check the file, access and free storage, then retry." }
            finally { busy = false }
        }
    }
    fun import(blockId: String, uri: Uri) = work {
        repository.import(blockId) { checkNotNull(getApplication<Application>().contentResolver.openInputStream(uri)) }
        message = "Reference saved. Keep your original separately; JSON backups exclude images and PDFs."
    }
    fun importPhoto(blockId: String, name: String) = work {
        try {
            repository.import(blockId) { photoCapture.file(name).inputStream() }
            message = "Photo saved. JSON backups exclude images and PDFs."
        } finally {
            withContext(NonCancellable + Dispatchers.IO) { photoCapture.discard(name) }
        }
    }
    fun photoUnavailable() { message = "Could not open the camera or gallery. Try again, or use Add image or PDF." }
    fun remove(blockId: String) = work {
        message = if (repository.remove(blockId)) "Reference removed."
            else "Reference removed. Private file cleanup will be retried when the app reopens."
    }
}
