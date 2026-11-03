package com.mrayover.practicelogger

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class) @Config(sdk = [35])
class MediaMigrationTest {
    private val context get() = ApplicationProvider.getApplicationContext<android.content.Context>()
    private fun schema(version: Int) = javaClass.classLoader!!.getResourceAsStream("com.mrayover.practicelogger.BlockDatabase/$version.json")!!.bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
    private fun create(version: Int, name: String): android.database.sqlite.SQLiteDatabase {
        val schema = schema(version); val file = context.getDatabasePath(name); file.parentFile!!.mkdirs()
        val sql = android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(file, null)
        val entities = schema.getJSONArray("entities")
        repeat(entities.length()) { i ->
            val entity = entities.getJSONObject(i); val table = entity.getString("tableName")
            sql.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
            val indices = entity.optJSONArray("indices") ?: org.json.JSONArray()
            repeat(indices.length()) { sql.execSQL(indices.getJSONObject(it).getString("createSql").replace("\${TABLE_NAME}", table)) }
        }
        val queries = schema.getJSONArray("setupQueries"); repeat(queries.length()) { sql.execSQL(queries.getString(it)) }
        sql.version = version
        return sql
    }
    @Test fun elevenToTwelvePreservesEveryExistingRowAndForeignKey(): Unit = runBlocking {
        val name = "migration-${UUID.randomUUID()}.db"; val sql = create(11, name)
        sql.execSQL("INSERT INTO blocks VALUES ('b','Legacy','Play',5,90,'Notes',1,1,2)")
        sql.execSQL("INSERT INTO sessions VALUES ('s',1,NULL,'IN_PROGRESS',0,'Session note')")
        sql.execSQL("INSERT INTO placements VALUES ('p','s','b','Legacy','Play',90,'Reference',0,300000,1234,'PENDING',0,'Quick note',126)")
        sql.execSQL("INSERT INTO routines VALUES ('r','Routine',1,2)")
        sql.execSQL("INSERT INTO routine_placements VALUES ('rp','r','b',0,60000)")
        for ((id, mode, ext) in listOf(Triple("free", "FREE", "m4a"), Triple("loop", "LOOP", "wav"), Triple("imported", "IMPORTED_LOOP", "mp3")))
            sql.execSQL("INSERT INTO takes VALUES ('$id','b',3,'$mode',1234,'${UUID.randomUUID()}.$ext','Original $id',NULL,NULL)")
        sql.execSQL("INSERT INTO goals VALUES ('g','Goal','Outcome',20,'Goal note',0,0,1,2)")
        sql.execSQL("INSERT INTO goal_blocks VALUES ('g','b',0)")
        sql.execSQL("INSERT INTO session_goals VALUES ('s','g')")
        sql.execSQL("INSERT INTO daily_logs VALUES ('2026-11-03','Keep Log')")
        sql.execSQL("INSERT INTO block_images VALUES ('b','${UUID.randomUUID()}.pdf','application/pdf',600,800,1000)")
        sql.execSQL("INSERT INTO practice_notes VALUES ('n','b','s','p',4,'Keep Note')")
        val entities = schema(11).getJSONArray("entities")
        val before = (0 until entities.length()).associate { i ->
            val table = entities.getJSONObject(i).getString("tableName")
            table to sql.rawQuery("SELECT * FROM $table", null).use { cursor -> buildList { while (cursor.moveToNext()) add((0 until cursor.columnCount).map { cursor.getString(it) }) } }
        }
        sql.close(); val db = BlockDatabase.open(context, name)
        try {
            assertTrue(db.media().readAll().isEmpty())
            before.forEach { (table, rows) -> db.openHelper.readableDatabase.query("SELECT * FROM $table").use { cursor ->
                assertEquals(table, rows, buildList { while (cursor.moveToNext()) add((0 until cursor.columnCount).map { cursor.getString(it) }) })
            } }
            db.openHelper.readableDatabase.query("PRAGMA foreign_key_check").use { assertEquals(0, it.count) }
            val dependencies = db.blocks().dependencies("b")
            assertEquals(3, dependencies.recordings); assertEquals(1, dependencies.images)
            try { db.blocks().delete("b"); fail("Deleted referenced Block") } catch (_: android.database.sqlite.SQLiteConstraintException) { }
            // Room validates generated schema12 on open, including its unique storage index.
            val asset = MediaAsset(UUID.randomUUID().toString(), "New", "New.png", "image/png", "IMAGE", "file.png", 1, 1)
            db.media().insert(asset)
            try { db.media().insert(asset.copy(id = UUID.randomUUID().toString())); fail("Duplicated storage ownership") } catch (_: android.database.sqlite.SQLiteConstraintException) { }
        } finally { db.close(); context.deleteDatabase(name) }
    }
    @Test fun versionOneMigratesThroughAllExplicitPathsAndRetainsOriginalBlock(): Unit = runBlocking {
        val name = "chain-${UUID.randomUUID()}.db"; val sql = create(1, name)
        sql.execSQL("INSERT INTO blocks VALUES ('b','Original','Play',5,90,'Notes',0,1,2)"); sql.close()
        val db = BlockDatabase.open(context, name)
        try {
            assertEquals("Original", db.blocks().find("b")!!.name); assertTrue(db.media().readAll().isEmpty())
            assertEquals(12, db.openHelper.readableDatabase.version)
            db.openHelper.readableDatabase.query("PRAGMA foreign_key_check").use { assertEquals(0, it.count) }
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
