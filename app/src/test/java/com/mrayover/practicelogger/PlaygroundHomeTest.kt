package com.mrayover.practicelogger

import com.mrayover.practicelogger.HomeDirection.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

class PlaygroundHomeTest {
    @Test fun equalElapsedTimeMovesTheSameDistanceAtDifferentFrameRates() {
        val start = PlaygroundHome.entrance
        HomeDirection.entries.forEach { direction ->
            val coarse = (1..10).fold(start) { position, _ -> PlaygroundHome.move(position, direction, 0.05f) }
            val fine = (1..50).fold(start) { position, _ -> PlaygroundHome.move(position, direction, 0.01f) }
            assertEquals(coarse.x, fine.x, 0.001f)
            assertEquals(coarse.y, fine.y, 0.001f)
            assertEquals(PlaygroundHome.speed * 0.5f, hypot(coarse.x - start.x, coarse.y - start.y), 0.001f)
        }
    }

    @Test fun touchSectorsUseScreenCoordinatesAndKeepTheCenterNeutral() {
        assertNull(HomeDirection.fromTouch(0f, 0f, 8f, HomeDirection.RIGHT))
        assertNull(HomeDirection.fromTouch(-4f, 4f, 8f, HomeDirection.UP_LEFT))
        for (direction in HomeDirection.entries) {
            assertEquals(direction, HomeDirection.fromTouch(direction.dx * 20f, direction.dy * 20f, 8f, null))
        }
    }

    @Test fun slidingHoldsThePreviousSectorOnlyNearItsBoundary() {
        fun touch(degrees: Double, previous: HomeDirection): HomeDirection? {
            val angle = Math.toRadians(degrees)
            return HomeDirection.fromTouch((30 * cos(angle)).toFloat(), (30 * sin(angle)).toFloat(), 8f, previous)
        }
        assertEquals(HomeDirection.RIGHT, touch(-25.0, HomeDirection.RIGHT))
        assertEquals(HomeDirection.UP_RIGHT, touch(-29.0, HomeDirection.RIGHT))
        assertEquals(HomeDirection.UP_RIGHT, touch(-20.0, HomeDirection.UP_RIGHT))
        assertEquals(HomeDirection.RIGHT, touch(-16.0, HomeDirection.UP_RIGHT))
        assertEquals(HomeDirection.LEFT, touch(180.0, HomeDirection.UP_RIGHT))
    }

    @Test fun heldDirectionsCombineWithoutOrderOrPointerCountPriority() {
        assertEquals(UP_RIGHT, HomeDirection.combine(listOf(UP, RIGHT)))
        assertEquals(UP_RIGHT, HomeDirection.combine(listOf(RIGHT, UP)))
        assertEquals(UP, HomeDirection.combine(listOf(UP_RIGHT, LEFT, RIGHT)))
        assertEquals(LEFT, HomeDirection.combine(listOf(UP_LEFT, DOWN)))
        assertEquals(RIGHT, HomeDirection.combine(listOf(UP_RIGHT, DOWN_RIGHT, null)))
        assertNull(HomeDirection.combine(listOf(UP_LEFT, DOWN_RIGHT)))
        assertNull(HomeDirection.combine(listOf(LEFT, RIGHT, RIGHT)))
        assertEquals(DOWN_LEFT, HomeDirection.combine(listOf(DOWN, LEFT)))
        assertEquals(DOWN, HomeDirection.combine(listOf(null, DOWN)))
        assertNull(HomeDirection.combine(emptyList()))
    }

    @Test fun theWholePlayerStaysInsideAllFourWallsAndCanWalkAwayFromCorners() {
        val room = PlaygroundHome
        val left = room.tile.toFloat()
        val right = (room.width - room.tile - room.playerWidth).toFloat()
        val top = room.floorTop.toFloat()
        val bottom = (room.height - room.tile - room.playerHeight).toFloat()
        val topRight = room.move(room.move(room.entrance, HomeDirection.UP, 4f), HomeDirection.RIGHT, 4f)
        assertEquals(HomePosition(right, top), topRight)
        assertEquals(topRight, room.move(topRight, HomeDirection.UP, 4f))
        val bottomRight = room.move(topRight, HomeDirection.DOWN, 4f)
        assertEquals(HomePosition(right, bottom), bottomRight)
        val bottomLeft = room.move(bottomRight, HomeDirection.LEFT, 4f)
        assertEquals(HomePosition(left, bottom), bottomLeft)
        assertEquals(HomePosition(left, top), room.move(bottomLeft, HomeDirection.UP, 4f))
    }

    @Test fun diagonalContactSlidesAlongEitherWallAndStopsAtTheirCorner() {
        val room = PlaygroundHome
        val right = (room.width - room.tile - room.playerWidth).toFloat()
        val top = room.floorTop.toFloat()
        val corner = HomePosition(right, top)
        val step = room.speed * 0.05f / kotlin.math.sqrt(2f)
        val nearRight = HomePosition(right - 1f, 100f)
        val nearTop = HomePosition(100f, top + 1f)
        assertEquals(HomePosition(right, nearRight.y - step), room.move(nearRight, HomeDirection.UP_RIGHT, 0.05f))
        assertEquals(HomePosition(nearTop.x + step, top), room.move(nearTop, HomeDirection.UP_RIGHT, 0.05f))
        assertEquals(corner, room.move(HomePosition(right - 1f, top + 1f), HomeDirection.UP_RIGHT, 0.05f))
        assertEquals(corner, room.move(corner, HomeDirection.UP_RIGHT, 0.05f))
        assertEquals(HomePosition(right - step, top + step), room.move(corner, HomeDirection.DOWN_LEFT, 0.05f))
    }
}
