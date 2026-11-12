package com.mrayover.practicelogger

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.time.LocalDate
import java.time.ZoneId
import java.math.BigDecimal
import java.math.RoundingMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

internal const val MAX_WEEKLY_TARGET_MINUTES = 10_080

@Entity(tableName = "weekly_practice_targets")
data class WeeklyPracticeTarget(@PrimaryKey val weekStart: String, val targetMinutes: Int)

internal fun validateWeeklyTarget(target: WeeklyPracticeTarget) {
    val date = LocalDate.parse(target.weekStart)
    require(date.toString() == target.weekStart && practiceWeek(date) == date) { "Week must start on a canonical Sunday date." }
    require(target.targetMinutes in 1..MAX_WEEKLY_TARGET_MINUTES) { "Enter 1–10,080 whole minutes per week." }
}

internal fun parseWeeklyTarget(text: String): Int? = text.takeIf { it.isNotEmpty() && it.all { c -> c in '0'..'9' } }
    ?.toIntOrNull()?.takeIf { it in 1..MAX_WEEKLY_TARGET_MINUTES }

@Dao
interface WeeklyPracticeTargetDao {
    @Query("SELECT * FROM weekly_practice_targets ORDER BY weekStart")
    fun observeAll(): Flow<List<WeeklyPracticeTarget>>
    @Query("SELECT * FROM weekly_practice_targets WHERE weekStart = :week")
    suspend fun find(week: String): WeeklyPracticeTarget?
    @Query("SELECT COUNT(*) FROM weekly_practice_targets")
    suspend fun count(): Int
    @Upsert suspend fun save(target: WeeklyPracticeTarget)
    @Query("DELETE FROM weekly_practice_targets WHERE weekStart = :week")
    suspend fun remove(week: String)
}

internal class WeeklyTargetRepository(private val db: BlockDatabase, private val today: () -> LocalDate = { LocalDate.now() }) {
    suspend fun set(week: LocalDate, minutes: Int?) = db.withTransaction {
        require(week == practiceWeek(week) && !week.isBefore(practiceWeek(today()))) { "Past weekly targets are read-only." }
        if (minutes == null) db.weeklyTargets().remove(week.toString())
        else WeeklyPracticeTarget(week.toString(), minutes).also { validateWeeklyTarget(it); db.weeklyTargets().save(it) }
    }
}

internal val WEEKLY_TARGET_MIGRATION_25_26 = object : Migration(25, 26) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS weekly_practice_targets (weekStart TEXT NOT NULL, targetMinutes INTEGER NOT NULL, PRIMARY KEY(weekStart))")
    }
}

internal fun targetWeekMs(entries: List<PracticeTimeEntry>, week: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Long =
    weeklyPracticeMs(entries, practiceWeek(week), zone)

/** No rounded minutes enter the ratio, and only the visual fill is capped. */
data class WeeklyTargetProgress(val actualMs: Long, val targetMinutes: Int?) {
    val targetMs: Long? get() = targetMinutes?.toLong()?.times(60_000L)
    val ratio: Double? get() = targetMs?.let { actualMs.toDouble() / it }
    val fill: Float get() = ratio?.coerceIn(0.0, 1.0)?.toFloat() ?: 0f
    val percentage: String? get() = ratio?.let {
        val percent = BigDecimal.valueOf(actualMs).multiply(BigDecimal(100)).divide(BigDecimal(targetMs!!), 1, RoundingMode.HALF_UP)
        if (actualMs > 0 && percent.signum() == 0) "<0.1%" else "${percent.stripTrailingZeros().toPlainString()}%"
    }
    val differenceMs: Long? get() = targetMs?.let { actualMs - it }
}

internal fun targetMinuteText(ms: Long): String {
    val minutes = BigDecimal.valueOf(ms).divide(BigDecimal(60_000), 2, RoundingMode.HALF_UP)
    return if (ms > 0 && minutes.signum() == 0) "<0.01" else minutes.stripTrailingZeros().toPlainString()
}

class WeeklyTargetViewModel(app: Application) : AndroidViewModel(app) {
    private val db = BlockDatabase.appInstance(app)
    private val repository = WeeklyTargetRepository(db)
    val targets = db.weeklyTargets().observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    var busy by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    fun save(week: LocalDate, minutes: Int?, done: () -> Unit) {
        if (busy) return
        busy = true; error = null
        viewModelScope.launch {
            try { repository.set(week, minutes); done() }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { error = "Target could not be saved. Check the week and retry; your entry is retained." }
            finally { busy = false }
        }
    }
}
