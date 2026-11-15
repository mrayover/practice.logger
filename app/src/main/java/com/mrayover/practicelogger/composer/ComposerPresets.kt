package com.mrayover.practicelogger.composer

import org.json.JSONObject
import org.json.JSONTokener
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption.ATOMIC_MOVE

/** Absolute amplitude envelope; fixed 300ms decay to sustain, never the MIDI pedal. */
data class ComposerSound(val attackMs: Float = 10f, val releaseMs: Float = 300f,
    val cutoffHz: Float = 18000f, val resonanceQ: Float = .707f,
    val sustain: Float = .8f, val pan: Float = 0f) {
    fun validate() {
        require(attackMs.isFinite() && attackMs in 0f..3000f)
        require(releaseMs.isFinite() && releaseMs in 10f..5000f)
        require(cutoffHz.isFinite() && cutoffHz in 40f..18000f)
        require(resonanceQ.isFinite() && resonanceQ in .5f..4f)
        require(sustain.isFinite() && sustain in 0f..1f)
        require(pan.isFinite() && pan in -1f..1f)
    }
    fun json(): JSONObject { validate(); return JSONObject().put("version",1).put("attackMs",attackMs.toDouble()).put("releaseMs",releaseMs.toDouble())
        .put("cutoffHz",cutoffHz.toDouble()).put("resonanceQ",resonanceQ.toDouble()).put("sustain",sustain.toDouble()).put("pan",pan.toDouble()) }
    companion object {
        fun decode(o: JSONObject): ComposerSound {
            require(o.keys().asSequence().toSet()==setOf("version","attackMs","releaseMs","cutoffHz","resonanceQ","sustain","pan"))
            require(o.get("version") == 1)
            fun number(n: String)=(o.get(n) as? Number)?.toFloat() ?: error("Invalid sound parameter")
            return ComposerSound(number("attackMs"),number("releaseMs"),number("cutoffHz"),number("resonanceQ"),number("sustain"),number("pan")).also { it.validate() }
        }
    }
}
internal data class ComposerPreset(val id: String,val title: String,val instrumentId: String,val sound: ComposerSound) {
    val reference get() = InstrumentReference(1,instrumentId,id,sound,title)
    fun json(): JSONObject = JSONObject().put("version",1).put("id",id).put("title",title).put("instrumentId",instrumentId).put("sound",sound.json())
}
/** Immutable per-UUID files, independent of projects. Failed creates never alter committed presets. */
internal class ComposerPresetStore(private val directory: File,private val beforeCommit: (() -> Unit)? = null) {
    companion object { private val lock=Any() }
    fun list(): Pair<List<ComposerPreset>,Int> = synchronized(lock) {
        var invalid=0
        val values=directory.listFiles().orEmpty().filter { it.extension=="json" }.mapNotNull { file ->
            runCatching { read(file.nameWithoutExtension) }.getOrElse { invalid++; null }
        }.sortedBy { it.title.lowercase() }
        values to invalid
    }
    fun read(id: String): ComposerPreset = synchronized(lock) {
        require(validCompositionId(id))
        val file=File(directory,"$id.json"); require(file.length() in 1..16384)
        val parser=JSONTokener(Charsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(file.readBytes())).toString())
        val o=parser.nextValue() as JSONObject; require(parser.nextClean()=='\u0000')
        require(o.keys().asSequence().toSet()==setOf("version","id","title","instrumentId","sound")); require(o.get("version")==1)
        ComposerPreset(o.getString("id"),o.getString("title"),o.getString("instrumentId"),ComposerSound.decode(o.getJSONObject("sound"))).also { validate(it); require(it.id==id) }
    }
    fun create(title: String,instrumentId: String,sound: ComposerSound): ComposerPreset = synchronized(lock) {
        val p=ComposerPreset(newCompositionId(),title.trim(),instrumentId,sound); validate(p)
        check(directory.isDirectory || directory.mkdirs()) { "Preset storage unavailable." }
        val target=File(directory,(p.id+".json")); check(!target.exists())
        val temporary=File(directory,(p.id+".new"))
        try {
            FileOutputStream(temporary).use { it.write(p.json().toString().toByteArray(Charsets.UTF_8)); it.fd.sync() }
            beforeCommit?.invoke(); Files.move(temporary.toPath(),target.toPath(),ATOMIC_MOVE)
            check(read(p.id)==p); p
        } catch(e: Exception) { temporary.delete(); throw e }
    }
    private fun validate(p: ComposerPreset) {
        require(validCompositionId(p.id)); require(p.title.isNotBlank() && p.title==p.title.trim() && p.title.length<=60 && p.title.none { it.isISOControl() }) { "Use a name of 1–60 characters." }
        require(ComposerInstruments.all.any { it.id==p.instrumentId }); p.sound.validate()
    }
}
