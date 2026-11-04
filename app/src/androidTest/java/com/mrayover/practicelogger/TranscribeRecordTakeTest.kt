package com.mrayover.practicelogger

import android.Manifest
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class TranscribeRecordTakeTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val app get() = compose.activity.application
    private val db get() = BlockDatabase.appInstance(app)
    private val recordings get() = ViewModelProvider(compose.activity)[RecordingViewModel::class.java]
    private val practice get() = ViewModelProvider(compose.activity)[PracticeViewModel::class.java]
    private val player get() =
        ViewModelProvider(compose.activity)["practice-transcribe", LibraryAudioViewModel::class.java].controller
    private val scenario get() = compose.activityRule.scenario

    private data class Setup(val block: String, val session: String, val audio: MediaAsset, val original: ByteArray)

    private fun openTranscribe(): Setup {
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            .executeShellCommand("pm grant ${app.packageName} ${Manifest.permission.RECORD_AUDIO}")
        val images = ViewModelProvider(compose.activity)[ImageReferenceViewModel::class.java]
        val setup = runBlocking {
            val block = BlockRepository(db).save(null, BlockDraft("Play along", "Listen", "5", "120"))
            val session = SessionRepository(db).let { it.addBlock(block); it.start() }
            val original = l1Fixture("audio/backing.wav")
            val audio = images.library.import("Play along.wav", null) { original.inputStream() }
            images.associations.link(block, MediaRole.AUDIO, audio.id)
            db.blockAudio().save(BlockAudioState(block, audio.id, 500, 75, 500, 2400, true, null))
            MasterAudioMarkerRepository(db).save(audio.id, null, 500, 2400, "Phrase", "Master note")
            Setup(block, session, audio, original)
        }
        compose.runOnIdle { practice.openSession(setup.session) }
        clickTag("practice-loop-record")
        clickTag("practice-mode-transcribe")
        compose.waitUntil(15_000) {
            player.state.value.let { it.mediaId == setup.audio.id && !it.preparing && recordings.ready }
        }
        return setup
    }

    private fun clickTag(tag: String) {
        compose.waitUntil(15_000) {
            compose.onAllNodesWithTag(tag).fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()
        }
        compose.onNodeWithTag(tag).scrollIfNeeded().assertIsDisplayed().performClick()
    }

    private fun waitForAudio() {
        compose.waitUntil(15_000) { recordings.recording && recordings.elapsedMs >= 1_000 }
    }

    private fun waitForReview() {
        compose.waitUntil(15_000) { recordings.reviewTake != null && !recordings.busy }
    }

    @Test fun referenceAndMicrophoneHaveIndependentControlsAndOwnership() {
        val setup = openTranscribe()
        val local = BlockAudioMarker(setup.block, setup.audio.id, "take-test-local",
            900, null, "Local cue", "Local note", null)
        runBlocking { db.blockAudio().save(local) }
        val before = runBlocking { db.takes().readAll() }
        val markers = runBlocking { db.markers().read(setup.audio.id) }
        val hadActivity = runBlocking { db.transcribeActivities().hasEntries() }
        compose.onNodeWithText("Start").performClick()
        compose.waitUntil(15_000) { practice.running && !practice.busy }
        clickTag("audio-play")
        compose.waitUntil(10_000) { player.state.value.playing }
        clickTag("transcribe-record-take")
        assertTrue(player.state.value.playing)
        waitForAudio()
        compose.onNodeWithTag("practice-notes-toggle").assertIsNotEnabled()
        compose.onNodeWithTag("practice-mode-record").assertIsNotEnabled()
        compose.waitUntil(10_000) { player.state.value.positionMs > 1900 }
        compose.waitUntil(10_000) { player.state.value.positionMs < 900 }
        assertTrue(player.state.value.playing)
        clickTag("audio-play")
        assertTrue(recordings.recording)
        compose.runOnIdle { player.seek(1200); player.speed(80) }
        assertEquals(1200L, player.state.value.positionMs)
        assertTrue(recordings.recording)
        compose.onNodeWithText("Stop", substring = false).scrollIfNeeded().performClick()
        assertTrue(recordings.recording)
        assertFalse(player.state.value.playing)
        clickTag("audio-play")
        clickTag("practice-recording-stop")
        waitForReview()
        assertTrue(player.state.value.playing)
        assertEquals(80, player.state.value.speedPercent)
        assertTrue(player.state.value.repeatRange)
        assertEquals(500L, player.state.value.aMs)
        assertEquals(2400L, player.state.value.bMs)
        assertEquals(before, runBlocking { db.takes().readAll() })
        val firstDraft = recordings.reviewTake!!
        clickTag("review-preview")
        compose.waitUntil(10_000) { recordings.playingId == firstDraft.id }
        assertFalse(player.state.value.playing)
        clickTag("review-rerecord")
        waitForAudio()
        assertFalse(player.state.value.playing)
        clickTag("practice-recording-stop")
        waitForReview()
        val replacement = recordings.reviewTake!!
        assertNotEquals(firstDraft.id, replacement.id)
        clickTag("review-save")
        compose.waitUntil(15_000) { recordings.reviewTake == null && !recordings.busy }
        val saved = runBlocking { db.takes().readAll() }.filter { it !in before }.single()
        assertEquals(replacement.id, saved.id)
        assertEquals(setup.block, saved.blockId)
        assertEquals(TakeKind.FREE, saved.kind)
        assertTrue(File(app.filesDir, "recordings/${saved.fileName}").length() > 0)
        assertFalse(player.state.value.playing)
        assertTrue(practice.running)

        clickTag("transcribe-record-take")
        waitForAudio()
        clickTag("practice-recording-stop")
        waitForReview()
        val discarded = recordings.reviewTake!!
        clickTag("review-discard")
        compose.waitUntil(15_000) { recordings.reviewTake == null }
        assertFalse(File(app.filesDir, "recordings/${discarded.fileName}").exists())
        assertEquals(before.size + 1, runBlocking { db.takes().readAll().size })
        compose.onNodeWithText("Pause", substring = false).performClick()
        compose.waitUntil(15_000) { !practice.running && !practice.busy }
        runBlocking {
            assertEquals(hadActivity, db.transcribeActivities().hasEntries())
            val session = db.sessions().readAll().single { it.session.id == setup.session }
            val elapsed = session.current!!.activeMs
            assertTrue(elapsed > 0L)
            assertEquals(elapsed, db.transcribeActivities().observePracticeTime().first()
                .single { it.startedAt == session.session.startedAt }.activeMs)
            assertEquals(setup.audio.id, db.blockMedia().find(setup.block, MediaRole.AUDIO.name)?.mediaId)
            assertEquals(markers, db.markers().read(setup.audio.id))
            assertEquals(local, db.blockAudio().marker(setup.block, setup.audio.id, local.id))
        }
        assertArrayEquals(setup.original, File(app.filesDir, "media-library/${setup.audio.fileName}").readBytes())
    }

    @Test fun interruptionsStopBothStreamsAndKeepReview() {
        openTranscribe()
        val before = runBlocking { db.takes().readAll() }
        clickTag("audio-play")
        clickTag("transcribe-record-take")
        waitForAudio()
        scenario.moveToState(Lifecycle.State.CREATED)
        assertFalse(recordings.recording)
        assertFalse(player.state.value.playing)
        scenario.moveToState(Lifecycle.State.RESUMED)
        waitForReview()
        clickTag("review-discard")
        clickTag("audio-play")
        clickTag("transcribe-record-take")
        waitForAudio()
        scenario.recreate()
        waitForReview()
        assertFalse(recordings.recording)
        assertFalse(player.state.value.playing)
        assertEquals(75, player.state.value.speedPercent)
        assertTrue(player.state.value.repeatRange)
        clickTag("review-discard")

        clickTag("audio-play")
        compose.waitUntil(10_000) { player.state.value.playing }
        clickTag("transcribe-record-take")
        waitForAudio()
        val manager = app.getSystemService(AudioManager::class.java)
        val competing = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build())
            .setOnAudioFocusChangeListener { }.build()
        try {
            compose.runOnIdle {
                assertEquals(AudioManager.AUDIOFOCUS_REQUEST_GRANTED, manager.requestAudioFocus(competing))
            }
            waitForReview()
            assertFalse(player.state.value.playing)
            assertFalse(recordings.recording)
        } finally {
            compose.runOnIdle { manager.abandonAudioFocusRequest(competing) }
        }
        clickTag("review-discard")
        assertFalse(player.state.value.playing)
        assertEquals(before, runBlocking { db.takes().readAll() })
    }
}
