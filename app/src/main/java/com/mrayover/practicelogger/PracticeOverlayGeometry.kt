package com.mrayover.practicelogger

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt

/** The centered ContentScale.Fit rectangle, transformed around the viewer's center. */
internal class ReferencePageTransform(
    viewport: IntSize, bitmap: IntSize, zoom: Float, pan: Offset
) {
    private val center = Offset(viewport.width / 2f, viewport.height / 2f)
    private val fit = minOf(viewport.width.toFloat() / bitmap.width, viewport.height.toFloat() / bitmap.height)
    private val width = bitmap.width * fit
    private val height = bitmap.height * fit
    // Compose's centered painter aligns the rounded, scaled size to whole pixels.
    private val origin = Offset(
        ((viewport.width - width.roundToInt()) / 2f).roundToInt().toFloat(),
        ((viewport.height - height.roundToInt()) / 2f).roundToInt().toFloat()
    )
    val bounds = Rect(center + (origin - center) * zoom + pan, androidx.compose.ui.geometry.Size(width * zoom, height * zoom))

    fun toPage(point: Offset): Offset = Offset(
        ((point.x - bounds.left) / bounds.width).coerceIn(0f, 1f),
        ((point.y - bounds.top) / bounds.height).coerceIn(0f, 1f)
    )

    fun toScreen(rect: Rect): Rect = Rect(
        bounds.left + rect.left * bounds.width, bounds.top + rect.top * bounds.height,
        bounds.left + rect.right * bounds.width, bounds.top + rect.bottom * bounds.height
    )
}

internal fun PracticeOverlay.rectangle() = Rect(left, top, right, bottom)

internal fun PracticeOverlay.withRectangle(rect: Rect) =
    copy(left = rect.left, top = rect.top, right = rect.right, bottom = rect.bottom)

internal fun overlayRectangle(start: Offset, end: Offset) = Rect(
    minOf(start.x, end.x), minOf(start.y, end.y), maxOf(start.x, end.x), maxOf(start.y, end.y)
)

internal fun moveOverlay(rect: Rect, delta: Offset): Rect = rect.translate(
    delta.x.coerceIn(-rect.left, 1f - rect.right), delta.y.coerceIn(-rect.top, 1f - rect.bottom)
)

