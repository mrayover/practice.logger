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
class RoutineRepositoryTest {
    private val context get() = ApplicationProvider.getApplicationContext<android.content.Context>()
    private fun memory() = Room.inMemoryDatabaseBuilder(context, BlockDatabase::class.java).build()
    private suspend fun block(db: BlockDatabase, name: String) = BlockRepository(db).save(null, BlockDraft(name, "Play $name", "2", "90", "Reference $name"))

    @Test fun createEditMultipleRoutinesPersistOrderDurationsAndStableBlockReferences(): Unit = runBlocking {
        val name = "saved-routines.db"; context.deleteDatabase(name)
        var db = BlockDatabase.open(context, name)
        try {
            val a = block(db, "A"); val b = block(db, "B"); val c = block(db, "C")
            val blocks = db.blocks().readAll()
            var repo = RoutineRepository(db) { 100 }
            val first = repo.save(null, " First ", listOf(RoutineSelection(a, 60_000), RoutineSelection(b, 180_000), RoutineSelection(a, 90_000)))
            val second = repo.save(null, "Second", listOf(RoutineSelection(c, 120_000)))
            assertNotEquals(first, second)
            val old = db.routines().find(first)!!.routine
            repo.save(first, "Renamed", listOf(RoutineSelection(b, 240_000), RoutineSelection(c, 60_000), RoutineSelection(a, 90_000)))
            db.close(); db = BlockDatabase.open(context, name); repo = RoutineRepository(db)
            val reopened = db.routines().find(first)!!
            assertEquals("Renamed", reopened.routine.name); assertEquals(old.createdAt, reopened.routine.createdAt)
            assertEquals(listOf(b, c, a), reopened.ordered.map { it.sourceBlockId })
            assertEquals(listOf(240_000L, 60_000L, 90_000L), reopened.ordered.map { it.configuredMs })
            assertEquals(2, db.routines().readAll().size); assertEquals(blocks, db.blocks().readAll())
            repo.delete(second); assertNotNull(db.routines().find(first)); assertNull(db.routines().find(second))
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun saveBuilderSelectionLeavesDraftAndOriginalBlocksUnchanged(): Unit = runBlocking {
        val db = memory()
        try {
            val a = block(db, "A"); val b = block(db, "B")
            val sessions = SessionRepository(db); sessions.addBlock(a); sessions.addBlock(b); sessions.addBlock(a)
            val draftId = db.sessions().withStatus("DRAFT")!!.id
            sessions.setDuration(db.sessions().find(draftId)!!.ordered[1].id, 77_000)
            sessions.move(db.sessions().find(draftId)!!.ordered[2].id, -1)
            val before = BackupRepository(db).snapshot()
            val id = RoutineRepository(db).saveDraft("Builder selection")
            val routine = db.routines().find(id)!!
            assertEquals(before.placements.sortedBy { it.position }.map { it.sourceBlockId }, routine.ordered.map { it.sourceBlockId })
            assertEquals(before.placements.sortedBy { it.position }.map { it.configuredMs }, routine.ordered.map { it.configuredMs })
            assertEquals(before.blocks, db.blocks().readAll())
            assertEquals(before.sessions, db.sessions().readAll().map { it.session })
            assertEquals(before.placements, db.sessions().find(draftId)!!.ordered)
            assertEquals("DRAFT", db.sessions().find(draftId)!!.session.status)
        } finally { db.close() }
    }

    @Test fun repeatedStartsUseCurrentBlocksAndIndependentPlacementsWithoutConsumingDraft(): Unit = runBlocking {
        val db = memory()
        try {
            val a = block(db, "Original"); val b = block(db, "Draft Block")
            val routines = RoutineRepository(db)
            val id = routines.save(null, "Warmup", listOf(RoutineSelection(a, 180_000), RoutineSelection(a, 60_000)))
            val template = db.routines().find(id)!!
            val sessions = SessionRepository(db); sessions.addBlock(b)
            val draft = db.sessions().find(db.sessions().withStatus("DRAFT")!!.id)!!
            val first = sessions.startRoutine(id)
            BlockRepository(db).save(a, BlockDraft("Updated", "Current action", "9", "135", "Current notes"))
            val second = sessions.startRoutine(id)
            assertNotEquals(first, second)
            val one = db.sessions().find(first)!!; val two = db.sessions().find(second)!!
            assertEquals(listOf(180_000L, 60_000L), two.ordered.map { it.configuredMs })
            assertEquals("Original", one.current!!.name); assertEquals("Updated", two.current!!.name)
            assertEquals("Current action", two.current!!.action); assertEquals(135, two.current!!.tempo)
            assertEquals("Current notes", two.current!!.referenceNotes)
            assertTrue(one.placements.map { it.id }.toSet().intersect(two.placements.map { it.id }.toSet()).isEmpty())
            assertTrue(two.placements.all { it.activeMs == 0L && it.note.isEmpty() && it.lastUsedBpm == null && it.outcome == "PENDING" })
            sessions.checkpoint(first, 1234); sessions.saveUsedBpm(first, one.current!!.id, 126)
            sessions.saveBlockNote(first, one.current!!.id, "Session only")
            sessions.finishBlock(first, "COMPLETED", "Session only"); sessions.finishBlock(first, "SKIPPED", "")
            assertEquals(template, db.routines().find(id)); assertEquals(draft, db.sessions().find(draft.session.id))
            assertEquals(0L, db.sessions().find(second)!!.activeMs)
            val history = db.sessions().find(first)!!
            routines.save(id, "Changed later", listOf(RoutineSelection(b, 9999)))
            assertEquals(history, db.sessions().find(first)); assertEquals(two, db.sessions().find(second))
            routines.delete(id)
            assertEquals(history, db.sessions().find(first)); assertEquals(2, db.blocks().readAll().size)
        } finally { db.close() }
    }

    @Test fun archivedUnavailableAndDeletedReferencesAreExplicitAndNeverPartiallyStart(): Unit = runBlocking {
        val db = memory()
        try {
            val a = block(db, "Active"); val b = block(db, "Archived")
            val repo = RoutineRepository(db)
            val id = repo.save(null, "Both", listOf(RoutineSelection(a, 60_000), RoutineSelection(b, 60_000)))
            val before = db.routines().find(id)
            try { db.openHelper.writableDatabase.execSQL("DELETE FROM blocks WHERE id = ?", arrayOf(b)); fail("Referenced Block deleted") }
            catch (_: android.database.sqlite.SQLiteConstraintException) { }
            assertNotNull(db.blocks().find(b))
            BlockRepository(db).setArchived(b, true)
            try { SessionRepository(db).startRoutine(id); fail("Archived Block silently omitted") }
            catch (e: RoutineUnavailableException) { assertTrue(e.message!!.contains("Archived")) }
            assertTrue(db.sessions().readAll().isEmpty()); assertEquals(before, db.routines().find(id))
            val unavailable = routineUnavailable(listOf(RoutineSelection("gone", 1)), emptyMap())
            assertTrue(unavailable.single().contains("gone"))
            try { repo.save(id, "Bad", listOf(RoutineSelection("gone", 1))); fail("Missing Block saved") }
            catch (_: RoutineUnavailableException) { }
            assertEquals(before, db.routines().find(id))
            BlockRepository(db).setArchived(b, false)
            assertEquals(2, db.sessions().find(SessionRepository(db).startRoutine(id))!!.placements.size)
            repo.delete(id)
            try { SessionRepository(db).startRoutine(id); fail("Deleted Routine started") } catch (_: RoutineUnavailableException) { }
            assertEquals(1, db.sessions().readAll().size)
        } finally { db.close() }
    }

    @Test fun invalidEditorWritesLeaveExistingRoutineAndOtherDataIntact(): Unit = runBlocking {
        val db = memory()
        try {
            val a = block(db, "A"); val repo = RoutineRepository(db)
            val id = repo.save(null, "Keep", listOf(RoutineSelection(a, 1)))
            val before = BackupRepository(db).snapshot()
            for ((name, rows) in listOf(" " to listOf(RoutineSelection(a, 1)), "x".repeat(121) to listOf(RoutineSelection(a, 1)), "Bad" to emptyList(), "Bad" to listOf(RoutineSelection(a, 0)), "Bad" to listOf(RoutineSelection(a, -1)))) {
                try { repo.save(id, name, rows); fail("Invalid Routine accepted") } catch (_: IllegalArgumentException) { }
                assertEquals(before, BackupRepository(db).snapshot())
            }
            try { repo.saveDraft("Empty"); fail("Empty builder saved") } catch (_: IllegalArgumentException) { }
            assertEquals(before, BackupRepository(db).snapshot())
        } finally { db.close() }
    }

    @Test fun migrationSixRetainsBlocksPracticeAndAllAudioOwnershipAndFiles(): Unit = runBlocking {
        val name = "routine-migration.db"; context.deleteDatabase(name)
        val directory = File(context.cacheDir, UUID.randomUUID().toString()).apply { mkdirs() }
        val schema = javaClass.classLoader!!.getResourceAsStream("com.mrayover.practicelogger.BlockDatabase/6.json")!!.bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
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
        val f1 = "00000000-0000-0000-0000-000000000001.m4a"; val f2 = "00000000-0000-0000-0000-000000000002.wav"; val f3 = "00000000-0000-0000-0000-000000000003.m4a"
        sql.execSQL("INSERT INTO takes VALUES ('t','b',3,'FREE',1234,'$f1','Original',NULL,NULL)")
        sql.execSQL("INSERT INTO takes VALUES ('l','b',4,'LOOP',4000,'$f2','Loop',120,8)")
        sql.execSQL("INSERT INTO takes VALUES ('f',NULL,5,'FREE',1234,'$f3','Standalone',NULL,NULL)")
        listOf(f1, f2, f3).forEach { File(directory, it).writeText("retain $it") }
        val setup = schema.getJSONArray("setupQueries"); repeat(setup.length()) { sql.execSQL(setup.getString(it)) }
        sql.version = 6; sql.close()
        val db = BlockDatabase.open(context, name)
        try {
            assertTrue(db.routines().readAll().isEmpty()); assertTrue(db.blocks().find("b")!!.archived)
            val session = db.sessions().find("s")!!
            assertEquals(1234L, session.activeMs); assertEquals("Quick note", session.current!!.note)
            assertEquals(126, session.current!!.lastUsedBpm); assertEquals("Session note", session.session.note)
            val takes = db.takes().readAll(); assertEquals(3, takes.size)
            assertEquals(BlockTake("t", "b", 3, "FREE", 1234, f1, "Original"), db.takes().find("t"))
            assertEquals(BlockTake("l", "b", 4, "LOOP", 4000, f2, "Loop", 120, 8), db.takes().find("l"))
            assertNull(db.takes().find("f")!!.blockId)
            val media = TakeRepository(db, directory) { 1234 }; assertEquals(0, media.recover())
            takes.forEach { assertEquals("retain ${it.fileName}", media.audioFile(it).readText()) }
            RoutineRepository(db).save(null, "Migrated", listOf(RoutineSelection("b", 60_000)))
            assertEquals(takes, db.takes().readAll())
            db.openHelper.writableDatabase.query("PRAGMA foreign_key_check").use { assertEquals(0, it.count) }
        } finally { db.close(); context.deleteDatabase(name); directory.deleteRecursively() }
    }
}
