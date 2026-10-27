package com.mrayover.practicelogger

import android.content.Context
import android.media.MediaMetadataRetriever
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.OutputStream
import java.util.UUID

@Entity(tableName = "takes", foreignKeys = [ForeignKey(entity = Block::class,
    parentColumns = ["id"], childColumns = ["blockId"], onDelete = ForeignKey.RESTRICT)],
    indices = [Index("blockId"), Index(value = ["fileName"], unique = true)])
data class BlockTake(
    @PrimaryKey val id: String,
    val blockId: String,
    val createdAt: Long,
    val mode: String = "FREE",
    val durationMs: Long,
    val fileName: String,
    val label: String = "",
    val recordedBpm: Int? = null,
    val beatCount: Int? = null
)

@Dao interface TakeDao {
    @Query("SELECT * FROM takes ORDER BY createdAt DESC, id") fun observeAll(): Flow<List<BlockTake>>
    @Query("SELECT * FROM takes ORDER BY createdAt DESC, id") suspend fun readAll(): List<BlockTake>
    @Query("SELECT * FROM takes WHERE id = :id") suspend fun find(id: String): BlockTake?
    @Insert suspend fun insert(take: BlockTake)
    @Query("UPDATE takes SET label = :label WHERE id = :id") suspend fun rename(id: String, label: String): Int
    @Query("DELETE FROM takes WHERE id = :id") suspend fun remove(id: String): Int
}

val RECORDING_MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS takes (id TEXT NOT NULL, blockId TEXT NOT NULL, createdAt INTEGER NOT NULL, mode TEXT NOT NULL, durationMs INTEGER NOT NULL, fileName TEXT NOT NULL, label TEXT NOT NULL, PRIMARY KEY(id), FOREIGN KEY(blockId) REFERENCES blocks(id) ON UPDATE NO ACTION ON DELETE RESTRICT)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_takes_blockId ON takes(blockId)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_takes_fileName ON takes(fileName)")
    }
}

val PHRASE_MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE takes ADD COLUMN recordedBpm INTEGER")
        db.execSQL("ALTER TABLE takes ADD COLUMN beatCount INTEGER")
    }
}

data class PendingTake(val id: String, val blockId: String, val createdAt: Long, val file: File, val phrase: PhraseSpec? = null)

/** Only generated UUID names are ever resolved inside this private directory. No user paths. */
class TakeRepository(private val db: BlockDatabase, private val directory: File,
    private val inspectDuration: (File) -> Long = ::recordedDuration) {
    constructor(db: BlockDatabase, context: Context) : this(db, File(context.filesDir, "recordings"))
    private val dao get() = db.takes()
    companion object {
        private val filesMutex = Mutex()
        private val activeFiles = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    }
    val takes = db.takes().observeAll()
    private val ownedName = Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}(\\.pending)?\\.(m4a|wav)")
    private fun resolve(name: String): File {
        require(ownedName.matches(name)) { "Invalid recording file name." }
        return File(directory, name).also { check(it.canonicalFile.parentFile == directory.canonicalFile) }
    }
    // Capture needs space immediately, without relying on the system reclaiming cache mid-write.
    @android.annotation.SuppressLint("UsableSpace")
    fun availableBytes(): Long = directory.usableSpace
    fun prepare(blockId: String, phrase: PhraseSpec? = null): PendingTake {
        check(directory.isDirectory || directory.mkdirs()) { "Recording storage is unavailable." }
        check(availableBytes() >= 2 * 1024 * 1024 + (phrase?.frames?.toLong() ?: 0L) * 2) { "Free some storage before recording." }
        val id = UUID.randomUUID().toString()
        val file = resolve("$id.pending." + if (phrase == null) "m4a" else "wav")
        activeFiles.add(file.absolutePath)
        try { check(file.createNewFile()) } catch (failure: Exception) { activeFiles.remove(file.absolutePath); throw failure }
        return PendingTake(id, blockId, System.currentTimeMillis(), file, phrase)
    }
    fun abandon(pending: PendingTake) { pending.file.delete(); activeFiles.remove(pending.file.absolutePath) }
    suspend fun save(pending: PendingTake): BlockTake = filesMutex.withLock {
        try {
        val duration = pending.phrase?.let { spec ->
            require(PhraseWave.read(pending.file).size == spec.frames) { "Incomplete phrase." }; spec.durationMs
        } ?: inspectDuration(pending.file)
        require(duration > 0 && pending.file.length() > 0) { "No usable audio was recorded." }
        java.io.FileOutputStream(pending.file, true).use { it.fd.sync() }
        val target = resolve(pending.id + "." + pending.file.extension)
        check(!target.exists() && pending.file.renameTo(target)) { "Could not finish the recording file." }
        val take = BlockTake(pending.id, pending.blockId, pending.createdAt, mode = if (pending.phrase == null) "FREE" else "LOOP",
            durationMs = duration, fileName = target.name, recordedBpm = pending.phrase?.bpm, beatCount = pending.phrase?.beats)
        try {
            db.withTransaction { checkNotNull(db.blocks().find(take.blockId)); dao.insert(take) }
        } catch (failure: Exception) {
            // Cancellation can arrive just after a commit; never delete audio referenced by a saved row.
            withContext(NonCancellable) { runCatching { if (dao.find(take.id) == null) target.delete() } }
            throw failure
        }
        take
        } finally { activeFiles.remove(pending.file.absolutePath) }
    }
    fun audioFile(take: BlockTake): File = resolve(take.fileName).also {
        check(it.isFile && it.length() > 0) { "The recording file is missing. Other takes are unchanged." }
    }
    suspend fun rename(id: String, label: String) {
        require(label.trim().length <= 120)
        check(dao.rename(id, label.trim()) == 1)
    }
    suspend fun delete(id: String): Boolean = filesMutex.withLock {
        val take = checkNotNull(dao.find(id))
        val file = resolve(take.fileName)
        db.withTransaction { check(dao.remove(id) == 1) }
        // If file deletion is interrupted, startup recovery retries the now-unreferenced private file.
        !file.exists() || file.delete()
    }
    fun export(take: BlockTake, output: OutputStream) { audioFile(take).inputStream().use { it.copyTo(output) }; output.flush() }
    /** Run once before enabling capture, after reading all metadata successfully. */
    suspend fun recover(): Int = filesMutex.withLock {
        check(directory.isDirectory || directory.mkdirs())
        val retained = dao.readAll().map { it.fileName }.toSet()
        var removed = 0
        checkNotNull(directory.listFiles()).forEach { file ->
            if (ownedName.matches(file.name) && file.name !in retained && file.absolutePath !in activeFiles) {
                check(file.delete()) { "Could not clean up an unfinished recording." }; removed++
            }
        }
        removed
    }
}

private fun recordedDuration(file: File): Long {
    val reader = MediaMetadataRetriever()
    try {
        reader.setDataSource(file.absolutePath)
        check(reader.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) == "yes")
        return checkNotNull(reader.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull())
    } finally { reader.release() }
}
