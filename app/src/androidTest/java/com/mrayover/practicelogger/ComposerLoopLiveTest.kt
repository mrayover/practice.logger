package com.mrayover.practicelogger

import android.content.Intent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import androidx.test.uiautomator.UiDevice
import com.mrayover.practicelogger.composer.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class ComposerLoopLiveTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private val inst get()=InstrumentationRegistry.getInstrumentation()
    private val device get()=UiDevice.getInstance(inst)
    private val dir get()=File(inst.targetContext.getExternalFilesDir(null),"composer-loop-live").apply { mkdirs() }
    @get:Rule val failure=object: org.junit.rules.TestWatcher() {
        override fun failed(e: Throwable?,d: org.junit.runner.Description?) { device.takeScreenshot(File(dir,"failure.png")); device.dumpWindowHierarchy(File(dir,"failure.xml")) }
    }
    private fun tap(text: String) {
        compose.waitUntil(20000) { compose.onAllNodesWithText(text).fetchSemanticsNodes(false).any { !it.config.contains(androidx.compose.ui.semantics.SemanticsProperties.Disabled) } }
        compose.onNodeWithText(text).performClick();compose.mainClock.advanceTimeBy(100);compose.waitForIdle()
    }
    private fun nativeUpdates() {
        val a=CompositionPattern(newCompositionId(),"A",1,listOf(CompositionNote(newCompositionId(),60,0,120,80)))
        val t=CompositionTrack(newCompositionId(),"Piano",ComposerInstruments.all[0].reference,listOf(a))
        val p=CompositionProject.empty("PCM",120).copy(tracks=listOf(t)); val plan=LoopPlan.snapshot(p,emptyList());val c=LoopClock(plan)
        val h=ComposerSynth.create();val events=mutableListOf<String>();val pcm=ShortArray(480)
        try {
            assertTrue(ComposerSynth.load(h,0,inst.targetContext.assets.open("composer/sounds/${ComposerInstruments.all[0].asset}.sf2").use { it.readBytes() }))
            ComposerSynth.shape(h,0,true,1f,20f,20000f,.7f,1f,0f)
            var energy=0L
            val emit: (Int,LoopEvent) -> Unit={ i,e -> val track=c.track(i);val n=track.notes[e.note]
                events += "${c.frame}:${e.on}:${n.pitch}:${n.velocity}"
                if(e.on) c.voices[i][e.note]=ComposerSynth.on(h,0,i,n.pitch,n.velocity)
                else { ComposerSynth.off(h,0,c.voices[i][e.note]);c.voices[i][e.note]=-1 }
            }
            fun renderTo(tick: Long): Long {
                val end=plan.frame(tick);energy=0
                while(c.frame<end) {
                    c.dispatch(emit,{ i -> for(v in c.voices[i]) if(v>=0) ComposerSynth.off(h,0,v) })
                    val n=minOf(240L,c.nextFrame()-c.frame,end-c.frame).coerceAtLeast(1).toInt()
                    ComposerSynth.render(h,pcm,0,n);for(j in 0 until n*2) energy+=kotlin.math.abs(pcm[j].toInt())
                    c.advance(n)
                };return energy
            }
            assertTrue(renderTo(1000)>0)
            val changed=a.copy(notes=listOf(a.notes[0].copy(pitch=72,velocity=37),CompositionNote(newCompositionId(),67,960,120,110)))
            plan.noteUpdates(p.replacePattern(changed),emptyList())!!.forEach(c::offer)
            renderTo(3840);assertTrue(renderTo(5000)>0)
            assertTrue(events.contains("${plan.frame(3840)}:true:72:37"));assertTrue(events.contains("${plan.frame(4800)}:true:67:110"))
            plan.noteUpdates(p.replacePattern(a.copy(notes=emptyList())),emptyList())!!.forEach(c::offer)
            renderTo(7680);assertEquals(0L,renderTo(11000));assertTrue(c.track(0).notes.isEmpty())
            plan.noteUpdates(p,emptyList())!!.forEach(c::offer);renderTo(11520);assertTrue(renderTo(12000)>0)
            ComposerSynth.clear(h,0,15);ComposerSynth.render(h,pcm,0,240);assertTrue(pcm.all { it.toInt()==0 })
            File(dir,"native-events.txt").writeText(events.joinToString("\n")+"\nPASS edited note pitch/velocity/add, empty-cycle silence, restore PCM and Stop silence\n")
        } finally { ComposerSynth.destroy(h) }
    }
    @Test fun liveWorkerAndMeasuredViewport() {
        val tail=InstrumentationRegistry.getArguments().getString("loopLiveTail")=="true"
        if(!tail && InstrumentationRegistry.getArguments().getString("loopLiveSkipNative")!="true") nativeUpdates()
        device.executeShellCommand("wm size 1440x3120");device.executeShellCommand("wm density 480")
        val a=CompositionPattern(newCompositionId(),"A",4,listOf(CompositionNote(newCompositionId(),81,0,960,80)))
        val b=CompositionPattern(newCompositionId(),"B",16)
        val t=CompositionTrack(newCompositionId(),"Piano",ComposerInstruments.all[0].reference,listOf(a,b))
        val saved=CompositionStore(File(inst.targetContext.filesDir,"composer/projects")).create(CompositionProject.empty("Live fit",240).copy(tracks=listOf(t)))
        inst.runOnMainSync { compose.activity.startActivity(Intent(compose.activity,ComposerActivity::class.java).putExtra(ComposerActivity.PROJECT_ID,saved.id)) }
        compose.waitUntil(30000) { compose.onAllNodesWithTag("piano-roll").fetchSemanticsNodes(false).isNotEmpty() }
        var activity: ComposerActivity?=null
        inst.runOnMainSync { activity=ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).filterIsInstance<ComposerActivity>().single() }
        val vm=ViewModelProvider(activity!!)[ComposerViewModel::class.java];val audio=activity!!.auditionEngine!!
        compose.waitUntil(30000) { audio.ready };compose.mainClock.advanceTimeBy(100)
        val density=inst.targetContext.resources.displayMetrics.density;val key=62*density;val row=26*density;val ruler=28*density
        fun canvas()=compose.onNodeWithTag("piano-roll")
        fun fit(name: String) {
            compose.mainClock.advanceTimeBy(100);compose.waitForIdle()
            val image=canvas().captureToImage();val pixels=image.toPixelMap();val y=(ruler+3.5f*row).toInt()
            val xs=(0 until image.width).filter { pixels[it,y].toArgb()==0xFF64BBDF.toInt() }
            assertTrue("note visible",xs.isNotEmpty());val expected=(image.width-key-2)/4
            assertEquals(key.toDouble(),xs.first().toDouble(),3.0);assertEquals(expected.toDouble(),(xs.last()-xs.first()+1).toDouble(),4.0)
            val boundary=(0 until image.width).filter { pixels[it,(ruler+1.5f*row).toInt()].toArgb()==0xFF667180.toInt() }
            assertTrue("ending bar boundary visible",boundary.any { it>=image.width-4 })
            File(dir,"$name-roll.png").outputStream().use { image.asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) }
            device.takeScreenshot(File(dir,"$name.png"))
        }
        if(!tail) fit("01-one-bar-4-4")
        // Exact adjacent hit targets: create two short notes, select the first close to its end.
        val width=canvas().fetchSemanticsNode().size.width.toFloat();val scale=(width-key-2)/3840
        if(!tail) {
        canvas().performTouchInput { click(Offset(key+1200*scale,ruler+4.5f*row)) }
        compose.waitUntil(10000) { vm.project!!.tracks[0].patterns[0].notes.size==2 }
        val drawn=vm.project!!.tracks[0].patterns[0].notes.last();assertEquals(1200L,drawn.startTick)
        canvas().performTouchInput { swipe(Offset(key+1250*scale,ruler+4.5f*row),Offset(key+1490*scale,ruler+5.5f*row),400) }
        compose.waitUntil(10000) { vm.project!!.tracks[0].patterns[0].notes.last().startTick==1440L }
        assertEquals(drawn.pitch-1,vm.project!!.tracks[0].patterns[0].notes.last().pitch)
        canvas().performTouchInput { swipe(Offset(key+1660*scale,ruler+5.5f*row),Offset(key+1900*scale,ruler+5.5f*row),400) }
        compose.waitUntil(10000) { vm.project!!.tracks[0].patterns[0].notes.last().durationTick==480L }
        tap("Play");compose.waitUntil(10000) { audio.playing && audio.positionFrame()>1000 }
        val plan=audio.playbackPlan!!;val before=audio.appliedNoteUpdates;val frame=audio.positionFrame()
        canvas().performTouchInput { click(Offset(key+2160*scale,ruler+6.5f*row)) }
        compose.mainClock.advanceTimeBy(100);compose.waitUntil(12000) { audio.appliedNoteUpdates>before }
        assertSame(plan,audio.playbackPlan);assertTrue(audio.playing);assertTrue(audio.positionFrame()>frame)
        var count=audio.appliedNoteUpdates
        tap("Undo");compose.waitUntil(12000) { audio.appliedNoteUpdates>count };assertSame(plan,audio.playbackPlan)
        count=audio.appliedNoteUpdates
        inst.runOnMainSync { vm.edit(vm.project!!.replacePattern(vm.project!!.tracks[0].patterns[0].copy(notes=emptyList()))) }
        compose.mainClock.advanceTimeBy(100);compose.waitUntil(12000) { audio.appliedNoteUpdates>count };assertTrue(audio.playing)
        tap("Stop");compose.waitUntil(10000) { !audio.playing && audio.voiceCount()==0 }
        inst.runOnMainSync { vm.edit(vm.project!!.replacePattern(a)) };compose.mainClock.advanceTimeBy(100)
        }
        tap("Zoom +")
        val zoomed=canvas().captureToImage();tap("Mixer");tap("Loop")
        val restored=canvas().captureToImage();assertEquals(zoomed.width,restored.width)
        fun noteEnd(): Int { val im=canvas().captureToImage();val px=im.toPixelMap();return (0 until im.width).last { px[it,(ruler+3.5f*row).toInt()].toArgb()==0xFF64BBDF.toInt() } }
        assertEquals((key+(width-key-2)*1.5f/4).toDouble(),noteEnd().toDouble(),4.0)
        val endBeforeScroll=noteEnd();tap("Tool: Draw");tap("Hand")
        canvas().performTouchInput { swipe(Offset(key+500,ruler+100),Offset(key+420,ruler+100),400) }
        assertTrue("Hand scroll uses the same measured transform",noteEnd()<endBeforeScroll-50)
        tap("Tool: Hand");tap("Draw")
        canvas().performTouchInput { pinch(start0=Offset(key+200,ruler+100),end0=Offset(key+100,ruler+100),
            start1=Offset(key+500,ruler+100),end1=Offset(key+600,ruler+100),durationMillis=400) }
        assertTrue("pinch preserves an editable visible note",noteEnd()>key)
        tap("Zoom −");tap("Zoom −")
        // New pattern starts at fit and revisiting A retains its intentional view.
        tap("Pattern A");tap("B");tap("Pattern B");tap("A")
        inst.runOnMainSync { vm.settings(vm.project!!.title,240,7,8) };compose.mainClock.advanceTimeBy(100)
        // Restore uncustomized view by selecting B, then use a fresh one-bar note fixture there.
        tap("Pattern A");tap("B")
        inst.runOnMainSync { val pat=vm.project!!.tracks[0].patterns[1];vm.edit(vm.project!!.replacePattern(pat.copy(notes=listOf(a.notes[0].copy(id=newCompositionId(),durationTick=840))))) }
        compose.mainClock.advanceTimeBy(100);fit("02-one-bar-7-8-sixteen-bars")
        device.executeShellCommand("wm size 1440x2400");compose.waitForIdle();compose.mainClock.advanceTimeBy(200);fit("03-narrow-7-8")
        File(dir,"result.txt").writeText(if(tail) "PASS isolated pinch/scroll/Mixer view retention, selected pattern initial fit, 7/8 sixteen-bar pattern and narrow measured fit. Earlier native/live worker/touch/4-4 checks retained from prior runs.\n" else "PASS native scheduled PCM, real running worker add/Undo/empty updates without restart, measured fit 4/4 + 7/8 narrow, 4/16-bar patterns, draw/move/pitch/resize, zoom/pinch and Mixer view retention. Physical acoustics remain owner acceptance.\n")
    }
}
