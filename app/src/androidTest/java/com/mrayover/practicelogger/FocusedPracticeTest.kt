package com.mrayover.practicelogger

import android.Manifest
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

// Pinned controls have no scrolling parent. Scroll only controls inside scrollable content.
fun SemanticsNodeInteraction.scrollIfNeeded(): SemanticsNodeInteraction {
    var parent = fetchSemanticsNode().parent
    while (parent != null) {
        if (parent.config.contains(androidx.compose.ui.semantics.SemanticsActions.ScrollBy)) return performScrollTo()
        parent = parent.parent
    }
    return assertIsDisplayed()
}

fun scrollPracticeWorkspace(compose: AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>, matcher: SemanticsMatcher) {
    if (compose.onAllNodesWithTag("practice-workspace").fetchSemanticsNodes().isEmpty() ||
        compose.onAllNodesWithTag("take-list").fetchSemanticsNodes().isNotEmpty()) return
    val workspace=compose.onNodeWithTag("practice-workspace")
    val target=compose.onNode(matcher)
    // A bounded real gesture traverses nested scroll containers the same way as a finger.
    repeat(12) {
        if (target.isDisplayed()) return
        val bounds=target.getUnclippedBoundsInRoot();val viewport=workspace.getUnclippedBoundsInRoot()
        android.util.Log.d("PracticeScroll", "step=$it target=$bounds viewport=$viewport")
        val down=bounds.top<viewport.top
        workspace.performTouchInput {
            val x=width-3f
            swipe(androidx.compose.ui.geometry.Offset(x,height*(if(down) .2f else .75f)),
                androidx.compose.ui.geometry.Offset(x,height*(if(down) .75f else .2f)),300)
        }
        compose.waitForIdle()
    }
    target.assertIsDisplayed()
}

fun revealPracticeReference(compose: AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>) {
    if (compose.onAllNodesWithTag("practice-workspace").fetchSemanticsNodes().isNotEmpty() &&
        compose.onAllNodesWithTag("take-list").fetchSemanticsNodes().isEmpty()) {
        compose.onNodeWithTag("reference-viewer").performScrollTo()
        scrollPracticeWorkspace(compose, hasTestTag("reference-viewer"))
        compose.onNodeWithTag("reference-viewer").assertIsDisplayed()
    }
}

fun revealPracticeNotes(compose: AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>) {
    if (compose.onAllNodesWithText("Quick note (optional)").fetchSemanticsNodes().isEmpty())
        compose.onNodeWithText("Notes").scrollIfNeeded().performClick()
}

class FocusedPracticeTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val device get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private fun click(text: String) = compose.onNodeWithText(text).scrollIfNeeded().performClick()
    private fun shown(text: String) { compose.waitUntil(15000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() } }
    private fun screenshot(name: String) {
        compose.waitForIdle(); InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        val dir = File(compose.activity.getExternalFilesDir(null), "screenshots"); dir.mkdirs()
        device.takeScreenshot(File(dir, "focused-$name.png"))
    }
    @Test fun compactAudioCrossingAndProtectedExit() {
        val app=compose.activity.application
        val db=BlockDatabase.open(app)
        val practice=ViewModelProvider(compose.activity)[PracticeViewModel::class.java]
        val recordings=ViewModelProvider(compose.activity)[RecordingViewModel::class.java]
        try {
            device.executeShellCommand("pm grant ${app.packageName} ${Manifest.permission.RECORD_AUDIO}")
            val session=runBlocking {
                val id=BlockRepository(db).save(null,BlockDraft("Focused fixture","Play the phrase slowly. Keep the hands relaxed and listen to each note.","1","112"))
                val repo=SessionRepository(db); repo.addBlock(id); repo.addBlock(id); val sid=repo.start()
                val rows=db.sessions().find(sid)!!.ordered
                db.sessions().update(rows[0].copy(configuredMs=4000)); db.sessions().update(rows[1].copy(configuredMs=4000))
                sid
            }
            shown("Practice Now"); compose.runOnIdle { practice.openSession(session) }; shown("Practice Description")
            screenshot("initial-before-assertions")
            listOf("Start", "Complete", "Skip").forEach { label ->
                val layouts=mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
                compose.onNodeWithText(label,useUnmergedTree=true).performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) { it(layouts) }
                assertTrue("Primary label must not break inside a word: $label",layouts.isNotEmpty() && layouts.all { it.lineCount==1 })
            }

            // At normal font scale this verifies real visibility without scrolling first.
            if (app.resources.configuration.fontScale < 1.5f) {
                listOf("Practice Description","Free recording","Loop recording","Recordings","Start","Complete","Skip").forEach { compose.onNodeWithText(it).assertIsDisplayed() }
                compose.onNodeWithTag("metronome-bpm").assertIsDisplayed()
            }
            screenshot("collapsed")
            click("Start"); compose.waitUntil(12000) { !practice.running && practice.completionSignals==1 }
            compose.waitUntil(5000) { practice.completionFramesWritten()>=28_800 }
            assertEquals(4000L,runBlocking { db.sessions().find(session)!!.current!!.activeMs })
            assertEquals("PENDING",runBlocking { db.sessions().find(session)!!.current!!.outcome })
            screenshot("zero")
            revealPracticeNotes(compose)
            compose.onNodeWithText("Quick note (optional)").performScrollTo().performTextInput("Preserve this note")
            compose.runOnIdle {
                (app.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager).hideSoftInputFromWindow(compose.activity.window.decorView.windowToken,0)
            }
            click("Save Block note"); compose.waitUntil(15000) { !practice.busy }
            compose.activityRule.scenario.recreate(); shown("Practice Description")
            assertEquals(1,practice.completionSignals)
            compose.runOnIdle { practice.openSession(session) }; compose.waitUntil(15000) { !practice.busy }
            assertEquals(1,practice.completionSignals)
            click("Continue"); compose.waitUntil(5000) { practice.running }; click("Pause")
            assertEquals(1,practice.completionSignals)
            click("Complete"); shown("Block 2 of 2")
            click("Start click"); compose.runOnIdle { practice.changeWorkingBpm(113) }
            click("Start"); compose.waitUntil(12000) { !practice.running && practice.completionSignals==2 }
            compose.waitUntil(5000) { practice.completionFramesWritten()>=28_800 }
            assertTrue(practice.clicking); assertEquals(113,practice.workingBpm)
            // Continue dismisses the dialog without changing click state.
            compose.onNodeWithTag("home-brand", useUnmergedTree = true).performClick(); shown("Leave current session?")
            compose.onNode(hasText("Continue") and hasAnyAncestor(isDialog())).performClick(); assertTrue(practice.clicking)
            // Force a real SQLite note-write failure and prove navigation waits for success.
            revealPracticeNotes(compose)
            compose.onNodeWithText("Quick note (optional)").performScrollTo().performTextInput("retry note")
            compose.runOnIdle { (app.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager).hideSoftInputFromWindow(compose.activity.window.decorView.windowToken,0) }
            runBlocking { db.openHelper.writableDatabase.execSQL("CREATE TRIGGER focused_save_failure BEFORE UPDATE OF note ON placements BEGIN SELECT RAISE(FAIL, 'injected note failure'); END") }
            compose.onNodeWithTag("home-brand", useUnmergedTree = true).performClick(); compose.onNodeWithText("Back to Main").performClick()
            shown("Practice paused"); compose.onNodeWithTag("practice-timer").assertExists(); assertFalse(practice.running)
            runBlocking { db.openHelper.writableDatabase.execSQL("DROP TRIGGER focused_save_failure") }
            compose.onNodeWithText("OK").performClick(); compose.onNodeWithText("Back to Main").performClick(); shown("Practice Now")
            assertFalse(practice.clicking)
            assertEquals("retry note",runBlocking { db.sessions().find(session)!!.current!!.note })
            compose.runOnIdle { practice.openSession(session) }; shown("Practice Description"); assertEquals(2,practice.completionSignals)
            click("Continue"); compose.waitUntil(5000) { practice.running }
            // The direct free take uses the existing engine; exit/Complete cannot lose it.
            click("Record"); compose.waitUntil(10000) { recordings.recording && recordings.elapsedMs>=1000 }
            compose.onNodeWithText("Complete").assertIsNotEnabled(); compose.onNodeWithTag("practice-recording-stop").assertIsDisplayed()
            screenshot("free-recording"); compose.onNodeWithTag("practice-recording-stop").performClick()
            compose.waitUntil(15000) { !recordings.busy && recordings.takes.value.any { it.blockId==db.sessions().let { runBlocking { it.find(session)!!.current!!.sourceBlockId } } } }
            compose.onNodeWithContentDescription("Play latest free take").scrollIfNeeded().performClick()
            compose.waitUntil(10000) { recordings.playingId!=null }; compose.runOnIdle { recordings.stopPlayback() }
            // Android Back shares the same confirmation and saves the unfinished session.
            device.pressBack(); shown("Leave current session?"); compose.onNodeWithText("Back to Main").performClick(); shown("Practice Now")
            assertEquals(2,practice.completionSignals)
            // A new timer run paused well before zero cannot announce completion.
            val next=runBlocking {
                val id=BlockRepository(db).save(null,BlockDraft("Pause safety","Pause before duration","1")); val repo=SessionRepository(db);repo.addBlock(id);repo.start()
            }
            compose.runOnIdle { practice.openSession(next) }; shown("Start"); click("Start"); click("Pause")
            compose.waitUntil(5000) { !practice.running && !practice.busy }; assertEquals(2,practice.completionSignals)
            screenshot("paused")
            // Header protects an unrelated dirty Block editor.
            click("Save for later"); compose.onNodeWithText("Back to Main").performClick(); shown("Practice Now")
            click("Blocks"); click("New Block"); compose.onNodeWithTag("Name").performScrollTo().performTextInput("Unsaved draft")
            compose.onNodeWithTag("home-brand", useUnmergedTree = true).performClick(); shown("Discard unsaved changes?")
            screenshot("editor-guard")
        } catch (failure: Throwable) { screenshot("failure"); throw failure } finally { db.close() }
    }
}
