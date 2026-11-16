package com.mrayover.practicelogger.composer

import android.content.*
import android.media.*
import android.os.*
import java.util.concurrent.LinkedBlockingQueue

/** Composer-only output/focus owner. Native state, commands and rendering have one owner. */
internal class ComposerAudio(context: Context,private val interrupted: (String?) -> Unit = {}) {
    private val app=context.applicationContext
    private val manager=app.getSystemService(AudioManager::class.java)
    private val main=Handler(Looper.getMainLooper())
    private val attributes=AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
    private val focus=AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(attributes).setWillPauseWhenDucked(true)
        .setOnAudioFocusChangeListener({ if(it != AudioManager.AUDIOFOCUS_GAIN) interrupt() },main).build()
    private val commands=LinkedBlockingQueue<() -> Unit>()
    private var foreground=true
    private var focusOwned=false
    @Volatile private var closed=false
    @Volatile private var epoch=0
    @Volatile var ready=false; private set
    @Volatile var playing=false; private set
    @Volatile var looping=true; private set
    @Volatile private var output: AudioTrack?=null
    @Volatile private var voiceTotal=0
    @Volatile internal var nonZeroFramesWritten=0L; private set
    @Volatile internal var framesWritten=0L; private set
    @Volatile var playbackPlan: LoopPlan?=null; private set
    @Volatile private var frozenFrame=0L
    @Volatile internal var masterGain=1f; private set
    @Volatile internal var appliedMasterGain: Float?=null; private set
    private var handle=0L
    private var clock: LoopClock?=null
    @Volatile private var draining=false
    private var stopAtFrame=Long.MAX_VALUE
    private val allowed=BooleanArray(8) { true }
    private val gains=FloatArray(8) { 1f }
    private val auditionTokens=LongArray(64) { Long.MIN_VALUE }
    private val auditionBanks=IntArray(64)
    private val auditionVoices=LongArray(64)
    private val emit: (Int,LoopEvent) -> Unit = { track,event ->
        val c=clock!!; val t=c.plan.tracks[track]
        if(event.on) {
            c.voices[track][event.note]=if(allowed[track]) ComposerSynth.on(handle,t.bank,track,t.notes[event.note].pitch,t.notes[event.note].velocity) else -1
        } else {
            val voice=c.voices[track][event.note]
            if(voice>=0) ComposerSynth.off(handle,t.bank,voice)
            c.voices[track][event.note]=-1
        }
    }
    private val noisy=object : BroadcastReceiver() { override fun onReceive(context: Context?,intent: Intent?) { interrupt() } }
    init {
        if(Build.VERSION.SDK_INT>=33) app.registerReceiver(noisy,IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY),Context.RECEIVER_NOT_EXPORTED)
        else app.registerReceiver(noisy,IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
        Thread(::run,"composer-soundfont").start()
    }
    internal val interruptionToken get() = epoch
    internal fun voiceCount()=voiceTotal
    fun positionFrame(): Long = if(playing || draining) minOf(output?.playbackHeadPosition?.toLong()?.and(0xffffffffL) ?: frozenFrame,framesWritten) else frozenFrame
    private fun report(message: String?) { main.post { if(!closed) interrupted(message) } }
    private fun focus(): Boolean {
        if(closed || !foreground) return false
        if(!ready) { report("Instrument sounds are loading or unavailable. Try again when ready."); return false }
        if(!focusOwned) focusOwned=manager.requestAudioFocus(focus)==AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        if(!focusOwned) report("Audio is in use. Try again.")
        return focusOwned
    }
    private fun submit(action: () -> Unit) { val generation=epoch; commands.offer { if(!closed && generation==epoch) action() } }
    fun noteOn(token: Long,pitch: Int,bank: Int=0,track: Int=0,velocity: Int=100): Boolean {
        if(bank !in ComposerInstruments.all.indices || !focus()) return false
        submit {
            releaseToken(token)
            val slot=auditionTokens.indexOfFirst { it==Long.MIN_VALUE }
            if(slot>=0) {
                startOutput()
                auditionTokens[slot]=token; auditionBanks[slot]=bank
                auditionVoices[slot]=ComposerSynth.on(handle,bank,8+track,pitch.coerceIn(0,127),velocity.coerceIn(1,127))
            }
        }
        return true
    }
    private fun configure(channel: Int,sound: ComposerSound?) {
        val v=sound ?: ComposerSound()
        ComposerSynth.shape(handle,channel,sound!=null,v.attackMs,v.releaseMs,v.cutoffHz,v.resonanceQ,v.sustain,v.pan)
    }
    fun previewSound(track: Int,sound: ComposerSound?) { sound?.validate(); submit { configure(8+track,sound) } }
    fun noteOff(token: Long) { submit { releaseToken(token) } }
    private fun releaseToken(token: Long) {
        val i=auditionTokens.indexOf(token)
        if(i>=0) { ComposerSynth.off(handle,auditionBanks[i],auditionVoices[i]); auditionTokens[i]=Long.MIN_VALUE }
    }
    /** Pattern switches/gesture cancellation release only audition, preserving the Loop transport. */
    fun stopAll() { submit { ComposerSynth.clear(handle,8,15); auditionTokens.fill(Long.MIN_VALUE) } }
    fun play(plan: LoopPlan,repeat: Boolean) {
        if(!focus()) return
        submit {
            stopOutput(); ComposerSynth.clear(handle,0,15); auditionTokens.fill(Long.MIN_VALUE)
            plan.tracks.forEachIndexed { i,t -> configure(i,t.sound) }
            clock=LoopClock(plan); playbackPlan=plan; frozenFrame=0; looping=repeat; draining=false
            stopAtFrame=if(repeat) Long.MAX_VALUE else plan.frame(plan.regionTicks)
            startOutput(); playing=true
        }
    }
    fun setLoop(value: Boolean) { submit {
        looping=value
        val c=clock
        stopAtFrame=if(value || c==null) Long.MAX_VALUE else c.plan.frame((c.plan.tick(c.frame).toLong()/c.plan.regionTicks+1)*c.plan.regionTicks)
    } }
    fun stop() { submit {
        frozenFrame=positionFrame(); playing=false; draining=false; clock=null
        ComposerSynth.clear(handle,0,15); auditionTokens.fill(Long.MIN_VALUE); stopOutput()
    } }
    fun beginning() { submit {
        val plan=playbackPlan; val wasPlaying=playing
        playing=false; draining=false; stopOutput(); ComposerSynth.clear(handle,0,15); auditionTokens.fill(Long.MIN_VALUE)
        frozenFrame=0; clock=if(wasPlaying && plan!=null) LoopClock(plan) else null
        stopAtFrame=if(looping || plan==null) Long.MAX_VALUE else plan.frame(plan.regionTicks)
        if(wasPlaying) { startOutput(); playing=true }
    } }
    fun mix(enabled: BooleanArray,volume: FloatArray,previewVolume: FloatArray = volume,pan: FloatArray = FloatArray(8),previewPan: FloatArray = pan) {
        submit {
            for(i in 0..7) {
                val active=enabled.getOrElse(i) { true }
                if(!active && allowed[i]) ComposerSynth.clear(handle,i,i)
                allowed[i]=active; gains[i]=volume.getOrElse(i) { 1f }.coerceIn(0f,1f)
                ComposerSynth.pan(handle,i,pan.getOrElse(i) { 0f }); ComposerSynth.pan(handle,8+i,previewPan.getOrElse(i) { 0f })
                ComposerSynth.volume(handle,i,gains[i]); ComposerSynth.volume(handle,8+i,previewVolume.getOrElse(i) { 1f }.coerceIn(0f,1f))
            }
        }
    }
    fun processing(sequence: List<ChannelProcessing>, preview: List<ChannelProcessing>) {
        (sequence + preview).forEach { it.validate() }
        submit {
            for(i in 0..15) {
                val p = (if(i < 8) sequence.getOrNull(i) else preview.getOrNull(i-8)) ?: ChannelProcessing()
                ComposerSynth.processing(handle,i,p.lowDb,p.lowMidDb,p.highMidDb,p.highDb,p.eqEnabled,p.width)
            }
        }
    }
    fun masterVolume(value: Float) {
        require(value.isFinite() && value in 0f..1f)
        // Retain independently of output lifetime and command interruption.
        masterGain=value
        submit { output?.let { check(it.setVolume(masterGain)==AudioTrack.SUCCESS); appliedMasterGain=masterGain } }
    }
    fun setForeground(value: Boolean) { foreground=value; if(!value) interrupt() }
    private fun interrupt() {
        if(closed) return
        epoch++; commands.clear()
        playing=false
        commands.offer {
            frozenFrame=0; playing=false; draining=false; clock=null
            ComposerSynth.clear(handle,0,15); auditionTokens.fill(Long.MIN_VALUE); stopOutput()
        }
        if(focusOwned) { manager.abandonAudioFocusRequest(focus); focusOwned=false }
        report(null)
    }
    fun close() {
        if(closed) return
        closed=true; epoch++; commands.clear(); commands.offer {}
        if(focusOwned) { manager.abandonAudioFocusRequest(focus); focusOwned=false }
        app.unregisterReceiver(noisy)
    }
    private fun startOutput() {
        if(output!=null) return
        val minimum=AudioTrack.getMinBufferSize(48000,AudioFormat.CHANNEL_OUT_STEREO,AudioFormat.ENCODING_PCM_16BIT)
        check(minimum>0)
        val audio=AudioTrack.Builder().setAudioAttributes(attributes)
            .setAudioFormat(AudioFormat.Builder().setSampleRate(48000).setChannelMask(AudioFormat.CHANNEL_OUT_STEREO).setEncoding(AudioFormat.ENCODING_PCM_16BIT).build())
            .setTransferMode(AudioTrack.MODE_STREAM).setBufferSizeInBytes(maxOf(minimum,3840)).setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY).build()
        check(audio.state==AudioTrack.STATE_INITIALIZED)
        check(audio.setVolume(masterGain)==AudioTrack.SUCCESS)
        appliedMasterGain=masterGain
        framesWritten=0; output=audio; audio.play()
    }
    private fun stopOutput() {
        val old=output; output=null; appliedMasterGain=null
        runCatching { old?.pause() }; runCatching { old?.flush() }; runCatching { old?.release() }
        main.post { if(output==null && !playing && focusOwned) { manager.abandonAudioFocusRequest(focus); focusOwned=false } }
    }
    private fun run() {
        try {
            handle=ComposerSynth.create()
            ComposerInstruments.all.forEachIndexed { i,instrument ->
                if(closed) return
                val bytes=app.assets.open("composer/sounds/${instrument.asset}.sf2").use { it.readBytes() }
                check(ComposerSynth.load(handle,i,bytes)) { "Could not load ${instrument.title}." }
            }
            ready=true
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
            val pcm=ShortArray(480)
            var quiet=0
            while(!closed) {
                if(output==null) commands.take().invoke()
                while(true) { val command=commands.poll() ?: break; command() }
                if(closed) break
                // Stop can dispose output before another render; publish the cleared state now.
                voiceTotal=ComposerSynth.voices(handle)
                val audio=output ?: continue
                var offset=0
                while(offset<240) {
                    val c=clock
                    if(playing && c!=null) {
                        val end=stopAtFrame
                        if(!looping && c.frame>=end) {
                            ComposerSynth.clear(handle,0,7); playing=false; draining=true; clock=null; frozenFrame=end
                            continue
                        }
                        c.dispatch(emit)
                        val next=if(looping) c.nextFrame() else minOf(c.nextFrame(),end)
                        val count=minOf((240-offset).toLong(),next-c.frame).coerceAtLeast(1).toInt()
                        ComposerSynth.render(handle,pcm,offset,count); c.advance(count); offset+=count
                    } else { ComposerSynth.render(handle,pcm,offset,240-offset); offset=240 }
                }
                var written=0
                while(written<pcm.size && !closed) {
                    val n=audio.write(pcm,written,pcm.size-written,AudioTrack.WRITE_BLOCKING)
                    check(n>0) { "Composer output write failed." }; written+=n
                }
                framesWritten+=written/2
                var audible=0L; for(s in pcm) if(s.toInt()!=0) audible++
                nonZeroFramesWritten+=audible
                voiceTotal=ComposerSynth.voices(handle)
                quiet=if(!playing && voiceTotal==0) quiet+240 else 0
                if(draining && positionFrame()>=frozenFrame) draining=false
                if(quiet>=48000) { stopOutput(); quiet=0 }
            }
        } catch(failure: Throwable) {
            ready=false; report("Composer audio unavailable: ${failure.message}")
        } finally {
            playing=false; voiceTotal=0; stopOutput()
            if(handle!=0L) ComposerSynth.destroy(handle)
        }
    }
}
