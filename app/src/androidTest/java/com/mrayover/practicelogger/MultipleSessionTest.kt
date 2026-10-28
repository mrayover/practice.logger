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
    private fun click(text: String) { compose.onNodeWithText(text, substring = false).performScrollTo().performClick() }
    @Test fun startFreshWhileKeepingOlderPracticeAndSwitchBack() {
        val db = BlockDatabase.open(compose.activity.applicationContext)
        try {
            runBlocking {
                BlockRepository(db).save(null, BlockDraft("Parallel A", "First independent practice", "1"))
                BlockRepository(db).save(null, BlockDraft("Parallel B", "Second independent practice", "1"))
            }
            shown("Practice Now"); click("Practice Now"); shown("Add Parallel A"); click("Add Parallel A")
            click("Start Session"); shown("Parallel A")
            compose.onNodeWithText("Quick note (optional)").performScrollTo().performTextInput("Keep A")
            click("Save for later"); openHomePractice(compose); shown("Continue Session")
            shown("Add Parallel B"); click("Add Parallel B")
            click("Start Session"); shown("Parallel B")
            compose.onNodeWithText("Quick note (optional)").performScrollTo().performTextInput("Keep B")
            click("Save for later"); openHomePractice(compose); shown("Continue Session")
            val first = runBlocking { db.sessions().readAll().single { it.session.status == "IN_PROGRESS" && it.current!!.name == "Parallel A" } }
            assertEquals(2, runBlocking { db.sessions().readAll().count { it.session.status == "IN_PROGRESS" } })
            compose.onNodeWithTag("continue-session-" + first.session.id).performScrollTo().performClick()
            shown("Parallel A")
            compose.onNodeWithText("Quick note (optional)").performScrollTo().assertTextContains("Keep A")
            compose.onNodeWithText("Ready when you are").assertExists()
            compose.activityRule.scenario.recreate(); shown("Parallel A")
            compose.onNodeWithText("Quick note (optional)").performScrollTo().assertTextContains("Keep A")
            click("Save for later"); openHomePractice(compose); shown("Continue Session")
        } finally { db.close() }
    }
}

class MultipleSessionReopenTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun shown(text: String) { compose.waitUntil(15000) { compose.onAllNodesWithText(text, substring = false).fetchSemanticsNodes().isNotEmpty() } }
    private fun click(text: String) { compose.onNodeWithText(text, substring = false).performScrollTo().performClick() }
    @Test fun bothUnfinishedSessionsSurviveProcessRestartAndFinishIndependently() {
        openHomePractice(compose); shown("Continue Session")
        val db = BlockDatabase.open(compose.activity.applicationContext)
        try {
            val first = runBlocking { db.sessions().readAll().single { it.session.status == "IN_PROGRESS" && it.current!!.name == "Parallel A" } }
            assertEquals(2, runBlocking { db.sessions().readAll().count { it.session.status == "IN_PROGRESS" } })
            val firstControl = compose.onAllNodesWithTag("continue-session-" + first.session.id)
            if (firstControl.fetchSemanticsNodes().isNotEmpty()) firstControl[0].performScrollTo().performClick()
            else click("Continue Session")
            shown("Parallel A"); compose.onNodeWithText("Quick note (optional)").performScrollTo().assertTextContains("Keep A")
            click("Complete"); shown("Session Summary"); click("Home"); openHomePractice(compose); shown("Continue Session")
            click("Continue Session"); shown("Parallel B")
            compose.onNodeWithText("Quick note (optional)").performScrollTo().assertTextContains("Keep B")
            compose.onNodeWithText("Ready when you are").assertExists()
            click("Complete"); shown("Session Summary"); click("Home"); shown("Practice Now")
            runBlocking {
                val pair = db.sessions().readAll().filter { it.ordered.firstOrNull()?.name in setOf("Parallel A", "Parallel B") }
                assertEquals(2, pair.size)
                assertTrue(pair.all { it.session.status == "COMPLETED" && it.activeMs == 0L })
                assertEquals(setOf("Keep A", "Keep B"), pair.map { it.current!!.note }.toSet())
            }
        } finally { db.close() }
    }
}
