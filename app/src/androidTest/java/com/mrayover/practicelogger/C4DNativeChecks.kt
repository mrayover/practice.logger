package com.mrayover.practicelogger

import com.mrayover.practicelogger.composer.*
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import java.io.File
import kotlin.math.abs

/** Objective PCM checks on the actual packaged native renderer, without AudioTrack. */
internal fun c4dNativeChecks(report: File) {
    val assets=InstrumentationRegistry.getInstrumentation().targetContext.assets
    val bytes=assets.open("composer/sounds/${ComposerInstruments.all[0].asset}.sf2").use { it.readBytes() }
    fun handle()=ComposerSynth.create().also { assertTrue(ComposerSynth.load(it,0,bytes)) }
    val sum=handle(); val singles=LongArray(8) { handle() }
    val mixed=ShortArray(480); val isolated=Array(8) { ShortArray(480) }
    try {
        // Identical pitches on one bank, separate channels and independently released tokens.
        val tokens=LongArray(8); val separateTokens=LongArray(8)
        for(c in 0..7) {
            tokens[c]=ComposerSynth.on(sum,0,c,60,55+c)
            separateTokens[c]=ComposerSynth.on(singles[c],0,c,60,55+c)
        }
        repeat(80) { block ->
            if(block==30) { ComposerSynth.off(sum,0,tokens[3]); ComposerSynth.off(singles[3],0,separateTokens[3]) }
            ComposerSynth.render(sum,mixed,0,240)
            for(c in 0..7) ComposerSynth.render(singles[c],isolated[c],0,240)
            for(i in mixed.indices) assertTrue("Independent summation/once-only voice advance block $block sample $i",abs(mixed[i].toInt()-isolated.sumOf { it[i].toInt() })<=8)
        }
        // Pan is post-instrument. Right-panned custom preset still reaches full mixer Left.
        for(c in 0..7) { ComposerSynth.clear(sum,0,15); ComposerSynth.clear(singles[c],0,15) }
        ComposerSynth.shape(sum,0,true,10f,300f,18000f,.707f,.8f,1f)
        fun energy(pan: Float): Pair<Long,Long> {
            ComposerSynth.clear(sum,0,15); ComposerSynth.pan(sum,0,pan); ComposerSynth.on(sum,0,0,60,100)
            var l=0L;var r=0L
            repeat(20) { ComposerSynth.render(sum,mixed,0,240); for(i in mixed.indices step 2) { l+=abs(mixed[i].toInt());r+=abs(mixed[i+1].toInt()) } }
            return l to r
        }
        val center=energy(0f); val left=energy(-1f);val right=energy(1f)
        assertEquals(0L,center.first);assertTrue(center.second>0)
        assertTrue(left.first>0);assertEquals(0L,left.second)
        assertEquals(0L,right.first);assertTrue(right.second>0)
        // Pan affects only its channel. Opposite channels remain exactly separated.
        ComposerSynth.clear(sum,0,15); ComposerSynth.shape(sum,0,false,10f,300f,18000f,.707f,.8f,0f)
        ComposerSynth.pan(sum,0,-1f);ComposerSynth.pan(sum,1,1f)
        ComposerSynth.on(sum,0,0,60,70);ComposerSynth.on(sum,0,1,67,70)
        repeat(10) { ComposerSynth.render(sum,mixed,0,240) }
        ComposerSynth.clear(sum,0,0)
        repeat(10) { ComposerSynth.render(sum,mixed,0,240); assertTrue(mixed.indices.step(2).all { mixed[it].toInt()==0 });assertTrue(mixed.indices.step(2).any { mixed[it+1].toInt()!=0 }) }
        ComposerSynth.clear(sum,0,15); ComposerSynth.render(sum,mixed,0,240);assertTrue(mixed.all { it.toInt()==0 })
    } finally { ComposerSynth.destroy(sum); singles.forEach(ComposerSynth::destroy) }
    // Realistic eight-track / 32-note polyphony across all factory banks, plus custom shaping.
    val h=ComposerSynth.create()
    try {
        ComposerInstruments.all.forEachIndexed { i,v -> assertTrue(ComposerSynth.load(h,i,assets.open("composer/sounds/${v.asset}.sf2").use { it.readBytes() })) }
        val timings=LongArray(1200); val out=ShortArray(480)
        for(c in 0..7) ComposerSynth.pan(h,c,(c-3.5f)/3.5f)
        repeat(timings.size) { b ->
            if(b%100==0) {
                ComposerSynth.clear(h,0,15)
                for(c in 0..7) for(n in 0..3) ComposerSynth.on(h,c,c,48+n*4+c%3,80)
            }
            val start=System.nanoTime(); ComposerSynth.render(h,out,0,240); timings[b]=System.nanoTime()-start
        }
        val avg=timings.average()/1e6;timings.sort(); val p99=timings[(timings.size*.99).toInt()]/1e6
        report.writeText("PASS independent additive PCM / once-per-frame advancement, overlapping same pitch / independent release, stereo center/left/right and preset Pan combination, channel separation, Stop silence. Eight-track 32-note render: mean ${avg}ms, p99 ${p99}ms per 5ms block (emulator).\n")
        assertTrue("Audio render mean exceeds 5ms block budget: $avg",avg<5.0)
    } finally { ComposerSynth.destroy(h) }
}
