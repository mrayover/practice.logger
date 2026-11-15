package com.mrayover.practicelogger.composer

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class) @Config(sdk=[35])
class ComposerC4CTest {
    @Test fun velocityChangesOnlySelectedValuesAndPersistsIntoPlayback() {
        val notes=listOf(CompositionNote(newCompositionId(),60,137,319,47),CompositionNote(newCompositionId(),64,777,160,113),CompositionNote(newCompositionId(),67,1200,480,100))
        val ids=notes.take(2).map { it.id }.toSet()
        val edited=notes.withVelocity(ids,1)
        assertEquals(notes.take(2).map { it.copy(velocity=1) },edited.take(2))
        assertSame(notes[2],edited[2]); assertEquals(notes,notes.withVelocity(emptySet(),127))
        assertEquals(127,notes.withVelocity(setOf(notes[0].id),127)[0].velocity)
        assertTrue(runCatching { notes.withVelocity(ids,0) }.isFailure)
        val pattern=CompositionPattern(newCompositionId(),"A",4,edited)
        val p=CompositionProject.empty("Velocity").copy(tracks=listOf(CompositionTrack(newCompositionId(),"Track 1",ComposerInstruments.all[0].reference,listOf(pattern))))
        val dir=kotlin.io.path.createTempDirectory().toFile()
        try {
            val store=CompositionStore(dir); val saved=store.create(p); val reopened=CompositionStore(dir).read(saved.id)
            assertEquals(edited,reopened.tracks[0].patterns[0].notes)
            assertEquals(edited,LoopPlan.snapshot(reopened,listOf(pattern.id)).tracks[0].notes)
        } finally { dir.deleteRecursively() }
    }
    @Test fun independentPatternSelectionsUseOwnedIdsAndIndependentLengths() {
        val p=CompositionProject.empty("Mixer")
        val tracks=(0..7).map { i -> CompositionTrack(newCompositionId(),"Track ${i+1}",ComposerInstruments.all[i].reference,
            (0..2).map { j -> CompositionPattern(newCompositionId(),"Variation ${j+1}",listOf(1,4,8)[j],listOf(CompositionNote(newCompositionId(),40+i+j,0,240,20+i+j))) }) }
        val project=p.copy(tracks=tracks)
        val choices=tracks.mapIndexed { i,t -> t.patterns[i%3].id }
        val plan=LoopPlan.snapshot(project,choices)
        assertEquals(tracks.map { it.id },plan.trackIds)
        tracks.forEachIndexed { i,t -> assertEquals(t.patterns[i%3].notes,plan.tracks[i].notes); assertEquals(project.patternTicks(t.patterns[i%3]),plan.tracks[i].length) }
        assertEquals(project.barTicks()*8,plan.regionTicks)
        val changed=choices.toMutableList().also { it[1]=tracks[1].patterns[2].id }
        val next=LoopPlan.snapshot(project,changed)
        assertEquals(tracks[1].patterns[2].notes,next.tracks[1].notes)
        plan.tracks.indices.filter { it!=1 }.forEach { assertEquals(plan.tracks[it],next.tracks[it]) }
        assertEquals(tracks[0].patterns[0].notes,LoopPlan.snapshot(project,listOf(tracks[1].patterns[2].id)).tracks[0].notes)
    }
}
