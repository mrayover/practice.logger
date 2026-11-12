package com.mrayover.practicelogger

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.*
import androidx.room.withTransaction
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate

internal data class EditorReference(val identity: String, val asset: MediaAsset? = null, val legacy: BlockImage? = null) {
    val title get() = asset?.originalName ?: "Block-owned ${if (legacy?.mimeType == "application/pdf") "PDF" else "image"}"
    val type get() = asset?.originalName?.substringAfterLast('.', "")?.uppercase() ?: if (legacy?.mimeType == "application/pdf") "PDF" else "IMAGE"
}

/** A versioned private draft; originals become Library-owned only in the full Save transaction. */
internal class BlockEditorMedia(private val app: Application, private val db: BlockDatabase,
    private val library: MediaLibraryRepository, private val legacy: ImageReferenceRepository,
    private val associations: BlockMediaRepository) {
    private val root = File(app.filesDir, "block-editor-staging")
    private lateinit var directory: File
    private lateinit var stageLibrary: MediaLibraryRepository
    private var blockId: String? = null
    private var baselineVisual = emptyList<String>()
    private var baselineAudio = emptyList<String>()
    private val assets = linkedMapOf<String, MediaAsset>()
    private var oldVisual: BlockImage? = null
    private var staged = linkedMapOf<String, MediaAsset>()
    private val removals = linkedMapOf<String, String>()
    var visualReferences by mutableStateOf<List<EditorReference>>(emptyList()); private set
    var audioReferences by mutableStateOf<List<EditorReference>>(emptyList()); private set
    var ready by mutableStateOf(false); private set
    val visual get() = visualReferences.firstOrNull()?.asset
    val audio get() = audioReferences.firstOrNull()?.asset
    val visualChanged get() = order(MediaRole.VISUAL) != baselineVisual
    val audioChanged get() = order(MediaRole.AUDIO) != baselineAudio
    val dirty get() = visualChanged || audioChanged
    fun isStaged(asset: MediaAsset) = asset.id in staged
    fun references(role: MediaRole) = if (role == MediaRole.VISUAL) visualReferences else audioReferences
    fun isSaved(role: MediaRole, identity: String) = identity in if (role == MediaRole.VISUAL) baselineVisual else baselineAudio
    private fun order(role: MediaRole) = references(role).map { it.identity }
    private fun setOrder(role: MediaRole, order: List<String>) {
        require(order.distinct().size == order.size)
        val items = order.map { identity ->
            if (identity.startsWith("library:")) EditorReference(identity, checkNotNull(assets[identity.removePrefix("library:")]) { "A Library reference is unavailable. Cancel and reopen this editor." })
            else { check(role == MediaRole.VISUAL && identity == oldVisual?.let { "legacy:${it.fileName}" }); EditorReference(identity, legacy = oldVisual) }
        }
        if (role == MediaRole.VISUAL) visualReferences = items else audioReferences = items
    }
    private var token: String? = null
    private var creationId = java.util.UUID.randomUUID().toString()
    private var savedId: String? = null
    private var commitTime: Long? = null
    private var commitBlock: String? = null
    val completedId get() = savedId

    suspend fun open(session: String, id: String?) = withContext(Dispatchers.IO) {
        if (token == session && ready) return@withContext
        require(session.matches(Regex("[0-9a-f-]{36}")))
        ready = false; token = session; blockId = id; savedId = null; commitTime = null; commitBlock = null
        creationId = java.util.UUID.randomUUID().toString()
        directory = File(root, session)
        check(directory.mkdirs() || directory.isDirectory)
        stageLibrary = MediaLibraryRepository(db, directory)
        ReferencePhotoCapture(app).recoverAbandoned()
        root.listFiles()?.filter { it.isDirectory && it.name != session && it.name.matches(Regex("[0-9a-f-]{36}")) &&
            System.currentTimeMillis() - it.lastModified() > 7 * 86_400_000L }?.forEach { it.deleteRecursively() }
        assets.clear(); staged.clear(); removals.clear()
        db.withTransaction {
            baselineVisual = id?.let { db.referenceOrder(it, MediaRole.VISUAL) }.orEmpty()
            baselineAudio = id?.let { db.referenceOrder(it, MediaRole.AUDIO) }.orEmpty()
            oldVisual = id?.let { db.images().find(it) }
            db.media().readAll().forEach { assets[it.id] = it }
        }
        setOrder(MediaRole.VISUAL, baselineVisual); setOrder(MediaRole.AUDIO, baselineAudio)
        val manifest = File(directory, "draft.json")
        if (manifest.exists()) {
            val json = JSONObject(manifest.readText())
            check(json.optString("block") == (id ?: ""))
            val version = json.optInt("version", 1); require(version in 1..2) { "This draft needs a newer app." }
            creationId = json.getString("creationId")
            savedId = json.optString("savedId").ifEmpty { null }
            if (savedId != null && db.blocks().find(checkNotNull(savedId)) != null) { ready = true; return@withContext }
            check(savedId == null) { "The saved Block is no longer available. Cancel this completed editor." }
            // The creation UUID witnesses the entire Room transaction, including week and Builder writes.
            if (id == null && db.blocks().find(creationId) != null) { savedId = creationId; ready = true; return@withContext }
            fun list(key: String, fallback: List<String>) = json.optJSONArray(key)?.let { a -> List(a.length()) { a.getString(it) } } ?: fallback
            baselineVisual = list("baselineVisual", listOfNotNull(json.optString("identity").ifEmpty { null }))
            baselineAudio = list("baselineAudio", listOfNotNull(json.optString("originalAudio").ifEmpty { null }?.let { "library:$it" }))
            val items = json.getJSONArray("staged")
            for (index in 0 until items.length()) {
                val item = items.getJSONObject(index)
                val asset = MediaAsset(item.getString("id"), item.getString("title"), item.getString("name"), item.getString("mime"),
                    item.getString("category"), item.getString("file"), item.getLong("bytes"), item.getLong("time"),
                    item.optInt("width").takeIf { it > 0 }, item.optInt("height").takeIf { it > 0 }, item.optInt("pages").takeIf { it > 0 },
                    item.optLong("duration").takeIf { it > 0 }, item.getString("hash"))
                require(asset.fileName.matches(Regex("[0-9a-f-]{36}\\.(jpg|png|webp|pdf|mp3|m4a|wav)")))
                staged[asset.id] = asset; assets[asset.id] = asset
            }
            if (version == 2) {
                // Preserve a removed legacy row's metadata so interrupted removal can still be witnessed.
                json.optJSONObject("legacy")?.let { l -> oldVisual = BlockImage(id!!, l.getString("file"), l.getString("mime"), l.getInt("width"), l.getInt("height"), l.getLong("bytes"), l.optInt("position")) }
                setOrder(MediaRole.VISUAL, list("visuals", emptyList())); setOrder(MediaRole.AUDIO, list("audios", emptyList()))
                json.optJSONObject("removals")?.let { r -> r.keys().forEach { removals[it] = r.getString(it) } }
                commitTime = json.optLong("commitTime").takeIf { it > 0 }
                commitBlock = json.optString("commitBlock").ifEmpty { null }
                if (id != null && committed(id)) { savedId = id; ready = true; return@withContext }
            } else {
                // Scalar intent replaces/removes ONLY the reviewed first source, retaining any other members.
                for (role in MediaRole.entries) {
                    val key = if (role == MediaRole.VISUAL) "visual" else "audio"
                    val baseline = if (role == MediaRole.VISUAL) baselineVisual else baselineAudio
                    val desired = baseline.toMutableList()
                    if (json.optBoolean("${key}Changed")) {
                        baseline.firstOrNull()?.let { desired.remove(it); removals[it] = lossSnapshot(role, it) }
                        json.optString(key).ifEmpty { null }?.let { selected -> desired.remove("library:$selected"); desired.add(0, "library:$selected") }
                    }
                    setOrder(role, desired)
                }
                // Recover a Stage2 commit whose final receipt write was interrupted. Require its
                // ENTIRE intended collection plus every selected staged asset, never a subset.
                val pending = (visualReferences + audioReferences).mapNotNull { it.asset }.filter { it.id in staged }
                if (id != null && pending.isNotEmpty() && db.withTransaction {
                    pending.all { db.media().find(it.id) == it } &&
                        db.referenceOrder(id, MediaRole.VISUAL) == order(MediaRole.VISUAL) &&
                        db.referenceOrder(id, MediaRole.AUDIO) == order(MediaRole.AUDIO)
                }) { savedId = id; persist(); ready = true; return@withContext }
            }
            // Unavailable temporary bytes stay represented and removable; Save validates every intended item.
        }
        directory.listFiles()?.filter { it.name != "draft.json" && it.name !in staged.values.map { a -> a.fileName } }?.forEach { it.delete() }
        persist(); ready = true
    }

    private suspend fun committed(id: String): Boolean = commitTime != null && db.withTransaction {
        db.blocks().find(id)?.let { it.updatedAt == commitTime && it.toString() == commitBlock } == true && db.referenceOrder(id, MediaRole.VISUAL) == order(MediaRole.VISUAL) &&
            db.referenceOrder(id, MediaRole.AUDIO) == order(MediaRole.AUDIO) &&
            (visualReferences + audioReferences).mapNotNull { it.asset }.all { db.media().find(it.id) != null }
    }

    private fun persist() {
        val json = JSONObject().put("version", 2).put("savedId", savedId ?: "").put("creationId", creationId).put("block", blockId ?: "")
            .put("baselineVisual", JSONArray(baselineVisual)).put("baselineAudio", JSONArray(baselineAudio))
            .put("visuals", JSONArray(order(MediaRole.VISUAL))).put("audios", JSONArray(order(MediaRole.AUDIO)))
            .put("removals", JSONObject(removals.toMap())).put("commitTime", commitTime).put("commitBlock", commitBlock)
        oldVisual?.let { a -> json.put("legacy", JSONObject().put("file", a.fileName).put("mime", a.mimeType).put("width", a.width).put("height", a.height).put("bytes", a.byteCount).put("position", a.position)) }
        val items = JSONArray()
        staged.values.forEach { a -> items.put(JSONObject().put("id", a.id).put("title", a.title).put("name", a.originalName)
            .put("mime", a.mimeType).put("category", a.category).put("file", a.fileName).put("bytes", a.byteCount)
            .put("time", a.importedAt).put("width", a.width).put("height", a.height).put("pages", a.pageCount).put("duration", a.durationMs).put("hash", a.sha256)) }
        json.put("staged", items)
        val pending = File(directory, "draft.tmp")
        java.io.FileOutputStream(pending).use { it.write(json.toString().toByteArray()); it.fd.sync() }
        java.nio.file.Files.move(pending.toPath(), File(directory, "draft.json").toPath(), java.nio.file.StandardCopyOption.ATOMIC_MOVE, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
        directory.setLastModified(System.currentTimeMillis())
    }

    private fun change(role: MediaRole, desired: List<String>) {
        val previous = order(role)
        setOrder(role, desired)
        try { persist() } catch (failure: Exception) { setOrder(role, previous); throw failure }
    }
    suspend fun choose(role: MediaRole, id: String): Boolean = withContext(Dispatchers.IO) {
        check(ready)
        val asset = checkNotNull(db.media().find(id)) { "This Library item is unavailable." }
        add(role, asset)
    }
    private fun add(role: MediaRole, asset: MediaAsset): Boolean {
        require(role.accepts(asset.category))
        if ("library:${asset.id}" in order(role)) return false
        assets[asset.id] = asset; change(role, order(role) + "library:${asset.id}"); return true
    }
    suspend fun move(role: MediaRole, identity: String, delta: Int) = withContext(Dispatchers.IO) {
        check(ready); require(delta == -1 || delta == 1)
        val list = order(role).toMutableList(); val index = list.indexOf(identity)
        check(index >= 0); val target = index + delta
        if (target in list.indices) { java.util.Collections.swap(list, index, target); change(role, list) }
    }
    private suspend fun lossSnapshot(role: MediaRole, identity: String): String {
        val id = blockId ?: return "pending"
        return if (role == MediaRole.VISUAL) db.overlays().read(id, identity).sortedBy { it.id }.toString()
        else { val media = identity.removePrefix("library:"); "${db.blockAudio().state(id, media)}|${db.blockAudio().read(id, media).sortedBy { it.id }}" }
    }
    suspend fun remove(role: MediaRole, identity: String, confirmed: Boolean = false) = withContext(Dispatchers.IO) {
        check(ready && identity in order(role))
        val baseline = if (role == MediaRole.VISUAL) baselineVisual else baselineAudio
        if (identity in baseline) {
            check(confirmed) { "Review and confirm removing this saved reference first." }
            removals[identity] = db.withTransaction { lossSnapshot(role, identity) }
        }
        change(role, order(role).filter { it != identity })
    }
    // Compatibility for old internal scalar callers: never clear a whole role.
    suspend fun select(role: MediaRole, asset: MediaAsset?) {
        references(role).firstOrNull()?.let { remove(role, it.identity, confirmed = true) }
        if (asset != null) { assets[asset.id] = asset; add(role, asset) }
    }
    suspend fun import(role: MediaRole, uri: Uri) = withContext(Dispatchers.IO) {
        val resolver = app.contentResolver; val mime = resolver.getType(uri)
        val name = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { if (it.moveToFirst() && !it.isNull(0)) it.getString(0) else null }
            ?: when (mime) { "image/jpeg" -> "Photo.jpg"; "image/png" -> "Photo.png"; "image/webp" -> "Photo.webp"; else -> error("Choose a file with its original filename.") }
        stage(role, name, mime) { checkNotNull(resolver.openInputStream(uri)) }
    }
    internal suspend fun stage(role: MediaRole, name: String, mime: String?, open: () -> java.io.InputStream): Boolean = withContext(Dispatchers.IO) {
        check(ready)
        val asset = stageLibrary.stage(name, mime, role, open)
        // Reuse a verified canonical original, or the same pending bytes, rather than copying it again on Save.
        val existing = db.media().findMatching(asset.mimeType, asset.byteCount, asset.sha256).firstOrNull { library.available(it) }
            ?: staged.values.firstOrNull { it.mimeType == asset.mimeType && it.byteCount == asset.byteCount && it.sha256 == asset.sha256 && stageLibrary.available(it) }
        if (existing != null) { File(directory, asset.fileName).delete(); return@withContext add(role, existing) }
        staged[asset.id] = asset
        try { add(role, asset) } catch (failure: Exception) { staged.remove(asset.id); File(directory, asset.fileName).delete(); throw failure }
    }
    suspend fun photo(capture: ReferencePhotoCapture, name: String): Boolean = try {
        stage(MediaRole.VISUAL, "Camera photo.jpg", "image/jpeg") { capture.file(name).inputStream() }
    } finally { capture.discard(name) }
    suspend fun preview(asset: MediaAsset, page: Int): ReferencePage = if (asset.id in staged)
        ImageReferenceRepository(db, directory).loadOriginalPage(File(directory, asset.fileName), asset.mimeType, page) else library.visualPage(asset.id, page)
    fun audioFile(asset: MediaAsset) = File(if (asset.id in staged) directory else File(app.filesDir, "media-library"), asset.fileName)
    suspend fun discard() = withContext(Dispatchers.IO) {
        if (::directory.isInitialized) check(directory.deleteRecursively() || !directory.exists()) { "Temporary cleanup failed. Retry." }
        ready = false; token = null; staged.clear(); visualReferences = emptyList(); audioReferences = emptyList()
    }

    suspend fun save(id: String?, draft: BlockDraft, week: LocalDate?, appendToBuilder: Boolean = false,
        onPromoted: suspend (Int) -> Unit = {},
        beforeCommit: suspend () -> Unit = {}): String = withContext(Dispatchers.IO + NonCancellable) {
        check(ready && id == blockId); require(draft.errors().isEmpty())
        savedId?.let { return@withContext it }
        if (id == null && db.blocks().find(creationId) != null) { savedId = creationId; return@withContext creationId }
        if (id != null && committed(id)) { savedId = id; return@withContext id }
        val selected = (visualReferences + audioReferences).mapNotNull { it.asset }.distinctBy { it.id }
        val promoted = mutableListOf<Pair<MediaAsset, File>>()
        val canonical = File(app.filesDir, "media-library")
        val result = ImageReferenceRepository.filesMutex.withLock { MediaLibraryRepository.filesMutex.withLock {
            try {
                check(canonical.mkdirs() || canonical.isDirectory)
                selected.filter { it.id in staged }.forEach { asset ->
                    check(stageLibrary.available(asset)) { "A pending original is missing or changed. Remove that item or retry." }
                    val target = File(canonical, asset.fileName)
                    if (target.exists() && db.media().find(asset.id) == null) check(target.delete()) { "Could not recover a pending promotion. Check storage and retry." }
                    check(!target.exists()) { "Pending media already exists. Cancel and reopen to recover." }
                    promoted += asset to target
                    File(directory, asset.fileName).inputStream().use { input -> java.io.FileOutputStream(target).use { output -> input.copyTo(output); output.fd.sync() } }
                    onPromoted(promoted.size)
                }
                // Synced all-item intent precedes commit. updatedAt is the transaction witness for an existing Block.
                commitTime = maxOf(System.currentTimeMillis(), (id?.let { db.blocks().find(it)?.updatedAt } ?: 0L) + 1)
                persist()
                db.withTransaction {
                    if (id != null) for (role in MediaRole.entries) check(db.referenceOrder(id, role) == if (role == MediaRole.VISUAL) baselineVisual else baselineAudio) {
                        "References changed elsewhere. Your draft is retained. Cancel and reopen this Block to review the latest references."
                    }
                    promoted.forEach { (asset, _) -> db.media().insert(asset) }
                    for (role in MediaRole.entries) for (item in references(role)) {
                        item.asset?.let { asset -> checkNotNull(db.media().find(asset.id)); require(role.accepts(asset.category)); check(library.available(asset)) { "A reference original is unavailable. Your draft is retained." } }
                        item.legacy?.let { image ->
                            val original = File(app.filesDir, "block-images/${image.fileName}")
                            check(original.isFile && original.length() == image.byteCount) { "The Block-owned original is unavailable. Your draft is retained." }
                            // The legacy file lock is already held; do not reacquire it through loadPage.
                            legacy.loadOriginalPage(original, image.mimeType, 0).bitmap.recycle()
                        }
                    }
                    val saved = BlockRepository(db) { checkNotNull(commitTime) }.save(id, draft, week, creationId)
                    for (role in MediaRole.entries) {
                        val baseline = if (role == MediaRole.VISUAL) baselineVisual else baselineAudio
                        val desired = order(role)
                        for (identity in baseline.filter { it !in desired }) {
                            check(removals[identity] == lossSnapshot(role, identity)) { "Saved markings or audio work changed. Review removal again before saving." }
                            if (role == MediaRole.VISUAL) {
                                db.discardVisualMarkings(saved, identity, VisualChangeCheck(identity, true))
                                if (identity.startsWith("legacy:")) db.images().remove(saved) else db.blockMedia().removeExact(saved, role.name, identity.removePrefix("library:"))
                            } else associations.removeAudio(saved, identity.removePrefix("library:"), identity.removePrefix("library:"))
                        }
                        for (identity in desired.filter { it !in baseline }) {
                            require(identity.startsWith("library:")); associations.appendChecked(saved, role, identity.removePrefix("library:"))
                        }
                        db.writeReferenceOrder(saved, role, desired)
                    }
                    if (id == null && appendToBuilder) SessionRepository(db).addBlock(saved)
                    commitBlock = checkNotNull(db.blocks().find(saved)).toString()
                    persist()
                    beforeCommit(); saved
                }
            } catch (failure: Exception) {
                promoted.forEach { (asset, file) -> runCatching { if (db.media().find(asset.id) == null) file.delete() } }
                throw failure
            }
        } }
        savedId = result
        // Retain all-item metadata in the receipt; only delete temporary originals after Room commits.
        runCatching { persist() }
        staged.values.forEach { File(directory, it.fileName).delete() }
        runCatching { legacy.recover() }
        result
    }
}
