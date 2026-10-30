package com.mrayover.practicelogger

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class RoutineWorkflowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun shown(text: String) { compose.waitUntil(15000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() } }
    private fun click(text: String) { compose.onNodeWithText(text).scrollIfNeeded().performClick() }
    private fun row(index: Int, text: String) = compose.onNode(hasText(text) and hasAnyAncestor(hasTestTag("routine-placement-$index")))
    @Test fun builderSaveManagementIndependentPracticeAndRecovery() {
        val db = BlockDatabase.open(compose.activity.applicationContext)
        try {
            val ids = runBlocking {
                // Only reset this disposable emulator's builder draft, never practice/history/audio.
                SessionRepository(db).discardDraft()
                listOf("Routine A", "Routine B", "Routine C").map { BlockRepository(db).save(null, BlockDraft(it, "Play $it", "1", "100")) }
            }
            val blocks = runBlocking { db.blocks().readAll() }
            openHomePractice(compose)
            click("Add Routine A"); shown("1. Routine A")
            click("Add Routine B"); shown("2. Routine B")
            click("Add Routine A"); shown("3. Routine A")
            val draft = runBlocking { db.sessions().find(db.sessions().withStatus("DRAFT")!!.id)!! }
            click("Save as Routine")
            compose.onNodeWithText("Routine name").performTextInput("Warmup saved")
            compose.onNodeWithText("Save Routine").performClick()
            shown("Routine saved. Your session draft is unchanged.")
            val routineId = runBlocking {
                assertEquals(draft, db.sessions().find(draft.session.id)); assertEquals(blocks, db.blocks().readAll())
                db.routines().readAll().single { it.routine.name == "Warmup saved" }.routine.id
            }
            compose.onNodeWithTag("home-brand").performClick(); openHomeRoutines(compose)
            compose.waitForIdle(); captureStage3("routine-01-list")
            compose.onNodeWithTag("edit-routine-$routineId").scrollIfNeeded().performClick()
            shown("Routine name"); compose.onNodeWithText("Routine name").performTextReplacement("Daily routine")
            row(1, "Up").scrollIfNeeded().performClick(); shown("1. Routine B")
            row(0, "Change duration").scrollIfNeeded().performClick()
            compose.onNodeWithText("Minutes").performTextReplacement("3")
            compose.onNodeWithText("Apply duration").performClick(); shown("Routine duration: 3:00")
            row(2, "Remove").scrollIfNeeded().performClick()
            click("Add Routine C"); shown("3. Routine C")
            compose.activityRule.scenario.recreate(); shown("Routine name")
            compose.onNodeWithText("Routine name").assertTextContains("Daily routine")
            shown("1. Routine B"); shown("3. Routine C"); shown("Routine duration: 3:00")
            compose.onNodeWithText("Routine name").performScrollTo(); compose.waitForIdle(); captureStage3("routine-02-editor")
            click("Save Routine"); shown("Daily routine")
            val template = runBlocking { db.routines().find(routineId)!! }
            assertEquals(listOf(ids[1], ids[0], ids[2]), template.ordered.map { it.sourceBlockId })
            assertEquals(listOf(180_000L, 60_000L, 60_000L), template.ordered.map { it.configuredMs })
            click("New Routine"); shown("Routine name")
            compose.onNodeWithText("Routine name").performTextInput("Disposable routine")
            click("Add Routine A"); shown("1. Routine A"); click("Save Routine"); shown("Disposable routine")
            val otherId = runBlocking { db.routines().readAll().single { it.routine.name == "Disposable routine" }.routine.id }
            compose.onNodeWithTag("delete-routine-$otherId").scrollIfNeeded().performClick()
            compose.onNodeWithText("Keep Routine").performClick()
            compose.onNodeWithTag("delete-routine-$otherId").scrollIfNeeded().performClick()
            compose.onNodeWithText("Delete Routine permanently").performClick()
            compose.waitUntil(15000) { runBlocking { db.routines().find(otherId) == null } }
            runBlocking { BlockRepository(db).setArchived(ids[1], true) }
            compose.waitUntil(15000) { compose.onAllNodesWithText("Archived: Routine B", substring = true).fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("start-routine-$routineId").assertIsNotEnabled()
            runBlocking { BlockRepository(db).setArchived(ids[1], false) }
            compose.waitUntil(15000) { !compose.onNodeWithTag("start-routine-$routineId").fetchSemanticsNode().config.contains(androidx.compose.ui.semantics.SemanticsProperties.Disabled) }
            compose.onNodeWithTag("start-routine-$routineId").scrollIfNeeded().performClick()
            shown("Ready when you are"); compose.onNodeWithTag("practice-timer").assertTextEquals("3:00")
            val first = runBlocking { db.sessions().readAll().single { it.current?.name == "Routine B" && it.session.status == "IN_PROGRESS" }.session.id }
            revealPracticeNotes(compose)
            compose.onNodeWithText("Quick note (optional)").performScrollTo().performTextInput("Routine recovery note")
            click("Save Block note"); shown("Block note saved")
            click("Start"); shown("Pause"); click("Pause"); shown("Resume")
            leavePracticeForLater(compose); openHomeRoutines(compose)
            runBlocking { BlockRepository(db).save(ids[1], BlockDraft("Routine B updated", "Current action", "9", "140", "Current notes")) }
            compose.onNodeWithTag("start-routine-$routineId").scrollIfNeeded().performClick(); shown("Ready when you are")
            shown("Routine B updated"); compose.onNodeWithTag("practice-timer").assertTextEquals("3:00")
            runBlocking {
                val all = db.sessions().readAll(); val one = db.sessions().find(first)!!
                val two = all.single { it.current?.name == "Routine B updated" && it.session.status == "IN_PROGRESS" }
                assertNotEquals(first, two.session.id); assertEquals("Routine recovery note", one.current!!.note)
                assertEquals("Routine B", one.current!!.name); assertEquals("", two.current!!.note); assertEquals(0L, two.activeMs)
                assertEquals(template, db.routines().find(routineId)); assertEquals(draft, db.sessions().find(draft.session.id))
                assertEquals("Current notes", two.current!!.referenceNotes)
            }
            compose.onNodeWithTag("practice-timer").scrollIfNeeded(); compose.waitForIdle(); captureStage3("routine-03-fresh-practice")
            click("Complete"); shown("Routine A"); click("Skip"); shown("Routine C"); click("Complete")
            shown("Session Summary"); compose.onNodeWithTag("home-brand").performClick(); openHomeRoutines(compose)
            compose.onNodeWithText("Recent Practice").performScrollTo().assertIsDisplayed()
            compose.waitForIdle(); captureStage3("routine-04-history-retained")
        } finally { db.close() }
    }
}

class RoutineReopenTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun coldReopenRetainsTemplateDraftAndIndependentUnfinishedPractice() {
        val db = BlockDatabase.open(compose.activity.applicationContext)
        try {
            openHomeRoutines(compose)
            compose.waitUntil(15000) { compose.onAllNodesWithText("Daily routine").fetchSemanticsNodes().isNotEmpty() }
            runBlocking {
                val routine = db.routines().readAll().single { it.routine.name == "Daily routine" }
                assertEquals(3, routine.placements.size); assertEquals(180_000L, routine.ordered.first().configuredMs)
                assertNotNull(db.sessions().withStatus("DRAFT"))
                val recovery = db.sessions().readAll().single { it.current?.note == "Routine recovery note" }
                assertEquals("IN_PROGRESS", recovery.session.status); assertEquals("Routine B", recovery.current!!.name)
            }
            compose.onNodeWithTag("home-brand").scrollIfNeeded().performClick(); openHomePractice(compose)
            compose.onNodeWithText("Continue Session").scrollIfNeeded().performClick()
            compose.waitUntil(15000) { compose.onAllNodesWithText("Routine B").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Resume").assertExists(); compose.onNodeWithText("Pause").assertDoesNotExist()
            revealPracticeNotes(compose)
            compose.onNodeWithText("Quick note (optional)").performScrollTo().assertTextContains("Routine recovery note")
            compose.waitForIdle(); captureStage3("routine-05-cold-recovery")
            leavePracticeForLater(compose)
            runBlocking { SessionRepository(db).discardDraft() }
        } finally { db.close() }
    }
}
