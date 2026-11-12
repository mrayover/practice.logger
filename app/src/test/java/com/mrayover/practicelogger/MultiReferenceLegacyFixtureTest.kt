package com.mrayover.practicelogger

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MultiReferenceLegacyFixtureTest {
    private fun fixture(check: (MultiReferenceLegacyFixture, BlockDatabase) -> Unit) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "stage3a-${UUID.randomUUID()}.db"
        val files = File(context.cacheDir, name)
        try {
            val fixture = MultiReferenceLegacyFixture(context, name, files)
            fixture.create()
            // Isolate schema/row verification from Windows native WAL journal transitions.
            fun open() = Room.databaseBuilder(context, BlockDatabase::class.java, name)
                .addMigrations(MULTI_REFERENCE_MIGRATION_23_24)
                .setJournalMode(RoomDatabase.JournalMode.TRUNCATE).build()
            val db = open()
            try { check(fixture, db) } finally { db.close() }
            val reopened = open()
            try { fixture.assertPreserved(reopened.openHelper.writableDatabase) } finally { reopened.close() }
        } finally { context.deleteDatabase(name); files.deleteRecursively() }
    }

    @Test fun populatedRoom23ReopensWithoutLosingAnyLegacyIdentityOrOriginal() = fixture { legacy, db ->
        assertEquals(24, db.openHelper.writableDatabase.version)
        legacy.assertPreserved(db.openHelper.writableDatabase)
        runBlocking {
            val dependencies = db.blocks().dependencies("image")
            assertEquals(1, dependencies.goals); assertEquals(1, dependencies.routines)
            assertEquals(1, dependencies.sessionEntries); assertEquals(1, dependencies.completedEntries)
            assertEquals(1, dependencies.weeks); assertEquals(2, dependencies.mediaLinks)
            val markers = effectiveBlockMarkers(db.markers().read("00000000-0000-4000-8000-000000000003"),
                db.blockAudio().read("image", "00000000-0000-4000-8000-000000000003"))
            assertEquals(setOf(BlockMarkerOwnership.LOCAL, BlockMarkerOwnership.CUSTOMIZED, BlockMarkerOwnership.MASTER),
                markers.visible.map { it.ownership }.toSet())
            assertEquals(1, markers.hidden.size)
        }
    }

    @Test fun frozenConstraintsProtectReferencesAndAnnotatedSources() = fixture { legacy, db ->
        val sql = db.openHelper.writableDatabase
        fun rejects(statement: String) {
            try { sql.execSQL(statement); fail("Accepted $statement") } catch (_: SQLiteConstraintException) { }
        }
        rejects("DELETE FROM media_assets WHERE id='00000000-0000-4000-8000-000000000001'")
        rejects("DELETE FROM blocks WHERE id='image'")
        rejects("DELETE FROM block_media_links WHERE blockId='pdf' AND role='VISUAL'")
        rejects("DELETE FROM block_images WHERE blockId='legacy'")
        rejects("UPDATE block_media_links SET role='INVALID' WHERE blockId='archived'")
        rejects("UPDATE media_assets SET category='AUDIO' WHERE id='00000000-0000-4000-8000-000000000001'")
        legacy.assertPreserved(sql)
    }
}
