package com.mrayover.practicelogger

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class) @Config(sdk = [35])
class LogRepositoryTest {
    private val context get() = ApplicationProvider.getApplicationContext<android.content.Context>()
    private fun memory() = Room.inMemoryDatabaseBuilder(context, BlockDatabase::class.java).build()
    private val empty = BackupData(emptyList(), emptyList(), emptyList())
    private fun rejects(action: () -> Unit) { try { action(); fail("Invalid Log accepted") } catch (_: Exception) { } }

    @Test fun rapidEditsStayOnOriginalDatesAndSurviveReopen(): Unit = runBlocking {
        val name = "log-persistence.db"; context.deleteDatabase(name)
        var db = BlockDatabase.open(context, name)
        val writer = LogWriter { db.logs().save(it) }
        try {
            repeat(150) { writer.edit("2026-10-29", "Text $it\n  whitespace 🎵"); writer.edit("2026-10-28", "Yesterday $it") }
            writer.flush()
            writer.edit("2026-10-29", ""); writer.flush(); writer.close()
            db.close(); db = BlockDatabase.open(context, name)
            assertEquals("", db.logs().find("2026-10-29")!!.text)
            assertEquals("Yesterday 149", db.logs().find("2026-10-28")!!.text)
            db.logs().save(DailyLog("2026-10-29", "Re-edited"))
            assertEquals(2, db.logs().readAll().size)
        } finally { writer.close(); db.close(); context.deleteDatabase(name) }
    }

    @Test fun flushWaitsForInflightSaveAndRetainsNewerEdits(): Unit = runBlocking {
        val entered = CompletableDeferred<Unit>(); val release = CompletableDeferred<Unit>()
        val saved = mutableMapOf<String, String>(); var first = true
        val writer = LogWriter { row ->
            if (first) { first = false; entered.complete(Unit); release.await() }
            saved[row.date] = row.text
        }
        try {
            writer.edit("2026-10-29", "First"); entered.await()
            writer.edit("2026-10-29", "Newest"); writer.edit("2026-10-28", "Past")
            val barrier = async { writer.flush() }; yield(); assertFalse(barrier.isCompleted)
            release.complete(Unit); barrier.await()
            assertEquals(mapOf("2026-10-29" to "Newest", "2026-10-28" to "Past"), saved)
            assertEquals("Saved", writer.status.value)
        } finally { release.complete(Unit); writer.close() }
    }

    @Test fun failedSaveCanRetryWithoutLosingDraft(): Unit = runBlocking {
        var shouldFail = true; var saved: DailyLog? = null
        val writer = LogWriter { if (shouldFail) error("Disk failure") else saved = it }
        try {
            writer.edit("2026-10-29", "Keep this")
            try { writer.flush(); fail("Expected failure") } catch (_: IllegalStateException) { }
            assertTrue(writer.status.value.startsWith("Could not save"))
            shouldFail = false; writer.flush()
            assertEquals(DailyLog("2026-10-29", "Keep this"), saved)
        } finally { writer.close() }
    }

    @Test fun weeksCrossYearLeapDayAndLocalBoundaries(): Unit = runBlocking {
        val db = memory()
        try {
            for (day in listOf("2025-12-28", "2025-12-29", "2026-01-01", "2026-01-04", "2026-01-05")) db.logs().save(DailyLog(day, day))
            val monday = logWeek(LocalDate.parse("2026-01-01"))
            assertEquals("2025-12-29", monday.toString())
            assertEquals(listOf("2025-12-29", "2026-01-01", "2026-01-04"), db.logs().range(monday.toString(), monday.plusDays(6).toString()).map { it.date })
            assertEquals(LocalDate.parse("2024-02-29"), logDate("2024-02-29"))
            val instant = java.time.Instant.parse("2026-10-30T01:00:00Z")
            assertEquals("2026-10-29", LocalDate.now(java.time.Clock.fixed(instant, java.time.ZoneId.of("America/Los_Angeles"))).toString())
            for (bad in listOf("2026-02-29", "2026-2-01", "2026-10-29T00:00:00Z", "0000-01-01")) rejects { logDate(bad) }
        } finally { db.close() }
    }

    @Test fun formatSixRoundTripsAndReadersOneThroughFiveRemainCompatible(): Unit = runBlocking {
        val source = memory(); val target = memory()
        try {
            source.logs().save(DailyLog("2026-10-28", "  Ideas\n🎵\n")); source.logs().save(DailyLog("2026-10-29", ""))
            val data = BackupRepository(source).snapshot(); val bytes = BackupCodec.encode(data)
            assertEquals(6, JSONObject(String(bytes)).getInt("version"))
            BackupRepository(target).restore(BackupCodec.decode(bytes))
            assertEquals(data, BackupRepository(target).snapshot())
            for (v in 1..5) {
                val root = JSONObject(String(BackupCodec.encode(empty))).put("version", v)
                root.remove("logs")
                val old = BackupCodec.decode(root.toString().toByteArray())
                assertEquals(empty, old)
            }
            try { BackupRepository(target).restore(empty); fail("Logs-only target overwritten") } catch (_: NonEmptyRestoreException) { }
            assertEquals(data, BackupRepository(target).snapshot())
        } finally { source.close(); target.close() }
    }

    @Test fun invalidLogsOversizeAndLateRestoreFailureAreSafe(): Unit = runBlocking {
        val db = memory()
        try {
            for (bad in listOf("2026-02-30", "", "2026-1-01", "0000-01-01")) rejects { BackupCodec.encode(empty.copy(logs = listOf(DailyLog(bad, "Text")))) }
            rejects { BackupCodec.encode(empty.copy(logs = List(2) { DailyLog("2026-10-29", "Duplicate") })) }
            rejects { BackupCodec.encode(empty.copy(logs = listOf(DailyLog("2026-10-29", "a".repeat(BACKUP_LIMIT))))) }
            val root = JSONObject(String(BackupCodec.encode(empty.copy(logs = listOf(DailyLog("2026-10-29", "Text"))))))
            root.getJSONArray("logs").getJSONObject(0).put("text", 42)
            rejects { BackupCodec.decode(root.toString().toByteArray()) }
            db.openHelper.writableDatabase.execSQL("CREATE TRIGGER reject_log BEFORE INSERT ON daily_logs BEGIN SELECT RAISE(ABORT, 'test'); END")
            val block = Block("b", "Keep", "Play", createdAt = 1, updatedAt = 1)
            try { BackupRepository(db).restore(empty.copy(blocks = listOf(block), logs = listOf(DailyLog("2026-10-29", "Test")))); fail("Partial restore") } catch (_: android.database.sqlite.SQLiteException) { }
            assertEquals(empty, BackupRepository(db).snapshot())
        } finally { db.close() }
    }

    @Test fun migrationEightPreservesAllOldTablesAndRecordings(): Unit = runBlocking {
        val name = "log-migration.db"; context.deleteDatabase(name)
        val schema = javaClass.classLoader!!.getResourceAsStream("com.mrayover.practicelogger.BlockDatabase/8.json")!!.bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
        val file = context.getDatabasePath(name); file.parentFile!!.mkdirs()
        val sql = android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(file, null)
        val entities = schema.getJSONArray("entities")
        repeat(entities.length()) { i ->
            val e = entities.getJSONObject(i); val table = e.getString("tableName")
            sql.execSQL(e.getString("createSql").replace("\${TABLE_NAME}", table))
            val indices = e.optJSONArray("indices") ?: org.json.JSONArray()
            repeat(indices.length()) { sql.execSQL(indices.getJSONObject(it).getString("createSql").replace("\${TABLE_NAME}", table)) }
        }
        sql.execSQL("INSERT INTO blocks VALUES ('b','Legacy','Play',5,90,'Notes',1,1,2)")
        sql.execSQL("INSERT INTO sessions VALUES ('s',1,NULL,'IN_PROGRESS',0,'Session note')")
        sql.execSQL("INSERT INTO placements VALUES ('p','s','b','Legacy','Play',90,'Reference',0,300000,1234,'PENDING',0,'Quick note',126)")
        sql.execSQL("INSERT INTO routines VALUES ('r','Routine',1,2)")
        sql.execSQL("INSERT INTO routine_placements VALUES ('rp','r','b',0,60000)")
        sql.execSQL("INSERT INTO takes VALUES ('t','b',3,'FREE',1234,'00000000-0000-0000-0000-000000000001.m4a','Original',NULL,NULL)")
        sql.execSQL("INSERT INTO goals VALUES ('g','Goal','Outcome',20,'Goal note',0,0,1,2)")
        sql.execSQL("INSERT INTO goal_blocks VALUES ('g','b',0)")
        sql.execSQL("INSERT INTO session_goals VALUES ('s','g')")
        val setup = schema.getJSONArray("setupQueries"); repeat(setup.length()) { sql.execSQL(setup.getString(it)) }
        val before = (0 until entities.length()).associate { i ->
            val table = entities.getJSONObject(i).getString("tableName")
            table to sql.rawQuery("SELECT * FROM $table", null).use { cursor -> buildList { while (cursor.moveToNext()) add((0 until cursor.columnCount).map { cursor.getString(it) }) } }
        }
        sql.version = 8; sql.close()
        val db = BlockDatabase.open(context, name)
        try {
            assertTrue(db.logs().readAll().isEmpty())
            before.forEach { (table, rows) ->
                db.openHelper.writableDatabase.query("SELECT * FROM $table").use { cursor ->
                    assertEquals(rows, buildList { while (cursor.moveToNext()) add((0 until cursor.columnCount).map { cursor.getString(it) }) })
                }
            }
            db.logs().save(DailyLog("2026-10-29", "New Log"))
            db.openHelper.writableDatabase.query("PRAGMA foreign_key_check").use { assertEquals(0, it.count) }
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
