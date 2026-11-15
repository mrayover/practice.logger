package com.mrayover.practicelogger.composer

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class) @Config(sdk = [35])
class CompositionStoreTest {
    @get:Rule val folder = TemporaryFolder()
    private fun directory() = File(folder.root, "composer/projects")
    private fun store() = CompositionStore(directory())
    private fun project() = CompositionProject.empty("Etude", 137, 7, 8)
    private fun rejected(action: () -> Unit) { assertTrue("Expected rejection", runCatching(action).isFailure) }

    @Test fun createRenameOpenDeletePreserveIdentitySettingsAndUnrelatedFiles() {
        val sentinel = File(folder.root, "practice-data").apply { writeText("keep") }
        val p = store().create(project())
        assertTrue(p.tracks.isEmpty()); assertTrue(p.clips.isEmpty())
        assertEquals(p, store().read(p.id)); assertEquals(960, p.ticksPerQuarter)
        val renamed = store().rename(p, "New title")
        assertEquals(p.id, renamed.id); assertEquals(137, renamed.tempoBpm)
        assertEquals(7, renamed.numerator); assertEquals(8, renamed.denominator)
        assertEquals(1L, renamed.revision); assertTrue(renamed.modifiedAt > p.modifiedAt)
        assertEquals(renamed, store().list().projects.single())
        store().delete(renamed)
        assertTrue(store().list().projects.isEmpty()); assertEquals("keep", sentinel.readText())
    }
    @Test fun creationRetryNeverDuplicatesOrReplacesExistingDocument() {
        val p = store().create(project())
        val saved = store().rename(p, "Saved edit")
        assertEquals(saved, store().create(p)); assertEquals(1, store().list().projects.size)
        rejected { store().create(p.copy(createdAt = p.createdAt + 1, modifiedAt = p.modifiedAt + 1)) }
        rejected { store().read("../practice-data") }
    }
    @Test fun richMusicalDocumentRoundTripsIndependentPatternsAndSparseArrangement() {
        val note = CompositionNote(newCompositionId(), 64, 320, 640, 99)
        val a = CompositionPattern(newCompositionId(), "A", 2, listOf(note))
        val b = CompositionPattern(newCompositionId(), "B", 16)
        val instrument = InstrumentReference(1, "core.piano", "acoustic")
        val t = CompositionTrack(newCompositionId(), "Piano", instrument, listOf(a, b))
        val noLane = CompositionTrack(newCompositionId(), "Bass", InstrumentReference(1, "core.bass", "electric"))
        val p = project().copy(tracks = listOf(t, noLane), clips = listOf(ArrangementClip(newCompositionId(), t.id, a.id, 12345)))
        assertEquals(p, CompositionCodec.decode(CompositionCodec.encode(p)))
        store().create(p); assertEquals(p, store().read(p.id))
        assertEquals(2, p.tracks.size); assertEquals(1, p.clips.size)
    }
    @Test fun invalidAndFutureInputCannotReplaceValidSavedProject() {
        val p = store().create(project())
        listOf(p.copy(title = " "), p.copy(tempoBpm = 0), p.copy(denominator = 3), p.copy(formatVersion = 2), p.copy(ticksPerQuarter = 480)).forEach { bad -> rejected { store().save(bad) } }
        val encoded = JSONObject(CompositionCodec.encode(p))
        rejected { CompositionCodec.decode(encoded.put("tempoBpm", "137").toString()) }
        rejected { CompositionCodec.decode(CompositionCodec.encode(p).dropLast(1)) }
        rejected { CompositionCodec.decode(JSONObject(CompositionCodec.encode(p)).put("formatVersion", 2).toString()) }
        rejected { CompositionCodec.decode(JSONObject(CompositionCodec.encode(p)).put("newMusicalField", true).toString()) }
        assertEquals(p, store().read(p.id))
    }
    @Test fun interruptedAtomicWriteRetainsPreviousSavedVersionAndCleansTemporaryFile() {
        val p = store().create(project())
        val failing = CompositionStore(directory()) { error("Simulated interruption before atomic commit") }
        rejected { failing.save(p.copy(title = "Not committed")) }
        assertEquals(p, store().read(p.id))
        assertFalse(File(directory(), "${p.id}.json.new").exists())
        File(directory(), "${p.id}.json.new").outputStream().use { it.write("broken".toByteArray()); it.fd.sync() }
        assertEquals(p, store().read(p.id))
        assertFalse(File(directory(), "${p.id}.json.new").exists())
        val neverCommitted = newCompositionId()
        File(directory(), "$neverCommitted.json.new").writeText("partial")
        assertEquals(1, store().list().projects.size)
        assertFalse(File(directory(), "$neverCommitted.json.new").exists())
    }
    @Test fun corruptOrFutureFilesArePreservedAndStaleWritersCannotOverwrite() {
        val p = store().create(project())
        val saved = store().save(p.copy(title = "First writer"))
        rejected { store().save(p.copy(title = "Stale writer")) }
        rejected { store().delete(p) }; assertEquals(saved, store().read(p.id))
        val target = File(directory(), "${p.id}.json")
        val future = JSONObject(CompositionCodec.encode(saved)).put("formatVersion", 99).toString()
        target.writeText(future)
        assertEquals(1, store().list().unreadableCount)
        rejected { store().save(saved) }; assertEquals(future, target.readText())
        target.writeText("bad json")
        assertEquals(1, store().list().unreadableCount); assertEquals("bad json", target.readText())
    }
    @Test fun trackLimitClipOwnershipAndNoteBoundsAreValidated() {
        val i = InstrumentReference(1, "core.piano", "acoustic")
        val tracks = (1..8).map { CompositionTrack(newCompositionId(), "Track $it", i) }
        CompositionCodec.validate(project().copy(tracks = tracks))
        rejected { CompositionCodec.validate(project().copy(tracks = tracks + CompositionTrack(newCompositionId(), "Extra", i))) }
        rejected { CompositionCodec.validate(project().copy(tracks = tracks, clips = listOf(ArrangementClip(newCompositionId(), tracks[0].id, newCompositionId(), 0)))) }
        val note = CompositionNote(newCompositionId(), 60, 0, Long.MAX_VALUE, 100)
        rejected { CompositionCodec.validate(project().copy(tracks = listOf(tracks[0].copy(patterns = listOf(CompositionPattern(newCompositionId(), "A", 1, listOf(note))))))) }
    }
    @Test fun legacyAtomicBackupRecoveryAndFilenameIdentityMismatchFailSafely() {
        val p = store().create(project())
        val base = File(directory(), "${p.id}.json")
        assertTrue(base.renameTo(File(base.path + ".bak")))
        base.writeText("interrupted old-style replacement")
        assertEquals(p, store().list().projects.single())
        base.writeText(CompositionCodec.encode(p.copy(id = newCompositionId())))
        rejected { store().read(p.id) }; assertEquals(1, store().list().unreadableCount)
    }
}
