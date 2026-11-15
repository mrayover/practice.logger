package com.mrayover.practicelogger.composer

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.nio.file.Files

@RunWith(RobolectricTestRunner::class) @Config(sdk=[35])
class ComposerManagementTest {
    @Test fun instrumentLabelsUseAssignedTypeWithoutRewritingMetadata() {
        val t=CompositionTrack(newCompositionId(),"Piano 3",ComposerInstruments.all[7].reference)
        assertEquals("3 · Synth pad",ComposerInstruments.trackLabel(t,2))
        assertEquals("3 · Electronic drums",ComposerInstruments.trackLabel(t.copy(instrument=ComposerInstruments.all[8].reference),2))
        val ref=ComposerPreset(newCompositionId(),"Warm Sweep","synth-pad",ComposerSound()).reference
        assertEquals("3 · Synth pad — Warm Sweep",ComposerInstruments.trackLabel(t.copy(instrument=ref),2))
        assertEquals("3 · Countermelody — Synth pad — Warm Sweep",ComposerInstruments.trackLabel(t.copy(title="Countermelody",instrument=ref),2))
        assertEquals("Piano 3",t.title)
        assertEquals("Acoustic piano",ComposerInstruments.identity(InstrumentReference(1,"reference-piano","default")))
    }

    private fun base(): CompositionProject = ComposerManagement.addTrack(CompositionProject.empty("Management"))
    private fun rejects(block: () -> Unit) { assertTrue(runCatching(block).isFailure) }
    @Test fun fullPatternCapacityAlphabetAndLengthValidation() {
        var p=base(); val id=p.tracks.first().id
        repeat(255) { p=ComposerManagement.addPattern(p,id) }
        val t=p.tracks.first(); assertEquals(256,t.patterns.size)
        assertEquals(listOf("Y","Z","AA","AB"),t.patterns.drop(24).take(4).map { it.title })
        assertEquals(256,t.patterns.map { it.title }.toSet().size); assertTrue(t.patterns.all { it.lengthBars==4 })
        rejects { ComposerManagement.addPattern(p,id) }; rejects { ComposerManagement.addPattern(p,id,t.patterns.first().id) }
        val original=t.patterns.first(); val note=CompositionNote(newCompositionId(),60,8*p.barTicks()-960,960,71)
        val longer=p.replacePattern(original.copy(lengthBars=8,notes=listOf(note)))
        rejects { CompositionCodec.validate(longer.replacePattern(longer.tracks[0].patterns[0].copy(lengthBars=4))) }
    }
    @Test fun duplicateRenameAndReferencesPreserveMusicalIdentity() {
        var p=base(); val t=p.tracks[0]; val a=t.patterns[0].copy(lengthBars=16,notes=listOf(CompositionNote(newCompositionId(),43,320,1600,67)))
        p=p.replacePattern(a); val clip=ArrangementClip(newCompositionId(),t.id,a.id,3840)
        p=p.copy(clips=listOf(clip)); val copied=ComposerManagement.addPattern(p,t.id,a.id); val b=copied.tracks[0].patterns.last()
        assertNotEquals(a.id,b.id); assertNotEquals(a.notes[0].id,b.notes[0].id)
        assertEquals(a.notes[0].copy(id=b.notes[0].id),b.notes[0]); assertEquals(16,b.lengthBars)
        val edited=copied.withNotes(b,b.notes.map { it.copy(pitch=70) }); assertEquals(a,edited.tracks[0].patterns[0])
        val renamed=ComposerManagement.renamePattern(edited,t.id,a.id," New A ")
        assertEquals(a.copy(title="New A"),renamed.tracks[0].patterns[0]); assertEquals(listOf(clip),renamed.clips)
        rejects { ComposerManagement.deletePattern(renamed,t.id,a.id) }; rejects { ComposerManagement.deleteTrack(renamed,t.id) }
        rejects { ComposerManagement.renamePattern(renamed,t.id,a.id," ") }; rejects { ComposerManagement.renameTrack(renamed,t.id,"x".repeat(121)) }
        rejects { ComposerManagement.renameTrack(renamed,t.id,"bad\nname") }
        assertEquals(a,ComposerManagement.deletePattern(copied,t.id,b.id).tracks[0].patterns[0])
    }
    @Test fun scopedDeletionEightTracksAndIndependentLoopSnapshots() {
        var p=base(); repeat(7) { p=ComposerManagement.addTrack(p) }; rejects { ComposerManagement.addTrack(p) }
        val first=p.tracks[0]; p=ComposerManagement.addPattern(p,first.id)
        val second=p.tracks[1]; val secondPattern=second.patterns[0]
        p=p.replacePattern(secondPattern.copy(lengthBars=8,notes=listOf(CompositionNote(newCompositionId(),45,0,960,90))))
        val ids=p.tracks.map { it.patterns.last().id }; val plan=LoopPlan.snapshot(p,ids)
        assertEquals(8*p.barTicks(),plan.regionTicks); assertEquals(p.tracks.map { it.id },plan.trackIds)
        val removed=ComposerManagement.deleteTrack(p,first.id); assertEquals(p.tracks.drop(1),removed.tracks)
        assertEquals(8,plan.tracks.size); assertEquals(45,plan.tracks[1].notes[0].pitch)
        assertEquals(45,LoopPlan.snapshot(removed,removed.tracks.map { it.patterns[0].id }).tracks[0].notes[0].pitch)
        var empty=removed; removed.tracks.forEach { empty=ComposerManagement.deleteTrack(empty,it.id) }
        assertTrue(empty.tracks.isEmpty()); assertEquals(1,ComposerManagement.addTrack(empty).tracks.size)
        val single=base(); val emptyTrack=ComposerManagement.deletePattern(single,single.tracks[0].id,single.tracks[0].patterns[0].id)
        assertTrue(emptyTrack.tracks[0].patterns.isEmpty()); assertEquals("A",ComposerManagement.addPattern(emptyTrack,emptyTrack.tracks[0].id).tracks[0].patterns[0].title)
    }
    @Test fun structuralChangesPersistV1AndSelfContainedV2WithoutMigration() {
        val dir=Files.createTempDirectory("composer-management").toFile()
        try {
            val store=CompositionStore(dir); var p=base(); val t=p.tracks[0]
            val legacy=p.copy(tracks=listOf(t.copy(title="Piano 3",instrument=ComposerInstruments.all[7].reference)))
            store.create(legacy); assertEquals(legacy,store.read(p.id)); assertEquals("1 · Synth pad",ComposerInstruments.trackLabel(store.read(p.id).tracks[0],0))
            p=ComposerManagement.renameTrack(legacy,t.id,"Countermelody")
            p=ComposerManagement.addPattern(p,t.id); p=ComposerManagement.addPattern(p,t.id,p.tracks[0].patterns[0].id)
            val ref=ComposerPreset(newCompositionId(),"Warm Sweep","synth-pad",ComposerSound(cutoffHz=850f)).reference
            p=p.copy(formatVersion=2,tracks=p.tracks.map { it.copy(instrument=ref) }); val saved=store.save(p)
            assertEquals(saved,CompositionStore(dir).read(p.id)); assertEquals("1 · Countermelody — Synth pad — Warm Sweep",ComposerInstruments.trackLabel(saved.tracks[0],0))
            assertEquals(t.id,saved.tracks[0].id); assertEquals(ref,saved.independentCopy("Copy").tracks[0].instrument)
        } finally { dir.deleteRecursively() }
    }
}
