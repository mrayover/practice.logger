package com.mrayover.practicelogger

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream

internal data class InspirationDraft(
    val details: Inspiration,
    val body: String = "",
    val input: InspirationInput? = null,
    val removeOriginal: Boolean = false
) {
    val hasVisual get() = input != null ||
        (!removeOriginal && details.mimeType != null && details.mimeType != "text/plain")
}

internal class InspirationsViewModel(app: Application) : AndroidViewModel(app) {
    private val resolver = app.contentResolver
    private val repository = InspirationRepository(BlockDatabase.appInstance(app), app.filesDir)
    val entries = repository.entries
    private val camera = ReferencePhotoCapture(app)
    private var pendingPhoto: String? = null
    private var draftPhoto: String? = null
    var current by mutableStateOf<Inspiration?>(null)
        private set
    var draft by mutableStateOf<InspirationDraft?>(null)
    private var initial: InspirationDraft? = null
    var body by mutableStateOf("")
        private set
    var preview by mutableStateOf(false)
    var chooseMethod by mutableStateOf(false)
        private set
    var busy by mutableStateOf(false)
        private set
    var message by mutableStateOf<String?>(null)
        private set
    var confirmDiscard by mutableStateOf<(() -> Unit)?>(null)
        private set

    fun add() {
        if (busy) return
        current = null
        startDraft(InspirationDraft(Inspiration("", "")))
        chooseMethod = true
    }

    fun write() { chooseMethod = false; message = null }

    fun open(row: Inspiration) = operation("Could not open this inspiration.") {
        body = repository.text(row)
        current = row
    }

    fun edit() {
        val row = current ?: return
        startDraft(InspirationDraft(row, body))
    }

    private fun startDraft(value: InspirationDraft) {
        initial = value
        draft = value
        message = null
    }

    fun openFile(uri: Uri) = operation("Could not import this file.") {
        val selected = withContext(Dispatchers.IO) {
            val name = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                if (it.moveToFirst()) it.getString(0) else null
            } ?: uri.lastPathSegment.orEmpty()
            val mime = requireNotNull(inspirationFormats[name.substringAfterLast('.').lowercase(java.util.Locale.ROOT)]) {
                "Choose a PDF, JPEG, PNG, WebP, UTF-8 TXT or MD file."
            }
            val input = InspirationInput(name) {
                checkNotNull(resolver.openInputStream(uri)) { "Could not read the selected file. Choose it again." }
            }
            val imported = if (mime == "text/plain") {
                val bytes = ByteArrayOutputStream()
                input.open().use { copyPortable(it, bytes, INSPIRATION_LIMIT) }
                require(bytes.size() > 0) { "The text file is empty." }
                try { bytes.toByteArray().inputStream().use(::readInspirationText) }
                catch (_: java.nio.charset.CharacterCodingException) {
                    throw IllegalArgumentException("Choose a plain-text file encoded as UTF-8.")
                }
            } else null
            Triple(name, input, imported)
        }
        val form = draft ?: return@operation
        val (name, input, imported) = selected
        draft = form.copy(
            details = form.details.copy(title = form.details.title.ifBlank { name.substringBeforeLast('.').take(255) }),
            body = imported.orEmpty(), input = if (imported == null) input else null
        )
        chooseMethod = false
    }

    fun launchCamera(launch: (Uri) -> Unit) {
        if (busy || pendingPhoto != null) return
        try {
            val name = camera.create()
            pendingPhoto = name
            launch(camera.uri(name))
        } catch (_: Exception) {
            pendingPhoto?.let(camera::discard)
            pendingPhoto = null
            message = "Camera unavailable. Try Choose File or check your camera app's permissions."
        }
    }

    fun cameraResult(success: Boolean) {
        val name = pendingPhoto ?: return
        pendingPhoto = null
        val form = draft
        if (!success || form == null) {
            camera.discard(name)
            if (form != null) message = "No photo was captured. Try Camera again or choose another method."
            return
        }
        draftPhoto?.let(camera::discard)
        draftPhoto = name
        draft = form.copy(input = InspirationInput("photo.jpg", camera.file(name)::inputStream))
        chooseMethod = false
        message = null
    }

    fun pickerUnavailable() { message = "The file picker is unavailable. Try another creation method." }

    fun removeOriginal() {
        val form = draft ?: return
        draft = form.copy(removeOriginal = true, input = null)
    }

    fun save() = operation("Could not save this inspiration.") {
        val form = draft ?: return@operation
        val original = initial ?: return@operation
        val bodyChanged = form.body != original.body
        val replacement = form.input ?: form.body.takeIf { bodyChanged && it.isNotEmpty() }?.let { value ->
            InspirationInput("text.txt") { value.toByteArray(Charsets.UTF_8).inputStream() }
        }
        val remove = form.removeOriginal || (bodyChanged && form.body.isEmpty() && !form.hasVisual)
        val saved = repository.save(current, form.details, replacement, remove)
        current = saved
        body = form.body
        closeDraft()
    }

    fun delete() = operation("Could not delete this inspiration.") {
        current?.let { repository.delete(it) }
        current = null
        body = ""
    }

    fun back(onMedia: () -> Unit) {
        if (busy || pendingPhoto != null) return
        when {
            preview -> preview = false
            draft != null -> guard { closeDraft() }
            current != null -> { current = null; body = ""; message = null }
            else -> { message = null; onMedia() }
        }
    }

    fun leave(onLeave: () -> Unit) {
        if (busy || pendingPhoto != null) return
        guard {
            closeDraft()
            current = null
            body = ""
            preview = false
            onLeave()
        }
    }

    private fun guard(action: () -> Unit) {
        if (draft != initial) confirmDiscard = action else action()
    }

    fun discard(confirmed: Boolean) {
        val action = confirmDiscard
        confirmDiscard = null
        if (confirmed) action?.invoke()
    }

    private fun closeDraft() {
        draftPhoto?.let(camera::discard)
        draftPhoto = null
        draft = null
        initial = null
        chooseMethod = false
        message = null
    }

    private fun operation(failure: String, action: suspend () -> Unit) {
        if (busy) return
        busy = true
        message = null
        viewModelScope.launch {
            try { action() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                message = if (error is IllegalArgumentException || error is IllegalStateException) {
                    error.message?.let { "$failure $it" } ?: failure
                } else failure
            } finally { busy = false }
        }
    }

    override fun onCleared() {
        pendingPhoto?.let(camera::discard)
        draftPhoto?.let(camera::discard)
    }

    suspend fun visualPage(row: Inspiration, page: Int) = repository.visualPage(row, page)
}
