package com.mrayover.practicelogger

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.*
import org.junit.Test

class PracticeOverlayGeometryTest {
    @Test fun centeredFitExcludesLetterbox() {
        val landscape = ReferencePageTransform(IntSize(401, 601), IntSize(800, 400), 1f, Offset.Zero)
        assertEquals(0f, landscape.bounds.left, .0001f)
        assertEquals(200f, landscape.bounds.top, .0001f)
        assertEquals(401f, landscape.bounds.right, .0001f)
        assertEquals(400.5f, landscape.bounds.bottom, .0001f)
        val portrait = ReferencePageTransform(IntSize(600, 400), IntSize(200, 400), 1f, Offset.Zero)
        assertEquals(Rect(200f, 0f, 400f, 400f), portrait.bounds)
    }

    @Test fun inverseMatchesZoomAndPanAfterResize() {
        val shape = Rect(.125f, .25f, .75f, .875f)
        for (viewport in listOf(IntSize(411, 501), IntSize(721, 381))) {
            val transform = ReferencePageTransform(viewport, IntSize(500, 800), 2.5f, Offset(-17f, 41f))
            val drawn = transform.toScreen(shape)
            val start = transform.toPage(drawn.topLeft)
            val end = transform.toPage(drawn.bottomRight)
            assertEquals(shape.left, start.x, .000001f)
            assertEquals(shape.top, start.y, .000001f)
            assertEquals(shape.right, end.x, .000001f)
            assertEquals(shape.bottom, end.y, .000001f)
        }
    }

    @Test fun movementStaysOnPageAndCorruptGeometryIsRejected() {
        assertEquals(Rect(0f, .75f, .25f, 1f), moveOverlay(Rect(.25f, .25f, .5f, .5f), Offset(-3f, 2f)))
        assertFalse(validOverlayBounds(Float.NaN, 0, 1, 1))
        assertFalse(validOverlayBounds(0, 0, 1, Float.POSITIVE_INFINITY))
        assertFalse(validOverlayBounds(1, 0, 1, 1))
        assertFalse(validOverlayBounds(0, .5f, 1, .25f))
    }
}
