package com.mrayover.practicelogger

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.UUID

@RunWith(RobolectricTestRunner::class) @Config(sdk = [35])
class MediaMarkerTest {
    @Test fun migrationTwelveMarkersReopenRestrictAndTransactionalAssetDeletion(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val name = "markers-${UUID.randomUUID()}.db"
        val oldSchema = javaClass.classLoader!!.getResourceAsStream("com.mrayover.practicelogger.BlockDatabase/12.json")!!.bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
        val file = context.getDatabasePath(name); file.parentFile!!.mkdirs()
        android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(file, null).use { sql ->
            val entities = oldSchema.getJSONArray("entities")
            repeat(entities.length()) { i ->
                val entity = entities.getJSONObject(i); val table = entity.getString("tableName")
                sql.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                val indices = entity.getJSONArray("indices")
                repeat(indices.length()) { sql.execSQL(indices.getJSONObject(it).getString("createSql").replace("\${TABLE_NAME}", table)) }
            }
            val queries = oldSchema.getJSONArray("setupQueries"); repeat(queries.length()) { sql.execSQL(queries.getString(it)) }
            sql.execSQL("INSERT INTO blocks VALUES ('old','Retained','Play',5,90,'Keep',0,1,2)")
            sql.execSQL("INSERT INTO media_assets VALUES ('audio','Backing','Backing.wav','audio/wav','AUDIO','${UUID.randomUUID()}.wav',44,1,NULL,NULL,NULL,1000,'hash')")
            sql.version = 12
        }
        var db = BlockDatabase.open(context, name)
        val dir = File(context.cacheDir, "markers-${UUID.randomUUID()}")
        try {
            assertEquals("Retained", db.blocks().find("old")!!.name)
            assertEquals(13, db.openHelper.readableDatabase.version)
            val repo = MasterAudioMarkerRepository(db)
            val point = repo.save("audio", null, 100, null, "Solo", "First note")
            val section = repo.save("audio", null, 200, 700, "Phrase", "Voicing")
            repo.save("audio", point.id, 150, null, "Miles", "Edited")
            assertEquals("Miles", db.markers().find(point.id)!!.label)
            try { db.media().remove("audio"); fail("Removed owned markers") } catch (_: android.database.sqlite.SQLiteConstraintException) { }
            db.close(); db = BlockDatabase.open(context, name)
            assertEquals(2, db.markers().read("audio").size)
            assertEquals(700L, db.markers().find(section.id)!!.endMs)
            val updated = MasterAudioMarkerRepository(db)
            updated.save("audio", section.id, 250, 800, "Phrase 2", "New")
            updated.delete("audio", point.id)
            assertEquals(1, db.markers().read("audio").size)
            db.openHelper.writableDatabase.execSQL("CREATE TABLE protected_link (id TEXT NOT NULL PRIMARY KEY, mediaId TEXT NOT NULL REFERENCES media_assets(id) ON DELETE RESTRICT)")
            db.openHelper.writableDatabase.execSQL("INSERT INTO protected_link VALUES ('link','audio')")
            val library = MediaLibraryRepository(db, dir)
            try { library.delete("audio"); fail("Deleted protected item") } catch (_: IllegalStateException) { }
            assertEquals(1, db.markers().read("audio").size) // marker deletion rolled back, too
            db.openHelper.writableDatabase.execSQL("DELETE FROM protected_link")
            library.delete("audio")
            assertTrue(db.markers().read("audio").isEmpty())
            db.openHelper.readableDatabase.query("PRAGMA foreign_key_check").use { assertEquals(0, it.count) }
        } finally { db.close(); context.deleteDatabase(name); dir.deleteRecursively() }
    }
}
