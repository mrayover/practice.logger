package com.mrayover.practicelogger

import androidx.compose.ui.test.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import kotlin.math.roundToInt

class MultiReferencePracticeTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val app get() = compose.activity.application
    private val db get() = BlockDatabase.appInstance(app)
    private val images get() = ViewModelProvider(compose.activity)[ImageReferenceViewModel::class.java]
    private val audio get() = ViewModelProvider(compose.activity)["practice-transcribe", LibraryAudioViewModel::class.java]
    private fun tag(name: String) = compose.onNodeWithTag(name).scrollIfNeeded().assertIsDisplayed()
    private fun click(text: String) = compose.onNodeWithText(text).scrollIfNeeded().assertIsDisplayed().performClick()
    private fun ready(id: String) = compose.waitUntil(20_000) {
        audio.controller.state.value.let { it.mediaId == id && !it.preparing && it.durationMs > 0 } && !audio.switching
    }
    private fun seed(): Pair<String, List<MediaAsset>> = runBlocking {
        val block = BlockRepository(db).save(null, BlockDraft("Multiple practice references", "Read and listen", "5", "120"))
        val a = images.library.import("First.wav", null) { l1Fixture("audio/backing.wav").inputStream() }
        val b = images.library.import("Second.mp3", null) { l1Fixture("audio/backing.mp3").inputStream() }
        images.associations.add(block, MediaRole.AUDIO, a.id)
        images.associations.add(block, MediaRole.AUDIO, b.id)
        block to listOf(a, b)
    }

    @Test fun awaitedCheckpointFailureRetryRapidSwitchAndIndependentPausedState() {
        val (block, assets) = seed()
        val (a, b) = assets
        val vm = audio
        val player = vm.controller
        compose.runOnIdle { vm.openBlock(block, a.id, a.durationMs!!) }; ready(a.id)
        val gate = CompletableDeferred<Unit>()
        var selected = false
        compose.runOnIdle {
            vm.beforeCheckpoint = { gate.await(); error("Injected checkpoint failure") }
            player.speed(65); player.range(300, 2200); player.repeatRange(true); player.seek(900)
            vm.openBlock(block, b.id, b.durationMs!!) { selected = true }
            // Repeated requests during the unresolved save cannot steal ownership.
            repeat(10) { vm.openBlock(block, b.id, b.durationMs!!) { selected = true } }
        }
        assertEquals(a.id, player.state.value.mediaId); assertFalse(selected)
        gate.complete(Unit)
        compose.waitUntil(15_000) { !vm.switching && vm.blockAudioError != null }
        assertEquals(a.id, player.state.value.mediaId); assertEquals(65, player.state.value.speedPercent)
        assertFalse(selected); assertFalse(player.state.value.playing)
        compose.runOnIdle { vm.beforeCheckpoint = {}; vm.openBlock(block, b.id, b.durationMs!!) { selected = true } }
        ready(b.id); assertTrue(selected); assertFalse(player.state.value.playing)
        val savedA = runBlocking { checkNotNull(db.blockAudio().state(block, a.id)) }
        assertEquals(65, savedA.speedPercent); assertEquals(300L, savedA.aMs); assertTrue(savedA.repeat)
        compose.runOnIdle { player.speed(90); player.range(1100, 2800); player.seek(1600); vm.openBlock(block, a.id, a.durationMs!!) }
        ready(a.id)
        assertEquals(65, player.state.value.speedPercent); assertEquals(300L, player.state.value.aMs)
        assertTrue(player.state.value.repeatRange); assertFalse(player.state.value.playing)
        compose.runOnIdle { vm.openBlock(block, b.id, b.durationMs!!) }; ready(b.id)
        assertEquals(90, player.state.value.speedPercent); assertEquals(1100L, player.state.value.aMs)
        assertFalse(player.state.value.playing)
        assertEquals(listOf(a.id, b.id), runBlocking { db.blockMedia().list(block, "AUDIO").map { it.mediaId } })
        compose.runOnIdle { vm.close() }
    }

    @Test fun practiceSelectorsRetainPdfPagesZoomAudioAndTimingAcrossRecreation() {
        val (block, assets) = seed()
        val pdfs = runBlocking {
            // Different canonical identities, including equal content, are intentional here.
            val a = images.library.import("First score.pdf", null) { l1Fixture("references/score.pdf").inputStream() }
            val b = images.library.import("Second score.pdf", null) { l1Fixture("references/score.pdf").inputStream() }
            images.associations.add(block, MediaRole.VISUAL, a.id)
            images.associations.add(block, MediaRole.VISUAL, b.id)
            listOf(a, b)
        }
        val visualBytes = java.io.ByteArrayOutputStream().use { output ->
            android.graphics.Bitmap.createBitmap(64, 40, android.graphics.Bitmap.Config.ARGB_8888).let {
                it.eraseColor(android.graphics.Color.GREEN); it.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, output); it.recycle()
            }; output.toByteArray()
        }
        val mixed = runBlocking {
            val image = images.library.import("Practice image.png", "image/png") { visualBytes.inputStream() }
            images.associations.add(block, MediaRole.VISUAL, image.id)
            val legacy = images.repository.import(block) { visualBytes.inputStream() }
            image to legacy
        }
        val practice = ViewModelProvider(compose.activity)[PracticeViewModel::class.java]
        val session = runBlocking { SessionRepository(db).let { it.addBlock(block); it.start() } }
        compose.runOnIdle { practice.openSession(session) }; prepReadyBlock(compose)
        tag("practice-reference-toggle").performClick()
        tag("reference-page").assertTextEquals("Page 1 of 2")
        click("Next page"); tag("reference-page").assertTextEquals("Page 2 of 2")
        tag("reference-viewer").performTouchInput { pinch(Offset(width*.4f,height*.4f), Offset(width*.6f,height*.6f), Offset(width*.15f,height*.15f), Offset(width*.85f,height*.85f)) }
        val zoom = tag("reference-viewer").fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.ContentDescription]
        tag("Visual reference-choose").performClick(); tag("Visual reference-library:${pdfs[1].id}").performClick()
        tag("reference-page").assertTextEquals("Page 1 of 2")
        tag("Visual reference-choose").performClick(); tag("Visual reference-library:${pdfs[0].id}").performClick()
        tag("reference-page").assertTextEquals("Page 2 of 2")
        assertEquals(zoom, tag("reference-viewer").fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.ContentDescription])
        tag("Visual reference-choose").performClick(); tag("Visual reference-library:${mixed.first.id}").performClick()
        tag("reference-viewer").performTouchInput { pinch(Offset(width*.4f,height*.4f), Offset(width*.6f,height*.6f), Offset(width*.15f,height*.15f), Offset(width*.85f,height*.85f)) }
        val imageZoom = tag("reference-viewer").fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.ContentDescription]
        tag("Visual reference-choose").performClick(); tag("Visual reference-legacy:${mixed.second.fileName}").performClick()
        tag("reference-viewer").assertIsDisplayed()
        tag("Visual reference-choose").performClick(); tag("Visual reference-library:${mixed.first.id}").performClick()
        assertEquals(imageZoom, tag("reference-viewer").fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.ContentDescription])
        // Overlay editing after switching must bind to this exact image, not either PDF.
        tag("overlay-tools").performClick(); click("Edit markings")
        tag("overlay-tools").performClick(); click("Highlight")
        tag("overlay-layer").performTouchInput { swipe(Offset(width*.2f,height*.2f), Offset(width*.7f,height*.5f), 400) }
        compose.waitUntil(15_000) { runBlocking { db.overlays().read(block, "library:${mixed.first.id}").size == 1 } }
        assertTrue(runBlocking { db.overlays().read(block, "library:${pdfs[0].id}").isEmpty() })
        click("Done")
        tag("practice-loop-record").performClick()
        tag("practice-mode-transcribe").performClick(); ready(assets[0].id)
        compose.runOnIdle { audio.controller.speed(75) }
        tag("Audio reference-choose").performClick(); tag("Audio reference-${assets[1].id}").performClick(); ready(assets[1].id)
        tag("Audio reference-choose").performClick(); tag("Audio reference-${assets[0].id}").performClick(); ready(assets[0].id)
        tag("audio-speed").assertTextEquals("75%")
        assertFalse(practice.running) // Prep stays Prep; selection never starts measured practice.
        compose.activityRule.scenario.recreate(); ready(assets[0].id)
        tag("audio-speed").assertTextEquals("75%")
        assertFalse(audio.controller.state.value.playing)
        val output = File(compose.activity.getExternalFilesDir(null), "screenshots").apply { mkdirs() }
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).takeScreenshot(File(output, "multi-reference-practice.png"))
        assertEquals(session, runBlocking { db.sessions().find(session)!!.session.id })
    }

    @Test fun referenceRecordingAppendsOnceOnlyAfterSuccessfulKeepAndSelectsPaused() {
        val (block, assets) = seed()
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.executeShellCommand("pm grant ${app.packageName} android.permission.RECORD_AUDIO")
        val practice = ViewModelProvider(compose.activity)[PracticeViewModel::class.java]
        val session = runBlocking { SessionRepository(db).let { it.addBlock(block); it.start() } }
        val takes = runBlocking { db.takes().readAll() }
        compose.runOnIdle { practice.openSession(session) }; prepReadyBlock(compose)
        tag("practice-loop-record").performClick(); tag("practice-mode-transcribe").performClick(); ready(assets[0].id)
        fun record() {
            click("Record Reference"); click("Start Recording")
            compose.waitUntil(20_000) { compose.onAllNodesWithText("Recording · 0 min 1 sec").fetchSemanticsNodes().isNotEmpty() }
            click("Stop and Review")
            compose.waitUntil(20_000) { compose.onAllNodesWithText("Review Reference").fetchSemanticsNodes().isNotEmpty() }
        }
        record(); click("Discard")
        assertEquals(2, runBlocking { db.blockMedia().list(block, "AUDIO").size })
        record()
        val concurrent = runBlocking {
            images.library.import("Concurrent.m4a", null) { l1Fixture("audio/backing.m4a").inputStream() }.also {
                images.associations.add(block, MediaRole.AUDIO, it.id)
            }
        }
        val before = runBlocking { db.media().readAll().map { it.id }.toSet() }
        click("Keep")
        compose.waitUntil(20_000) { compose.onAllNodesWithText("Reference could not be kept.", substring = true).fetchSemanticsNodes().isNotEmpty() }
        assertEquals(before, runBlocking { db.media().readAll().map { it.id }.toSet() })
        assertEquals(assets[0].id, audio.controller.state.value.mediaId)
        // Remove only the concurrently appended link, restoring the reviewed collection for retry.
        runBlocking { db.blockMedia().removeExact(block, "AUDIO", concurrent.id) }
        click("Keep")
        compose.waitUntil(25_000) { runBlocking { db.blockMedia().list(block, "AUDIO").size == 3 } }
        val order = runBlocking { db.blockMedia().list(block, "AUDIO").map { it.mediaId } }
        assertEquals(assets.map { it.id }, order.take(2)); ready(order.last())
        assertFalse(audio.controller.state.value.playing)
        assertEquals(takes, runBlocking { db.takes().readAll() })
        assertEquals(3, runBlocking { db.blockMedia().list(block, "AUDIO").size })
    }

    @Test fun narrowEnlargedSelectorsRemainReachableInLightDarkAndCustomTheme() {
        val (block, assets) = seed()
        runBlocking {
            repeat(2) { i -> images.library.import("Long score filename for reference $i.pdf", null) {
                l1Fixture("references/score.pdf").inputStream()
            }.also { images.associations.add(block, MediaRole.VISUAL, it.id) } }
        }
        val practice = ViewModelProvider(compose.activity)[PracticeViewModel::class.java]
        val session = runBlocking { SessionRepository(db).let { it.addBlock(block); it.start() } }
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val originalScale = device.executeShellCommand("settings get system font_scale").trim().toFloatOrNull() ?: 1f
        val density = app.resources.displayMetrics.density
        compose.runOnIdle { practice.openSession(session) }; prepReadyBlock(compose)
        tag("practice-reference-toggle").performClick()
        try {
            device.executeShellCommand("wm size ${(320 * density).roundToInt()}x${(880 * density).roundToInt()}")
            device.executeShellCommand("settings put system font_scale 2.0")
            for (theme in listOf(ThemeChoice.LIGHT, ThemeChoice.DARK, ThemeChoice.ORCHID)) {
                ThemePreferences(app).save(theme); compose.activityRule.scenario.recreate()
                assertTrue(compose.activity.resources.configuration.screenWidthDp <= 321)
                assertTrue(compose.activity.resources.configuration.fontScale >= 1.99f)
                tag("Visual reference-choose").assertIsEnabled().performClick(); click("Cancel")
                tag("practice-loop-record").performClick(); tag("practice-mode-transcribe").performClick(); ready(assets[0].id)
                tag("Audio reference-choose").assertIsEnabled().performClick(); click("Cancel")
                compose.onNodeWithText("Complete").assertIsDisplayed(); compose.onNodeWithText("Skip").assertIsDisplayed()
                val output = File(compose.activity.getExternalFilesDir(null), "screenshots").apply { mkdirs() }
                device.takeScreenshot(File(output, "multi-reference-320dp-200-${theme.name}.png"))
                tag("practice-reference-toggle").performClick()
            }
        } finally {
            device.executeShellCommand("wm size reset"); device.executeShellCommand("settings put system font_scale $originalScale")
            ThemePreferences(app).save(ThemeChoice.LIGHT)
        }
    }
}
