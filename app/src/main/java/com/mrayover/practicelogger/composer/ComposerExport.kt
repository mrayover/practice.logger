package com.mrayover.practicelogger.composer

import android.content.Context
import android.media.*
import android.os.StatFs
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.ceil
import kotlin.math.roundToInt

internal enum class ComposerExportFormat(val extension: String, val mime: String, val description: String) {
    M4A("m4a", "audio/mp4", "M4A · compact backing-track file"),
    WAV("wav", "audio/wav", "WAV · uncompressed audio, larger file")
}

internal data class ComposerExportSnapshot(val project: CompositionProject, val plan: LoopPlan) {
    companion object {
        fun capture(project: CompositionProject, selections: List<String>, arrangement: Boolean): ComposerExportSnapshot {
            // Detach all collections; no editor/history/store ownership crosses into the renderer.
            val frozen=project.copy(tracks=project.tracks.map { t -> t.copy(patterns=t.patterns.map { p ->
                p.copy(notes=p.notes.map { it.copy() })
            }) }, clips=project.clips.map { it.copy() })
            val plan=if(arrangement) LoopPlan.songSnapshot(frozen) else LoopPlan.snapshot(frozen,selections.toList())
            require(plan.tracks.isNotEmpty()) { "Add an instrument before exporting." }
            require(plan.frame(plan.regionTicks) <= MAX_EXPORT_FRAMES - 31L*48000) {
                "This composition exceeds the export limit (about 6 hours 12 minutes, the stereo PCM16 RIFF size limit)."
            }
            return ComposerExportSnapshot(frozen,plan)
        }
    }
}

internal const val MAX_EXPORT_FRAMES = (0xffffffffL-36)/4
internal fun exportFilename(title: String, format: ComposerExportFormat): String =
    title.replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), "_").trim().trim('.').take(100).ifBlank { "Composition" } + "." + format.extension

internal interface ComposerPcmSink : AutoCloseable {
    fun write(pcm: ShortArray, frames: Int)
    fun finish()
}

/** The header is finalized only after every PCM frame has been written. */
internal class ComposerWaveSink(file: File) : ComposerPcmSink {
    private val output=RandomAccessFile(file,"rw")
    private val bytes=ByteBuffer.allocate(1024).order(ByteOrder.LITTLE_ENDIAN)
    var frames=0L; private set
    init {
        try { output.setLength(0); output.write(ByteArray(44)) }
        catch(failure: Throwable) { output.close(); throw failure }
    }
    override fun write(pcm: ShortArray, frames: Int) {
        require(frames in 1..256 && this.frames+frames<=MAX_EXPORT_FRAMES)
        bytes.clear(); for(i in 0 until frames*2) bytes.putShort(pcm[i])
        output.write(bytes.array(),0,frames*4); this.frames+=frames
    }
    override fun finish() {
        val h=ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        h.put("RIFF".toByteArray()).putInt((36+frames*4).toInt()).put("WAVEfmt ".toByteArray())
            .putInt(16).putShort(1).putShort(2).putInt(48000).putInt(192000).putShort(4).putShort(16)
            .put("data".toByteArray()).putInt((frames*4).toInt())
        output.seek(0); output.write(h.array()); output.fd.sync()
    }
    override fun close() = output.close()
}

/** Incremental AAC-LC input and MPEG-4 muxing. No uncompressed staging file. */
internal class ComposerAacSink(file: File, private val checkCancelled: () -> Unit) : ComposerPcmSink {
    private var codec: MediaCodec?=null
    private var muxer: MediaMuxer?=null
    private var started=false
    private var track=-1
    private var frames=0L
    private var eos=false
    private val info=MediaCodec.BufferInfo()
    private var lastProgress=android.os.SystemClock.elapsedRealtime()
    init {
        try {
            val format=MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC,48000,2).apply {
                setInteger(MediaFormat.KEY_AAC_PROFILE,MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_BIT_RATE,192000)
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE,4096)
            }
            codec=MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
            codec!!.configure(format,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE)
            muxer=MediaMuxer(file.absolutePath,MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            codec!!.start()
        } catch(failure: Exception) { close(); throw IllegalStateException("M4A/AAC encoder unavailable: ${failure.message}. You can choose WAV instead.",failure) }
    }
    private fun waiting() {
        checkCancelled()
        check(android.os.SystemClock.elapsedRealtime()-lastProgress<10000) { "M4A encoder stopped responding." }
    }
    private fun drain(wait: Boolean=false) {
        val c=codec!!
        do {
            waiting()
            val index=c.dequeueOutputBuffer(info,if(wait) 10000 else 0)
            when {
                index==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    check(!started); track=muxer!!.addTrack(c.outputFormat); muxer!!.start(); started=true
                    lastProgress=android.os.SystemClock.elapsedRealtime()
                }
                index>=0 -> {
                    try {
                        if(info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG==0 && info.size>0) {
                            check(started); val buffer=c.getOutputBuffer(index)!!
                            buffer.position(info.offset); buffer.limit(info.offset+info.size)
                            muxer!!.writeSampleData(track,buffer,info)
                        }
                        eos=info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM!=0
                        lastProgress=android.os.SystemClock.elapsedRealtime()
                    } finally { c.releaseOutputBuffer(index,false) }
                }
                else -> return
            }
        } while(!eos)
    }
    private fun input(): Int {
        while(true) {
            waiting(); val index=codec!!.dequeueInputBuffer(10000)
            if(index>=0) { lastProgress=android.os.SystemClock.elapsedRealtime(); return index }
            drain()
        }
    }
    override fun write(pcm: ShortArray, frames: Int) {
        var offset=0
        while(offset<frames) {
            val index=input(); val buffer=codec!!.getInputBuffer(index)!!; buffer.clear(); buffer.order(ByteOrder.LITTLE_ENDIAN)
            val count=minOf(frames-offset,buffer.remaining()/4); check(count>0)
            for(i in offset*2 until (offset+count)*2) buffer.putShort(pcm[i])
            codec!!.queueInputBuffer(index,0,count*4,this.frames*1000000/48000,0)
            this.frames+=count; offset+=count; drain()
        }
    }
    override fun finish() {
        val index=input()
        codec!!.queueInputBuffer(index,0,0,frames*1000000/48000,MediaCodec.BUFFER_FLAG_END_OF_STREAM)
        while(!eos) drain(true)
        check(started) { "M4A encoder produced no audio track." }
        muxer!!.stop(); started=false
    }
    override fun close() {
        runCatching { codec?.stop() }; runCatching { codec?.release() }; codec=null
        runCatching { muxer?.release() }; muxer=null
    }
}

internal object ComposerOfflineExport {
    fun directory(context: Context)=File(context.cacheDir,"composer-audio-export").also { check(it.isDirectory || it.mkdirs()) }
    fun estimatedBytes(snapshot: ComposerExportSnapshot, format: ComposerExportFormat): Long {
        val frames=snapshot.plan.frame(snapshot.plan.regionTicks)+30L*48000
        return if(format==ComposerExportFormat.WAV) 44+frames*4 else frames*384000/(48000*8)+16L*1024*1024 // 2× target bitrate + container reserve
    }
    fun checkStorage(directory: File, needed: Long) {
        require(StatFs(directory.absolutePath).availableBytes>=needed+8L*1024*1024) { "Not enough temporary storage for this export. Free space and try again." }
    }
    fun render(context: Context, snapshot: ComposerExportSnapshot, format: ComposerExportFormat, file: File,
        checkCancelled: () -> Unit = {}, progress: (Int) -> Unit = {}): Long {
        checkStorage(file.parentFile!!,estimatedBytes(snapshot,format))
        val plan=snapshot.plan; val end=plan.frame(plan.regionTicks)
        var handle=0L
        try {
            handle=ComposerSynth.create(); check(handle!=0L)
            plan.tracks.map { it.bank }.distinct().forEach { bank ->
                checkCancelled()
                val instrument=ComposerInstruments.all[bank]
                val bytes=context.assets.open("composer/sounds/${instrument.asset}.sf2").use { it.readBytes() }
                check(ComposerSynth.load(handle,bank,bytes)) { "Could not load ${instrument.title}." }
            }
            val anySolo=snapshot.project.tracks.any { it.mixer.solo }
            val enabled=plan.tracks.indices.map { i -> snapshot.project.tracks[i].mixer.let { !it.mute && (!anySolo || it.solo) } }
            plan.tracks.forEachIndexed { i,t ->
                val sound=t.sound ?: ComposerSound(); val mix=snapshot.project.tracks[i].mixer; val p=mix.processing
                ComposerSynth.shape(handle,i,t.sound!=null,sound.attackMs,sound.releaseMs,sound.cutoffHz,sound.resonanceQ,sound.sustain,sound.pan)
                ComposerSynth.volume(handle,i,mix.volume); ComposerSynth.pan(handle,i,mix.pan)
                ComposerSynth.processing(handle,i,p.lowDb,p.lowMidDb,p.highMidDb,p.highDb,p.eqEnabled,p.width)
            }
            val tailSeconds=ComposerSynth.releaseBound(handle)
            check(tailSeconds.isFinite() && tailSeconds>=0f) { "Instrument release envelope is invalid." }
            // Some one-shot percussion banks declare a long release but end with their sample.
            // Let active voices finish naturally, with a documented 30-second safety ceiling.
            val tailLimit=ceil(minOf(tailSeconds,30f)*48000.0).toLong()+4800
            val clock=LoopClock(plan,!plan.song)
            val pcm=ShortArray(512)
            val emit: (Int,LoopEvent) -> Unit = { i,event ->
                val t=plan.tracks[i]
                if(event.on) {
                    if(clock.frame<end && enabled[i]) clock.voices[i][event.note]=ComposerSynth.on(handle,t.bank,i,t.notes[event.note].pitch,t.notes[event.note].velocity)
                } else {
                    val voice=clock.voices[i][event.note]
                    if(voice>=0) ComposerSynth.off(handle,t.bank,voice)
                    clock.voices[i][event.note]=-1
                }
            }
            val sink: ComposerPcmSink=if(format==ComposerExportFormat.WAV) ComposerWaveSink(file) else ComposerAacSink(file,checkCancelled)
            sink.use {
                var frames=0L; var released=false; var lastPercent=-1; var storageFrame=0L
                while(true) {
                    checkCancelled()
                    if(frames>=end && !released) {
                        // Deliver endpoint offs, suppress endpoint ons, then release any residual held voice.
                        clock.dispatch(emit)
                        plan.tracks.forEachIndexed { i,t -> clock.voices[i].filter { it>=0 }.forEach { ComposerSynth.off(handle,t.bank,it) } }
                        released=true
                    }
                    if(released && (frames>=end+tailLimit || ComposerSynth.voices(handle)==0)) break
                    if(!released) clock.dispatch(emit)
                    val next=if(released) end+tailLimit else minOf(end,clock.nextFrame())
                    val count=minOf(256L,next-frames).coerceAtLeast(1).toInt()
                    ComposerSynth.render(handle,pcm,0,count)
                    val master=snapshot.project.masterVolume
                    for(i in 0 until count*2) pcm[i]=(pcm[i]*master).roundToInt().coerceIn(-32768,32767).toShort()
                    sink.write(pcm,count); frames+=count; clock.advance(count)
                    if(frames>=storageFrame) { checkStorage(file.parentFile!!,0); storageFrame=frames+48000 }
                    val percent=(frames*100/end).coerceAtMost(99).toInt()
                    if(percent!=lastPercent) { progress(percent); lastPercent=percent }
                }
                checkCancelled(); sink.finish(); return frames
            }
        } catch(failure: Throwable) { file.delete(); throw failure }
        finally { if(handle!=0L) ComposerSynth.destroy(handle) }
    }
}
