package com.mrayover.practicelogger

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.sqrt

class PlaygroundGeometryTest {
    private val world = playgroundWorld(20)
    private val a = PlaygroundLayout("a", 0.0, 0.0, 0.0)
    private fun b(x: Double, y: Double = 0.0, angle: Double = 0.0) = PlaygroundLayout("b", x, y, angle)
    @Test fun coverageAndProportionalExpansion() {
        val w40 = playgroundWorld(21); val w80 = playgroundWorld(41)
        assertEquals(20, world.capacity); assertEquals(40, w40.capacity); assertEquals(80, w80.capacity)
        assertEquals(0.15, 20 / (world.width * world.height), 1e-12)
        assertEquals(sqrt(2.0), w40.width / world.width, 1e-12)
        assertEquals(4.0, w80.width * w80.height / (world.width * world.height), 1e-12)
    }
    @Test fun satSupportsExactRotatedEdgeAndCornerContact() {
        assertFalse(playgroundOverlaps(a, b(1.0)))
        assertTrue(playgroundOverlaps(a, b(0.99)))
        assertTrue(playgroundOverlaps(a, b(1.1, angle = 45.0)))
        assertFalse(playgroundOverlaps(a, b(0.5 + sqrt(0.5), angle = 45.0)))
        assertFalse(playgroundOverlaps(a, b(1.0, 1.0)))
        assertFalse(playgroundOverlaps(a.copy(rotationDegrees = 45.0), b(0.9, 0.9, 45.0))) // AABBs overlap, actual geometry does not.
    }
    @Test fun hitTestingUsesRotatedSquare() {
        val rotated = a.copy(rotationDegrees = 45.0)
        assertTrue(playgroundContains(rotated, WorldPoint(0.65, 0.0)))
        assertFalse(playgroundContains(rotated, WorldPoint(0.6, 0.6)))
    }
    @Test fun sweptTranslationStopsFastUpdatesAndAllowsRetreatAndEdgeSliding() {
        val obstacle = b(2.0)
        val stopped = movePlaygroundSquare(a, WorldPoint(100.0, 0.0), listOf(obstacle), world)
        assertEquals(1.0, stopped.x, 1e-9)
        assertEquals(obstacle, b(2.0))
        assertEquals(0.0, movePlaygroundSquare(stopped, WorldPoint(-1.0, 0.0), listOf(obstacle), world).x, 1e-9)
        assertEquals(2.0, movePlaygroundSquare(stopped, WorldPoint(0.0, 2.0), listOf(obstacle), world).y, 1e-9)
    }
    @Test fun fastRotatedTranslationStopsAtActualContact() {
        val rotated = a.copy(rotationDegrees = 45.0)
        val result = movePlaygroundSquare(rotated, WorldPoint(10.0, 0.0), listOf(b(2.0)), world)
        assertEquals(1.5 - sqrt(0.5), result.x, 1e-9)
        assertFalse(playgroundOverlaps(result, b(2.0)))
    }
    @Test fun rotationCannotPassThroughObstacleToClearEndOrientation() {
        val obstacle = b(1.1)
        val result = rotatePlaygroundSquare(a, 180.0, listOf(obstacle), world)
        assertTrue(result.rotationDegrees in 1.0..44.0)
        assertFalse(playgroundOverlaps(result, obstacle))
        val away = rotatePlaygroundSquare(result, -20.0, listOf(obstacle), world)
        assertTrue(away.rotationDegrees != result.rotationDegrees)
        assertFalse(playgroundOverlaps(away, obstacle))
        assertEquals(a.x, result.x, 0.0); assertEquals(a.y, result.y, 0.0)
    }
    @Test fun arbitraryRotationAndRotatedBoundaryClearance() {
        assertEquals(37.123, rotatePlaygroundSquare(a, 37.123, emptyList(), world).rotationDegrees, 1e-7)
        val edge = a.copy(x = world.width / 2 - 0.55)
        val rotated = rotatePlaygroundSquare(edge, 90.0, emptyList(), world)
        assertTrue(rotated.rotationDegrees < 45); assertTrue(validPlaygroundLayout(rotated, world))
        assertTrue(validPlaygroundLayout(movePlaygroundSquare(a.copy(rotationDegrees = 45.0), WorldPoint(100.0, 100.0), emptyList(), world), world))
    }
    @Test fun absolutePresetsAreExactAndNeverMoveOrPartiallyTurnBlockedSquares() {
        val turned = presetPlaygroundOrientation(a.copy(rotationDegrees = 350.0), 45.0, emptyList(), world)!!
        assertEquals(a.copy(rotationDegrees = 45.0), turned)
        assertEquals(a, presetPlaygroundOrientation(turned, 0.0, emptyList(), world))
        assertEquals(turned, presetPlaygroundOrientation(turned, 45.0, emptyList(), world))
        assertNull(presetPlaygroundOrientation(a, 45.0, listOf(b(1.1)), world))
        assertNull(presetPlaygroundOrientation(a.copy(x = world.width / 2 - 0.55), 45.0, emptyList(), world))
        // Both endpoints clear, but the intervening arc intersects: reject the whole operation.
        assertNull(presetPlaygroundOrientation(a.copy(rotationDegrees = 315.0), 45.0, listOf(b(0.95, 0.95)), world))
    }
    private fun completions(n: Int) = (1..n).map { BlockCompletion("p$it", "s", "b", it.toLong(), null, null, null) }
    @Test fun stableSparseInitialPlacementAndExpansionPreserveEveryExistingBit() {
        val first = initialPlaygroundLayouts(completions(20), emptyList())
        assertEquals(first, initialPlaygroundLayouts(completions(20).reversed(), emptyList()))
        val arranged = first.mapIndexed { i, s -> if (i == 0) s.copy(rotationDegrees = 17.0) else s }
        val forty = initialPlaygroundLayouts(completions(40), arranged)
        val eighty = initialPlaygroundLayouts(completions(80), forty)
        assertEquals(arranged, forty.take(20)); assertEquals(forty, eighty.take(40))
        validatePlaygroundLayouts(eighty, completions(80).mapTo(hashSetOf()) { it.placementId })
    }
    @Test fun rejectsMalformedOrOrphanedOrOverlappingArrangement() {
        for (rows in listOf(listOf(a.copy(x = Double.NaN)), listOf(a.copy(y = Double.POSITIVE_INFINITY)),
            listOf(a.copy(x = 1e9)), listOf(a.copy(rotationDegrees = 1e9)), listOf(a, a), listOf(a, b(0.1)))) {
            assertTrue(runCatching { validatePlaygroundLayouts(rows, setOf("a", "b")) }.isFailure)
        }
        assertTrue(runCatching { validatePlaygroundLayouts(listOf(a), emptySet()) }.isFailure)
    }
    @Test fun viewportFocusAndWorldMovementAreIndependentOfScale() {
        val camera = PlaygroundCamera(20.0, WorldPoint(100.0, 200.0)); val focus = WorldPoint(75.0, 80.0)
        val zoomed = camera.zoom(focus, 3.0, 5.0, 200.0)
        assertEquals(camera.world(focus).x, zoomed.world(focus).x, 1e-12)
        assertEquals(camera.world(focus).y, zoomed.world(focus).y, 1e-12)
        assertEquals(200.0, camera.zoom(focus, 1000.0, 5.0, 200.0).scale, 0.0)
        assertEquals(5.0, camera.zoom(focus, 0.001, 5.0, 200.0).scale, 0.0)
        assertEquals(1.0, (zoomed.world(focus + WorldPoint(60.0, 0.0)) - zoomed.world(focus)).x, 1e-12)
    }
}
