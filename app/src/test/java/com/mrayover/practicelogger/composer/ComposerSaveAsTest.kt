package com.mrayover.practicelogger.composer

import android.app.Application
import android.os.Looper
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File
import java.time.Duration

@RunWith(RobolectricTestRunner::class) @Config(sdk = [35])
class ComposerSaveAsTest {
    private val app get() = ApplicationProvider.getApplicationContext<Application>()
    private fun await(done: () -> Boolean) {
        val end = System.nanoTime()+10_000_000_000
        while (!done() && System.nanoTime()<end) { shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(25)); Thread.sleep(10) }
        assertTrue(done())
    }
    private fun music(): CompositionProject {
        val pattern = CompositionPattern(newCompositionId(),"A",4,listOf(CompositionNote(newCompositionId(),71,320,160,87)))
        val track = CompositionTrack(newCompositionId(),"Keys",ComposerInstruments.all[1].reference,listOf(pattern))
        return CompositionProject.empty("Original",137,7,8).copy(tracks=listOf(track),clips=listOf(ArrangementClip(newCompositionId(),track.id,pattern.id,960)))
    }
    @Test fun copyRemapsAllIdentitiesAndPreservesMusicAndClipOwnership() {
        val original = music().copy(revision=12)
        val copy = original.independentCopy("  New title  ",12345)
        assertEquals("New title",copy.title); assertEquals(0L,copy.revision); assertEquals(12345L,copy.createdAt); assertEquals(copy.createdAt,copy.modifiedAt)
        assertNotEquals(original.id,copy.id)
        assertEquals(original.copy(id=copy.id,title=copy.title,createdAt=copy.createdAt,modifiedAt=copy.modifiedAt,revision=0,tracks=copy.tracks,clips=copy.clips),copy)
        val t=copy.tracks.single(); val p=t.patterns.single(); val n=p.notes.single()
        assertNotEquals(original.tracks.single().id,t.id); assertNotEquals(original.tracks.single().patterns.single().id,p.id)
        assertNotEquals(original.tracks.single().patterns.single().notes.single().id,n.id)
        assertEquals(original.tracks.single().patterns.single().notes.single().copy(id=n.id),n)
        assertEquals(original.tracks.single().instrument,t.instrument)
        assertEquals(t.id,copy.clips.single().trackId); assertEquals(p.id,copy.clips.single().patternId)
        assertNotEquals(original.clips.single().id,copy.clips.single().id)
        assertEquals(copy,CompositionCodec.decode(CompositionCodec.encode(copy)))
    }
    @Test fun saveAsUsesUnsavedDraftSwitchesOwnershipAndClearsUndoAcrossRecovery() {
        val store=CompositionStore(File(app.filesDir,"composer/projects")); val original=store.create(music())
        val handle=SavedStateHandle(mapOf(ComposerActivity.PROJECT_ID to original.id))
        val vm=ComposerViewModel(app,handle); await { !vm.busy }
        vm.edit(original.copy(tempoBpm=149)); assertTrue(vm.dirty); assertEquals(1,vm.undoCount)
        vm.saveAs("Draft copy"); await { !vm.busy }
        assertNull(vm.error); assertFalse(vm.dirty); assertEquals(0,vm.undoCount)
        val copy=requireNotNull(vm.project); assertEquals(149,copy.tempoBpm); assertEquals(original,store.read(original.id))
        assertEquals(copy.id,handle.get<String>(ComposerActivity.PROJECT_ID)); assertTrue(store.list().projects.contains(copy))
        val restored=ComposerViewModel(app,handle); await { !restored.busy }; assertEquals(copy,restored.project)
        restored.edit(copy.copy(tempoBpm=155)); restored.save(); await { !restored.busy }
        assertEquals(155,store.read(copy.id).tempoBpm); assertEquals(original,store.read(original.id))
    }
    @Test fun invalidTitleAndStorageFailureRetainOriginalAndDraft() {
        val directory=File(app.filesDir,"composer/projects"); val store=CompositionStore(directory); val original=store.create(music())
        val vm=ComposerViewModel(app,SavedStateHandle(mapOf(ComposerActivity.PROJECT_ID to original.id))); await { !vm.busy }
        vm.edit(original.copy(tempoBpm=151)); val draft=vm.project
        vm.saveAs(" "); assertNotNull(vm.error); assertEquals(draft,vm.project); assertTrue(vm.dirty)
        val failing=CompositionStore(directory) { error("Simulated storage failure") }
        assertTrue(runCatching { failing.create(requireNotNull(draft).independentCopy("Failed copy")) }.isFailure)
        assertEquals(original,store.read(original.id)); assertEquals(draft,vm.project)
        assertFalse(store.list().projects.any { it.title=="Failed copy" })
    }
}
