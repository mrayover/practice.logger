package com.mrayover.practicelogger

import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.text.TextLayoutResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class SessionBuilderWorkflowTest : WeekUiChecks() {
    private fun draft() = runBlocking { db.sessions().find(db.sessions().withStatus("DRAFT")!!.id)!! }
    private fun lineup(label: String) = click(label, "builder-lineup")
    private fun picker(label: String) = click(label, "builder-picker")
    private fun add(id: String, count: Int) {
        compose.onNodeWithTag("builder-picker").performScrollToNode(hasTestTag("builder-add-$id"))
        compose.waitUntil(15_000) {
            compose.onAllNodes(hasTestTag("builder-add-$id") and isEnabled()).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("builder-add-$id").assertIsDisplayed().performClick()
        compose.waitUntil(15_000) { runBlocking { db.sessions().withStatus("DRAFT")?.let { db.sessions().find(it.id)!!.placements.size } } == count }
        compose.onNodeWithText("Done adding Blocks").assertIsDisplayed()
    }
    private fun expand(id: String) {
        compose.onNodeWithTag("builder-lineup").performScrollToNode(hasTestTag("placement-$id"))
        compose.onNodeWithTag("placement-$id").assertIsDisplayed().performClick()
    }
    private fun state(id: String, expected: String) = compose.onNodeWithTag("placement-$id")
        .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, expected))
    private fun back() = compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }

    @Test fun emptyRepeatedPlacementsOverridesReorderingRoutineRecoveryAndReadyGate() {
        val (a, b) = runBlocking {
            val repo = BlockRepository(db)
            repo.save(null, BlockDraft("Builder scales", "Saved action\nSecond action line", "5", "88", "Saved source")) to
                repo.save(null, BlockDraft("Builder chords", "Change cleanly", "7"))
        }
        openHomePractice(compose)
        click("Add Blocks")
        shown("Build Session")
        compose.onNodeWithText("Start Session").assertIsDisplayed().assertIsNotEnabled()
        compose.onNodeWithTag("builder-summary").assertTextEquals("0 Blocks · 0 min planned")
        compose.onNodeWithText("Find a Block").assertDoesNotExist()
        lineup("+ Add Blocks")
        add(a, 1)
        compose.onNodeWithText("Added 1 to session").assertExists()
        add(b, 2)
        add(a, 3)
        click("Done adding Blocks")
        val initial = draft()
        val first = initial.ordered[0].id
        val repeated = initial.ordered[2].id
        assertNotEquals(first, repeated)
        compose.onNodeWithTag("builder-summary").assertTextEquals("3 Blocks · 17 min planned")
        compose.onNodeWithText("Start Session").assertIsDisplayed().assertIsEnabled()
        expand(first)
        state(first, "Expanded")
        lineup("Change duration")
        compose.onNodeWithText("Minutes").performTextReplacement("0")
        click("Apply duration")
        shown("Enter positive whole minutes (up to 2147483647).")
        compose.onNodeWithText("Minutes").performTextReplacement("2")
        click("Apply duration")
        compose.waitUntil(15_000) { draft().ordered.first().configuredMs == 120_000L }
        compose.onNodeWithTag("builder-lineup").performScrollToIndex(0)
        compose.onNodeWithTag("builder-summary").assertTextEquals("3 Blocks · 14 min planned")
        assertEquals(5, runBlocking { db.blocks().find(a)!!.durationMinutes })
        assertEquals(300_000L, draft().ordered.last().configuredMs)
        expand(first)
        state(first, "Collapsed")
        expand(repeated)
        lineup("Up")
        compose.waitUntil(15_000) { draft().ordered[1].id == repeated }
        state(repeated, "Expanded")
        lineup("Down")
        compose.waitUntil(15_000) { draft().ordered[2].id == repeated }
        lineup("Remove from Session")
        compose.waitUntil(15_000) { draft().placements.size == 2 }
        assertEquals(listOf(a, b), draft().ordered.map { it.sourceBlockId })
        lineup("Save as Routine")
        compose.onNodeWithText("Routine name").performTextInput("Builder saved")
        click("Save Routine")
        shown("Routine saved. Your session draft is unchanged.")
        assertEquals(listOf(120_000L, 420_000L), runBlocking { db.routines().readAll().single().ordered.map { it.configuredMs } })
        val saved = draft()
        back()
        shown("Practice Now")
        click("Add Blocks")
        assertEquals(saved, draft())
        compose.activityRule.scenario.recreate()
        shown("Build Session")
        assertEquals(saved, draft())
        lineup("+ Add Blocks")
        back()
        shown("Build Session")
        assertEquals(saved, draft())
        click("Start Session")
        shown("Start Block")
        compose.onNodeWithText("Prep Block").assertIsDisplayed()
        assertEquals("IN_PROGRESS", runBlocking { db.sessions().find(saved.session.id)!!.session.status })
        assertEquals(0L, runBlocking { db.sessions().find(saved.session.id)!!.activeMs })
        click("Prep Block")
        shown("Start Practice")
        assertEquals(0L, runBlocking { db.sessions().find(saved.session.id)!!.activeMs })
        click("Start Practice")
        shown("Pause")
        click("Pause")
        shown("Resume")
    }

    @Test fun activeSearchGoalPickerCreateReturnAndConfirmedDiscard() {
        val (a, b, archived) = runBlocking {
            val goal = GoalRepository(db).save(null, "Builder tone", "", 0, "")
            val repo = BlockRepository(db)
            val a = repo.save(null, BlockDraft("Search alpha", "Match action", primaryGoalId = goal, primaryGoalChanged = true))
            val b = repo.save(null, BlockDraft("Search beta", "Other action"))
            val old = repo.save(null, BlockDraft("Search archived", "Match action"))
            repo.setArchived(old, true)
            Triple(a, b, old)
        }
        openHomePractice(compose)
        click("Add Blocks")
        lineup("+ Add Blocks")
        compose.onNodeWithTag("builder-picker").performScrollToNode(hasText("Find a Block"))
        compose.onNodeWithText("Find a Block").performTextInput("Match action")
        compose.onNodeWithTag("builder-picker").performScrollToNode(hasTestTag("builder-add-$a"))
        compose.onNodeWithTag("builder-add-$a").assertExists()
        compose.onNodeWithTag("builder-add-$b").assertDoesNotExist()
        compose.onNodeWithTag("builder-add-$archived").assertDoesNotExist()
        compose.onNodeWithText("Done adding Blocks").assertIsDisplayed()
        compose.onNodeWithTag("builder-picker").performScrollToNode(hasText("Find a Block"))
        compose.onNodeWithText("Find a Block").performTextClearance()
        compose.onNodeWithTag("builder-picker").performScrollToNode(hasTestTag("block-goal-filter"))
        compose.onNodeWithTag("block-goal-filter").performClick()
        compose.onNode(hasText("Priority 1 · Builder tone") and hasAnyAncestor(isPopup())).scrollIfNeeded().performClick()
        add(a, 1)
        compose.onNodeWithTag("builder-add-$b").assertDoesNotExist()
        picker("Create a new Block")
        shown("New Block")
        compose.onNodeWithText("Name").scrollIfNeeded().performTextInput("Created in builder")
        compose.onNodeWithText("Action").scrollIfNeeded().performTextInput("Created action")
        click("Save")
        shown("Build Session")
        compose.waitUntil(15_000) { draft().placements.size == 2 }
        assertEquals(listOf("Search alpha", "Created in builder"), draft().ordered.map { it.name })
        lineup("Discard draft session")
        click("Keep draft")
        assertEquals(2, draft().placements.size)
        lineup("Discard draft session")
        click("Discard draft")
        shown("Practice Now")
        assertNull(runBlocking { db.sessions().withStatus("DRAFT") })
    }

    @Test fun librarySnapshotsCompactTextGoalColorsAndIndependentExpansionAcrossThemes() {
        val name = "Long saved snapshot title ".repeat(8).trim()
        val action = "First saved action line ".repeat(8).trim() + "\nFull saved action"
        val block = runBlocking {
            val goal = GoalRepository(db).save(null, "Snapshot Goal", "", 0, "")
            BlockRepository(db).save(null, BlockDraft(name, action, "4", "96", "Snapshot notes", goal, true))
        }
        click("Blocks")
        openBlockPill(compose, "block-library-list", "library-block-$block")
        click("Add to Practice", "block-library-list")
        shown("Build Session")
        val first = draft().ordered.single().id
        runBlocking {
            BlockRepository(db).save(block, BlockDraft("Changed library title", "Changed library action", "9", "120", "Changed notes"))
            SessionRepository(db).addBlock(block)
        }
        compose.waitUntil(15_000) { draft().placements.size == 2 }
        val second = draft().ordered.last().id
        for (theme in listOf(ThemeChoice.LIGHT, ThemeChoice.DARK)) {
            ThemePreferences(compose.activity.applicationContext).save(theme)
            compose.activityRule.scenario.recreate()
            shown("Build Session")
            compose.onNodeWithTag("builder-lineup").performScrollToIndex(0)
            compose.onNodeWithTag("builder-summary").assertTextEquals("2 Blocks · 13 min planned")
            compose.onNodeWithText("Start Session").assertIsDisplayed()
            for ((text, lines) in listOf("1. $name" to 2, action.lineSequence().first() to 1)) {
                val layouts = mutableListOf<TextLayoutResult>()
                compose.onNodeWithText(text, useUnmergedTree = true).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
                assertEquals(lines, layouts.single().lineCount)
                assertTrue(layouts.single().isLineEllipsized(lines - 1))
            }
            compose.onNodeWithText("4:00 · 96 BPM").assertExists()
            val marker = compose.onNode(hasTestTag("placement-goal-marker") and hasAnyAncestor(hasTestTag("placement-$first")), useUnmergedTree = true)
            val pixels = marker.captureToImage().toPixelMap()
            assertEquals(resolvePalette(theme, false).colors.goalPriorities[0], pixels[pixels.width / 2, pixels.height / 2])
            expand(first)
            lineup("Snapshot notes")
            expand(second)
            state(second, "Expanded")
            expand(second)
            state(second, "Collapsed")
            compose.onNodeWithTag("builder-lineup").performScrollToNode(hasTestTag("placement-$first"))
            state(first, "Expanded")
            expand(first)
        }
        back()
        shown("Block Library")
        assertEquals(name, draft().ordered.first().name)
        assertEquals(action, draft().ordered.first().action)
    }
}
