package com.mrayover.practicelogger

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import java.io.File
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
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

    private fun importFromDownloads(name: String, bytes: ByteArray) {
        val file = File(compose.activity.getExternalFilesDir(null), name).apply { writeBytes(bytes) }
        device.executeShellCommand("mkdir -p /sdcard/Download")
        device.executeShellCommand("cp ${file.absolutePath} /sdcard/Download/$name")
        compose.onNodeWithTag("media-library-list").performScrollToNode(hasTestTag("media-import"))
        tag("media-import").performClick()
        checkNotNull(device.wait(Until.findObject(By.desc("Show roots")), 10000)).click()
        checkNotNull(device.wait(Until.findObject(By.text("Downloads")), 10000))
        device.findObjects(By.text("Downloads")).last().click()
        checkNotNull(device.wait(Until.findObject(By.descStartsWith(name)), 10000)
            ?: device.wait(Until.findObject(By.text(name)), 3000)).click()
    }

    @Test fun stopwatchPausesOnLeaveAndRecreationAndSavesOnce() {
        val db = BlockDatabase.appInstance(compose.activity.application)
        val goalId = runBlocking { GoalRepository(db).save(null, "Transcribe study", "", 37, "") }
        openHomePractice(compose)
        click("Transcribe")
        shown("00:00:00")
        assertNull(runBlocking { db.transcribeActivities().unfinished() })
        click("Start")
        shown("Stop & Save")
        click("No Goal")
        click("Transcribe study")
        compose.waitUntil(15_000) { runBlocking { db.transcribeActivities().unfinished()?.goalId == goalId } }
        val id = runBlocking { db.transcribeActivities().unfinished()!!.id }
        val checkpoint = runBlocking { db.transcribeActivities().find(id)!!.activeMs }
        compose.waitForIdle()
        click("Back to Practice Now")
        tag("practice-now-title").assertIsDisplayed()
        compose.waitUntil(15_000) { runBlocking { db.transcribeActivities().find(id)!!.activeMs > checkpoint } }
        click("Transcribe")
        shown("Resume")
        val paused = runBlocking { db.transcribeActivities().find(id)!! }
        assertTrue(paused.activeMs > 0)
        compose.activityRule.scenario.recreate()
        shown("Resume")
        assertEquals(paused, runBlocking { db.transcribeActivities().find(id) })
        val sql = db.openHelper.writableDatabase
        sql.execSQL("""
            CREATE TRIGGER fail_transcribe_finish BEFORE UPDATE ON transcribe_activities
            WHEN NEW.completedAt IS NOT NULL BEGIN SELECT RAISE(ABORT, 'test save failure'); END
        """.trimIndent())
        try {
            click("Stop & Save")
            shown("Could not save this activity. Check storage and tap Stop & Save to retry.")
            assertEquals(paused, runBlocking { db.transcribeActivities().find(id) })
        } finally {
            sql.execSQL("DROP TRIGGER fail_transcribe_finish")
        }
        click("Stop & Save")
        shown("Activity saved.")
        assertNull(runBlocking { db.transcribeActivities().unfinished() })
        assertEquals(paused.activeMs, runBlocking { db.goals().observeTime().first().single { it.goalId == goalId }.activeMs })
        assertEquals(37, runBlocking { db.goals().find(goalId)!!.goal.progress })
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
        val picker = compose.onNodeWithTag("media-library-list")
        val media = ViewModelProvider(compose.activity)[MediaLibraryViewModel::class.java]
        compose.waitUntil(15000) { media.ready }
        val emptyMessage = hasText("No audio in the Library yet.", substring = true)
        picker.performScrollToNode(emptyMessage)
        compose.onNode(emptyMessage).assertIsDisplayed()
        val bytes = l1Fixture("audio/backing.wav")
        importFromDownloads("T3A-reference.wav", bytes)
        compose.waitUntil(25000) { !media.busy && media.assets.value.any { it.originalName == "T3A-reference.wav" } }
        val source = media.assets.value.single { it.originalName == "T3A-reference.wav" }
        assertArrayEquals(bytes, File(app.filesDir, "media-library/${source.fileName}").readBytes())
        picker.performScrollToKey(source.id)
        tag("media-choose-${source.id}").performClick()
        val vm = ViewModelProvider(compose.activity)[LibraryAudioViewModel::class.java]
        val player = vm.controller
        compose.waitUntil(15000) { player.state.value.durationMs > 0 && !player.state.value.preparing && vm.waveform != null }
        assertEquals(source.id, player.state.value.mediaId)
        val timer = ViewModelProvider(compose.activity)[TranscribeViewModel::class.java]
        repeat(5) { tag("audio-slower").performClick() }
        tag("audio-speed").assertTextEquals("75%")
        repeat(5) { tag("audio-slower").performClick() }
        tag("audio-speed").assertTextEquals("50%")
        click("Zoom in")
        click("Play selection")
        compose.waitUntil(10000) { player.state.value.playing }
        click("Stop")
        tag("audio-repeat-range").performClick()
        tag("audio-play").performClick()
        compose.waitUntil(10000) { player.state.value.playing && player.state.value.repeatRange }
        assertEquals(0L, timer.elapsedMs)
        assertFalse(timer.hasActivity)
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
        scroll(1)
        tag("transcribe-piano").performClick()
        compose.onNodeWithContentDescription("Next octave").scrollIfNeeded().performClick()
        val pausedKeys = tag("piano-keyboard")
        pausedKeys.performTouchInput { down(Offset(width * .025f, height * .2f)) }
        tag("piano-notes").assertTextEquals("C5")
        assertFalse(player.state.value.playing)
        pausedKeys.performTouchInput { up() }
        tag("transcribe-piano").performClick()

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
        importFromDownloads("T3A-invalid.wav", "broken WAV".toByteArray())
        compose.waitUntil(25000) { !media.busy && media.message?.startsWith("Imported") == false }
        picker.performScrollToNode(hasTestTag("media-message"))
        assertTrue(media.assets.value.none { it.originalName == "T3A-invalid.wav" })
        picker.performScrollToNode(hasText("Cancel selection"))
        click("Cancel selection")
        assertEquals(source.id, player.state.value.mediaId)
        tag("audio-play").performClick()
        compose.runOnIdle { compose.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
        compose.waitUntil(15000) { compose.activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE }
        compose.waitUntil(15000) { player.state.value.mediaId == source.id && !player.state.value.preparing }
        assertFalse(player.state.value.playing)
        scroll(0)
        screenshot("t3a-landscape")
        val alternate = runBlocking {
            media.repository.import("T3A alternate.mp3", null) { l1Fixture("audio/backing.mp3").inputStream() }
        }
        click("Change Audio")
        compose.waitUntil(15000) { media.assets.value.any { it.id == alternate.id } }
        picker.performScrollToKey(alternate.id)
        tag("media-choose-${alternate.id}").performClick()
        compose.waitUntil(15000) { player.state.value.mediaId == alternate.id && !player.state.value.preparing }
        assertFalse(player.state.value.playing)
        scroll(0)
        tag("audio-play").performClick()
        device.pressBack()
        tag("practice-now-title").assertIsDisplayed()
        assertNull(player.state.value.mediaId)
        assertEquals(initialSessions, runBlocking { db.sessions().readAll() })
        click("Transcribe")
        click("Choose Audio")
        assertNull(player.state.value.mediaId)
    }
}
