package com.mrayover.practicelogger

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TranscribeActivityTest {
    @Test fun completedTimeCombinesWithBlockTimeAndSurvivesMediaDeletion(): Unit = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), BlockDatabase::class.java).build()
        try {
            val dao = db.transcribeActivities()
            val goal = Goal("g", "Goal", "", 25, "", true, 0, 0, 0)
            db.goals().insert(goal)
            val media = MediaAsset("audio", "Audio", "original.wav", "audio/wav", "AUDIO", "audio.wav", 1, 1)
            db.media().insert(media)
            db.sessions().insert(PracticeSession("s", 1, status = "IN_PROGRESS"))
            db.goals().insert(GoalSession("s", goal.id))
            repeat(2) {
                db.sessions().insert(Placement("p$it", "s", "b", "Block", "Play", null, "",
                    it, 60_000, activeMs = 1000))
            }
            dao.insert(TranscribeActivity("a", 1, activeMs = 3000, goalId = goal.id, audioId = media.id))
            assertEquals(2000L, dao.observePracticeTime().first().sumOf { it.activeMs })
            assertEquals(1, dao.save("a", 4000, goal.id, media.id, 2))
            assertEquals(0, dao.save("a", 8000, goal.id, media.id, 3))
            dao.insert(TranscribeActivity("b", 1, completedAt = 2, activeMs = 5000, goalId = goal.id))
            dao.insert(TranscribeActivity("c", 1, completedAt = 2, activeMs = 6000))
            assertEquals(11_000L, db.goals().observeTime().first().single().activeMs)
            assertEquals(17_000L, dao.observePracticeTime().first().single().activeMs)
            assertEquals(25, db.goals().find(goal.id)!!.goal.progress)
            db.openHelper.writableDatabase.execSQL("DELETE FROM media_assets WHERE id = 'audio'")
            assertNull(dao.find("a")!!.audioId)
            assertEquals(4000L, dao.find("a")!!.activeMs)
            db.openHelper.writableDatabase.execSQL("DELETE FROM session_goals")
            try {
                db.openHelper.writableDatabase.execSQL("DELETE FROM goals WHERE id = 'g'")
                fail("Removed an attributed Goal")
            } catch (_: SQLiteConstraintException) { }
        } finally { db.close() }
    }

    @Test fun activityOnlyTargetRefusesJsonRestore(): Unit = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), BlockDatabase::class.java).build()
        try {
            val backup = BackupRepository(db)
            val draft = TranscribeActivity("a", 1, activeMs = 3000)
            db.transcribeActivities().insert(draft)
            try {
                backup.restore(backup.snapshot())
                fail("Restore should refuse a target containing only an unfinished Transcribe activity")
            } catch (_: NonEmptyRestoreException) { }
            assertEquals(draft, db.transcribeActivities().unfinished())
        } finally { db.close() }
    }

    @Test fun localWeekUsesStartDateAndHalfOpenMondayBoundaries() {
        val monday = LocalDate.of(2026, 10, 26)
        val zone = ZoneId.of("America/Los_Angeles")
        val start = monday.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = monday.plusWeeks(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val entries = listOf(PracticeTimeEntry(start - 1, 1), PracticeTimeEntry(start, 2),
            PracticeTimeEntry(end - 1, 4), PracticeTimeEntry(end, 8))
        assertEquals(6L, weeklyPracticeMs(entries, monday, zone))
    }

    @Test fun monotonicClockRestoresPausedAndCanRunBeyondBlockLength() {
        var now = 1_000_000_000L
        val clock = ToolClock(12_000) { now }
        assertFalse(clock.running)
        assertEquals(12_000L, clock.elapsed)
        clock.start()
        now += 5 * 60 * 60 * 1000
        clock.pause()
        now += 30_000
        assertEquals(18_012_000L, clock.elapsed)
    }

    @Test fun onlyTheCurrentClockCanReleaseItsLease() {
        val block = Any()
        val transcribe = Any()
        try {
            assertTrue(PracticeTimingOwner.acquire(block))
            assertFalse(PracticeTimingOwner.acquire(transcribe))
            PracticeTimingOwner.release(transcribe)
            assertFalse(PracticeTimingOwner.acquire(transcribe))
            PracticeTimingOwner.release(block)
            assertTrue(PracticeTimingOwner.acquire(transcribe))
            assertFalse(PracticeTimingOwner.acquire(block))
        } finally {
            PracticeTimingOwner.release(block)
            PracticeTimingOwner.release(transcribe)
        }
    }
}
