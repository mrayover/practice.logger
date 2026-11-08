package com.mrayover.practicelogger

import android.content.Context
import android.database.sqlite.SQLiteDatabase
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class GoalAttributionTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()

    @Test fun firstAccrualCapturesCurrentPrimaryAcrossAllEntryPoints() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, BlockDatabase::class.java).build()
        try {
            val blocks = BlockRepository(db)
            val goals = GoalRepository(db)
            val sessions = SessionRepository(db)
            val attribution = db.goalAttribution()
            val ids = ('A'..'E').map { goals.save(null, it.toString(), "", 0, "") }
            val (a, b, c, d, e) = ids
            val draft = BlockDraft("Study", "Play", notes = "Keep these notes")
            val block = blocks.save(null, draft.copy(primaryGoalId = a, primaryGoalChanged = true))
            val unassigned = blocks.save(null, draft.copy(name = "Unassigned"))
            val alsoA = blocks.save(null, draft.copy(name = "Also A", primaryGoalId = a, primaryGoalChanged = true))
            suspend fun live(id: String) = GoalIndicators(db.goals().readAll()).block(id)
            suspend fun snapshot(session: String, position: Int = 0): PlacementGoalSnapshot? =
                attribution.snapshot(db.sessions().find(session)!!.ordered[position].id)

            assertEquals(ids, GoalIndicators(db.goals().readAll()).active.map { it.goal.id })
            assertEquals(1, live(block)!!.priority)
            sessions.addBlock(block)
            sessions.addBlock(unassigned)
            val ordinary = sessions.start()
            sessions.checkpoint(ordinary, 0)
            assertNull(snapshot(ordinary))
            sessions.checkpoint(ordinary, 1000)
            val original = snapshot(ordinary)!!
            assertEquals(a, original.goalId)
            assertEquals(1, original.priority)

            goals.move(c, -1)
            goals.move(c, -1)
            assertEquals(listOf(c, a, b, d, e), GoalIndicators(db.goals().readAll()).active.map { it.goal.id })
            assertEquals(2, live(block)!!.priority)
            assertEquals(2, live(alsoA)!!.priority)
            goals.setCompleted(a, true)
            assertTrue(live(block)!!.goal.completed)
            assertNull(live(block)!!.priority)
            goals.setCompleted(a, false)
            assertEquals(5, live(block)!!.priority)

            blocks.save(block, draft.copy(primaryGoalId = c, primaryGoalChanged = true))
            goals.setBlocks(a, listOf(alsoA, block))
            assertEquals(c, live(block)!!.goal.id)
            assertEquals(a, live(alsoA)!!.goal.id)
            sessions.checkpoint(ordinary, 2000)
            assertEquals(original, snapshot(ordinary))
            sessions.finishBlock(ordinary, "SKIPPED", "Keep time", placementId = db.sessions().find(ordinary)!!.current!!.id)
            sessions.checkpoint(ordinary, 1000)
            val absent = snapshot(ordinary, 1)!!
            assertNull(absent.goalId)
            assertNull(absent.priority)
            blocks.save(unassigned, draft.copy(primaryGoalId = b, primaryGoalChanged = true))
            sessions.checkpoint(ordinary, 1000)
            assertEquals(absent, snapshot(ordinary, 1))

            val routine = RoutineRepository(db).save(null, "Routine", listOf(RoutineSelection(block, 60_000)))
            val routineSession = sessions.startRoutine(routine)
            assertNull(snapshot(routineSession))
            goals.setCompleted(c, true)
            sessions.checkpoint(routineSession, 3000)
            assertEquals(c, snapshot(routineSession)!!.goalId)
            assertNull(snapshot(routineSession)!!.priority)
            blocks.setArchived(block, true)
            assertEquals(c, live(block)!!.goal.id)
            blocks.setArchived(block, false)

            val goalSession = sessions.startGoal(a)
            sessions.finishBlock(goalSession, "SKIPPED", "", placementId = db.sessions().find(goalSession)!!.current!!.id)
            assertNull(snapshot(goalSession))
            sessions.checkpoint(goalSession, 4000)
            assertEquals(c, snapshot(goalSession, 1)!!.goalId)
            assertEquals(a, db.goals().readSessions().single().goalId)
            assertEquals(4000L, db.goals().observeTime().first().single().activeMs)
            assertEquals(12000L, db.transcribeActivities().observePracticeTime().first().sumOf { it.activeMs })

            goals.setBlocks(c, emptyList())
            assertNull(live(block))
            assertTrue(db.goals().find(a)!!.blocks.any { it.blockId == block })
            blocks.save(block, draft.copy(primaryGoalId = b, primaryGoalChanged = true))
            blocks.save(block, draft.copy(primaryGoalChanged = true))
            assertNull(live(block))
            assertTrue(db.goals().find(b)!!.blocks.any { it.blockId == block })
            assertEquals("Keep these notes", db.blocks().find(block)!!.notes)
            assertEquals(original, snapshot(ordinary))
        } finally {
            db.close()
        }
    }

    @Test fun migrationPreservesLegacyRowsAndAdoptsOnlySingleLinks() = runBlocking {
        val name = "g1-room18.db"
        context.deleteDatabase(name)
        val file = context.getDatabasePath(name)
        file.parentFile!!.mkdirs()
        val schema = javaClass.classLoader!!.getResourceAsStream("com.mrayover.practicelogger.BlockDatabase/18.json")!!
            .bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
        SQLiteDatabase.openOrCreateDatabase(file, null).use { sql ->
            val entities = schema.getJSONArray("entities")
            repeat(entities.length()) { i ->
                val entity = entities.getJSONObject(i)
                val table = entity.getString("tableName")
                sql.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                entity.optJSONArray("indices")?.let { indexes ->
                    repeat(indexes.length()) { sql.execSQL(indexes.getJSONObject(it).getString("createSql").replace("\${TABLE_NAME}", table)) }
                }
            }
            for (id in listOf("zero", "one", "many")) {
                sql.execSQL("INSERT INTO blocks VALUES (?,?,'Play',5,NULL,'Notes',1,1,2)", arrayOf(id, id))
            }
            sql.execSQL("INSERT INTO goals VALUES ('active','Active','',0,'',0,0,1,2), ('done','Done','',100,'',1,-1,1,2)")
            sql.execSQL("INSERT INTO goal_blocks VALUES ('done','one',0), ('done','many',1), ('active','many',0)")
            sql.execSQL("INSERT INTO sessions VALUES ('s',1,NULL,'IN_PROGRESS',0,'Session note')")
            sql.execSQL("INSERT INTO session_goals VALUES ('s','done')")
            sql.execSQL("INSERT INTO placements VALUES ('p','s','one','one','Play',NULL,'Notes',0,300000,1200,'PENDING',0,'Note',NULL)")
            schema.getJSONArray("setupQueries").let { setup -> repeat(setup.length()) { sql.execSQL(setup.getString(it)) } }
            sql.version = 18
        }
        val db = BlockDatabase.open(context, name)
        try {
            val attribution = db.goalAttribution()
            assertNull(attribution.primaryGoalId("zero"))
            assertEquals("done", attribution.primaryGoalId("one"))
            assertNull(attribution.primaryGoalId("many"))
            assertEquals(3, db.goals().readAll().sumOf { it.blocks.size })
            assertEquals(listOf(GoalSession("s", "done")), db.goals().readSessions())
            assertTrue(db.blocks().find("one")!!.archived)
            assertEquals(1500L, SessionRepository(db).checkpoint("s", 300).activeMs)
            assertNull(attribution.snapshot("p"))
            db.openHelper.writableDatabase.query("PRAGMA foreign_key_check").use { assertFalse(it.moveToFirst()) }
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
        Unit
    }
}
