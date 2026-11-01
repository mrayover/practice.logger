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
                assertTrue("Aligned transport chevrons: $chevron / $x", kotlin.math.abs(chevron-x) <= compose.activity.resources.displayMetrics.density)
            }
            screenshot("transport-measurement")
            val density = compose.activity.resources.displayMetrics.density
            val row = compose.onNodeWithTag("practice-metronome-row").fetchSemanticsNode().boundsInRoot
            assertTrue("Chevron retains its original anchor", kotlin.math.abs(chevron-(row.right-196f*density)) <= density)
            val minus = compose.onNodeWithContentDescription("Decrease click BPM by 1").fetchSemanticsNode().boundsInRoot
            val plus = compose.onNodeWithContentDescription("Increase click BPM by 1").fetchSemanticsNode().boundsInRoot
            val record = compose.onNodeWithTag("practice-free-record").fetchSemanticsNode().boundsInRoot
            assertTrue("Tempo pair centered above Record: ${(minus.center.x+plus.center.x)/2} / ${record.center.x}", kotlin.math.abs((minus.center.x+plus.center.x)/2-record.center.x) <= density)
            fun textLayout(node: SemanticsNodeInteraction): androidx.compose.ui.text.TextLayoutResult {
                val results = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
                node.performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) { it(results) }
                return results.single()
            }
            val info = compose.onNodeWithTag("metronome-bpm").fetchSemanticsNode().boundsInRoot
            for (meter in MetronomeMeter.entries) {
                compose.runOnIdle { vm.configureMetronome(vm.metronomeConfig.copy(meter = meter)) }
                for (text in listOf("Metronome", meter.label)) {
                    val node = compose.onNodeWithText(text, useUnmergedTree = true).assertIsDisplayed()
                    val layout = textLayout(node)
                    assertEquals("Single-line $text", 1, layout.lineCount)
                    // Text rounds fractional paragraph height to integer physical pixels.
                    println("Metronome text $text: size=${layout.size}, paragraphHeight=${layout.multiParagraph.height}, lineWidth=${layout.getLineRight(0)-layout.getLineLeft(0)}, overflow=${layout.hasVisualOverflow}")
                    assertFalse("Unellipsized $text", layout.isLineEllipsized(0))
                    assertTrue("Unclipped width $text", layout.getLineRight(0)-layout.getLineLeft(0) <= layout.size.width+1f)
                    assertTrue("Unclipped height $text", layout.multiParagraph.height <= layout.size.height+1f)
                    val bounds = node.fetchSemanticsNode().boundsInRoot
                    assertTrue("$text stays inside information area", bounds.left >= info.left-1f && bounds.right <= info.right+1f)
                }
            }
            compose.runOnIdle { vm.configureMetronome(vm.metronomeConfig.copy(meter = MetronomeMeter.THREE_FOUR)) }
            val clickLabel = compose.onNodeWithText("Start click", useUnmergedTree = true)
            val clickBubble = compose.onNodeWithTag("practice-metronome-pulse").fetchSemanticsNode().boundsInRoot
            val clickLayout = textLayout(clickLabel)
            val clickTextCenter = clickLabel.fetchSemanticsNode().boundsInRoot.left + (clickLayout.getLineLeft(0)+clickLayout.getLineRight(0))/2
            assertTrue("Click text centered: $clickTextCenter / ${clickBubble.center.x}", kotlin.math.abs(clickTextCenter-clickBubble.center.x) <= density)
            for (type in listOf("free", "loop")) {
                val action = compose.onNodeWithTag("practice-$type-record").fetchSemanticsNode().boundsInRoot
                assertTrue("Bubble left meets $type action right: ${clickBubble.left} / ${action.right}", kotlin.math.abs(clickBubble.left-action.right) <= density)
            }
            screenshot("transport")
            compose.runOnIdle { compose.activity.themePreferences.save(ThemeChoice.DARK) }
            screenshot("transport-dark")
            compose.onNodeWithTag("practice-metronome-expand").performClick(); shown("BPM (30–300)")
            compose.onNodeWithTag("practice-metronome-expand").performClick()
            compose.onNodeWithTag("header-settings").performClick(); shown("Leave current session?")
            click("Continue"); shown("Practice Description")
            compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
            shown("Leave current session?"); click("Back to Main"); shown("Practice Now")
            compose.onNodeWithText("Settings").assertDoesNotExist()
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
