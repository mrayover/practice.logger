package com.mrayover.practicelogger

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.roundToInt
import kotlin.math.sqrt

internal enum class HomeDirection(val dx: Int, val dy: Int, val label: String, val arrow: String) {
    // Clockwise screen-space sectors starting at the positive X axis.
    RIGHT(1, 0, "Right", "→"),
    DOWN_RIGHT(1, 1, "Down right", "↘"),
    DOWN(0, 1, "Down", "↓"),
    DOWN_LEFT(-1, 1, "Down left", "↙"),
    LEFT(-1, 0, "Left", "←"),
    UP_LEFT(-1, -1, "Up left", "↖"),
    UP(0, -1, "Up", "↑"),
    UP_RIGHT(1, -1, "Up right", "↗");

    fun includes(cardinal: HomeDirection): Boolean =
        if (cardinal.dx != 0) dx == cardinal.dx else dy == cardinal.dy

    companion object {
        fun fromTouch(x: Float, y: Float, deadZone: Float, previous: HomeDirection?): HomeDirection? {
            if (x * x + y * y <= deadZone * deadZone) return null
            val sector = atan2(y, x) / (PI.toFloat() / 4f)
            if (previous != null) {
                val difference = abs((sector + 8f) % 8f - previous.ordinal)
                // Retain the current sector for five degrees beyond either edge.
                if (minOf(difference, 8f - difference) <= 0.5f + 5f / 45f) return previous
            }
            return entries[(sector.roundToInt() + entries.size) % entries.size]
        }

        fun combine(directions: Iterable<HomeDirection?>): HomeDirection? {
            var left = 0
            var right = 0
            var up = 0
            var down = 0
            for (direction in directions) {
                if (direction == null) continue
                left = minOf(left, direction.dx)
                right = maxOf(right, direction.dx)
                up = minOf(up, direction.dy)
                down = maxOf(down, direction.dy)
            }
            return entries.firstOrNull { it.dx == left + right && it.dy == up + down }
        }
    }
}

internal data class HomePosition(val x: Float, val y: Float)

internal object PlaygroundHome {
    const val tile = 16
    const val columns = 14
    const val rows = 16
    const val width = columns * tile
    const val height = rows * tile
    const val floorTop = 2 * tile
    const val playerWidth = 12
    const val playerHeight = 16
    const val speed = 92f
    val entrance = HomePosition((width - playerWidth) / 2f, (height - playerHeight) / 2f)

    fun move(player: HomePosition, direction: HomeDirection, seconds: Float): HomePosition {
        val magnitude = sqrt((direction.dx * direction.dx + direction.dy * direction.dy).toFloat())
        val distance = speed * seconds / magnitude
        // The walkable floor is rectangular, so clamping each axis also stops diagonal corner clipping.
        return HomePosition(
            (player.x + direction.dx * distance).coerceIn(tile.toFloat(), (width - tile - playerWidth).toFloat()),
            (player.y + direction.dy * distance).coerceIn(floorTop.toFloat(), (height - tile - playerHeight).toFloat())
        )
    }

    fun interact() {
        // The room has no interactive objects in this phase.
    }
}
