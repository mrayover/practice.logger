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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RoutineBackupTest {
    private val context get() = ApplicationProvider.getApplicationContext<android.content.Context>()
    private fun memory() = Room.inMemoryDatabaseBuilder(context, BlockDatabase::class.java).build()
    private suspend fun fixture(db: BlockDatabase): BackupData {
        val a = BlockRepository(db).save(null, BlockDraft("A", "Play", "2", "100"))
        val b = BlockRepository(db).save(null, BlockDraft("B", "Listen", "3"))
        RoutineRepository(db).save(null, "Warmup", listOf(RoutineSelection(b, 60_000), RoutineSelection(a, 180_000), RoutineSelection(b, 90_000)))
        RoutineRepository(db).save(null, "Second", listOf(RoutineSelection(a, 120_000)))
        SessionRepository(db).addBlock(a)
        return BackupRepository(db).snapshot()
    }
    @Test fun versionFourFileRoundTripRestoresRoutinePlacementsAndReopens(): Unit = runBlocking {
        val source = memory(); val name = "routine-restored.db"; context.deleteDatabase(name)
        var target = BlockDatabase.open(context, name)
        try {
            val original = fixture(source); val bytes = BackupCodec.encode(original)
            assertEquals(5, JSONObject(String(bytes)).getInt("version"))
            assertEquals(original, BackupCodec.decode(bytes))
            BackupRepository(target).restore(BackupCodec.decode(bytes))
            target.close(); target = BlockDatabase.open(context, name)
            assertEquals(original, BackupRepository(target).snapshot())
            original.routines.forEach { routine ->
                val expected = original.routinePlacements.filter { it.routineId == routine.id }.sortedBy { it.position }
                val session = target.sessions().find(SessionRepository(target).startRoutine(routine.id))!!
                assertEquals(expected.map { it.sourceBlockId }, session.ordered.map { it.sourceBlockId })
                assertEquals(expected.map { it.configuredMs }, session.ordered.map { it.configuredMs })
            }
            assertTrue(target.takes().readAll().isEmpty())
        } finally { source.close(); target.close(); context.deleteDatabase(name) }
    }
    @Test fun legacyOneTwoAndThreeReadWithoutInventingRoutines(): Unit = runBlocking {
        val source = memory()
        try {
            val data = fixture(source)
            for (version in 1..3) {
                val root = JSONObject(String(BackupCodec.encode(data))).put("version", version)
                root.remove("routines"); root.remove("routinePlacements")
                if (version < 3) { val rows = root.getJSONArray("placements"); repeat(rows.length()) { rows.getJSONObject(it).remove("lastUsedBpm") } }
                val old = BackupCodec.decode(root.toString().toByteArray())
                assertTrue(old.routines.isEmpty()); assertTrue(old.routinePlacements.isEmpty())
                assertEquals(data.blocks, old.blocks); assertEquals(data.sessions, old.sessions)
                val target = memory()
                try { BackupRepository(target).restore(old); assertEquals(old, BackupRepository(target).snapshot()) } finally { target.close() }
            }
        } finally { source.close() }
    }
    @Test fun malformedRoutineReferencesOrderNamesDurationsAndRequiredArraysReject(): Unit = runBlocking {
        val db = memory()
        try {
            val data = fixture(db)
            val row = data.routinePlacements.first()
            val cases = listOf(
                data.copy(routines = data.routines + data.routines.first()),
                data.copy(routines = data.routines.map { it.copy(name = " ") }),
                data.copy(routines = data.routines.map { it.copy(updatedAt = -1) }),
                data.copy(routinePlacements = emptyList()),
                data.copy(routinePlacements = data.routinePlacements + row),
                data.copy(routinePlacements = data.routinePlacements.map { if (it.id == row.id) it.copy(sourceBlockId = "missing") else it }),
                data.copy(routinePlacements = data.routinePlacements.map { if (it.id == row.id) it.copy(routineId = "missing") else it }),
                data.copy(routinePlacements = data.routinePlacements.map { if (it.id == row.id) it.copy(position = 100) else it }),
                data.copy(routinePlacements = data.routinePlacements.map { if (it.id == row.id) it.copy(configuredMs = 0) else it })
            )
            cases.forEach { try { BackupCodec.validate(it); fail("Invalid routine backup accepted") } catch (_: IllegalArgumentException) { } }
            for (key in listOf("routines", "routinePlacements")) {
                val root = JSONObject(String(BackupCodec.encode(data))); root.remove(key)
                try { BackupCodec.decode(root.toString().toByteArray()); fail("Missing Routine array accepted") } catch (_: org.json.JSONException) { }
            }
            val unsupported = JSONObject(String(BackupCodec.encode(data))).put("version", 6)
            try { BackupCodec.decode(unsupported.toString().toByteArray()); fail("Future format accepted") } catch (_: IllegalArgumentException) { }
        } finally { db.close() }
    }
    @Test fun lateRoutinePlacementFailureRollsBackEveryRestoredTable(): Unit = runBlocking {
        val source = memory(); val target = memory()
        try {
            val data = fixture(source)
            target.openHelper.writableDatabase.execSQL("CREATE TRIGGER reject_routine BEFORE INSERT ON routine_placements BEGIN SELECT RAISE(ABORT, 'test failure'); END")
            try { BackupRepository(target).restore(data); fail("Partial restore committed") } catch (_: android.database.sqlite.SQLiteException) { }
            assertEquals(BackupData(emptyList(), emptyList(), emptyList()), BackupRepository(target).snapshot())
            assertTrue(target.takes().readAll().isEmpty())
        } finally { source.close(); target.close() }
    }
    @Test fun existingRoutineOrStandaloneTakeMakesRestoreNonempty(): Unit = runBlocking {
        val source = memory(); val target = memory(); val audioOnly = memory()
        try {
            val data = fixture(source)
            target.routines().insert(SavedRoutine("keep", "Keep", 1, 1))
            try { BackupRepository(target).restore(data); fail("Existing Routine overwritten") } catch (_: NonEmptyRestoreException) { }
            assertEquals("Keep", target.routines().find("keep")!!.routine.name); assertTrue(target.blocks().readAll().isEmpty())
            val take = BlockTake("audio", null, 1, "FREE", 1000, "00000000-0000-0000-0000-000000000001.m4a")
            audioOnly.takes().insert(take)
            try { BackupRepository(audioOnly).restore(data); fail("Audio-only app treated as empty") } catch (_: NonEmptyRestoreException) { }
            assertEquals(listOf(take), audioOnly.takes().readAll()); assertTrue(audioOnly.blocks().readAll().isEmpty())
        } finally { source.close(); target.close(); audioOnly.close() }
    }
}
