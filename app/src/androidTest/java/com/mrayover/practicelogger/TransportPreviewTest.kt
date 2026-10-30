package com.mrayover.practicelogger

import android.Manifest
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

/** Assignment 1 only: one owner-preview transport path, not a release regression suite. */
class TransportPreviewTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val device get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private fun shown(text: String) { compose.waitUntil(15000) {
        compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
    } }
    private fun click(text: String) { shown(text); compose.onNodeWithText(text).scrollIfNeeded().assertIsDisplayed().performClick() }
    private fun tagged(tag: String) = compose.onNodeWithTag(tag).scrollIfNeeded().assertIsDisplayed()
    private fun clearKeyboard() {
        compose.runOnIdle {
            val view = compose.activity.window.decorView
            (compose.activity.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager)
                .hideSoftInputFromWindow(view.windowToken, 0)
            view.clearFocus()
        }
        compose.waitForIdle()
    }
    private fun select(type: String, id: String) {
        tagged("practice-$type-selector").performClick()
        tagged("practice-select-$id").performClick()
    }
    @Test fun pinnedWorkspacePanelsNotesAndOlderTakes() {
        val app = compose.activity.application
        val db = BlockDatabase.open(app)
        val practice = ViewModelProvider(compose.activity)[PracticeViewModel::class.java]
        val recordings = ViewModelProvider(compose.activity)[RecordingViewModel::class.java]
        try {
            device.executeShellCommand("pm grant ${app.packageName} ${Manifest.permission.RECORD_AUDIO}")
            val session = runBlocking {
                val id = BlockRepository(db).save(null, BlockDraft("Transport preview", "Listen and play slowly. ".repeat(30), "5", "120"))
                ImageReferenceRepository(db, File(app.filesDir, "block-images")).import(id) {
                    InstrumentationRegistry.getInstrumentation().context.assets.open("references/score.pdf")
                }
                val repo = SessionRepository(db); repo.addBlock(id); repo.start()
            }
            shown("Practice Now"); compose.runOnIdle { practice.openSession(session) }
            shown("Practice Description")
            val placement = practice.catalog.value.active!!.current!!.id
            val block = practice.catalog.value.active!!.current!!.sourceBlockId
            compose.waitUntil(10000) { recordings.ready }
            compose.onNodeWithText("Recordings").assertDoesNotExist()
            click("Start"); click("Start click"); click("Expand")
            compose.waitUntil(15000) { compose.onAllNodesWithTag("reference-viewer").fetchSemanticsNodes().isNotEmpty() }
            tagged("practice-free-record"); tagged("practice-loop-record")
            val fullHeight = compose.onNodeWithTag("practice-workspace-viewport").fetchSemanticsNode().boundsInRoot.height
            click("Notes")
            compose.onNodeWithTag("practice-notes-panel").assertExists()
            assertTrue(compose.onNodeWithTag("practice-workspace-viewport").fetchSemanticsNode().boundsInRoot.height < fullHeight)
            compose.runOnIdle { assertTrue(practice.running); assertTrue(practice.clicking) }
            click("Record Loop")
            compose.onNodeWithTag("practice-notes-panel").assertDoesNotExist()
            compose.onNodeWithTag("practice-loop-panel").assertExists()
            compose.runOnIdle { assertTrue(practice.running); assertTrue(practice.clicking) }
            click("Cancel")
            repeat(2) { index ->
                tagged("practice-free-record").performClick()
                compose.waitUntil(10000) { recordings.recording && recordings.elapsedMs >= 1300 }
                if (index == 0) {
                    click("Notes")
                    compose.onNodeWithText("Quick note (optional)").performTextReplacement("Saved during free capture")
                    clearKeyboard()
                    click("Save and return")
                    compose.waitUntil(10000) { compose.onAllNodesWithTag("practice-notes-panel").fetchSemanticsNodes().isEmpty() }
                    compose.runOnIdle { assertTrue(recordings.recording); assertTrue(practice.clicking) }
                }
                tagged("practice-free-record").performClick()
                compose.waitUntil(15000) { !recordings.busy && recordings.takes.value.count { it.blockId == block && it.mode == "FREE" } == index + 1 }
            }
            val free = recordings.takes.value.filter { it.blockId == block && it.mode == "FREE" }.sortedBy { it.createdAt }
            select("free", free.last().id); select("free", free.first().id)
            compose.onNodeWithContentDescription("Play selected free recording").scrollIfNeeded().performClick()
            compose.waitUntil(10000) { recordings.playingId == free.first().id }
            compose.onNodeWithContentDescription("Stop selected free recording").scrollIfNeeded().performClick()
            click("Notes"); compose.onNodeWithText("Saved during free capture").assertExists(); click("Notes")
            repeat(2) { index ->
                click("Record Loop")
                compose.onNodeWithText("Phrase BPM").scrollIfNeeded().performTextReplacement("240")
                clearKeyboard(); click("4 beats")
                // The inline Record button is distinct from the compact free-record control.
                compose.onAllNodesWithText("Record").onFirst().scrollIfNeeded().assertIsDisplayed().performClick()
                compose.waitUntil(10000) { recordings.recording }
                compose.onNodeWithTag("practice-loop-panel").assertDoesNotExist()
                tagged("practice-recording-stop")
                compose.waitUntil(15000) { !recordings.recording && recordings.takes.value.count { it.blockId == block && it.mode == "LOOP" } == index + 1 }
                compose.waitUntil(10000) { recordings.playingId != null }
                compose.onNodeWithContentDescription("Stop selected loop recording").scrollIfNeeded().performClick()
            }
            val loops = recordings.takes.value.filter { it.blockId == block && it.mode == "LOOP" }.sortedBy { it.createdAt }
            select("loop", loops.last().id); select("loop", loops.first().id)
            compose.onNodeWithContentDescription("Play selected loop recording").scrollIfNeeded().performClick()
            compose.waitUntil(10000) { recordings.playingId == loops.first().id }
            click("Notes")
            compose.runOnIdle { assertEquals(loops.first().id, recordings.playingId); assertEquals(placement, practice.catalog.value.active!!.current!!.id) }
            click("Notes")
            compose.onNodeWithContentDescription("Stop selected loop recording").scrollIfNeeded().performClick()
            assertEquals(4, runBlocking { db.takes().readAll().count { it.blockId == block } })
            assertEquals("Saved during free capture", practice.catalog.value.active!!.current!!.note)
            tagged("practice-bottom-actions")
        } finally {
            val dir = File(app.getExternalFilesDir(null), "transport-preview"); dir.mkdirs()
            device.takeScreenshot(File(dir, "transport-final.png"))
            compose.runOnIdle { recordings.stopPlayback(); recordings.finishCapture(); practice.stopClick() }
            db.close()
        }
    }
}
