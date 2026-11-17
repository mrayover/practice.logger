package com.mrayover.practicelogger

import android.content.Intent
import android.media.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import androidx.test.uiautomator.*
import com.mrayover.practicelogger.composer.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.regex.Pattern
import kotlin.math.abs
import kotlinx.coroutines.*

class ComposerExportWorkflowTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private val inst get()=InstrumentationRegistry.getInstrumentation()
    private val context get()=inst.targetContext
    private val device get()=UiDevice.getInstance(inst)
    private val scratch get()=File(context.cacheDir,"composer-export-verification").apply { mkdirs() }
    private val dir get()=File(context.getExternalFilesDir(null),"composer-export").apply { mkdirs() }
    @get:Rule val failure=object: org.junit.rules.TestWatcher() {
        override fun failed(e: Throwable?,d: org.junit.runner.Description?) { device.takeScreenshot(File(dir,"failure.png"));device.dumpWindowHierarchy(File(dir,"failure.xml")) }
    }
    private fun fixture(): CompositionProject {
        val tracks=listOf(4,2,1).mapIndexed { i,bars ->
            val notes=mutableListOf(CompositionNote(newCompositionId(),if(i==2)36 else if(i==1)48 else 72,0,960,80+i*15))
            if(i==0) notes+=CompositionNote(newCompositionId(),76,14400,960,100)
            CompositionTrack(newCompositionId(),"Export ${i+1}",ComposerInstruments.all[listOf(0,3,8)[i]].reference,
                listOf(CompositionPattern(newCompositionId(),"A",bars,notes)))
        }
        var p=CompositionProject.empty("C6A backing",240).copy(formatVersion=4,tracks=tracks)
        for(t in tracks) p=ArrangementEdits.add(p,t.id,t.patterns[0].id,0)
        p=ArrangementEdits.add(p,tracks[2].id,tracks[2].patterns[0].id,6*p.barTicks())
        return ArrangementEdits.makeUnique(p,p.clips.last().id).let { unique ->
            val variation=unique.tracks[2].patterns.last()
            unique.replacePattern(variation.copy(notes=variation.notes.map { it.copy(pitch=38,velocity=47) }))
        }
    }
    private fun render(p: CompositionProject,name: String,song: Boolean=false,format: ComposerExportFormat=ComposerExportFormat.WAV): Pair<File,Long> {
        val f=File(scratch,"$name.${format.extension}")
        val n=ComposerOfflineExport.render(context,ComposerExportSnapshot.capture(p,emptyList(),song),format,f)
        return f to n
    }
    private fun pcm(file: File)=ByteBuffer.wrap(file.readBytes(),44,(file.length()-44).toInt()).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().let { b -> ShortArray(b.remaining()).also { b.get(it) } }
    private fun energy(samples: ShortArray,start: Int=0,end: Int=samples.size)=samples.sliceArray(start until minOf(end,samples.size)).sumOf { abs(it.toInt()).toLong() }
    private data class Decoded(val frames: Long,val energy: Long,val early: Long,val late: Long)
    private fun decode(file: File): Decoded {
        val extractor=MediaExtractor(); var decoder: MediaCodec?=null
        try {
            extractor.setDataSource(file.absolutePath);assertEquals(1,extractor.trackCount)
            val format=extractor.getTrackFormat(0)
            assertEquals("audio/mp4a-latm",format.getString(MediaFormat.KEY_MIME));assertEquals(48000,format.getInteger(MediaFormat.KEY_SAMPLE_RATE));assertEquals(2,format.getInteger(MediaFormat.KEY_CHANNEL_COUNT))
            val csd=format.getByteBuffer("csd-0")!!;assertEquals(2,(csd.get(0).toInt() and 255) ushr 3)
            extractor.selectTrack(0);decoder=MediaCodec.createDecoderByType("audio/mp4a-latm");decoder.configure(format,null,null,0);decoder.start()
            val info=MediaCodec.BufferInfo();var sent=false;var finished=false;var frames=0L;var total=0L;var early=0L;var late=0L
            val deadline=android.os.SystemClock.elapsedRealtime()+20000
            while(!finished) {
                check(android.os.SystemClock.elapsedRealtime()<deadline)
                if(!sent) {
                    val index=decoder.dequeueInputBuffer(10000)
                    if(index>=0) {
                        val input=decoder.getInputBuffer(index)!!;val size=extractor.readSampleData(input,0)
                        if(size<0) { decoder.queueInputBuffer(index,0,0,0,MediaCodec.BUFFER_FLAG_END_OF_STREAM);sent=true }
                        else { decoder.queueInputBuffer(index,0,size,extractor.sampleTime,0);extractor.advance() }
                    }
                }
                val index=decoder.dequeueOutputBuffer(info,10000)
                if(index>=0) {
                    val output=decoder.getOutputBuffer(index)!!.order(ByteOrder.LITTLE_ENDIAN);output.position(info.offset);output.limit(info.offset+info.size)
                    var samples=0
                    while(output.remaining()>=2) { val v=abs(output.short.toInt()).toLong();total+=v;if(frames+samples/2<4800)early+=v;if(frames+samples/2>180000)late+=v;samples++ }
                    frames+=samples/2;finished=info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM!=0;decoder.releaseOutputBuffer(index,false)
                }
            }
            return Decoded(frames,total,early,late).also { File(dir,"aac-decoded.txt").writeText("$format\n$it") }
        } finally { runCatching { decoder?.stop() };decoder?.release();extractor.release() }
    }
    @Test fun nativeFilesMixerTailCancellationAndRetry() {
        val p=fixture();val original=CompositionCodec.encode(p)
        val (wave,frames)=render(p,"loop-native")
        assertTrue(frames>=192000);assertTrue(frames<192000+31*48000)
        val samples=pcm(wave);assertEquals(frames*2,samples.size.toLong());assertTrue(energy(samples)>0)
        val (song,songFrames)=render(p,"arrange-native",true)
        assertTrue(songFrames>=336000);assertEquals(0L,energy(pcm(song),5*48000*2,6*48000*2))
        val (aac,aacFrames)=render(p,"loop-native",format=ComposerExportFormat.M4A)
        assertEquals(frames,aacFrames);assertTrue(aac.length()<wave.length()/3)
        assertEquals("ftyp",String(aac.readBytes(),4,4))
        val decoded=decode(aac);assertTrue(abs(decoded.frames-frames)<=4096);assertTrue(decoded.energy>0);assertTrue(decoded.early>0);assertTrue(decoded.late>0)
        val half=pcm(render(p.copy(masterVolume=.5f),"half").first)
        assertEquals(samples.size,half.size)
        for(i in samples.indices) assertEquals(kotlin.math.round(samples[i]*.5f).toDouble(),half[i].toDouble(),1.0)
        val muted=p.copy(tracks=p.tracks.map { it.copy(mixer=it.mixer.copy(mute=true)) })
        assertEquals(0L,energy(pcm(render(muted,"muted").first)))
        val solo=p.copy(tracks=p.tracks.mapIndexed { i,t -> t.copy(mixer=t.mixer.copy(solo=i==0,pan=-1f)) })
        val soloPcm=pcm(render(solo,"solo-left").first);assertTrue(energy(soloPcm)>0)
        assertEquals(0L,soloPcm.indices.filter { it%2==1 }.sumOf { abs(soloPcm[it].toInt()).toLong() })
        val processed=p.copy(tracks=p.tracks.map { it.copy(mixer=it.mixer.copy(volume=.6f,processing=ChannelProcessing(lowDb=6f,highDb=-8f,width=0f))) })
        assertFalse(samples.contentEquals(pcm(render(processed,"processed").first)))
        val shape=ComposerSound(releaseMs=500f)
        val t=p.tracks[0].copy(instrument=p.tracks[0].instrument.copy(presetId=newCompositionId(),sound=shape,presetTitle="Tail"),patterns=listOf(CompositionPattern(newCompositionId(),"Held",1,listOf(CompositionNote(newCompositionId(),72,0,3840,100)))))
        val tail=p.copy(tracks=listOf(t),clips=emptyList())
        val (tailFile,tailFrames)=render(tail,"tail")
        assertTrue(tailFrames>48000);assertTrue(tailFrames<=48000+28800);assertTrue(energy(pcm(tailFile),96000)>0)
        val cancelled=File(scratch,"cancelled.wav");var checks=0
        assertTrue(runCatching { ComposerOfflineExport.render(context,ComposerExportSnapshot.capture(p,emptyList(),false),ComposerExportFormat.WAV,cancelled,{ if(++checks>20) throw CancellationException("test") }) }.isFailure)
        assertFalse(cancelled.exists());assertTrue(wave.readBytes().contentEquals(render(p,"retry").first.readBytes()))
        assertEquals(original,CompositionCodec.encode(p))
        listOf(wave,song,aac,tailFile).forEach { file -> file.inputStream().use { input -> File(dir,file.name).outputStream().use { input.copyTo(it) } } }
        File(dir,"native-results.txt").writeText("Loop $frames frames / ${wave.length()} bytes; Arrange $songFrames frames; AAC $aacFrames frames / ${aac.length()} bytes; decoded $decoded; custom tail $tailFrames frames. Mixer, silence, cancellation and retry pass.")
    }
    private fun activity(): ComposerActivity { var a: ComposerActivity?=null;inst.runOnMainSync { a=ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).filterIsInstance<ComposerActivity>().single() };return a!! }
    @Test fun destinationFailureAndEncoderSetupFailureCleanOnlyExportFiles() {
        val source=fixture();val before=CompositionCodec.encode(source)
        val store=androidx.lifecycle.ViewModelStore()
        lateinit var export: ComposerExportViewModel
        inst.runOnMainSync {
            export=ComposerExportViewModel(context.applicationContext as android.app.Application,androidx.lifecycle.SavedStateHandle())
            store.put("export-safety",export)
            export.prepare(source,emptyList(),false,ComposerExportFormat.WAV)
        }
        try {
            compose.waitUntil(15000) { export.request!=null }
            inst.runOnMainSync { export.destination(android.net.Uri.parse("content://missing.c6a.provider/export")) {} }
            compose.waitUntil(30000) { !export.active }
            assertTrue(export.message!!.startsWith("Export failed:"))
            assertTrue(ComposerOfflineExport.directory(context).listFiles().orEmpty().isEmpty())
            inst.runOnMainSync { export.prepare(source,emptyList(),false,ComposerExportFormat.WAV) }
            compose.waitUntil(15000) { export.request!=null }
            inst.runOnMainSync { export.destination(android.net.Uri.parse("content://missing.c6a.provider/cancel")) { delay(60000) } }
            assertTrue(export.rendering)
            inst.runOnMainSync { export.cancel() }
            compose.waitUntil(10000) { !export.active && !export.rendering }
            assertTrue(export.message!!.startsWith("Export cancelled."))
            val invalid=File(scratch,"encoder-invalid-directory").apply { mkdirs() }
            assertTrue(runCatching { ComposerAacSink(invalid) {}.use { it.finish() } }.isFailure)
            // A new real encoder still works after configuration/muxer setup failure.
            assertTrue(decode(render(source,"encoder-after-failure",format=ComposerExportFormat.M4A).first).energy>0)
            assertEquals(before,CompositionCodec.encode(source))
            File(dir,"failure-cleanup.txt").writeText("Destination failure and AAC setup failure release resources; private export staging empty; source unchanged; encoder retry decodes.")
        } finally { inst.runOnMainSync { store.clear() } }
    }
    private fun vm()=ViewModelProvider(activity())[ComposerViewModel::class.java]
    private fun tap(text: String) { compose.onNodeWithText(text).performClick();compose.waitForIdle() }
    private fun exportAction() {
        if(compose.onAllNodesWithText("Export Audio").fetchSemanticsNodes(false).isEmpty()) tap("More ▾")
        tap("Export Audio")
    }
    private fun shot(name: String) { compose.waitForIdle();device.takeScreenshot(File(dir,"$name.png")) }
    private fun readSavedFile(name: String, outputName: String): File {
        val output=File(scratch,outputName)
        android.os.ParcelFileDescriptor.AutoCloseInputStream(inst.uiAutomation.executeShellCommand("cat /sdcard/Download/$name")).use { input -> output.outputStream().use { input.copyTo(it) } }
        output.inputStream().use { input -> File(dir,outputName).outputStream().use { input.copyTo(it) } }
        return output
    }
    private fun savePicker(name: String) {
        device.wait(Until.hasObject(By.clazz("android.widget.EditText")),10000)
        device.waitForIdle()
        if(!device.hasObject(By.text("Downloads"))) {
            device.wait(Until.findObject(By.desc("Show roots")),10000)!!.click()
            val entry=device.wait(Until.findObject(By.text("Downloads")),10000)!!.visibleBounds
            device.click(entry.centerX(),entry.centerY())
            device.waitForIdle()
        }
        assertTrue(device.wait(Until.hasObject(By.text("Downloads")),10000))
        device.wait(Until.findObject(By.clazz("android.widget.EditText")),10000)!!.text=name
        device.wait(Until.findObject(By.text(Pattern.compile("SAVE",Pattern.CASE_INSENSITIVE))),10000)!!.click()
        compose.waitUntil(30000) { compose.onAllNodesWithTag("composer-export-result").fetchSemanticsNodes(false).isNotEmpty() }
        compose.onNodeWithText("Audio exported",substring=true).assertExists();shot("saved-${name.substringAfterLast('.')}");tap("OK")
    }
    @Test fun realPickerUnsavedDraftFilesAndPlaybackSurviveExport() {
        device.executeShellCommand("wm size 1440x3120");device.executeShellCommand("wm density 480")
        val store=CompositionStore(File(context.filesDir,"composer/projects"));val saved=store.create(fixture())
        inst.runOnMainSync { compose.activity.startActivity(Intent(compose.activity,ComposerActivity::class.java).putExtra(ComposerActivity.PROJECT_ID,saved.id)) }
        compose.waitUntil(30000) { compose.onAllNodesWithTag("piano-roll").fetchSemanticsNodes(false).isNotEmpty() }
        val audio=activity().auditionEngine!!;compose.waitUntil(30000) { audio.ready }
        val editor=vm()
        inst.runOnMainSync { editor.editLatest { it.copy(masterVolume=.43f,title="C6A unsaved backing") } }
        val draft=vm().project!!;val undo=vm().undoCount
        exportAction();compose.onNodeWithText("M4A · compact backing-track file").assertExists();shot("export-dialog")
        tap("Choose destination");savePicker("c6a-owner-loop.m4a")
        assertEquals(draft,vm().project);assertEquals(undo,vm().undoCount);assertEquals(saved,store.read(saved.id));assertTrue(vm().dirty)
        val m4a=readSavedFile("c6a-owner-loop.m4a","picker-loop.m4a")
        assertTrue(decode(m4a).energy>0)
        tap("Arrange");exportAction();tap("Full Arrangement");tap("WAV — uncompressed");shot("arrange-wave-dialog");tap("Choose destination");savePicker("c6a-owner-arrange.wav")
        val wave=readSavedFile("c6a-owner-arrange.wav","picker-arrange.wav")
        val expected=render(draft,"picker-expected",true).first;assertTrue(expected.readBytes().contentEquals(wave.readBytes()))
        exportAction();tap("Choose destination");device.wait(Until.hasObject(By.clazz("android.widget.EditText")),10000);device.pressBack()
        compose.waitUntil(15000) { compose.onAllNodesWithText("Export cancelled.").fetchSemanticsNodes(false).isNotEmpty() };tap("OK")
        assertEquals(draft,vm().project);assertEquals(undo,vm().undoCount)
        val resumedAudio=activity().auditionEngine!!;compose.waitUntil(30000) { resumedAudio.ready }
        tap("Play");compose.waitUntil(15000) { resumedAudio.playing && resumedAudio.playbackPlan!!.song };tap("Stop")
        tap("Loop");tap("Play");compose.waitUntil(15000) { resumedAudio.playing && resumedAudio.playbackPlan?.song==false };tap("Stop")
        assertEquals(draft,vm().project);assertEquals(undo,vm().undoCount)
        assertTrue(ComposerOfflineExport.directory(context).listFiles().orEmpty().isEmpty());shot("returned-draft")
    }
}
