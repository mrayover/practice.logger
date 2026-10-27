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

class BeatPcmTest {
    @Test fun sampleClockProducesSteadyBeatsAndRespondsToTempoChanges() {
        val pcm = BeatPcm()
        val chunk = ShortArray(480)
        var clicks = 0; var silent = true
        repeat(400) {
            pcm.render(chunk, 120)
            for (sample in chunk) {
                if (sample.toInt() != 0 && silent) { clicks++; silent = false }
                // Only consider a full silent chunk a beat boundary (sine zero crossings aren't boundaries).
            }
            if (chunk.all { it.toInt() == 0 }) silent = true
        }
        assertEquals(8, clicks) // Four seconds at 120 BPM.
        var fast = 0; silent = true
        repeat(400) {
            pcm.render(chunk, 300)
            if (chunk.any { it.toInt() != 0 } && silent) { fast++; silent = false }
            if (chunk.all { it.toInt() == 0 }) silent = true
        }
        assertEquals(20, fast)
    }
    @Test fun inputRangeAndSavedReferenceFallback() {
        assertNull(parseWorkingBpm("29")); assertNull(parseWorkingBpm("301")); assertNull(parseWorkingBpm("120.5"))
        assertNull(parseWorkingBpm("2147483648")); assertEquals(30, parseWorkingBpm(" 30 ")); assertEquals(300, parseWorkingBpm("300"))
        val p = Placement("p", "s", "b", "Block", "Play", 90, "", 0, 1000)
        assertEquals(90, initialWorkingBpm(p)); assertEquals(120, initialWorkingBpm(p.copy(tempo = 999)))
        assertEquals(125, initialWorkingBpm(p.copy(lastUsedBpm = 125)))
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MetronomePersistenceTest {
    private val context get() = ApplicationProvider.getApplicationContext<android.content.Context>()
    @Test fun bpmHistoryBackupAndLegacyReadersPreserveTimerAndBlockDefault(): Unit = runBlocking {
        val name = "metronome-history.db"; context.deleteDatabase(name)
        var db = BlockDatabase.open(context, name)
        try {
            val block = BlockRepository(db).save(null, BlockDraft("Click", "Play", "5", "90"))
            var repo = SessionRepository(db); repo.addBlock(block); repo.addBlock(block)
            val session = repo.start(); val placement = db.sessions().find(session)!!.current!!
            repo.checkpoint(session, 1234)
            repo.saveUsedBpm(session, placement.id, 126)
            assertEquals(1234L, db.sessions().find(session)!!.current!!.activeMs)
            assertEquals(90, db.blocks().find(block)!!.tempo)
            try { repo.saveUsedBpm(session, placement.id, 301); fail("Invalid BPM") } catch (_: IllegalArgumentException) { }
            repo.finishBlock(session, "COMPLETED", "Keep note")
            try { repo.saveUsedBpm(session, placement.id, 130); fail("Changed prior placement") } catch (_: IllegalStateException) { }
            db.close(); db = BlockDatabase.open(context, name); repo = SessionRepository(db)
            assertEquals(126, db.sessions().find(session)!!.ordered[0].lastUsedBpm)
            repo.finishBlock(session, "SKIPPED", "")
            val snapshot = BackupRepository(db).snapshot()
            val bytes = BackupCodec.encode(snapshot)
            assertEquals(snapshot, BackupCodec.decode(bytes))
            for (version in 1..2) {
                val legacy = JSONObject(String(bytes)).put("version", version)
                val rows = legacy.getJSONArray("placements")
                repeat(rows.length()) { rows.getJSONObject(it).remove("lastUsedBpm") }
                assertTrue(BackupCodec.decode(legacy.toString().toByteArray()).placements.all { it.lastUsedBpm == null })
            }
            val target = Room.inMemoryDatabaseBuilder(context, BlockDatabase::class.java).build()
            try { BackupRepository(target).restore(BackupCodec.decode(bytes)); assertEquals(snapshot, BackupRepository(target).snapshot()) }
            finally { target.close() }
        } finally { db.close(); context.deleteDatabase(name) }
    }
    @Test fun realSchemaTwoUpgradeRetainsAllRecords(): Unit = runBlocking {
        val name = "metronome-migration.db"; context.deleteDatabase(name)
        val schema = javaClass.classLoader!!.getResourceAsStream("com.mrayover.practicelogger.BlockDatabase/2.json")!!.bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
        val file = context.getDatabasePath(name); file.parentFile!!.mkdirs()
        val sql = android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(file, null)
        val entities = schema.getJSONArray("entities")
        repeat(entities.length()) { i ->
            val entity = entities.getJSONObject(i); val table = entity.getString("tableName")
            sql.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
            val indices = entity.getJSONArray("indices")
            repeat(indices.length()) { sql.execSQL(indices.getJSONObject(it).getString("createSql").replace("\${TABLE_NAME}", table)) }
        }
        sql.execSQL("INSERT INTO blocks VALUES ('b','Legacy','Play',5,90,'Reference',0,1,2)")
        sql.execSQL("INSERT INTO sessions VALUES ('s',1,NULL,'IN_PROGRESS',0,'Session note')")
        sql.execSQL("INSERT INTO placements VALUES ('p','s','b','Legacy','Play',90,'Reference',0,300000,1234,'PENDING',0,'Quick note')")
        val setup = schema.getJSONArray("setupQueries")
        repeat(setup.length()) { sql.execSQL(setup.getString(it)) }
        sql.version = 2; sql.close()
        val db = BlockDatabase.open(context, name)
        try {
            val before = db.sessions().find("s")!!
            assertEquals(1234L, before.current!!.activeMs); assertEquals("Quick note", before.current!!.note)
            assertEquals("Session note", before.session.note); assertNull(before.current!!.lastUsedBpm)
            SessionRepository(db).saveUsedBpm("s", "p", 121)
            assertEquals(before.current!!.copy(lastUsedBpm = 121), db.sessions().find("s")!!.current)
            assertEquals(90, db.blocks().find("b")!!.tempo)
        } finally { db.close(); context.deleteDatabase(name) }
    }
}

