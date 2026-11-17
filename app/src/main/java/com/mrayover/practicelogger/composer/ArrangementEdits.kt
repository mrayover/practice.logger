package com.mrayover.practicelogger.composer

/** Editing policy only: legacy documents remain readable, including overlapping clips. */
internal object ArrangementEdits {
    const val MAX_START_TICK = 960L * 4 * 1_000_000
    fun pattern(p: CompositionProject, c: ArrangementClip) = p.tracks.single { it.id == c.trackId }.patterns.single { it.id == c.patternId }
    fun end(p: CompositionProject, c: ArrangementClip) = c.startTick + p.patternTicks(pattern(p,c))
    fun destination(p: CompositionProject, c: ArrangementClip, tick: Long) {
        require(tick in 0..MAX_START_TICK && tick % p.barTicks() == 0L) { "Choose a whole bar within the document limit." }
        val end = tick + p.patternTicks(pattern(p,c))
        require(p.clips.none { it.id != c.id && it.trackId == c.trackId && tick < end(p,it) && it.startTick < end }) {
            "That position overlaps another clip on this instrument. Choose a free bar."
        }
    }
    fun add(p: CompositionProject, trackId: String, patternId: String, tick: Long): CompositionProject {
        val c=ArrangementClip(newCompositionId(),trackId,patternId,tick)
        require(pattern(p,c).notes.isNotEmpty()) { "Add notes to this pattern in Loop first." }
        destination(p,c,tick)
        return p.copy(clips=p.clips+c).also(CompositionCodec::validate)
    }
    fun move(p: CompositionProject, id: String, tick: Long): CompositionProject {
        val c=p.clips.single { it.id==id }; destination(p,c,tick)
        return p.copy(clips=p.clips.map { if(it.id==id) it.copy(startTick=tick) else it }).also(CompositionCodec::validate)
    }
    fun delete(p: CompositionProject, id: String) = p.copy(clips=p.clips.filterNot { it.id==id }).also(CompositionCodec::validate)
    /** Clone and rebind in one validated snapshot, without changing clip or mixer identity. */
    fun makeUnique(p: CompositionProject, id: String): CompositionProject {
        val clip=p.clips.single { it.id==id }
        val copied=ComposerManagement.addPattern(p,clip.trackId,clip.patternId,copySuffix="Variation")
        val variation=copied.tracks.single { it.id==clip.trackId }.patterns.last()
        return copied.copy(clips=copied.clips.map { if(it.id==id) it.copy(patternId=variation.id) else it })
            .also(CompositionCodec::validate)
    }
    /** Validate the complete batch in a private snapshot; callers commit exactly once. */
    fun repeat(p: CompositionProject, id: String, additional: Int): CompositionProject {
        require(additional in 1..8) { "Choose 2–8 additional instances, or Duplicate for one copy." }
        val source=p.clips.single { it.id==id }
        val length=p.patternTicks(pattern(p,source))
        var result=p
        for(i in 1..additional) result=add(result,source.trackId,source.patternId,source.startTick+i*length)
        return result
    }
    fun checkLength(before: CompositionProject, after: CompositionProject, patternId: String) {
        after.clips.filter { it.patternId==patternId }.forEach { c ->
            after.clips.filter { it.id!=c.id && it.trackId==c.trackId }.forEach { other ->
                val overlaps=c.startTick < end(after,other) && other.startTick < end(after,c)
                val wasOverlapping=c.startTick < end(before,other) && other.startTick < end(before,c)
                require(!overlaps || wasOverlapping) { "Length kept: arrangement clips would overlap. Move or delete the neighboring clip first." }
            }
        }
    }
}
