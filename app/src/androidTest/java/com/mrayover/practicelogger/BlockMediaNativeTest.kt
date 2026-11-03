package com.mrayover.practicelogger

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class BlockMediaNativeTest {
    @Test fun sharedPdfAndAudioKeepOriginalsAndMasterMarkersAcrossDeletionRefusal(): Unit = runBlocking {
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        val db = BlockDatabase.open(app)
        val library = MediaLibraryRepository(db, File(app.filesDir, "media-library"))
        val legacy = ImageReferenceRepository(db, File(app.filesDir, "block-images"))
        val links = BlockMediaRepository(db, library, legacy)
        try {
            val a = BlockRepository(db).save(null, BlockDraft("L3 shared A", "Play"))
            val b = BlockRepository(db).save(null, BlockDraft("L3 shared B", "Play"))
            val pdf = library.import("Shared.pdf", null) { l1Fixture("references/score.pdf").inputStream() }
            val audio = library.import("Shared.mp3", null) { l1Fixture("audio/backing.mp3").inputStream() }
            val originals = listOf(pdf, audio).associate { it.id to l1Hash(File(app.filesDir, "media-library/${it.fileName}")) }
            val files = File(app.filesDir, "media-library").list()!!.toSet()
            for (id in listOf(a, b)) { links.link(id, MediaRole.VISUAL, pdf.id); links.link(id, MediaRole.AUDIO, audio.id) }
            assertEquals(files, File(app.filesDir, "media-library").list()!!.toSet())
            assertEquals(2, db.blocks().dependencies(a).mediaLinks)
            val marker = MasterAudioMarkerRepository(db).save(audio.id, null, 100, null, "Preserve", "Master")
            for (asset in listOf(pdf, audio)) {
                try { library.delete(asset.id); fail("Deleted referenced original") } catch (_: IllegalStateException) { }
                assertEquals(originals[asset.id], l1Hash(File(app.filesDir, "media-library/${asset.fileName}")))
            }
            assertNotNull(db.markers().find(marker.id))
            library.rename(pdf.id, "Renamed shared score"); library.rename(audio.id, "Renamed shared audio")
            for (id in listOf(a, b)) {
                assertEquals(pdf.id, db.blockMedia().find(id, "VISUAL")!!.mediaId)
                assertEquals(audio.id, db.blockMedia().find(id, "AUDIO")!!.mediaId)
            }
            val page = library.visualPage(pdf.id, 1); assertEquals(2, page.pageCount); page.bitmap.recycle()
            links.unlink(a, MediaRole.VISUAL); links.unlink(a, MediaRole.AUDIO)
            assertEquals(2, db.blockMedia().readAll().count { it.blockId == b })
            assertTrue(BlockRepository(db).deletePermanently(b) is BlockDeletionResult.Blocked)
            links.unlink(b, MediaRole.VISUAL); links.unlink(b, MediaRole.AUDIO)
            assertTrue(File(app.filesDir, "media-library/${audio.fileName}").exists())
            library.delete(audio.id); library.delete(pdf.id); assertNull(db.markers().find(marker.id))
            db.openHelper.readableDatabase.query("PRAGMA foreign_key_check").use { assertEquals(0, it.count) }
        } finally { db.close() }
    }
}
