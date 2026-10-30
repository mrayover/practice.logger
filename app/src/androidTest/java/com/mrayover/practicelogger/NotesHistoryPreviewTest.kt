package com.mrayover.practicelogger

import android.Manifest
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class NotesHistoryPreviewTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val device get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private fun shown(text: String) { compose.waitUntil(15000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() } }
    private fun click(text: String) { shown(text); compose.onNodeWithText(text).scrollIfNeeded().assertIsDisplayed().performClick() }
    private fun tag(name: String) = compose.onNodeWithTag(name).scrollIfNeeded().assertIsDisplayed()
    private fun clearKeyboard() = compose.runOnIdle {
        compose.activity.currentFocus?.clearFocus()
        compose.activity.getSystemService(android.view.inputmethod.InputMethodManager::class.java)
            .hideSoftInputFromWindow(compose.activity.window.decorView.windowToken, 0)
    }
    private fun screenshot(name: String) {
        compose.waitForIdle(); val dir = File(compose.activity.getExternalFilesDir(null), "notes-preview");dir.mkdirs()
        device.takeScreenshot(File(dir, "$name.png"))
    }
    private fun collapsedHeaders() {
        compose.onNodeWithTag("practice-description-toggle").assert(SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.StateDescription, "Collapsed"))
        compose.onNodeWithTag("practice-reference-toggle").assert(SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.StateDescription, "Collapsed"))
        compose.onNodeWithTag("practice-description").assertDoesNotExist()
    }
    @Test fun appendEditHistoryAndTemporaryWorkspaceCollapse() {
        val app = compose.activity.application
        val db = BlockDatabase.open(app)
        val practice = ViewModelProvider(compose.activity)[PracticeViewModel::class.java]
        val audio = ViewModelProvider(compose.activity)[RecordingViewModel::class.java]
        val notes = ViewModelProvider(compose.activity)[BlockNotesViewModel::class.java]
        try {
            device.executeShellCommand("pm grant ${app.packageName} ${Manifest.permission.RECORD_AUDIO}")
            val (block, session) = runBlocking {
                val b = BlockRepository(db).save(null, BlockDraft("Note history preview", "Practice slowly", "5", "120"))
                ImageReferenceRepository(db, File(app.filesDir,"block-images")).import(b) { InstrumentationRegistry.getInstrumentation().context.assets.open("references/score.pdf") }
                val repo = SessionRepository(db);repo.addBlock(b);val prior = repo.start()
                repo.saveBlockNote(prior, db.sessions().find(prior)!!.current!!.id, "Legacy earlier session note")
                repo.addBlock(b);val current = repo.start()
                val takeRepo = TakeRepository(db, app);val spec = PhraseSpec(240, 4);val pending = takeRepo.prepare(b, spec)
                PhraseWave.write(pending.file, ShortArray(spec.frames)); takeRepo.save(pending)
                b to current
            }
            shown("Practice Now");compose.runOnIdle { practice.openSession(session) }
            shown("Practice Description")
            val placement = practice.catalog.value.active!!.current!!.id
            compose.waitUntil(15000) { audio.ready && compose.onAllNodesWithTag("reference-viewer").fetchSemanticsNodes().isNotEmpty() }
            click("Next page");shown("Page 2 of 2")
            tag("reference-viewer").performTouchInput {
                pinch(Offset(width*.4f,height*.4f),Offset(width*.6f,height*.6f),Offset(width*.15f,height*.15f),Offset(width*.85f,height*.85f))
            }
            shown("Reset PDF zoom")
            click("Start");click("Start click");click("Notes");collapsedHeaders()
            compose.runOnIdle { assertTrue(practice.running);assertTrue(practice.clicking) }
            compose.waitUntil(10000) { notes.history.value.any { it.blockId == block && it.legacy } }
            tag("practice-new-note").performClick().performTextReplacement("First new note")
            // Save remains pinned outside the scrolling editor/history, including with IME open.
            compose.onNodeWithText("Save Note").assertIsDisplayed().performClick()
            compose.waitUntil(10000) { notes.history.value.count { it.blockId == block } == 2 && notes.draft(placement).value.isEmpty() }
            compose.onNodeWithTag("practice-notes-panel").assertExists();clearKeyboard()
            val first = notes.history.value.first { it.blockId == block && !it.legacy }
            tag("practice-note-${first.id}").performClick()
            tag("practice-note-edit-${first.id}").performClick()
            tag("practice-note-editor-${first.id}").performTextReplacement("First new note edited")
            clearKeyboard();tag("practice-note-save-${first.id}").performClick()
            compose.waitUntil(10000) { notes.history.value.any { it.id == first.id && it.text == "First new note edited" } }
            assertEquals(first.createdAt, notes.history.value.first { it.id == first.id }.createdAt)
            tag("practice-note-edit-${first.id}").performClick()
            tag("practice-note-editor-${first.id}").performTextReplacement("Cancel this edit")
            clearKeyboard();tag("practice-note-cancel-${first.id}").performClick()
            assertEquals("First new note edited", notes.history.value.first { it.id == first.id }.text)
            tag("practice-free-record").performClick()
            compose.waitUntil(10000) { audio.recording && audio.elapsedMs >= 1000 }
            tag("practice-new-note").performTextReplacement("Second new note")
            clearKeyboard();click("Save Note")
            compose.waitUntil(10000) { notes.history.value.count { it.blockId == block } == 3 && notes.draft(placement).value.isEmpty() }
            compose.runOnIdle { assertTrue(audio.recording); assertTrue(practice.running); assertTrue(practice.clicking) }
            tag("practice-free-record").performClick()
            compose.waitUntil(15000) { !audio.recording && !audio.busy && audio.takes.value.any { it.blockId == block && it.mode == "FREE" } }
            compose.onNodeWithTag("practice-notes-content-scrollbar").assertExists();screenshot("notes-history")
            click("Record Loop");collapsedHeaders();compose.onNodeWithTag("practice-notes-panel").assertDoesNotExist()
            click("Cancel");shown("Page 2 of 2");shown("Reset PDF zoom")
            tag("practice-free-selector").performClick();collapsedHeaders();click("Close");shown("Page 2 of 2")
            tag("practice-loop-selector").performClick();collapsedHeaders();click("Close");shown("Page 2 of 2")
            tag("practice-description-toggle").performClick();shown("Practice slowly")
            click("Notes");collapsedHeaders();click("Return to Practice");shown("Practice slowly")
            tag("practice-description-toggle").performClick()
            click("Notes");collapsedHeaders();click("Return to Practice");collapsedHeaders()
            tag("practice-reference-toggle").performClick();shown("Page 2 of 2")
            click("Notes");tag("practice-new-note").performTextReplacement("Keep this draft")
            clearKeyboard();click("Return to Practice");click("Notes")
            tag("practice-new-note").assertTextContains("Keep this draft")
            click("Return to Practice");click("Complete");shown("Unsaved note text");click("Keep editing")
            tag("practice-new-note").assertTextContains("Keep this draft")
            tag("practice-new-note").performTextReplacement("");clearKeyboard();click("Return to Practice")
            compose.runOnIdle { assertEquals(placement, practice.catalog.value.active!!.current!!.id); assertTrue(practice.running) }
            File(app.filesDir,"notes-preview-fixture.json").writeText(JSONObject().put("block",block).put("session",session).put("placement",placement).toString())
            screenshot("notes-restored-reference")
            click("Save for later");click("Back to Main");shown("Practice Now")
        } finally {
            screenshot("notes-final")
            compose.runOnIdle { audio.finishCapture();audio.stopPlayback();practice.stopClick() };db.close()
        }
    }
}

class NotesHistoryReopenTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun savedNotesAndEditsSurviveFreshProcess() {
        val app = compose.activity.application
        val fixture = JSONObject(File(app.filesDir,"notes-preview-fixture.json").readText())
        val db = BlockDatabase.open(app)
        try {
            val rows = runBlocking { db.practiceNotes().readAll().filter { it.blockId == fixture.getString("block") } }
            assertEquals(setOf("First new note edited", "Second new note"), rows.map { it.text }.toSet())
            assertEquals(2, rows.size)
            val practice = ViewModelProvider(compose.activity)[PracticeViewModel::class.java]
            val notes = ViewModelProvider(compose.activity)[BlockNotesViewModel::class.java]
            compose.waitUntil(15000) { practice.catalog.value.ready }
            compose.runOnIdle { practice.openSession(fixture.getString("session")) }
            compose.waitUntil(15000) { compose.onAllNodesWithText("Notes").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Notes").scrollIfNeeded().performClick()
            compose.waitUntil(10000) { notes.history.value.count { it.blockId == fixture.getString("block") } == 3 }
            compose.onNodeWithTag("practice-new-note").scrollIfNeeded().performTextReplacement("Third note after reopen")
            compose.activity.getSystemService(android.view.inputmethod.InputMethodManager::class.java).hideSoftInputFromWindow(compose.activity.window.decorView.windowToken,0)
            compose.onNodeWithText("Save Note").assertIsDisplayed().performClick()
            compose.waitUntil(10000) { notes.history.value.count { it.blockId == fixture.getString("block") } == 4 }
            assertEquals(3, runBlocking { db.practiceNotes().readAll().size })
            compose.onNodeWithTag("practice-notes-panel").assertExists()
        } finally { db.close() }
    }
}
