package com.mrayover.practicelogger.composer

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.nio.file.Files

@RunWith(RobolectricTestRunner::class) @Config(sdk=[35])
class ArrangementEditsTest {
    private fun base(): CompositionProject {
        val a=CompositionPattern(newCompositionId(),"A",2,listOf(CompositionNote(newCompositionId(),60,0,960,80)))
        val t=CompositionTrack(newCompositionId(),"Piano",ComposerInstruments.all[0].reference,listOf(a))
        return CompositionProject.empty("Arrange").copy(tracks=listOf(t,t.copy(id=newCompositionId(),title="Bass",instrument=ComposerInstruments.all[3].reference,
            patterns=listOf(a.copy(id=newCompositionId(),title="B",notes=a.notes.map { it.copy(id=newCompositionId()) })))))
    }
    private fun add(p: CompositionProject, bar: Long, track: Int=0)=ArrangementEdits.add(p,p.tracks[track].id,p.tracks[track].patterns[0].id,bar*p.barTicks())
    private fun rejects(block: () -> Unit) { assertTrue(runCatching(block).isFailure) }
    @Test fun sharedInstancesFreshIdsWholeBarsAndLaneIndependence() {
        val initial=base(); val p=add(add(add(initial,0),2),0,1)
        assertEquals(initial.tracks,p.tracks)
        assertEquals(3,p.clips.map { it.id }.toSet().size)
        assertTrue(p.clips.all { validCompositionId(it.id) && it.startTick%p.barTicks()==0L })
        assertEquals(p.clips[0].patternId,p.clips[1].patternId)
        assertEquals(2*p.barTicks(),ArrangementEdits.end(p,p.clips[0]))
        rejects { add(p,1) }; rejects { ArrangementEdits.add(initial,initial.tracks[0].id,initial.tracks[0].patterns[0].id,1) }
        rejects { add(initial,-1) }; rejects { add(initial,ArrangementEdits.MAX_START_TICK/p.barTicks()+1) }
        rejects { ArrangementEdits.add(initial,initial.tracks[0].id,initial.tracks[1].patterns[0].id,0) }
    }
    @Test fun moveDeleteAndImmutableUndoPreserveOwnership() {
        val p=add(add(base(),0),4); val c=p.clips[0]
        val moved=ArrangementEdits.move(p,c.id,2*p.barTicks())
        assertEquals(c.copy(startTick=2*p.barTicks()),moved.clips[0]); assertEquals(p.tracks,moved.tracks)
        rejects { ArrangementEdits.move(p,c.id,3*p.barTicks()) }
        val deleted=ArrangementEdits.delete(moved,c.id)
        assertEquals(listOf(moved.clips[1]),deleted.clips); assertEquals(p.tracks,deleted.tracks)
        // The existing history stores these immutable snapshots; restoring one restores the exact clip IDs.
        val history=ArrayDeque<CompositionProject>(); history.addLast(p); history.addLast(moved)
        assertEquals(moved,history.removeLast()); assertEquals(p,history.removeLast())
    }
    @Test fun patternLengthGuardAndNamesRemainSharedWithoutChangingLegacyReads() {
        val p=add(add(base(),0),2); val a=p.tracks[0].patterns[0]
        rejects { p.replacePattern(a.copy(lengthBars=4)) }
        val renamed=ComposerManagement.renamePattern(p,p.tracks[0].id,a.id,"Theme")
        assertTrue(renamed.clips.all { ArrangementEdits.pattern(renamed,it).title=="Theme" })
        val legacy=p.copy(clips=p.clips.map { it.copy(startTick=0) })
        assertEquals(legacy,CompositionCodec.decode(CompositionCodec.encode(legacy)))
        assertEquals(legacy.clips,legacy.replacePattern(a.copy(lengthBars=4)).clips)
    }
    @Test fun exactAtomicReopenAndSaveAsReferencesAcrossSupportedFormats() {
        val store=CompositionStore(Files.createTempDirectory("arrangement").toFile())
        for(v in 1..4) {
            val p=store.create(base().copy(formatVersion=v))
            val arranged=add(add(p,0),4,1); val saved=store.save(arranged)
            assertEquals(saved,store.read(saved.id)); assertEquals(saved,CompositionCodec.decode(CompositionCodec.encode(saved)))
            val copy=store.create(saved.independentCopy("Copy"))
            assertEquals(copy,store.read(copy.id)); assertNotEquals(saved.id,copy.id)
            copy.clips.forEachIndexed { i,c ->
                assertNotEquals(saved.clips[i].id,c.id); assertNotEquals(saved.clips[i].trackId,c.trackId)
                assertNotEquals(saved.clips[i].patternId,c.patternId)
                assertEquals(saved.clips[i].startTick,c.startTick); assertEquals(2,ArrangementEdits.pattern(copy,c).lengthBars)
            }
            assertEquals(saved,store.read(saved.id))
        }
    }
    @Test fun meterLengthsAndFarPlacementsUseDocumentLimit() {
        val p=base().copy(numerator=7,denominator=8); val a=p.tracks[0].patterns[0]
        assertEquals(3360L,p.barTicks())
        val long=p.replacePattern(a.copy(lengthBars=16)); val bar=ArrangementEdits.MAX_START_TICK/p.barTicks()
        val placed=add(long,bar); assertEquals(16*p.barTicks(),ArrangementEdits.end(placed,placed.clips[0])-placed.clips[0].startTick)
    }
}
