package com.mrayover.practicelogger

import android.content.Context
import androidx.room.withTransaction
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/** Real Android WAL, physical originals and separate empty database/file roots. No owner installation. */
class MultiReferenceNativeTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private class Interrupted : Error("Injected process-interruption boundary")
    private fun fixture(test: suspend (String, File, MultiReferenceLegacyFixture) -> Unit) = runBlocking {
        val name = "multi-native-${UUID.randomUUID()}.db"
        val files = File(context.filesDir, name)
        try {
            val frozen = MultiReferenceLegacyFixture(context, name, files) { instrumentation.context.assets.open(it) }
            frozen.create(); test(name, files, frozen)
        } finally { context.deleteDatabase(name); files.deleteRecursively() }
    }
    @Test fun populatedUpgradeAndColdReopenUseProductionWal() = fixture { name, _, frozen ->
        repeat(2) {
            val db = BlockDatabase.open(context, name)
            try {
                val sql = db.openHelper.writableDatabase
                assertEquals(24, sql.version)
                sql.query("PRAGMA journal_mode").use { c -> assertTrue(c.moveToFirst()); assertEquals("wal", c.getString(0)) }
                frozen.assertPreserved(sql)
                assertEquals(0, db.blockMedia().readAll().maxOf { link -> link.position })
                sql.query("PRAGMA foreign_key_list(block_audio_state)").use { c ->
                    assertEquals(3, c.count); while(c.moveToNext()) assertEquals("block_media_links", c.getString(2))
                }
            } finally { db.close() }
        }
    }
    @Test fun zip8CollectionsRoundTripRecoveryAndHistoricalArchives() = fixture { name, files, frozen ->
        val db = BlockDatabase.open(context, name)
        val targetNames = mutableListOf<String>()
        try {
            frozen.assertPreserved(db.openHelper.writableDatabase)
            // Frozen23's LOOP original has an 8kHz import header; prepare a valid 48kHz saved loop
            // for media-decoder acceptance, after independently proving migration byte preservation.
            val loop = File(files, "recordings/00000000-0000-4000-8000-000000000005.wav")
            val samples = 192000
            loop.writeBytes(ByteBuffer.allocate(44 + samples * 2).order(ByteOrder.LITTLE_ENDIAN).apply {
                put("RIFF".toByteArray()); putInt(36 + samples * 2); put("WAVEfmt ".toByteArray()); putInt(16)
                putShort(1); putShort(1); putInt(48000); putInt(96000); putShort(2); putShort(16)
                put("data".toByteArray()); putInt(samples * 2)
            }.array())
            val library = MediaLibraryRepository(db, File(files, "media-library"))
            val legacy = ImageReferenceRepository(db, File(files, "block-images"))
            val refs = BlockMediaRepository(db, library, legacy)
            val audioId = "00000000-0000-4000-8000-000000000003"
            val imageId = "00000000-0000-4000-8000-000000000001"
            val pdfId = "00000000-0000-4000-8000-000000000002"
            val oldArchive = File(files, "single.zip")
            PortableZip(context, db, files).create(oldArchive)
            // Historical versions use frozen columns and their original single-role restrictions.
            for (version in 1..7) {
                val archive = historical(oldArchive, version, files)
                val targetName = "$name-v$version".also(targetNames::add)
                val targetFiles = File(files, "historical-$version")
                val target = BlockDatabase.open(context, targetName)
                try {
                    val restore = PortableRestore(context, target, targetFiles)
                    val staged = restore.stage(archive::inputStream)
                    assertEquals(5, staged.verified.originals.size)
                    restore.restore(staged)
                    assertEquals(db.blockMedia().readAll().map { it.copy(position = 0) }, target.blockMedia().readAll())
                    assertEquals(db.blockAudio().read("image", audioId), target.blockAudio().read("image", audioId))
                    for (original in staged.verified.originals) assertArrayEquals(original.path,
                        original.file(files).readBytes(), original.file(targetFiles).readBytes())
                } finally { target.close() }
                // Malformed old archives cannot exploit Room24's wider key.
                val malformed = mutateRecords(archive, File(files, "bad-old-$version.zip")) { records ->
                    val rows = records.getJSONObject("tables").getJSONArray("block_media_links")
                    rows.put(JSONArray().put("image").put("VISUAL").put(pdfId))
                }
                assertTrue(runCatching { PortableZip(context, db, files).verify(malformed) }.isFailure)
            }
            refs.add("image", MediaRole.VISUAL, pdfId)
            refs.add("legacy", MediaRole.VISUAL, imageId)
            val second = db.media().find(audioId)!!.copy(id = UUID.randomUUID().toString(), fileName = "${UUID.randomUUID()}.wav")
            File(files, "media-library/$audioId.wav").copyTo(File(files, "media-library/${second.fileName}")); db.media().insert(second)
            refs.add("image", MediaRole.AUDIO, second.id)
            BlockAudioRepository(db).saveState(db.blockAudio().state("image", audioId)!!.copy(mediaId = second.id, positionMs = 1600, speedPercent = 65))
            val audioOrder = refs.ordered("image", MediaRole.AUDIO)
            refs.reorder("image", MediaRole.AUDIO, audioOrder, audioOrder.reversed())
            val visualOrder = refs.ordered("image", MediaRole.VISUAL)
            refs.reorder("image", MediaRole.VISUAL, visualOrder, visualOrder.reversed())
            val archive = File(files, "multi.zip")
            val verified = PortableZip(context, db, files).create(archive)
            assertEquals(6, verified.originals.size)
            assertEquals(8, verified.records["block_media_links"].size)
            for ((label, mutation) in listOf<Pair<String, (JSONObject) -> Unit>>(
                "duplicate" to { records -> val rows = records.getJSONObject("tables").getJSONArray("block_media_links"); rows.put(rows.getJSONArray(0)) },
                "negative" to { it.getJSONObject("tables").getJSONArray("block_media_links").getJSONArray(0).put(3, -1) },
                "gap" to { it.getJSONObject("tables").getJSONArray("block_media_links").getJSONArray(0).put(3, 99) },
                "fraction" to { it.getJSONObject("tables").getJSONArray("block_media_links").getJSONArray(0).put(3, .5) },
                "wrong-role" to { it.getJSONObject("tables").getJSONArray("block_media_links").getJSONArray(0).put(1, "INVALID") },
                "missing-asset" to { it.getJSONObject("tables").getJSONArray("block_media_links").getJSONArray(0).put(2, "missing") }
            )) assertTrue(label, runCatching { PortableZip(context, db, files).verify(mutateRecords(archive, File(files, "$label.zip"), change = mutation)) }.isFailure)
            val expected = portableTables.associate { table -> table.name to table.snapshot(db.openHelper.writableDatabase) {}.toString() }
            for (boundary in listOf(RestoreBoundary.BEFORE_COMMIT, RestoreBoundary.PROMOTED, RestoreBoundary.COMMITTED)) {
                val targetName = "$name-${boundary.name}".also(targetNames::add)
                val targetFiles = File(files, boundary.name)
                var target = BlockDatabase.open(context, targetName)
                val restore = PortableRestore(context, target, targetFiles) { if (it == boundary) {
                    if (boundary == RestoreBoundary.BEFORE_COMMIT) error("Injected failed commit") else throw Interrupted()
                } }
                val staged = restore.stage(archive::inputStream)
                assertEquals(verified.records.counts.toString(), staged.verified.records.counts.toString()) // preview
                assertTrue(runCatching { restore.restore(staged) }.isFailure)
                target.close(); target = BlockDatabase.open(context, targetName)
                try {
                    val recovery = PortableRestore(context, target, targetFiles)
                    recovery.recover()
                    if (boundary != RestoreBoundary.COMMITTED) {
                        assertTrue(portableTargetEmpty(target.openHelper.writableDatabase))
                        assertTrue(File(targetFiles, "media-library").list().orEmpty().isEmpty())
                        recovery.restore(recovery.stage(archive::inputStream))
                    }
                    assertFalse(recovery.recover())
                } finally { target.close() }
                target = BlockDatabase.open(context, targetName)
                try {
                    for (table in portableTables) assertEquals(table.name, expected[table.name], table.snapshot(target.openHelper.writableDatabase) {}.toString())
                    for (original in verified.originals) assertArrayEquals(original.path, original.file(files).readBytes(), original.file(targetFiles).readBytes())
                    assertTrue(runCatching { PortableRestore(context, target, targetFiles).stage(archive::inputStream) }.exceptionOrNull() is NonEmptyRestoreException)
                } finally { target.close() }
            }
        } finally { db.close(); targetNames.forEach(context::deleteDatabase) }
    }

    private suspend fun historical(source: File, version: Int, files: File): File {
        val archive = mutateRecords(source, File(files, "frozen-v$version.zip"), version) { records ->
            val tables = records.getJSONObject("tables")
            val images = tables.getJSONArray("block_images"); repeat(images.length()) { images.getJSONArray(it).remove(6) }
            val links = tables.getJSONArray("block_media_links"); repeat(links.length()) { links.getJSONArray(it).remove(3) }
            if (version < 7) tables.remove(INSPIRATION_STORE)
            if (version < 6) listOf("block_completions", "block_reflections", "session_reflections").forEach(tables::remove)
            if (version < 5) { val placements = tables.getJSONArray("placements"); repeat(placements.length()) { placements.getJSONArray(it).remove(14) } }
            if (version < 4) tables.remove("week_blocks")
            if (version < 3) listOf("block_primary_goals", "placement_goal_snapshots").forEach(tables::remove)
            if (version < 2) tables.remove("practice_overlays")
        }
        val exported = File(context.getExternalFilesDir(null), "stage3b-fixtures").apply { mkdirs() }
        archive.copyTo(File(exported, "portable-v$version.zip"), overwrite = true)
        return archive
    }
    private suspend fun mutateRecords(source: File, destination: File, version: Int = 8, change: (JSONObject) -> Unit): File {
        val entries = ZipFile(source).use { zip -> zip.entries().asSequence().associate { it.name to zip.getInputStream(it).use { input -> input.readBytes() } }.toMutableMap() }
        val records = JSONObject(entries.getValue("records.json").toString(Charsets.UTF_8))
        if (version == 1) records.remove("version") else records.put("version", version)
        change(records)
        entries["records.json"] = records.toString().toByteArray()
        val manifest = JSONObject(entries.getValue("manifest.json").toString(Charsets.UTF_8))
        manifest.put("version", version).put("roomSchema", if (version == 8) 24 else 16 + version)
        val counts = JSONObject(); val tables = records.getJSONObject("tables")
        tables.keys().forEach { counts.put(it, tables.getJSONArray(it).length()) }; manifest.put("counts", counts)
        val inventory = manifest.getJSONArray("entries")
        repeat(inventory.length()) { i -> val item = inventory.getJSONObject(i)
            if (item.getString("path") == "records.json") {
                val checksum = entries.getValue("records.json").inputStream().use { copyPortable(it, null, 16L * 1024 * 1024) }
                item.put("bytes", checksum.bytes).put("sha256", checksum.sha256)
            }
        }
        entries["manifest.json"] = manifest.toString().toByteArray()
        ZipOutputStream(destination.outputStream()).use { zip -> entries.forEach { (path, bytes) -> zip.putNextEntry(ZipEntry(path)); zip.write(bytes); zip.closeEntry() } }
        return destination
    }
}
