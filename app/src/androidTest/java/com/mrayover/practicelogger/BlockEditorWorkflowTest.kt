package com.mrayover.practicelogger

import androidx.compose.ui.test.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

class BlockEditorWorkflowTest : WeekUiChecks() {
    @Before fun reset() { db.clearAllTables() }
    private fun save() {
        compose.waitUntil(15_000) { compose.onAllNodes(hasTestTag("block-save") and isEnabled()).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("block-save").assertIsDisplayed().assertIsEnabled().performClick()
    }
    private fun field(tag: String, value: String) = compose.onNodeWithTag(tag).scrollIfNeeded().performTextReplacement(value)
    private fun choose(role: MediaRole, id: String) {
        val target = compose.onNodeWithTag("block-${role.name}-choose")
        target.performScrollTo()
        captureStage3("editor-before-reference-touch")
        println("EDITOR REFERENCE BOUNDS " + target.fetchSemanticsNode().boundsInRoot)
        println("EDITOR TREE " + compose.onRoot(useUnmergedTree = true).printToString())
        target.assertIsDisplayed().assertIsEnabled().performClick()
        captureStage3("editor-reference-options")
        click("Choose from Library")
        waitForTag("media-library-list")
        compose.onNodeWithTag("media-library-list").performScrollToNode(hasTestTag("media-choose-$id"))
        compose.onNodeWithTag("media-choose-$id").performClick()
        shown("New Block")
    }

    @Test fun firstSaveWithLibraryReferenceValidationKeyboardRecreationAndThemes() {
        val app = compose.activity.application
        val library = MediaLibraryRepository(db, File(app.filesDir, "media-library"))
        val visual = runBlocking { library.import("Editor score.pdf", "application/pdf") { l1Fixture("references/score.pdf").inputStream() } }
        val goal = runBlocking { GoalRepository(db).save(null, "Editor Goal", "", 0, "") }
        click("Blocks"); click("New Block")
        shown("New Block")
        save(); shown("Enter a name.")
        field("Name", "Long Block name ".repeat(18))
        field("Action", "First action\nSecond action ".repeat(10))
        compose.onNodeWithTag("primary-goal-picker").scrollIfNeeded().performClick()
        compose.onNode(hasText("Priority 1 · Editor Goal") and hasAnyAncestor(isPopup())).scrollIfNeeded().performClick()
        choose(MediaRole.VISUAL, visual.id)
        compose.onNodeWithTag("block-VISUAL-title").scrollIfNeeded().assertTextContains("Editor score.pdf", substring = true)
        assertTrue(runBlocking { db.blocks().readAll().isEmpty() })
        for (theme in listOf(ThemeChoice.LIGHT, ThemeChoice.DARK, ThemeChoice.ORCHID)) {
            ThemePreferences(app).save(theme)
            compose.activityRule.scenario.recreate()
            shown("New Block")
            compose.onNodeWithTag("block-save").assertIsDisplayed()
            compose.onNodeWithTag("block-VISUAL-title").scrollIfNeeded().assertExists()
            captureStage3("editor-${theme.name.lowercase()}")
        }
        click("Notes / Source (optional)")
        field("Notes / source (optional)", "Long source text\n".repeat(30))
        compose.onNodeWithTag("block-save").assertIsDisplayed()
        save(); shown("Block Library")
        val block = runBlocking { db.blocks().readAll().single() }
        assertEquals(visual.id, runBlocking { db.blockMedia().find(block.id, "VISUAL")!!.mediaId })
        assertEquals(goal, runBlocking { db.goalAttribution().primaryGoalId(block.id) })
        assertTrue(block.notes.contains("Long source text"))
        openBlockPill(compose, "block-library-list", "library-block-${block.id}")
        click("Edit", "block-library-list")
        field("Name", "Cancelled change")
        compose.onNodeWithTag("block-VISUAL-choose").scrollIfNeeded().performClick()
        click("Continue"); click("Remove reference")
        click("Cancel"); click("Discard")
        shown("Block Library")
        assertEquals(block, runBlocking { db.blocks().find(block.id) })
        assertEquals(visual.id, runBlocking { db.blockMedia().find(block.id, "VISUAL")!!.mediaId })
        assertTrue(library.available(visual))
    }

    @Test fun builderSaveAndCancelPreserveLineupAndReturnOnce() {
        val previous = runBlocking {
            BlockRepository(db).save(null, BlockDraft("Existing builder Block", "Keep order")).also { SessionRepository(db).addBlock(it) }
        }
        val session = runBlocking { db.sessions().withStatus("DRAFT")!!.id }
        val before = runBlocking { db.sessions().find(session)!!.ordered }
        openHomePractice(compose); click("Add Blocks")
        click("+ Add Blocks", "builder-lineup"); click("Create a new Block", "builder-picker")
        field("Name", "Cancel builder editor")
        click("Cancel"); click("Discard")
        shown("Build Session")
        assertEquals(before, runBlocking { db.sessions().find(session)!!.ordered })
        click("+ Add Blocks", "builder-lineup"); click("Create a new Block", "builder-picker")
        field("Name", "Created once"); field("Action", "Play")
        save(); shown("Build Session")
        compose.waitUntil(15_000) { runBlocking { db.sessions().find(session)!!.placements.size == 2 } }
        val after = runBlocking { db.sessions().find(session)!!.ordered }
        assertEquals(previous, after[0].sourceBlockId); assertEquals(before[0], after[0]); assertEquals("Created once", after[1].name)
        compose.activityRule.scenario.recreate(); shown("Build Session")
        assertEquals(after, runBlocking { db.sessions().find(session)!!.ordered })
    }
}
