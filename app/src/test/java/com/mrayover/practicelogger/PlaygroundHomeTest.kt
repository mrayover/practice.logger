package com.mrayover.practicelogger

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaygroundHomeTest {
    @Test fun equalElapsedTimeMovesTheSameDistanceAtDifferentFrameRates() {
        val start = PlaygroundHome.entrance
        HomeDirection.entries.forEach { direction ->
            val coarse = (1..10).fold(start) { position, _ -> PlaygroundHome.move(position, direction, 0.05f) }
            val fine = (1..50).fold(start) { position, _ -> PlaygroundHome.move(position, direction, 0.01f) }
            assertEquals(coarse.x, fine.x, 0.001f)
            assertEquals(coarse.y, fine.y, 0.001f)
        }
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
}
