package com.mrayover.practicelogger

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class GoalWorkflowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun shown(text: String) { compose.waitUntil(15000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() } }
    private fun click(text: String) { shown(text); compose.onNodeWithText(text).performScrollTo().performClick() }
    @Test fun goalManagementSharedBlocksIndependentPracticeAndRecovery() {
        val db = BlockDatabase.open(compose.activity.applicationContext)
        try {
            val b = runBlocking { BlockRepository(db).save(null, BlockDraft("Goal active fixture", "Play a melody", "2")) }
            val archived = runBlocking { BlockRepository(db).save(null, BlockDraft("Goal archived fixture", "Play changes", "1")) }
            val sessions = SessionRepository(db)
            val draft = runBlocking { sessions.addBlock(b); db.sessions().find(db.sessions().withStatus("DRAFT")!!.id)!! }
            val existing = runBlocking { db.sessions().readAll().filter { it.session.status == "IN_PROGRESS" } }
            shown("Practice Now"); click("Edit"); shown("Manage Goals"); click("New Goal")
            compose.onNodeWithText("Goal title").performTextInput("Goal workflow")
            compose.onNodeWithText("Desired outcome (optional)").performTextInput("Memorize melody and changes")
            compose.onNodeWithText("Completion percentage (0–100)").performTextReplacement("37")
            compose.onNodeWithText("Goal notes (optional)").performScrollTo().performTextInput("Listen and assess")
            compose.activityRule.scenario.recreate(); shown("Goal title")
            compose.onNodeWithText("Goal title").assertTextContains("Goal workflow")
            captureStage3("goal-00-editor")
            click("Save Goal"); shown("37% complete · set by you")
            val id = runBlocking { db.goals().readAll().single { it.goal.title == "Goal workflow" }.goal.id }
            click("Manage Blocks"); click("Add Goal active fixture")
            shown("Remove Goal active fixture"); click("Add Goal archived fixture")
            shown("Remove Goal archived fixture"); captureStage3("goal-00-blocks"); click("Back to Goal")
            runBlocking { BlockRepository(db).setArchived(archived, true) }
            shown("Archived: Goal archived fixture · 1 min")
            captureStage3("goal-01-detail")
            click("Edit Goal"); compose.onNodeWithText("Goal title").performTextReplacement("Goal workflow edited")
            click("Save Goal"); shown("Goal workflow edited")
            click("Back to Goals"); click("New Goal")
            compose.onNodeWithText("Goal title").performTextInput("Second Goal")
            click("Save Goal"); click("Manage Blocks"); click("Add Goal active fixture")
            shown("Remove Goal active fixture"); click("Back to Goal"); click("Back to Goals")
            compose.onNodeWithContentDescription("Move Second Goal up").performScrollTo().performClick()
            shown("1. Second Goal · 0%"); captureStage3("goal-02-management")
            click("Back to Home"); shown("1. Second Goal · 0%"); shown("2. Goal workflow edited · 37%")
            captureStage3("goal-03-home")
            click("2. Goal workflow edited · 37%")
            click("Practice Goal"); shown("Ready when you are"); shown("Goal: Goal workflow edited")
            click("Start"); shown("Practicing")
            compose.waitUntil(15000) { runBlocking { db.goals().readSessions().any { it.goalId == id && (db.sessions().find(it.sessionId)?.activeMs ?: 0) > 500 } } }
            click("Pause"); click("Save for later"); shown("Practice Now")
            val first = runBlocking { db.goals().readSessions().single { it.goalId == id }.sessionId }
            val committed = runBlocking { db.sessions().find(first)!! }
            assertTrue(committed.activeMs > 0)
            click("2. Goal workflow edited · 37%"); click("Practice Goal"); shown("Ready when you are")
            click("Complete"); shown("Session Summary"); shown("Goal: Goal workflow edited")
            captureStage3("goal-04-summary")
            click("Home"); click("2. Goal workflow edited · 37%")
            click("Mark Goal completed"); shown("Completed Goal")
            click("Back to Goals"); click("Completed Goals"); shown("Goal workflow edited · 37%")
            click("Goal workflow edited · 37%"); click("Reopen Goal"); shown("Practice Goal")
            compose.activityRule.scenario.recreate(); shown("37% complete · set by you")
            captureStage3("goal-05-recovered")
            runBlocking {
                assertEquals(draft, db.sessions().find(draft.session.id))
                existing.forEach { assertEquals(it, db.sessions().find(it.session.id)) }
                assertEquals(committed, db.sessions().find(first))
                assertEquals(2, db.goals().readSessions().count { it.goalId == id })
                assertEquals(2, db.goals().find(id)!!.blocks.size)
                assertEquals(listOf(id), db.goals().readSessions().filter { it.sessionId == first }.map { it.goalId })
                assertEquals("Listen and assess", db.goals().find(id)!!.goal.notes)
            }
        } finally { db.close() }
    }
}

class GoalReopenTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun coldRestartKeepsGoalTimeProgressAndExplicitHistory() {
        val db = BlockDatabase.open(compose.activity.applicationContext)
        try {
            val g = runBlocking { db.goals().readAll().single { it.goal.title == "Goal workflow edited" } }
            val rows = runBlocking { db.goals().readSessions().filter { it.goalId == g.goal.id }.map { db.sessions().find(it.sessionId)!! } }
            assertNotNull(runBlocking { db.sessions().withStatus("DRAFT") })
            assertEquals(37, g.goal.progress); assertEquals(2, rows.size)
            assertEquals(1, rows.count { it.session.status == "COMPLETED" }); assertEquals(1, rows.count { it.session.status == "IN_PROGRESS" })
            assertTrue(rows.sumOf { it.activeMs } > 0)
            compose.waitUntil(15000) { compose.onAllNodesWithText("2. Goal workflow edited · 37%").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("2. Goal workflow edited · 37%").performScrollTo().performClick()
            compose.waitUntil(15000) { compose.onAllNodesWithText(practiceTime(rows.sumOf { it.activeMs }) + " actual practice").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("37% complete · set by you").assertExists()
            captureStage3("goal-06-cold-reopen")
        } finally { db.close() }
    }
}
