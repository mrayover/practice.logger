package com.mrayover.practicelogger

import android.content.res.AssetManager
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import org.json.JSONObject

internal class PlaygroundSprite private constructor(
    val image: ImageBitmap,
    val idle: Frame,
    private val walk: List<Frame>
) {
    class Frame(
        val offset: IntOffset,
        val size: IntSize,
        val feet: Int,
        val durationNanos: Long
    )

    private val walkCycleNanos = walk.sumOf { it.durationNanos }

    fun frameAt(walkElapsedNanos: Long): Frame {
        if (walkElapsedNanos < 0) return idle
        var remaining = walkElapsedNanos % walkCycleNanos
        for (frame in walk) {
            if (remaining < frame.durationNanos) return frame
            remaining -= frame.durationNanos
        }
        error("Invalid walk cycle")
    }

    companion object {
        fun load(assets: AssetManager): PlaygroundSprite {
            val export = assets.open("playground/Batel.json").bufferedReader().use { JSONObject(it.readText()) }
            val meta = export.getJSONObject("meta")
            val bitmap = assets.open("playground/${meta.getString("image")}").use {
                checkNotNull(BitmapFactory.decodeStream(it))
            }
            // Aseprite's JSON Hash export lists frames in timeline order.
            val frames = export.getJSONObject("frames").let { entries ->
                entries.keys().asSequence().map { key ->
                    val entry = entries.getJSONObject(key)
                    require(!entry.getBoolean("rotated") && !entry.getBoolean("trimmed"))
                    val rect = entry.getJSONObject("frame")
                    val offset = IntOffset(rect.getInt("x"), rect.getInt("y"))
                    val size = IntSize(rect.getInt("w"), rect.getInt("h"))
                    // Anchor the lowest opaque row to the same floor point in every pose.
                    val feet = (size.height - 1 downTo 0).first { y ->
                        (0 until size.width).any { x -> bitmap.getPixel(offset.x + x, offset.y + y) ushr 24 != 0 }
                    } + 1
                    val duration = entry.getLong("duration")
                    require(duration > 0)
                    Frame(offset, size, feet, duration * 1_000_000L)
                }.toList()
            }
            val tags = meta.getJSONArray("frameTags")
            fun taggedFrames(name: String): List<Frame> {
                val tag = (0 until tags.length()).asSequence().map { tags.getJSONObject(it) }
                    .first { it.getString("name") == name }
                require(tag.getString("direction") == "forward")
                return frames.subList(tag.getInt("from"), tag.getInt("to") + 1)
            }
            return PlaygroundSprite(bitmap.asImageBitmap(), taggedFrames("idle").first(), taggedFrames("walk"))
        }
    }
}
