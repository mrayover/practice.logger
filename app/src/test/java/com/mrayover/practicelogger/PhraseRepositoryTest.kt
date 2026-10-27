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
class PhraseRepositoryTest {
    private val context get() = ApplicationProvider.getApplicationContext<android.content.Context>()
    @Test fun loopsAndFreeTakesStayIndependentAndOnlyExactCompleteWavCommits(): Unit = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, BlockDatabase::class.java).build()
        val directory = File(context.cacheDir, UUID.randomUUID().toString())
        try {
            val block = BlockRepository(db).save(null, BlockDraft("Phrase", "Play"))
            val repo = TakeRepository(db, directory) { 1000 }
            repo.recover()
            val free = repo.prepare(block).also { it.file.writeText("free audio") }.let { repo.save(it) }
            val spec = PhraseSpec(120, 8)
            val samples = ShortArray(spec.frames) { (it % 32000).toShort() }
            val draft = repo.prepare(block, spec); PhraseWave.write(draft.file, samples)
            val loop = repo.save(draft)
            assertEquals("LOOP", loop.mode); assertEquals(120, loop.recordedBpm); assertEquals(8, loop.beatCount)
            assertEquals(4000L, loop.durationMs); assertTrue(loop.fileName.endsWith(".wav"))
            assertArrayEquals(samples, PhraseWave.read(repo.audioFile(loop)))
            val partial = repo.prepare(block, spec); PhraseWave.write(partial.file, samples.copyOf(samples.size - 1))
            try { repo.save(partial); fail("Partial phrase saved") } catch (_: IllegalArgumentException) { repo.abandon(partial) }
            repo.rename(loop.id, "Loop one"); BlockRepository(db).setArchived(block, true)
            assertEquals(2, db.takes().readAll().size)
            val exported = java.io.ByteArrayOutputStream(); repo.export(loop, exported)
            assertArrayEquals(repo.audioFile(loop).readBytes(), exported.toByteArray())
            val json = String(BackupCodec.encode(BackupRepository(db).snapshot()))
            assertFalse(json.contains(loop.id)); assertFalse(json.contains("Loop one"))
            repo.delete(loop.id); assertEquals(free, db.takes().find(free.id)); assertTrue(repo.audioFile(free).exists())
            val active = repo.prepare(block, spec); assertEquals(0, repo.recover()); assertTrue(active.file.exists()); repo.abandon(active)
            val orphan = File(directory, UUID.randomUUID().toString() + ".pending.wav").also { it.writeText("interrupted") }
            assertEquals(1, repo.recover()); assertFalse(orphan.exists())
        } finally { db.close(); directory.deleteRecursively() }
    }
    @Test fun schemaFourUpgradeRetainsFreeTakeAndPracticeWithNullPhraseFields(): Unit = runBlocking {
        val name = "phrase-migration.db"; context.deleteDatabase(name)
        val schema = javaClass.classLoader!!.getResourceAsStream("com.mrayover.practicelogger.BlockDatabase/4.json")!!.bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
        val file = context.getDatabasePath(name); file.parentFile!!.mkdirs()
        val sql = android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(file, null)
        val entities = schema.getJSONArray("entities")
        repeat(entities.length()) { index ->
            val e = entities.getJSONObject(index); val table = e.getString("tableName")
            sql.execSQL(e.getString("createSql").replace("\${TABLE_NAME}", table))
            val indices = e.optJSONArray("indices") ?: org.json.JSONArray()
            repeat(indices.length()) { sql.execSQL(indices.getJSONObject(it).getString("createSql").replace("\${TABLE_NAME}", table)) }
        }
        sql.execSQL("INSERT INTO blocks VALUES ('b','Legacy','Play',5,90,'Notes',1,1,2)")
        sql.execSQL("INSERT INTO sessions VALUES ('s',1,NULL,'IN_PROGRESS',0,'Session note')")
        sql.execSQL("INSERT INTO placements VALUES ('p','s','b','Legacy','Play',90,'Reference',0,300000,1234,'PENDING',0,'Quick note',126)")
        sql.execSQL("INSERT INTO takes VALUES ('t','b',3,'FREE',1234,'00000000-0000-0000-0000-000000000001.m4a','Saved free take')")
        val setup = schema.getJSONArray("setupQueries"); repeat(setup.length()) { sql.execSQL(setup.getString(it)) }
        sql.version = 4; sql.close()
        val db = BlockDatabase.open(context, name)
        try {
            val take = db.takes().find("t")!!
            assertEquals("Saved free take", take.label); assertEquals("FREE", take.mode)
            assertNull(take.recordedBpm); assertNull(take.beatCount)
            val session = db.sessions().find("s")!!
            assertEquals(1234L, session.activeMs); assertEquals(126, session.current!!.lastUsedBpm)
            assertEquals("Quick note", session.current!!.note); assertTrue(db.blocks().find("b")!!.archived)
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
