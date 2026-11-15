package com.mrayover.practicelogger

import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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

class ComposerC4BWorkflowTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private val inst get()=InstrumentationRegistry.getInstrumentation()
    private val device get()=UiDevice.getInstance(inst)
    private val dir get()=File(inst.targetContext.getExternalFilesDir(null),"composer-c4b").apply { mkdirs() }
    @get:Rule val failure=object: org.junit.rules.TestWatcher() {
        override fun failed(e: Throwable?,d: org.junit.runner.Description?) { device.takeScreenshot(File(dir,"failure.png")); device.dumpWindowHierarchy(File(dir,"failure.xml")) }
    }
    private fun activity(): ComposerActivity { var a: ComposerActivity?=null; inst.runOnMainSync { a=ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).filterIsInstance<ComposerActivity>().single() }; return a!! }
    private fun vm()=ViewModelProvider(activity())[ComposerViewModel::class.java]
    private fun current()=vm().project!!
    private fun tap(t: String) { compose.waitUntil(20000) { compose.onAllNodesWithText(t).fetchSemanticsNodes(false).any { !it.config.contains(androidx.compose.ui.semantics.SemanticsProperties.Disabled) } }; val n=compose.onNodeWithText(t); if(!n.isDisplayed()) n.performScrollTo(); n.performClick(); compose.waitForIdle() }
    private fun pattern() { compose.onNodeWithText("Pattern ",substring=true).performClick() }
    private fun instrument() { compose.onNodeWithText("Instrument ",substring=true).performClick() }
    private fun action(t: String,track: Boolean=false) { if(track) instrument() else pattern(); tap(t) }
    private fun workspace() { compose.waitUntil(30000) { compose.onAllNodesWithTag("piano-roll").fetchSemanticsNodes(false).isNotEmpty() }; compose.waitForIdle() }
    private fun shot(name: String) { compose.waitForIdle(); device.waitForIdle(); Thread.sleep(350); assertTrue(device.takeScreenshot(File(dir,"$name.png"))) }
    private fun rename(action: String,name: String,track: Boolean=false) { action(action,track); compose.onNodeWithTag("management-name").performTextReplacement(name); device.pressBack(); tap("Save name") }
    private fun save() { tap("Save"); compose.waitUntil(15000) { !vm().busy && !vm().dirty } }
    private fun undoTo(p: CompositionProject) { tap("Undo"); assertEquals(p,current()) }
    @Test fun namingPatternTrackUndoSnapshotAndColdReopen() {
        val store=CompositionStore(File(inst.targetContext.filesDir,"composer/projects"))
        if(InstrumentationRegistry.getArguments().getString("c4bCold")=="true") {
            val expected=CompositionCodec.decode(File(dir,"project.json").readText()); assertEquals(expected,store.read(expected.id))
            inst.runOnMainSync { compose.activity.startActivity(Intent(compose.activity,ComposerActivity::class.java).putExtra(ComposerActivity.PROJECT_ID,expected.id)) }; workspace()
            compose.onNodeWithText("Instrument 1 · Countermelody — Electronic drums").assertIsDisplayed()
            instrument(); compose.onNodeWithText("3 · Synth pad — Warm Sweep").assertIsDisplayed(); shot("10-cold-instruments"); device.pressBack()
            val audio=activity().auditionEngine!!; compose.waitUntil(30000) { audio.ready }; tap("Play"); compose.waitUntil(15000) { audio.playing && audio.nonZeroFramesWritten>0 }; tap("Stop")
            return
        }
        device.executeShellCommand("wm size 1440x3120"); device.executeShellCommand("wm density 480")
        val notes=listOf(CompositionNote(newCompositionId(),60,0,960,87),CompositionNote(newCompositionId(),64,1920,480,64))
        val a=CompositionPattern(newCompositionId(),"A",4,notes)
        val t=CompositionTrack(newCompositionId(),"Piano 3",ComposerInstruments.all[7].reference,listOf(a))
        val bass=CompositionTrack(newCompositionId(),"Track 2",ComposerInstruments.all[3].reference,listOf(a.copy(id=newCompositionId(),lengthBars=8,notes=notes.map { it.copy(id=newCompositionId(),pitch=43) })))
        val preset=ComposerPresetStore(File(inst.targetContext.filesDir,"composer/presets")).create("Warm Sweep","synth-pad",ComposerSound(cutoffHz=850f))
        val pad=CompositionTrack(newCompositionId(),"Track 3",preset.reference,listOf(a.copy(id=newCompositionId(),notes=notes.map { it.copy(id=newCompositionId()) })))
        val initial=store.create(CompositionProject.empty("C4B management").copy(formatVersion=2,tracks=listOf(t,bass,pad)))
        inst.runOnMainSync { compose.activity.startActivity(Intent(compose.activity,ComposerActivity::class.java).putExtra(ComposerActivity.PROJECT_ID,initial.id)) }; workspace()
        compose.onNodeWithText("Instrument 1 · Synth pad").assertIsDisplayed(); shot("01-correct-legacy-selector")
        instrument(); compose.onNodeWithText("2 · Electric bass").assertIsDisplayed(); compose.onNodeWithText("3 · Synth pad — Warm Sweep").assertIsDisplayed(); shot("02-instrument-identities"); tap("3 · Synth pad — Warm Sweep")
        compose.onNodeWithText("Instrument 3 · Synth pad — Warm Sweep").assertIsDisplayed(); shot("03-custom-preset")
        instrument(); tap("1 · Synth pad")
        tap("Sound Synth pad"); tap("Electronic drums"); compose.onNodeWithText("Instrument 1 · Electronic drums").assertIsDisplayed()
        val beforeRename=current(); action("Rename Track",true); shot("04-rename-track"); compose.onNodeWithTag("management-name").performTextReplacement("Countermelody"); device.pressBack(); tap("Save name")
        compose.onNodeWithText("Instrument 1 · Countermelody — Electronic drums").assertIsDisplayed(); undoTo(beforeRename); rename("Rename Track","Countermelody",true)
        tap("Sound Electronic drums"); tap("Organ"); compose.onNodeWithText("Instrument 1 · Countermelody — Organ").assertIsDisplayed(); tap("Sound Organ"); tap("Electronic drums")
        val audio=activity().auditionEngine!!; compose.waitUntil(30000) { audio.ready }
        tap("Play"); compose.waitUntil(10000) { audio.playing }; val plan=audio.playbackPlan!!; assertEquals(8*initial.barTicks(),plan.regionTicks)
        repeat(4) { val before=current(); action("Create Pattern"); assertEquals(before.tracks[0].patterns.size+1,current().tracks[0].patterns.size); if(it==0) { undoTo(before); action("Create Pattern") } }
        assertSame(plan,audio.playbackPlan); assertTrue(audio.playing)
        instrument(); tap("2 · Electric bass"); compose.onNodeWithText("Pattern A").assertIsDisplayed(); instrument(); tap("1 · Countermelody — Electronic drums"); compose.onNodeWithText("Pattern E").assertIsDisplayed()
        pattern(); compose.onNodeWithText("E").assertIsDisplayed(); shot("05-expanded-patterns"); compose.onNodeWithText("Delete Pattern").performScrollTo(); shot("06-management-menu"); tap("A")
        val source=current().tracks[0].patterns[0]; val beforeCopy=current(); action("Duplicate Pattern")
        val duplicate=current().tracks[0].patterns.last(); assertNotEquals(source.id,duplicate.id); assertEquals(source.notes[0].copy(id=duplicate.notes[0].id),duplicate.notes[0]); undoTo(beforeCopy); action("Duplicate Pattern")
        val beforePatternRename=current(); rename("Rename Pattern","Variation"); assertEquals("Variation",current().tracks[0].patterns.last().title); undoTo(beforePatternRename); rename("Rename Pattern","Variation")
        val beforeDelete=current(); action("Delete Pattern"); shot("07-delete-pattern-confirmation"); tap("Cancel"); assertEquals(beforeDelete,current()); action("Delete Pattern"); tap("Confirm deletion"); assertEquals(beforeDelete.tracks[0].patterns.size-1,current().tracks[0].patterns.size); undoTo(beforeDelete)
        pattern(); tap("Variation"); action("Delete Pattern"); tap("Confirm deletion")
        // The source and all its note identities remain unchanged through independent duplication/deletion.
        assertEquals(source,current().tracks[0].patterns.first()); assertSame(plan,audio.playbackPlan); tap("Stop")
        val beforeAdd=current(); tap("+ Instrument"); assertEquals(4,current().tracks.size); undoTo(beforeAdd); tap("+ Instrument")
        val beforeTrackDelete=current(); action("Delete Track",true); shot("08-delete-track-confirmation"); tap("Cancel"); assertEquals(beforeTrackDelete,current()); action("Delete Track",true); tap("Confirm deletion"); undoTo(beforeTrackDelete); instrument(); tap("4 · Acoustic piano"); action("Delete Track",true); tap("Confirm deletion")
        instrument(); tap("2 · Electric bass"); tap("Mute ☐"); tap("Solo ☐")
        tap("Play"); compose.waitUntil(10000) { audio.playing }; val deletionPlan=audio.playbackPlan!!
        action("Delete Track",true); tap("Confirm deletion"); assertTrue(audio.playing); assertSame(deletionPlan,audio.playbackPlan)
        assertEquals(listOf(t.id,pad.id),current().tracks.map { it.id }); tap("Stop"); tap("Play"); compose.waitUntil(10000) { audio.playing && audio.playbackPlan!==deletionPlan }; assertEquals(listOf(t.id,pad.id),audio.playbackPlan!!.trackIds); tap("Stop")
        tap("Undo"); assertEquals(listOf(t.id,bass.id,pad.id),current().tracks.map { it.id })
        instrument(); tap("1 · Countermelody — Electronic drums")
        save(); val exact=current(); File(dir,"project.json").writeText(CompositionCodec.encode(exact)); assertEquals(exact,store.read(exact.id))
        val act=activity(); inst.runOnMainSync { act.recreate() }; workspace(); assertEquals(exact,current()); compose.onNodeWithText("Instrument 1 · Countermelody — Electronic drums").assertIsDisplayed(); shot("09-recreated")
        assertEquals(preset.reference,ComposerPresetStore(File(inst.targetContext.filesDir,"composer/presets")).read(preset.id).reference)
        File(dir,"result.txt").writeText("PASS naming, factory/custom identities, structural operations/Undo, cancellation, snapshot retention, deleted-track indexing, save/recreation. Cold reopen is a separate execution.\n")
    }
}
