package com.mrayover.practicelogger

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class TranscribeWorkflowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun tag(name: String) = compose.onNodeWithTag(name).scrollIfNeeded().assertIsDisplayed()
    private fun click(label: String) = compose.onNodeWithText(label).scrollIfNeeded().assertIsDisplayed().performClick()
    private fun shown(name: String) = compose.waitUntil(15000) {
        compose.onAllNodesWithTag(name).fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()
    }
    private fun screenshot(name: String) {
        val dir = File(compose.activity.getExternalFilesDir(null), "t1-native").apply { mkdirs() }
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).takeScreenshot(File(dir, "$name.png"))
    }

    @Test fun canonicalPhrasePracticeUsesExistingTransport() {
        val app = compose.activity.application
        val db = BlockDatabase.appInstance(app)
        val media = ViewModelProvider(compose.activity)[ImageReferenceViewModel::class.java]
        val practice = ViewModelProvider(compose.activity)[PracticeViewModel::class.java]
        val (block, other, session) = runBlocking {
            val repo = BlockRepository(db)
            val first = repo.save(null, BlockDraft("Transcribe one", "Listen", "5", "120"))
            val second = repo.save(null, BlockDraft("Transcribe two", "Listen", "5", "120"))
            val id = SessionRepository(db).let { it.addBlock(first); it.addBlock(second); it.start() }
            Triple(first, second, id)
        }
        val (audio, replacement, section) = runBlocking {
            val first = media.library.import("Practice reference.wav", null) { l1Fixture("audio/backing.wav").inputStream() }
            val second = media.library.import("Alternate reference.mp3", null) { l1Fixture("audio/backing.mp3").inputStream() }
            for (id in listOf(block, other)) media.associations.link(id, MediaRole.AUDIO, first.id)
            val marker = MasterAudioMarkerRepository(db).save(first.id, null, 500, 2400, "First phrase", "Read-only note")
            Triple(first, second, marker)
        }
        val originals = File(app.filesDir, "media-library").listFiles()!!.map { it.name }.toSet()
        compose.runOnIdle { practice.openSession(session) }
        shown("practice-loop-record")
        click("Start")
        compose.waitUntil(15000) { practice.running && !practice.busy }
        tag("practice-reference-toggle").performClick()
        tag("practice-loop-record").performClick()
        tag("phrase-beats").performTextClearance()
        tag("phrase-beats").performTextInput("6")
        tag("phrase-beats").performImeAction()
        tag("practice-mode-transcribe").performClick()
        shown("audio-waveform")
        val audioVm = ViewModelProvider(compose.activity)["practice-transcribe", LibraryAudioViewModel::class.java]
        val player = audioVm.controller
        compose.waitUntil(15000) { player.state.value.durationMs > 0 && !player.state.value.preparing && audioVm.waveform != null }
        assertEquals(audio.id, player.state.value.mediaId)
        compose.onNodeWithTag("practice-timer").assertIsDisplayed()
        compose.onNodeWithTag("practice-notes-toggle").assertIsDisplayed()
        compose.onNodeWithText("Complete").assertIsDisplayed()
        compose.onNodeWithText("Skip").assertIsDisplayed()
        screenshot("portrait-transport")
        repeat(5) { tag("audio-slower").performClick() }
        tag("audio-speed").assertTextEquals("75%")
        tag("master-jump-${section.id}").performClick()
        assertEquals(500L, player.state.value.aMs)
        assertEquals(2400L, player.state.value.bMs)
        assertFalse(player.state.value.repeatRange)
        tag("transcribe-repeat").performClick()
        tag("audio-play").performClick()
        compose.waitUntil(10000) { player.state.value.playing && player.state.value.positionMs > 650 }
        assertTrue(player.state.value.repeatRange)
        assertTrue(practice.running)
        tag("transcribe-piano").performClick()
        shown("piano-keyboard")
        tag("piano-register").assertTextEquals("C4–E5")
        val dragDistance = with(compose.density) { 38.dp.toPx() }
        tag("piano-range-handle").performTouchInput { swipe(center, center - Offset(dragDistance, 0f)) }
        tag("piano-register").assertTextEquals("C♯4–F5")
        val keyboard = tag("piano-keyboard")
        keyboard.performTouchInput { down(Offset(width * .025f, height * .2f)) }
        tag("piano-notes").assertTextEquals("C♯4")
        assertTrue(player.state.value.playing)
        keyboard.performTouchInput { up() }
        tag("piano-notes").assertTextEquals("—")
        screenshot("portrait-piano")
        tag("transcribe-piano").performClick()
        assertTrue(player.state.value.playing)

        compose.runOnIdle { compose.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
        compose.waitUntil(15000) { compose.activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE }
        shown("audio-play")
        assertFalse(player.state.value.playing)
        tag("audio-play")
        compose.onNodeWithTag("practice-notes-toggle").assertIsDisplayed()
        screenshot("landscape-transport")
        compose.runOnIdle { compose.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT }
        compose.waitUntil(15000) { compose.activity.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT }
        tag("practice-mode-record").performClick()
        tag("phrase-beats").assertTextContains("6")
        tag("practice-mode-transcribe").performClick()
        tag("transcribe-choose-audio").performClick()
        click("Cancel")
        assertEquals(audio.id, player.state.value.mediaId)
        tag("transcribe-choose-audio").performClick()
        click("Choose from Library")
        shown("media-library-list")
        compose.onNodeWithTag("media-library-list").performScrollToNode(hasTestTag("media-choose-${replacement.id}"))
        tag("media-choose-${replacement.id}").performClick()
        compose.waitUntil(15000) { player.state.value.mediaId == replacement.id }
        assertEquals(audio.id, runBlocking { db.blockMedia().find(other, MediaRole.AUDIO.name)?.mediaId })
        assertEquals(originals, File(app.filesDir, "media-library").listFiles()!!.map { it.name }.toSet())
        click("Close")
        shown("practice-transport")
        compose.onNodeWithTag("practice-reference-toggle").assert(SemanticsMatcher.expectValue(
            androidx.compose.ui.semantics.SemanticsProperties.StateDescription, "Expanded"))
        assertFalse(player.state.value.playing)
    }
}
