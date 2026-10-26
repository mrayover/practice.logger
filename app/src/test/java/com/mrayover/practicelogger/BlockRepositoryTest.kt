package com.mrayover.practicelogger

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BlockRepositoryTest {
    @Test fun createEditArchiveRestoreSurviveDatabaseReopen() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        context.deleteDatabase("catalog-test.db")
        var tick = 100L
        var db = BlockDatabase.open(context, "catalog-test.db")
        val id: String
        try {
            var repo = BlockRepository(db) { tick }
            id = repo.save(null, BlockDraft(" Bridge ", " Play from memory ", "7", "120", " page 3 "))
            assertEquals("Bridge", db.blocks().find(id)!!.name)
            tick = 200
            repo.save(id, BlockDraft("Bridge revised", "Slowly", "11", "", "source"))
            repo.setArchived(id, true)
            val saved = db.blocks().find(id)!!
            assertEquals(100L, saved.createdAt)
            assertEquals(200L, saved.updatedAt)
            assertTrue(saved.archived)
            assertNull(saved.tempo)
            db.close()
            db = BlockDatabase.open(context, "catalog-test.db")
            assertEquals(saved, db.blocks().find(id))
            repo = BlockRepository(db) { 300L }
            repo.setArchived(id, false)
            assertFalse(db.blocks().find(id)!!.archived)
            assertEquals("Slowly", db.blocks().find(id)!!.action)
            assertEquals(11, db.blocks().find(id)!!.durationMinutes)
        } finally { db.close(); context.deleteDatabase("catalog-test.db") }
    }
    @Test fun invalidCreateDoesNotWrite() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val db = androidx.room.Room.inMemoryDatabaseBuilder(context, BlockDatabase::class.java).build()
        try {
            val repo = BlockRepository(db)
            try { repo.save(null, BlockDraft(" ", "action")); fail("Invalid draft saved") } catch (_: IllegalArgumentException) { }
            val id = repo.save(null, BlockDraft("Valid", "Practice"))
            try { repo.save(id, BlockDraft("broken", "")); fail("Invalid edit saved") } catch (_: IllegalArgumentException) { }
            assertEquals("Valid", db.blocks().find(id)!!.name)
        } finally { db.close() }
    }
}

class BlockValidationTest {
    @Test fun requiredFieldsAndNumericBoundaries() {
        assertEquals(setOf("name", "action"), BlockDraft().errors().keys)
        for (bad in listOf("", "0", "-1", "1.5", "abc", "2147483648")) assertTrue(BlockDraft("n", "a", bad).errors().containsKey("duration"))
        for (bad in listOf("0", "-5", "1.2", "abc", "999999999999")) assertTrue(BlockDraft("n", "a", tempo = bad).errors().containsKey("tempo"))
        assertTrue(BlockDraft("n", "a", "2147483647", " ").errors().isEmpty())
        assertTrue(BlockDraft("n", "a", "5", "120").errors().isEmpty())
    }
    @Test fun searchIsLiteralCaseInsensitiveAndArchiveAware() {
        val active = Block("a", "Bridge 100%", "Play slowly", notes = "Book_1", createdAt = 1, updatedAt = 1)
        val archived = active.copy(id = "b", archived = true)
        val all = listOf(active, archived)
        for (query in listOf(" bridge ", "SLOWLY", "%", "BOOK_1")) assertEquals(listOf(active), matchingBlocks(all, query, false))
        assertEquals(listOf(archived), matchingBlocks(all, "", true))
        assertTrue(matchingBlocks(all, "missing", false).isEmpty())
    }
}
