package com.mrayover.practicelogger

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class) @Config(sdk = [35])
class WeeklyPracticeTargetTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private val week = LocalDate.parse("2026-11-08")
    private fun memory() = Room.inMemoryDatabaseBuilder(context, BlockDatabase::class.java).addCallback(mediaConstraintCallback).build()

    @Test fun createEditClearHistoricalPreservationAndRollover() = runBlocking {
        val name = "weekly-target-reopen.db"
        context.deleteDatabase(name)
        var db = BlockDatabase.open(context, name)
        var today = week.plusDays(4)
        try {
            var repo = WeeklyTargetRepository(db) { today }
            assertNull(db.weeklyTargets().find(week.toString()))
            repo.set(week, 180)
            assertEquals(180, db.weeklyTargets().observeAll().first().single().targetMinutes)
            repo.set(week, 200)
            assertEquals(200, db.weeklyTargets().find(week.toString())!!.targetMinutes)
            repo.set(week, null)
            assertEquals(0, db.weeklyTargets().count())
            repo.set(week, 180)
            db.close(); db = BlockDatabase.open(context, name)
            repo = WeeklyTargetRepository(db) { today }
            assertEquals(180, db.weeklyTargets().find(week.toString())!!.targetMinutes)
            today = week.plusWeeks(1)
            assertEquals(today, practiceWeek(today))
            assertNull(db.weeklyTargets().find(today.toString()))
            assertTrue(runCatching { repo.set(week, 300) }.isFailure)
            assertTrue(runCatching { repo.set(week, null) }.isFailure)
            repo.set(today.plusWeeks(1), 90)
            assertEquals(180, db.weeklyTargets().find(week.toString())!!.targetMinutes)
            assertNull(db.weeklyTargets().find(today.toString()))
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun positiveWholeBoundedCanonicalInputsOnly() = runBlocking {
        for (input in listOf("", "0", "-1", "1.5", "1e3", "+4", " 4", "10,080", "10081", "999999999999999999999", "４")) assertNull(input, parseWeeklyTarget(input))
        assertEquals(180, parseWeeklyTarget("180")); assertEquals(10080, parseWeeklyTarget("10080"))
        for (target in listOf(WeeklyPracticeTarget("2026-11-09", 180), WeeklyPracticeTarget("2026-11-8", 180), WeeklyPracticeTarget("2026-11-08", 0), WeeklyPracticeTarget("2026-11-08", 10081))) {
            assertTrue(runCatching { validateWeeklyTarget(target) }.isFailure)
        }
        val db = memory()
        try { assertTrue(runCatching { WeeklyTargetRepository(db) { week }.set(week.plusDays(1), 180) }.isFailure) }
        finally { db.close() }
    }

    @Test fun precisionUnderExactOverAndUnsetDoNotCapActual() {
        assertNull(WeeklyTargetProgress(90_000, null).percentage)
        assertEquals("50%", WeeklyTargetProgress(90 * 60_000L, 180).percentage)
        assertEquals(0.5f, WeeklyTargetProgress(90 * 60_000L, 180).fill)
        assertEquals("100%", WeeklyTargetProgress(180 * 60_000L, 180).percentage)
        val over = WeeklyTargetProgress(240 * 60_000L, 180)
        assertEquals("133.3%", over.percentage); assertEquals(1f, over.fill)
        assertEquals(60 * 60_000L, over.differenceMs)
        assertEquals(240 * 60_000L, over.actualMs)
        assertEquals("0.8%", WeeklyTargetProgress(90_000, 180).percentage)
        assertEquals("<0.1%", WeeklyTargetProgress(1, 180).percentage)
        assertEquals("1.5", targetMinuteText(90_000))
    }

    @Test fun sundayCalendarDiffersFromJournalAndUsesLocalDstBoundaries() {
        for (zoneName in listOf("America/Los_Angeles", "Pacific/Auckland", "UTC")) {
            val zone = ZoneId.of(zoneName)
            for (sunday in listOf(LocalDate.parse("2026-03-08"), LocalDate.parse("2026-11-01"))) {
                val from = sunday.atStartOfDay(zone).toInstant().toEpochMilli()
                val end = sunday.plusWeeks(1).atStartOfDay(zone).toInstant().toEpochMilli()
                val entries = listOf(PracticeTimeEntry(from - 1, 900), PracticeTimeEntry(from, 10),
                    PracticeTimeEntry(end - 1, 20), PracticeTimeEntry(end, 999))
                assertEquals(30L, targetWeekMs(entries, sunday.plusDays(6), zone))
            }
        }
        val zone = ZoneId.of("America/Los_Angeles")
        val sunday = week.atStartOfDay(zone).toInstant().toEpochMilli()
        val entries = listOf(PracticeTimeEntry(sunday, 90_000))
        assertEquals(90_000L, targetWeekMs(entries, week.plusDays(1), zone))
        assertEquals(0L, weeklyPracticeMs(entries, logWeek(week.plusDays(1)), zone))
        val instant = week.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        assertEquals(0L, targetWeekMs(listOf(PracticeTimeEntry(instant, 1)), week, zone))
        assertEquals(1L, targetWeekMs(listOf(PracticeTimeEntry(instant, 1)), week, ZoneId.of("UTC")))
    }

    @Test fun persistedG3UnionIncludesPartialUnfinishedRepeatedOvertimeAndCompletedTranscribeOnce() = runBlocking {
        val db = memory()
        try {
            val start = week.atTime(23, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val block = BlockRepository(db).save(null, BlockDraft("Study", "Play"))
            db.sessions().insert(PracticeSession("s", start, status = "IN_PROGRESS"))
            for ((i, ms) in listOf(30_001L, 125_000L).withIndex()) db.sessions().insert(Placement("p$i", "s", block, "Study", "Play", null, "", i, 60_000, ms, prepMs = 500_000))
            val goal = GoalRepository(db).save(null, "Goal", "", 50, "")
            db.goals().insert(GoalBlock(goal, block, 0)); db.goals().insert(GoalSession("s", goal))
            db.goalAttribution().insert(PlacementGoalSnapshot("p0", goal, 1))
            db.transcribeActivities().insert(TranscribeActivity("done", start, start + 100_000, 90_000, goal))
            db.transcribeActivities().insert(TranscribeActivity("unfinished", start, activeMs = 999_999))
            db.sessions().insert(PracticeSession("draft", start, status = "DRAFT"))
            db.sessions().insert(Placement("draft-p", "draft", block, "Study", "Play", null, "", 0, 60_000, 999_999))
            val entries = db.transcribeActivities().observePracticeTime().first()
            assertEquals(245_001L, targetWeekMs(entries, week))
            assertEquals(3, db.transcribeActivities().observeFacts().first().size)
            assertEquals(50, db.goals().find(goal)!!.goal.progress)
            WeeklyTargetRepository(db) { week }.set(week, 180)
            assertEquals(245_001L, targetWeekMs(db.transcribeActivities().observePracticeTime().first(), week))
        } finally { db.close() }
    }

    @Test fun room25UpgradePreservesAllExistingTableDefinitionsAndPractice() = runBlocking {
        val name = "w2-room25.db"; context.deleteDatabase(name)
        val file = context.getDatabasePath(name).apply { parentFile!!.mkdirs() }
        val schema = javaClass.classLoader!!.getResourceAsStream("com.mrayover.practicelogger.BlockDatabase/25.json")!!.bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
        SQLiteDatabase.openOrCreateDatabase(file, null).use { sql ->
            val entities = schema.getJSONArray("entities")
            repeat(entities.length()) { i ->
                val e = entities.getJSONObject(i)
                fun execute(q: String) = sql.execSQL(q.replace("$" + "{TABLE_NAME}", e.getString("tableName")))
                execute(e.getString("createSql"))
                e.optJSONArray("indices")?.let { indices -> repeat(indices.length()) { execute(indices.getJSONObject(it).getString("createSql")) } }
            }
            sql.execSQL("INSERT INTO blocks VALUES ('b','Block','Act',5,NULL,'Keep',1,1,2)")
            sql.execSQL("INSERT INTO sessions VALUES ('s',1,NULL,'IN_PROGRESS',0,'Session')")
            sql.execSQL("INSERT INTO placements VALUES ('p','s','b','Block','Act',NULL,'Ref',0,300000,1200,'PENDING',0,'Note',120,300)")
            schema.getJSONArray("setupQueries").let { q -> repeat(q.length()) { sql.execSQL(q.getString(it)) } }
            sql.version = 25
        }
        val db = BlockDatabase.open(context, name)
        try {
            assertEquals(27, db.openHelper.writableDatabase.version)
            assertEquals("Keep", db.blocks().find("b")!!.notes)
            assertEquals(1200L, db.sessions().find("s")!!.current!!.activeMs)
            assertEquals(300L, db.sessions().find("s")!!.current!!.prepMs)
            assertEquals(0, db.weeklyTargets().count())
            db.openHelper.writableDatabase.query("PRAGMA foreign_key_check").use { assertFalse(it.moveToFirst()) }
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun zip11RoundTripHistoricalTargetsZip10DefaultsAndTargetOnlyRestoreGuards() = runBlocking {
        val db = memory(); val restored = memory(); val old = memory()
        val folder = File(context.cacheDir, "w2-zip").apply { mkdirs() }
        try {
            db.weeklyTargets().save(WeeklyPracticeTarget(week.minusWeeks(1).toString(), 90))
            WeeklyTargetRepository(db) { week }.set(week, 180)
            val zip = PortableZip(context, db, folder)
            val file = File(folder, "targets.zip"); zip.create(file)
            val verified = zip.verify(file)
            assertEquals(PORTABLE_VERSION, verified.records.payload.getInt("version"))
            restored.withTransaction { verified.records.insertInto(restored.openHelper.writableDatabase) }
            assertEquals(db.weeklyTargets().observeAll().first(), restored.weeklyTargets().observeAll().first())
            assertFalse(portableTargetEmpty(restored.openHelper.writableDatabase))
            val json = BackupCodec.encode(BackupRepository(db).snapshot())
            assertFalse(json.toString(Charsets.UTF_8).contains("weekly_practice_targets"))
            assertTrue(runCatching { BackupRepository(restored).restore(BackupCodec.decode(json)) }.exceptionOrNull() is NonEmptyRestoreException)
            val historical = JSONObject(verified.records.payload.toString()).put("version", 10)
            historical.getJSONObject("tables").remove("weekly_practice_targets")
            historical.getJSONObject("tables").remove("playground_layouts")
            val records = PortableRecords(historical, 10); records.validateRelationships(context)
            // Exercise the frozen ZIP10 envelope as well as its records (no media in this fixture).
            val oldFile = File(folder, "targets-v10.zip")
            val recordBytes = historical.toString().toByteArray(Charsets.UTF_8)
            val hash = java.security.MessageDigest.getInstance("SHA-256").digest(recordBytes).joinToString("") { "%02x".format(it) }
            val manifest = java.util.zip.ZipFile(file).use { archive ->
                JSONObject(archive.getInputStream(archive.getEntry("manifest.json")).bufferedReader().use { it.readText() })
            }.put("version", 10).put("roomSchema", 25).put("counts", records.counts)
                .put("entries", JSONArray().put(PortableEntry("records.json", "records", "app", recordBytes.size.toLong(), hash).encode()))
            java.util.zip.ZipOutputStream(oldFile.outputStream()).use { archive ->
                for ((path, bytes) in listOf("records.json" to recordBytes, "manifest.json" to manifest.toString().toByteArray(Charsets.UTF_8))) {
                    archive.putNextEntry(java.util.zip.ZipEntry(path)); archive.write(bytes); archive.closeEntry()
                }
            }
            val oldArchive = zip.verify(oldFile)
            old.withTransaction { oldArchive.records.insertInto(old.openHelper.writableDatabase) }
            assertEquals(0, old.weeklyTargets().count())
            for (rows in listOf(JSONArray().put(JSONArray().put("2026-11-09").put(180)),
                JSONArray().put(JSONArray().put("2026-11-08").put(0)),
                JSONArray().put(JSONArray().put("2026-11-08").put(10081)),
                JSONArray().put(JSONArray().put("2026-11-08").put(1.5)),
                JSONArray().put(JSONArray().put("2026-11-8").put(180)),
                JSONArray().put(JSONArray().put("2026-11-08").put(180)).put(JSONArray().put("2026-11-08").put(90)))) {
                val bad = JSONObject(verified.records.payload.toString())
                bad.getJSONObject("tables").put("weekly_practice_targets", rows)
                assertTrue(runCatching { PortableRecords(bad, PORTABLE_VERSION) }.isFailure)
            }
        } finally { db.close(); restored.close(); old.close(); folder.deleteRecursively() }
    }
}
