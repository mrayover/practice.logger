package com.mrayover.practicelogger

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.UUID

@RunWith(RobolectricTestRunner::class) @Config(sdk = [35])
class BlockEditorSaveTest {
    @Test fun emptyReferenceSaveRollsBackWeekAndBuilderAndRetriesOnce(): Unit = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val name = "editor-save-${UUID.randomUUID()}.db"
        val db = BlockDatabase.open(app, name)
        val library = MediaLibraryRepository(db, File(app.filesDir, "media-library"))
        val legacy = ImageReferenceRepository(db, File(app.filesDir, "block-images"))
        val links = BlockMediaRepository(db, library, legacy)
        val token = UUID.randomUUID().toString()
        val editor = BlockEditorMedia(app, db, library, legacy, links)
        try {
            editor.open(token, null)
            val week = practiceWeek(java.time.LocalDate.of(2026, 11, 10))
            var rejected = false
            try { editor.save(null, BlockDraft("Retry", "Play"), week, true) { error("Injected commit failure") } }
            catch (_: IllegalStateException) { rejected = true }
            assertTrue(rejected); assertTrue(db.blocks().readAll().isEmpty()); assertTrue(db.sessions().readAll().isEmpty())
            assertTrue(db.weekPlan().read(week.toString()).isEmpty())
            val id = editor.save(null, BlockDraft("Retry", "Play"), week, true)
            assertEquals(id, db.weekPlan().read(week.toString()).single().blockId)
            val recovered = BlockEditorMedia(app, db, library, legacy, links); recovered.open(token, null)
            assertEquals(id, recovered.save(null, BlockDraft("Retry", "Play"), week, true))
            assertEquals(1, db.blocks().readAll().size); assertEquals(1, db.sessions().readAll().single().placements.size)
        } finally { editor.discard(); db.close(); app.deleteDatabase(name) }
    }
}
