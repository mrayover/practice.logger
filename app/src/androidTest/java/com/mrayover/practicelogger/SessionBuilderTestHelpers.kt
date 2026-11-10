package com.mrayover.practicelogger

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import kotlinx.coroutines.runBlocking

internal fun addBuilderBlock(compose: AndroidComposeTestRule<*, MainActivity>, name: String) {
    val db = BlockDatabase.appInstance(compose.activity.applicationContext)
    val count = runBlocking { db.sessions().withStatus("DRAFT")?.let { db.sessions().find(it.id)!!.placements.size } ?: 0 }
    compose.onNodeWithTag("builder-lineup").performScrollToNode(hasText("+ Add Blocks"))
    compose.onNodeWithText("+ Add Blocks").performClick()
    compose.onNodeWithTag("builder-picker").performScrollToNode(hasContentDescription("Add $name"))
    compose.waitUntil(15_000) {
        compose.onAllNodes(hasContentDescription("Add $name") and isEnabled()).fetchSemanticsNodes().isNotEmpty()
    }
    compose.onNodeWithContentDescription("Add $name").assertIsDisplayed().performClick()
    compose.waitUntil(15_000) {
        runBlocking { db.sessions().withStatus("DRAFT")?.let { db.sessions().find(it.id)!!.placements.size } } == count + 1
    }
    compose.onNodeWithText("Done adding Blocks").performClick()
}

internal fun expandBuilderPlacement(compose: AndroidComposeTestRule<*, MainActivity>, index: Int) {
    val db = BlockDatabase.appInstance(compose.activity.applicationContext)
    val id = runBlocking { db.sessions().find(db.sessions().withStatus("DRAFT")!!.id)!!.ordered[index].id }
    val tag = "placement-$id"
    compose.onNodeWithTag("builder-lineup").performScrollToNode(hasTestTag(tag))
    val header = compose.onNodeWithTag(tag).assertIsDisplayed()
    if (header.fetchSemanticsNode().config[SemanticsProperties.StateDescription] == "Collapsed") header.performClick()
}
