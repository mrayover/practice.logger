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
class ComposerRecoveryTest {
    private val app get() = ApplicationProvider.getApplicationContext<Application>()
    private fun await(condition: () -> Boolean) {
        val deadline = System.nanoTime() + 5_000_000_000L
        while (!condition() && System.nanoTime() < deadline) {
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(50))
            Thread.sleep(10)
        }
        assertTrue("Saved-state recovery did not become ready", condition())
    }
    @Test fun pendingCreationRecoversOnceAndConsumedLaunchDoesNotReplay() {
        val p = CompositionProject.empty("Recovered creation")
        val handle = SavedStateHandle(mapOf("pendingCreate" to CompositionCodec.encode(p)))
        val vm = ComposerLibraryViewModel(app, handle)
        await { !vm.busy }
        assertEquals(p.id, vm.openId)
        vm.launched()
        assertNull(vm.openId); assertNull(handle.get<String>("pendingOpen")); assertNull(handle.get<String>("pendingCreate"))
        val restored = ComposerLibraryViewModel(app, SavedStateHandle())
        await { !restored.busy }
        assertNull(restored.openId)
        assertEquals(1, restored.listing.projects.count { it.id == p.id })
        CompositionStore(File(app.filesDir, "composer/projects")).delete(p)
    }
    @Test fun dirtySettingsRecoverWithoutChangingSavedIdentityOrReplacingFile() {
        val store = CompositionStore(File(app.filesDir, "composer/projects"))
        val saved = store.create(CompositionProject.empty("Saved settings", 111, 5, 4))
        val draft = saved.copy(title = "Unsaved settings", tempoBpm = 143)
        val handle = SavedStateHandle(mapOf(ComposerActivity.PROJECT_ID to saved.id, "draft" to CompositionCodec.encode(draft)))
        val recovered = ComposerViewModel(app, handle)
        await { !recovered.loading }
        assertNull(recovered.error); assertEquals(draft, recovered.project); assertTrue(recovered.dirty)
        assertEquals(saved, store.read(saved.id))
        recovered.save()
        await { !recovered.busy }
        assertNull(recovered.error); assertFalse(recovered.dirty)
        assertEquals(saved.id, store.read(saved.id).id); assertEquals(143, store.read(saved.id).tempoBpm)
        assertNull(handle.get<String>("draft"))
        store.delete(requireNotNull(recovered.project))
    }
}
