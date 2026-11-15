package com.mrayover.practicelogger.composer

/** Stable document identities; SF2 bank/program resolution stays inside the registry. */
internal data class ComposerInstrument(val id: String,val title: String,val asset: String,val drums: Boolean = false) {
    val reference get() = InstrumentReference(1,id,"default")
}
internal object ComposerInstruments {
    val all = listOf(
        ComposerInstrument("acoustic-piano","Acoustic piano","piano"),
        ComposerInstrument("fm-electric-piano","FM electric piano","fm-piano"),
        ComposerInstrument("organ","Organ","organ"),
        ComposerInstrument("electric-bass","Electric bass","electric-bass"),
        ComposerInstrument("synth-bass","Synth bass","synth-bass"),
        ComposerInstrument("synth-strings","Synth strings","strings"),
        ComposerInstrument("synth-lead","Synth lead","lead"),
        ComposerInstrument("synth-pad","Synth pad","pad"),
        ComposerInstrument("electronic-drums","Electronic drums","drums",true)
    )
    fun index(ref: InstrumentReference): Int {
        if(ref.registryVersion != 1 || (ref.presetId != "default" && ref.sound == null)) return -1
        if(ref.instrumentId == "reference-piano") return 0 // C1/C2 alias, never rewrite on read.
        return all.indexOfFirst { it.id == ref.instrumentId }
    }
    fun title(ref: InstrumentReference) = ref.presetTitle ?: all.getOrNull(index(ref))?.title ?: "Unavailable sound (${ref.instrumentId})"
    fun identity(ref: InstrumentReference): String {
        val type = all.getOrNull(index(ref))?.title ?: "Unavailable sound (${ref.instrumentId})"
        return ref.presetTitle?.let { "$type — $it" } ?: type
    }
    private fun descriptiveName(track: CompositionTrack) = track.title.takeUnless { it.matches(Regex("(?:Piano|Track) [1-9][0-9]*")) }
    fun compactTrackLabel(track: CompositionTrack, index: Int): String =
        "${index+1} · " + identity(track.instrument) + (descriptiveName(track)?.let { " — $it" } ?: "")
    fun trackLabel(track: CompositionTrack, index: Int): String {
        // Legacy automatic titles are display hints, never instrument identities. Keep saved bytes intact.
        val name = descriptiveName(track)
        return "${index+1} · " + (name?.let { "$it — " } ?: "") + identity(track.instrument)
    }
    fun drumName(pitch: Int): String = when(pitch) {
        35,36 -> "Kick"; 38,40 -> "Snare"; 39 -> "Clap"; 42 -> "Closed hat"; 44 -> "Pedal hat"; 46 -> "Open hat"
        41,43,45,47,48,50 -> "Tom"; 49 -> "Cymbal 1"; 51 -> "Cymbal 2"; 70,82 -> "Shaker"; 75 -> "Claves"; else -> "— $pitch"
    }
}

/** JNI methods are called only by the one worker (or isolated native tests). */
internal object ComposerSynth {
    init { System.loadLibrary("composer_soundfont") }
    external fun create(): Long
    external fun load(handle: Long,bank: Int,bytes: ByteArray): Boolean
    external fun on(handle: Long,bank: Int,channel: Int,pitch: Int,velocity: Int): Long
    external fun off(handle: Long,bank: Int,voice: Long)
    external fun clear(handle: Long,firstChannel: Int,lastChannel: Int)
    external fun volume(handle: Long,channel: Int,gain: Float)
    external fun shape(handle: Long,channel: Int,enabled: Boolean,attack: Float,release: Float,cutoff: Float,q: Float,sustain: Float,pan: Float)
    external fun voices(handle: Long): Int
    external fun render(handle: Long,output: ShortArray,offsetFrames: Int,frames: Int)
    external fun destroy(handle: Long)
}
