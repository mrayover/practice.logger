package com.mrayover.practicelogger.composer

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class) @Config(sdk=[35])
class LoopLiveUpdatesTest {
    private fun note(start: Long=0,duration: Long=480,pitch: Int=60,velocity: Int=80)=CompositionNote(newCompositionId(),pitch,start,duration,velocity)
    private fun project()=CompositionProject.empty("Live",120).copy(tracks=listOf(1,2).map { bars ->
        CompositionTrack(newCompositionId(),"Track",ComposerInstruments.all[0].reference,
            listOf(CompositionPattern(newCompositionId(),"A",bars,listOf(note(0,3840L*bars))))) })
    private fun runTo(c: LoopClock,tick: Long,events: MutableList<String>) {
        val end=c.plan.frame(tick)
        fun dispatch() { c.dispatch({ i,e -> val n=c.track(i).notes[e.note]; events += "${c.frame}:$i:${e.on}:${n.pitch}:${n.velocity}" },{ i ->
            assertTrue("old voices released before replacement",c.voices[i].all { it<0 })
        }) }
        while(c.frame<end) { dispatch(); c.advance(minOf(240L,c.nextFrame()-c.frame,end-c.frame).coerceAtLeast(1).toInt()) }
        dispatch()
    }
    @Test fun independentBoundariesReplaceAllNoteFieldsAndPreserveClock() {
        val p=project(); val plan=LoopPlan.snapshot(p,emptyList()); val c=LoopClock(plan); val events=mutableListOf<String>()
        runTo(c,960,events)
        val edited=p.copy(tracks=p.tracks.map { t -> t.copy(patterns=t.patterns.map { it.copy(notes=listOf(note(0,240,67,39),note(960,120,72,111))) }) })
        plan.noteUpdates(edited,emptyList())!!.forEach(c::offer)
        runTo(c,3840,events)
        assertEquals(plan.frame(3840),c.frame); assertSame(plan,c.plan)
        assertEquals(edited.tracks[0].patterns[0].notes,c.track(0).notes)
        assertEquals(p.tracks[1].patterns[0].notes,c.track(1).notes)
        assertEquals(listOf("${c.frame}:0:false:60:80","${c.frame}:0:true:67:39"),events.filter { it.startsWith("${c.frame}:") })
        runTo(c,7680,events)
        assertEquals(edited.tracks[1].patterns[0].notes,c.track(1).notes)
        val boundary=events.filter { it.startsWith("${c.frame}:") }
        assertTrue(boundary.first().contains(":false:")); assertEquals(2,boundary.count { it.contains(":true:") })
    }
    @Test fun rapidEditsCoalesceAndUndoCanRestoreOriginalOrActiveNotes() {
        val p=project(); val plan=LoopPlan.snapshot(p,emptyList()); val c=LoopClock(plan); val events=mutableListOf<String>()
        runTo(c,480,events)
        fun offer(notes: List<CompositionNote>) { plan.noteUpdates(p.replacePattern(p.tracks[0].patterns[0].copy(notes=notes)),emptyList())!!.forEach(c::offer) }
        offer(listOf(note(pitch=64))); offer(listOf(note(pitch=65))); offer(p.tracks[0].patterns[0].notes)
        runTo(c,3840,events); assertEquals(60,c.track(0).notes[0].pitch)
        offer(listOf(note(pitch=66))); offer(listOf(note(pitch=67)))
        runTo(c,7680,events); assertEquals(67,c.track(0).notes[0].pitch)
        offer(p.tracks[0].patterns[0].notes)
        runTo(c,11520,events); assertEquals(p.tracks[0].patterns[0].notes,c.track(0).notes)
        assertFalse(events.any { it.contains(":true:66:") || it.contains(":true:65:") })
    }
    @Test fun emptyPatternUpdatesAndSilentCyclesWakeAtTheirOwnBoundary() {
        val p=project(); val plan=LoopPlan.snapshot(p,emptyList()); val c=LoopClock(plan); val events=mutableListOf<String>()
        runTo(c,120,events)
        val empty=p.replacePattern(p.tracks[0].patterns[0].copy(notes=emptyList()))
        plan.noteUpdates(empty,emptyList())!!.forEach(c::offer); runTo(c,3840,events)
        assertTrue(c.track(0).notes.isEmpty()); assertTrue(c.voices[0].isEmpty())
        runTo(c,4000,events)
        plan.noteUpdates(p,emptyList())!!.forEach(c::offer)
        assertEquals(plan.frame(7680),c.nextFrame()); runTo(c,7680,events)
        assertEquals(p.tracks[0].patterns[0].notes,c.track(0).notes)
        assertEquals(1,events.count { it=="${c.frame}:0:true:60:80" })
    }
    @Test fun staleSelectionsStructuralChangesAndSongSnapshotsAreRejected() {
        val p=project(); val plan=LoopPlan.snapshot(p,emptyList()); val b=p.tracks[0].patterns[0].copy(id=newCompositionId(),notes=emptyList())
        val switched=p.copy(tracks=p.tracks.mapIndexed { i,t -> if(i==0) t.copy(patterns=t.patterns+b) else t })
        assertNull(plan.noteUpdates(switched,listOf(b.id)))
        assertNull(plan.noteUpdates(p.copy(tempoBpm=121),emptyList()))
        assertNull(plan.noteUpdates(p.copy(numerator=7,denominator=8),emptyList()))
        assertNull(plan.noteUpdates(p.copy(tracks=p.tracks.reversed()),emptyList()))
        assertNull(plan.noteUpdates(p.replacePattern(p.tracks[0].patterns[0].copy(lengthBars=4)),emptyList()))
        val song=LoopPlan.songSnapshot(ArrangementEdits.add(p,p.tracks[0].id,p.tracks[0].patterns[0].id,0))
        assertNull(song.noteUpdates(p,emptyList()))
        val c=LoopClock(plan); val wrong=LoopTrack(0,3840,listOf(note()),emptyList(),patternId=b.id)
        c.offer(LoopNoteUpdate(p.tracks[0].id,wrong)); assertEquals(plan.frame(0),c.nextFrame())
    }
    @Test fun cancellingPendingUpdatesRetainsFiniteSnapshotAndValidationRejectsBadNotes() {
        val p=project(); val plan=LoopPlan.snapshot(p,emptyList()); val c=LoopClock(plan); val events=mutableListOf<String>()
        runTo(c,120,events)
        val changed=p.replacePattern(p.tracks[0].patterns[0].copy(notes=emptyList()))
        plan.noteUpdates(changed,emptyList())!!.forEach(c::offer); c.clearUpdates()
        runTo(c,3840,events); assertEquals(p.tracks[0].patterns[0].notes,c.track(0).notes)
        val invalid=p.replacePattern(p.tracks[0].patterns[0].copy(notes=listOf(note(velocity=0))))
        assertTrue(runCatching { plan.noteUpdates(invalid,emptyList()) }.isFailure)
        assertEquals(p.tracks[0].patterns[0].notes,plan.tracks[0].notes)
    }
    @Test fun measuredOneBarFitUsesMeterAndRelativeZoomForAllPatternLengths() {
        for(width in listOf(900f,1800f)) for(meter in listOf(4 to 4,3 to 4,7 to 8,42 to 1,1 to 32)) {
            val p=CompositionProject.empty("Fit",numerator=meter.first,denominator=meter.second)
            val scale=rollPixelsPerTick(width,186f,p.barTicks(),1f)
            assertEquals(width-2,186+p.barTicks()*scale,.01f)
            for(bars in listOf(1,4,16)) assertEquals(width-2,186+p.barTicks()*scale,.01f)
            assertEquals(scale*1.5f,rollPixelsPerTick(width,186f,p.barTicks(),1.5f),.0001f)
            assertEquals(0f,rollMaxPan(p.barTicks(),width,186f,scale),.0001f)
            assertTrue(rollMaxPan(p.barTicks()*16,width,186f,scale)>p.barTicks()*14)
            val n=note(p.barTicks()/4,p.barTicks()/8)
            val left=186+n.startTick*scale; val right=left+n.durationTick*scale
            assertEquals(n.startTick.toDouble(),((left-186)/scale).toDouble(),.02)
            assertEquals(n.durationTick.toDouble(),((right-left)/scale).toDouble(),.02)
        }
    }
}
