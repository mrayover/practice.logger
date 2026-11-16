package com.mrayover.practicelogger.composer

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class) @Config(sdk=[35])
class SongPlanTest {
    private fun base(): CompositionProject {
        val tracks=listOf(5,4).mapIndexed { i,bank -> CompositionTrack(newCompositionId(),"Piano ${i+1}",ComposerInstruments.all[bank].reference,
            listOf(CompositionPattern(newCompositionId(),"A",1,listOf(CompositionNote(newCompositionId(),60-i*12,0,3840,73+i))))) }
        var p=CompositionProject.empty("Song",137).copy(tracks=tracks)
        listOf(0 to 0L,0 to 1L,1 to 1L,1 to 3L).forEach { (t,b) -> p=ArrangementEdits.add(p,tracks[t].id,tracks[t].patterns[0].id,b*p.barTicks()) }
        return p
    }
    @Test fun songPlacementsSilenceVelocitiesSnapshotAndFiniteBoundary() {
        val p=base(); val plan=LoopPlan.songSnapshot(p)
        assertTrue(plan.song); assertEquals(15360L,plan.regionTicks)
        assertEquals(listOf(0L,3840L),plan.tracks[0].notes.map { it.startTick })
        assertEquals(listOf(3840L,11520L),plan.tracks[1].notes.map { it.startTick })
        assertEquals(listOf(74,74),plan.tracks[1].notes.map { it.velocity })
        assertTrue(plan.tracks.all { it.length==plan.regionTicks })
        val clock=LoopClock(plan,false); val events=mutableListOf<Triple<Long,Int,Boolean>>()
        while(clock.nextFrame()!=Long.MAX_VALUE) {
            clock.advance((clock.nextFrame()-clock.frame).toInt()); clock.dispatch { t,e -> events+=Triple(e.tick,t,e.on) }
        }
        assertEquals(8,events.size)
        assertEquals(listOf(false,true,true),events.filter { it.first==3840L }.map { it.third })
        assertFalse(events.any { it.second==1 && it.third && (it.first==0L || it.first==7680L) })
        assertEquals(15360L,events.last().first); assertFalse(events.last().third)
        assertEquals(p.clips,CompositionCodec.decode(CompositionCodec.encode(p)).clips)
        val pat=p.tracks[0].patterns[0]; val edited=p.replacePattern(pat.copy(notes=emptyList()))
        assertTrue(plan.tracks[0].notes.isNotEmpty()); assertTrue(LoopPlan.songSnapshot(edited).tracks[0].notes.isEmpty())
    }
    @Test fun repeatUsesWholeSongRegionAndLoopRemainsIndependent() {
        val p=base();val plan=LoopPlan.songSnapshot(p);val clock=LoopClock(plan)
        val starts=mutableListOf<Long>()
        while(clock.frame<=plan.frame(plan.regionTicks)) {
            clock.dispatch { t,e -> if(t==0 && e.on) starts+=clock.frame }
            val next=clock.nextFrame();if(next>plan.frame(plan.regionTicks)) break
            clock.advance((next-clock.frame).toInt())
        }
        assertEquals(listOf(0L,plan.frame(3840),plan.frame(15360)),starts)
        val loop=LoopPlan.snapshot(p,emptyList());assertFalse(loop.song);assertEquals(3840L,loop.regionTicks)
        val meter=p.copy(numerator=7,denominator=8,clips=emptyList(),tracks=p.tracks.map { t -> t.copy(patterns=t.patterns.map { it.copy(notes=it.notes.map { n -> n.copy(durationTick=3360) }) }) })
        val placed=ArrangementEdits.add(meter,meter.tracks[0].id,meter.tracks[0].patterns[0].id,3360)
        val metered=LoopPlan.songSnapshot(placed); assertEquals(3360L,metered.barTicks);assertEquals(480L,metered.beatTicks)
        assertEquals(Math.round(6720*48000.0*60/(137*960)),metered.frame(metered.regionTicks))
    }
    @Test fun duplicateRepeatAreAtomicFreshReferencesAndPersistent() {
        val p=base();val source=p.clips.last();val copied=ArrangementEdits.repeat(p,source.id,1)
        assertEquals(source.startTick+p.barTicks(),copied.clips.last().startTick)
        assertNotEquals(source.id,copied.clips.last().id);assertEquals(source.patternId,copied.clips.last().patternId)
        val repeated=ArrangementEdits.repeat(p,source.id,8)
        assertEquals(p.clips.size+8,repeated.clips.size);assertEquals(repeated.clips.size,repeated.clips.map { it.id }.toSet().size)
        assertEquals(p.tracks,repeated.tracks)
        val blocked=ArrangementEdits.add(p,source.trackId,source.patternId,source.startTick+3*p.barTicks())
        assertTrue(runCatching { ArrangementEdits.repeat(blocked,source.id,4) }.isFailure);assertEquals(p.clips.size+1,blocked.clips.size)
        assertTrue(runCatching { ArrangementEdits.repeat(p,source.id,9) }.isFailure)
        assertEquals(repeated,CompositionCodec.decode(CompositionCodec.encode(repeated)))
        val independent=repeated.independentCopy("Copy");assertEquals(repeated.clips.size,independent.clips.size)
        independent.clips.forEach { c -> assertTrue(independent.tracks.any { it.id==c.trackId && it.patterns.any { pat -> pat.id==c.patternId } }) }
        assertNull(ComposerInstruments.descriptiveName(p.tracks[0]));assertNull(ComposerInstruments.descriptiveName(p.tracks[0].copy(title="Track 3")))
        assertEquals("Walking Bass",ComposerInstruments.descriptiveName(p.tracks[1].copy(title="Walking Bass")))
        assertTrue(runCatching { LoopPlan.songSnapshot(p.copy(clips=emptyList())) }.isFailure)
    }
}
