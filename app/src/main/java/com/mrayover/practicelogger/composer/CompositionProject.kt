package com.mrayover.practicelogger.composer

import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.util.UUID

const val COMPOSER_FORMAT_VERSION = 1
const val COMPOSER_PPQ = 960
const val COMPOSER_MAX_TRACKS = 8
const val COMPOSER_MAX_PATTERNS = 256
internal fun newCompositionId(): String = UUID.randomUUID().toString()
internal fun validCompositionId(value: String): Boolean =
    runCatching { UUID.fromString(value).toString() == value }.getOrDefault(false)

data class InstrumentReference(val registryVersion: Int, val instrumentId: String, val presetId: String, val sound: ComposerSound? = null, val presetTitle: String? = null)
data class CompositionNote(val id: String, val pitch: Int, val startTick: Long, val durationTick: Long, val velocity: Int)
data class CompositionPattern(val id: String, val title: String, val lengthBars: Int, val notes: List<CompositionNote> = emptyList())
data class TrackMixer(val volume: Float = 1f, val pan: Float = 0f, val mute: Boolean = false, val solo: Boolean = false, val selectedPatternId: String? = null)

data class CompositionTrack(val id: String, val title: String, val instrument: InstrumentReference,
    val patterns: List<CompositionPattern> = emptyList(), val mixer: TrackMixer = TrackMixer())
data class ArrangementClip(val id: String, val trackId: String, val patternId: String, val startTick: Long)
data class CompositionProject(
    val id: String, val title: String, val createdAt: Long, val modifiedAt: Long,
    val revision: Long = 0, val tempoBpm: Int = 120, val numerator: Int = 4, val denominator: Int = 4,
    val ticksPerQuarter: Int = COMPOSER_PPQ, val tracks: List<CompositionTrack> = emptyList(),
    val clips: List<ArrangementClip> = emptyList(), val formatVersion: Int = COMPOSER_FORMAT_VERSION, val masterVolume: Float = 1f
) {
    /** Save As copies the live draft and remaps every identity, including arrangement references. */
    fun independentCopy(title: String, now: Long = System.currentTimeMillis()): CompositionProject {
        CompositionCodec.validate(this)
        val identities = mutableMapOf<String, String>()
        fun fresh(id: String) = identities.getOrPut(id, ::newCompositionId)
        tracks.forEach { t -> fresh(t.id); t.patterns.forEach { fresh(it.id) } }
        val copiedTracks = tracks.map { track -> track.copy(id = fresh(track.id), mixer = track.mixer.copy(selectedPatternId = track.mixer.selectedPatternId?.let { identities.getValue(it) }), patterns = track.patterns.map { pattern ->
            pattern.copy(id = fresh(pattern.id), notes = pattern.notes.map { it.copy(id = fresh(it.id)) })
        }) }
        return copy(id = newCompositionId(), title = title.trim(), createdAt = now, modifiedAt = now, revision = 0,
            tracks = copiedTracks, clips = clips.map { it.copy(id = fresh(it.id), trackId = identities.getValue(it.trackId), patternId = identities.getValue(it.patternId)) })
            .also(CompositionCodec::validate)
    }
    companion object {
        fun empty(title: String, tempo: Int = 120, numerator: Int = 4, denominator: Int = 4,
            id: String = newCompositionId(), now: Long = System.currentTimeMillis()): CompositionProject =
            CompositionProject(id, title.trim(), now, now, tempoBpm = tempo, numerator = numerator, denominator = denominator)
                .also(CompositionCodec::validate)
    }
}

/** Bounded, strict v1–v3 codec. Reject unknown fields/versions rather than lose unrecognized musical data. */
object CompositionCodec {
    const val MAX_BYTES = 8 * 1024 * 1024
    private const val MAX_TICK = 960L * 4 * 1_000_000
    fun validate(p: CompositionProject) {
        require(p.formatVersion in 1..3) { "Unsupported Composer project version." }
        require(p.masterVolume.isFinite() && p.masterVolume in 0f..1f)
        require(p.formatVersion >= 3 || (p.masterVolume == 1f && p.tracks.all { it.mixer == TrackMixer() }))
        val ids = mutableSetOf<String>()
        fun identity(id: String) { require(validCompositionId(id) && ids.add(id)) { "Invalid or repeated project identity." } }
        fun title(value: String) { require(value.isNotBlank() && value == value.trim() && value.length <= 120 && value.none { it.isISOControl() }) { "Use a title of 1–120 characters." } }
        identity(p.id); title(p.title)
        require(p.createdAt > 0 && p.modifiedAt >= p.createdAt && p.revision in 0 until Long.MAX_VALUE)
        require(p.tempoBpm in 30..300) { "Tempo must be a whole number from 30 to 300 BPM." }
        require(p.numerator in 1..32 && p.denominator in setOf(1, 2, 4, 8, 16, 32)) { "Use a meter such as 4/4 or 6/8 (1–32 beats; denominator 1, 2, 4, 8, 16 or 32)." }
        require(p.ticksPerQuarter == COMPOSER_PPQ)
        require(p.tracks.size <= COMPOSER_MAX_TRACKS && p.clips.size <= 10000)
        var noteCount = 0
        p.tracks.forEach { track ->
            identity(track.id); title(track.title)
            val m = track.mixer
            require(m.volume.isFinite() && m.volume in 0f..1f && m.pan.isFinite() && m.pan in -1f..1f)
            require(m.selectedPatternId == null || track.patterns.any { it.id == m.selectedPatternId }) { "Selected pattern must belong to its track." }
            require(track.instrument.registryVersion == 1)
            val ref = track.instrument
            if (ref.sound != null) {
                require(p.formatVersion >= 2 && validCompositionId(ref.presetId) && ComposerInstruments.index(ref) >= 0)
                ref.sound.validate()
                require(ref.presetTitle != null && ref.presetTitle.isNotBlank() && ref.presetTitle.length <= 60 && ref.presetTitle == ref.presetTitle.trim() && ref.presetTitle.none { it.isISOControl() })
            } else require(ref.presetTitle == null)
            listOf(track.instrument.instrumentId, track.instrument.presetId).forEach {
                require(it.matches(Regex("[a-zA-Z0-9][a-zA-Z0-9._:-]{0,119}")))
            }
            require(track.patterns.size <= COMPOSER_MAX_PATTERNS)
            track.patterns.forEach { pattern ->
                identity(pattern.id); title(pattern.title)
                require(pattern.lengthBars in setOf(1, 2, 4, 8, 16))
                val length = pattern.lengthBars.toLong() * p.numerator * COMPOSER_PPQ * 4 / p.denominator
                noteCount += pattern.notes.size
                require(noteCount <= 100000)
                pattern.notes.forEach { note ->
                    identity(note.id)
                    require(note.pitch in 0..127 && note.velocity in 1..127)
                    require(note.startTick in 0 until length && note.durationTick in 1..length && note.startTick <= length - note.durationTick)
                }
            }
        }
        p.clips.forEach { clip ->
            identity(clip.id)
            val track = p.tracks.singleOrNull { it.id == clip.trackId }
            require(track != null && track.patterns.any { it.id == clip.patternId }) { "Clip must reference a pattern belonging to its track." }
            require(clip.startTick in 0..MAX_TICK)
        }
    }
    fun encode(p: CompositionProject): String {
        validate(p)
        fun array(items: List<JSONObject>) = JSONArray().also { a -> items.forEach(a::put) }
        val root = JSONObject().put("formatVersion", p.formatVersion).put("id", p.id).put("title", p.title)
            .put("createdAt", p.createdAt).put("modifiedAt", p.modifiedAt).put("revision", p.revision)
            .put("tempoBpm", p.tempoBpm).put("numerator", p.numerator).put("denominator", p.denominator)
            .put("ticksPerQuarter", p.ticksPerQuarter).also { if(p.formatVersion >= 3) it.put("masterVolume", p.masterVolume) }
            .put("tracks", array(p.tracks.map { t -> JSONObject().put("id", t.id).put("title", t.title).also { if(p.formatVersion >= 3) it.put("mixer", JSONObject().put("volume",t.mixer.volume).put("pan",t.mixer.pan).put("mute",t.mixer.mute).put("solo",t.mixer.solo).put("selectedPatternId",t.mixer.selectedPatternId ?: JSONObject.NULL)) }
                .put("instrument", JSONObject().put("registryVersion", t.instrument.registryVersion).put("instrumentId", t.instrument.instrumentId).put("presetId", t.instrument.presetId).also { if(p.formatVersion >= 2) { it.put("sound",t.instrument.sound?.json() ?: JSONObject.NULL); it.put("presetTitle",t.instrument.presetTitle ?: JSONObject.NULL) } })
                .put("patterns", array(t.patterns.map { pat -> JSONObject().put("id", pat.id).put("title", pat.title).put("lengthBars", pat.lengthBars)
                    .put("notes", array(pat.notes.map { n -> JSONObject().put("id", n.id).put("pitch", n.pitch).put("startTick", n.startTick).put("durationTick", n.durationTick).put("velocity", n.velocity) })) })) }))
            .put("clips", array(p.clips.map { c -> JSONObject().put("id", c.id).put("trackId", c.trackId).put("patternId", c.patternId).put("startTick", c.startTick) }))
        return root.toString().also { require(it.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "Composition exceeds the file size limit." } }
    }
    fun decode(text: String): CompositionProject {
        require(text.toByteArray(Charsets.UTF_8).size <= MAX_BYTES)
        val parser = JSONTokener(text)
        val root = parser.nextValue() as? JSONObject ?: error("Invalid composition document")
        require(parser.nextClean() == '\u0000') { "Unexpected content after composition document." }
        require(root.long("formatVersion") in 1L..3L) { "Unsupported Composer project version. This file has been preserved." }
        val v = root.int("formatVersion")
        root.fields("formatVersion,id,title,createdAt,modifiedAt,revision,tempoBpm,numerator,denominator,ticksPerQuarter,tracks,clips" + if(v >= 3) ",masterVolume" else "")
        return CompositionProject(root.string("id"), root.string("title"), root.long("createdAt"), root.long("modifiedAt"),
            root.long("revision"), root.int("tempoBpm"), root.int("numerator"), root.int("denominator"), root.int("ticksPerQuarter"),
            root.objects("tracks", 8).map { t ->
                t.fields("id,title,instrument,patterns" + if(v >= 3) ",mixer" else "")
                val i = t.getJSONObject("instrument").also { it.fields(if(root.long("formatVersion")==1L) "registryVersion,instrumentId,presetId" else "registryVersion,instrumentId,presetId,sound,presetTitle") }
                CompositionTrack(t.string("id"), t.string("title"), InstrumentReference(i.int("registryVersion"), i.string("instrumentId"), i.string("presetId"), if(i.has("sound") && !i.isNull("sound")) ComposerSound.decode(i.getJSONObject("sound")) else null, if(i.has("presetTitle") && !i.isNull("presetTitle")) i.string("presetTitle") else null),
                    t.objects("patterns", COMPOSER_MAX_PATTERNS).map { pat ->
                        pat.fields("id,title,lengthBars,notes")
                        CompositionPattern(pat.string("id"), pat.string("title"), pat.int("lengthBars"), pat.objects("notes", 100000).map { n ->
                            n.fields("id,pitch,startTick,durationTick,velocity")
                            CompositionNote(n.string("id"), n.int("pitch"), n.long("startTick"), n.long("durationTick"), n.int("velocity"))
                        })
                    }, if(v >= 3) t.getJSONObject("mixer").let { m ->
                        m.fields("volume,pan,mute,solo,selectedPatternId")
                        TrackMixer(m.number("volume"),m.number("pan",-1.0),m.boolean("mute"),m.boolean("solo"),if(m.isNull("selectedPatternId")) null else m.string("selectedPatternId"))
                    } else TrackMixer())
            }, root.objects("clips", 10000).map { c ->
                c.fields("id,trackId,patternId,startTick")
                ArrangementClip(c.string("id"), c.string("trackId"), c.string("patternId"), c.long("startTick"))
            }, formatVersion = v, masterVolume = if(v >= 3) root.number("masterVolume") else 1f).also(::validate)
    }
    private fun JSONObject.number(name: String, minimum: Double = 0.0): Float = (get(name) as? Number)?.toDouble()?.also { require(it.isFinite() && it in minimum..1.0) }?.toFloat() ?: error("Invalid number $name")
    private fun JSONObject.boolean(name: String): Boolean = (get(name) as? Boolean) ?: error("Invalid boolean $name")
    private fun JSONObject.fields(names: String) { require(keys().asSequence().toSet() == names.split(',').toSet()) { "Unrecognized or missing project fields. File preserved." } }
    private fun JSONObject.string(name: String): String = (get(name) as? String) ?: error("Invalid $name")
    private fun JSONObject.long(name: String): Long = when (val value = get(name)) { is Int -> value.toLong(); is Long -> value; else -> error("Invalid integer $name") }
    private fun JSONObject.int(name: String): Int = long(name).also { require(it in Int.MIN_VALUE..Int.MAX_VALUE) }.toInt()
    private fun JSONObject.objects(name: String, max: Int): List<JSONObject> = getJSONArray(name).let { a ->
        require(a.length() <= max); (0 until a.length()).map(a::getJSONObject)
    }
}
