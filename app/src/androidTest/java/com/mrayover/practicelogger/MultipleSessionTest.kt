package com.mrayover.practicelogger

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class MultipleSessionWorkflowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun shown(text: String) { compose.waitUntil(15000) { compose.onAllNodesWithText(text, substring = false).fetchSemanticsNodes().isNotEmpty() } }
    private fun click(text: String) { compose.onNodeWithText(text, substring = false).scrollIfNeeded().performClick() }
    @Test fun startFreshWhileKeepingOlderPracticeAndSwitchBack() {
        val db = BlockDatabase.open(compose.activity.applicationContext)
        try {
            runBlocking {
                BlockRepository(db).save(null, BlockDraft("Parallel A", "First independent practice", "1"))
                BlockRepository(db).save(null, BlockDraft("Parallel B", "Second independent practice", "1"))
            }
            shown("Practice Now"); click("Practice Now"); click("Add Blocks"); shown("Add Parallel A"); click("Add Parallel A")
            click("Start Session"); shown("Parallel A")
            revealPracticeNotes(compose)
            compose.onNodeWithTag("practice-new-note").performScrollTo().performTextInput("Keep A"); savePracticeNote(compose)
            leavePracticeForLater(compose); openHomePractice(compose); shown("In Progress")
            click("Add Blocks"); shown("Add Parallel B"); click("Add Parallel B")
            click("Start Session"); shown("Parallel B")
            revealPracticeNotes(compose)
            compose.onNodeWithTag("practice-new-note").performScrollTo().performTextInput("Keep B"); savePracticeNote(compose)
            leavePracticeForLater(compose); openHomePractice(compose); shown("In Progress")
            val first = runBlocking { db.sessions().readAll().single { it.session.status == "IN_PROGRESS" && it.current!!.name == "Parallel A" } }
            assertEquals(2, runBlocking { db.sessions().readAll().count { it.session.status == "IN_PROGRESS" } })
            click("In Progress")
            compose.onNodeWithTag("continue-session-" + first.session.id).scrollIfNeeded().performClick()
            shown("Parallel A")
            revealPracticeNotes(compose)
            assertSavedPracticeNote(compose, "Keep A")
            compose.onNodeWithText("Start").assertExists()
            compose.activityRule.scenario.recreate(); shown("Parallel A")
            revealPracticeNotes(compose)
            assertSavedPracticeNote(compose, "Keep A")
            leavePracticeForLater(compose); openHomePractice(compose); shown("In Progress")
        } finally { db.close() }
    }
}

class MultipleSessionReopenTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun shown(text: String) { compose.waitUntil(15000) { compose.onAllNodesWithText(text, substring = false).fetchSemanticsNodes().isNotEmpty() } }
    private fun click(text: String) { compose.onNodeWithText(text, substring = false).scrollIfNeeded().performClick() }
    @Test fun bothUnfinishedSessionsSurviveProcessRestartAndFinishIndependently() {
        openHomePractice(compose); shown("In Progress")
        val db = BlockDatabase.open(compose.activity.applicationContext)
        try {
            val first = runBlocking { db.sessions().readAll().single { it.session.status == "IN_PROGRESS" && it.current!!.name == "Parallel A" } }
            assertEquals(2, runBlocking { db.sessions().readAll().count { it.session.status == "IN_PROGRESS" } })
            click("In Progress")
            compose.onNodeWithTag("continue-session-" + first.session.id).scrollIfNeeded().performClick()
            shown("Parallel A"); revealPracticeNotes(compose); assertSavedPracticeNote(compose, "Keep A")
            click("Complete"); shown("Session Summary"); compose.onNodeWithTag("home-brand", useUnmergedTree = true).performClick(); openHomePractice(compose); shown("In Progress")
            continueFirstSession(compose); shown("Parallel B")
            revealPracticeNotes(compose)
            assertSavedPracticeNote(compose, "Keep B")
            compose.onNodeWithText("Start").assertExists()
            click("Complete"); shown("Session Summary"); compose.onNodeWithTag("home-brand", useUnmergedTree = true).performClick(); shown("Practice Now")
            runBlocking {
                val pair = db.sessions().readAll().filter { it.ordered.firstOrNull()?.name in setOf("Parallel A", "Parallel B") }
                assertEquals(2, pair.size)
                assertTrue(pair.all { it.session.status == "COMPLETED" && it.activeMs == 0L })
                assertEquals(setOf("Keep A", "Keep B"), db.practiceNotes().readAll().filter { note -> pair.any { it.session.id == note.sessionId } }.map { it.text }.toSet())
            }
        } finally { db.close() }
    }
}
