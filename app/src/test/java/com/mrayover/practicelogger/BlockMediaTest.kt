package com.mrayover.practicelogger

import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.*
import java.util.UUID

@RunWith(RobolectricTestRunner::class) @Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BlockMediaTest {
    private val context get() = ApplicationProvider.getApplicationContext<android.content.Context>()
    private fun image() = ByteArrayOutputStream().use { out ->
        Bitmap.createBitmap(32, 24, Bitmap.Config.ARGB_8888).let { it.compress(Bitmap.CompressFormat.PNG, 95, out); it.recycle() }; out.toByteArray()
    }
    private suspend fun rejected(action: suspend () -> Unit) {
        try { action(); fail("Accepted unsafe link operation") } catch (_: Exception) { }
    }
    @Test fun sharedIdentityAtomicReplacementRestrictiveDeletionMarkersAndReopen(): Unit = runBlocking {
        val name = "block-media-${UUID.randomUUID()}.db"; var db = BlockDatabase.open(context, name)
        val dir = File(context.cacheDir, UUID.randomUUID().toString()); val oldDir = File(dir, "legacy")
        try {
            var library = MediaLibraryRepository(db, dir); var legacy = ImageReferenceRepository(db, oldDir)
            var links = BlockMediaRepository(db, library, legacy)
            val blocks = BlockRepository(db); val a = blocks.save(null, BlockDraft("A", "Play")); val b = blocks.save(null, BlockDraft("B", "Play"))
            val bytes = image(); val visual = library.import("Score.png", null) { bytes.inputStream() }
            // Asset metadata is sufficient for relationship checks; audio decoding is covered natively.
            val audioFile = File(dir, "${UUID.randomUUID()}.wav").apply { writeBytes(bytes) }
            val audio = visual.copy(id = UUID.randomUUID().toString(), title = "Audio", originalName = "Audio.wav", category = "AUDIO", mimeType = "audio/wav", fileName = audioFile.name, durationMs = 1000)
            db.media().insert(audio)
            for (id in listOf(a, b)) { links.link(id, MediaRole.VISUAL, visual.id); links.link(id, MediaRole.AUDIO, audio.id) }
            assertEquals(2, dir.listFiles()!!.size); assertEquals(4, db.blockMedia().readAll().size)
            assertFalse(blocks.reviewDeletion(a).dependencies.canDelete); assertEquals(2, blocks.reviewDeletion(a).dependencies.mediaLinks)
            val marker = MasterAudioMarkerRepository(db).save(audio.id, null, 100, null, "Keep", "Master")
            rejected { library.delete(audio.id) }; assertNotNull(db.markers().find(marker.id)); assertArrayEquals(bytes, audioFile.readBytes())
            rejected { db.blocks().delete(a) }; rejected { db.media().remove(visual.id) }
            rejected { links.link(a, MediaRole.AUDIO, visual.id) }; rejected { links.link(a, MediaRole.VISUAL, audio.id) }
            rejected { links.link(a, MediaRole.AUDIO, "missing") }; rejected { links.link("missing", MediaRole.AUDIO, audio.id) }
            rejected { db.blockMedia().save(BlockMediaLink(a, "INVALID", visual.id)) }
            rejected { db.blockMedia().save(BlockMediaLink(a, "VISUAL", audio.id)) }
            rejected { db.openHelper.writableDatabase.execSQL("UPDATE media_assets SET category='AUDIO' WHERE id=?", arrayOf(visual.id)) }
            library.rename(visual.id, "Renamed"); library.rename(audio.id, "Renamed audio"); blocks.setArchived(a, true)
            links.unlink(a, MediaRole.AUDIO); assertEquals(audio.id, db.blockMedia().find(b, "AUDIO")!!.mediaId)
            assertTrue(audioFile.exists()); rejected { library.delete(audio.id) }
            links.unlink(b, MediaRole.AUDIO); library.delete(audio.id); assertFalse(audioFile.exists()); assertNull(db.markers().find(marker.id))
            db.close(); db = BlockDatabase.open(context, name); library = MediaLibraryRepository(db, dir); legacy = ImageReferenceRepository(db, oldDir); links = BlockMediaRepository(db, library, legacy)
            assertEquals(2, db.blockMedia().readAll().size); assertTrue(db.blocks().find(a)!!.archived)
            assertEquals("Renamed", db.media().find(visual.id)!!.title); assertTrue(library.available(visual))
            links.unlink(a, MediaRole.VISUAL); assertNotNull(db.blockMedia().find(b, "VISUAL")); assertTrue(File(dir, visual.fileName).exists())
            links.unlink(b, MediaRole.VISUAL); library.delete(visual.id); assertFalse(File(dir, visual.fileName).exists())
            db.openHelper.readableDatabase.query("PRAGMA foreign_key_check").use { assertEquals(0, it.count) }
        } finally { db.close(); context.deleteDatabase(name); dir.deleteRecursively() }
    }
    @Test fun legacyRetirementRollsBackAndImportedAssetSurvivesFailedAssociation(): Unit = runBlocking {
        val name = "retire-${UUID.randomUUID()}.db"; val db = BlockDatabase.open(context, name)
        val dir = File(context.cacheDir, UUID.randomUUID().toString()); val oldDir = File(dir, "old")
        try {
            val legacy = ImageReferenceRepository(db, oldDir); val library = MediaLibraryRepository(db, dir); val links = BlockMediaRepository(db, library, legacy)
            val id = BlockRepository(db).save(null, BlockDraft("Legacy", "Read")); val bytes = image()
            val old = legacy.import(id) { bytes.inputStream() }; val file = File(oldDir, old.fileName)
            val canonical = library.import("New.png", null) { bytes.inputStream() }
            rejected { links.link(id, MediaRole.VISUAL, canonical.id) }; assertEquals(old, db.images().find(id)); assertArrayEquals(bytes, file.readBytes())
            db.openHelper.writableDatabase.execSQL("CREATE TRIGGER reject_link BEFORE INSERT ON block_media_links BEGIN SELECT RAISE(ABORT,'injected commit failure'); END")
            rejected { links.link(id, MediaRole.VISUAL, canonical.id, replaceLegacy = true) }
            assertEquals(old, db.images().find(id)); assertArrayEquals(bytes, file.readBytes()); assertTrue(library.available(canonical))
            assertTrue(db.blockMedia().readAll().isEmpty()); assertEquals(0, legacy.recover()); assertEquals(0, library.recover())
            db.openHelper.writableDatabase.execSQL("DROP TRIGGER reject_link")
            links.link(id, MediaRole.VISUAL, canonical.id, replaceLegacy = true)
            assertNull(db.images().find(id)); assertFalse(file.exists()); assertTrue(library.available(canonical))
            val replacement = library.import("Replacement.png", null) { bytes.inputStream() }
            db.openHelper.writableDatabase.execSQL("CREATE TRIGGER reject_update BEFORE UPDATE ON block_media_links BEGIN SELECT RAISE(ABORT,'injected replacement failure'); END")
            rejected { links.link(id, MediaRole.VISUAL, replacement.id) }
            assertEquals(canonical.id, db.blockMedia().find(id, "VISUAL")!!.mediaId)
            File(dir, replacement.fileName).delete(); rejected { links.link(id, MediaRole.VISUAL, replacement.id) }
            assertEquals(canonical.id, db.blockMedia().find(id, "VISUAL")!!.mediaId)
        } finally { db.close(); context.deleteDatabase(name); dir.deleteRecursively() }
    }
    @Test fun explicitThirteenToFifteenRetainsEveryTableAndForeignKey() = verifyMigration(13)
    @Test fun explicitFourteenToFifteenRetainsEveryTableAndForeignKey() = verifyMigration(14)

    private fun verifyMigration(version: Int): Unit = runBlocking {
        val name = "migration$version-${UUID.randomUUID()}.db"
        val schema = javaClass.classLoader!!.getResourceAsStream("com.mrayover.practicelogger.BlockDatabase/$version.json")!!.bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
        val path = context.getDatabasePath(name).apply { parentFile!!.mkdirs() }
        val before = linkedMapOf<String, List<List<String?>>>()
        android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(path, null).use { sql ->
            val entities = schema.getJSONArray("entities")
            repeat(entities.length()) { i -> val e = entities.getJSONObject(i); val table = e.getString("tableName")
                sql.execSQL(e.getString("createSql").replace("\${TABLE_NAME}", table))
                val indices = e.optJSONArray("indices") ?: org.json.JSONArray()
                repeat(indices.length()) { sql.execSQL(indices.getJSONObject(it).getString("createSql").replace("\${TABLE_NAME}", table)) }
            }
            val setup = schema.getJSONArray("setupQueries"); repeat(setup.length()) { sql.execSQL(setup.getString(it)) }
            sql.execSQL("INSERT INTO blocks VALUES ('b','Keep','Play',5,90,'Notes',1,1,2)")
            sql.execSQL("INSERT INTO media_assets VALUES ('a','Keep audio','keep.wav','audio/wav','AUDIO','${UUID.randomUUID()}.wav',44,1,NULL,NULL,NULL,1000,'hash')")
            sql.execSQL("INSERT INTO media_markers VALUES ('m','a','POINT',100,NULL,'Keep marker','Keep note')")
            sql.execSQL("INSERT INTO block_images VALUES ('b','${UUID.randomUUID()}.pdf','application/pdf',600,800,1000)")
            sql.execSQL("INSERT INTO takes VALUES ('t','b',3,'IMPORTED_LOOP',1234,'${UUID.randomUUID()}.mp3','Original',NULL,NULL)")
            if (version == 14) sql.execSQL("INSERT INTO block_media_links VALUES ('b','AUDIO','a')")
            repeat(entities.length()) { i -> val table = entities.getJSONObject(i).getString("tableName")
                before[table] = sql.rawQuery("SELECT * FROM $table", null).use { c -> buildList { while(c.moveToNext()) add((0 until c.columnCount).map { c.getString(it) }) } }
            }
            sql.version = version
        }
        val db = BlockDatabase.open(context, name)
        try {
            assertEquals(if (version == 14) 1 else 0, db.blockMedia().readAll().size)
            assertEquals(17, db.openHelper.readableDatabase.version)
            before.forEach { (table, rows) -> db.openHelper.readableDatabase.query("SELECT * FROM $table").use { c ->
                assertEquals(table, rows, buildList { while(c.moveToNext()) add((0 until c.columnCount).map { c.getString(it) }) })
            } }
            db.blockMedia().save(BlockMediaLink("b", "AUDIO", "a"))
            rejected { db.blockMedia().save(BlockMediaLink("b", "VISUAL", "a")) }
            db.openHelper.readableDatabase.query("PRAGMA foreign_key_check").use { assertEquals(0, it.count) }
        } finally { db.close(); context.deleteDatabase(name) }
    }
    @Test fun jsonSevenRestoresDataWithoutDanglingMediaLinks(): Unit = runBlocking {
        val name = "backup-links-${UUID.randomUUID()}.db"; val db = BlockDatabase.open(context, name)
        val targetName = "backup-target-${UUID.randomUUID()}.db"; val target = BlockDatabase.open(context, targetName)
        val dir = File(context.cacheDir, UUID.randomUUID().toString())
        try {
            val block = BlockRepository(db).save(null, BlockDraft("Keep data", "Read"))
            val library = MediaLibraryRepository(db, dir); val asset = library.import("Excluded.png", null) { image().inputStream() }
            BlockMediaRepository(db, library, ImageReferenceRepository(db, File(dir, "old"))).link(block, MediaRole.VISUAL, asset.id)
            val bytes = BackupCodec.encode(BackupRepository(db).snapshot())
            assertFalse(String(bytes).contains(asset.id)); assertFalse(String(bytes).contains(asset.fileName))
            BackupRepository(target).restore(BackupCodec.decode(bytes))
            assertNotNull(target.blocks().find(block)); assertTrue(target.blockMedia().readAll().isEmpty()); assertTrue(target.media().readAll().isEmpty())
            rejected { BackupRepository(db).restore(BackupCodec.decode(bytes)) }
            assertEquals(asset.id, db.blockMedia().find(block, "VISUAL")!!.mediaId); assertTrue(library.available(asset))
        } finally { db.close(); target.close(); context.deleteDatabase(name); context.deleteDatabase(targetName); dir.deleteRecursively() }
    }

}
