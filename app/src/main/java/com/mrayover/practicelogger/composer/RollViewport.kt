package com.mrayover.practicelogger.composer

/** Relative zoom and tick-based scroll survive resizing; all horizontal geometry uses this scale. */
internal fun rollPixelsPerTick(width: Float,keyWidth: Float,barTicks: Long,zoom: Float): Float =
    (width-keyWidth-2f).coerceAtLeast(1f)/barTicks*zoom

internal fun rollMaxPan(length: Long,width: Float,keyWidth: Float,scale: Float): Float =
    (length-(width-keyWidth)/scale).coerceAtLeast(0f)
