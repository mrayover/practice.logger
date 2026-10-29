package com.mrayover.practicelogger

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.DayOfWeek
import java.time.temporal.TemporalAdjusters

@Entity(tableName = "daily_logs")
data class DailyLog(@PrimaryKey val date: String, val text: String)

fun logDate(value: String): LocalDate {
    require(Regex("\\d{4}-\\d{2}-\\d{2}").matches(value)) { "Invalid Log date." }
    return LocalDate.parse(value).also { require(it.year in 1..9999 && it.toString() == value) { "Invalid Log date." } }
}
fun logWeek(date: LocalDate): LocalDate = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

@Dao interface LogDao {
    @Query("SELECT * FROM daily_logs ORDER BY date") suspend fun readAll(): List<DailyLog>
    @Query("SELECT * FROM daily_logs WHERE date = :date") suspend fun find(date: String): DailyLog?
    @Query("SELECT * FROM daily_logs WHERE date BETWEEN :first AND :last ORDER BY date")
    suspend fun range(first: String, last: String): List<DailyLog>
    @Query("SELECT EXISTS(SELECT 1 FROM daily_logs)") suspend fun hasEntries(): Boolean
    @Upsert suspend fun save(entry: DailyLog)
    @Insert suspend fun insert(entry: DailyLog)
}

val LOG_MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `daily_logs` (`date` TEXT NOT NULL, `text` TEXT NOT NULL, PRIMARY KEY(`date`))")
    }
}

/** Each edit carries its original date. Coalesce only edits for the same date.
 * The IO worker outlives screen navigation; flush is a durable navigation/lifecycle barrier. */
class LogWriter(private val save: suspend (DailyLog) -> Unit) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val signal = Channel<Unit>(Channel.CONFLATED)
    private val lock = Mutex()
    private val pending = linkedMapOf<String, DailyLog>()
    private val mutableStatus = MutableStateFlow("Saved")
    val status = mutableStatus.asStateFlow()
    init { scope.launch { for (ignored in signal) { try { flush() } catch (_: Exception) { /* Retain drafts for retry. */ } }; scope.cancel() } }
    fun edit(date: String, text: String) {
        logDate(date)
        synchronized(pending) { pending[date] = DailyLog(date, text); mutableStatus.value = "Saving…" }
        signal.trySend(Unit)
    }
    suspend fun flush() = withContext(Dispatchers.IO) {
        lock.withLock {
            try {
                while (true) {
                    val row = synchronized(pending) { pending.values.firstOrNull() } ?: break
                    save(row)
                    synchronized(pending) { if (pending[row.date] == row) pending.remove(row.date) }
                }
                synchronized(pending) { if (pending.isEmpty()) mutableStatus.value = "Saved" }
            } catch (e: Exception) {
                mutableStatus.value = "Could not save. Your text is kept here. Retry before leaving."
                throw e
            }
        }
    }
    fun close() { signal.close() }
}
