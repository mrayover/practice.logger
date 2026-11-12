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
    private val db = BlockDatabase.appInstance(app)
    internal val overlays = PracticeOverlayRepository(db)
    val photoCapture = ReferencePhotoCapture(app)
    val repository = ImageReferenceRepository(db, File(app.filesDir, "block-images"))
    val images = repository.images.catch { message = "Could not read references. Reopen the app to retry."; emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val library = MediaLibraryRepository(db, File(app.filesDir, "media-library"))
    val associations = BlockMediaRepository(db, library, repository)
    internal val editor = BlockEditorMedia(app, db, library, repository, associations)
    internal fun openEditor(token: String, id: String?, recovered: (String) -> Unit = {}) = work {
        editor.open(token, id); editor.completedId?.let(recovered)
    }
    internal fun editorChoose(role: MediaRole, id: String) = work { if (!editor.choose(role, id)) message = "This reference is already attached. Its position was kept." }
    internal fun editorRemove(role: MediaRole, identity: String, confirmed: Boolean) = work { editor.remove(role, identity, confirmed) }
    internal fun editorMove(role: MediaRole, identity: String, delta: Int) = work { editor.move(role, identity, delta) }
    internal fun editorImport(role: MediaRole, uri: Uri) = work { if (!editor.import(role, uri)) message = "This reference is already attached. Its position was kept." }
    internal fun editorPhoto(name: String) = work { editor.photo(photoCapture, name) }
    internal fun discardEditor(done: () -> Unit) = work { editor.discard(); done() }
    val links = associations.links.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val assets = library.assets.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val visuals = combine(images, links, assets, ::resolveBlockVisuals)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    internal data class VisualChange(val block: String, val next: String?, val reviewed: VisualChangeCheck) {
        val legacy get() = reviewed.identity?.startsWith("legacy:") == true
    }
    internal var pendingVisualChange by mutableStateOf<VisualChange?>(null); private set
    internal data class AudioChange(val block: String, val next: String?, val previous: String, val order: List<String>)
    internal var pendingAudioChange by mutableStateOf<AudioChange?>(null); private set
    suspend fun loadPage(reference: BlockVisualReference, page: Int): ReferencePage =
        reference.mediaId?.let { library.visualPage(it, page) } ?: repository.loadPage(checkNotNull(reference.legacy), page)
    fun choose(blockId: String, role: MediaRole, mediaId: String) = work {
        if (role == MediaRole.AUDIO) changeAudio(blockId, mediaId)
        else changeVisual(blockId, mediaId)
    }
    private suspend fun changeVisual(blockId: String, next: String?) {
        val change = VisualChange(blockId, next, db.reviewVisualChange(blockId))
        if (next == null && change.reviewed.identity == null) return
        if (change.reviewed.identity != next?.let { "library:$it" } &&
            (next == null || change.legacy || change.reviewed.hasMarkings)) {
            pendingVisualChange = change
        } else applyVisualChange(change)
    }
    internal fun cancelVisualChange() { pendingVisualChange = null }
    internal fun confirmVisualChange() {
        val change = pendingVisualChange ?: return
        cancelVisualChange()
        work { applyVisualChange(change) }
    }
    private suspend fun applyVisualChange(change: VisualChange) {
        val (block, next, reviewed) = change
        message = when {
            next != null -> {
                associations.linkChecked(block, MediaRole.VISUAL, next, replaceLegacy = change.legacy,
                    reviewedVisual = reviewed)
                if (change.legacy) "Library visual attached. The old Block-owned reference was retired."
                else "Visual reference attached from Library."
            }
            change.legacy -> if (repository.removeChecked(block, reviewed)) "Reference removed."
                else "Reference removed. Private file cleanup will be retried when the app reopens."
            else -> {
                associations.unlinkChecked(block, MediaRole.VISUAL, reviewedVisual = reviewed)
                "Visual reference removed from this Block. The Library original was kept."
            }
        }
    }
    fun unlink(blockId: String, role: MediaRole) = work {
        if (role == MediaRole.AUDIO) changeAudio(blockId, null)
        else changeVisual(blockId, null)
    }

    internal suspend fun overlayCopyTargets(reference: BlockVisualReference): List<Block> {
        val compatible = db.blockMedia().readAll().filter {
            it.role == MediaRole.VISUAL.name && it.mediaId == reference.mediaId && it.blockId != reference.blockId
        }.mapTo(hashSetOf()) { it.blockId }
        return if (compatible.isEmpty()) emptyList() else db.blocks().readAll().filter { it.id in compatible }
    }

    internal suspend fun copyOverlays(reference: BlockVisualReference, target: String): Int {
        val loaded = loadPage(reference, 0)
        loaded.bitmap.recycle()
        return overlays.copy(reference, target, loaded.pageCount)
    }

    internal fun cancelAudioChange() { pendingAudioChange = null }
    internal fun confirmAudioChange() {
        val change = pendingAudioChange ?: return
        cancelAudioChange()
        work { changeAudio(change.block, change.next, change.previous, change.order) }
    }
    private suspend fun changeAudio(block: String, next: String?, confirmed: String? = null, reviewedOrder: List<String>? = null) {
        try {
            if (next == null) associations.unlink(block, MediaRole.AUDIO, confirmed, reviewedOrder)
            else associations.link(block, MediaRole.AUDIO, next, confirmedAudioId = confirmed, reviewedOrder = reviewedOrder)
            message = if (next == null) "Audio reference removed. The Library original was kept."
                else "Audio reference attached from Library."
        } catch (loss: ConfirmAudioLoss) {
            pendingAudioChange = AudioChange(block, next, loss.mediaId, loss.order)
        }
    }
    private suspend fun attachImported(blockId: String, role: MediaRole, asset: MediaAsset) {
        try {
            require(role.accepts(asset.category)) { "Choose a compatible media type." }
            if (role == MediaRole.AUDIO) {
                changeAudio(blockId, asset.id)
                if (pendingAudioChange != null) message = "Imported into Library. Confirm replacement to attach it."
            } else {
                changeVisual(blockId, asset.id)
                if (pendingVisualChange != null) message = "Imported into Library. Confirm replacement to attach it."
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
            catch (e: IllegalStateException) { message = e.message ?: "Could not update the reference. Your existing attachment was kept." }
            catch (_: Exception) { message = "Could not update the reference. Your existing attachment was kept. Check the file, access and free storage, then retry." }
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
}
