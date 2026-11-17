package com.mrayover.practicelogger.composer

/** Single immutable project mutations: the ViewModel validates/checkpoints and records one Undo each. */
internal object ComposerManagement {
    private fun track(p: CompositionProject,id: String) = requireNotNull(p.tracks.find { it.id==id }) { "Instrument track is no longer available." }
    private fun pattern(t: CompositionTrack,id: String) = requireNotNull(t.patterns.find { it.id==id }) { "Pattern is no longer available." }
    private fun update(p: CompositionProject,t: CompositionTrack) = p.copy(tracks=p.tracks.map { if(it.id==t.id) t else it }).also(CompositionCodec::validate)
    fun nextPatternTitle(t: CompositionTrack): String {
        fun letters(n: Int): String { var v=n; var out=""; do { out=('A'+v%26)+out; v=v/26-1 } while(v>=0); return out }
        return generateSequence(0) { it+1 }.map(::letters).first { name -> t.patterns.none { it.title==name } }
    }
    fun addPattern(p: CompositionProject,trackId: String,sourceId: String?=null,copySuffix: String="Copy"): CompositionProject {
        val t=track(p,trackId); require(t.patterns.size<COMPOSER_MAX_PATTERNS) { "Each instrument supports up to 256 patterns." }
        val source=sourceId?.let { pattern(t,it) }
        var name=nextPatternTitle(t)
        if(source!=null) {
            val stem=source.title.take(106)+" $copySuffix"; name=stem; var serial=2
            while(t.patterns.any { it.title==name }) name="$stem "+serial++
        }
        val added=source?.copy(id=newCompositionId(),title=name,notes=source.notes.map { it.copy(id=newCompositionId()) })
            ?: CompositionPattern(newCompositionId(),name,4)
        return update(p,t.copy(patterns=t.patterns+added))
    }
    fun renamePattern(p: CompositionProject,trackId: String,patternId: String,title: String): CompositionProject {
        val t=track(p,trackId); pattern(t,patternId)
        return update(p,t.copy(patterns=t.patterns.map { if(it.id==patternId) it.copy(title=title.trim()) else it }))
    }
    fun deletePattern(p: CompositionProject,trackId: String,patternId: String): CompositionProject {
        val t=track(p,trackId); pattern(t,patternId)
        require(p.clips.none { it.trackId==trackId && it.patternId==patternId }) { "Cannot delete: this pattern is referenced by arrangement clips." }
        return update(p,t.copy(patterns=t.patterns.filterNot { it.id==patternId },mixer=t.mixer.copy(selectedPatternId=t.mixer.selectedPatternId?.takeUnless { it==patternId })))
    }
    fun renameTrack(p: CompositionProject,trackId: String,title: String): CompositionProject = update(p,track(p,trackId).copy(title=title.trim()))
    fun addTrack(p: CompositionProject): CompositionProject {
        require(p.tracks.size<COMPOSER_MAX_TRACKS) { "A composition supports up to eight instruments." }
        var n=1; while(p.tracks.any { it.title=="Track $n" }) n++
        return p.copy(tracks=p.tracks+CompositionTrack(newCompositionId(),"Track $n",ComposerInstruments.all.first().reference,
            listOf(CompositionPattern(newCompositionId(),"A",4)))).also(CompositionCodec::validate)
    }
    fun deleteTrack(p: CompositionProject,trackId: String): CompositionProject {
        track(p,trackId)
        require(p.clips.none { it.trackId==trackId }) { "Cannot delete: this instrument track is referenced by arrangement clips." }
        return p.copy(tracks=p.tracks.filterNot { it.id==trackId }).also(CompositionCodec::validate)
    }
}
