package com.mrayover.practicelogger
import android.media.MediaPlayer
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File

/** Runs against immutable build76, before installing L3. Do not reference any L1 production classes here. */
class BlockMediaSeedTest {
    @Test fun seedBuild76Ownership(): Unit = runBlocking {
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        val db = BlockDatabase.open(app)
        try {
            assertEquals(13, db.openHelper.readableDatabase.version)
            val first = BlockRepository(db).save(null, BlockDraft("Legacy audio and PDF", "Keep original ownership"))
            val second = BlockRepository(db).save(null, BlockDraft("Legacy photo", "Keep original photo"))
            val repo = TakeRepository(db, app)
            val free = repo.prepare(first); free.file.writeBytes(l1Fixture("audio/backing.m4a")); repo.save(free)
            val phrase = repo.prepare(first, PhraseSpec(120, 4)); PhraseWave.write(phrase.file, ShortArray(PhraseSpec(120, 4).frames)); repo.save(phrase)
            for (extension in listOf("mp3", "m4a", "wav")) {
                val bytes = if (extension == "wav") phrase.file.let { repo.audioFile(db.takes().readAll().single { it.mode == "LOOP" }).readBytes() } else l1Fixture("audio/backing.$extension")
                repo.save(AudioImport(repo).prepare(first, "Legacy backing.$extension") { bytes.inputStream() })
            }
            val images = ImageReferenceRepository(db, File(app.filesDir, "block-images"))
            images.import(first) { l1Fixture("references/score.pdf").inputStream() }
            images.import(second) { l1Png().inputStream() }
            db.logs().save(DailyLog("2026-11-03", "L3 upgrade keeps data"))
            val sessions = SessionRepository(db); sessions.addBlock(first); sessions.start()
            val library = MediaLibraryRepository(db, File(app.filesDir, "media-library"))
            val canonical = library.import("Existing Library.wav", null) { l1Fixture("audio/backing.wav").inputStream() }
            MasterAudioMarkerRepository(db).save(canonical.id, null, 100, null, "Existing master", "Keep")
            val manifest = JSONObject().put("backup", BackupCodec.encode(BackupRepository(db).snapshot()).toString(Charsets.UTF_8))
            for (table in listOf("takes", "block_images", "media_assets", "media_markers")) {
                val rows = JSONArray()
                db.openHelper.readableDatabase.query("SELECT * FROM $table ORDER BY 1").use { cursor ->
                    while (cursor.moveToNext()) rows.put(JSONObject().apply { for (column in 0 until cursor.columnCount) put(cursor.getColumnName(column), if (cursor.isNull(column)) JSONObject.NULL else cursor.getString(column)) })
                }
                manifest.put(table, rows)
            }
            val hashes = JSONObject()
            for (dir in listOf("recordings", "block-images", "media-library")) File(app.filesDir, dir).listFiles()!!.forEach { hashes.put("$dir/${it.name}", l1Hash(it)) }
            manifest.put("hashes", hashes)
            File(app.filesDir, "l3-legacy-manifest.json").writeText(manifest.toString())
        } finally { db.close() }
    }
}

class BlockMediaUpgradeTest {
    @Test fun actualInstallOverRetainsLegacyBytesIdentitiesPlaybackExportAndBlockProtection(): Unit = runBlocking {
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        val manifest = JSONObject(File(app.filesDir, "l3-legacy-manifest.json").readText())
        val db = BlockDatabase.open(app)
        try {
            assertEquals(16, db.openHelper.readableDatabase.version)
            assertEquals(BackupCodec.decode(manifest.getString("backup").toByteArray()), BackupRepository(db).snapshot())
            assertTrue(db.blockMedia().readAll().isEmpty())
            for (table in listOf("takes", "block_images", "media_assets", "media_markers")) {
                val expected = manifest.getJSONArray(table)
                db.openHelper.readableDatabase.query("SELECT * FROM $table ORDER BY 1").use { cursor ->
                    assertEquals(expected.length(), cursor.count)
                    var row = 0
                    while (cursor.moveToNext()) {
                        for (column in 0 until cursor.columnCount) {
                            val key = cursor.getColumnName(column)
                            assertEquals("$table $row $key", if (expected.getJSONObject(row).isNull(key)) null else expected.getJSONObject(row).getString(key), cursor.getString(column))
                        }; row++
                    }
                }
            }
            val repo = TakeRepository(db, app); val images = ImageReferenceRepository(db, File(app.filesDir, "block-images"))
            assertEquals(0, repo.recover()); assertEquals(0, images.recover())
            assertEquals(0, MediaLibraryRepository(db, File(app.filesDir, "media-library")).recover())
            val hashes = manifest.getJSONObject("hashes")
            hashes.keys().forEach { path -> assertEquals(hashes.getString(path), l1Hash(File(app.filesDir, path))) }
            for (take in db.takes().readAll()) {
                val output = ByteArrayOutputStream(); repo.export(take, output)
                assertArrayEquals(repo.audioFile(take).readBytes(), output.toByteArray())
                if (take.kind == TakeKind.IMPORTED_LOOP) {
                    val player = MediaPlayer()
                    try {
                        player.setDataSource(repo.audioFile(take).absolutePath); player.isLooping = true; player.prepare(); player.start()
                        val until = android.os.SystemClock.elapsedRealtime() + 3000
                        while (player.currentPosition == 0 && android.os.SystemClock.elapsedRealtime() < until) Thread.sleep(25)
                        assertTrue("Legacy ${take.audioMime} playback did not progress", player.currentPosition > 0)
                    } finally { player.release() }
                    repo.rename(take.id, "Renamed ${take.id.take(8)}")
                    val changed = db.takes().find(take.id)!!
                    assertEquals(take.copy(label = changed.label), changed); assertEquals(take.blockId, changed.blockId)
                }
            }
            for (image in db.images().readAll()) {
                images.decode(image).recycle()
                assertTrue(BlockRepository(db).deletePermanently(image.blockId) is BlockDeletionResult.Blocked)
            }
            hashes.keys().forEach { path -> assertEquals(hashes.getString(path), l1Hash(File(app.filesDir, path))) }
        } finally { db.close() }
    }
}
