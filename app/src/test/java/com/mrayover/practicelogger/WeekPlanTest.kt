package com.mrayover.practicelogger

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.time.LocalDate
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WeekPlanTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private val sunday = LocalDate.of(2026, 11, 8)

    @Test fun repetitionTargetsPreserveEveryRewardAndSurviveReopen() = runBlocking {
        val name = "week-repetitions-${UUID.randomUUID()}.db"
        var db = BlockDatabase.open(context, name)
        try {
            val repo = BlockRepository(db)
            val id = repo.save(null, BlockDraft("Practice", "Play", "10"), sunday)
            suspend fun plans() = db.weekPlan().read(sunday.toString())
            suspend fun minutes() = plannedWeekMinutes(plans(), db.blocks().readAll(), sunday)
            assertEquals(3, plans().single().plannedRepetitions)
            assertEquals(30L, minutes())
            repo.addToWeek(sunday, id) // Re-adding existing membership must not reset edits.
            repo.setWeekRepetitions(sunday, id, 4)
            repo.addToWeek(sunday, id)
            assertEquals(4, plans().single().plannedRepetitions)
            val sessions = SessionRepository(db) { sunday.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() + 1000 }
            repeat(6) {
                val session = sessions.startBlock(id)
                sessions.finishBlock(session, "COMPLETED", "", placementId = db.sessions().find(session)!!.current!!.id)
            }
            val rewards = PlaygroundRepository(db).ensureLayouts()
            assertEquals(6, rewards.size)
            assertEquals(6, weekCompletionCounts(db.sessions().readAll(), sunday)[id])
            assertEquals(4, WeekRepetitionProgress(4, 6).dots)
            assertTrue(WeekRepetitionProgress(4, 6).fulfilled)
            repo.setWeekRepetitions(sunday, id, 8)
            assertEquals(2, WeekRepetitionProgress(8, 6).remaining)
            assertFalse(WeekRepetitionProgress(8, 6).fulfilled)
            assertEquals(80L, minutes())
            repo.setWeekRepetitions(sunday, id, 2)
            assertTrue(WeekRepetitionProgress(2, 6).fulfilled)
            assertTrue(runCatching { repo.setWeekRepetitions(sunday, id, 0) }.isFailure)
            repo.carryWeek(sunday, sunday.plusWeeks(1))
            assertEquals(3, db.weekPlan().read(sunday.plusWeeks(1).toString()).single().plannedRepetitions)
            repo.save(id, BlockDraft("Practice", "Play", "15"))
            assertEquals(30L, minutes())
            assertEquals(rewards, PlaygroundRepository(db).ensureLayouts())
            val tables = JSONObject()
            portableTables.forEach { table -> tables.put(table.name, table.snapshot(db.openHelper.writableDatabase) {}) }
            val payload = JSONObject().put("version", PORTABLE_VERSION).put("tables", tables)
                .put("preferences", PortablePreferences.snapshot(context).encode())
            val records = PortableRecords(payload, PORTABLE_VERSION)
            val restored = Room.inMemoryDatabaseBuilder(context, BlockDatabase::class.java).build()
            try {
                restored.withTransaction { records.insertInto(restored.openHelper.writableDatabase) }
                assertEquals(plans(), restored.weekPlan().read(sunday.toString()))
                assertEquals(db.playground().completions(), restored.playground().completions())
            } finally { restored.close() }
            db.close(); db = BlockDatabase.open(context, name)
            assertEquals(2, plans().single().plannedRepetitions)
            assertEquals(6, weekCompletionCounts(db.sessions().readAll(), sunday)[id])
            assertEquals(rewards, PlaygroundRepository(db).ensureLayouts())
            BlockRepository(db).removeFromWeek(sunday, id)
            assertEquals(0L, minutes())
            assertEquals(rewards, PlaygroundRepository(db).ensureLayouts())
        } finally { db.close(); context.deleteDatabase(name) }
        Unit
    }

    @Test fun room27WeeklyMigrationRetainsMembershipAndOnePracticeTarget() = runBlocking {
        val name = "week-migrate-${UUID.randomUUID()}.db"
        val schema = javaClass.classLoader!!.getResourceAsStream("com.mrayover.practicelogger.BlockDatabase/27.json")!!
            .bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
        val file = context.getDatabasePath(name).apply { parentFile!!.mkdirs() }
        SQLiteDatabase.openOrCreateDatabase(file, null).use { sql ->
            val entities = schema.getJSONArray("entities")
            repeat(entities.length()) { i ->
                val entity = entities.getJSONObject(i)
                fun execute(query: String) = sql.execSQL(query.replace("$" + "{TABLE_NAME}", entity.getString("tableName")))
                execute(entity.getString("createSql"))
                entity.optJSONArray("indices")?.let { indices -> repeat(indices.length()) { execute(indices.getJSONObject(it).getString("createSql")) } }
            }
            sql.execSQL("INSERT INTO blocks VALUES ('b','Practice','Play',10,NULL,'',0,1,1)")
            sql.execSQL("INSERT INTO week_blocks VALUES ('2026-11-08','b',0)")
            sql.execSQL("INSERT INTO sessions (id,startedAt,status,currentIndex,note) VALUES ('s',1,'COMPLETED',0,'')")
            sql.execSQL("INSERT INTO placements (id,sessionId,sourceBlockId,name,action,tempo,referenceNotes,position,configuredMs,activeMs,outcome,overtimeAccepted,note,lastUsedBpm,prepMs) VALUES ('p','s','b','Practice','Play',NULL,'',0,600000,600000,'COMPLETED',0,'',NULL,0)")
            sql.execSQL("INSERT INTO block_completions VALUES ('p','s','b',1,NULL,NULL,NULL)")
            sql.execSQL("INSERT INTO playground_layouts VALUES ('p',0,0,0)")
            schema.getJSONArray("setupQueries").let { queries -> repeat(queries.length()) { sql.execSQL(queries.getString(it)) } }
            sql.version = 27
        }
        try {
            BlockDatabase.open(context, name).useDb { db ->
                assertEquals(29, db.openHelper.writableDatabase.version)
                assertEquals(WeekBlock(sunday.toString(), "b", 0, 1), db.weekPlan().read(sunday.toString()).single())
                assertEquals("p", db.playground().completions().single().placementId)
                assertEquals("p", db.playground().readAll().single().placementId)
                db.openHelper.writableDatabase.query("PRAGMA foreign_key_check").use { assertFalse(it.moveToFirst()) }
            }
        } finally { context.deleteDatabase(name) }
        Unit
    }

    private inline fun BlockDatabase.useDb(action: (BlockDatabase) -> Unit) {
        try { action(this) } finally { close() }
    }

    @Test fun weeksStartSundayEvenAcrossCalendarYears() {
        assertEquals(LocalDate.of(2026, 12, 27), practiceWeek(LocalDate.of(2027, 1, 2)))
        assertEquals(LocalDate.of(2027, 1, 3), practiceWeek(LocalDate.of(2027, 1, 3)))
        assertEquals(sunday, practiceWeek(sunday.plusDays(6)))
    }

    @Test fun membershipIsOrderedAndExplicitAndArchivedBlocksRemainProtected() = runBlocking {
        val name = "week-${UUID.randomUUID()}.db"
        val previous = sunday.minusWeeks(1)
        try {
            BlockDatabase.open(context, name).useDb { db ->
                val repo = BlockRepository(db)
                val a = repo.save(null, BlockDraft("First", "Practice"), previous)
                val b = repo.save(null, BlockDraft("Second", "Practice"), previous)
                val c = repo.save(null, BlockDraft("Third", "Practice"), previous)
                assertTrue(db.weekPlan().read(sunday.toString()).isEmpty())
                repo.addToWeek(sunday, b)
                repo.setArchived(c, true)
                repeat(2) { repo.carryWeek(previous, sunday) }
                assertEquals(listOf(b, a), db.weekPlan().read(sunday.toString()).map { it.blockId })
                assertTrue(repo.deletePermanently(c) is BlockDeletionResult.Blocked)
                repo.removeFromWeek(sunday, b)
                repo.addToWeek(sunday, b)
                assertEquals(listOf(0, 1), db.weekPlan().read(sunday.toString()).map { it.position })
                assertEquals(listOf(a, b), db.weekPlan().read(sunday.toString()).map { it.blockId })
                assertEquals(listOf(a, b, c), db.weekPlan().read(previous.toString()).map { it.blockId })
                assertEquals(3, db.blocks().readAll().size)
                repo.removeFromWeek(previous, c)
                assertEquals(BlockDeletionResult.Deleted, repo.deletePermanently(c))
            }
            BlockDatabase.open(context, name).useDb { db ->
                assertEquals(listOf("First", "Second"), db.weekPlan().read(sunday.toString())
                    .map { db.blocks().find(it.blockId)!!.name })
                assertTrue(db.weekPlan().read(sunday.plusWeeks(1).toString()).isEmpty())
            }
        } finally {
            context.deleteDatabase(name)
        }
        Unit
    }

    @Test fun singleSessionPreservesDraftAndBatchUsesExistingBuilderAtomically() = runBlocking {
        Room.inMemoryDatabaseBuilder(context, BlockDatabase::class.java).build().useDb { db ->
            val blocks = BlockRepository(db)
            val a = blocks.save(null, BlockDraft("First", "Play", "3"), sunday)
            val b = blocks.save(null, BlockDraft("Second", "Play"), sunday)
            val sessions = SessionRepository(db)
            sessions.addBlock(b)
            val draft = db.sessions().withStatus("DRAFT")!!
            val independent = sessions.startBlock(a)
            val practice = db.sessions().find(independent)!!
            assertEquals("IN_PROGRESS", practice.session.status)
            assertEquals(listOf(a), practice.ordered.map { it.sourceBlockId })
            assertEquals(180_000L, practice.current!!.configuredMs)
            assertEquals(0L, practice.activeMs)
            assertEquals(listOf(b), db.sessions().find(draft.id)!!.ordered.map { it.sourceBlockId })
            blocks.setArchived(b, true)
            assertTrue(runCatching { sessions.addBlocks(listOf(a, b)) }.isFailure)
            assertEquals(1, db.sessions().find(draft.id)!!.placements.size)
            blocks.setArchived(b, false)
            sessions.addBlocks(listOf(a, b))
            assertEquals(draft.id, sessions.start())
            assertEquals(listOf(b, a, b), db.sessions().find(draft.id)!!.ordered.map { it.sourceBlockId })
        }
    }

    @Test fun migratesRoom19WithExistingBlockAndSessionIntact() = runBlocking {
        val name = "week-migration-${UUID.randomUUID()}.db"
        val file = context.getDatabasePath(name)
        file.parentFile!!.mkdirs()
        val schema = javaClass.classLoader!!.getResourceAsStream("com.mrayover.practicelogger.BlockDatabase/19.json")!!
            .bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
        try {
            SQLiteDatabase.openOrCreateDatabase(file, null).use { sql ->
                val tables = schema.getJSONArray("entities")
                repeat(tables.length()) { i ->
                    val table = tables.getJSONObject(i)
                    val nameInSchema = table.getString("tableName")
                    sql.execSQL(table.getString("createSql").replace("\${TABLE_NAME}", nameInSchema))
                    table.optJSONArray("indices")?.let { indexes ->
                        repeat(indexes.length()) { j ->
                            sql.execSQL(indexes.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", nameInSchema))
                        }
                    }
                }
                sql.execSQL("INSERT INTO blocks VALUES ('legacy','Legacy Block','Play',4,NULL,'Notes',0,1,2)")
                sql.execSQL("INSERT INTO sessions VALUES ('s',1,NULL,'IN_PROGRESS',0,'Keep this note')")
                sql.execSQL("INSERT INTO placements VALUES ('p','s','legacy','Legacy Block','Play',NULL,'Notes',0,240000,1200,'PENDING',0,'Note',NULL)")
                val setup = schema.getJSONArray("setupQueries")
                repeat(setup.length()) { sql.execSQL(setup.getString(it)) }
                sql.version = 19
            }
            BlockDatabase.open(context, name).useDb { db ->
                assertEquals("Notes", db.blocks().find("legacy")!!.notes)
                val current = db.sessions().find("s")!!.current!!
                assertEquals(1200L, current.activeMs)
                assertEquals(0L, current.prepMs)
                SessionRepository(db).checkpointPrep("s", current.id, 600)
                assertEquals(current.copy(prepMs = 600), db.sessions().find("s")!!.current)
                assertTrue(db.weekPlan().read(sunday.toString()).isEmpty())
                BlockRepository(db).addToWeek(sunday, "legacy")
                assertEquals("legacy", db.weekPlan().read(sunday.toString()).single().blockId)
            }
        } finally {
            context.deleteDatabase(name)
        }
    }

    @Test fun zipRecordsRoundTripAndRejectInvalidMembership() = runBlocking {
        val root = File(context.cacheDir, "week-${UUID.randomUUID()}").apply { check(mkdir()) }
        val archive = File(root, "week.zip")
        val source = Room.inMemoryDatabaseBuilder(context, BlockDatabase::class.java).build()
        val target = Room.inMemoryDatabaseBuilder(context, BlockDatabase::class.java).build()
        try {
            val blocks = BlockRepository(source)
            val a = blocks.save(null, BlockDraft("First", "Play"), sunday)
            val b = blocks.save(null, BlockDraft("Second", "Play"), sunday)
            blocks.setArchived(b, true)
            blocks.addToWeek(sunday.minusWeeks(1), a)
            val sessions = SessionRepository(source)
            val session = sessions.startBlock(a)
            val placement = source.sessions().find(session)!!.current!!
            sessions.checkpointPrep(session, placement.id, 2500L)
            assertNull(source.goalAttribution().snapshot(placement.id))
            val zip = PortableZip(context, source, root)
            zip.create(archive)
            val verified = zip.verify(archive)
            target.withTransaction { verified.records.insertInto(target.openHelper.writableDatabase) }
            assertEquals(source.weekPlan().read(sunday.toString()), target.weekPlan().read(sunday.toString()))
            assertEquals(source.weekPlan().read(sunday.minusWeeks(1).toString()), target.weekPlan().read(sunday.minusWeeks(1).toString()))
            assertTrue(target.blocks().find(b)!!.archived)
            assertEquals(placement.copy(prepMs = 2500L), target.sessions().find(session)!!.current)
            assertNull(target.goalAttribution().snapshot(placement.id))

            val original = verified.records.payload.toString()
            val legacy = JSONObject(original).put("version", 3)
            legacy.getJSONObject("tables").apply {
                remove("week_blocks")
                remove(INSPIRATION_STORE)
                remove("block_completions")
                remove("block_reflections")
                remove("session_reflections")
                getJSONArray("placements").getJSONArray(0).remove(14)
            }
            val badPrep = JSONObject(original)
            badPrep.getJSONObject("tables").getJSONArray("placements").getJSONArray(0).put(14, -1)
            assertTrue(runCatching { PortableRecords(badPrep, PORTABLE_VERSION) }.isFailure)
            assertEquals(emptyList<JSONObject>(), PortableRecords(legacy, 3)["week_blocks"])
            for (bad in listOf(
                JSONArray().put(sunday.plusDays(1).toString()).put(a).put(0),
                JSONArray().put(sunday.toString()).put("missing").put(0),
                JSONArray().put(sunday.toString()).put(a).put(1),
            )) {
                val corrupted = JSONObject(original)
                corrupted.getJSONObject("tables").put("week_blocks", JSONArray().put(bad))
                assertTrue(runCatching { PortableRecords(corrupted, PORTABLE_VERSION) }.isFailure)
            }
            val duplicate = JSONArray().put(sunday.toString()).put(a).put(0)
            val corrupted = JSONObject(original)
            corrupted.getJSONObject("tables").put("week_blocks", JSONArray().put(duplicate).put(JSONArray(duplicate.toString()).put(2, 1)))
            assertTrue(runCatching { PortableRecords(corrupted, PORTABLE_VERSION) }.isFailure)
        } finally {
            source.close()
            target.close()
            root.deleteRecursively()
        }
        Unit
    }
}
