package com.mrayover.practicelogger

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

/** Private camera staging only; accepted photos still use the reference repository's validation. */
class ReferencePhotoCapture(private val context: Context) {
    private val directory get() = File(context.cacheDir, "reference-camera")
    fun create(): String {
        check(directory.mkdirs() || directory.isDirectory)
        return "${UUID.randomUUID()}.jpg".also { check(file(it).createNewFile()) }
    }
    fun file(name: String): File {
        require(name.matches(Regex("[0-9a-f-]{36}\\.jpg")))
        return File(directory, name)
    }
    fun uri(name: String): Uri = FileProvider.getUriForFile(context, "${context.packageName}.reference-camera", file(name))
    fun discard(name: String) { file(name).delete() }
}
