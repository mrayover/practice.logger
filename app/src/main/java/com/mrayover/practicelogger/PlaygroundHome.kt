package com.mrayover.practicelogger

internal enum class HomeDirection(val dx: Int, val dy: Int, val label: String, val arrow: String) {
    UP(0, -1, "Up", "↑"), DOWN(0, 1, "Down", "↓"),
    LEFT(-1, 0, "Left", "←"), RIGHT(1, 0, "Right", "→")
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
        val distance = speed * seconds
        return HomePosition(
            (player.x + direction.dx * distance).coerceIn(tile.toFloat(), (width - tile - playerWidth).toFloat()),
            (player.y + direction.dy * distance).coerceIn(floorTop.toFloat(), (height - tile - playerHeight).toFloat())
        )
    }

    fun interact() {
        // The room has no interactive objects in this phase.
    }
}
