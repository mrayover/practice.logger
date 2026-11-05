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
    val blockId: String?,
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

/** Nullable ownership is the standalone collection; existing Block foreign keys remain enforced. */
val FREE_RECORDING_MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE takes_new (id TEXT NOT NULL, blockId TEXT, createdAt INTEGER NOT NULL, mode TEXT NOT NULL, durationMs INTEGER NOT NULL, fileName TEXT NOT NULL, label TEXT NOT NULL, recordedBpm INTEGER, beatCount INTEGER, PRIMARY KEY(id), FOREIGN KEY(blockId) REFERENCES blocks(id) ON UPDATE NO ACTION ON DELETE RESTRICT)")
        db.execSQL("INSERT INTO takes_new (id,blockId,createdAt,mode,durationMs,fileName,label,recordedBpm,beatCount) SELECT id,blockId,createdAt,mode,durationMs,fileName,label,recordedBpm,beatCount FROM takes")
        db.execSQL("DROP TABLE takes")
        db.execSQL("ALTER TABLE takes_new RENAME TO takes")
        db.execSQL("CREATE INDEX index_takes_blockId ON takes(blockId)")
        db.execSQL("CREATE UNIQUE INDEX index_takes_fileName ON takes(fileName)")
    }
}

data class PendingTake(val id: String, val blockId: String?, val createdAt: Long, val file: File, val phrase: PhraseSpec? = null, val imported: ImportedAudioInfo? = null)

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
    private val ownedName = Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}(\\.pending)?\\.(m4a|wav|mp3)")
    private fun resolve(name: String): File {
        require(ownedName.matches(name)) { "Invalid recording file name." }
        return File(directory, name).also { check(it.canonicalFile.parentFile == directory.canonicalFile) }
    }
    // Capture needs space immediately, without relying on the system reclaiming cache mid-write.
    @android.annotation.SuppressLint("UsableSpace")
    fun availableBytes(): Long = directory.usableSpace
    fun prepare(blockId: String?, phrase: PhraseSpec? = null): PendingTake = prepareFile(blockId, if (phrase == null) "m4a" else "wav", phrase)
    fun prepareImport(blockId: String, extension: String): PendingTake {
        require(extension in setOf("m4a", "wav", "mp3"))
        return prepareFile(blockId, extension, null, AudioImport.MAX_BYTES+512*1024)
    }
    private fun prepareFile(blockId: String?, extension: String, phrase: PhraseSpec?, minimumSpace: Long = 0): PendingTake {
        check(directory.isDirectory || directory.mkdirs()) { "Recording storage is unavailable." }
        requireRecordingSpace(availableBytes(), phrase)
        check(availableBytes() >= minimumSpace) { "Free at least 51 MiB before importing audio." }
        val id = UUID.randomUUID().toString(); val file = resolve("$id.pending.$extension")
        activeFiles.add(file.absolutePath)
        try { check(file.createNewFile()) } catch (failure: Exception) { activeFiles.remove(file.absolutePath); throw failure }
        return PendingTake(id, blockId, System.currentTimeMillis(), file, phrase)
    }
    fun abandon(pending: PendingTake) { pending.file.delete(); activeFiles.remove(pending.file.absolutePath) }
    /** Validates temporary audio without creating a permanent row or renaming its file. */
    fun inspect(pending: PendingTake): BlockTake {
        val duration = pending.imported?.let {
            require(it.durationMs in 1..AudioImport.MAX_DURATION_MS && pending.file.length() in 1..AudioImport.MAX_BYTES)
            it.durationMs
        } ?: pending.phrase?.let { spec ->
            require(PhraseWave.frames(pending.file) == spec.frames) { "Incomplete phrase." }; spec.durationMs
        } ?: inspectDuration(pending.file)
        require(duration > 0 && pending.file.length() > 0) { "No usable audio was recorded." }
        return BlockTake(pending.id, pending.blockId, pending.createdAt, mode = if (pending.imported != null) TakeKind.IMPORTED_LOOP.stored else if (pending.phrase == null) "FREE" else "LOOP",
            label = pending.imported?.name ?: "",
            durationMs = duration, fileName = pending.file.name, recordedBpm = pending.phrase?.bpm, beatCount = pending.phrase?.beats)
    }
    suspend fun save(pending: PendingTake): BlockTake = filesMutex.withLock {
      // Resolve commit/rollback before honoring cancellation, so a failed Save is safe to retry.
      withContext(NonCancellable) {
        val inspected = inspect(pending)
        java.io.FileOutputStream(pending.file, true).use { it.fd.sync() }
        val target = resolve(pending.id + "." + pending.file.extension)
        check(!target.exists() && pending.file.renameTo(target)) { "Could not finish the recording file." }
        val take = inspected.copy(fileName = target.name)
        try {
            db.withTransaction { take.blockId?.let { checkNotNull(db.blocks().find(it)) }; dao.insert(take) }
        } catch (failure: Exception) {
            // Keep failed transactions reviewable; never remove bytes referenced by a saved row.
            if (dao.find(take.id) == null) check(target.renameTo(pending.file)) { "Could not recover temporary audio after failed save." }
            throw failure
        }
        activeFiles.remove(pending.file.absolutePath)
        take
      }
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
    internal suspend fun <T> withSavedAudio(take: BlockTake, action: suspend (File) -> T): T = filesMutex.withLock {
        check(dao.find(take.id)?.fileName == take.fileName) { "The saved take is no longer available." }
        action(resolve(take.fileName))
    }

    fun export(take: BlockTake, output: OutputStream, checkActive: () -> Unit = {}): Long {
        val copied = audioFile(take).inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var total = 0L
            while (true) {
                checkActive()
                val count = input.read(buffer)
                if (count < 0) break
                output.write(buffer, 0, count)
                total += count
            }
            total
        }
        output.flush()
        return copied
    }
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
