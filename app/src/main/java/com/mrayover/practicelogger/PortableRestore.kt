package com.mrayover.practicelogger

import android.content.Context
import android.system.Os
import android.system.OsConstants
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.migration.Migration
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

@Entity(tableName = "portable_restore_witness")
internal data class PortableRestoreWitness(@PrimaryKey val token: String)

internal val PORTABLE_MIGRATION_16_17 = object : Migration(16, 17) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS portable_restore_witness (token TEXT NOT NULL PRIMARY KEY)")
    }
}

internal enum class RestoreBoundary { JOURNALED, PROMOTED, BEFORE_COMMIT, COMMITTED, PREFERENCES_SAVED }

internal class RestoreRecoveryRequired(cause: Exception) :
    IllegalStateException("Restore needs recovery before you can use the app. Retry recovery or reopen practice.logger.", cause)

/** Files are promoted under the media locks before committing references and their witness together. */
internal class PortableRestore(
    private val context: Context,
    private val db: BlockDatabase,
    private val files: File = context.filesDir,
    private val freeBytes: (File) -> Long = File::getUsableSpace,
    private val atBoundary: (RestoreBoundary) -> Unit = {}
) {
    private val directory = File(files, "portable-restore")
    private val journal = File(directory, "journal.json")
    private val pendingJournal = File(directory, "journal.pending")
    private val codec = PortableZip(context, db, files)

    data class Staged internal constructor(val directory: File, val verified: VerifiedPortableZip) {
        val media get() = File(directory, "media")
    }

    private data class Intent(val token: String, val inventory: List<PortableEntry>, val settings: PortablePreferences) {
        fun encode() = JSONObject().put("token", token).put("entries", JSONArray(inventory.map { it.encode() }))
            .put("preferences", settings.encode())

        companion object {
            fun decode(payload: JSONObject): Intent {
                val token = payload.getString("token")
                require(UUID.fromString(token).toString() == token)
                val entries = payload.getJSONArray("entries")
                val items = List(entries.length()) { PortableEntry.decode(entries.getJSONObject(it)) }
                require(items.map { it.path }.toSet().size == items.size)
                return Intent(token, items, PortablePreferences.decode(payload.getJSONObject("preferences")))
            }
        }
    }

    suspend fun stage(open: () -> InputStream): Staged = withContext(Dispatchers.IO) {
        ensureEmpty()
        require(!journal.exists()) { "Finish recovery before choosing a new backup." }
        check(directory.isDirectory || directory.mkdirs())
        val stage = File(directory, "stage-${UUID.randomUUID()}")
        check(stage.mkdir())
        try {
            check(freeBytes(stage) >= PORTABLE_SPACE_RESERVE) { "Free private storage before restoring." }
            val archive = File(stage, "input.zip")
            FileOutputStream(archive).use { output ->
                open().use { copyPortable(it, output, PORTABLE_ZIP_LIMIT, stage) }
                output.fd.sync()
            }
            val media = File(stage, "media")
            check(media.mkdir())
            val verified = codec.verify(archive, media)
            check(archive.delete())
            Staged(stage, verified)
        } catch (failure: Throwable) {
            stage.deleteRecursively()
            throw failure
        }
    }

    private suspend fun ensureEmpty() = db.withTransaction {
        if (!portableTargetEmpty(db.openHelper.writableDatabase)) throw NonEmptyRestoreException()
    }

    fun discard(staged: Staged) {
        require(staged.directory.parentFile == directory && staged.directory.name.startsWith("stage-"))
        staged.directory.deleteRecursively()
    }

    suspend fun restore(staged: Staged): Unit = withContext(Dispatchers.IO) {
        operation.withLock {
            require(staged.directory.parentFile == directory && staged.directory.isDirectory)
            withPortableMediaLocks {
                require(!journal.exists()) { "Finish recovery before restoring another backup." }
                val plan = Intent(UUID.randomUUID().toString(), staged.verified.inventory.values.toList(), staged.verified.preferences)
                // A stage is private, but recheck its bytes before making a durable intent.
                for (entry in plan.inventory) verifyOriginal(staged.media, entry)
                currentCoroutineContext().ensureActive()
                withContext(NonCancellable) commit@{
                    try {
                        db.withTransaction {
                            val sql = db.openHelper.writableDatabase
                            if (!portableTargetEmpty(sql)) throw NonEmptyRestoreException()
                            // Every final path must be absent before it can be owned by this intent.
                            for (entry in plan.inventory) {
                                check(!target(files, entry).exists()) { "Restore found a conflicting private original. Reopen and retry." }
                            }
                            writeIntent(plan)
                            atBoundary(RestoreBoundary.JOURNALED)
                            val parents = linkedSetOf<File>()
                            for (entry in plan.inventory) {
                                val final = target(files, entry)
                                val parent = final.parentFile!!
                                check(parent.isDirectory || parent.mkdir()) { "Could not create the private media folder." }
                                check(!final.exists() && target(staged.media, entry).renameTo(final)) { "Could not promote a restored original." }
                                parents.add(parent)
                                atBoundary(RestoreBoundary.PROMOTED)
                            }
                            // File bytes were synced at extraction. Persist both sides of each rename.
                            for (parent in parents) {
                                syncPrivateDirectory(parent)
                                syncPrivateDirectory(File(staged.media, parent.name))
                            }
                            if (parents.isNotEmpty()) syncPrivateDirectory(files)
                            staged.verified.records.insertInto(sql)
                            sql.execSQL("DELETE FROM portable_restore_witness")
                            sql.execSQL("INSERT INTO portable_restore_witness (token) VALUES (?)", arrayOf(plan.token))
                            atBoundary(RestoreBoundary.BEFORE_COMMIT)
                        }
                        atBoundary(RestoreBoundary.COMMITTED)
                    } catch (failure: Exception) {
                        if (!journal.exists()) throw failure
                        val committed = recoverOrRequire()
                        if (!committed) throw failure
                        return@commit
                    }
                    recoverOrRequire()
                    Unit
                }
            }
        }
    }

    /** Used before ViewModels and their orphan cleanup start, and to retry a failed finalization. */
    suspend fun recover(): Boolean = withContext(Dispatchers.IO) {
        operation.withLock { withPortableMediaLocks { recoverOrRequire() } }
    }

    private suspend fun recoverOrRequire(): Boolean =
        try { recoverLocked() }
        catch (failure: Exception) {
            if (failure is kotlinx.coroutines.CancellationException) throw failure
            throw RestoreRecoveryRequired(failure)
        }

    private suspend fun recoverLocked(): Boolean {
        if (!journal.exists()) {
            check(!pendingJournal.exists() || pendingJournal.delete()) { "Could not clean up the unfinished journal." }
            return false
        }
        val intent = journal.inputStream().use { input ->
            val output = java.io.ByteArrayOutputStream()
            copyPortable(input, output, 16L * 1024 * 1024)
            Intent.decode(readPortableObject(output.toByteArray()))
        }
        val committed = db.withTransaction {
            val sql = db.openHelper.writableDatabase
            sql.query("SELECT 1 FROM portable_restore_witness WHERE token = ?", arrayOf(intent.token)).use { it.moveToFirst() }
        }
        if (committed) {
            for (entry in intent.inventory) verifyOriginal(files, entry)
            intent.settings.apply(context)
            atBoundary(RestoreBoundary.PREFERENCES_SAVED)
        } else {
            // With no witness, this intent owns these previously absent paths only.
            val parents = linkedSetOf<File>()
            for (entry in intent.inventory) {
                val original = target(files, entry)
                if (original.exists()) {
                    check(original.delete()) { "Could not remove an uncommitted restore file." }
                    parents.add(original.parentFile!!)
                }
            }
            for (parent in parents) syncPrivateDirectory(parent)
        }
        journal.delete()
        check(!journal.exists()) { "Could not clear the recovery journal." }
        syncPrivateDirectory(directory)
        // A stale witness is harmless once the journal is absent. A future restore replaces it.
        return committed
    }

    private fun writeIntent(intent: Intent) {
        val bytes = intent.encode().toString().toByteArray(Charsets.UTF_8)
        require(bytes.size <= 16 * 1024 * 1024)
        FileOutputStream(pendingJournal).use { output ->
            output.write(bytes)
            output.fd.sync()
        }
        check(!journal.exists() && pendingJournal.renameTo(journal)) { "Could not publish the restore journal." }
        syncPrivateDirectory(directory)
        syncPrivateDirectory(files)
    }

    private fun target(root: File, entry: PortableEntry): File {
        val filename = entry.path.substringAfterLast('/')
        require(entry.path == "originals/${entry.type}/$filename")
        return portableMediaFile(root, entry.type, filename)
    }

    private suspend fun verifyOriginal(root: File, entry: PortableEntry) {
        val original = target(root, entry)
        require(original.isFile && original.length() == entry.bytes) { "A staged original is missing or changed." }
        val result = original.inputStream().use { copyPortable(it, null, entry.bytes) }
        require(result.bytes == entry.bytes && result.sha256 == entry.sha256) { "Restored original failed verification." }
    }

    companion object {
        private val operation = Mutex()
        private val startup = Mutex()
        @Volatile private var started = false

        suspend fun beforeAppStarts(context: Context) = withContext(Dispatchers.IO) {
            startup.withLock {
                if (started) return@withLock
                val restore = PortableRestore(context, BlockDatabase.appInstance(context))
                restore.recover()
                InspirationRepository(restore.db, context.filesDir).recover()
                restore.directory.listFiles()?.filter { it.name.startsWith("stage-") }?.forEach { it.deleteRecursively() }
                File(context.filesDir, "portable-backup").listFiles()?.filter { it.extension == "zip" }?.forEach { it.delete() }
                started = true
            }
        }
    }
}

internal fun syncPrivateDirectory(file: File) {
    check(file.isDirectory)
    val fd = Os.open(file.absolutePath, OsConstants.O_RDONLY, 0)
    try { Os.fsync(fd) } finally { Os.close(fd) }
}
