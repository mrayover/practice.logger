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
import java.io.File
import java.io.OutputStream
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RecordingRepositoryTest {
    private val context get() = ApplicationProvider.getApplicationContext<android.content.Context>()
    // File-management tests supply a probe; native M4A validation/capture/playback run in device tests.
    private fun repo(db: BlockDatabase, directory: File) = TakeRepository(db, directory) { file -> if (file.length() > 0) 1234 else 0 }
    @Test fun multipleTakesRemainWithStableBlockAcrossArchiveReopenAndJsonRestore(): Unit = runBlocking {
        val name = "recordings-persistence.db"; context.deleteDatabase(name)
        val directory = File(context.cacheDir, UUID.randomUUID().toString())
        var db = BlockDatabase.open(context, name)
        val target = Room.inMemoryDatabaseBuilder(context, BlockDatabase::class.java).build()
        try {
            val block = BlockRepository(db).save(null, BlockDraft("Original", "Play"))
            var repository = repo(db, directory); repository.recover()
            val first = repository.prepare(block).also { it.file.writeBytes(byteArrayOf(1, 2, 3)) }.let { repository.save(it) }
            val second = repository.prepare(block).also { it.file.writeBytes(byteArrayOf(4, 5, 6)) }.let { repository.save(it) }
            assertNotEquals(first.id, second.id); assertNotEquals(first.fileName, second.fileName)
            repository.rename(first.id, " Keep this take ")
            BlockRepository(db).save(block, BlockDraft("Renamed", "Changed")); BlockRepository(db).setArchived(block, true)
            db.close(); db = BlockDatabase.open(context, name); repository = repo(db, directory); repository.recover()
            val takes = db.takes().readAll()
            assertEquals(2, takes.size); assertTrue(takes.all { it.blockId == block && it.mode == "FREE" && it.durationMs == 1234L })
            assertEquals("Keep this take", takes.single { it.id == first.id }.label)
            assertArrayEquals(byteArrayOf(1, 2, 3), repository.audioFile(first).readBytes())
            assertArrayEquals(byteArrayOf(4, 5, 6), repository.audioFile(second).readBytes())
            val backup = BackupCodec.encode(BackupRepository(db).snapshot())
            assertFalse(String(backup).contains(first.fileName)); assertFalse(String(backup).contains("Keep this take"))
            BackupRepository(target).restore(BackupCodec.decode(backup))
            assertEquals(0, target.takes().readAll().size); assertTrue(target.blocks().find(block)!!.archived)
            assertEquals(2, db.takes().readAll().size)
        } finally { db.close(); target.close(); context.deleteDatabase(name); directory.deleteRecursively() }
    }
    @Test fun interruptedFilesFailedSavesAndFailedExportsNeverDestroyEarlierTakes(): Unit = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, BlockDatabase::class.java).build()
        val directory = File(context.cacheDir, UUID.randomUUID().toString())
        try {
            val block = BlockRepository(db).save(null, BlockDraft("Block", "Play"))
            val repository = repo(db, directory); repository.recover()
            val saved = repository.prepare(block).also { it.file.writeBytes(byteArrayOf(1, 2, 3)) }.let { repository.save(it) }
            val interrupted = File(directory, UUID.randomUUID().toString() + ".pending.m4a").also { it.writeText("partial") }
            val orphan = File(directory, UUID.randomUUID().toString() + ".m4a").also { it.writeText("uncommitted") }
            val unrelated = File(directory, "unrelated.txt").also { it.writeText("keep") }
            val active = repository.prepare(block).also { it.file.writeText("currently capturing") }
            assertEquals(2, repository.recover()); assertFalse(interrupted.exists()); assertFalse(orphan.exists()); assertTrue(unrelated.exists())
            assertTrue(active.file.exists()); repository.abandon(active)
            val empty = repository.prepare(block)
            try { repository.save(empty); fail("Empty take saved") } catch (_: IllegalArgumentException) { repository.abandon(empty) }
            val wrongBlock = repository.prepare("missing").also { it.file.writeText("audio") }
            try { repository.save(wrongBlock); fail("Missing Block saved") } catch (_: IllegalStateException) { }
            assertFalse(File(directory, wrongBlock.id + ".m4a").exists())
            try { repository.export(saved, object : OutputStream() { override fun write(b: Int) { throw java.io.IOException("Provider failed") } }); fail("Export succeeded") }
            catch (_: java.io.IOException) { }
            assertArrayEquals(byteArrayOf(1, 2, 3), repository.audioFile(saved).readBytes())
            val output = java.io.ByteArrayOutputStream(); repository.export(saved, output)
            assertArrayEquals(byteArrayOf(1, 2, 3), output.toByteArray())
            try { repository.audioFile(saved.copy(fileName = "../outside.m4a")); fail("Unsafe path allowed") } catch (_: IllegalArgumentException) { }
            assertEquals(listOf(saved), db.takes().readAll())
        } finally { db.close(); directory.deleteRecursively() }
    }
    @Test fun confirmedDeleteAndRenameAffectOnlyChosenTake(): Unit = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, BlockDatabase::class.java).build()
        val directory = File(context.cacheDir, UUID.randomUUID().toString())
        try {
            val block = BlockRepository(db).save(null, BlockDraft("Block", "Play"))
            val repository = repo(db, directory); repository.recover()
            val a = repository.prepare(block).also { it.file.writeText("first") }.let { repository.save(it) }
            val b = repository.prepare(block).also { it.file.writeText("second") }.let { repository.save(it) }
            try { repository.rename(a.id, "a".repeat(121)); fail("Long label accepted") } catch (_: IllegalArgumentException) { }
            repository.rename(b.id, "Second")
            val original = db.takes().find(b.id)!!
            repository.delete(a.id)
            assertNull(db.takes().find(a.id)); assertFalse(File(directory, a.fileName).exists())
            assertEquals(original, db.takes().find(b.id)); assertEquals("second", repository.audioFile(b).readText())
        } finally { db.close(); directory.deleteRecursively() }
    }
    @Test fun schemaThreeMigrationPreservesPracticeAndStartsWithNoTakes(): Unit = runBlocking {
        val name = "recording-migration.db"; context.deleteDatabase(name)
        val schema = javaClass.classLoader!!.getResourceAsStream("com.mrayover.practicelogger.BlockDatabase/3.json")!!.bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
        val file = context.getDatabasePath(name); file.parentFile!!.mkdirs()
        val sql = android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(file, null)
        val entities = schema.getJSONArray("entities")
        repeat(entities.length()) { i ->
            val e = entities.getJSONObject(i); val table = e.getString("tableName")
            sql.execSQL(e.getString("createSql").replace("\${TABLE_NAME}", table))
            val indices = e.optJSONArray("indices") ?: org.json.JSONArray()
            repeat(indices.length()) { sql.execSQL(indices.getJSONObject(it).getString("createSql").replace("\${TABLE_NAME}", table)) }
        }
        sql.execSQL("INSERT INTO blocks VALUES ('b','Legacy','Play',5,90,'Reference',1,1,2)")
        sql.execSQL("INSERT INTO sessions VALUES ('s',1,NULL,'IN_PROGRESS',0,'Session note')")
        sql.execSQL("INSERT INTO placements VALUES ('p','s','b','Legacy','Play',90,'Reference',0,300000,1234,'PENDING',0,'Quick note',126)")
        val setup = schema.getJSONArray("setupQueries"); repeat(setup.length()) { sql.execSQL(setup.getString(it)) }
        sql.version = 3; sql.close()
        val db = BlockDatabase.open(context, name)
        try {
            val bundle = db.sessions().find("s")!!
            assertEquals(1234L, bundle.activeMs); assertEquals(126, bundle.current!!.lastUsedBpm)
            assertEquals("Quick note", bundle.current!!.note); assertEquals("Session note", bundle.session.note)
            assertTrue(db.blocks().find("b")!!.archived); assertTrue(db.takes().readAll().isEmpty())
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
