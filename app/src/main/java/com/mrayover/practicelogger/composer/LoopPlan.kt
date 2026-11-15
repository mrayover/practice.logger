package com.mrayover.practicelogger.composer

import kotlin.math.roundToLong

internal data class LoopEvent(val tick: Long,val note: Int,val on: Boolean)
internal data class LoopTrack(val bank: Int,val length: Long,val notes: List<CompositionNote>,val events: List<LoopEvent>)
internal data class LoopPlan(val tempo: Int,val barTicks: Long,val beatTicks: Long,val regionTicks: Long,val tracks: List<LoopTrack>) {
    fun frame(tick: Long): Long = (tick * (48000.0 * 60 / (tempo * COMPOSER_PPQ))).roundToLong()
    fun tick(frame: Long): Double = frame * (tempo * COMPOSER_PPQ) / (48000.0 * 60)
    companion object {
        fun snapshot(project: CompositionProject,patterns: List<String>): LoopPlan {
            CompositionCodec.validate(project)
            val tracks = project.tracks.mapIndexed { i,t ->
                val pattern = t.patterns.find { it.id == patterns.getOrNull(i) } ?: t.patterns.firstOrNull()
                val bank = ComposerInstruments.index(t.instrument)
                require(bank >= 0) { "Sound unavailable for ${t.title}. Choose an installed sound first." }
                val notes = pattern?.notes?.toList() ?: emptyList()
                LoopTrack(bank,pattern?.let(project::patternTicks) ?: project.barTicks(),notes,
                    notes.flatMapIndexed { n,note -> listOf(LoopEvent(note.startTick,n,true),LoopEvent(note.startTick+note.durationTick,n,false)) }
                        .sortedWith(compareBy<LoopEvent> { it.tick }.thenBy { it.on }))
            }
            // Supported lengths are powers of two, so the longest is also their LCM.
            return LoopPlan(project.tempoBpm,project.barTicks(),3840L/project.denominator,tracks.maxOfOrNull { it.length } ?: project.barTicks(),tracks)
        }
    }
}

/** Per-pattern streams share absolute musical time; no per-loop rounding drift or UI timers. */
internal class LoopClock(val plan: LoopPlan) {
    private val indices = IntArray(plan.tracks.size)
    private val cycles = LongArray(plan.tracks.size)
    val voices = plan.tracks.map { LongArray(it.notes.size) { -1L } }
    var frame = 0L; private set
    private fun nextTick(track: Int): Long {
        val t=plan.tracks[track]
        return if(t.events.isEmpty()) Long.MAX_VALUE else cycles[track]*t.length+t.events[indices[track]].tick
    }
    fun nextFrame(): Long {
        var next=Long.MAX_VALUE
        for(i in plan.tracks.indices) { val tick=nextTick(i); if(tick != Long.MAX_VALUE) next=minOf(next,plan.frame(tick)) }
        return next
    }
    fun advance(frames: Int) { frame += frames }
    fun dispatch(emit: (Int,LoopEvent) -> Unit) {
        while(true) {
            var chosen = -1; var earliest=Long.MAX_VALUE; var on=true
            for(i in plan.tracks.indices) {
                val tick=nextTick(i); if(tick==Long.MAX_VALUE) continue
                val f=plan.frame(tick); val e=plan.tracks[i].events[indices[i]]
                if(f<earliest || (f==earliest && !e.on && on)) { chosen=i; earliest=f; on=e.on }
            }
            if(chosen<0 || earliest>frame) return
            val t=plan.tracks[chosen]; emit(chosen,t.events[indices[chosen]])
            if(++indices[chosen] == t.events.size) { indices[chosen]=0; cycles[chosen]++ }
        }
    }
}
