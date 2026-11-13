package com.mrayover.practicelogger

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class) @Config(sdk = [35])
class PlaygroundRepositoryTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private fun memory() = Room.inMemoryDatabaseBuilder(context, BlockDatabase::class.java).addCallback(mediaConstraintCallback).build()
    private suspend fun seed(db: BlockDatabase) {
        db.blocks().insert(Block("b", "Current title", "Current instruction", createdAt = 1, updatedAt = 1))
        db.sessions().insert(PracticeSession("s", 1, status = "IN_PROGRESS"))
        repeat(8) { i -> db.sessions().insert(Placement("p$i", "s", "b", "Original $i", "Original action", null, "", i, 60000)) }
        val sessions = SessionRepository(db) { 1000 }
        repeat(7) { i -> sessions.finishBlock("s", if (i == 6) "SKIPPED" else "COMPLETED", "", placementId = "p$i") }
    }
    @Test fun completionIdentityRepeatedBlockSkippedPendingAndRetries() = runBlocking {
        val db = memory()
        try {
            seed(db); val repo = PlaygroundRepository(db)
            val rows = repo.ensureLayouts(); assertEquals(6, rows.size)
            assertEquals(rows, repo.ensureLayouts())
            assertEquals(6, db.playground().completions().size)
            assertTrue(rows.none { it.placementId in setOf("p6", "p7") })
            assertTrue(runCatching { SessionRepository(db).finishBlock("s", "COMPLETED", "", placementId = "p0") }.isFailure)
            assertEquals(rows, repo.ensureLayouts())
            // Prior JSON/history placements alone must not invent completion evidence.
            db.sessions().update(db.sessions().find("s")!!.current!!.copy(outcome = "COMPLETED"))
            assertEquals(rows, repo.ensureLayouts())
            assertTrue(runCatching { repo.save(rows.first().copy(x = Double.NaN)) }.isFailure)
            assertTrue(runCatching { repo.save(rows.first().copy(placementId = "missing")) }.isFailure)
            assertEquals(rows, repo.ensureLayouts())
        } finally { db.close() }
    }
    @Test fun exactCoordinatesAndRotationSurviveDatabaseReopen() = runBlocking {
        val name = "pg1-reopen.db"; context.deleteDatabase(name)
        var db = BlockDatabase.open(context, name)
        try {
            seed(db)
            val rows = PlaygroundRepository(db).ensureLayouts()
            val first = rows.first(); val others = rows.drop(1)
            val rotated = rotatePlaygroundSquare(first, 23.456, others, playgroundWorld(rows.size))
            val moved = movePlaygroundSquare(rotated, WorldPoint(0.05, 0.03), others, playgroundWorld(rows.size))
            PlaygroundRepository(db).save(moved)
            db.close(); db = BlockDatabase.open(context, name)
            val reopened = PlaygroundRepository(db).ensureLayouts()
            assertEquals(moved, reopened.first { it.placementId == moved.placementId })
            assertEquals(others.toSet(), reopened.filter { it.placementId != moved.placementId }.toSet())
        } finally { db.close(); context.deleteDatabase(name) }
    }
    @Test fun immutableColorEvidenceAndNeutralLegacy() = runBlocking {
        val db = memory()
        try {
            seed(db)
            val goals = GoalRepository(db) { 1000 }
            val first = goals.save(null, "First", "", 0, "")
            val second = goals.save(null, "Second", "", 0, "")
            goals.setBlocks(second, listOf("b"))
            db.goalAttribution().setPrimary(BlockPrimaryGoal("b", second))
            SessionRepository(db) { 1000 }.finishBlock("s", "COMPLETED", "", placementId = "p7")
            val before = db.playground().completions()
            assertEquals(goalCompletionColor(2), before.first { it.placementId == "p7" }.colorArgb)
            goals.move(second, -1)
            assertEquals(0, db.goals().find(second)!!.goal.priority)
            assertEquals(1, db.goals().find(first)!!.goal.priority)
            db.goalAttribution().clearPrimary("b")
            db.blocks().update(db.blocks().find("b")!!.copy(name = "Changed again"))
            PlaygroundRepository(db).ensureLayouts()
            assertEquals(before, db.playground().completions())
            assertNull(before.first { it.placementId == "p1" }.colorArgb)
        } finally { db.close() }
    }
    @Test fun room26AdditiveMigrationPreservesEveryPreviousEntity() = runBlocking {
        val name = "pg1-room26.db"; context.deleteDatabase(name)
        val schema = javaClass.classLoader!!.getResourceAsStream("com.mrayover.practicelogger.BlockDatabase/26.json")!!.bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
        val file = context.getDatabasePath(name).apply { parentFile!!.mkdirs() }
        SQLiteDatabase.openOrCreateDatabase(file, null).use { sql ->
            val entities = schema.getJSONArray("entities")
            repeat(entities.length()) { i -> val e = entities.getJSONObject(i)
                fun execute(q: String) = sql.execSQL(q.replace("$" + "{TABLE_NAME}", e.getString("tableName")))
                execute(e.getString("createSql")); e.optJSONArray("indices")?.let { indices -> repeat(indices.length()) { execute(indices.getJSONObject(it).getString("createSql")) } }
            }
            sql.execSQL("INSERT INTO weekly_practice_targets VALUES ('2026-11-08',180)")
            schema.getJSONArray("setupQueries").let { q -> repeat(q.length()) { sql.execSQL(q.getString(it)) } }
            sql.version = 26
        }
        val db = BlockDatabase.open(context, name)
        try {
            assertEquals(27, db.openHelper.writableDatabase.version)
            assertEquals(180, db.weeklyTargets().find("2026-11-08")!!.targetMinutes)
            assertTrue(db.playground().readAll().isEmpty())
            db.openHelper.writableDatabase.query("PRAGMA foreign_key_check").use { assertFalse(it.moveToFirst()) }
            val newSchema = javaClass.classLoader!!.getResourceAsStream("com.mrayover.practicelogger.BlockDatabase/27.json")!!.bufferedReader().use { JSONObject(it.readText()).getJSONObject("database").getJSONArray("entities") }
            val old = schema.getJSONArray("entities")
            repeat(old.length()) { i ->
                val e = old.getJSONObject(i)
                val found = (0 until newSchema.length()).map { newSchema.getJSONObject(it) }.first { it.getString("tableName") == e.getString("tableName") }
                assertEquals(e.toString(), found.toString())
            }
        } finally { db.close(); context.deleteDatabase(name) }
    }
    @Test fun zip12ExactRoundTripAndFrozenZip11FallbackRejectsInvalidLayouts() = runBlocking {
        val db = memory(); val restored = memory(); val old = memory()
        val folder = File(context.cacheDir, "pg1-zips").apply { mkdirs() }
        try {
            seed(db); val repo = PlaygroundRepository(db); val rows = repo.ensureLayouts()
            val rotated = rotatePlaygroundSquare(rows.first(), 21.789, rows.drop(1), playgroundWorld(rows.size)); repo.save(rotated)
            val zip = PortableZip(context, db, folder); val file = File(folder, "pg1.zip"); zip.create(file)
            val verified = zip.verify(file)
            assertEquals(12, verified.records.payload.getInt("version"))
            restored.withTransaction { verified.records.insertInto(restored.openHelper.writableDatabase) }
            assertEquals(db.playground().readAll(), restored.playground().readAll())
            val historical = JSONObject(verified.records.payload.toString()).put("version", 11)
            historical.getJSONObject("tables").remove("playground_layouts")
            val records = PortableRecords(historical, 11); records.validateRelationships(context)
            val bytes = historical.toString().toByteArray(Charsets.UTF_8)
            val hash = java.security.MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
            val manifest = java.util.zip.ZipFile(file).use { archive -> JSONObject(archive.getInputStream(archive.getEntry("manifest.json")).bufferedReader().use { it.readText() }) }
                .put("version", 11).put("roomSchema", 26).put("counts", records.counts)
                .put("entries", JSONArray().put(PortableEntry("records.json", "records", "app", bytes.size.toLong(), hash).encode()))
            val oldFile = File(folder, "v11.zip")
            java.util.zip.ZipOutputStream(oldFile.outputStream()).use { out ->
                for ((path, data) in listOf("manifest.json" to manifest.toString().toByteArray(), "records.json" to bytes)) {
                    out.putNextEntry(java.util.zip.ZipEntry(path)); out.write(data); out.closeEntry()
                }
            }
            val oldVerified = zip.verify(oldFile)
            old.withTransaction { oldVerified.records.insertInto(old.openHelper.writableDatabase) }
            assertEquals(6, PlaygroundRepository(old).ensureLayouts().size)
            assertEquals(6, old.playground().completions().size)
            for (badRows in listOf(
                JSONArray().put(JSONArray().put("missing").put(0).put(0).put(0)),
                JSONArray().put(JSONArray().put("p0").put(1e9).put(0).put(0)),
                JSONArray().put(JSONArray().put("p0").put(0).put(0).put(0)).put(JSONArray().put("p1").put(0.5).put(0).put(45)),
                JSONArray().put(JSONArray().put("p0").put(0).put(0).put(0)).put(JSONArray().put("p0").put(2).put(0).put(0)))) {
                val bad = JSONObject(verified.records.payload.toString()); bad.getJSONObject("tables").put("playground_layouts", badRows)
                assertTrue(runCatching { PortableRecords(bad, 12) }.isFailure)
            }
        } finally { db.close(); restored.close(); old.close(); folder.deleteRecursively() }
    }
}
