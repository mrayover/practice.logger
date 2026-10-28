package com.mrayover.practicelogger

import androidx.room.Room
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class StandaloneRecordingTest {
    private val context get() = ApplicationProvider.getApplicationContext<android.content.Context>()
    @Test fun standaloneTakesShareFileSafetyWithoutCreatingOrChangingBlocks(): Unit = runBlocking {
        val name = "standalone-files.db"; context.deleteDatabase(name)
        val directory = File(context.cacheDir, UUID.randomUUID().toString())
        var db = BlockDatabase.open(context, name)
        try {
            val block = BlockRepository(db).save(null, BlockDraft("Keep Block", "Play"))
            var repo = TakeRepository(db, directory) { 1234 }
            val linked = repo.prepare(block).also { it.file.writeText("block audio") }.let { repo.save(it) }
            val first = repo.prepare(null).also { it.file.writeText("free one") }.let { repo.save(it) }
            val second = repo.prepare(null).also { it.file.writeText("free two") }.let { repo.save(it) }
            repo.rename(first.id, "Standalone saved take")
            db.close(); db = BlockDatabase.open(context, name); repo = TakeRepository(db, directory) { 1234 }; repo.recover()
            assertEquals(1, db.blocks().readAll().size)
            assertEquals(2, db.takes().readAll().count { it.blockId == null })
            assertEquals("Standalone saved take", db.takes().find(first.id)!!.label)
            val output = java.io.ByteArrayOutputStream(); repo.export(first, output)
            assertArrayEquals("free one".toByteArray(), output.toByteArray())
            repo.delete(second.id)
            assertEquals(linked, db.takes().find(linked.id)); assertEquals("block audio", repo.audioFile(linked).readText())
            assertEquals("free one", repo.audioFile(first).readText())
            assertFalse(String(BackupCodec.encode(BackupRepository(db).snapshot())).contains(first.id))
        } finally { db.close(); context.deleteDatabase(name); directory.deleteRecursively() }
    }

    @Test fun schemaFiveUpgradePreservesBlockFreeAndLoopTakesAndPractice(): Unit = runBlocking {
        val name = "standalone-migration.db"; context.deleteDatabase(name)
        val schema = javaClass.classLoader!!.getResourceAsStream("com.mrayover.practicelogger.BlockDatabase/5.json")!!.bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
        val file = context.getDatabasePath(name); file.parentFile!!.mkdirs()
        val sql = android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(file, null)
        val entities = schema.getJSONArray("entities")
        repeat(entities.length()) { i ->
            val entity = entities.getJSONObject(i); val table = entity.getString("tableName")
            sql.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
            val indices = entity.optJSONArray("indices") ?: org.json.JSONArray()
            repeat(indices.length()) { sql.execSQL(indices.getJSONObject(it).getString("createSql").replace("\${TABLE_NAME}", table)) }
        }
        sql.execSQL("INSERT INTO blocks VALUES ('b','Legacy','Play',5,90,'Notes',1,1,2)")
        sql.execSQL("INSERT INTO sessions VALUES ('s',1,NULL,'IN_PROGRESS',0,'Session note')")
        sql.execSQL("INSERT INTO placements VALUES ('p','s','b','Legacy','Play',90,'Reference',0,300000,1234,'PENDING',0,'Quick note',126)")
        sql.execSQL("INSERT INTO takes VALUES ('t','b',3,'FREE',1234,'00000000-0000-0000-0000-000000000001.m4a','Original',NULL,NULL)")
        sql.execSQL("INSERT INTO takes VALUES ('l','b',4,'LOOP',4000,'00000000-0000-0000-0000-000000000002.wav','Loop',120,8)")
        val setup = schema.getJSONArray("setupQueries"); repeat(setup.length()) { sql.execSQL(setup.getString(it)) }
        sql.version = 5; sql.close()
        val db = BlockDatabase.open(context, name)
        try {
            assertEquals(BlockTake("t", "b", 3, "FREE", 1234, "00000000-0000-0000-0000-000000000001.m4a", "Original"), db.takes().find("t"))
            assertEquals(BlockTake("l", "b", 4, "LOOP", 4000, "00000000-0000-0000-0000-000000000002.wav", "Loop", 120, 8), db.takes().find("l"))
            assertEquals(1234L, db.sessions().find("s")!!.activeMs)
            assertEquals("Quick note", db.sessions().find("s")!!.current!!.note)
            assertTrue(db.blocks().find("b")!!.archived)
            db.takes().insert(BlockTake("solo", null, 5, "FREE", 1000, "00000000-0000-0000-0000-000000000003.m4a"))
            assertNull(db.takes().find("solo")!!.blockId)
            try {
                db.takes().insert(BlockTake("bad", "missing", 5, "FREE", 1000, "00000000-0000-0000-0000-000000000004.m4a"))
                fail("Missing Block foreign key accepted")
            } catch (_: android.database.sqlite.SQLiteConstraintException) { }
            db.openHelper.writableDatabase.query("PRAGMA foreign_key_check").use { assertEquals(0, it.count) }
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
