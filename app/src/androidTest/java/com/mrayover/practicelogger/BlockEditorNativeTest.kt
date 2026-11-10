package com.mrayover.practicelogger

import android.app.Application
import android.graphics.Bitmap
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID

class BlockEditorNativeTest {
    private val app get() = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as Application
    private fun jpeg() = ByteArrayOutputStream().use { output ->
        Bitmap.createBitmap(40, 30, Bitmap.Config.ARGB_8888).let { it.compress(Bitmap.CompressFormat.JPEG, 90, output); it.recycle() }
        output.toByteArray()
    }
    private fun wav(): ByteArray {
        val pcm = 16_000
        return ByteBuffer.allocate(44 + pcm).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray()); putInt(36 + pcm); put("WAVEfmt ".toByteArray()); putInt(16)
            putShort(1); putShort(1); putInt(8000); putInt(16000); putShort(2); putShort(16)
            put("data".toByteArray()); putInt(pcm); repeat(pcm / 2) { putShort(0) }
        }.array()
    }
    private suspend fun fixture(action: suspend (BlockDatabase, MediaLibraryRepository, ImageReferenceRepository, BlockMediaRepository) -> Unit) {
        val name = "editor-${UUID.randomUUID()}.db"
        val db = BlockDatabase.open(app, name)
        val library = MediaLibraryRepository(db, File(app.filesDir, "media-library"))
        val legacy = ImageReferenceRepository(db, File(app.filesDir, "block-images"))
        try { action(db, library, legacy, BlockMediaRepository(db, library, legacy)) }
        finally {
            db.media().readAll().forEach { File(app.filesDir, "media-library/${it.fileName}").delete() }
            db.images().readAll().forEach { File(app.filesDir, "block-images/${it.fileName}").delete() }
            db.close(); app.deleteDatabase(name)
        }
    }
    private suspend fun reject(action: suspend () -> Unit) {
        var rejected = false
        try { action() } catch (_: Exception) { rejected = true }
        assertTrue("Unsafe operation succeeded", rejected)
    }

    @Test fun stagedBothRolesFailureRetryRecoveryAndExactlyOnceBuilderCommit(): Unit = runBlocking { fixture { db, library, legacy, links ->
        fun editor() = BlockEditorMedia(app, db, library, legacy, links)
        val token = UUID.randomUUID().toString()
        var draft = editor(); draft.open(token, null)
        val before = File(app.filesDir, "media-library").list()?.toSet().orEmpty()
        draft.stage(MediaRole.VISUAL, "Score.jpg", "image/jpeg") { jpeg().inputStream() }
        draft.stage(MediaRole.AUDIO, "Example.wav", "audio/wav") { wav().inputStream() }
        val imageId = draft.visual!!.id; val audioId = draft.audio!!.id
        assertTrue(db.blocks().readAll().isEmpty()); assertTrue(db.media().readAll().isEmpty())
        assertEquals(before, File(app.filesDir, "media-library").list()?.toSet().orEmpty())
        reject { draft.save(null, BlockDraft(), null) }
        assertEquals(imageId, draft.visual!!.id)
        // Rebuild the editor owner to simulate process recreation from the synced manifest.
        draft = editor(); draft.open(token, null)
        assertEquals(imageId, draft.visual!!.id); assertEquals(audioId, draft.audio!!.id)
        val previous = BlockRepository(db).save(null, BlockDraft("Previous", "Keep"))
        SessionRepository(db).addBlock(previous)
        val session = db.sessions().withStatus("DRAFT")!!.id
        val placement = db.sessions().find(session)!!.ordered.single()
        SessionRepository(db).setDuration(placement.id, 123_000)
        val week = practiceWeek(java.time.LocalDate.of(2026, 11, 10))
        reject { draft.save(null, BlockDraft("One save", "Play"), week, true) { error("Injected database failure") } }
        assertEquals(1, db.blocks().readAll().size); assertTrue(db.media().readAll().isEmpty())
        assertTrue(db.blockMedia().readAll().isEmpty()); assertTrue(db.weekPlan().read(week.toString()).isEmpty())
        assertEquals(before, File(app.filesDir, "media-library").list()?.toSet().orEmpty())
        assertEquals(1, db.sessions().find(session)!!.placements.size)
        // Simulate a proven uncommitted promotion left after interruption/failed cleanup.
        File(app.filesDir, "media-library/${draft.visual!!.fileName}").writeText("partial")
        val saved = draft.save(null, BlockDraft("One save", "Play"), week, true)
        assertEquals(2, db.blockMedia().readAll().size); assertEquals(2, db.media().readAll().size)
        assertEquals(saved, db.weekPlan().read(week.toString()).single().blockId)
        val lineup = db.sessions().find(session)!!.ordered
        assertEquals(listOf(previous, saved), lineup.map { it.sourceBlockId }); assertEquals(123_000L, lineup[0].configuredMs)
        val recovered = editor(); recovered.open(token, null)
        assertEquals(saved, recovered.save(null, BlockDraft("One save", "Play"), week, true))
        assertEquals(2, db.sessions().find(session)!!.placements.size)
        assertTrue(library.available(db.media().find(imageId)!!)); assertTrue(library.available(db.media().find(audioId)!!))
        recovered.discard()
        db.openHelper.readableDatabase.query("PRAGMA foreign_key_check").use { assertEquals(0, it.count) }
    } }

    @Test fun existingSelectionCancellationUnchangedStateRemovalAndHistoricalSnapshots(): Unit = runBlocking { fixture { db, library, legacy, links ->
        val blocks = BlockRepository(db)
        val id = blocks.save(null, BlockDraft("Original", "Old action", notes = "Source"))
        val shared = blocks.save(null, BlockDraft("Shared", "Keep"))
        val visual = library.import("Shared.jpg", "image/jpeg") { jpeg().inputStream() }
        val audio = library.import("Shared.wav", "audio/wav") { wav().inputStream() }
        for (block in listOf(id, shared)) { links.link(block, MediaRole.VISUAL, visual.id); links.link(block, MediaRole.AUDIO, audio.id) }
        SessionRepository(db).addBlock(id)
        val session = db.sessions().withStatus("DRAFT")!!.id
        val snapshot = db.sessions().find(session)!!.ordered.single()
        val state = BlockAudioState(id, audio.id, 100, 75, 0, 500, true, null)
        db.blockAudio().save(state)
        val marker = BlockAudioMarker(id, audio.id, UUID.randomUUID().toString(), 100, null, "Local", "Keep", null)
        db.blockAudio().save(marker)
        val overlay = PracticeOverlay(UUID.randomUUID().toString(), id, "library:${visual.id}", 0, "COVER", .1f, .1f, .5f, .5f, 0, 0)
        db.overlays().insert(overlay)
        fun editor() = BlockEditorMedia(app, db, library, legacy, links)
        val cancelled = editor(); cancelled.open(UUID.randomUUID().toString(), id)
        cancelled.select(MediaRole.VISUAL, null); cancelled.select(MediaRole.AUDIO, null)
        cancelled.discard()
        assertEquals(visual.id, db.blockMedia().find(id, "VISUAL")!!.mediaId)
        assertEquals(state, db.blockAudio().state(id, audio.id))
        assertEquals(overlay, db.overlays().find(overlay.id))
        val edit = editor(); edit.open(UUID.randomUUID().toString(), id)
        assertEquals(id, edit.save(id, BlockDraft("Renamed", "New action", notes = "Source"), null))
        assertEquals(state, db.blockAudio().state(id, audio.id))
        assertEquals(overlay, db.overlays().find(overlay.id))
        assertEquals(marker, db.blockAudio().read(id, audio.id).single())
        assertEquals(snapshot, db.sessions().find(session)!!.ordered.single())
        val removal = editor(); removal.open(UUID.randomUUID().toString(), id)
        removal.select(MediaRole.VISUAL, null); removal.select(MediaRole.AUDIO, null)
        removal.save(id, BlockDraft("Renamed", "New action"), null)
        assertTrue(db.blockMedia().readAll().none { it.blockId == id })
        assertEquals(2, db.blockMedia().readAll().count { it.blockId == shared })
        assertTrue(library.available(visual)); assertTrue(library.available(audio))
        assertFalse(blocks.reviewDeletion(id).dependencies.canDelete)
    } }

    @Test fun photoPdfCancelUnsupportedInputAndInterruptedStagingCleanup(): Unit = runBlocking { fixture { db, library, legacy, links ->
        val token = UUID.randomUUID().toString()
        val editor = BlockEditorMedia(app, db, library, legacy, links); editor.open(token, null)
        val capture = ReferencePhotoCapture(app); val photo = capture.create(); capture.file(photo).writeBytes(jpeg())
        editor.photo(capture, photo)
        assertFalse(capture.file(photo).exists()); assertEquals("IMAGE", editor.visual!!.category)
        val imageId = editor.visual!!.id
        reject { editor.stage(MediaRole.VISUAL, "Wrong.pdf", "application/pdf") { "broken".byteInputStream() } }
        assertEquals(imageId, editor.visual!!.id)
        editor.stage(MediaRole.VISUAL, "Pages.pdf", "application/pdf") { l1Fixture("references/score.pdf").inputStream() }
        val page = editor.preview(editor.visual!!, 1); assertEquals(2, page.pageCount); page.bitmap.recycle()
        val root = File(app.filesDir, "block-editor-staging")
        val abandoned = File(root, UUID.randomUUID().toString()).apply { mkdirs(); File(this, "interrupted.pending").writeText("partial"); setLastModified(1) }
        val recovery = BlockEditorMedia(app, db, library, legacy, links); recovery.open(token, null)
        assertFalse(abandoned.exists()); assertEquals("PDF", recovery.visual!!.category)
        recovery.discard()
        assertFalse(File(root, token).exists()); assertTrue(db.media().readAll().isEmpty()); assertTrue(db.blocks().readAll().isEmpty())
    } }

    @Test fun individualRolesExistingLibrarySelectionsAndGoalCommit(): Unit = runBlocking { fixture { db, library, legacy, links ->
        val goal = GoalRepository(db).save(null, "Editor primary", "", 0, "")
        val visual = library.import("Select.jpg", "image/jpeg") { jpeg().inputStream() }
        val audio = library.import("Select.wav", "audio/wav") { wav().inputStream() }
        for (asset in listOf(visual, audio)) {
            val role = if (asset.category == "AUDIO") MediaRole.AUDIO else MediaRole.VISUAL
            val edit = BlockEditorMedia(app, db, library, legacy, links)
            edit.open(UUID.randomUUID().toString(), null); edit.choose(role, asset.id)
            assertEquals(0, db.blocks().readAll().count { it.name == role.name })
            val saved = edit.save(null, BlockDraft(role.name, "Play", primaryGoalId = goal, primaryGoalChanged = true), null)
            assertEquals(listOf(BlockMediaLink(saved, role.name, asset.id)), db.blockMedia().readAll().filter { it.blockId == saved })
            assertEquals(goal, db.goalAttribution().primaryGoalId(saved))
        }
        assertEquals(2, db.media().readAll().size)
    } }
}
