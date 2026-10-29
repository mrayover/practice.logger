package com.mrayover.practicelogger

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class BlockDeletionWorkflowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun shown(text: String) { compose.waitUntil(15000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() } }
    private fun click(text: String) { shown(text); val node = compose.onNodeWithText(text); if (!node.isDisplayed()) node.performScrollTo(); node.performClick() }
    private fun request() { compose.onNodeWithTag("block-delete-action").performScrollTo().performClick() }
    private fun confirm() { compose.onNodeWithTag("block-delete-confirm").performClick() }
    @Test fun confirmedAndCancelledDeletionActiveArchivedAndLateDependency() {
        val db = BlockDatabase.open(compose.activity.applicationContext)
        try {
            val repo = BlockRepository(db)
            val active = runBlocking { repo.save(null, BlockDraft("Delete active fixture", "Play")) }
            val archived = runBlocking { repo.save(null, BlockDraft("Delete archived fixture", "Play")).also { repo.setArchived(it, true) } }
            val protected = runBlocking { repo.save(null, BlockDraft("Delete protected fixture", "Play")) }
            shown("Blocks"); click("Blocks"); shown("Block Library")
            compose.onNodeWithTag("block-library-list").performScrollToNode(hasText("Delete active fixture"))
            click("Delete active fixture"); shown("Edit Block"); request()
            shown("Delete \"Delete active fixture\" permanently?")
            captureStage3("deletion-01-confirm")
            // Dialog cancel leaves the full persistent snapshot unchanged.
            val before = runBlocking { BackupRepository(db).snapshot() }
            compose.onNode(hasText("Cancel") and hasAnyAncestor(isDialog())).performClick()
            assertEquals(before, runBlocking { BackupRepository(db).snapshot() })
            request(); shown("Delete \"Delete active fixture\" permanently?")
            compose.activityRule.scenario.recreate()
            shown("Delete \"Delete active fixture\" permanently?"); confirm(); shown("Block Library")
            compose.waitUntil(15000) { runBlocking { db.blocks().find(active) == null } }
            compose.onAllNodesWithText("Delete active fixture").assertCountEquals(0)
            compose.onNodeWithTag("block-library-list").performScrollToNode(hasText("Archived")); click("Archived")
            compose.onNodeWithTag("block-library-list").performScrollToNode(hasText("Delete archived fixture"))
            click("Delete archived fixture"); shown("Edit Block"); request()
            shown("Delete \"Delete archived fixture\" permanently?"); confirm(); shown("Block Library")
            compose.waitUntil(15000) { runBlocking { db.blocks().find(archived) == null } }
            compose.onNodeWithTag("block-library-list").performScrollToNode(hasText("Active")); click("Active")
            compose.onNodeWithTag("block-library-list").performScrollToNode(hasText("Delete protected fixture"))
            click("Delete protected fixture"); shown("Edit Block"); request()
            shown("Delete \"Delete protected fixture\" permanently?")
            runBlocking {
                db.sessions().insert(PracticeSession("delete-protected-history", 1, 2, "COMPLETED"))
                db.sessions().insert(Placement("delete-protected-entry", "delete-protected-history", protected,
                    "History snapshot", "Play", null, "", 0, 60000, outcome = "COMPLETED"))
            }
            val protectedBefore = runBlocking { BackupRepository(db).snapshot() }
            confirm(); shown("This Block cannot be deleted yet.")
            compose.onNodeWithText("1 entries belong to completed practice history", substring = true).assertExists()
            captureStage3("deletion-02-protected")
            assertEquals(protectedBefore, runBlocking { BackupRepository(db).snapshot() })
            click("OK")
            request(); shown("This Block cannot be deleted yet."); click("OK")
            assertNotNull(runBlocking { db.blocks().find(protected) })
        } finally { db.close() }
    }
}

class BlockDeletionReopenTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun coldRestartRetainsDeletionAndHistoricalBlock() {
        val db = BlockDatabase.open(compose.activity.applicationContext)
        try { runBlocking {
            val blocks = db.blocks().readAll()
            assertFalse(blocks.any { it.name == "Delete active fixture" || it.name == "Delete archived fixture" })
            val protected = blocks.single { it.name == "Delete protected fixture" }
            assertEquals(1, BlockRepository(db).reviewDeletion(protected.id).dependencies.completedEntries)
            BackupCodec.validate(BackupRepository(db).snapshot())
        } } finally { db.close() }
    }
}
