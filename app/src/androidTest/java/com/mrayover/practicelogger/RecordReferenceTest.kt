package com.mrayover.practicelogger

import android.Manifest
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class RecordReferenceTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val app get() = compose.activity.application
    private val db get() = BlockDatabase.appInstance(app)

    @Test fun standaloneKeepsOneCanonicalReferenceAndNoTake() {
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            .executeShellCommand("pm grant ${app.packageName} ${Manifest.permission.RECORD_AUDIO}")
        val existing = runBlocking { db.media().readAll().map { it.id }.toSet() }
        val takes = runBlocking { db.takes().readAll() }
        openHomePractice(compose)
        click("Transcribe")
        click("Record Reference")
        compose.waitUntil(15_000) {
            compose.onAllNodes(hasText("Start Recording") and isEnabled())
                .fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()
        }
        click("Start Recording")
        compose.waitUntil(15_000) {
            compose.onAllNodesWithText("Recording · 0 min 1 sec")
                .fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()
        }
        click("Stop and Review")
        compose.waitUntil(15_000) {
            compose.onAllNodesWithText("Review Reference").fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()
        }
        click("Keep")
        compose.waitUntil(25_000) { runBlocking { db.media().readAll().any { it.id !in existing } } }
        val asset = runBlocking { db.media().readAll().single { it.id !in existing } }
        compose.waitUntil(15_000) {
            compose.onAllNodesWithText(asset.title).fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()
        }
        assertEquals("audio/mp4", asset.mimeType)
        assertTrue(File(app.filesDir, "media-library/${asset.fileName}").length() > 0)
        assertEquals(takes, runBlocking { db.takes().readAll() })
    }

    @Test fun protectedBlockReplacementRollsBackNewAssetUntilConfirmed(): Unit = runBlocking {
        val directory = File(app.filesDir, "media-library")
        val library = MediaLibraryRepository(db, directory)
        val links = BlockMediaRepository(db, library, ImageReferenceRepository(db, File(app.filesDir, "block-images")))
        val block = BlockRepository(db).save(null, BlockDraft("Capture test", "Transcribe"))
        val previous = library.import("Previous reference.m4a", "audio/mp4") { l1Fixture("audio/backing.m4a").inputStream() }
        links.link(block, MediaRole.AUDIO, previous.id)
        val local = BlockAudioState(block, previous.id, 0, 75, 0, previous.durationMs!!, false, null)
        db.blockAudio().save(local)
        val fixture = File(app.cacheDir, "record-reference-test.m4a")
        try {
            fixture.writeBytes(l1Fixture("audio/backing.m4a"))
            val before = db.media().readAll()
            val files = directory.list()!!.toSet()
            try {
                library.importRecordedReference("New reference.m4a", fixture) {
                    links.attachRecordedAudio(block, it.id, previous.id, null)
                }
                fail("Replacement requires confirmation")
            } catch (loss: ConfirmAudioLoss) { assertEquals(previous.id, loss.mediaId) }
            assertEquals(before, db.media().readAll())
            assertEquals(files, directory.list()!!.toSet())
            assertEquals(local, db.blockAudio().state(block, previous.id))
            val kept = library.importRecordedReference("New reference.m4a", fixture) {
                links.attachRecordedAudio(block, it.id, previous.id, previous.id)
            }
            assertEquals(kept.id, db.blockMedia().find(block, MediaRole.AUDIO.name)!!.mediaId)
            assertNull(db.blockAudio().state(block, previous.id))
            assertTrue(library.available(previous))
            assertArrayEquals(fixture.readBytes(), File(directory, kept.fileName).readBytes())
        } finally { fixture.delete() }
    }

    private fun click(label: String) = compose.onNodeWithText(label).scrollIfNeeded().performClick()
}
