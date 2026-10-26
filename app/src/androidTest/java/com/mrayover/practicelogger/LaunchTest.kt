package com.mrayover.practicelogger

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test

class LaunchTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun shown(text: String) { compose.waitUntil(10000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() } }
    @Test fun catalogWorkflowAndDraftRecovery() {
        shown("Manage Blocks")
        compose.onNodeWithText("Manage Blocks").performClick()
        compose.onNodeWithText("New Block").performClick()
        compose.onNodeWithText("Save").performScrollTo().performClick()
        compose.onNodeWithText("Enter a name.").assertExists()
        compose.onNodeWithTag("Name").performScrollTo().performTextInput("Restart smoke")
        compose.onNodeWithTag("Action").performScrollTo().performTextInput("Play the bridge from memory")
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("Name").assertTextContains("Restart smoke")
        compose.onNodeWithText("Save").performScrollTo().performClick()
        compose.activityRule.scenario.recreate()
        shown("Block Library")
        shown("Restart smoke")
        compose.onNodeWithText("Restart smoke").performClick()
        compose.onNodeWithTag("Default duration (minutes)").performScrollTo().performTextReplacement("8")
        compose.onNodeWithText("Save").performScrollTo().performClick()
        shown("Block Library")
        shown("8 min")
        compose.onNodeWithText("Search Blocks").performTextInput("nonexistent")
        compose.onNodeWithText("No matching Blocks.").assertIsDisplayed()
        compose.onNodeWithText("Search Blocks").performTextReplacement("bridge")
        compose.onNodeWithText("Restart smoke").assertExists().performClick()
        compose.onNodeWithText("Archive Block").performScrollTo().performClick()
        compose.onNodeWithText("Archive").performClick()
        shown("Block Library")
        shown("Restart smoke")
        compose.onNodeWithText("Active", useUnmergedTree = true).performClick()
        compose.onNodeWithText("No Blocks yet. Create your first Block.").assertExists()
        compose.onNodeWithText("Archived", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Restart smoke").performClick()
        compose.onNodeWithText("Restore Block").performScrollTo().performClick()
        compose.onNodeWithText("Restore", substring = false).performClick()
        shown("Block Library")
        shown("Restart smoke")
        compose.onNodeWithText("Restart smoke").performClick()
        compose.onNodeWithTag("Name").performTextReplacement("Discard me")
        compose.onNodeWithText("Cancel").performScrollTo().performClick()
        compose.onNodeWithText("Keep editing").performClick()
        compose.onNodeWithTag("Name").performScrollTo().assertTextContains("Discard me")
        compose.onNodeWithText("Cancel").performScrollTo().performClick()
        compose.onNodeWithText("Discard", substring = false).performClick()
        shown("Block Library")
        compose.onNodeWithText("Restart smoke").assertExists()
        compose.onNodeWithText("Discard me").assertDoesNotExist()
    }
}
