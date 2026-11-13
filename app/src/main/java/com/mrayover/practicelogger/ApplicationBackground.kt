package com.mrayover.practicelogger

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import kotlin.random.Random

internal val LocalCosmicBackground = staticCompositionLocalOf { false }
@Composable internal fun pageBackgroundColor(): Color =
    if (LocalCosmicBackground.current) Color.Transparent else MaterialTheme.colorScheme.background

internal data class CosmicStar(val x: Float, val y: Float, val radius: Float, val alpha: Float, val tint: Int)
// Generated once, outside composition/scrolling. Fixed normalized positions also survive recreation.
internal val cosmicStars: List<CosmicStar> = Random(0xC05C1C).let { random ->
    List(96) { i -> CosmicStar(random.nextFloat(), random.nextFloat(),
        .45f + random.nextFloat() * .65f, if (i % 17 == 0) .64f else .16f + random.nextFloat() * .25f, i % 3) }
}

/** One static draw layer; no pointer handlers, animation, timer or media ownership. */
@Composable internal fun ApplicationBackground(content: @Composable () -> Unit) {
    val cosmic = LocalCosmicBackground.current
    val background = MaterialTheme.colorScheme.background
    val decoration = if (!cosmic) Modifier.background(background) else Modifier.drawWithCache {
        val gradient = Brush.linearGradient(listOf(Color(0xFF10162D), Color(0xFF1B2141), Color(0xFF23203E)),
            start = Offset.Zero, end = Offset(size.width, size.height))
        val glow = Brush.radialGradient(listOf(Color(0x143D61A8), Color.Transparent),
            center = Offset(size.width * .28f, size.height * .35f), radius = size.maxDimension * .7f)
        val tints = listOf(Color(0xFFA9DCFF), Color(0xFFC8B6FF), Color(0xFFF0EEFF))
        val positions = cosmicStars.map { Offset(it.x * size.width, it.y * size.height) }
        val radii = cosmicStars.map { it.radius * density }
        onDrawBehind {
            drawRect(gradient)
            drawRect(glow)
            cosmicStars.forEachIndexed { i, star ->
                drawCircle(tints[star.tint], radii[i], positions[i], alpha = star.alpha)
            }
        }
    }.testTag("cosmic-background")
    Box(Modifier.fillMaxSize().then(decoration)) { content() }
}
