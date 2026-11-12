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
}
