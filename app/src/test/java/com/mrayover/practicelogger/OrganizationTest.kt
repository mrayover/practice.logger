package com.mrayover.practicelogger

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class OrganizationTest {
    private val context get() = ApplicationProvider.getApplicationContext<android.content.Context>()
    private fun database() = Room.inMemoryDatabaseBuilder(context, BlockDatabase::class.java).build()

    @Test fun categoriesSourcesAndBlockMetadataSurviveRenameAndReopen() = runBlocking {
        val db = database()
        try {
            val org = OrganizationRepository(db)
            val category = org.createCategory("Chord Vocabulary")
            val source = org.createSource("Drop 2", "Randy Vincent", "BOOK")
            val blockId = BlockRepository(db).save(null, BlockDraft(
                name = "Major seventh inversions", action = "Practice four inversions",
                categoryId = category.id, sourceId = source.id, sourceLocation = "Chapter 2, p. 31"
            ))
            val block = requireNotNull(db.blocks().find(blockId))
            assertEquals(category.id, block.categoryId)
            assertEquals(source.id, block.sourceId)
            assertEquals("Chapter 2, p. 31", block.sourceLocation)
            val snapshot = OrganizationSnapshot(listOf(category), listOf(source))
            assertEquals(listOf(block), matchingBlocks(listOf(block), "randy", false, organization = snapshot))
            assertEquals(listOf(block), matchingBlocks(listOf(block), "chord vocabulary", false, organization = snapshot))
            org.renameCategory(category.id, "Harmony")
            org.updateSource(source.id, "Drop Two", "Vincent", "BOOK")
            assertEquals(category.id, db.blocks().find(blockId)?.categoryId)
            assertEquals(source.id, db.blocks().find(blockId)?.sourceId)
            org.deleteSource(source.id)
            assertNull(db.blocks().find(blockId)?.sourceId)
            assertEquals("", db.blocks().find(blockId)?.sourceLocation)
            org.deleteCategory(category.id)
            assertNull(db.blocks().find(blockId)?.categoryId)
            assertNotNull(db.blocks().find(blockId))
        } finally { db.close() }
    }

    @Test fun invalidAssociationsRejectWithoutChangingBlock() = runBlocking {
        val db = database()
        try {
            val repo = BlockRepository(db)
            val id = repo.save(null, BlockDraft("Arpeggios", "Practice"))
            try {
                repo.save(id, BlockDraft("Changed", "Practice", sourceId = "missing"))
                fail("Missing Source was accepted")
            } catch (_: IllegalArgumentException) { }
            assertEquals("Arpeggios", db.blocks().find(id)?.name)
            val org = OrganizationRepository(db)
            val category = org.createCategory("Technique")
            try { org.createCategory("technique"); fail("Duplicate category accepted") }
            catch (_: IllegalArgumentException) { }
            assertEquals(category.id, db.organization().categoryWithName("TECHNIQUE"))
        } finally { db.close() }
    }
}
