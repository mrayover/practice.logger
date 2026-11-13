package com.mrayover.practicelogger

import kotlin.math.*

/** One world unit is one square side. World origin never changes on expansion. */
internal data class PlaygroundWorld(val capacity: Int, val width: Double, val height: Double)
internal fun playgroundWorld(count: Int): PlaygroundWorld {
    require(count in 0..ZIP_RECORD_LIMIT)
    var capacity = 20
    while (capacity < count) capacity *= 2
    val area = capacity / 0.15
    return PlaygroundWorld(capacity, sqrt(area * 1.5), sqrt(area / 1.5))
}
internal data class WorldPoint(val x: Double, val y: Double) {
    operator fun plus(b: WorldPoint) = WorldPoint(x + b.x, y + b.y)
    operator fun minus(b: WorldPoint) = WorldPoint(x - b.x, y - b.y)
    operator fun times(k: Double) = WorldPoint(x * k, y * k)
    fun dot(b: WorldPoint) = x * b.x + y * b.y
}
private const val GEOMETRY_EPS = 1e-9
private fun PlaygroundLayout.axes(): List<WorldPoint> {
    val a = Math.toRadians(rotationDegrees)
    return listOf(WorldPoint(cos(a), sin(a)), WorldPoint(-sin(a), cos(a)))
}
private fun PlaygroundLayout.radius(axis: WorldPoint): Double = axes().sumOf { abs(it.dot(axis)) } / 2
internal fun playgroundContains(s: PlaygroundLayout, p: WorldPoint): Boolean =
    s.axes().all { abs((p - WorldPoint(s.x, s.y)).dot(it)) <= 0.5 + GEOMETRY_EPS }

/** Positive separating gap; zero/negative means contact/penetration. */
private fun gap(a: PlaygroundLayout, b: PlaygroundLayout): Double = (a.axes() + b.axes()).maxOf { axis ->
    abs(WorldPoint(a.x - b.x, a.y - b.y).dot(axis)) - a.radius(axis) - b.radius(axis)
}
internal fun playgroundOverlaps(a: PlaygroundLayout, b: PlaygroundLayout) = gap(a, b) < -GEOMETRY_EPS
private fun boundaryGap(s: PlaygroundLayout, w: PlaygroundWorld): Double = minOf(
    w.width / 2 - abs(s.x) - s.radius(WorldPoint(1.0, 0.0)),
    w.height / 2 - abs(s.y) - s.radius(WorldPoint(0.0, 1.0)))
internal fun validPlaygroundLayout(s: PlaygroundLayout, w: PlaygroundWorld): Boolean =
    s.placementId.isNotBlank() && listOf(s.x, s.y, s.rotationDegrees).all { it.isFinite() } &&
        abs(s.rotationDegrees) <= 360.0 && boundaryGap(s, w) >= -GEOMETRY_EPS
internal fun validatePlaygroundLayouts(rows: List<PlaygroundLayout>, completionIds: Set<String>) {
    require(rows.map { it.placementId }.toSet().size == rows.size) { "Duplicate Playground placement." }
    val world = playgroundWorld(completionIds.size)
    rows.forEachIndexed { index, row ->
        require(row.placementId in completionIds && validPlaygroundLayout(row, world)) { "Invalid Playground coordinates or completion." }
        require(rows.take(index).none { playgroundOverlaps(row, it) }) { "Playground squares overlap." }
    }
}

/** Swept SAT: intersect time intervals on all four axes, so even a very fast drag cannot tunnel. */
internal fun movePlaygroundSquare(s: PlaygroundLayout, delta: WorldPoint, others: List<PlaygroundLayout>, w: PlaygroundWorld): PlaygroundLayout {
    require(delta.x.isFinite() && delta.y.isFinite())
    var fraction = 1.0
    val rx = s.radius(WorldPoint(1.0, 0.0)); val ry = s.radius(WorldPoint(0.0, 1.0))
    fun bound(value: Double, d: Double, limit: Double) {
        if (d > 0) fraction = min(fraction, (limit - value) / d)
        if (d < 0) fraction = min(fraction, (-limit - value) / d)
    }
    bound(s.x, delta.x, w.width / 2 - rx); bound(s.y, delta.y, w.height / 2 - ry)
    for (other in others) {
        var entry = Double.NEGATIVE_INFINITY; var exit = Double.POSITIVE_INFINITY
        for (axis in s.axes() + other.axes()) {
            val offset = WorldPoint(s.x - other.x, s.y - other.y).dot(axis)
            val velocity = delta.dot(axis)
            val radius = s.radius(axis) + other.radius(axis)
            if (abs(velocity) < 1e-15) {
                if (abs(offset) >= radius - GEOMETRY_EPS) { entry = Double.POSITIVE_INFINITY; break }
            } else {
                val t1 = (-radius - offset) / velocity; val t2 = (radius - offset) / velocity
                entry = max(entry, min(t1, t2)); exit = min(exit, max(t1, t2))
            }
        }
        if (entry < exit && exit > GEOMETRY_EPS && entry < fraction) fraction = max(0.0, entry)
    }
    return s.copy(x = s.x + delta.x * fraction.coerceIn(0.0, 1.0), y = s.y + delta.y * fraction.coerceIn(0.0, 1.0))
}

/** Conservative advancement uses a Lipschitz bound on rotating vertices. Every traversed arc
 * is certified clear, including large updates whose end orientation would be clear again. */
internal fun rotatePlaygroundSquare(s: PlaygroundLayout, degrees: Double, others: List<PlaygroundLayout>, w: PlaygroundWorld): PlaygroundLayout {
    require(degrees.isFinite())
    var angle = s.rotationDegrees
    var remaining = degrees.coerceIn(-360.0, 360.0)
    repeat(2048) {
        if (abs(remaining) < 1e-7) return s.copy(rotationDegrees = normalizePlaygroundAngle(angle))
        val current = s.copy(rotationDegrees = normalizePlaygroundAngle(angle))
        val clearance = min(boundaryGap(current, w), others.minOfOrNull { gap(current, it) } ?: Double.POSITIVE_INFINITY)
        // A vertex moves at most sqrt(0.5) * radians. Retain micron-scale clearance.
        val safe = Math.toDegrees(max(0.0, clearance) / sqrt(0.5)) * 0.9
        // At exact contact allow a numerical-scale probe in the requested direction,
        // so turning away is possible. Its maximum vertex travel is 1.3e-9 world units.
        val step = sign(remaining) * min(abs(remaining), min(5.0, max(1e-7, safe)))
        val next = current.copy(rotationDegrees = normalizePlaygroundAngle(angle + step))
        if (!validPlaygroundLayout(next, w) || others.any { playgroundOverlaps(next, it) }) return current
        angle += step; remaining -= step
    }
    return s.copy(rotationDegrees = normalizePlaygroundAngle(angle))
}
internal fun normalizePlaygroundAngle(angle: Double) = ((angle % 360) + 360) % 360

internal fun initialPlaygroundLayouts(completions: List<BlockCompletion>, saved: List<PlaygroundLayout>): List<PlaygroundLayout> {
    validatePlaygroundLayouts(saved, completions.mapTo(hashSetOf()) { it.placementId })
    val result = saved.toMutableList(); val existing = saved.mapTo(hashSetOf()) { it.placementId }
    val world = playgroundWorld(completions.size)
    fun radical(n: Int, base: Int): Double {
        var number = n; var factor = 1.0; var value = 0.0
        while (number > 0) { factor /= base; value += (number % base) * factor; number /= base }
        return value
    }
    for ((ordinal, completion) in completions.sortedWith(compareBy<BlockCompletion> { it.completedAt }.thenBy { it.placementId }).withIndex()) {
        if (completion.placementId in existing) continue
        var found: PlaygroundLayout? = null
        for (attempt in 1..8192) {
            val sequence = ordinal * 8192 + attempt
            val candidate = PlaygroundLayout(completion.placementId,
                (radical(sequence, 2) - 0.5) * (world.width - 1.5),
                (radical(sequence, 3) - 0.5) * (world.height - 1.5), 0.0)
            if (validPlaygroundLayout(candidate, world) && result.all { gap(candidate, it) >= 0.25 }) { found = candidate; break }
        }
        check(found != null) { "No clear space was found. Your arrangement is safe; retry Playground." }
        result += found
    }
    return result
}

/** Screen = world * scale + offset. Pinch preserves its world focus. */
internal data class PlaygroundCamera(val scale: Double, val offset: WorldPoint) {
    fun world(screen: WorldPoint) = (screen - offset) * (1 / scale)
    fun zoom(focus: WorldPoint, factor: Double, min: Double, max: Double): PlaygroundCamera {
        val next = (scale * factor).coerceIn(min, max)
        return PlaygroundCamera(next, focus - world(focus) * next)
    }
}

/** Absolute preset is all-or-nothing: validate the entire shortest arc with existing rotation. */
internal fun presetPlaygroundOrientation(s: PlaygroundLayout, target: Double, others: List<PlaygroundLayout>, w: PlaygroundWorld): PlaygroundLayout? {
    require(target == 0.0 || target == 45.0)
    val delta = ((target - normalizePlaygroundAngle(s.rotationDegrees) + 540.0) % 360.0) - 180.0
    val reached = rotatePlaygroundSquare(s, delta, others, w)
    val exact = s.copy(rotationDegrees = target)
    val error = abs(((reached.rotationDegrees - target + 540.0) % 360.0) - 180.0)
    return exact.takeIf { error < 1e-7 && validPlaygroundLayout(it, w) && others.none { other -> playgroundOverlaps(it, other) } }
}
