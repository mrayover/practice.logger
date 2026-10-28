package com.mrayover.practicelogger

import kotlin.math.*

internal data class TuningPitch(val frequency: Double, val midi: Int, val cents: Double) {
    val note: String get() = arrayOf("C", "C♯", "D", "D♯", "E", "F", "F♯", "G", "G♯", "A", "A♯", "B")[Math.floorMod(midi, 12)] + (midi / 12 - 1)
}

/** YIN cumulative normalized difference, with an RMS gate and parabolic lag refinement. */
internal object PitchDetector {
    fun detect(pcm: ShortArray, sampleRate: Int): TuningPitch? {
        if (pcm.size < 4096 || sampleRate !in 44_100..48_000) return null
        val samples = DoubleArray(pcm.size / 2) { i -> (pcm[i * 2].toDouble() + pcm[i * 2 + 1]) / (2 * 32768.0) }
        val mean = samples.average()
        for (i in samples.indices) samples[i] -= mean
        if (sqrt(samples.sumOf { it * it } / samples.size) < 0.008) return null
        val rate = sampleRate / 2.0
        val maxLag = min((rate / 50).toInt() + 1, samples.size / 2 - 1)
        val window = samples.size - maxLag
        val normalized = DoubleArray(maxLag + 1) { 1.0 }
        var sum = 0.0
        for (lag in 1..maxLag) {
            var difference = 0.0
            for (i in 0 until window) { val d = samples[i] - samples[i + lag]; difference += d * d }
            sum += difference
            normalized[lag] = if (sum > 0) difference * lag / sum else 1.0
        }
        var lag = max(2, (rate / 1600).toInt())
        while (lag < maxLag - 1) {
            if (normalized[lag] < 0.12) {
                while (lag + 1 < maxLag && normalized[lag + 1] < normalized[lag]) lag++
                val a = normalized[lag - 1]; val b = normalized[lag]; val c = normalized[lag + 1]
                val denominator = 2 * (a - 2 * b + c)
                val refined = lag + if (abs(denominator) > 1e-12) ((a - c) / denominator).coerceIn(-1.0, 1.0) else 0.0
                val frequency = rate / refined
                if (frequency !in 50.0..1600.0) return null
                val fractionalMidi = 69 + 12 * log2(frequency / 440.0)
                val midi = fractionalMidi.roundToInt()
                return TuningPitch(frequency, midi, (fractionalMidi - midi) * 100)
            }
            lag++
        }
        return null
    }
}
