package com.mrayover.practicelogger

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.lifecycle.Lifecycle
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt

class PlaygroundWorkflowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val db get() = BlockDatabase.appInstance(compose.activity.application)
    private val floorColors = setOf(0xFF663931.toInt(), 0xFFD9A066.toInt())
    private val wallColors = setOf(0xFF323C39.toInt(), 0xFF8A6F30.toInt(), 0xFFAC3232.toInt())
    private val doorColors = setOf(0xFF000000.toInt(), 0xFF45283C.toInt(), 0xFF663931.toInt(), 0xFFFBF236.toInt())
    private val spriteColors = setOf(0xFF000000.toInt(), 0xFF76428A.toInt(), 0xFFFFFFFF.toInt()) + wallColors
    private val spriteBackgrounds = floorColors + wallColors + doorColors
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
        waitForTotal(6)
        // Changing emulator display size can leave a launcher ANR over the foreground test app.
        if (device.hasObject(By.textContains("Quickstep isn't responding"))) {
            checkNotNull(device.findObject(By.text("Close app"))).click()
        }
        compose.waitUntil(10_000) { compose.activity.hasWindowFocus() }
        val page = bounds("playground-home")
        val viewport = bounds("playground-canvas")
        val controls = bounds("playground-controls")
        println("Playground fixed page $page, viewport $viewport, controls $controls")
        screenshot("playground-home")
        assertTrue("Gameplay uses more than half the fixed page: $viewport in $page", viewport.height > page.height * 0.5f)
        assertTrue("Gameplay uses the available width", viewport.width > page.width * 0.90f)
        assertTrue("Controls sit below gameplay", controls.top > viewport.bottom)
        val tags = listOf("canvas", "inventory", "explore", "dpad", "up", "down", "left", "right", "a", "b")
        val fixedBounds = tags.associateWith { bounds("playground-$it") }
        val appBounds = bounds("app-surface")
        fixedBounds.values.forEach { rect ->
            assertTrue(rect.left >= appBounds.left && rect.right <= appBounds.right &&
                rect.top >= appBounds.top && rect.bottom <= appBounds.bottom)
        }
        compose.onAllNodes(SemanticsMatcher.keyIsDefined(androidx.compose.ui.semantics.SemanticsActions.ScrollBy))
            .assertCountEquals(0)
        listOf("Stats", "Workshop", "Kitchen").forEach { compose.onNodeWithText(it).assertIsNotEnabled() }
        for (label in listOf("Home", "Stats", "Workshop", "Kitchen")) {
            val results = mutableListOf<TextLayoutResult>()
            compose.onNodeWithText(label, useUnmergedTree = true).assertIsDisplayed()
                .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
            assertFalse("$label was ellipsized in the destination row", results.single().isLineEllipsized(0))
        }
        node("playground-explore").assertIsNotEnabled()
        node("playground-b").assertIsNotEnabled()
        verifyController(controls, fixedBounds)
        verifyRoomFit(controls.height / 96f)
        verifyBatelTiming()
        assertPlayerFrame(0)

        val before = playerPosition()
        node("playground-canvas").performTouchInput { swipeUp(); swipeLeft() }
        assertEquals(before, playerPosition())
        assertEquals(viewport, bounds("playground-canvas"))
        node("playground-a").assertIsEnabled().performClick()
        assertEquals(before, playerPosition())

        compose.waitUntil(10_000) {
            compose.activity.hasWindowFocus() &&
                node("playground-right").fetchSemanticsNode().config.getOrNull(SemanticsActions.OnClick) != null
        }
        compose.mainClock.autoAdvance = false
        try {
            val pad = node("playground-dpad")
            pad.performTouchInput { down(center) }
            compose.mainClock.advanceTimeBy(400)
            pad.performTouchInput { up() }
            assertEquals("The D-pad center is neutral", before, playerPosition())

            val right = holdTouch(node("playground-right"), 470, verifyWalk = true) { center }
            assertTrue("Holding Right moves from $before to $right", right.x > before.x)
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

            verifyEightDirectionTouch()
            val rightWall = hold("right", 2600)
            assertEquals(rightWall, hold("right", 320))
            val slid = holdDiagonal(HomeDirection.UP_RIGHT, 160)
            assertEquals(rightWall.x, slid.x, 0f)
            assertTrue(slid.y < rightWall.y)
            val topWall = hold("up", 3100)
            assertEquals(topWall, hold("up", 320))
            assertEquals(topWall, holdDiagonal(HomeDirection.UP_RIGHT, 160))
            assertPlayerFrame(0)
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
        waitForTotal(7)
        node("playground-inventory-panel").assertDoesNotExist()
        assertEquals(fixedBounds, tags.associateWith { bounds("playground-$it") })
        assertEquals(stopped, playerPosition())
        node("playground-inventory").performClick()
        node("playground-inventory-panel").assertIsDisplayed()
        compose.waitUntil(10_000) { !compose.activity.hasWindowFocus() }
        device.pressBack()
        compose.waitUntil(10_000) { compose.activity.hasWindowFocus() }
        node("playground-inventory-panel").assertDoesNotExist()
        node("playground-home").assertIsDisplayed()
        compose.activityRule.scenario.recreate()
        waitForTotal(7)
        val appearance = compose.activity.themePreferences.choice.value
        try {
            compose.runOnIdle { compose.activity.themePreferences.save(ThemeChoice.COSMIC) }
            compose.waitForIdle()
            verifyController(bounds("playground-controls"), tags.associateWith { bounds("playground-$it") })
            screenshot("playground-controller-cosmic")
        } finally {
            compose.runOnIdle { compose.activity.themePreferences.save(appearance) }
        }
        compose.onNodeWithText("Journal").performClick()
        compose.onNodeWithText("Playground").assertIsDisplayed()
        assertEquals(listOf(savedLayout), runBlocking { db.playground().readAll() })
        assertEquals(original + completion(6), runBlocking { db.playground().completions() })
    }

    private fun verifyEightDirectionTouch() {
        val pad = node("playground-dpad")
        val padBounds = bounds("playground-dpad")
        val armCenters = listOf("up", "down", "left", "right").associateWith {
            bounds("playground-$it").center - padBounds.topLeft
        }
        fun afterInput(vararg arms: String): Offset {
            // Android can deliver a batched MOVE after the test clock starts advancing.
            // Wait for the controller to render the resolved input before measuring the next interval.
            compose.waitUntil(5_000) {
                compose.mainClock.advanceTimeByFrame()
                val pixels = pad.captureToImage().toPixelMap()
                val pressed = armCenters.filterValues { center ->
                    val x = center.x.toInt()
                    val y = center.y.toInt()
                    (y - 10..y + 10).any { row ->
                        (x - 10..x + 10).any { column -> pixels[column, row].toArgb() == 0xFF315C47.toInt() }
                    }
                }.keys
                pressed == arms.toSet()
            }
            return playerPosition()
        }
        val start = playerPosition()
        val cardinal = hold("left", 160)
        hold("right", 160)
        val cardinalDistance = start.x - cardinal.x
        for (direction in listOf(HomeDirection.UP_RIGHT, HomeDirection.DOWN_LEFT,
            HomeDirection.UP_LEFT, HomeDirection.DOWN_RIGHT)) {
            val before = playerPosition()
            val after = holdDiagonal(direction, 160, verifyWalk = direction == HomeDirection.UP_RIGHT)
            val delta = after - before
            assertTrue("$direction moves on both axes: $delta", delta.x * direction.dx > 0 && delta.y * direction.dy > 0)
            val viewport = bounds("playground-canvas")
            val scale = minOf(viewport.width / PlaygroundHome.width, viewport.height / PlaygroundHome.height)
            assertEquals("Diagonal and cardinal movement cover the same distance", cardinalDistance,
                delta.getDistance(), ceil(4 * scale))
        }

        fun advance(): Offset {
            compose.runOnIdle {}
            compose.mainClock.advanceTimeBy(176)
            return playerPosition()
        }
        pad.performTouchInput { down(center + Offset(0f, -height / 4f)) }
        val beforeSlide = afterInput("up")
        val up = advance()
        assertTrue(up.y < beforeSlide.y)
        assertEquals(beforeSlide.x, up.x, 0f)
        pad.performTouchInput { moveTo(0, center + Offset(height / 4f, -height / 4f)) }
        val beforeDiagonal = afterInput("up", "right")
        val diagonal = advance()
        assertTrue(diagonal.x > beforeDiagonal.x && diagonal.y < beforeDiagonal.y)
        pad.performTouchInput { moveTo(0, center + Offset(height / 4f, 0f)) }
        val beforeRight = afterInput("right")
        val right = advance()
        assertTrue(right.x > beforeRight.x)
        assertEquals(beforeRight.y, right.y, 0f)
        pad.performTouchInput { moveTo(0, center) }
        val neutral = afterInput()
        assertEquals(neutral, advance())
        pad.performTouchInput { up() }

        val aCenter = bounds("playground-a").center - bounds("playground-dpad").topLeft
        pad.performTouchInput {
            down(0, center + Offset(0f, height / 4f))
            down(1, center + Offset(-height / 4f, 0f))
            down(2, aCenter)
        }
        val beforeCombined = afterInput("down", "left")
        val combined = advance()
        assertTrue(combined.x < beforeCombined.x && combined.y > beforeCombined.y)
        pad.performTouchInput { up(2) }
        val afterA = advance()
        assertTrue(afterA.x < combined.x && afterA.y > combined.y)
        pad.performTouchInput {
            down(2, center + Offset(height / 4f, 0f))
        }
        val beforeDown = afterInput("down")
        val onlyDown = advance()
        assertEquals(beforeDown.x, onlyDown.x, 0f)
        assertTrue(onlyDown.y > beforeDown.y)
        pad.performTouchInput { down(3, center + Offset(0f, -height / 4f)) }
        val cancelledAxes = afterInput()
        assertEquals(cancelledAxes, advance())
        pad.performTouchInput { up(0); up(2); up(3) }
        val beforeLeft = afterInput("left")
        val onlyLeft = advance()
        assertTrue(onlyLeft.x < beforeLeft.x)
        assertEquals(beforeLeft.y, onlyLeft.y, 0f)
        pad.performTouchInput { cancel() }
        val cancelled = afterInput()
        assertEquals(cancelled, advance())

        pad.performTouchInput { down(center + Offset(height / 4f, 0f)) }
        val held = advance()
        assertTrue(held.x > cancelled.x)
        compose.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
        compose.mainClock.advanceTimeBy(176)
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        val resumed = advance()
        assertEquals("Resuming does not restore a held direction", resumed, advance())
        pad.performTouchInput { cancel() }
    }

    private fun verifyController(controls: Rect, bounds: Map<String, Rect>) {
        val pad = bounds.getValue("dpad")
        val a = bounds.getValue("a")
        val b = bounds.getValue("b")
        assertTrue("B is lower-left of A with a gap", b.right < a.left && b.top > a.top)
        assertTrue("The D-pad stays to the left", pad.right < b.left)
        assertEquals("The control row is still two 48 dp targets tall", 2 * a.height, controls.height, 1f)
        for (key in listOf("dpad", "up", "down", "left", "right", "a", "b")) {
            val control = bounds.getValue(key)
            assertTrue("$key stays inside the control row", control.left >= controls.left &&
                control.right <= controls.right && control.top >= controls.top && control.bottom <= controls.bottom)
        }
        val arms = listOf("up", "down", "left", "right").map(bounds::getValue)
        arms.forEachIndexed { index, arm ->
            assertTrue("Direction targets stay distinct", arms.drop(index + 1).none { arm.overlaps(it) })
        }

        val pixels = node("playground-dpad").captureToImage().toPixelMap()
        val fill = 0xFF463953.toInt()
        fun pixel(x: Float, y: Float) = pixels[(x * pixels.width).toInt(), (y * pixels.height).toInt()].toArgb()
        for (point in listOf(Offset(0.5f, 0.5f), Offset(0.39f, 0.15f), Offset(0.39f, 0.85f),
            Offset(0.10f, 0.5f), Offset(0.90f, 0.5f), Offset(0.4f, 0.5f), Offset(0.6f, 0.5f))) {
            assertEquals("The arms and junction share one surface at $point", fill, pixel(point.x, point.y))
        }
        assertNotEquals("The area outside the cross is clear", fill, pixel(0.10f, 0.10f))
    }

    private fun verifyRoomFit(density: Float) {
        val pixels = node("playground-canvas").captureToImage().toPixelMap()
        fun isWall(x: Int, y: Int) = pixels[x, y].toArgb() in wallColors
        val columns = (0 until pixels.width).filter { isWall(it, pixels.height / 2) }
        val rows = (0 until pixels.height).filter { isWall(pixels.width / 2, it) }
        assertTrue("Solid wall colors are visible along both room axes", columns.isNotEmpty() && rows.isNotEmpty())
        val left = columns.first()
        val right = pixels.width - 1 - columns.last()
        val top = rows.first()
        val bottom = pixels.height - 1 - rows.last()
        val width = pixels.width - left - right
        val height = pixels.height - top - bottom

        println("Playground room ${width}x${height}, frame $left/$top/$right/$bottom px")
        assertTrue("The room stays inside the viewport", minOf(left, right, top, bottom) >= 1)
        assertTrue("Opposite margins match within pixel rounding", abs(left - right) <= 1 && abs(top - bottom) <= 1)
        assertTrue("The limiting margin stays narrow", minOf(left, top) <= 6 * density + 2)
        assertEquals("The tiles retain square proportions",
            height * PlaygroundHome.width.toFloat() / PlaygroundHome.height, width.toFloat(), 2f)
        val floorAndPlayerColors = spriteBackgrounds + spriteColors
        assertTrue("No blended seams along the room's center tile edge",
            (left until left + width).all { pixels[it, pixels.height / 2].toArgb() in floorAndPlayerColors })

        val room = PlaygroundHome
        val floor = loadArtwork("FloorTile1.png")
        val wall = loadArtwork("Wall1.png")
        val door = loadArtwork("Door1.png")
        try {
            assertEquals(30, floor.width)
            assertEquals(45, wall.width)
            assertEquals(45, door.width)
            val scale = roomScale(pixels.width, pixels.height)
            val originX = (pixels.width - room.width * scale) / 2
            val originY = (pixels.height - room.height * scale) / 2
            fun verifyTile(bitmap: Bitmap, x: Int, y: Int, size: Int) {
                val x0 = (originX + x * scale).roundToInt()
                val y0 = (originY + y * scale).roundToInt()
                val w = (originX + (x + size) * scale).roundToInt() - x0
                val h = (originY + (y + size) * scale).roundToInt() - y0
                for (dy in 0 until h step 3) {
                    for (dx in 0 until w step 3) {
                        val sourceX = (dx + 0.5) * bitmap.width / w
                        val sourceY = (dy + 0.5) * bitmap.height / h
                        // Either adjacent texel is valid exactly on a nearest-neighbor tie.
                        if (abs(sourceX - sourceX.roundToInt()) < 0.01 ||
                            abs(sourceY - sourceY.roundToInt()) < 0.01) continue
                        assertEquals("Artwork pixel at room ($x,$y), offset ($dx,$dy)",
                            bitmap.getPixel(sourceX.toInt(), sourceY.toInt()), pixels[x0 + dx, y0 + dy].toArgb())
                    }
                }
            }
            for (row in 0 until room.rows) {
                for (column in 0 until room.columns) {
                    // The doorway is checked separately. Batel overlaps the central tiles at entrance.
                    if (column in 6..7 && (row < 2 || row in 6..8)) continue
                    val isWall = row < 2 || row == room.rows - 1 || column == 0 || column == room.columns - 1
                    verifyTile(if (isWall) wall else floor, column * room.tile, row * room.tile, room.tile)
                }
            }
            verifyTile(door, room.width / 2 - 12, 6, 24)
        } finally {
            floor.recycle()
            wall.recycle()
            door.recycle()
        }
    }

    private fun loadArtwork(filename: String): Bitmap =
        compose.activity.assets.open("playground/$filename").use { checkNotNull(BitmapFactory.decodeStream(it)) }

    private fun completion(index: Int): BlockCompletion {
        // Saved rank intentionally disagrees with saved color. The inventory reads the color.
        val goalId = if (index < 5) "historical-goal" else null
        return BlockCompletion("pg$index", "pg-session", "pg-block", 2000L + index, goalId,
            goalId?.let { 1 }, if (index < 5) goalCompletionColor(index + 1) else null)
    }

    private fun hold(direction: String, millis: Long, cancelled: Boolean = false): Offset {
        val button = node("playground-$direction")
        return holdTouch(button, millis, cancelled) { center }
    }

    private fun holdDiagonal(direction: HomeDirection, millis: Long, verifyWalk: Boolean = false): Offset =
        holdTouch(node("playground-dpad"), millis, verifyWalk = verifyWalk) {
            center + Offset(direction.dx * height / 4f, direction.dy * height / 4f)
        }

    private fun holdTouch(
        button: SemanticsNodeInteraction, millis: Long, cancelled: Boolean = false,
        verifyWalk: Boolean = false,
        position: TouchInjectionScope.() -> Offset
    ): Offset {
        button.performTouchInput { down(position()) }
        compose.runOnIdle {}
        if (verifyWalk) {
            compose.mainClock.advanceTimeBy(64)
            assertPlayerFrame(1)
            compose.mainClock.advanceTimeBy(96)
            assertPlayerFrame(2)
            if (millis > 160) {
                compose.mainClock.advanceTimeBy(112)
                assertPlayerFrame(1)
                compose.mainClock.advanceTimeBy(millis - 272)
            }
        } else {
            compose.mainClock.advanceTimeBy(millis)
        }
        val moved = playerPosition()
        button.performTouchInput { if (cancelled) cancel() else up() }
        compose.runOnIdle {}
        val stopped = playerPosition()
        if (verifyWalk) assertPlayerFrame(0)
        val viewport = bounds("playground-canvas")
        val dispatchTolerance = ceil(2 * minOf(viewport.width / PlaygroundHome.width, viewport.height / PlaygroundHome.height))
        assertTrue("Only the in-flight frame may finish while Android dispatches release",
            abs(stopped.x - moved.x) <= dispatchTolerance && abs(stopped.y - moved.y) <= dispatchTolerance)
        compose.mainClock.advanceTimeBy(220)
        assertEquals("Movement stops after release or cancellation", stopped, playerPosition())
        return stopped
    }

    private fun node(tag: String) = compose.onNodeWithTag(tag)
    private fun bounds(tag: String): Rect = node(tag).assertIsDisplayed().fetchSemanticsNode().boundsInRoot

    private fun waitForTotal(total: Int) {
        val text = "Blocks $total"
        compose.waitUntil(10_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText(text).assertIsDisplayed()
    }

    private fun assertCount(label: String, count: Int) {
        node("playground-count-$label").assertIsDisplayed().assertTextEquals(count.toString())
    }

    private fun playerPosition(): Offset = playerPosition(node("playground-canvas").captureToImage().toPixelMap())

    private fun playerPosition(pixels: PixelMap): Offset {
        var left = pixels.width
        var top = pixels.height
        // The three foot-aligned Batel poses have the same outer bounds.
        val scale = roomScale(pixels.width, pixels.height)
        val spriteHeight = ceil(scale * 30).toInt()
        val doorTop = ((pixels.height - PlaygroundHome.height * scale) / 2 + 6 * scale).roundToInt()
        val doorBottom = ((pixels.height - PlaygroundHome.height * scale) / 2 + 30 * scale).roundToInt()
        val doorLeft = (pixels.width / 2f - 12 * scale).roundToInt()
        val doorRight = (pixels.width / 2f + 12 * scale).roundToInt()
        for (y in 0 until pixels.height) {
            if (y >= top + spriteHeight) break
            for (x in 0 until pixels.width) {
                // All three poses have a black outer silhouette. Exclude the black door outline.
                if (pixels[x, y].toArgb() == 0xFF000000.toInt() &&
                    !(x in doorLeft until doorRight && y in doorTop until doorBottom)) {
                    left = minOf(left, x)
                    top = minOf(top, y)
                }
            }
        }
        check(left < pixels.width) { "Batel is missing from the viewport" }
        return Offset(left.toFloat(), top.toFloat())
    }

    private fun roomScale(width: Int, height: Int): Float {
        val density = bounds("playground-controls").height / 96f
        return minOf(
            (width - 2 * density) / PlaygroundHome.width,
            (height - 2 * density) / PlaygroundHome.height,
            (width + height - 24 * density) / (PlaygroundHome.width + PlaygroundHome.height)
        )
    }

    private fun verifyBatelTiming() {
        val sprite = PlaygroundSprite.load(compose.activity.assets)
        assertEquals(90, sprite.image.width)
        assertEquals(30, sprite.image.height)
        assertEquals(0, sprite.frameAt(-1).offset.x)
        assertEquals(30, sprite.frameAt(0).offset.x)
        assertEquals(30, sprite.frameAt(99_999_999).offset.x)
        assertEquals(60, sprite.frameAt(100_000_000).offset.x)
        assertEquals(60, sprite.frameAt(199_999_999).offset.x)
        assertEquals(30, sprite.frameAt(200_000_000).offset.x)
    }

    private fun assertPlayerFrame(frame: Int) {
        val pixels = node("playground-canvas").captureToImage().toPixelMap()
        val (left, top) = playerPosition(pixels)
        val scale = (roomScale(pixels.width, pixels.height) * 30).roundToInt() / 30f
        val originX = left.toInt() - ceil(5 * scale - 0.5f).toInt()
        val originY = top.toInt() - ceil(3 * scale - 0.5f).toInt()
        val bitmap = compose.activity.assets.open("playground/Batel.png").use { BitmapFactory.decodeStream(it) }
        try {
            val sourceTop = if (frame == 1) 1 else 0
            for (y in 3..27) {
                for (x in 5..25) {
                    val expected = bitmap.getPixel(frame * 30 + x, y - sourceTop)
                    val actual = pixels[originX + ((x + 0.5f) * scale).toInt(),
                        originY + ((y + 0.5f) * scale).toInt()].toArgb()
                    if (expected ushr 24 == 0) {
                        assertTrue("Room shows through Batel frame $frame at $x,$y", actual in spriteBackgrounds)
                    } else {
                        assertEquals("Batel frame $frame pixel $x,$y", expected, actual)
                    }
                }
            }
        } finally {
            bitmap.recycle()
        }
    }

    private fun screenshot(name: String) {
        val folder = File(compose.activity.getExternalFilesDir(null), "screenshots").apply { mkdirs() }
        assertTrue(device.takeScreenshot(File(folder, "$name.png")))
    }
}
