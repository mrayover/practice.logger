package com.mrayover.practicelogger.composer

import kotlin.math.roundToLong

internal data class LoopEvent(val tick: Long,val note: Int,val on: Boolean)
internal data class LoopTrack(val bank: Int,val length: Long,val notes: List<CompositionNote>,val events: List<LoopEvent>,val sound: ComposerSound? = null,val patternId: String? = null)
internal data class LoopPlan(val tempo: Int,val barTicks: Long,val beatTicks: Long,val regionTicks: Long,val tracks: List<LoopTrack>,val trackIds: List<String> = emptyList(),val song: Boolean = false) {
    fun frame(tick: Long): Long = (tick * (48000.0 * 60 / (tempo * COMPOSER_PPQ))).roundToLong()
    fun tick(frame: Long): Double = frame * (tempo * COMPOSER_PPQ) / (48000.0 * 60)
    companion object {
        fun songSnapshot(project: CompositionProject, requireSound: Boolean = true): LoopPlan {
            CompositionCodec.validate(project)
            require(project.clips.any { ArrangementEdits.pattern(project,it).notes.isNotEmpty() }) { "Place a populated pattern in Arrange first." }
            val end=project.clips.maxOf { ArrangementEdits.end(project,it) }
            // Bound expansion before allocating: repeated references can multiply a large document.
            require(project.clips.sumOf { ArrangementEdits.pattern(project,it).notes.size.toLong() } <= 200000) {
                "This arrangement is too large to play. Use fewer pattern instances."
            }
            val tracks=project.tracks.map { t ->
                val bank=ComposerInstruments.index(t.instrument)
                require(!requireSound || bank>=0) { "Sound unavailable for ${t.title}. Choose an installed sound first." }
                val notes=project.clips.filter { it.trackId==t.id }.flatMap { c ->
                    ArrangementEdits.pattern(project,c).notes.map { it.copy(startTick=c.startTick+it.startTick) }
                }
                LoopTrack(bank,end,notes,notes.flatMapIndexed { n,note ->
                    listOf(LoopEvent(note.startTick,n,true),LoopEvent(note.startTick+note.durationTick,n,false))
                }.sortedWith(compareBy<LoopEvent> { it.tick }.thenBy { it.on }),t.instrument.sound)
            }
            return LoopPlan(project.tempoBpm,project.barTicks(),3840L/project.denominator,end,tracks,project.tracks.map { it.id },song=true)
        }
        fun snapshot(project: CompositionProject,patterns: List<String>, requireSound: Boolean = true): LoopPlan {
            CompositionCodec.validate(project)
            val tracks = project.tracks.mapIndexed { i,t ->
                val pattern = t.patterns.find { it.id == patterns.getOrNull(i) } ?: t.patterns.firstOrNull()
                val bank = ComposerInstruments.index(t.instrument)
                require(!requireSound || bank >= 0) { "Sound unavailable for ${t.title}. Choose an installed sound first." }
                val notes = pattern?.notes?.toList() ?: emptyList()
                LoopTrack(bank,pattern?.let(project::patternTicks) ?: project.barTicks(),notes,
                    notes.flatMapIndexed { n,note -> listOf(LoopEvent(note.startTick,n,true),LoopEvent(note.startTick+note.durationTick,n,false)) }
                        .sortedWith(compareBy<LoopEvent> { it.tick }.thenBy { it.on }),t.instrument.sound,pattern?.id)
            }
            // Supported lengths are powers of two, so the longest is also their LCM.
            return LoopPlan(project.tempoBpm,project.barTicks(),3840L/project.denominator,tracks.maxOfOrNull { it.length } ?: project.barTicks(),tracks,project.tracks.map { it.id })
        }
    }
}

/** Per-pattern streams share absolute musical time; no per-loop rounding drift or UI timers. */
internal class LoopClock(val plan: LoopPlan, var repeatEvents: Boolean = true) {
    private val active = plan.tracks.toTypedArray()
    private val pending = arrayOfNulls<LoopNoteUpdate>(active.size)
    private val boundaries = LongArray(active.size)
    private val indices = IntArray(plan.tracks.size)
    private val cycles = LongArray(plan.tracks.size)
    val voices = plan.tracks.map { LongArray(it.notes.size) { -1L } }.toTypedArray()
    fun track(index: Int) = active[index]
    fun clearUpdates() { pending.fill(null) }
    fun offer(update: LoopNoteUpdate) {
        val i=plan.trackIds.indexOf(update.trackId)
        if(plan.song || i<0 || active[i].patternId!=update.track.patternId || active[i].length!=update.track.length) return
        if(active[i].notes==update.track.notes) { pending[i]=null; return }
        // Keep an already pending boundary, coalescing rapid edits without delaying activation.
        if(pending[i]==null) {
            var cycle=plan.tick(frame).toLong()/active[i].length+1
            while(plan.frame(cycle*active[i].length)<=frame) cycle++
            boundaries[i]=cycle*active[i].length
        }
        pending[i]=update
    }
    var frame = 0L; private set
    private fun nextTick(track: Int): Long {
        val t=active[track]
        return if(t.events.isEmpty() || (!repeatEvents && cycles[track]>0)) Long.MAX_VALUE else cycles[track]*t.length+t.events[indices[track]].tick
    }
    fun nextFrame(): Long {
        var next=Long.MAX_VALUE
        for(i in active.indices) {
            val tick=nextTick(i); if(tick != Long.MAX_VALUE) next=minOf(next,plan.frame(tick))
            if(pending[i]!=null) next=minOf(next,plan.frame(boundaries[i]))
        }
        return next
    }
    fun advance(frames: Int) { frame += frames }
    fun dispatch(emit: (Int,LoopEvent) -> Unit) = dispatch(emit,{})
    fun dispatch(emit: (Int,LoopEvent) -> Unit, release: (Int) -> Unit) {
        while(true) {
            var chosen = -1; var earliest=Long.MAX_VALUE; var priority=3
            var replacement=false
            for(i in active.indices) {
                val tick=nextTick(i)
                if(tick!=Long.MAX_VALUE) {
                    val f=plan.frame(tick); val p=if(active[i].events[indices[i]].on) 2 else 0
                    if(f<earliest || (f==earliest && p<priority)) { chosen=i; earliest=f; priority=p; replacement=false }
                }
                if(pending[i]!=null) {
                    val f=plan.frame(boundaries[i])
                    if(f<earliest || (f==earliest && 1<priority)) { chosen=i; earliest=f; priority=1; replacement=true }
                }
            }
            if(chosen<0 || earliest>frame) return
            if(replacement) {
                release(chosen)
                val update=pending[chosen]!!
                active[chosen]=update.track; voices[chosen]=update.voices
                indices[chosen]=0; cycles[chosen]=boundaries[chosen]/update.track.length; pending[chosen]=null
                continue
            }
            val t=active[chosen]; emit(chosen,t.events[indices[chosen]])
            if(++indices[chosen] == t.events.size) { indices[chosen]=0; cycles[chosen]++ }
        }
    }
}

/** Allocated and validated away from rendering; only the worker owns pending/active state. */
internal class LoopNoteUpdate(val trackId: String,val track: LoopTrack) {
    val voices=LongArray(track.notes.size) { -1L }
}

internal fun LoopPlan.noteUpdates(project: CompositionProject, selections: List<String>): List<LoopNoteUpdate>? {
    if(song || tempo!=project.tempoBpm || barTicks!=project.barTicks() || beatTicks!=3840L/project.denominator ||
        trackIds!=project.tracks.map { it.id }) return null
    val latest=LoopPlan.snapshot(project,selections)
    if(tracks.indices.any { i -> val a=tracks[i]; val b=latest.tracks[i]
        a.patternId!=b.patternId || a.length!=b.length || a.bank!=b.bank || a.sound!=b.sound }) return null
    return latest.tracks.mapIndexed { i,t -> LoopNoteUpdate(trackIds[i],t) }
}
