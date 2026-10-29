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
class ImageMigrationTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    @Test fun migrationNinePreservesAllTablesAndAddsOnlyImages(): Unit = runBlocking {
        val name = "image-migration.db"; context.deleteDatabase(name)
        val schema = javaClass.classLoader!!.getResourceAsStream("com.mrayover.practicelogger.BlockDatabase/9.json")!!.bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
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
        sql.execSQL("INSERT INTO daily_logs VALUES ('2026-10-29','Keep Log')")
        val setup = schema.getJSONArray("setupQueries"); repeat(setup.length()) { sql.execSQL(setup.getString(it)) }
        val before = (0 until entities.length()).associate { i ->
            val table = entities.getJSONObject(i).getString("tableName")
            table to sql.rawQuery("SELECT * FROM $table", null).use { cursor -> buildList { while (cursor.moveToNext()) add((0 until cursor.columnCount).map { cursor.getString(it) }) } }
        }
        sql.version = 9; sql.close()
        val db = BlockDatabase.open(context, name)
        try {
            assertTrue(db.images().readAll().isEmpty())
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
