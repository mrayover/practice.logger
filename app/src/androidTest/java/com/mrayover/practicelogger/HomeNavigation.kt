package com.mrayover.practicelogger

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.AndroidComposeTestRule

/** Explicit home-menu routes shared by the existing end-to-end workflows. */
fun openHomeRoutines(compose: AndroidComposeTestRule<*, MainActivity>) {
    compose.waitUntil(15000) { compose.onAllNodesWithTag("home-brand").fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty() }
    compose.onNodeWithText("Routines").performScrollTo().performClick()
    compose.waitUntil(15000) { compose.onAllNodesWithText("Recent Practice").fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty() }
}

fun openHomeTools(compose: AndroidComposeTestRule<*, MainActivity>) {
    compose.waitUntil(15000) { compose.onAllNodesWithTag("home-brand").fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty() }
    compose.onNodeWithText("Tools").performScrollTo().performClick()
    compose.waitUntil(15000) { compose.onAllNodesWithText("Backup and restore").fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty() }
}
