package com.mrayover.practicelogger

import kotlin.math.*

fun pianoFrequency(note: Int): Double = 440.0 * 2.0.pow((note - 69) / 12.0)
fun pianoNoteName(note: Int): String = arrayOf("C", "C♯", "D", "D♯", "E", "F", "F♯", "G", "G♯", "A", "A♯", "B")[Math.floorMod(note, 12)] + (note / 12 - 1)
internal fun pianoBlack(note: Int) = Math.floorMod(note, 12) in listOf(1, 3, 6, 8, 10)

/** Shared drawing/hit geometry: black keys take precedence over the white keys beneath them. */
internal class PianoGeometry(val first: Int, val last: Int) {
    val whites = (first..last).filterNot(::pianoBlack)
    val blacks = (first..last).filter(::pianoBlack)
    fun blackLeft(note: Int): Float = whites.count { it < note } - .31f
    fun hit(x: Float, y: Float, width: Float, height: Float): Int? {
        if (x < 0 || y < 0 || x >= width || y >= height) return null
        val unit = width / whites.size
        if (y < height * .62f) blacks.firstOrNull { x / unit >= blackLeft(it) && x / unit < blackLeft(it) + .62f }?.let { return it }
        return whites[(x / unit).toInt().coerceIn(whites.indices)]
    }
}

/** Bounded, allocation-free per sample synthesis. Tokens distinguish fingers on the same note.
 * At capacity, ignore new presses until a voice releases; never cut a sounding voice mid-wave.
 */
internal class PianoPcm(private val rate: Int = 48_000) {
    private class Voice(val token: Long, val note: Int, var phase: Double = 0.0, var age: Int = 0,
        var release: Int = -1, var releaseLevel: Double = 0.0)
    private val voices = mutableListOf<Voice>()
    private val attack = rate * .006
    private val releaseFrames = (rate * .045).toInt()
    private fun level(v: Voice) = min(1.0, v.age / attack) * (.65 + .35 * exp(-v.age / (rate * .18)))
    @Synchronized fun noteOn(token: Long, note: Int): Boolean {
        require(note in 24..96)
        if (voices.any { it.token == token && it.release < 0 } || voices.size >= 16) return false
        voices.add(Voice(token, note)); return true
    }
    @Synchronized fun noteOff(token: Long) { voices.filter { it.token == token && it.release < 0 }.forEach { it.releaseLevel = level(it); it.release = 0 } }
    @Synchronized fun stopAll() { voices.forEach { if (it.release < 0) { it.releaseLevel = level(it); it.release = 0 } } }
    @Synchronized fun clear() = voices.clear()
    @Synchronized fun voiceCount() = voices.size
    @Synchronized fun render(output: ShortArray) {
        for (i in output.indices) {
            var mix = 0.0
            for (v in voices) {
                val frequency = pianoFrequency(v.note)
                val envelope = if (v.release < 0) level(v) else v.releaseLevel * (1.0 - v.release.toDouble() / releaseFrames).coerceAtLeast(0.0).pow(2)
                var tone = sin(v.phase)
                if (frequency * 2 < rate / 2) tone += .25 * sin(v.phase * 2) * exp(-v.age / (rate * .3))
                if (frequency * 3 < rate / 2) tone += .10 * sin(v.phase * 3) * exp(-v.age / (rate * .15))
                mix += tone * envelope * .24
                v.phase = (v.phase + 2 * PI * frequency / rate) % (2 * PI)
                v.age++; if (v.release >= 0) v.release++
            }
            output[i] = (tanh(mix) * 28_000).toInt().toShort()
            voices.removeAll { it.release >= releaseFrames }
        }
    }
}
