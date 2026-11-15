package com.mrayover.practicelogger.composer

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import android.app.Application
import android.os.Looper
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import androidx.test.core.app.ApplicationProvider
import org.robolectric.Shadows.shadowOf
import java.io.File
import java.time.Duration

@RunWith(RobolectricTestRunner::class) @Config(sdk=[35])
class PianoRollEditsTest {
    private fun fixture(): CompositionProject {
        val a = CompositionPattern(newCompositionId(),"A",4,listOf(CompositionNote(newCompositionId(),60,137,319,47),CompositionNote(newCompositionId(),67,777,160,113)))
        val b = CompositionPattern(newCompositionId(),"B",4)
        return CompositionProject.empty("C2 exact notes",137,7,8).copy(tracks=listOf(CompositionTrack(newCompositionId(),"Piano",InstrumentReference(1,"reference-piano","default"),listOf(a,b))))
    }
    @Test fun allSubdivisionsAreCanonicalAndFreeTimingIsNeverQuantized() {
        assertEquals(listOf(960L,480L,240L,120L,320L,160L,80L),RollGrid.entries.map { it.ticks })
        RollGrid.entries.forEach { grid ->
            assertEquals(grid.ticks*3,snapTick(grid.ticks*3.1,grid,true))
            assertEquals(137L,snapTick(137.0,grid,false))
        }
        val p=fixture(); assertEquals(3360L,p.barTicks())
        assertEquals(p,CompositionCodec.decode(CompositionCodec.encode(p)))
    }
    @Test fun snappingAtPatternEdgesStaysOnGridAndFreeTicksRemainExact() {
        assertEquals(3120L,boundedTick(3360.0,0,3359,RollGrid.SIXTEENTH,true))
        assertEquals(240L,boundedTick(-400.0,1,1000,RollGrid.SIXTEENTH,true,1000))
        assertEquals(80L,boundedTick(1.0,1,79+1L,RollGrid.THIRTY_SECOND_TRIPLET,true,80))
        assertEquals(137L,boundedTick(137.0,0,3359,RollGrid.QUARTER,false))
        assertEquals(1L,boundedTick(-400.0,1,1000,RollGrid.QUARTER,false))
    }
    @Test fun crossTrackPastePreservesValuesAndSourceWithFreshIdsAndGrowsWithoutOverwriting() {
        val p=fixture(); val a=p.tracks[0].patterns[0]
        val copies=pastedNotes(a.notes,13000)
        assertEquals(listOf(13000L,13640L),copies.map { it.startTick })
        assertEquals(a.notes.map { it.pitch },copies.map { it.pitch })
        assertEquals(a.notes.map { it.durationTick },copies.map { it.durationTick })
        assertEquals(a.notes.map { it.velocity },copies.map { it.velocity })
        assertTrue(copies.none { n -> a.notes.any { it.id==n.id } })
        val other=CompositionTrack(newCompositionId(),"Piano 2",p.tracks[0].instrument,listOf(CompositionPattern(newCompositionId(),"B",4)))
        val next=p.copy(tracks=p.tracks+other).withNotes(other.patterns[0],copies+ pastedNotes(a.notes,13000))
        assertEquals(8,next.tracks[1].patterns[0].lengthBars)
        assertEquals(4,next.tracks[1].patterns[0].notes.size)
        assertEquals(a,next.tracks[0].patterns[0]); CompositionCodec.validate(next)
        assertTrue(runCatching { next.withNotes(next.tracks[1].patterns[0],pastedNotes(a.notes,999999)) }.isFailure)
    }
    private fun await(condition: () -> Boolean) {
        val deadline=System.nanoTime()+10_000_000_000L
        while(!condition() && System.nanoTime()<deadline) { shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(30)); Thread.sleep(10) }
        assertTrue(condition())
    }
    @Test fun sessionUndoAcrossPatternsSaveExactValuesAndDiskDraftRecovery() {
        val app=ApplicationProvider.getApplicationContext<Application>()
        val store=CompositionStore(File(app.filesDir,"composer/projects"))
        val p=store.create(fixture())
        val state=SavedStateHandle(mapOf(ComposerActivity.PROJECT_ID to p.id))
        val vm=ComposerViewModel(app,state); await { !vm.loading }; assertNull(vm.error)
        val a=p.tracks[0].patterns[0]; val b=p.tracks[0].patterns[1]
        val moved=a.notes.map { it.copy(pitch=it.pitch+2,startTick=it.startTick+17) }
        vm.edit(p.withNotes(a,moved)); assertEquals(1,vm.undoCount)
        val copied=pastedNotes(moved,3000)
        vm.edit(vm.project!!.withNotes(b,copied)); assertEquals(2,vm.undoCount)
        vm.undo(); assertEquals(emptyList<CompositionNote>(),vm.project!!.tracks[0].patterns[1].notes)
        vm.undo(); assertEquals(p,vm.project); assertEquals(0,vm.undoCount)
        vm.edit(p.withNotes(a,moved))
        val draftId=state.get<String>("draftFile")!!
        val file=File(app.filesDir,"composer/drafts/$draftId.json")
        // Earlier and final edits can have identical values. Do not mistake an
        // intermediate matching file for completion while queued writes still drain.
        await { vm.viewModelScope.coroutineContext[Job]!!.children.none { it.isActive } }
        await { file.isFile && runCatching { CompositionCodec.decode(file.readText())==vm.project }.getOrDefault(false) }
        assertNull(state.get<String>("draft"))
        val restored=ComposerViewModel(app,SavedStateHandle(mapOf(ComposerActivity.PROJECT_ID to p.id,"draftFile" to draftId)))
        await { !restored.loading }; assertEquals(vm.project,restored.project); assertEquals(p,store.read(p.id))
        restored.save(); await { !restored.busy }; assertNull(restored.error)
        val saved=store.read(p.id); assertEquals(moved,saved.tracks[0].patterns[0].notes)
        assertEquals(saved,CompositionCodec.decode(CompositionCodec.encode(saved)))
        store.delete(saved)
    }
}
