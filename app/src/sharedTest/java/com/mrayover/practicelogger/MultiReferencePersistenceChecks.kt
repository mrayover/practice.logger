package com.mrayover.practicelogger

import android.app.Application
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.UUID
import org.json.JSONObject

abstract class MultiReferencePersistenceChecks {
    protected abstract val context: Application
    protected abstract val journalMode: RoomDatabase.JournalMode
    protected open fun resource(path: String): java.io.InputStream = javaClass.classLoader!!.getResourceAsStream(path)!!
    private fun raw(name: String) = FrameworkSQLiteOpenHelperFactory().create(SupportSQLiteOpenHelper.Configuration.builder(context)
        .name(name).callback(object : SupportSQLiteOpenHelper.Callback(23) {
            override fun onConfigure(db: SupportSQLiteDatabase) { db.setForeignKeyConstraintsEnabled(true) }
            override fun onCreate(db: SupportSQLiteDatabase) = error("Expected frozen database")
            override fun onUpgrade(db: SupportSQLiteDatabase, old: Int, new: Int) = error("Unexpected upgrade")
        }).build())
    private fun open(name: String, migration: Migration = MULTI_REFERENCE_MIGRATION_23_24) =
        Room.databaseBuilder(context, BlockDatabase::class.java, name).addMigrations(migration)
            .setJournalMode(journalMode).build()
    private fun fixture(test: (String, File, MultiReferenceLegacyFixture) -> Unit) {
        val name = "multi-${UUID.randomUUID()}.db"; val files = File(context.cacheDir, name)
        try { val old = MultiReferenceLegacyFixture(context, name, files, ::resource); old.create(); test(name, files, old) }
        finally { context.deleteDatabase(name); files.deleteRecursively() }
    }
    private fun rejects(action: () -> Unit) {
        var failed = false; try { action() } catch (_: Exception) { failed = true }; assertTrue("Unsafe operation accepted", failed)
    }
    @Test fun migrationPreservesUnannotatedLegacyCanonicalCoexistence() = fixture { name, files, frozen ->
        val old = raw(name)
        try {
            val fileName = "${UUID.randomUUID()}.img"
            File(files, "block-images/00000000-0000-4000-8000-000000000004.img").copyTo(File(files, "block-images/$fileName"))
            old.writableDatabase.execSQL("INSERT INTO block_images VALUES ('image',?,'image/png',1,1,68)", arrayOf(fileName))
            frozen.remember(old.writableDatabase)
        } finally { old.close() }
        repeat(2) {
            val db = open(name)
            try {
                frozen.assertPreserved(db.openHelper.writableDatabase)
                runBlocking {
                    assertEquals(0, db.images().find("image")!!.position)
                    assertEquals(1, db.blockMedia().find("image", "VISUAL")!!.position)
                    assertEquals(2, db.referenceOrder("image", MediaRole.VISUAL).size)
                }
            } finally { db.close() }
        }
    }
    @Test fun everyEarlierEmptySchemaMigratesThrough24() {
        for (version in 1..23) {
            val name = "empty-$version-${UUID.randomUUID()}.db"
            try {
                val schema = resource("com.mrayover.practicelogger.BlockDatabase/$version.json").bufferedReader().use {
                    JSONObject(it.readText()).getJSONObject("database")
                }
                val old = FrameworkSQLiteOpenHelperFactory().create(SupportSQLiteOpenHelper.Configuration.builder(context).name(name)
                    .callback(object : SupportSQLiteOpenHelper.Callback(version) {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            val entities = schema.getJSONArray("entities")
                            repeat(entities.length()) { i -> val e = entities.getJSONObject(i)
                                fun execute(sql: String) = db.execSQL(sql.replace("$" + "{TABLE_NAME}", e.getString("tableName")))
                                execute(e.getString("createSql"))
                                val indexes = e.optJSONArray("indices")
                                if (indexes != null) repeat(indexes.length()) { execute(indexes.getJSONObject(it).getString("createSql")) }
                            }
                            val setup = schema.getJSONArray("setupQueries")
                            repeat(setup.length()) { db.execSQL(setup.getString(it)) }
                        }
                        override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = error("Unexpected upgrade")
                    }).build())
                try { assertEquals(version, old.writableDatabase.version) } finally { old.close() }
                val db = BlockDatabase.builder(context, name).setJournalMode(journalMode).build()
                try {
                    assertEquals(24, db.openHelper.writableDatabase.version)
                    assertTrue(portableTargetEmpty(db.openHelper.writableDatabase))
                    db.openHelper.writableDatabase.query("PRAGMA foreign_key_check").use { assertEquals(0, it.count) }
                } finally { db.close() }
            } finally { context.deleteDatabase(name) }
        }
    }
    @Test fun missingOrCorruptOriginalsNeverDeleteMigrationRelationships() = fixture { name, files, frozen ->
        val missing = File(files, "media-library/00000000-0000-4000-8000-000000000001.png")
        val corrupt = File(files, "media-library/00000000-0000-4000-8000-000000000003.wav")
        assertTrue(missing.delete()); corrupt.writeBytes(byteArrayOf(1, 2, 3))
        repeat(2) {
            val db = open(name)
            try {
                frozen.assertPreserved(db.openHelper.writableDatabase, checkOriginals = false)
                assertFalse(missing.exists()); assertArrayEquals(byteArrayOf(1, 2, 3), corrupt.readBytes())
            } finally { db.close() }
        }
    }
    @Test fun migratedRecordsValidateWithZip8RelationshipsAndDensePositions() = fixture { name, _, frozen -> runBlocking {
        val db = open(name)
        try {
            val sql = db.openHelper.writableDatabase
            frozen.assertPreserved(sql)
            val tables = JSONObject()
            portableTables.forEach { tables.put(it.name, it.snapshot(sql) {}) }
            val payload = JSONObject().put("version", 8).put("tables", tables)
                .put("preferences", PortablePreferences.snapshot(context).encode())
            PortableRecords(payload, 8).validateRelationships(context)
            for (position in listOf(-1, 2)) {
                val changed = JSONObject(payload.toString())
                changed.getJSONObject("tables").getJSONArray("block_media_links").getJSONArray(0).put(3, position)
                rejects { PortableRecords(changed, 8) }
            }
        } finally { db.close() }
    } }
    @Test fun migrationRollsBackAtEveryDestructiveBoundary() = fixture { name, _, frozen ->
        val points = listOf("DROP TABLE block_audio_markers", "DROP TABLE block_audio_state", "DROP TABLE block_media_links",
            "ALTER TABLE block_images", "CREATE TABLE block_media_links", "INSERT INTO block_media_links", "CREATE TABLE `block_audio_state`",
            "INSERT INTO block_audio_state", "INSERT INTO block_audio_markers", "DROP TABLE stage3b_block_media_links")
        for (point in points) {
            var injected = false
            val failing = object : Migration(23, 24) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    MULTI_REFERENCE_MIGRATION_23_24.migrate(object : SupportSQLiteDatabase by db {
                        override fun execSQL(sql: String) {
                            db.execSQL(sql)
                            if (sql.startsWith(point)) { injected = true; error("Injected migration failure: $point") }
                        }
                    })
                }
            }
            val db = open(name, failing)
            try { rejects { db.openHelper.writableDatabase } } finally { db.close() }
            assertTrue("Failure boundary not reached: $point", injected)
            val check = raw(name)
            try { assertEquals(23, check.writableDatabase.version); frozen.assertPreserved(check.writableDatabase) } finally { check.close() }
        }
        val migrated = open(name)
        try { frozen.assertPreserved(migrated.openHelper.writableDatabase) } finally { migrated.close() }
    }
    @Test fun invalidHistoricalOwnershipFailsClosed() = fixture { name, _, _ ->
        val old = raw(name)
        try {
            val sql = old.writableDatabase
            sql.execSQL("DROP TRIGGER block_media_type_update")
            sql.execSQL("UPDATE block_media_links SET role='INVALID' WHERE blockId='archived' AND role='VISUAL'")
        } finally { old.close() }
        val db = open(name)
        try { rejects { db.openHelper.writableDatabase } } finally { db.close() }
        val check = raw(name)
        try { assertEquals(23, check.writableDatabase.version) } finally { check.close() }
    }
    @Test fun orderedCollectionsPreserveIndependentLocalWorkAndScalarEdits(): Unit = fixture { name, files, frozen -> runBlocking {
        val db = open(name)
        try {
            frozen.assertPreserved(db.openHelper.writableDatabase)
            val library = MediaLibraryRepository(db, File(files, "media-library"))
            val legacy = ImageReferenceRepository(db, File(files, "block-images"))
            val repository = BlockMediaRepository(db, library, legacy)
            val image = "00000000-0000-4000-8000-000000000001"
            val pdf = "00000000-0000-4000-8000-000000000002"
            val audio = "00000000-0000-4000-8000-000000000003"
            val second = db.media().find(audio)!!.copy(id = UUID.randomUUID().toString(), fileName = "${UUID.randomUUID()}.wav")
            File(files, "media-library/$audio.wav").copyTo(File(files, "media-library/${second.fileName}")); db.media().insert(second)
            repository.add("image", MediaRole.VISUAL, pdf)
            repository.add("legacy", MediaRole.VISUAL, image)
            assertEquals(1, PracticeOverlayRepository(db).copy(BlockVisualReference("image", "library:$image", "image/png", mediaId = image), "legacy", 1))
            repository.add("image", MediaRole.AUDIO, second.id)
            val before = repository.ordered("image", MediaRole.AUDIO)
            val state = db.blockAudio().state("image", audio)
            val markers = db.blockAudio().read("image", audio)
            BlockAudioRepository(db).saveState(state!!.copy(mediaId = second.id, positionMs = 1500))
            repository.reorder("image", MediaRole.AUDIO, before, before.reversed())
            repository.add("image", MediaRole.AUDIO, audio)
            assertEquals(before.reversed(), repository.ordered("image", MediaRole.AUDIO))
            assertEquals(state, db.blockAudio().state("image", audio)); assertEquals(markers, db.blockAudio().read("image", audio))
            rejects { runBlocking { library.delete(audio) } }
            rejects { runBlocking { repository.reorder("image", MediaRole.AUDIO, before, before) } }
            rejects { runBlocking { repository.removeReference("image", MediaRole.AUDIO, "library:$audio", before.reversed()) } }
            repository.removeReference("image", MediaRole.AUDIO, "library:${second.id}", before.reversed(), second.id)
            assertEquals(state, db.blockAudio().state("image", audio)); assertNull(db.blockAudio().state("image", second.id))
            val visualOrder = repository.ordered("image", MediaRole.VISUAL)
            repository.reorder("image", MediaRole.VISUAL, visualOrder, visualOrder.reversed())
            val editor = BlockEditorMedia(context, db, library, legacy, repository)
            editor.open(UUID.randomUUID().toString(), "image")
            editor.select(MediaRole.VISUAL, null)
            editor.save("image", BlockDraft("Image", "Play"), null)
            assertEquals(listOf("library:$image"), repository.ordered("image", MediaRole.VISUAL))
            assertEquals(1, db.overlays().read("image", "library:$image").size)
            assertEquals(2, repository.ordered("legacy", MediaRole.VISUAL).size)
            assertEquals(state, db.blockAudio().state("image", audio))
            val order = repository.ordered("legacy", MediaRole.VISUAL)
            repository.reorder("legacy", MediaRole.VISUAL, order, order.reversed())
            assertTrue(db.hasVisual("legacy", order.first()))
            rejects { db.openHelper.writableDatabase.execSQL("UPDATE block_media_links SET position=-1") }
            rejects { db.openHelper.writableDatabase.execSQL("UPDATE block_images SET position=0.5") }
            assertEquals(1, db.overlays().read("legacy", order.first()).size)
            val currentOrder = repository.ordered("legacy", MediaRole.VISUAL)
            rejects { runBlocking { repository.removeReference("legacy", MediaRole.VISUAL, "library:$image", currentOrder) } }
            repository.removeReference("legacy", MediaRole.VISUAL, "library:$image", currentOrder,
                reviewedVisual = VisualChangeCheck("library:$image", true))
            assertEquals(1, db.overlays().read("legacy", order.first()).size)
            editor.discard()
        } finally { db.close() }
    } }
    @Test fun scalarEmptyAudioDoesNotRemoveVisualAndStaleEditorIsRejected() = fixture { name, files, _ -> runBlocking {
        val db = open(name)
        try {
            db.openHelper.writableDatabase
            val library = MediaLibraryRepository(db, File(files, "media-library")); val legacy = ImageReferenceRepository(db, File(files, "block-images"))
            val repository = BlockMediaRepository(db, library, legacy)
            repository.unlink("pdf", MediaRole.AUDIO)
            assertNotNull(db.blockMedia().find("pdf", "VISUAL"))
            val editor = BlockEditorMedia(context, db, library, legacy, repository)
            editor.open(UUID.randomUUID().toString(), "image"); editor.select(MediaRole.VISUAL, null)
            repository.add("image", MediaRole.VISUAL, "00000000-0000-4000-8000-000000000002")
            rejects { runBlocking { editor.save("image", BlockDraft("Must not commit", "Play"), null) } }
            assertEquals(2, repository.ordered("image", MediaRole.VISUAL).size)
            editor.discard()
        } finally { db.close() }
    } }
}
