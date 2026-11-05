package com.mrayover.practicelogger

import org.json.JSONObject
import java.io.File

internal val portableDigest = Regex("[0-9a-f]{64}")
private val generatedMediaName = Regex("[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}\\.(img|jpg|png|webp|pdf|m4a|wav|mp3)")

internal data class PortableOriginal(
    val store: String,
    val owner: String,
    val name: String,
    val row: JSONObject
) {
    val path = "originals/$store/$name"
    private val take get() = store == "recordings"
    private val canonical get() = store == "media-library"
    private val mime get() = if (take) when (name.substringAfterLast('.')) {
        "m4a" -> "audio/mp4"
        "wav" -> "audio/wav"
        else -> "audio/mpeg"
    } else row.getString("mimeType")
    private val audio get() = mime.startsWith("audio/")
    val bytes: Long? get() = if (take) null else row.getLong("byteCount")
    val limit: Long get() = if (audio) AudioImport.MAX_BYTES else ImageReferenceRepository.MAX_BYTES.toLong()
    val overlayIdentity: String? get() = when {
        audio -> null
        canonical -> "library:$owner"
        else -> "legacy:$name"
    }
    val originalHash: String? get() = if (canonical) row.getString("sha256") else null

    init {
        require(owner.isNotBlank() && generatedMediaName.matches(name)) { "Invalid original filename or identity." }
        val extension = name.substringAfterLast('.')
        require(when (store) {
            "recordings" -> extension in setOf("m4a", "wav", "mp3")
            "block-images" -> extension in setOf("img", "pdf")
            "media-library" -> extension != "img"
            else -> false
        })
        require(bytes?.let { it in 1..limit } != false)
        require(originalHash == null || portableDigest.matches(checkNotNull(originalHash)))
        val expectedMime = when (extension) {
            "img" -> null
            "jpg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            "pdf" -> "application/pdf"
            "m4a" -> "audio/mp4"
            "wav" -> "audio/wav"
            else -> "audio/mpeg"
        }
        require(if (expectedMime == null) mime in setOf("image/jpeg", "image/png", "image/webp") else mime == expectedMime)
        if (canonical) {
            require(row.getString("category") == if (audio) "AUDIO" else if (mime == "application/pdf") "PDF" else "IMAGE")
            require(row.getLong("importedAt") >= 0 && row.getString("title").isNotBlank())
            if (audio) {
                require(row.getLong("durationMs") in 1..AudioImport.MAX_DURATION_MS)
                require(row.isNull("width") && row.isNull("height") && row.isNull("pageCount"))
            } else {
                require(row.isNull("durationMs") && row.getInt("width") > 0 && row.getInt("height") > 0)
                require(if (extension == "pdf") row.getInt("pageCount") in 1..100 else row.isNull("pageCount"))
            }
        }
        if (take) {
            require(row.getLong("createdAt") >= 0 && row.getLong("durationMs") > 0)
            require(when (row.getString("mode")) {
                "FREE" -> extension == "m4a" && row.isNull("recordedBpm") && row.isNull("beatCount")
                "LOOP" -> extension == "wav" && row.getInt("recordedBpm") in 30..300 && row.getInt("beatCount") in 1..64
                "IMPORTED_LOOP" -> !row.isNull("blockId") && row.isNull("recordedBpm") && row.isNull("beatCount")
                else -> false
            }) { "Invalid recording mode or settings." }
        }
    }

    fun file(root: File): File = portableMediaFile(root, store, name)

    suspend fun validateFile(file: File, db: BlockDatabase): Int? {
        require(file.isFile && file.length() in 1..limit && (bytes == null || file.length() == bytes)) {
            "Missing or changed original for $store item $owner."
        }
        when {
            !audio -> {
                val info = ImageReferenceRepository(db, file.parentFile!!).inspectOriginal(file)
                require(info.mimeType == mime && info.width == row.getInt("width") && info.height == row.getInt("height"))
                if (canonical && mime == "application/pdf") require(info.pageCount == row.getInt("pageCount"))
                return info.pageCount ?: 1
            }
            take && row.getString("mode") == "LOOP" -> {
                val spec = PhraseSpec(row.getInt("recordedBpm"), row.getInt("beatCount"))
                require(PhraseWave.frames(file) == spec.frames && row.getLong("durationMs") == spec.durationMs)
            }
            take && row.getString("mode") == "FREE" -> require(recordedDuration(file) == row.getLong("durationMs"))
            else -> require(inspectImportedAudio(file, name.substringAfterLast('.')) == row.getLong("durationMs"))
        }
        return null
    }
}

internal fun portableMediaFile(root: File, store: String, name: String): File {
    require(store in setOf("recordings", "block-images", "media-library") && generatedMediaName.matches(name))
    val directory = File(root, store)
    return File(directory, name).also { file ->
        require(directory.canonicalFile == File(root.canonicalFile, store) &&
            file.canonicalFile == File(directory.canonicalFile, name)) { "Invalid private media path." }
    }
}

internal fun PortableRecords.originals(): List<PortableOriginal> = listOf(
    Triple("media_assets", "media-library", "id"),
    Triple("block_images", "block-images", "blockId"),
    Triple("takes", "recordings", "id")
).flatMap { (table, store, identity) ->
    this[table].map { row -> PortableOriginal(store, row.getString(identity), row.getString("fileName"), row) }
}.also { originals ->
    require(originals.map { it.path }.toSet().size == originals.size) { "Duplicate original file ownership." }
}
