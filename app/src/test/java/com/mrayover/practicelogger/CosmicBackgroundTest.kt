package com.mrayover.practicelogger

import org.junit.Assert.*
import org.junit.Test

class CosmicBackgroundTest {
    @Test fun sparseStaticStarsStayBoundedAndRepeatable() {
        assertEquals(96, cosmicStars.size)
        assertEquals(cosmicStars, cosmicStars.toList())
        assertTrue(cosmicStars.all { it.x in 0f..1f && it.y in 0f..1f && it.radius in .45f..1.1f && it.alpha in .16f.. .64f && it.tint in 0..2 })
        assertEquals(6, cosmicStars.count { it.alpha > .5f })
        assertEquals(96, cosmicStars.map { it.x to it.y }.distinct().size)
    }
}
