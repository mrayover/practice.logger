package com.mrayover.practicelogger

import android.Manifest
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class PracticeLifecycleTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val app get() = compose.activity.application
    private val db get() = BlockDatabase.appInstance(app)
    private val practice get() = ViewModelProvider(compose.activity)[PracticeViewModel::class.java]
    private val recordings get() = ViewModelProvider(compose.activity)[RecordingViewModel::class.java]

    private fun click(text: String) = compose.onNodeWithText(text).scrollIfNeeded().assertIsDisplayed().performClick()
    private fun tag(tag: String) = compose.onNodeWithTag(tag).scrollIfNeeded().assertIsDisplayed()
    private fun shown(text: String) = compose.waitUntil(15000) {
        compose.onAllNodesWithText(text).fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()
    }
    private fun current(id: String) = runBlocking { db.sessions().find(id)!!.current!! }
    private fun screenshot(name: String) {
        val dir = File(app.getExternalFilesDir(null), "screenshots").apply { mkdirs() }
        assertTrue(UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).takeScreenshot(File(dir, "$name.png")))
    }

    @Test fun deliberateGatesAndForegroundPrepCheckpoints() {
        val sessionId = runBlocking {
            val blocks = BlockRepository(db)
            val first = blocks.save(null, BlockDraft("First lifecycle Block", "Play a phrase"))
            val second = blocks.save(null, BlockDraft("Second lifecycle Block", "Listen"))
            val third = blocks.save(null, BlockDraft("Third lifecycle Block", "Prepare for later"))
            val sessions = SessionRepository(db)
            sessions.addBlocks(listOf(first, second, third))
            sessions.start()
        }
        val first = current(sessionId)
        compose.runOnIdle { practice.openSession(sessionId) }
        shown("Start Block")
        assertReady("Block 1 of 3", "Start Block", "Prep Block")
        screenshot("p1-ready")
        Thread.sleep(400)
        assertEquals(first, current(sessionId))
        click("Start Block")
        shown("Pause")
        compose.waitUntil(10000) { current(sessionId).activeMs > 0L }
        assertEquals(0L, current(sessionId).prepMs)
        click("Pause")
        shown("Resume")
        click("Resume")
        shown("Pause")
        click("Complete")
        shown("Up Next: Second lifecycle Block")
        assertReady("Block 2 of 3", "Start Next Block", "Prep Next Block")
        Thread.sleep(400)
        assertEquals(0L, current(sessionId).activeMs)
        assertEquals(0L, current(sessionId).prepMs)
        screenshot("p1-up-next")
        click("Prep Next Block")
        shown("Start Practice")
        compose.waitUntil(10000) { current(sessionId).prepMs >= 250L }
        assertEquals(0L, current(sessionId).activeMs)
        val preparedId = current(sessionId).id
        assertNull(runBlocking { db.goalAttribution().snapshot(preparedId) })
        screenshot("p1-prep")
        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        Thread.sleep(600)
        val background = current(sessionId)
        Thread.sleep(600)
        assertEquals(background, current(sessionId))
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        compose.activityRule.scenario.recreate()
        shown("Start Practice")
        assertEquals(0L, current(sessionId).activeMs)
        click("Skip")
        shown("Up Next: Third lifecycle Block")
        click("Prep Next Block")
        shown("Start Practice")
        compose.waitUntil(10000) { current(sessionId).prepMs >= 250L }
        leavePracticeForLater(compose)
        shown("Practice Now")
        val bundle = runBlocking { db.sessions().find(sessionId)!! }
        assertEquals(listOf("COMPLETED", "SKIPPED", "PENDING"), bundle.ordered.map { it.outcome })
        assertEquals(first.id, bundle.ordered.first().id)
        assertEquals(0L, bundle.ordered[1].activeMs)
        assertTrue(bundle.ordered[1].prepMs > 0)
        assertEquals(0L, bundle.current!!.activeMs)
    }

    private fun assertReady(position: String, start: String, prep: String) {
        compose.onNodeWithText(position).scrollIfNeeded().assertIsDisplayed()
        tag("practice-ready-start").assertTextContains(start)
        tag("practice-ready-prep").assertTextContains(prep)
        compose.onNodeWithTag("practice-timer").assertDoesNotExist()
    }

    @Test fun prepPlaybackRecordingAndNotesStayAvailableBeforeStart() {
        val images = ViewModelProvider(compose.activity)[ImageReferenceViewModel::class.java]
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.executeShellCommand("pm grant ${app.packageName} ${Manifest.permission.RECORD_AUDIO}")
        val sessionId = runBlocking {
            val block = BlockRepository(db).save(null, BlockDraft("Prep tools fixture", "Listen, record, then start"))
            val wave = File.createTempFile("prep-reference", ".wav", app.cacheDir)
            try {
                PhraseWave.write(wave, ShortArray(PhraseSpec.RATE * 5) { if (it % 120 < 60) 500 else -500 })
                val audio = images.library.import("Prep reference.wav", null, open = wave::inputStream)
                images.associations.link(block, MediaRole.AUDIO, audio.id)
            } finally {
                wave.delete()
            }
            SessionRepository(db).startBlock(block)
        }
        compose.runOnIdle { practice.openSession(sessionId) }
        prepReadyBlock(compose)
        tag("practice-loop-record").performClick()
        tag("practice-mode-transcribe").performClick()
        val player = ViewModelProvider(compose.activity)["practice-transcribe", LibraryAudioViewModel::class.java].controller
        compose.waitUntil(15000) { player.state.value.durationMs > 0 && !player.state.value.preparing }
        tag("audio-play").performClick()
        compose.waitUntil(10000) { player.state.value.playing && player.state.value.positionMs > 0L }
        assertEquals(0L, current(sessionId).activeMs)
        click("Close")
        compose.waitUntil(15000) { recordings.ready }
        tag("practice-free-record").performClick()
        compose.waitUntil(15000) { recordings.recording && recordings.elapsedMs >= 1000L }
        compose.onNodeWithText("Complete").assertIsNotEnabled()
        compose.onNodeWithText("Skip").assertIsNotEnabled()
        compose.onNodeWithTag("home-brand", useUnmergedTree = true).performClick()
        shown("Leave current session?")
        compose.onNodeWithText("Back to Main").assertIsNotEnabled()
        click("Continue")
        tag("practice-recording-stop").performClick()
        shown("Discard")
        click("Discard")
        compose.waitUntil(15000) { recordings.reviewTake == null && !recordings.busy }
        revealPracticeNotes(compose)
        compose.onNodeWithTag("practice-new-note").performScrollTo().performTextInput("Unfinished prep note")
        click("Complete")
        shown("Unsaved note text")
        click("Keep editing")
        assertEquals(0L, current(sessionId).activeMs)
        click("Start Practice")
        shown("Pause")
        compose.waitUntil(10000) { current(sessionId).activeMs > 0L }
        val checkpoint = current(sessionId)
        assertTrue(checkpoint.prepMs > 0)
        assertNotNull(runBlocking { db.goalAttribution().snapshot(checkpoint.id) })
        Thread.sleep(500)
        assertEquals(checkpoint.prepMs, current(sessionId).prepMs)
        click("Pause")
        click("Complete")
        shown("Unsaved note text")
        click("Discard draft and continue")
        shown("Session Summary")
    }
}

class PracticeLifecycleReopenTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun preparedBlockReopensReadyWithoutAccruingTime() {
        val db = BlockDatabase.appInstance(compose.activity.application)
        val before = runBlocking {
            db.sessions().readAll().single { it.current?.name == "Third lifecycle Block" }
        }
        assertTrue(before.current!!.prepMs > 0L)
        openHomePractice(compose)
        compose.onNodeWithText("In Progress").scrollIfNeeded().performClick()
        compose.onNodeWithTag("continue-session-${before.session.id}").scrollIfNeeded().performClick()
        compose.onNodeWithTag("practice-ready-start").scrollIfNeeded().assertIsDisplayed()
        compose.onNodeWithTag("practice-ready-prep").scrollIfNeeded().assertIsDisplayed()
        compose.onNodeWithTag("practice-timer").assertDoesNotExist()
        Thread.sleep(600)
        assertEquals(before, runBlocking { db.sessions().find(before.session.id) })
    }
}
