package com.mrayover.practicelogger

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull

/** Explicit home-menu routes shared by the existing end-to-end workflows. */
fun openHomeRoutines(compose: AndroidComposeTestRule<*, MainActivity>) {
    compose.waitUntil(15000) { compose.onAllNodesWithTag("home-menu").fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty() }
    compose.onNodeWithText("Routines").scrollIfNeeded().performClick()
    compose.waitUntil(15000) { compose.onAllNodesWithText("Recent Practice").fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty() }
}

fun openHomeTools(compose: AndroidComposeTestRule<*, MainActivity>) {
    compose.waitUntil(15000) { compose.onAllNodesWithTag("home-menu").fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty() }
    compose.onNodeWithText("Tools").scrollIfNeeded().performClick()
    compose.waitUntil(15000) { compose.onAllNodesWithText("Backup and restore").fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty() }
}

fun openHomePractice(compose: AndroidComposeTestRule<*, MainActivity>) {
    compose.waitUntil(15000) { compose.onAllNodesWithTag("home-menu").fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty() }
    compose.onNodeWithText("Practice Now").scrollIfNeeded().performClick()
    compose.waitUntil(15000) { compose.onAllNodesWithTag("practice-now-title").fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty() }
}

fun continueFirstSession(compose: AndroidComposeTestRule<*, MainActivity>) {
    compose.onNodeWithText("In Progress").scrollIfNeeded().performClick()
    compose.onAllNodes(SemanticsMatcher("saved session") {
        it.config.getOrNull(SemanticsProperties.TestTag)?.startsWith("continue-session-") == true
    }).onFirst().scrollIfNeeded().performClick()
}

/** Enter the existing workspace for tests that need tools before measured practice. */
fun prepReadyBlock(compose: AndroidComposeTestRule<*, MainActivity>) {
    compose.waitUntil(15000) {
        compose.onAllNodesWithTag("practice-ready-prep").fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()
    }
    compose.onNodeWithTag("practice-ready-prep").scrollIfNeeded().assertIsDisplayed().performClick()
}

/** Compact references expose reset through accessibility while preserving real zoom state. */
fun assertPracticeReferenceZoomed(compose: AndroidComposeTestRule<*, MainActivity>) {
    compose.waitUntil(10000) {
        val viewer = compose.onAllNodesWithTag("reference-viewer").fetchSemanticsNodes(atLeastOneRootRequired = false).singleOrNull()
        val description = viewer?.config?.get(SemanticsProperties.ContentDescription)?.singleOrNull().orEmpty()
        (description.substringAfterLast("zoom ").replace(',', '.').toFloatOrNull() ?: 0f) > 1f
    }
    val config = compose.onNodeWithTag("reference-viewer").assertIsDisplayed().fetchSemanticsNode().config
    org.junit.Assert.assertTrue("Zoom reset must remain accessible", config[SemanticsActions.CustomActions].any { it.label == "Reset zoom" })
}

fun resetPracticeReferenceZoom(compose: AndroidComposeTestRule<*, MainActivity>) {
    val action = compose.onNodeWithTag("reference-viewer").assertIsDisplayed().fetchSemanticsNode()
        .config[SemanticsActions.CustomActions].single { it.label == "Reset zoom" }
    compose.runOnIdle { org.junit.Assert.assertTrue("Reset zoom accessibility action must succeed", action.action()) }
    compose.waitForIdle()
}
