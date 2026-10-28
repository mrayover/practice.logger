package com.mrayover.practicelogger

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test

class LaunchTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun shown(text: String) { compose.waitUntil(10000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() } }
    private fun openSmoke() {
        // A recreated text editor can leave IME covering the library's small viewport on API26.
        compose.activityRule.scenario.onActivity { activity ->
            val input = activity.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            input.hideSoftInputFromWindow(activity.window.decorView.windowToken, 0)
        }
        compose.waitUntil(10000) {
            var hidden = false
            compose.activityRule.scenario.onActivity { activity ->
                hidden = androidx.core.view.ViewCompat.getRootWindowInsets(activity.window.decorView)
                    ?.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime()) != true
            }
            hidden
        }
        compose.onNodeWithTag("block-library").performScrollToNode(hasText("Restart smoke"))
        compose.onNodeWithText("Restart smoke").assertIsDisplayed().performClick()
        shown("Edit Block")
    }
    @Test fun catalogWorkflowAndDraftRecovery() {
        shown("Practice Now")
        compose.onNodeWithText("Goals").assertExists()
        compose.onNodeWithTag("goals-placeholder").assertExists()
        compose.onNodeWithText("Edit").assertIsNotEnabled()
        captureStage3("00-home")
        compose.onNodeWithText("Recording").performScrollTo().performClick()
        shown("Choose a Block")
        compose.onNodeWithText("Your Blocks will appear here.").assertExists()
        compose.onNodeWithText("Back to Home").performClick()
        openHomeTools(compose)
        compose.onNodeWithText("Back to Home").performClick()
        openHomeRoutines(compose)
        compose.onNodeWithText("Back to Home").performScrollTo().performClick()
        compose.onNodeWithText("Practice Now").performScrollTo().performClick()
        shown("Build a Session")
        compose.onNodeWithText("Back to Home").performScrollTo().performClick()
        shown("Blocks")
        compose.onNodeWithText("Blocks").performScrollTo().performClick()
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
        openSmoke()
        shown("Default duration (minutes)")
        compose.onNodeWithTag("Default duration (minutes)").performScrollTo().performTextReplacement("8")
        compose.onNodeWithText("Save").performScrollTo().performClick()
        shown("Block Library")
        shown("8 min")
        compose.onNodeWithText("Search Blocks").performTextInput("nonexistent")
        compose.onNodeWithText("No matching Blocks.").assertIsDisplayed()
        compose.onNodeWithText("Search Blocks").performTextReplacement("bridge")
        openSmoke()
        compose.onNodeWithText("Archive Block").performScrollTo().performClick()
        compose.onNodeWithText("Archive").performClick()
        shown("Block Library")
        shown("Restart smoke")
        compose.onNodeWithText("Active", useUnmergedTree = true).performClick()
        compose.onNodeWithText("No Blocks yet. Create your first Block.").assertExists()
        compose.onNodeWithText("Archived", useUnmergedTree = true).performClick()
        openSmoke()
        compose.onNodeWithText("Restore Block").performScrollTo().performClick()
        compose.onNodeWithText("Restore", substring = false).performClick()
        shown("Block Library")
        shown("Restart smoke")
        openSmoke()
        compose.onNodeWithTag("Name").performTextReplacement("Discard me")
        compose.onNodeWithText("Cancel").performScrollTo().performClick()
        compose.onNodeWithText("Keep editing").performClick()
        compose.onNodeWithTag("Name").performScrollTo().assertTextContains("Discard me")
        compose.onNodeWithText("Cancel").performScrollTo().performClick()
        compose.onNodeWithText("Discard", substring = false).performClick()
        shown("Block Library")
        compose.onNodeWithText("Restart smoke").assertExists()
        compose.onNodeWithText("Discard me").assertDoesNotExist()
        compose.onNodeWithText("Back to Home").performClick()
        compose.onNodeWithText("Recording").performScrollTo().performClick()
        shown("Choose a Block")
        compose.onNodeWithText("Restart smoke").performScrollTo().performClick()
        shown("Block recordings")
        compose.onNodeWithText("Close recordings").performScrollTo().performClick()
        compose.onNodeWithText("Back to Home").performScrollTo().performClick()
    }
}
