package com.mrayover.practicelogger

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class StandaloneTranscribeTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val device get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private fun click(text: String) = compose.onNodeWithText(text).scrollIfNeeded().performClick()
    private fun tag(name: String) = compose.onNodeWithTag(name).scrollIfNeeded()
    private fun shown(text: String) = compose.waitUntil(15000) {
        compose.onAllNodesWithText(text).fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()
    }
    private fun scroll(index: Int) = compose.onNodeWithTag("standalone-transcribe").performScrollToIndex(index)
    private fun screenshot(name: String) {
        val folder = File(compose.activity.getExternalFilesDir(null), "t1-native").apply { mkdirs() }
        device.takeScreenshot(File(folder, "$name.png"))
    }

    @Test fun practiceChoicesPreserveDraftAndStartSavedRoutine() {
        val db = BlockDatabase.appInstance(compose.activity.application)
        val (block, routine) = runBlocking {
            val block = BlockRepository(db).save(null, BlockDraft("T3A fixture", "Listen", "2"))
            val routine = RoutineRepository(db).save(null, "T3A routine", listOf(RoutineSelection(block, 180000)))
            block to routine
        }
        openHomePractice(compose)
        compose.onNodeWithText("Continue Session").assertDoesNotExist()
        click("Add Blocks")
        click("Add T3A fixture")
        shown("1. T3A fixture")
        val draft = runBlocking { db.sessions().readAll().single { it.session.status == "DRAFT" } }
        assertEquals(block, draft.ordered.single().sourceBlockId)
        device.pressBack()
        tag("practice-now-title").assertIsDisplayed()
        click("Add Blocks")
        shown("1. T3A fixture")
        device.pressBack()
        click("Routines")
        tag("start-routine-$routine").performClick()
        shown("T3A fixture")
        tag("practice-timer").assertTextEquals("3:00")
        leavePracticeForLater(compose)
        openHomePractice(compose)
        shown("Continue Session")
        continueFirstSession(compose)
        tag("practice-timer").assertTextEquals("3:00")
        leavePracticeForLater(compose)
        assertEquals(draft, runBlocking { db.sessions().find(draft.session.id) })
    }

    @Test fun standaloneUsesCanonicalPlaybackAndMasterMarkers() {
        val app = compose.activity.application
        val db = BlockDatabase.appInstance(app)
        val initialSessions = runBlocking { db.sessions().readAll() }
        openHomePractice(compose)
        click("Transcribe")
        click("Choose Audio")
        val library = MediaLibraryRepository(db, File(app.filesDir, "media-library"))
        val source = runBlocking {
            library.import("T3A reference.wav", null, role = MediaRole.AUDIO) {
                l1Fixture("audio/backing.wav").inputStream()
            }
        }
        val picker = compose.onNodeWithTag("media-library-list")
        compose.waitUntil(15000) { runCatching { picker.performScrollToKey(source.id); true }.getOrDefault(false) }
        tag("media-choose-${source.id}").performClick()
        val vm = ViewModelProvider(compose.activity)[LibraryAudioViewModel::class.java]
        val player = vm.controller
        compose.waitUntil(15000) { player.state.value.durationMs > 0 && !player.state.value.preparing && vm.waveform != null }
        assertEquals(source.id, player.state.value.mediaId)
        repeat(5) { tag("audio-slower").performClick() }
        tag("audio-speed").assertTextEquals("75%")
        click("Zoom in")
        click("Play selection")
        compose.waitUntil(10000) { player.state.value.playing }
        click("Stop")
        tag("audio-repeat-range").performClick()
        tag("audio-play").performClick()
        compose.waitUntil(10000) { player.state.value.playing && player.state.value.repeatRange }
        scroll(1)
        tag("transcribe-piano").performClick()
        val keys = tag("piano-keyboard")
        keys.performTouchInput { down(Offset(width * .025f, height * .2f)) }
        assertTrue(player.state.value.playing)
        keys.performTouchInput { up() }
        screenshot("t3a-reference-piano")
        tag("transcribe-piano").performClick()
        assertTrue(player.state.value.playing)
        scroll(0)
        tag("audio-play").performClick()
        assertFalse(player.state.value.playing)

        scroll(2)
        tag("audio-add-point").performClick()
        tag("marker-label").performTextInput("Standalone cue")
        click("Save marker")
        compose.waitUntil(10000) { runBlocking { db.markers().read(source.id).any { it.label == "Standalone cue" } } }
        val marker = runBlocking { db.markers().read(source.id).single { it.label == "Standalone cue" } }
        scroll(3)
        click("Edit")
        tag("marker-label").performTextReplacement("Revised cue")
        click("Save marker")
        compose.waitUntil(10000) { runBlocking { db.markers().find(marker.id)?.label == "Revised cue" } }
        scroll(3)
        click("Delete")
        click("Delete marker")
        compose.waitUntil(10000) { runBlocking { db.markers().find(marker.id) == null } }

        scroll(0)
        click("Change Audio")
        click("Cancel selection")
        assertEquals(source.id, player.state.value.mediaId)
        tag("audio-play").performClick()
        compose.runOnIdle { compose.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
        compose.waitUntil(15000) { compose.activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE }
        compose.waitUntil(15000) { player.state.value.mediaId == source.id && !player.state.value.preparing }
        assertFalse(player.state.value.playing)
        scroll(0)
        screenshot("t3a-landscape")
        tag("audio-play").performClick()
        device.pressBack()
        tag("practice-now-title").assertIsDisplayed()
        assertNull(player.state.value.mediaId)
        assertEquals(initialSessions, runBlocking { db.sessions().readAll() })
    }
}
