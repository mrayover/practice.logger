package com.mrayover.practicelogger

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import kotlin.math.*

class PlaygroundWorkflowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val db get() = BlockDatabase.appInstance(compose.activity.application)
    private fun canvas() = compose.onNodeWithTag("playground-canvas")
    private fun waitFor(tag: String) = compose.waitUntil(15000) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty() }
    private fun rows() = runBlocking { db.playground().readAll() }
    private fun overview() { compose.onNodeWithTag("playground-overview").performClick(); compose.waitForIdle() }
    private fun location(x: Double, y: Double): Offset {
        val bounds = canvas().fetchSemanticsNode().boundsInRoot
        val world = playgroundWorld(3)
        val scale = min(bounds.width / world.width, bounds.height / world.height) * 0.9
        return Offset((bounds.width / 2 + x * scale).toFloat(), (bounds.height / 2 + y * scale).toFloat())
    }
    @Test fun earnedCanvasSelectionDragTwistZoomPanAndRecreation() {
        val folder = File(compose.activity.getExternalFilesDir(null), "home-preview").apply { mkdirs() }
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        try {
            runBlocking {
                db.blocks().insert(Block("pg-block", "Current name", "Current action", createdAt = 1, updatedAt = 1))
                db.sessions().insert(PracticeSession("pg-session", 1000, 2000, "COMPLETED", 3))
                repeat(3) { i ->
                    db.sessions().insert(Placement("pg$i", "pg-session", "pg-block", "Historical square $i", "Historical instruction", null, "", i, 60000, 42000, "COMPLETED"))
                    db.sessions().insert(BlockCompletion("pg$i", "pg-session", "pg-block", 2000 + i.toLong(), if (i == 0) "historical-goal" else null, if (i == 0) 1 else null, if (i == 0) 0xff4a9165.toInt() else null))
                }
                db.sessions().save(BlockReflection("pg0", 4, focus = "Historical reflection"))
                db.playground().save(PlaygroundLayout("pg0", -2.0, 0.0, 0.0))
                db.playground().save(PlaygroundLayout("pg1", 0.0, 0.0, 0.0))
                db.playground().save(PlaygroundLayout("pg2", 3.0, 2.0, 45.0))
            }
            compose.onNodeWithText("Log").performScrollTo().performClick()
            compose.onNodeWithText("Playground", substring = false).performClick()
            waitFor("playground-canvas"); compose.waitForIdle()
            fun paintedWidth(): Int {
                val image = canvas().captureToImage().toPixelMap()
                return (0 until image.width).count { image[it, image.height / 2].toArgb() == 0xff4a9165.toInt() }
            }
            val defaultWidth = paintedWidth()
            overview()
            val overviewWidth = paintedWidth()
            assertEquals(overviewWidth * 1.2, defaultWidth.toDouble(), 2.0)
            val pixels = canvas().captureToImage().toPixelMap()
            fun pixel(x: Double, y: Double): Int { val p = location(x, y); return pixels[p.x.toInt(), p.y.toInt()].toArgb() }
            val background = pixel(-5.0, -2.0)
            assertEquals(0xff4a9165.toInt(), pixel(-2.0, 0.0))
            for ((x, y) in listOf(-2.0 to 0.0, 0.0 to 0.0, 3.0 to 2.0)) assertNotEquals("Every earned square must be visible", background, pixel(x, y))
            // Same viewport, world-anchored dots and external selection halo in every palette.
            for (theme in ThemeChoice.entries.filter { it != ThemeChoice.SYSTEM }) {
                compose.runOnIdle { compose.activity.themePreferences.save(theme) }; compose.waitForIdle()
                overview()
                val image = canvas().captureToImage().toPixelMap()
                fun colorAt(x: Double, y: Double): androidx.compose.ui.graphics.Color {
                    val point = location(x, y)
                    return image[point.x.roundToInt(), point.y.roundToInt()]
                }
                val dot = colorAt(-4.0, -2.0); val plain = colorAt(-4.2, -2.2)
                assertNotEquals("Dots visible in $theme", plain.toArgb(), dot.toArgb())
                assertTrue("Dots remain faint", abs(dot.red - plain.red) < 0.2f && abs(dot.green - plain.green) < 0.2f && abs(dot.blue - plain.blue) < 0.2f)
                canvas().performTouchInput { click(location(-2.0, 0.0)) }; waitFor("playground-details"); overview()
                device.takeScreenshot(File(folder, "pg11-selected-" + theme.id + ".png"))
                canvas().performTouchInput { click(location(-4.2, -2.2)) }
                compose.onNodeWithTag("playground-details").assertDoesNotExist()
            }
            compose.runOnIdle { compose.activity.themePreferences.save(ThemeChoice.LIGHT) }; compose.waitForIdle(); overview()
            val stationary = rows().filter { it.placementId != "pg0" }
            canvas().performTouchInput { click(location(-2.0, 0.0)) }
            waitFor("playground-details")
            compose.onNodeWithText("Historical square 0").assertIsDisplayed()
            compose.onNodeWithTag("playground-expand").performClick()
            compose.onNodeWithText("Focus: Historical reflection").performScrollTo().assertIsDisplayed()
            compose.onNodeWithTag("playground-expand").performScrollTo().performClick()
            overview()
            // Remote drag is relative to the square, never to the empty-space start.
            overview()
            val remoteBefore = rows().first { it.placementId == "pg0" }
            val start = location(-4.0, -2.0)
            val end = location(-4.0, -1.0)
            canvas().performTouchInput { swipe(start, end, 300) }
            compose.waitUntil(15000) { rows().first { it.placementId == "pg0" }.y > 0.5 }
            val remoteAfter = rows().first { it.placementId == "pg0" }
            assertEquals(remoteBefore.x, remoteAfter.x, 0.01)
            assertTrue(remoteAfter.y in 0.5..1.05)
            compose.onNodeWithTag("playground-details").assertExists()
            assertEquals(stationary, rows().filter { it.placementId != "pg0" })
            // Restore the fixture for the obstacle test through the same arrangement repository.
            runBlocking { PlaygroundRepository(db).save(remoteBefore) }
            compose.waitForIdle(); overview()
            // Fast drag crosses the obstacle's complete width; swept geometry must stop at x=-1.
            canvas().performTouchInput { swipe(location(-4.0, -2.0), location(1.0, -2.0), 100) }
            compose.waitUntil(15000) { abs(rows().first { it.placementId == "pg0" }.x + 1.0) < 0.01 }
            assertEquals(stationary, rows().filter { it.placementId != "pg0" })
            compose.onNodeWithTag("playground-45").performClick()
            compose.onNodeWithTag("playground-orientation-message").assertIsDisplayed()
            assertEquals(0.0, rows().first { it.placementId == "pg0" }.rotationDegrees, 0.0)
            overview()
            canvas().performTouchInput { swipe(location(-1.0, 0.0), location(-2.5, 0.0), 300) }
            compose.waitUntil(15000) { rows().first { it.placementId == "pg0" }.x < -2.0 }
            overview()
            compose.onNodeWithTag("playground-45").performClick()
            compose.waitUntil(15000) { rows().first { it.placementId == "pg0" }.rotationDegrees == 45.0 }
            compose.onNodeWithTag("playground-upright").performClick()
            compose.waitUntil(15000) { rows().first { it.placementId == "pg0" }.rotationDegrees == 0.0 }
            val beforeTwist = rows().first { it.placementId == "pg0" }
            val center = location(beforeTwist.x, beforeTwist.y)
            canvas().performTouchInput {
                down(0, center + Offset(-5f, 0f)); down(1, center + Offset(5f, 0f))
                for (step in 1..12) {
                    val radians = step * PI / 72
                    updatePointerTo(0, center + Offset((-5 * cos(radians)).toFloat(), (-5 * sin(radians)).toFloat()))
                    updatePointerTo(1, center + Offset((5 * cos(radians)).toFloat(), (5 * sin(radians)).toFloat()))
                    move(20)
                }
                up(0); up(1)
            }
            compose.waitUntil(15000) { rows().first { it.placementId == "pg0" }.rotationDegrees > 10 }
            val arranged = rows()
            assertEquals(stationary, arranged.filter { it.placementId != "pg0" })
            validatePlaygroundLayouts(arranged, setOf("pg0", "pg1", "pg2"))
            // An empty-space pinch/pan changes only the viewport and retains selection.
            canvas().performTouchInput { pinch(Offset(width * .3f, height * .2f), Offset(width * .5f, height * .2f), Offset(width * .2f, height * .2f), Offset(width * .6f, height * .2f)) }
            compose.onNodeWithTag("playground-details").assertExists()
            assertEquals(arranged, rows())
            device.takeScreenshot(File(folder, "pg1-selected.png"))
            compose.activityRule.scenario.recreate(); waitFor("playground-canvas")
            assertEquals(arranged, rows())
            overview(); canvas().performTouchInput { click(location(arranged.first { it.placementId == "pg0" }.x, 0.0)) }
            waitFor("playground-details")
            canvas().performTouchInput { click(Offset(width * .1f, height * .1f)) }
            compose.onNodeWithTag("playground-details").assertDoesNotExist()
            val beforePan = rows()
            canvas().performTouchInput { swipe(Offset(width * .1f, height * .1f), Offset(width * .2f, height * .15f)) }
            assertEquals(beforePan, rows())
            overview()
            device.takeScreenshot(File(folder, "pg1-overview.png"))
            compose.onNodeWithText("Journal").performClick()
            compose.onNodeWithTag("log-editor").assertExists()
        } catch (failure: Throwable) {
            device.takeScreenshot(File(folder, "pg1-failure.png"))
            File(folder, "pg1-state.txt").writeText(compose.onRoot().printToString())
            throw failure
        }
    }
}
