package com.mrayover.practicelogger

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class InspirationMigrationTest {
    @Test fun publishedSchemaMigratesAndReopensWithExistingData() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "v22.db"
        val file = context.getDatabasePath(name)
        file.parentFile!!.mkdirs()
        val schema = checkNotNull(javaClass.classLoader!!.getResourceAsStream("com.mrayover.practicelogger.BlockDatabase/22.json"))
            .bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
        try {
            SQLiteDatabase.openOrCreateDatabase(file, null).use { sql ->
                val entities = schema.getJSONArray("entities")
                repeat(entities.length()) { i ->
                    val table = entities.getJSONObject(i)
                    val tableName = table.getString("tableName")
                    sql.execSQL(table.getString("createSql").replace("$" + "{TABLE_NAME}", tableName))
                    val indexes = table.optJSONArray("indices") ?: return@repeat
                    repeat(indexes.length()) { j ->
                        sql.execSQL(indexes.getJSONObject(j).getString("createSql").replace("$" + "{TABLE_NAME}", tableName))
                    }
                }
                val setup = schema.getJSONArray("setupQueries")
                repeat(setup.length()) { sql.execSQL(setup.getString(it)) }
                sql.execSQL("INSERT INTO blocks VALUES ('old','Old Block','Play',4,NULL,'Legacy notes',0,1,2)")
                sql.execSQL("INSERT INTO week_blocks (weekStart,blockId,position) VALUES ('2026-11-08','old',0)")
                sql.execSQL("INSERT INTO daily_logs (date,text) VALUES ('2026-11-08','Saved journal')")
                sql.version = 22
            }
            val newEntry = Inspiration(UUID.randomUUID().toString(), "A new title")
            BlockDatabase.open(context, name).useDatabase { db ->
                assertEquals("Legacy notes", db.blocks().find("old")!!.notes)
                assertEquals("old", db.weekPlan().read("2026-11-08").single().blockId)
                assertEquals("Saved journal", db.logs().readAll().single().text)
                assertFalse(db.inspirations().hasEntries())
                db.inspirations().insert(newEntry)
            }
            BlockDatabase.open(context, name).useDatabase { db ->
                assertEquals(newEntry, db.inspirations().find(newEntry.id))
                assertTrue(db.inspirations().hasEntries())
            }
        } finally { context.deleteDatabase(name) }
    }
}

private inline fun <T> BlockDatabase.useDatabase(action: (BlockDatabase) -> T): T =
    try { action(this) } finally { close() }
