package com.mrayover.practicelogger.composer

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class) @Config(sdk=[35])
class LoopPlanTest {
    private fun note(tick: Long,duration: Long=80,pitch: Int=60,velocity: Int=93)=CompositionNote(newCompositionId(),pitch,tick,duration,velocity)
    private fun project(): CompositionProject {
        fun track(bars: Int,notes: List<CompositionNote>,ref: InstrumentReference)=CompositionTrack(newCompositionId(),"Track",ref,listOf(CompositionPattern(newCompositionId(),"A",bars,notes)))
        return CompositionProject.empty("Timing",137).copy(tracks=listOf(
            track(1,listOf(note(0,3840),note(80),note(120),note(160),note(240),note(320),note(480),note(960)),InstrumentReference(1,"reference-piano","default")),
            track(2,listOf(note(0,960),note(137,319,67,47)),ComposerInstruments.all[1].reference)))
    }
    @Test fun canonicalTimingAndLegacyReferencesSurviveSnapshotAndRoundTrip() {
        val p=project(); val before=CompositionCodec.encode(p); val plan=LoopPlan.snapshot(p,emptyList())
        assertEquals(7680L,plan.regionTicks); assertEquals(0,plan.tracks[0].bank); assertEquals(1,plan.tracks[1].bank)
        for(t in listOf(80L,120,160,240,320,480,960,137)) assertEquals(Math.round(t*48000.0*60/(137*960)),plan.frame(t))
        assertEquals(before,CompositionCodec.encode(p)); assertEquals(p,CompositionCodec.decode(before))
        val changed=p.copy(tracks=p.tracks.map { it.copy(instrument=ComposerInstruments.all[7].reference) })
        assertEquals(p.tracks.map { it.patterns },CompositionCodec.decode(CompositionCodec.encode(changed)).tracks.map { it.patterns })
    }
    @Test fun differentLengthsRepeatTogetherAndBoundaryOffPrecedesOn() {
        val plan=LoopPlan.snapshot(project(),emptyList()); val c=LoopClock(plan)
        val boundary=plan.frame(3840); val events=mutableListOf<Triple<Long,Int,Boolean>>()
        while(c.frame<=boundary) {
            c.dispatch { track,e -> events += Triple(c.frame,track,e.on) }
            if(c.frame==boundary) break
            c.advance(minOf(240L,c.nextFrame()-c.frame,boundary-c.frame).toInt())
        }
        assertEquals(2,events.count { it.first==0L && it.third })
        val atEnd=events.filter { it.first==boundary }
        assertEquals(listOf(false,true),atEnd.map { it.third }); assertTrue(atEnd.all { it.second==0 })
    }
    @Test fun repeatedFrameBoundariesHaveNoAccumulatingTempoRoundingError() {
        val plan=LoopPlan.snapshot(project(),emptyList())
        val total=plan.frame(3840*100000L)
        assertEquals(Math.round(3840*100000L*48000.0*60/(137*960)),total)
        assertTrue(kotlin.math.abs(plan.tick(total)-3840*100000L)<0.03)
    }
    @Test fun selectedPatternsArePerTrackAndSnapshotIsCoherentDuringEditing() {
        val p=project(); val b=CompositionPattern(newCompositionId(),"B",4,listOf(note(10)))
        val updated=p.copy(tracks=p.tracks.mapIndexed { i,t -> if(i==0) t.copy(patterns=t.patterns+b) else t })
        val plan=LoopPlan.snapshot(updated,listOf(b.id,updated.tracks[1].patterns[0].id))
        assertEquals(15360L,plan.regionTicks); assertEquals(b.notes,plan.tracks[0].notes)
        val edited=updated.replacePattern(b.copy(notes=emptyList()))
        assertEquals(b.notes,plan.tracks[0].notes); assertTrue(edited.tracks[0].patterns[1].notes.isEmpty())
    }
    @Test fun unavailablePresetsAreExplicitAndPreserved() {
        val p=project(); val unknown=InstrumentReference(1,"future-pack","unknown")
        val edited=p.copy(tracks=p.tracks.map { it.copy(instrument=unknown) })
        assertEquals(edited,CompositionCodec.decode(CompositionCodec.encode(edited)))
        assertTrue(runCatching { LoopPlan.snapshot(edited,emptyList()) }.exceptionOrNull()!!.message!!.contains("Sound unavailable"))
        assertEquals(9,ComposerInstruments.all.map { it.id }.toSet().size)
    }
}
