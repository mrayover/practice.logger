package com.mrayover.practicelogger

import android.content.pm.ActivityInfo
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.util.UUID

class LibraryWorkspaceTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val inst get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = inst.targetContext
    private fun fixture(name: String) = inst.context.assets.open(name)
    private fun await(message: String, timeout: Long = 15000, predicate: () -> Boolean) {
        val until = System.currentTimeMillis() + timeout
        while (!predicate() && System.currentTimeMillis() < until) Thread.sleep(25)
        assertTrue(message, predicate())
    }
    private fun digest(file: File) = MessageDigest.getInstance("SHA-256").let { digest ->
        file.inputStream().use { input -> val bytes = ByteArray(32768); while (true) { val n = input.read(bytes); if (n < 0) break; digest.update(bytes, 0, n) } }
        digest.digest().toList()
    }
    private fun libraryTag(tag: String): SemanticsNodeInteraction {
        val matcher = hasTestTag(tag)
        try { return compose.onNode(matcher).assertIsDisplayed() } catch (_: AssertionError) { }
        compose.onNodeWithTag("media-library-list").performScrollToNode(matcher)
        return compose.onNode(matcher).assertIsDisplayed()
    }
    private fun shot(name: String) {
        val directory = File(context.getExternalFilesDir(null), "l2-native").apply { mkdirs() }
        UiDevice.getInstance(inst).takeScreenshot(File(directory, "$name.png"))
    }
    @Test fun existingBlockPdfViewerRetainsPageZoomAndDoesNotCreatePracticeData() {
        val db = BlockDatabase.appInstance(context)
        val id = runBlocking {
            BlockRepository(db).save(null, BlockDraft("L2 existing Block viewer", "Read the original"))
        }
        val repository = ImageReferenceRepository(db, File(context.filesDir, "block-images"))
        val reference = runBlocking { repository.import(id) { fixture("references/score.pdf") } }
        val file = File(context.filesDir, "block-images/${reference.fileName}"); val before = digest(file)
        fun counts() = listOf("sessions", "placements", "takes", "goals").associateWith { table ->
            db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM $table").use { it.moveToFirst(); it.getInt(0) }
        }
        val originalCounts = counts()
        compose.waitUntil(15000) { compose.onAllNodesWithTag("home-menu").fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty() }
        compose.onNodeWithText("Blocks").scrollIfNeeded().performClick()
        compose.onNodeWithTag("block-library-list").performScrollToNode(hasText("L2 existing Block viewer"))
        compose.onNodeWithText("L2 existing Block viewer").performClick()
        compose.onNodeWithText("Show reference").performScrollTo().performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithTag("reference-viewer").fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty() }
        compose.onNodeWithTag("reference-viewer").performScrollTo().performTouchInput {
            pinch(Offset(width*.4f,height*.4f),Offset(width*.6f,height*.6f),Offset(width*.15f,height*.15f),Offset(width*.85f,height*.85f))
        }
        compose.onNodeWithText("Reset PDF zoom").performScrollTo().performClick()
        compose.onNodeWithText("Next page").performScrollTo().performClick()
        compose.onNodeWithText("Page 2 of 2").assertExists()
        compose.onNodeWithText("Previous page").performScrollTo().performClick()
        compose.onNodeWithText("Page 1 of 2").assertExists()
        assertEquals(before, digest(file)); assertEquals(originalCounts, counts())
    }
    @Test fun libraryVisualAudioMarkersRotationAndListState() {
        val vm = ViewModelProvider(compose.activity)[MediaLibraryViewModel::class.java]
        compose.waitUntil(15000) { vm.ready }
        val assets = runBlocking {
            listOf(vm.repository.import("L2 Score.pdf", null) { fixture("references/score.pdf") },
                vm.repository.import("L2 Image.png", null) { l1Png().inputStream() },
                vm.repository.import("L2 Audio with a very long musical reference title.wav", null) { fixture("audio/backing.wav") })
        }
        val pdf = assets[0]; val image = assets[1]; val audio = assets[2]
        val originals = assets.associate { it.id to digest(File(context.filesDir, "media-library/${it.fileName}")) }
        compose.waitUntil(15000) { compose.onAllNodesWithTag("home-menu").fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty() }
        compose.onNodeWithText("Library").scrollIfNeeded().performClick()
        libraryTag("media-search").performTextInput("L2")
        libraryTag("media-open-${pdf.id}").performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithTag("reference-viewer").fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty() }
        compose.onNodeWithText("Page 1 of 2").assertIsDisplayed()
        compose.onNodeWithText("Next page").performClick()
        compose.onNodeWithText("Page 2 of 2").assertIsDisplayed()
        compose.onNodeWithText("Next page").assertIsNotEnabled()
        compose.waitUntil(10000) { compose.onAllNodesWithTag("reference-viewer").fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty() }
        compose.onNodeWithTag("reference-viewer").performTouchInput {
            pinch(Offset(width*.4f,height*.4f),Offset(width*.6f,height*.6f),Offset(width*.15f,height*.15f),Offset(width*.85f,height*.85f))
        }
        compose.onNodeWithTag("reference-viewer").performTouchInput { swipe(center, center + Offset(30f, 20f)) }
        compose.onNodeWithText("Reset zoom").performClick(); shot("pdf")
        compose.onNodeWithText("Previous page").performClick()
        compose.onNodeWithText("Back to Library").performClick()
        libraryTag("media-search").assertTextContains("L2")
        libraryTag("media-open-${image.id}").performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithTag("reference-viewer").fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty() }
        compose.onNodeWithText("Back to Library").performClick()
        // Error/retry reads the same canonical original, without replacing it or metadata.
        val file = File(context.filesDir, "media-library/${image.fileName}"); val temporary = File(file.parentFile, "test-held")
        assertTrue(file.renameTo(temporary))
        try {
            libraryTag("media-open-${image.id}").performClick()
            compose.waitUntil(10000) { compose.onAllNodesWithText("Retry preview").fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty() }
            assertTrue(temporary.renameTo(file)); compose.onNodeWithText("Retry preview").performClick()
            compose.waitUntil(10000) { compose.onAllNodesWithTag("reference-viewer").fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty() }
        } finally { if (temporary.exists()) temporary.renameTo(file) }
        compose.onNodeWithText("Back to Library").performClick()
        libraryTag("media-open-${audio.id}").performClick()
        val audioVm = ViewModelProvider(compose.activity)[LibraryAudioViewModel::class.java]
        compose.waitUntil(15000) { audioVm.waveform != null && !audioVm.controller.state.value.preparing }
        compose.onNodeWithTag("audio-edit-range").performClick()
        compose.onNodeWithTag("range-a").performScrollTo().performTextReplacement("0.500")
        compose.onNodeWithTag("range-b").performScrollTo().performTextReplacement("1.000")
        compose.onNodeWithText("Apply range").performClick()
        repeat(5) { compose.onNodeWithTag("audio-slower").performClick() }
        compose.onNodeWithTag("audio-speed").assertTextEquals("75%")
        compose.onNodeWithTag("audio-add-point").performClick()
        compose.onNodeWithTag("marker-label").performScrollTo().performTextInput("L2 Solo")
        compose.onNodeWithTag("marker-note").performScrollTo().performTextInput("Listen to the voicing")
        compose.onNodeWithText("Save marker").performClick()
        compose.waitUntil(10000) { !audioVm.saving }
        compose.onNodeWithTag("audio-add-section").performClick()
        compose.onNodeWithTag("marker-label").performScrollTo().performTextInput("L2 Phrase")
        compose.onNodeWithText("Save marker").performClick()
        val db = BlockDatabase.appInstance(context)
        compose.waitUntil(10000) { runBlocking { db.markers().read(audio.id).size == 2 } }
        val section = runBlocking { db.markers().read(audio.id).single { it.type == "SECTION" } }
        val dense = runBlocking {
            (0 until 30).map { audioVm.markers.save(audio.id, null, 500L + it, null, "Dense marker $it", "") }
        }
        compose.onNodeWithText("Zoom in").performScrollTo().performClick()
        compose.onNodeWithTag("audio-waveform").performScrollTo().performTouchInput { swipe(center, center + Offset(-40f, 0f)) }
        compose.onNodeWithText("Fit track").performScrollTo().performClick()
        compose.onNodeWithTag("master-markers").performScrollToNode(hasTestTag("master-jump-${dense.last().id}"))
        compose.onNodeWithTag("audio-play").assertIsDisplayed()
        compose.onNodeWithTag("audio-edit-range").assertIsDisplayed()
        shot("dense-markers")
        runBlocking { dense.forEach { audioVm.markers.delete(audio.id, it.id) } }
        compose.onNodeWithTag("master-markers").performScrollToNode(hasTestTag("master-jump-${section.id}"))
        compose.onNodeWithTag("master-jump-${section.id}").performClick()
        assertFalse(audioVm.controller.state.value.repeatRange)
        compose.onNodeWithTag("audio-play").performClick()
        compose.waitUntil(10000) { audioVm.controller.state.value.playing }
        shot("audio-portrait")
        compose.runOnIdle { compose.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
        compose.waitUntil(15000) { compose.activity.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE }
        compose.onNodeWithTag("audio-play").assertIsDisplayed(); shot("audio-landscape")
        val rotatedVm = ViewModelProvider(compose.activity)[LibraryAudioViewModel::class.java]
        assertEquals(75, rotatedVm.controller.state.value.speedPercent)
        assertEquals(500L, rotatedVm.controller.state.value.aMs)
        compose.onNodeWithText("Back to Library").performClick()
        assertFalse(rotatedVm.controller.state.value.playing)
        libraryTag("media-search").assertTextContains("L2")
        for (asset in assets) assertEquals(originals[asset.id], digest(File(context.filesDir, "media-library/${asset.fileName}")))
        context.getSharedPreferences("l2-test", 0).edit().putString("audio", audio.id).putString("hash", originals[audio.id].toString()).commit()
    }
    @Test fun realFormatsControllerLoopSpeedSeekEndAndForeground() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, BlockDatabase::class.java).build()
        val dir = File(context.cacheDir, "controller-${UUID.randomUUID()}")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val repo = MediaLibraryRepository(db, dir)
        lateinit var controller: LibraryAudioController
        inst.runOnMainSync { controller = LibraryAudioController(context, repo, scope) }
        try {
            for (ext in listOf("mp3", "m4a", "wav")) {
                val asset = repo.import("Gate.$ext", null) { fixture("audio/backing.$ext") }
                val file = File(dir, asset.fileName); val before = digest(file)
                inst.runOnMainSync { controller.open(asset.id) }
                await("$ext preparation") { !controller.state.value.preparing && controller.state.value.durationMs > 0 }
                assertNull(controller.state.value.error)
                inst.runOnMainSync { controller.range(500, 900); controller.repeatRange(true); controller.speed(50); controller.play() }
                await("$ext playback") { controller.state.value.playing }
                var wraps = 0; var previous = 0L
                val until = System.currentTimeMillis() + 7500
                while (wraps < 3 && System.currentTimeMillis() < until) {
                    val now = controller.state.value.positionMs
                    if (previous > 750 && now < 650) wraps++
                    previous = now; Thread.sleep(25)
                }
                assertTrue("$ext only $wraps short-loop wraps", wraps >= 3)
                inst.runOnMainSync { controller.repeatRange(false); controller.playSelection() }
                await("$ext selected range stops without repeat") { !controller.state.value.playing }
                assertTrue(controller.state.value.positionMs in 800..1100)
                inst.runOnMainSync {
                    assertFalse(controller.range(900, 500)); assertEquals(500L, controller.state.value.aMs)
                    controller.speed(75); assertEquals(500L, controller.state.value.aMs)
                    controller.seek(1500); controller.play()
                }
                await("$ext seek while playing") { controller.state.value.positionMs in 1400..2200 }
                inst.runOnMainSync { controller.pause() }
                assertFalse(controller.state.value.playing)
                inst.runOnMainSync { controller.speed(50) }
                Thread.sleep(150); assertFalse(controller.state.value.playing) // paused speed changes never start audio
                inst.runOnMainSync { controller.play(); controller.setForeground(false) }
                assertFalse(controller.state.value.playing)
                inst.runOnMainSync { controller.setForeground(true) }
                Thread.sleep(150); assertFalse(controller.state.value.playing)
                inst.runOnMainSync { controller.play() }
                await("$ext foreground retry") { controller.state.value.playing }
                inst.runOnMainSync { controller.speed(100); controller.seek(controller.state.value.durationMs - 250) }
                await("$ext end of file") { !controller.state.value.playing }
                inst.runOnMainSync { controller.repeatFile(true); controller.seek(controller.state.value.durationMs - 200); controller.play() }
                await("$ext full-file repeat") { controller.state.value.playing && controller.state.value.positionMs < 600 }
                inst.runOnMainSync { controller.repeatFile(false) }
                if (ext == "wav") {
                    val manager = context.getSystemService(android.media.AudioManager::class.java)
                    val competing = android.media.AudioFocusRequest.Builder(android.media.AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                        .setOnAudioFocusChangeListener { }.build()
                    inst.runOnMainSync { assertEquals(android.media.AudioManager.AUDIOFOCUS_REQUEST_GRANTED, manager.requestAudioFocus(competing)) }
                    await("focus loss stops Library playback") { !controller.state.value.playing }
                    inst.runOnMainSync { manager.abandonAudioFocusRequest(competing) }
                    Thread.sleep(150); assertFalse(controller.state.value.playing)
                }
                inst.runOnMainSync { controller.stop() }
                await("$ext stop position") { controller.state.value.positionMs < 200 }
                inst.runOnMainSync { controller.close() }
                assertEquals(before, digest(file))
            }
            inst.runOnMainSync { controller.open(UUID.randomUUID().toString()) }
            await("missing original error") { controller.state.value.error != null }
        } finally {
            inst.runOnMainSync { controller.dispose() }; scope.cancel(); db.close(); dir.deleteRecursively()
        }
    }
    @Test fun waveformFaithfulnessLongDurationCacheRecoveryAndOriginalIntegrity() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, BlockDatabase::class.java).build()
        val dir = File(context.cacheDir, "wave-${UUID.randomUUID()}")
        val cacheDir = File(context.cacheDir, "peaks-${UUID.randomUUID()}")
        val source = File(context.cacheDir, "long-${UUID.randomUUID()}.wav")
        val rate = 8000; val seconds = 900; val size = rate * seconds * 2
        source.outputStream().buffered().use { output ->
            output.write(ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
                put("RIFF".toByteArray()); putInt(size + 36); put("WAVEfmt ".toByteArray()); putInt(16)
                putShort(1); putShort(1); putInt(rate); putInt(rate*2); putShort(2); putShort(16); put("data".toByteArray()); putInt(size)
            }.array())
            val loud = ByteArray(rate*2) { if (it % 2 == 0) 0 else 64 }
            val silence = ByteArray(rate*2)
            repeat(seconds) { output.write(if (it % 2 == 0) loud else silence) }
        }
        try {
            val repo = MediaLibraryRepository(db, dir)
            val asset = repo.import("Long.wav", null) { source.inputStream() }
            val original = File(dir, asset.fileName); val before = digest(original)
            val cache = AudioWaveformCache(repo, cacheDir)
            val wave = cache.load(asset.id)
            assertEquals(900000L, wave.durationMs)
            assertTrue(wave.peaks.size <= AudioWaveform.MAX_BINS)
            assertEquals(.5f, wave.peaks[100], .001f)
            assertEquals(0f, wave.peaks[300], .001f)
            assertTrue(cacheDir.listFiles()!!.single().length() < 800000)
            cacheDir.listFiles()!!.single().writeBytes(byteArrayOf(0))
            assertArrayEquals(wave.peaks, cache.load(asset.id).peaks, 0f)
            cache.remove(asset.id); assertTrue(cacheDir.listFiles()!!.isEmpty())
            assertArrayEquals(wave.peaks, cache.load(asset.id).peaks, 0f)
            assertEquals(before, digest(original))
            for (ext in listOf("mp3", "m4a")) {
                val encoded = repo.import("Signal.$ext", null) { fixture("audio/backing.$ext") }
                val peaks = cache.load(encoded.id).peaks
                assertTrue("$ext is not decorative/silent", peaks.count { it > .01f } > peaks.size / 2)
            }
        } finally { db.close(); source.delete(); dir.deleteRecursively(); cacheDir.deleteRecursively() }
    }
}
