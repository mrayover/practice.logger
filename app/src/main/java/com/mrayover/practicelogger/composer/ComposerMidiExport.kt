package com.mrayover.practicelogger.composer

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.OutputStream

/** Writes SMF directly from the same absolute tick schedules as Composer playback. */
internal object ComposerMidiExport {
    private const val MAX_NOTES = 200_000L
    private const val MAX_DELTA = 0x0fff_ffffL
    // Two events per note at most seven bytes each, plus names and long-gap separators.
    const val MAX_FILE_BYTES = 3_000_000L

    private val programs = mapOf(
        "acoustic-piano" to 0, "fm-electric-piano" to 5, "organ" to 16,
        "electric-bass" to 33, "synth-bass" to 38, "synth-strings" to 50,
        "synth-lead" to 81, "synth-pad" to 89
    )

    fun checkSize(plan: LoopPlan) {
        require(plan.tracks.sumOf { it.notes.size * (plan.regionTicks / it.length) } <= MAX_NOTES) {
            "This MIDI export exceeds 200,000 notes. Use fewer notes or pattern repetitions."
        }
    }

    fun write(snapshot: ComposerExportSnapshot, destination: OutputStream, checkCancelled: () -> Unit = {}) {
        val (project, plan) = snapshot
        checkSize(plan)
        val populated = plan.tracks.indices.filter { plan.tracks[it].notes.isNotEmpty() }
        val output = DataOutputStream(destination)
        output.writeInt(0x4d546864) // MThd
        output.writeInt(6)
        output.writeShort(1)
        output.writeShort(1 + populated.size)
        output.writeShort(COMPOSER_PPQ)

        val conductor = Track()
        conductor.name(project.title)
        val tempo = (60_000_000 + plan.tempo / 2) / plan.tempo
        conductor.meta(0, 0x51, byteArrayOf((tempo shr 16).toByte(), (tempo shr 8).toByte(), tempo.toByte()))
        conductor.meta(0, 0x58, byteArrayOf(
            project.numerator.toByte(), Integer.numberOfTrailingZeros(project.denominator).toByte(),
            (24 * 4 / project.denominator).toByte(), 8
        ))
        var bytes = 14L + conductor.finish(output, plan.regionTicks)
        val spareChannels = (0..15).filter { it != 9 && it !in populated }.iterator()
        for (index in populated) {
            checkCancelled()
            val track = plan.tracks[index]
            val instrument = ComposerInstruments.all.getOrNull(track.bank)
            // Composer supports eight instruments, so melodic channels never reach channel 10.
            val drums = instrument?.drums == true
            val channel = if (drums) 9 else index
            val chunk = Track()
            chunk.name(project.tracks[index].title)
            val program = programs[instrument?.id]
            program?.let { chunk.program(0, channel, it) }
            val voices = NoteChannels(channel, track.notes.size, drums, spareChannels)
            var offset = 0L
            while (offset < plan.regionTicks) {
                for (event in track.events) {
                    checkCancelled()
                    // The scheduler orders releases first, including across adjacent Loop cycles.
                    val note = track.notes[event.note]
                    val tick = offset + event.tick
                    val voice = voices.channel(event, note.pitch) { extraChannel ->
                        program?.let { chunk.program(tick, extraChannel, it) }
                    }
                    chunk.note(tick, voice, note, event.on)
                }
                offset += track.length
            }
            bytes += chunk.finish(output, plan.regionTicks)
            check(bytes <= MAX_FILE_BYTES) { "This MIDI export exceeds the file size limit." }
        }
        checkCancelled()
        output.flush()
    }

    /**
     * MIDI 1.0 has no note ID. Give simultaneous melodic voices of the same pitch
     * distinct channels when possible, while keeping the instrument in one SMF track.
     */
    private class NoteChannels(
        private val primary: Int,
        noteCount: Int,
        private val drums: Boolean,
        private val spareChannels: Iterator<Int>
    ) {
        private val owned = mutableListOf(primary)
        private val assigned = IntArray(noteCount)
        private val held = Array(16) { IntArray(128) }

        fun channel(event: LoopEvent, pitch: Int, configure: (Int) -> Unit): Int {
            if (drums) return primary
            if (!event.on) {
                val channel = assigned[event.note]
                held[channel][pitch]--
                return channel
            }
            val channel = owned.firstOrNull { held[it][pitch] == 0 }
                ?: if (spareChannels.hasNext()) {
                    spareChannels.next().also { owned.add(it); configure(it) }
                } else primary
            assigned[event.note] = channel
            held[channel][pitch]++
            return channel
        }
    }

    private class Track {
        private val data = ByteArrayOutputStream()
        private var previous = 0L

        private fun delta(tick: Long) {
            var remaining = tick - previous
            check(remaining >= 0) { "MIDI events are out of order." }
            // A legal empty text event carries time across gaps larger than one SMF delta.
            while (remaining > MAX_DELTA) {
                vlq(MAX_DELTA)
                data.write(0xff)
                data.write(0x01)
                data.write(0)
                remaining -= MAX_DELTA
            }
            vlq(remaining)
            previous = tick
        }

        private fun vlq(value: Long) {
            check(value in 0..MAX_DELTA)
            var shift = 0
            while (value shr (shift + 7) != 0L) shift += 7
            while (shift > 0) {
                data.write(((value shr shift).toInt() and 0x7f) or 0x80)
                shift -= 7
            }
            data.write(value.toInt() and 0x7f)
        }

        fun meta(tick: Long, type: Int, payload: ByteArray) {
            delta(tick)
            data.write(0xff)
            data.write(type)
            vlq(payload.size.toLong())
            data.write(payload)
        }

        fun name(value: String) = meta(0, 0x03, value.toByteArray(Charsets.UTF_8))

        fun program(tick: Long, channel: Int, program: Int) {
            delta(tick)
            data.write(0xc0 or channel)
            data.write(program)
        }

        fun note(tick: Long, channel: Int, note: CompositionNote, on: Boolean) {
            delta(tick)
            data.write((if (on) 0x90 else 0x80) or channel)
            data.write(note.pitch)
            data.write(if (on) note.velocity else 0)
        }

        fun finish(output: DataOutputStream, end: Long): Long {
            meta(end, 0x2f, byteArrayOf())
            output.writeInt(0x4d54726b) // MTrk
            output.writeInt(data.size())
            data.writeTo(output)
            return 8L + data.size()
        }
    }
}
