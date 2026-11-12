package com.mrayover.practicelogger

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt

class BlockEditorCollectionWorkflowTest : WeekUiChecks() {
    private val vm get() = ViewModelProvider(compose.activity)[ImageReferenceViewModel::class.java]
    @Before fun reset() { db.clearAllTables() }
    private fun ready() = compose.waitUntil(15_000) { vm.editor.ready && !vm.busy }
    private fun field(tag: String, text: String) = compose.onNodeWithTag(tag).scrollIfNeeded().performTextReplacement(text)
    private fun tag(tag: String) = compose.onNodeWithTag(tag).scrollIfNeeded().assertIsDisplayed()
    private fun choose(role: MediaRole, id: String) {
        tag("block-${role.name}-choose").assertIsEnabled().performClick(); click("Choose from Library")
        waitForTag("media-library-list")
        compose.onNodeWithTag("media-library-list").performScrollToNode(hasTestTag("media-choose-$id"))
        compose.onNodeWithTag("media-choose-$id").performClick(); ready()
    }
    private fun save() { compose.onNodeWithTag("block-save").assertIsDisplayed().assertIsEnabled().performClick(); shown("Block Library") }
    private fun wav(value: Int): ByteArray = ByteBuffer.allocate(64044).order(ByteOrder.LITTLE_ENDIAN).apply {
        put("RIFF".toByteArray()); putInt(64036); put("WAVEfmt ".toByteArray()); putInt(16)
        putShort(1); putShort(1); putInt(8000); putInt(16000); putShort(2); putShort(16)
        put("data".toByteArray()); putInt(64000); repeat(32000) { putShort(value.toShort()) }
    }.array()
    @Test fun multipleBeforeSavePreviewReorderRemoveRecreateSaveReopenAndConfirm() {
        val app = compose.activity.application
        val library = MediaLibraryRepository(db, File(app.filesDir, "media-library"))
        val pdf = runBlocking { library.import("Pages.pdf", "application/pdf") { l1Fixture("references/score.pdf").inputStream() } }
        val jpg = ByteArrayOutputStream().use { output -> Bitmap.createBitmap(32, 24, Bitmap.Config.ARGB_8888).let { it.compress(Bitmap.CompressFormat.JPEG, 90, output); it.recycle() }; output.toByteArray() }
        val image = runBlocking { library.import("Image.jpg", "image/jpeg") { jpg.inputStream() } }
        val audio = runBlocking { library.import("First.wav", "audio/wav") { wav(1).inputStream() } }
        click("Blocks"); click("New Block"); ready()
        field("Name", "Editor collection"); field("Action", "Practice")
        choose(MediaRole.VISUAL, image.id); choose(MediaRole.VISUAL, pdf.id); choose(MediaRole.AUDIO, audio.id)
        choose(MediaRole.VISUAL, image.id)
        compose.onNodeWithTag("block-media-message").scrollIfNeeded().assertTextContains("already attached", substring = true)
        runBlocking { vm.editor.stage(MediaRole.AUDIO, "Pending.wav", "audio/wav") { wav(2).inputStream() } }
        val pending = vm.editor.audioReferences.last().asset!!
        tag("reference-up-library:${pdf.id}").performClick(); ready()
        tag("reference-up-library:${pending.id}").performClick(); ready()
        assertEquals(pdf.id, vm.editor.visual!!.id); assertEquals(pending.id, vm.editor.audio!!.id)
        tag("reference-preview-library:${pdf.id}").performClick()
        click("Next page"); shown("Page 2 of 2"); click("Close preview")
        tag("reference-preview-library:${pending.id}").performClick(); click("Play preview")
        shown("Stop preview"); click("Close preview")
        assertTrue(runBlocking { db.blocks().readAll().isEmpty() })
        // Removing an individual pending item leaves all other selections and order intact.
        tag("reference-remove-library:${image.id}").performClick(); ready()
        assertEquals(listOf(pdf.id), vm.editor.visualReferences.map { it.asset!!.id })
        compose.activityRule.scenario.recreate(); shown("New Block"); ready()
        compose.onNodeWithTag("Name").scrollIfNeeded().assertTextContains("Editor collection")
        assertEquals(listOf(pending.id, audio.id), vm.editor.audioReferences.map { it.asset!!.id })
        save()
        val block = runBlocking { db.blocks().readAll().single() }
        assertEquals(listOf("library:${pending.id}", "library:${audio.id}"), runBlocking { db.referenceOrder(block.id, MediaRole.AUDIO) })
        openBlockPill(compose, "block-library-list", "library-block-${block.id}"); click("Edit", "block-library-list"); ready()
        tag("reference-preview-library:${pdf.id}").performClick(); click("Close preview")
        tag("reference-remove-library:${pending.id}").performClick(); click("Keep reference")
        assertEquals(2, vm.editor.audioReferences.size)
        tag("reference-remove-library:${pending.id}").performClick(); click("Remove reference"); ready(); save()
        assertEquals(listOf("library:${audio.id}"), runBlocking { db.referenceOrder(block.id, MediaRole.AUDIO) })
        assertTrue(library.available(pending)); assertTrue(library.available(pdf))
    }
    @Test fun compact320dp200PercentThemesKeepControlsAndPinnedSaveUsable() {
        val app = compose.activity.application
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val originalScale = device.executeShellCommand("settings get system font_scale").trim().toFloatOrNull() ?: 1f
        val density = app.resources.displayMetrics.density
        val library = MediaLibraryRepository(db, File(app.filesDir, "media-library"))
        val pdf = runBlocking { library.import("A deliberately long musical reference filename for ellipsis.pdf", "application/pdf") { l1Fixture("references/score.pdf").inputStream() } }
        val second = runBlocking { library.import("Second.pdf", "application/pdf") { l1Fixture("references/score.pdf").inputStream() } }
        val audio = runBlocking { library.import("Long audio reference filename for ellipsis.wav", "audio/wav") { wav(3).inputStream() } }
        try {
            click("Blocks"); click("New Block"); ready(); field("Name", "Accessible collection"); field("Action", "Play")
            choose(MediaRole.VISUAL, pdf.id); choose(MediaRole.VISUAL, second.id); choose(MediaRole.AUDIO, audio.id)
            device.executeShellCommand("wm size ${(320 * density).roundToInt()}x${(880 * density).roundToInt()}")
            device.executeShellCommand("settings put system font_scale 2.0")
            for (theme in listOf(ThemeChoice.LIGHT, ThemeChoice.DARK, ThemeChoice.ORCHID)) {
                ThemePreferences(app).save(theme); compose.activityRule.scenario.recreate(); shown("New Block"); ready()
                assertTrue(compose.activity.resources.configuration.screenWidthDp <= 321)
                assertTrue(compose.activity.resources.configuration.fontScale >= 1.99f)
                compose.onNodeWithTag("block-save").assertIsDisplayed().assertIsEnabled()
                for (identity in listOf("library:${pdf.id}", "library:${second.id}", "library:${audio.id}")) {
                    tag("reference-preview-$identity").assertIsEnabled()
                    tag("reference-remove-$identity").assertIsEnabled()
                }
                tag("reference-up-library:${second.id}").assertIsEnabled().performClick(); ready()
                tag("reference-down-library:${second.id}").assertIsEnabled().performClick(); ready()
                captureStage3("collections-320dp-200-${theme.name}")
            }
            click("Cancel"); click("Discard"); shown("Block Library")
            assertTrue(runBlocking { db.blocks().readAll().isEmpty() }); assertTrue(library.available(pdf)); assertTrue(library.available(audio))
        } finally {
            device.executeShellCommand("wm size reset"); device.executeShellCommand("settings put system font_scale $originalScale")
            ThemePreferences(app).save(ThemeChoice.LIGHT)
        }
    }
}
