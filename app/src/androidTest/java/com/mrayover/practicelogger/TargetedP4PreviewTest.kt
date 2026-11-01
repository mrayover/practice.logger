package com.mrayover.practicelogger

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

class TargetedP4PreviewTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun shown(text: String) = compose.waitUntil(15000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    private fun click(text: String) { shown(text); compose.onNodeWithText(text).scrollIfNeeded().performClick() }
    private fun home() { compose.onNodeWithTag("home-brand", true).performClick(); shown("Practice Now") }
    private fun screenshot(name: String) {
        val folder = File(compose.activity.getExternalFilesDir(null), "p4-targeted").apply { mkdirs() }
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).takeScreenshot(File(folder,"$name.png"))
    }
    @Test fun libraryPlaceholderSharedGearAndSelectedOutlines() {
        shown("Practice Now"); click("Library"); shown("Coming soon.")
        compose.onNodeWithTag("header-settings").assertIsDisplayed().performClick(); shown("Settings")
        compose.onNodeWithText("Appearance").assertExists(); screenshot("settings")
        home(); click("Blocks"); shown("Block Library")
        compose.onNodeWithText("Active").assertIsSelected(); compose.onNodeWithText("Archived").assertIsNotSelected()
        screenshot("blocks"); click("Archived"); compose.onNodeWithText("Archived").assertIsSelected()
        compose.onNodeWithTag("header-settings").performClick(); shown("Settings")
    }
    @Test fun compactTransportAccentTempoAndFooter() {
        val db = BlockDatabase.open(compose.activity.application)
        val vm = ViewModelProvider(compose.activity)[PracticeViewModel::class.java]
        try {
            val id = runBlocking {
                val b = BlockRepository(db).save(null, BlockDraft("P4 transport", "Practice slowly", "5", "120"))
                val repo = SessionRepository(db); repo.addBlock(b); repo.start()
            }
            shown("Practice Now"); compose.runOnIdle { vm.openSession(id) }; shown("Practice Description")
            compose.onNodeWithText("4/4").assertIsDisplayed()
            compose.onNodeWithTag("practice-metronome-accent").assertIsOff().performClick().assertIsOn()
            compose.runOnIdle { assertTrue(vm.metronomeConfig.accent) }
            compose.onNodeWithContentDescription("Increase click BPM by 1").performClick()
            compose.runOnIdle { assertEquals(121, vm.workingBpm) }
            compose.onNodeWithContentDescription("Decrease click BPM by 1").performClick()
            compose.runOnIdle { assertEquals(120, vm.workingBpm) }
            val buttons = listOf("Start", "Complete", "Notes", "Skip").map { compose.onNodeWithText(it).assertIsDisplayed().fetchSemanticsNode().boundsInRoot }
            assertTrue(buttons.all { kotlin.math.abs(it.top-buttons.first().top) <= 1f })
            compose.onNodeWithTag("practice-transport-scrollbar").assertDoesNotExist()
            val chevron = compose.onNodeWithTag("practice-metronome-chevron", true).fetchSemanticsNode().boundsInRoot.center.x
            for (type in listOf("free", "loop")) {
                val x = compose.onNodeWithTag("practice-$type-chevron", true).fetchSemanticsNode().boundsInRoot.center.x
                assertTrue("Aligned transport chevrons: $chevron / $x", kotlin.math.abs(chevron-x) <= 1f)
            }
            screenshot("transport")
            compose.onNodeWithTag("practice-metronome-expand").performClick(); shown("BPM (30–300)")
            compose.onNodeWithTag("practice-metronome-expand").performClick()
            compose.onNodeWithTag("header-settings").performClick(); shown("Leave current session?")
            click("Continue"); shown("Practice Description")
        } finally { compose.runOnIdle { vm.stopClick() }; db.close() }
    }
    @Test fun completedSessionSummaryDisplaysAndEditsSavedPracticeNotes() {
        val db = BlockDatabase.open(compose.activity.application)
        try {
            val (id, noteId) = runBlocking {
                val b = BlockRepository(db).save(null, BlockDraft("P4 history", "Practice", "5", "120"))
                val repo = SessionRepository(db); repo.addBlock(b); val id = repo.start()
                val placement = db.sessions().find(id)!!.current!!
                val n = PracticeNoteRepository(db).append(b,id,placement.id,"Saved during practice")
                repo.finishBlock(id,"COMPLETED","",preserveNote=true)
                id to "note:${n.id}"
            }
            shown("Practice Now"); openHomeRoutines(compose); shown("Recent Practice")
            // Open the exact session via its pill, rather than relying on date uniqueness.
            compose.onNodeWithTag("recent-session-$id").scrollIfNeeded().performClick(); shown("Session Summary")
            compose.onNodeWithTag("practice-note-$noteId").scrollIfNeeded().performClick(); shown("Saved during practice")
            compose.onNodeWithTag("practice-note-edit-$noteId").scrollIfNeeded().performClick()
            compose.onNodeWithTag("practice-note-editor-$noteId").performTextReplacement("Edited in historical summary")
            compose.activity.getSystemService(android.view.inputmethod.InputMethodManager::class.java)
                .hideSoftInputFromWindow(compose.activity.window.decorView.windowToken,0)
            compose.onNodeWithTag("practice-note-save-$noteId").scrollIfNeeded().performClick(); shown("Edited in historical summary")
            screenshot("history")
            compose.activityRule.scenario.recreate(); shown("Session Summary")
            compose.onNodeWithTag("practice-note-text-$noteId").scrollIfNeeded().assertTextContains("Edited in historical summary")
            assertEquals("Edited in historical summary",runBlocking { db.practiceNotes().readAll().single { "note:${it.id}" == noteId }.text })
        } finally { db.close() }
    }
}
