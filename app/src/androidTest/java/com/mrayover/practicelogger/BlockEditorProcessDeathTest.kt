package com.mrayover.practicelogger

import android.app.Application
import android.os.Process
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.UUID

/** Disposable-emulator-only process interruption between the first and second original promotions. */
class BlockEditorProcessDeathTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val app get() = instrumentation.targetContext.applicationContext as Application
    private val name = "editor-kill.db"
    private val tokenFile get() = File(app.filesDir, "editor-kill-token.txt")
    private fun editor(db: BlockDatabase): BlockEditorMedia {
        val library = MediaLibraryRepository(db, File(app.filesDir, "media-library"))
        val legacy = ImageReferenceRepository(db, File(app.filesDir, "block-images"))
        return BlockEditorMedia(app, db, library, legacy, BlockMediaRepository(db, library, legacy))
    }
    @Test fun dieAfterFirstOriginalPromotion(): Unit = runBlocking {
        assertEquals("true", InstrumentationRegistry.getArguments().getString("stage3b_emulator_only"))
        app.deleteDatabase(name)
        val db = BlockDatabase.open(app, name)
        val token = UUID.randomUUID().toString(); tokenFile.writeText(token)
        val draft = editor(db); draft.open(token, null)
        draft.stage(MediaRole.VISUAL, "One.pdf", "application/pdf") { l1Fixture("references/score.pdf").inputStream() }
        draft.stage(MediaRole.VISUAL, "Two.jpg", "image/jpeg") {
            val output = java.io.ByteArrayOutputStream()
            android.graphics.Bitmap.createBitmap(20, 20, android.graphics.Bitmap.Config.ARGB_8888).let { it.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, output); it.recycle() }
            output.toByteArray().inputStream()
        }
        draft.save(null, BlockDraft("Interrupted editor", "Retain all files"), null, true,
            onPromoted = { if (it == 1) Process.killProcess(Process.myPid()) })
        fail("Editor promotion did not terminate")
    }
    @Test fun recoverAllPendingItemsAndSaveExactlyOnce(): Unit = runBlocking {
        val db = BlockDatabase.open(app, name)
        val draft = editor(db)
        try {
            assertTrue(db.blocks().readAll().isEmpty()); assertTrue(db.media().readAll().isEmpty())
            draft.open(tokenFile.readText(), null)
            assertEquals(2, draft.visualReferences.size)
            val order = draft.visualReferences.map { it.identity }
            val page = draft.preview(draft.visualReferences.first().asset!!, 1); assertEquals(2, page.pageCount); page.bitmap.recycle()
            val saved = draft.save(null, BlockDraft("Interrupted editor", "Retain all files"), null, true)
            assertEquals(order, db.referenceOrder(saved, MediaRole.VISUAL))
            assertEquals(2, db.media().readAll().size)
            val replay = editor(db); replay.open(tokenFile.readText(), null)
            assertEquals(saved, replay.completedId); assertEquals(1, db.sessions().readAll().single().placements.size)
            replay.discard()
        } finally {
            db.media().readAll().forEach { File(app.filesDir, "media-library/${it.fileName}").delete() }
            draft.discard(); db.close(); app.deleteDatabase(name); tokenFile.delete()
        }
    }
}
