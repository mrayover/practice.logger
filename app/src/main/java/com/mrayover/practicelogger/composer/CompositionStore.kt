package com.mrayover.practicelogger.composer

import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING

data class CompositionListing(val projects: List<CompositionProject>, val unreadableCount: Int)

/** Owns only files/composer/projects. Call on IO; the shared lock serializes all in-process owners. */
class CompositionStore(private val directory: File, private val beforeCommit: (() -> Unit)? = null) {
    companion object { private val lock = Any() }
    private fun file(id: String): File {
        require(validCompositionId(id)) { "Invalid composition identity." }
        return File(directory, "$id.json")
    }
    fun list(): CompositionListing = synchronized(lock) {
        var invalid = 0
        // Recover a previous .bak and clean uncommitted .new files, only for UUID project names.
        val ids = directory.listFiles().orEmpty().mapNotNull { f ->
            val name = f.name.removeSuffix(".new").removeSuffix(".bak")
            name.removeSuffix(".json").takeIf { name.endsWith(".json") && validCompositionId(it) }
        }.toSet()
        val projects = ids.mapNotNull { id ->
            val base = file(id)
            if (!base.exists() && !File(base.path + ".bak").exists()) {
                // A never-committed creation has no saved version. Remove only its known temporary file.
                File(base.path + ".new").delete(); null
            } else runCatching { read(id) }.getOrElse { invalid++; null }
        }
        CompositionListing(projects.sortedByDescending { it.modifiedAt }, invalid)
    }
    fun read(id: String): CompositionProject = synchronized(lock) {
        val base = file(id)
        val backup = File(base.path + ".bak")
        if (backup.exists()) Files.move(backup.toPath(), base.toPath(), ATOMIC_MOVE, REPLACE_EXISTING)
        if (base.exists()) File(base.path + ".new").delete()
        val bytes = base.inputStream().use { input ->
            input.readBytesBounded(CompositionCodec.MAX_BYTES)
        }
        val decoder = Charsets.UTF_8.newDecoder()
        CompositionCodec.decode(decoder.decode(java.nio.ByteBuffer.wrap(bytes)).toString()).also {
            require(it.id == id) { "Composition identity does not match its file." }
        }
    }
    fun create(project: CompositionProject): CompositionProject = synchronized(lock) {
        CompositionCodec.validate(project)
        require(project.revision == 0L)
        val base = file(project.id)
        if (base.exists() || File(base.path + ".bak").exists()) {
            // Idempotent retry after Activity/process recovery; never rewrite a committed document.
            return@synchronized read(project.id).also {
                require(it.createdAt == project.createdAt) { "Composition identity is already in use." }
            }
        }
        write(project); project
    }
    fun save(project: CompositionProject): CompositionProject = synchronized(lock) {
        CompositionCodec.validate(project) // Reject bad input before even opening the old file.
        val current = read(project.id)
        require(current.revision == project.revision && current.createdAt == project.createdAt) { "This composition changed elsewhere. Reopen it before saving." }
        val saved = project.copy(revision = current.revision + 1,
            modifiedAt = maxOf(System.currentTimeMillis(), current.modifiedAt + 1))
        write(saved); saved
    }
    fun rename(project: CompositionProject, title: String): CompositionProject = save(project.copy(title = title.trim()))
    fun delete(project: CompositionProject) = synchronized(lock) {
        require(read(project.id).revision == project.revision) { "This composition changed. Refresh before deleting." }
        val base = file(project.id)
        // Removing the committed base is the deletion commit; stale temporary writes cannot resurrect it.
        Files.deleteIfExists(File(base.path + ".new").toPath())
        Files.deleteIfExists(File(base.path + ".bak").toPath())
        Files.delete(base.toPath())
    }
    private fun write(project: CompositionProject) {
        val bytes = CompositionCodec.encode(project).toByteArray(Charsets.UTF_8)
        check(directory.isDirectory || directory.mkdirs()) { "Composer storage is unavailable." }
        val base = file(project.id)
        val temporary = File(base.path + ".new")
        try {
            FileOutputStream(temporary).use { stream ->
                stream.write(bytes)
                stream.fd.sync()
            }
            beforeCommit?.invoke()
            // Same-directory atomic rename or an exception. Never fall back to delete-then-move.
            Files.move(temporary.toPath(), base.toPath(), ATOMIC_MOVE, REPLACE_EXISTING)
            check(read(project.id) == project) { "Composition save could not be verified. Reopen before retrying." }
        } catch (failure: Exception) {
            temporary.delete()
            throw failure
        }
    }
    private fun java.io.InputStream.readBytesBounded(limit: Int): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val count = read(buffer)
            if (count < 0) break
            require(out.size() <= limit - count) { "Composition file is too large." }
            out.write(buffer, 0, count)
        }
        return out.toByteArray()
    }
}
