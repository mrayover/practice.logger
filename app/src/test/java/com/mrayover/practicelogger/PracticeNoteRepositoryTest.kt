package com.mrayover.practicelogger

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class) @Config(sdk = [35])
class PracticeNoteRepositoryTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private fun memory() = Room.inMemoryDatabaseBuilder(context, BlockDatabase::class.java).build()
    private suspend fun fixture(db: BlockDatabase): Pair<String, SessionBundle> {
        val b = BlockRepository(db).save(null, BlockDraft("Notes", "Permanent description", "5", "120", "Permanent reference"))
        val repo = SessionRepository(db); repo.addBlock(b); val id = repo.start()
        val bundle = db.sessions().find(id)!!
        repo.saveBlockNote(id, bundle.current!!.id, "Legacy placement note")
        return b to db.sessions().find(id)!!
    }
    @Test fun migrateTenPreservesEveryOldTableAndNotesSurviveReopen(): Unit = runBlocking {
        val name = "notes-migration.db"; context.deleteDatabase(name)
        val schema = javaClass.classLoader!!.getResourceAsStream("com.mrayover.practicelogger.BlockDatabase/10.json")!!.bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
        val file = context.getDatabasePath(name); file.parentFile!!.mkdirs()
        val sql = android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(file, null)
        val entities = schema.getJSONArray("entities")
        repeat(entities.length()) { i ->
            val e = entities.getJSONObject(i); val table = e.getString("tableName")
            sql.execSQL(e.getString("createSql").replace("\${TABLE_NAME}", table))
            val indices = e.optJSONArray("indices") ?: org.json.JSONArray()
            repeat(indices.length()) { sql.execSQL(indices.getJSONObject(it).getString("createSql").replace("\${TABLE_NAME}", table)) }
        }
        sql.execSQL("INSERT INTO blocks VALUES ('b','Legacy','Instructions',5,90,'Reference',0,1,2)")
        sql.execSQL("INSERT INTO sessions VALUES ('s',1,NULL,'IN_PROGRESS',0,'Overall session note')")
        sql.execSQL("INSERT INTO placements VALUES ('p','s','b','Legacy','Instructions',90,'Reference',0,300000,1234,'PENDING',0,'Earlier practice note',126)")
        sql.execSQL("INSERT INTO routines VALUES ('r','Routine',1,2)")
        sql.execSQL("INSERT INTO routine_placements VALUES ('rp','r','b',0,60000)")
        sql.execSQL("INSERT INTO takes VALUES ('t','b',3,'FREE',1234,'original.m4a','Original',NULL,NULL)")
        sql.execSQL("INSERT INTO goals VALUES ('g','Goal','Outcome',20,'Goal note',0,0,1,2)")
        sql.execSQL("INSERT INTO goal_blocks VALUES ('g','b',0)")
        sql.execSQL("INSERT INTO session_goals VALUES ('s','g')")
        sql.execSQL("INSERT INTO daily_logs VALUES ('2026-10-30','Independent daily Log')")
        sql.execSQL("INSERT INTO block_images VALUES ('b','original.pdf','application/pdf',600,800,100)")
        val setup = schema.getJSONArray("setupQueries"); repeat(setup.length()) { sql.execSQL(setup.getString(it)) }
        val before = (0 until entities.length()).associate { i ->
            val table = entities.getJSONObject(i).getString("tableName")
            table to sql.rawQuery("SELECT * FROM $table", null).use { c -> buildList { while (c.moveToNext()) add((0 until c.columnCount).map { c.getString(it) }) } }
        }
        sql.version = 10; sql.close()
        var db = BlockDatabase.open(context, name)
        try {
            assertTrue(db.practiceNotes().readAll().isEmpty())
            before.forEach { (table, rows) -> db.openHelper.writableDatabase.query("SELECT * FROM $table").use { c ->
                assertEquals(table, rows, buildList { while (c.moveToNext()) add((0 until c.columnCount).map { c.getString(it) }) })
            } }
            assertEquals("Earlier practice note", db.practiceNotes().history().first().single().text)
            var tick = 1000L; val repo = PracticeNoteRepository(db) { ++tick }
            val a = repo.append("b", "s", "p", "First entry")
            val b = repo.append("b", "s", "p", "Second entry")
            assertNotEquals(a.id, b.id)
            db.close(); db = BlockDatabase.open(context, name)
            assertEquals(listOf(b, a), db.practiceNotes().readAll())
            val history = db.practiceNotes().history().first()
            assertEquals(listOf("Second entry", "First entry", "Earlier practice note"), history.map { it.text })
            assertEquals(1, history.count { it.legacy })
            val editRepo = PracticeNoteRepository(db)
            editRepo.update(history.first(), "Second entry edited")
            assertEquals(b.createdAt, db.practiceNotes().readAll().first().createdAt)
            editRepo.update(history.single { it.legacy }, "Earlier practice note edited")
            assertEquals("Earlier practice note edited", db.sessions().find("s")!!.current!!.note)
            db.close(); db = BlockDatabase.open(context, name)
            assertEquals("Second entry edited", db.practiceNotes().readAll().first().text)

            assertEquals("Overall session note", db.sessions().find("s")!!.session.note)
            assertEquals("Earlier practice note edited", db.sessions().find("s")!!.current!!.note)
            db.openHelper.writableDatabase.query("PRAGMA foreign_key_check").use { assertEquals(0, it.count) }
        } finally { db.close(); context.deleteDatabase(name) }
    }
    @Test fun independentEntriesValidateOwnershipAndKeepFailuresAtomic(): Unit = runBlocking {
        val db = memory()
        try {
            val (block, bundle) = fixture(db); val p = bundle.current!!.id
            val repo = PracticeNoteRepository(db)
            val a = repo.append(block, bundle.session.id, p, "Same text")
            val b = repo.append(block, bundle.session.id, p, "Same text")
            assertNotEquals(a.id, b.id); assertEquals(2, db.practiceNotes().readAll().size)
            for ((id, text) in listOf("wrong-block" to "No", block to "  ")) {
                var failed = false
                try { repo.append(id, bundle.session.id, p, text) } catch (_: Exception) { failed = true }
                assertTrue(failed)
            }
            db.openHelper.writableDatabase.execSQL("CREATE TRIGGER reject_note BEFORE INSERT ON practice_notes BEGIN SELECT RAISE(ABORT, 'test'); END")
            var failed = false
            try { repo.append(block, bundle.session.id, p, "Failed draft") } catch (_: Exception) { failed = true }
            assertTrue(failed); assertEquals(2, db.practiceNotes().readAll().size)
            assertEquals("Legacy placement note", db.sessions().find(bundle.session.id)!!.current!!.note)
        } finally { db.close() }
    }
    @Test fun backupSevenRoundTripLegacyImportsAndRejectedRestoreAreSafe(): Unit = runBlocking {
        val source = memory(); val target = memory(); val rejected = memory()
        try {
            val (block, bundle) = fixture(source)
            source.logs().save(DailyLog("2026-10-30", "Daily Log"))
            PracticeNoteRepository(source).append(block, bundle.session.id, bundle.current!!.id, "Independent new note")
            val data = BackupRepository(source).snapshot(); val bytes = BackupCodec.encode(data)
            assertEquals(7, JSONObject(String(bytes)).getInt("version"))
            BackupRepository(target).restore(BackupCodec.decode(bytes))
            assertEquals(data, BackupRepository(target).snapshot())
            assertEquals(2, target.practiceNotes().history().first().size)
            for (v in 1..6) {
                val root = JSONObject(String(bytes)).put("version", v); root.remove("practiceNotes")
                val old = BackupCodec.decode(root.toString().toByteArray())
                assertTrue(old.practiceNotes.isEmpty()); assertEquals("Legacy placement note", old.placements.single().note)
                assertEquals("Permanent reference", old.blocks.single().notes)
            }
            var failed = false
            try { BackupCodec.encode(data.copy(practiceNotes = listOf(data.practiceNotes.single().copy(blockId = "missing")))) } catch (_: Exception) { failed = true }
            assertTrue(failed)
            rejected.openHelper.writableDatabase.execSQL("CREATE TRIGGER reject_note BEFORE INSERT ON practice_notes BEGIN SELECT RAISE(ABORT, 'test'); END")
            failed = false
            try { BackupRepository(rejected).restore(data) } catch (_: Exception) { failed = true }
            assertTrue(failed); assertEquals(BackupData(emptyList(), emptyList(), emptyList()), BackupRepository(rejected).snapshot())
            failed = false
            try { BackupRepository(target).restore(data) } catch (_: NonEmptyRestoreException) { failed = true }
            assertTrue(failed); assertEquals(data, BackupRepository(target).snapshot())
        } finally { source.close(); target.close(); rejected.close() }
    }
}
