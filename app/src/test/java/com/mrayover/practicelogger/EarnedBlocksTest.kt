package com.mrayover.practicelogger

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class EarnedBlocksTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()

    @Test fun repeatedPlacementsFrozenGoalAndConcurrentSpendingSurviveReopen() = runBlocking {
        val name = "earned-${UUID.randomUUID()}.db"
        var db = BlockDatabase.open(context, name)
        try {
            val goals = GoalRepository(db) { 10 }
            val firstGoal = goals.save(null, "Higher priority", "", 0, "")
            val earnedGoal = goals.save(null, "Ear training", "", 0, "")
            db.blocks().insert(Block("b", "Current name", "", createdAt = 1, updatedAt = 1))
            goals.setBlocks(earnedGoal, listOf("b"))
            db.goalAttribution().setPrimary(BlockPrimaryGoal("b", earnedGoal))
            db.sessions().insert(PracticeSession("s", 10, status = "IN_PROGRESS"))
            repeat(5) { index ->
                db.sessions().insert(Placement("p$index", "s", "b", "Original name", "", null, "", index, 60_000))
            }
            val sessions = SessionRepository(db) { 42 }
            sessions.finishBlock("s", "COMPLETED", "", placementId = "p0")
            assertTrue(runCatching { sessions.finishBlock("s", "COMPLETED", "", placementId = "p0") }.isFailure)
            sessions.finishBlock("s", "COMPLETED", "", placementId = "p1")
            sessions.finishBlock("s", "SKIPPED", "", placementId = "p2")
            goals.move(earnedGoal, -1)
            val changedGoal = db.goals().find(earnedGoal)!!.goal
            assertTrue(changedGoal.priority < db.goals().find(firstGoal)!!.goal.priority)
            db.goals().update(changedGoal.copy(title = "Renamed"))
            db.goalAttribution().clearPrimary("b")
            sessions.finishBlock("s", "COMPLETED", "", placementId = "p3")

            val repository = EarnedBlocksRepository(db) { 43 }
            val color = goalCompletionColor(2)
            val history = repository.history.first()
            assertEquals(listOf("p3", "p1", "p0"), history.map { it.placementId })
            assertTrue(history.all { it.name == "Original name" && it.completedAt == 42L })
            assertNull(history.first().goalId)
            assertTrue(history.drop(1).all { it.goalId == earnedGoal && it.goalTitle == "Ear training" && it.colorArgb == color })
            assertEquals(3, repository.inventory.first().earned)
            assertNull(repository.consume("missing-color", goalCompletionColor(1)))
            assertEquals("p0", repository.consume("first-use", color))
            assertEquals("p0", repository.consume("first-use", color))
            assertTrue(runCatching { repository.consume("first-use", null) }.isFailure)
            val adjacentUses = (0..1).map { async { repository.consume("last-use-$it", color) } }.awaitAll()
            assertEquals(listOf("p1"), adjacentUses.filterNotNull())
            assertNull(repository.consume("out-of-stock", color))
            assertEquals(history, repository.history.first())

            db.close()
            db = BlockDatabase.open(context, name)
            val reopened = EarnedBlocksRepository(db)
            assertEquals(history, reopened.history.first())
            assertEquals(setOf(
                EarnedBlockBalance(null, false, 1, 0), EarnedBlockBalance(color, true, 2, 2)
            ), reopened.inventory.first().balances.toSet())
            assertEquals("p0", reopened.consume("first-use", color))
            assertEquals(1, reopened.inventory.first().available)
            assertTrue(db.playground().readAll().isEmpty())
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }

    @Test fun migratesRoom29AndRoundTripsCurrentAndPreviousZipRecords() = runBlocking {
        val name = "earned-legacy-${UUID.randomUUID()}.db"
        val schema = javaClass.classLoader!!.getResourceAsStream("com.mrayover.practicelogger.BlockDatabase/29.json")!!
            .bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
        val file = context.getDatabasePath(name).apply { parentFile!!.mkdirs() }
        SQLiteDatabase.openOrCreateDatabase(file, null).use { sql ->
            val entities = schema.getJSONArray("entities")
            repeat(entities.length()) { index ->
                val entity = entities.getJSONObject(index)
                fun execute(create: String) = sql.execSQL(create.replace("\${TABLE_NAME}", entity.getString("tableName")))
                execute(entity.getString("createSql"))
                entity.optJSONArray("indices")?.let { indices ->
                    repeat(indices.length()) { execute(indices.getJSONObject(it).getString("createSql")) }
                }
            }
            val setup = schema.getJSONArray("setupQueries")
            repeat(setup.length()) { sql.execSQL(setup.getString(it)) }
            sql.execSQL("INSERT INTO blocks VALUES ('b','Current name','Current exercise',1,NULL,'',0,10,10,NULL,NULL,'')")
            sql.execSQL("INSERT INTO sessions VALUES ('s',10,50,'COMPLETED',0,'')")
            sql.execSQL("INSERT INTO placements VALUES ('p','s','b','Historical name','Original exercise',NULL,'',0,60000,42000,'COMPLETED',0,'',NULL,0)")
            sql.execSQL("INSERT INTO block_completions VALUES ('p','s','b',50,'old-goal',1,${goalCompletionColor(1)})")
            sql.execSQL("INSERT INTO playground_layouts VALUES ('p',0,0,0)")
            sql.version = 29
        }
        val db = BlockDatabase.open(context, name)
        val restored = Room.inMemoryDatabaseBuilder(context, BlockDatabase::class.java).build()
        val old = Room.inMemoryDatabaseBuilder(context, BlockDatabase::class.java).build()
        val root = File(context.cacheDir, name).apply { mkdirs() }
        try {
            val repository = EarnedBlocksRepository(db) { 60 }
            assertEquals(30, db.openHelper.writableDatabase.version)
            val history = repository.history.first()
            assertEquals(EarnedBlockHistoryEntry("p", "Historical name", 50, "old-goal", null, goalCompletionColor(1)), history.single())
            assertEquals(1, repository.inventory.first().available)
            assertEquals("p", repository.consume("game-1", goalCompletionColor(1)))

            val zip = PortableZip(context, db, root)
            val archive = File(root, "current.zip")
            zip.create(archive)
            val records = zip.verify(archive).records
            restored.withTransaction { records.insertInto(restored.openHelper.writableDatabase) }
            assertEquals(history, EarnedBlocksRepository(restored).history.first())
            assertEquals(0, EarnedBlocksRepository(restored).inventory.first().available)
            assertEquals("p", EarnedBlocksRepository(restored).consume("game-1", goalCompletionColor(1)))
            assertEquals(listOf(PlaygroundLayout("p", 0.0, 0.0, 0.0)), restored.playground().readAll())

            val duplicate = JSONArray().put("p").put("game-2").put(60)
            val orphan = JSONArray().put("missing").put("game-2").put(60)
            for (row in listOf(duplicate, orphan)) {
                val invalid = JSONObject(records.payload.toString())
                invalid.getJSONObject("tables").getJSONArray("earned_block_spends").put(row)
                assertTrue(runCatching { PortableRecords(invalid, PORTABLE_VERSION) }.isFailure)
            }

            val previous = JSONObject(records.payload.toString()).put("version", 14)
            val previousTables = previous.getJSONObject("tables")
            previousTables.remove("earned_block_spends")
            previousTables.getJSONArray("block_completions").getJSONArray(0).remove(7)
            val oldArchive = File(root, "previous.zip")
            writeV14Archive(archive, oldArchive, previous)
            val oldRecords = zip.verify(oldArchive).records
            old.withTransaction { oldRecords.insertInto(old.openHelper.writableDatabase) }
            assertEquals(history, EarnedBlocksRepository(old).history.first())
            assertEquals(1, EarnedBlocksRepository(old).inventory.first().available)
            assertEquals(db.playground().readAll(), old.playground().readAll())
        } finally {
            db.close()
            restored.close()
            old.close()
            context.deleteDatabase(name)
            root.deleteRecursively()
        }
        Unit
    }

    private fun writeV14Archive(current: File, output: File, records: JSONObject) {
        val recordsBytes = records.toString().toByteArray()
        val digest = MessageDigest.getInstance("SHA-256").digest(recordsBytes).joinToString("") { "%02x".format(it) }
        val manifest = ZipFile(current).use { archive ->
            archive.getInputStream(archive.getEntry("manifest.json")).bufferedReader().use { JSONObject(it.readText()) }
        }.put("version", 14).put("roomSchema", 29)
            .put("counts", PortableRecords(records, 14).counts)
            .put("entries", JSONArray().put(PortableEntry("records.json", "records", "app", recordsBytes.size.toLong(), digest).encode()))
        ZipOutputStream(output.outputStream()).use { zip ->
            for ((name, bytes) in listOf("manifest.json" to manifest.toString().toByteArray(), "records.json" to recordsBytes)) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
    }
}
