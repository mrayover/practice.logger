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
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
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


    @Test fun sourceMediaUnlinkDoesNotDeleteOriginalOrBlock() = runBlocking {
        val db = database()
        val files = java.io.File(context.cacheDir, "organization-${java.util.UUID.randomUUID()}").apply { mkdirs() }
        try {
            val media = MediaLibraryRepository(db, files)
            val bytes = java.io.ByteArrayOutputStream().use { out ->
                val bitmap = android.graphics.Bitmap.createBitmap(16, 16, android.graphics.Bitmap.Config.ARGB_8888)
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
                bitmap.recycle()
                out.toByteArray()
            }
            val asset = media.import("exercise.png", "image/png") { bytes.inputStream() }
            val organization = OrganizationRepository(db)
            val first = organization.createSource("Book", "Author")
            val second = organization.createSource("Lesson", "Teacher", "LESSON")
            organization.setMediaSources(asset.id, setOf(first.id, second.id))
            assertEquals(setOf(first.id, second.id), db.organization().mediaSourceIds(asset.id).toSet())
            val block = BlockRepository(db).save(null, BlockDraft("Practice", "Play", sourceId = first.id))
            organization.deleteSource(first.id)
            assertEquals(setOf(second.id), db.organization().mediaSourceIds(asset.id).toSet())
            assertNull(db.blocks().find(block)?.sourceId)
            assertNotNull(db.media().find(asset.id))
            assertTrue(java.io.File(files, asset.fileName).exists())
            organization.setMediaSources(asset.id, emptySet())
            assertTrue(db.organization().mediaSourceIds(asset.id).isEmpty())
            assertNotNull(db.media().find(asset.id))
        } finally { db.close(); files.deleteRecursively() }
    }


    @Test fun upgradesRoom28WithoutChangingExistingBlocks() = runBlocking {
        val name = "organization-migration-${java.util.UUID.randomUUID()}.db"
        val schema = javaClass.classLoader!!.getResourceAsStream("com.mrayover.practicelogger.BlockDatabase/28.json")!!
            .bufferedReader().use { org.json.JSONObject(it.readText()).getJSONObject("database") }
        val file = context.getDatabasePath(name).apply { parentFile!!.mkdirs() }
        try {
            android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(file, null).use { sql ->
                val entities = schema.getJSONArray("entities")
                repeat(entities.length()) { i ->
                    val e = entities.getJSONObject(i)
                    sql.execSQL(e.getString("createSql").replace("\${TABLE_NAME}", e.getString("tableName")))
                    e.optJSONArray("indices")?.let { indices ->
                        repeat(indices.length()) { j ->
                            sql.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", e.getString("tableName")))
                        }
                    }
                }
                sql.execSQL("INSERT INTO blocks VALUES ('existing','Legacy exercise','Practice',10,NULL,'p. 3',0,1,2)")
                val setup = schema.getJSONArray("setupQueries")
                repeat(setup.length()) { sql.execSQL(setup.getString(it)) }
                sql.version = 28
            }
            val db = BlockDatabase.open(context, name)
            try {
                assertEquals(29, db.openHelper.writableDatabase.version)
                val block = requireNotNull(db.blocks().find("existing"))
                assertEquals("Legacy exercise", block.name)
                assertNull(block.categoryId)
                assertNull(block.sourceId)
                assertEquals("", block.sourceLocation)
                assertTrue(db.organization().mediaSourceIds("missing").isEmpty())
            } finally { db.close() }
        } finally { context.deleteDatabase(name) }
    }
}
