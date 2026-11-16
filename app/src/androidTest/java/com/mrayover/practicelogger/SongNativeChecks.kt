package com.mrayover.practicelogger

import androidx.test.platform.app.InstrumentationRegistry
import com.mrayover.practicelogger.composer.*
import org.junit.Assert.*
import java.io.File
import kotlin.math.abs

/** Exercise the song clock through the packaged native synthesizer, without another AudioTrack. */
internal fun songNativeChecks(result: File) {
    val context=InstrumentationRegistry.getInstrumentation().targetContext
    val tracks=listOf(5,4).mapIndexed { i,bank -> CompositionTrack(newCompositionId(),"Part $i",ComposerInstruments.all[bank].reference,
        listOf(CompositionPattern(newCompositionId(),"A",1,listOf(CompositionNote(newCompositionId(),60-12*i,0,480,80+i*20))))) }
    var p=CompositionProject.empty("Native song").copy(tracks=tracks)
    listOf(0 to 0L,0 to 1L,1 to 1L,1 to 3L).forEach { (t,b) -> p=ArrangementEdits.add(p,tracks[t].id,tracks[t].patterns[0].id,b*p.barTicks()) }
    val plan=LoopPlan.songSnapshot(p);val handle=ComposerSynth.create()
    try {
        listOf(5,4).forEach { bank -> assertTrue(ComposerSynth.load(handle,bank,context.assets.open("composer/sounds/${ComposerInstruments.all[bank].asset}.sf2").use { it.readBytes() })) }
        // Test scheduling silence with an explicit20ms release; factory SF2 release tails are musical sound data.
        for(channel in 0..1) ComposerSynth.shape(handle,channel,true,1f,20f,18000f,.707f,.8f,0f)
        val clock=LoopClock(plan,false);val pcm=ShortArray(480)
        val energy=LongArray(4);var gap=0L;var ons=0;var offs=0;val velocities=mutableListOf<Int>()
        val emit: (Int,LoopEvent)->Unit={ t,e ->
            val part=plan.tracks[t];val note=part.notes[e.note]
            if(e.on) { ons++;velocities+=note.velocity;clock.voices[t][e.note]=ComposerSynth.on(handle,part.bank,t,note.pitch,note.velocity) }
            else { offs++;ComposerSynth.off(handle,part.bank,clock.voices[t][e.note]);clock.voices[t][e.note]=-1 }
        }
        val endpoint=plan.frame(plan.regionTicks)
        while(clock.frame<endpoint) {
            clock.dispatch(emit)
            val count=minOf(240L,clock.nextFrame()-clock.frame,endpoint-clock.frame).toInt()
            assertTrue(count>0);ComposerSynth.render(handle,pcm,0,count)
            val bar=(clock.frame/96000).toInt()
            var sum=0L;for(i in 0 until count*2) sum+=abs(pcm[i].toInt())
            energy[bar]+=sum
            if(clock.frame in 240000L..335760L) gap+=sum // Late Bar3, after release tails.
            clock.advance(count)
        }
        clock.dispatch(emit)
        assertEquals(4,ons);assertEquals(4,offs);assertEquals(listOf(80,80,100,100),velocities)
        assertTrue(energy[0]>0 && energy[1]>0 && energy[3]>0);assertEquals("Silent gap",0L,gap)
        assertEquals(Long.MAX_VALUE,clock.nextFrame())
        // Stop is immediate, including same-pitch simultaneous independent voices.
        val v1=ComposerSynth.on(handle,5,0,60,80);val v2=ComposerSynth.on(handle,5,1,60,100)
        assertTrue(v1!=v2);ComposerSynth.off(handle,5,v1);ComposerSynth.render(handle,pcm,0,240);assertTrue(pcm.any { it.toInt()!=0 })
        ComposerSynth.clear(handle,0,15);ComposerSynth.render(handle,pcm,0,240);assertTrue(pcm.all { it.toInt()==0 });assertEquals(0,ComposerSynth.voices(handle))
        result.writeText("PASS packaged native song snapshot/clock, exact 4 on/off events and velocities, concurrent instruments, repeated references, audible Bars1/2/4, silent late Bar3, finite endpoint, independent same-pitch release and immediate Stop silence. Energy=${energy.toList()} gap=$gap\n")
    } finally { ComposerSynth.destroy(handle) }
}
