package com.mrayover.practicelogger

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.UUID

class EarnedBlocksWorkflowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun logAndPlaygroundShareEarnedTotalsAcrossSpendingAndCompleteRestore() {
        val context = compose.activity.application
        val db = BlockDatabase.appInstance(context)
        val repository = EarnedBlocksRepository(db) { 3_000 }
        val color = goalCompletionColor(1)
        runBlocking {
            db.blocks().insert(Block("earned-b", "Current title", "Current exercise", createdAt = 1, updatedAt = 1))
            val goals = GoalRepository(db) { 1_000 }
            val goal = goals.save(null, "Original Goal", "", 0, "")
            goals.setBlocks(goal, listOf("earned-b"))
            db.goalAttribution().setPrimary(BlockPrimaryGoal("earned-b", goal))
            db.sessions().insert(PracticeSession("earned-s", 1_000, status = "IN_PROGRESS"))
            repeat(3) { index ->
                db.sessions().insert(Placement("earned-p$index", "earned-s", "earned-b", "Original Block",
                    "Original exercise", null, "", index, 60_000))
            }
            val sessions = SessionRepository(db) { 2_000 }
            sessions.finishBlock("earned-s", "COMPLETED", "", placementId = "earned-p0")
            db.goalAttribution().clearPrimary("earned-b")
            sessions.finishBlock("earned-s", "COMPLETED", "", placementId = "earned-p1")
            sessions.finishBlock("earned-s", "SKIPPED", "", placementId = "earned-p2")
            val original = db.goals().find(goal)!!.goal
            db.goals().update(original.copy(title = "Updated Goal"))
        }

        compose.onNodeWithText("Log").performScrollTo().performClick()
        compose.onNodeWithText("Earned Blocks").performClick()
        assertCount("earned-count-Lifetime Earned", "2")
        assertCount("earned-count-Red", "1")
        assertCount("earned-count-No Goal", "1")
        val row = hasAnyAncestor(hasTestTag("earned-block-earned-p0"))
        compose.onNode(hasText("Original Goal · Red") and row).performScrollTo().assertIsDisplayed()
        compose.onNode(hasText("Original Block") and row).assertExists()
        compose.onNodeWithTag("earned-block-earned-p2").assertDoesNotExist()
        compose.onNodeWithText("Playground").performScrollTo().performClick()
        compose.onNodeWithTag("playground-inventory").performClick()
        assertCount("playground-count-Lifetime Earned", "2")
        assertCount("playground-count-Used", "0")
        assertCount("playground-count-Available", "2")

        runBlocking { assertEquals("earned-p0", repository.consume("game-action", color)) }
        assertCount("playground-count-Lifetime Earned", "2")
        assertCount("playground-count-Used", "1")
        assertCount("playground-count-Available", "1")
        assertCount("playground-count-Red", "0")
        compose.onNodeWithText("Close").performScrollTo().performClick()
        compose.onNodeWithText("Journal").performClick()
        compose.onNodeWithText("Earned Blocks").performClick()
        assertCount("earned-count-Lifetime Earned", "2")
        assertCount("earned-count-Red", "1")
        compose.onNode(hasText("Original Goal · Red") and row).performScrollTo().assertIsDisplayed()

        runBlocking {
            val name = "earned-restore-${UUID.randomUUID()}"
            val root = File(context.cacheDir, name).apply { mkdirs() }
            val targetFiles = File(root, "target").apply { mkdirs() }
            val archive = File(root, "backup.zip")
            val targetDb = BlockDatabase.open(context, "$name.db")
            try {
                PortableZip(context, db, context.filesDir).create(archive)
                val restore = PortableRestore(context, targetDb, targetFiles)
                restore.restore(restore.stage(archive::inputStream))
                val restored = EarnedBlocksRepository(targetDb)
                assertEquals(repository.history.first(), restored.history.first())
                assertEquals(repository.inventory.first(), restored.inventory.first())
                assertEquals("earned-p0", restored.consume("game-action", color))
                assertTrue(targetDb.playground().readAll().isEmpty())
            } finally {
                targetDb.close()
                context.deleteDatabase("$name.db")
                root.deleteRecursively()
            }
        }
    }

    private fun assertCount(tag: String, count: String) {
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag(tag).fetchSemanticsNodes().singleOrNull()?.config
                ?.getOrNull(SemanticsProperties.Text)?.singleOrNull()?.text == count
        }
        compose.onNodeWithTag(tag).assertTextEquals(count)
    }
}
