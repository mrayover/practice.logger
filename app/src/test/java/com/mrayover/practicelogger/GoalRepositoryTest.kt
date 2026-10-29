package com.mrayover.practicelogger

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class) @Config(sdk = [35])
class GoalRepositoryTest {
    private val context get() = ApplicationProvider.getApplicationContext<android.content.Context>()
    private fun memory() = Room.inMemoryDatabaseBuilder(context, BlockDatabase::class.java).build()
    private suspend fun block(db: BlockDatabase, name: String = "Shared") = BlockRepository(db).save(null, BlockDraft(name, "Play", "2", "90", "Reference"))
    private suspend fun goal(db: BlockDatabase, title: String = "Learn") = GoalRepository(db) { 100 }.save(null, title, "Outcome", 23, "Notes")
    private suspend fun rejects(action: suspend () -> Unit) { try { action(); fail("Invalid operation accepted") } catch (_: IllegalArgumentException) { } }

    @Test fun lifecycleLimitOrderingAndHomeTopTwoPreserveIdentity(): Unit = runBlocking {
        val db = memory()
        try {
            val repo = GoalRepository(db) { 200 }; val ids = (1..5).map { goal(db, "Goal $it") }
            rejects { goal(db, "Six") }; rejects { repo.save(ids[0], " ", "", 1, "") }
            rejects { repo.save(ids[0], "Title", "", 101, "") }
            val old = db.goals().find(ids[0])!!.goal
            repo.save(ids[0], "Edited", "Clear outcome", 78, "Updated notes")
            assertEquals(old.id, db.goals().find(ids[0])!!.goal.id)
            assertEquals(old.createdAt, db.goals().find(ids[0])!!.goal.createdAt)
            repo.move(ids[2], -1); repo.move(ids[2], -1)
            assertEquals(listOf(ids[2], ids[0]), homeGoals(db.goals().readAll()).map { it.goal.id })
            repo.setCompleted(ids[0], true)
            assertEquals(78, db.goals().find(ids[0])!!.goal.progress)
            val sixth = goal(db, "Replacement")
            rejects { repo.setCompleted(ids[0], false) }
            assertTrue(db.goals().find(ids[0])!!.goal.completed)
            repo.setCompleted(sixth, true); repo.setCompleted(ids[0], false)
            assertEquals(ids[0], activeGoals(db.goals().readAll()).last().goal.id)
            assertEquals((0..4).toList(), activeGoals(db.goals().readAll()).map { it.goal.priority })
            assertEquals(6, db.goals().readAll().size)
        } finally { db.close() }
    }

    @Test fun sharedArchivedBlocksAndActualTimeDoNotDoubleCountOrMutateDrafts(): Unit = runBlocking {
        val db = memory()
        try {
            val b = block(db); val archived = block(db, "Archived"); val a = goal(db); val other = goal(db, "Other")
            val repo = GoalRepository(db); val sessions = SessionRepository(db)
            repo.setBlocks(a, listOf(b, archived)); repo.setBlocks(other, listOf(b))
            BlockRepository(db).setArchived(archived, true)
            sessions.addBlock(b); val normal = sessions.start(); sessions.checkpoint(normal, 800)
            sessions.addBlock(b); val draft = db.sessions().find(db.sessions().withStatus("DRAFT")!!.id)!!
            val first = sessions.startGoal(a); sessions.checkpoint(first, 1234)
            sessions.saveBlockNote(first, db.sessions().find(first)!!.current!!.id, "Keep this")
            val prior = db.sessions().find(first)!!
            BlockRepository(db).save(b, BlockDraft("Current", "New action", "3", "120", "New reference"))
            val second = sessions.startGoal(a); val shared = sessions.startGoal(other)
            sessions.checkpoint(second, 2000); sessions.checkpoint(shared, 3000)
            assertEquals(draft, db.sessions().find(draft.session.id)); assertEquals(prior, db.sessions().find(first))
            assertEquals(1, db.sessions().find(second)!!.placements.size)
            assertEquals(180_000L, db.sessions().find(second)!!.current!!.configuredMs)
            assertEquals("Current", db.sessions().find(second)!!.current!!.name)
            sessions.finishBlock(first, "SKIPPED", "Keep this")
            val totals = db.goals().observeTime().first().associate { it.goalId to it.activeMs }
            assertEquals(3234L, totals[a]); assertEquals(3000L, totals[other])
            assertEquals(23, db.goals().find(a)!!.goal.progress)
            val blocks = db.blocks().readAll(); repo.setCompleted(a, true)
            assertEquals(blocks, db.blocks().readAll()); assertEquals(2, db.goals().find(a)!!.blocks.size)
            rejects { sessions.startGoal(a) }
            repo.setCompleted(a, false); repo.setBlocks(a, listOf(archived)); rejects { sessions.startGoal(a) }
            rejects { repo.setBlocks(a, listOf(b, b)) }; rejects { repo.setBlocks(a, listOf("missing")) }
            assertEquals(listOf(archived), db.goals().find(a)!!.ordered.map { it.blockId })
            // Database primary key prevents attributing any session to two Goals.
            try { db.goals().insert(GoalSession(second, other)); fail("Double attribution") } catch (_: android.database.sqlite.SQLiteConstraintException) { }
        } finally { db.close() }
    }

    @Test fun backupRoundTripReopenIncludesHistoryAndLegacyOneThroughFour(): Unit = runBlocking {
        val db = memory(); val name = "goal-roundtrip.db"; context.deleteDatabase(name)
        try {
            val b = block(db); val g = goal(db); GoalRepository(db).setBlocks(g, listOf(b))
            val sessions = SessionRepository(db); val id = sessions.startGoal(g)
            sessions.checkpoint(id, 4321); sessions.finishBlock(id, "COMPLETED", "Historical note")
            sessions.startGoal(g)
            val data = BackupRepository(db).snapshot(); val bytes = BackupCodec.encode(data)
            assertEquals(6, JSONObject(String(bytes)).getInt("version"))
            var target = BlockDatabase.open(context, name)
            BackupRepository(target).restore(BackupCodec.decode(bytes)); target.close()
            target = BlockDatabase.open(context, name)
            assertEquals(data, BackupRepository(target).snapshot())
            assertEquals(4321L, target.goals().observeTime().first().single().activeMs)
            assertEquals("Historical note", target.sessions().find(id)!!.current!!.note)
            assertEquals(23, target.goals().find(g)!!.goal.progress); target.close()
            for (v in 1..4) {
                val root = JSONObject(String(BackupCodec.encode(BackupData(data.blocks, emptyList(), emptyList())))).put("version", v)
                root.remove("goals"); root.remove("goalBlocks"); root.remove("sessionGoals")
                val legacy = BackupCodec.decode(root.toString().toByteArray())
                assertTrue(legacy.goals.isEmpty()); assertTrue(legacy.sessionGoals.isEmpty()); assertEquals(data.blocks, legacy.blocks)
            }
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun invalidGoalBackupsAndNonemptyGoalsOnlyTargetAreProtected(): Unit = runBlocking {
        val db = memory(); val target = memory()
        try {
            val b = block(db); val g = goal(db); GoalRepository(db).setBlocks(g, listOf(b))
            SessionRepository(db).startGoal(g)
            val data = BackupRepository(db).snapshot(); val goal = data.goals.single()
            val bad = listOf(data.copy(goals = listOf(goal.copy(progress = -1))),
                data.copy(goals = listOf(goal.copy(priority = 2))), data.copy(goals = listOf(goal.copy(id = ""))),
                data.copy(goals = listOf(goal.copy(title = " "))), data.copy(goals = listOf(goal.copy(updatedAt = 0))),
                data.copy(goalBlocks = data.goalBlocks + data.goalBlocks), data.copy(goalBlocks = listOf(GoalBlock(g, "missing", 0))),
                data.copy(goalBlocks = listOf(GoalBlock(g, b, 2))), data.copy(sessionGoals = data.sessionGoals + data.sessionGoals),
                data.copy(sessionGoals = listOf(GoalSession("missing", g))),
                data.copy(goals = (0..5).map { goal.copy(id = "g$it", priority = it) }))
            bad.forEach { rejects { BackupRepository(target).restore(it) }; assertTrue(target.goals().readAll().isEmpty()) }
            goal(target, "Existing")
            val before = BackupRepository(target).snapshot()
            try { BackupRepository(target).restore(data); fail("Nonempty restore") } catch (_: NonEmptyRestoreException) { }
            assertEquals(before, BackupRepository(target).snapshot())
        } finally { db.close(); target.close() }
    }

    @Test fun lateGoalLinkFailureRollsBackAllTables(): Unit = runBlocking {
        val source = memory(); val target = memory()
        try {
            val b = block(source); val g = goal(source); GoalRepository(source).setBlocks(g, listOf(b))
            SessionRepository(source).startGoal(g)
            val data = BackupRepository(source).snapshot()
            target.openHelper.writableDatabase.execSQL("CREATE TRIGGER reject_goal_link BEFORE INSERT ON session_goals BEGIN SELECT RAISE(ABORT, 'test failure'); END")
            try { BackupRepository(target).restore(data); fail("Partial Goal restore") } catch (_: android.database.sqlite.SQLiteException) { }
            assertEquals(BackupData(emptyList(), emptyList(), emptyList()), BackupRepository(target).snapshot())
        } finally { source.close(); target.close() }
    }

    @Test fun migrationSevenPreservesEveryExistingTableAndPermitsGoals(): Unit = runBlocking {
        val name = "goals-migration.db"; context.deleteDatabase(name)
        val schema = javaClass.classLoader!!.getResourceAsStream("com.mrayover.practicelogger.BlockDatabase/7.json")!!.bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
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
        sql.execSQL("INSERT INTO sessions VALUES ('d',2,NULL,'DRAFT',0,'')")
        sql.execSQL("INSERT INTO placements VALUES ('p','s','b','Legacy','Play',90,'Reference',0,300000,1234,'PENDING',0,'Quick note',126)")
        sql.execSQL("INSERT INTO routines VALUES ('r','Legacy routine',1,2)")
        sql.execSQL("INSERT INTO routine_placements VALUES ('rp','r','b',0,60000)")
        sql.execSQL("INSERT INTO takes VALUES ('t','b',3,'FREE',1234,'00000000-0000-0000-0000-000000000001.m4a','Original',NULL,NULL)")
        val setup = schema.getJSONArray("setupQueries"); repeat(setup.length()) { sql.execSQL(setup.getString(it)) }
        sql.version = 7; sql.close()
        val db = BlockDatabase.open(context, name)
        try {
            assertTrue(db.goals().readAll().isEmpty()); assertTrue(db.goals().readSessions().isEmpty())
            assertEquals(1234L, db.sessions().find("s")!!.activeMs); assertEquals("Quick note", db.sessions().find("s")!!.current!!.note)
            assertEquals(126, db.sessions().find("s")!!.current!!.lastUsedBpm); assertEquals("Session note", db.sessions().find("s")!!.session.note)
            assertEquals("DRAFT", db.sessions().find("d")!!.session.status); assertEquals("Original", db.takes().find("t")!!.label)
            assertEquals(60000L, db.routines().find("r")!!.ordered.single().configuredMs)
            val id = goal(db); GoalRepository(db).setBlocks(id, listOf("b"))
            assertTrue(db.blocks().find("b")!!.archived)
            db.openHelper.writableDatabase.query("PRAGMA foreign_key_check").use { assertEquals(0, it.count) }
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
