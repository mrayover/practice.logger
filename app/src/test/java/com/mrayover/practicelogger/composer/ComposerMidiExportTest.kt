package com.mrayover.practicelogger.composer

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ComposerMidiExportTest {
    private fun note(pitch: Int, start: Long, duration: Long, velocity: Int = 90) =
        CompositionNote(newCompositionId(), pitch, start, duration, velocity)

    private fun pattern(bars: Int, vararg notes: CompositionNote) =
        CompositionPattern(newCompositionId(), "Pattern", bars, notes.toList())

    private fun track(title: String, instrument: Int, vararg patterns: CompositionPattern) =
        CompositionTrack(newCompositionId(), title, ComposerInstruments.all[instrument].reference, patterns.toList())

    private fun project(vararg tracks: CompositionTrack) =
        CompositionProject.empty("MIDI test", 137, 7, 8).copy(tracks = tracks.toList())

    private fun export(p: CompositionProject, arrangement: Boolean = false, selections: List<String> = emptyList()): ByteArray {
        val snapshot = ComposerExportSnapshot.capture(p, selections, arrangement, ComposerExportFormat.MIDI)
        return ByteArrayOutputStream().also { ComposerMidiExport.write(snapshot, it) }.toByteArray()
    }

    private data class NoteEvent(val tick: Long, val channel: Int, val on: Boolean, val pitch: Int, val velocity: Int)
    private data class MidiTrack(
        val end: Long, val meta: Map<Int, ByteArray>, val notes: List<NoteEvent>, val programs: List<Int>
    )
    private data class MidiFile(val resolution: Int, val tracks: List<MidiTrack>) {
        val tickLength get() = tracks.maxOf { it.end }
    }

    private fun DataInputStream.vlq(): Int {
        var value = 0
        repeat(4) {
            val byte = readUnsignedByte()
            value = value * 128 + (byte and 127)
            if (byte < 128) return value
        }
        error("Invalid MIDI variable-length quantity")
    }

    private fun parse(bytes: ByteArray): MidiFile {
        val input = DataInputStream(ByteArrayInputStream(bytes))
        assertEquals(0x4d546864, input.readInt())
        assertEquals(6, input.readInt())
        assertEquals(1, input.readUnsignedShort())
        val count = input.readUnsignedShort()
        val resolution = input.readUnsignedShort()
        val tracks = List(count) {
            assertEquals(0x4d54726b, input.readInt())
            val chunk = ByteArray(input.readInt()).also(input::readFully)
            val data = DataInputStream(ByteArrayInputStream(chunk))
            val meta = mutableMapOf<Int, ByteArray>()
            val notes = mutableListOf<NoteEvent>()
            val programs = mutableListOf<Int>()
            var tick = 0L
            while (data.available() > 0) {
                tick += data.vlq()
                val status = data.readUnsignedByte()
                when {
                    status == 0xff -> {
                        val type = data.readUnsignedByte()
                        meta[type] = ByteArray(data.vlq()).also(data::readFully)
                        if (type == 0x2f) assertEquals(0, data.available())
                    }
                    status in 0xc0..0xcf -> programs += data.readUnsignedByte().also { assertTrue(it < 128) }
                    status in 0x80..0x9f -> {
                        val pitch = data.readUnsignedByte()
                        val velocity = data.readUnsignedByte()
                        assertTrue(pitch < 128 && velocity < 128)
                        notes += NoteEvent(tick, status and 15, status >= 0x90, pitch, velocity)
                    }
                    else -> error("Unexpected MIDI status ${status.toString(16)}")
                }
            }
            assertArrayEquals(byteArrayOf(), meta[0x2f])
            MidiTrack(tick, meta, notes, programs)
        }
        assertEquals(0, input.available())
        return MidiFile(resolution, tracks)
    }

    @Test fun typeOneConductorAndSingleTrackUseExactTicks() {
        val p = project(track("Solo piano", 0, pattern(1, note(64, 127, 129, 111))))
        val bytes = export(p)
        assertArrayEquals(byteArrayOf(77, 84, 104, 100, 0, 0, 0, 6, 0, 1, 0, 2, 3, -64), bytes.copyOf(14))
        val seq = parse(bytes)
        assertEquals(960, seq.resolution)
        assertEquals(3360L, seq.tickLength)
        assertEquals("Solo piano", String(seq.tracks[1].meta.getValue(3), Charsets.UTF_8))
        assertArrayEquals(byteArrayOf(7, 3, 12, 8), seq.tracks[0].meta[0x58])
        val usPerQuarter = seq.tracks[0].meta.getValue(0x51).fold(0) { result, b -> (result shl 8) or (b.toInt() and 255) }
        assertEquals(437956, usPerQuarter)
        assertEquals(emptyList<NoteEvent>(), seq.tracks[0].notes)
        assertEquals(listOf(NoteEvent(127, 0, true, 64, 111), NoteEvent(256, 0, false, 64, 0)), seq.tracks[1].notes)
        assertTrue(seq.tracks.all { it.end == seq.tickLength })
    }

    @Test fun loopUsesSelectionRepeatsPolyphonyAndNoteOffBeforeRetrigger() {
        val chosen = pattern(1, note(60, 0, 960), note(64, 0, 100), note(60, 240, 240), note(60, 960, 960))
        val p = project(
            track("Keys", 1, pattern(1, note(20, 0, 10)), chosen),
            track("Bass", 3, pattern(2, note(36, 0, 480))),
            track("Drums", 8, pattern(1, note(38, 0, 30, 53))),
            track("Empty", 0, pattern(4))
        )
        val before = CompositionCodec.encode(p)
        val bytes = export(p, selections = listOf(chosen.id))
        assertArrayEquals(bytes, export(p, selections = listOf(chosen.id)))
        assertEquals(before, CompositionCodec.encode(p))
        val seq = parse(bytes)
        assertEquals(4, seq.tracks.size)
        assertEquals(4 * p.barTicks(), seq.tickLength)
        val keys = seq.tracks[1].notes
        assertEquals(listOf(0L, 240L, 960L, 3360L, 3600L, 4320L, 6720L, 6960L, 7680L, 10080L, 10320L, 11040L),
            keys.filter { it.on && it.pitch == 60 }.map { it.tick })
        assertEquals(listOf(0L, 3360L, 6720L, 10080L), keys.filter { it.on && it.pitch == 64 }.map { it.tick })
        assertEquals(listOf(false, true), keys.filter { it.tick == 960L }.map { it.on })
        val nested = keys.filter { it.tick == 240L || it.tick == 480L }
        assertEquals(2, nested.size)
        assertTrue(nested[0].channel != 0)
        assertEquals(nested[0].channel, nested[1].channel)
        assertFalse(keys.any { it.pitch == 20 })
        assertEquals(listOf(0L, 6720L), seq.tracks[2].notes.filter { it.on }.map { it.tick })
        assertTrue(seq.tracks[3].notes.all { it.channel == 9 && it.pitch == 38 })
        assertEquals(listOf(33), seq.tracks[2].programs)
    }

    @Test fun arrangementPreservesRepeatedClipVariationAndVeryLongGaps() {
        val source = pattern(1, note(61, 120, 960))
        val t = track("Keys", 0, source)
        var p = project(t)
        val bar = p.barTicks()
        p = ArrangementEdits.add(p, t.id, source.id, 2 * bar)
        p = ArrangementEdits.repeat(p, p.clips.single().id, 2)
        val far = ArrangementEdits.MAX_START_TICK / bar * bar
        p = ArrangementEdits.add(p, t.id, source.id, far)
        p = ArrangementEdits.makeUnique(p, p.clips.last().id)
        val variation = p.tracks.single().patterns.last()
        p = p.replacePattern(variation.copy(notes = variation.notes.map { it.copy(pitch = 72, velocity = 47) }))
        val seq = parse(export(p, arrangement = true))
        assertEquals(listOf(2 * bar + 120, 3 * bar + 120, 4 * bar + 120, far + 120),
            seq.tracks[1].notes.filter { it.on }.map { it.tick })
        assertEquals(NoteEvent(far + 120, 0, true, 72, 47), seq.tracks[1].notes.first { it.pitch == 72 })
        assertEquals(far + 1080, seq.tracks[1].notes.last().tick)
        assertEquals(far + bar, seq.tickLength)
        assertTrue(seq.tracks.all { it.end == far + bar })
        val invalid = p.copy(clips = p.clips + p.clips.first().copy(id = newCompositionId(), trackId = newCompositionId()))
        assertTrue(runCatching { export(invalid, arrangement = true) }.isFailure)
    }

    @Test fun emptyTracksAndExpansionLimitsAreSafe() {
        val empty = project(track("Empty", 0, pattern(2)))
        assertEquals(1, parse(export(empty)).tracks.size)
        assertEquals(2 * empty.barTicks(), parse(export(empty)).tickLength)
        val dense = pattern(1, *Array(12_501) { note(60, 0, 1) })
        val oversized = project(track("Dense", 0, dense), track("Long", 0, pattern(16)))
        assertTrue(runCatching { export(oversized) }.exceptionOrNull()?.message.orEmpty().contains("200,000"))
        assertTrue(runCatching { export(empty, arrangement = true) }.isFailure)
    }
}
