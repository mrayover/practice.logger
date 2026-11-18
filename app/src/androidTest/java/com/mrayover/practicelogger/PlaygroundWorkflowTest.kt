package com.mrayover.practicelogger

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class PlaygroundWorkflowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val db get() = BlockDatabase.appInstance(compose.activity.application)
    private val playerColor = 0xFF20323B.toInt()
    private val device get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @Test fun fixedHomeMovementAndLiveReadOnlyInventory() {
        val savedLayout = PlaygroundLayout("pg0", -2.0, 1.25, 45.0)
        runBlocking {
            db.blocks().insert(Block("pg-block", "Current name", "Current instruction", createdAt = 1, updatedAt = 1))
            db.sessions().insert(PracticeSession("pg-session", 1000, 2000, "COMPLETED", 8))
            repeat(8) { index ->
                db.sessions().insert(Placement("pg$index", "pg-session", "pg-block", "Historical Block", "", null,
                    "", index, 60000, 42000, if (index == 7) "SKIPPED" else "COMPLETED"))
            }
            repeat(6) { index -> db.sessions().insert(completion(index)) }
            db.playground().save(savedLayout)
        }
        val original = runBlocking { db.playground().completions() }

        compose.onNodeWithText("Log").performScrollTo().performClick()
        compose.onNodeWithText("Playground", substring = false).performClick()
        compose.onNodeWithText("Blocks 6").assertIsDisplayed()
        val page = bounds("playground-home")
        val viewport = bounds("playground-canvas")
        val controls = bounds("playground-controls")
        assertTrue("Gameplay takes the largest portion of the fixed page", viewport.height > page.height * 0.48f)
        assertTrue("Gameplay uses the available width", viewport.width > page.width * 0.90f)
        assertTrue("Controls sit below gameplay", controls.top > viewport.bottom)
        val tags = listOf("canvas", "inventory", "explore", "up", "down", "left", "right", "a", "b")
        val fixedBounds = tags.associateWith { bounds("playground-$it") }
        val appBounds = bounds("app-surface")
        fixedBounds.values.forEach { rect ->
            assertTrue(rect.left >= appBounds.left && rect.right <= appBounds.right &&
                rect.top >= appBounds.top && rect.bottom <= appBounds.bottom)
        }
        compose.onAllNodes(SemanticsMatcher.keyIsDefined(androidx.compose.ui.semantics.SemanticsActions.ScrollBy))
            .assertCountEquals(0)
        listOf("Stats", "Workshop", "Kitchen").forEach { compose.onNodeWithText(it).assertIsNotEnabled() }
        node("playground-explore").assertIsNotEnabled()
        node("playground-b").assertIsNotEnabled()
        screenshot("playground-home")

        val before = playerPosition()
        node("playground-canvas").performTouchInput { swipeUp(); swipeLeft() }
        assertEquals(before, playerPosition())
        assertEquals(viewport, bounds("playground-canvas"))
        node("playground-a").assertIsEnabled().performClick()
        assertEquals(before, playerPosition())

        compose.mainClock.autoAdvance = false
        try {
            val right = hold("right", 470)
            assertTrue(right.x > before.x)
            assertEquals(before.y, right.y, 0f)
            val up = hold("up", 320)
            assertTrue(up.y < right.y)
            assertEquals(right.x, up.x, 0f)
            val left = hold("left", 260, cancelled = true)
            assertTrue(left.x < up.x)
            assertEquals(up.y, left.y, 0f)
            val down = hold("down", 200)
            assertTrue(down.y > left.y)
            assertEquals(left.x, down.x, 0f)

            val rightWall = hold("right", 2600)
            assertEquals(rightWall, hold("right", 320))
            val topWall = hold("up", 3100)
            assertEquals(topWall, hold("up", 320))
            assertEquals(rightWall.x, topWall.x, 0f)
            assertTrue(viewport.width > topWall.x && topWall.y >= 0)
        } finally {
            compose.mainClock.autoAdvance = true
        }
        val stopped = playerPosition()

        node("playground-inventory").performClick()
        node("playground-inventory-panel").assertIsDisplayed()
        listOf("Red", "Orange", "Yellow", "Green", "Blue", "Neutral").forEach { assertCount(it, 1) }
        assertCount("Total available", 6)
        screenshot("playground-inventory")
        runBlocking { db.sessions().insert(completion(6)) }
        compose.waitUntil(10_000) {
            node("playground-count-Total available").fetchSemanticsNode().config
                .getOrNull(androidx.compose.ui.semantics.SemanticsProperties.Text)?.singleOrNull()?.text == "7"
        }
        assertCount("Neutral", 2)
        compose.onNodeWithText("Close").performClick()
        compose.onNodeWithText("Blocks 7").assertIsDisplayed()
        node("playground-inventory-panel").assertDoesNotExist()
        assertEquals(fixedBounds, tags.associateWith { bounds("playground-$it") })
        assertEquals(stopped, playerPosition())
        node("playground-inventory").performClick()
        device.pressBack()
        node("playground-inventory-panel").assertDoesNotExist()
        node("playground-home").assertIsDisplayed()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("Blocks 7").assertIsDisplayed()
        compose.onNodeWithText("Journal").performClick()
        compose.onNodeWithText("Playground").assertIsDisplayed()
        assertEquals(listOf(savedLayout), runBlocking { db.playground().readAll() })
        assertEquals(original + completion(6), runBlocking { db.playground().completions() })
    }

    private fun completion(index: Int): BlockCompletion {
        // Saved rank intentionally disagrees with saved color. The inventory reads the color.
        val goalId = if (index < 5) "historical-goal" else null
        return BlockCompletion("pg$index", "pg-session", "pg-block", 2000L + index, goalId,
            goalId?.let { 1 }, if (index < 5) goalCompletionColor(index + 1) else null)
    }

    private fun hold(direction: String, millis: Long, cancelled: Boolean = false): Offset {
        val button = node("playground-$direction")
        button.performTouchInput { down(center) }
        compose.mainClock.advanceTimeBy(millis)
        val moved = playerPosition()
        button.performTouchInput { if (cancelled) cancel() else up() }
        compose.mainClock.advanceTimeBy(220)
        assertEquals("Movement stops after release or cancellation", moved, playerPosition())
        return moved
    }

    private fun node(tag: String) = compose.onNodeWithTag(tag)
    private fun bounds(tag: String): Rect = node(tag).assertIsDisplayed().fetchSemanticsNode().boundsInRoot

    private fun assertCount(label: String, count: Int) {
        node("playground-count-$label").assertIsDisplayed().assertTextEquals(count.toString())
    }

    private fun playerPosition(): Offset {
        val pixels = node("playground-canvas").captureToImage().toPixelMap()
        for (y in 0 until pixels.height) {
            for (x in 0 until pixels.width) {
                if (pixels[x, y].toArgb() == playerColor) return Offset(x.toFloat(), y.toFloat())
            }
        }
        error("The player rectangle is missing from the viewport")
    }

    private fun screenshot(name: String) {
        val folder = File(compose.activity.getExternalFilesDir(null), "screenshots").apply { mkdirs() }
        assertTrue(device.takeScreenshot(File(folder, "$name.png")))
    }
}
