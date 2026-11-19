package com.mrayover.practicelogger

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.UUID

class UpNextHistoryWorkflowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val app get() = compose.activity.application
    private val db get() = BlockDatabase.appInstance(app)
    private val practice get() = ViewModelProvider(compose.activity)[PracticeViewModel::class.java]
    private val recordings get() = ViewModelProvider(compose.activity)[RecordingViewModel::class.java]

    @Test fun upcomingBlockUsesSavedHistoryAndExistingFreePlayback() {
        val title = "Drop 2 voicings"
        val firstTake = runBlocking {
            insertRecording(null, System.currentTimeMillis() - 7 * 86_400_000L, "Earlier free practice", accessible = true)
        }
        val (session, reviewed, empty) = runBlocking {
            val blocks = BlockRepository(db)
            val first = blocks.save(null, BlockDraft("Warm up", "Play slowly"))
            complete(first, firstTake.createdAt) { BlockReflection(it, 3, focus = "Relax the left hand.") }
            val reviewed = blocks.save(null, BlockDraft(title, "Voice lead"))
            val empty = blocks.save(null, BlockDraft(title, "Use a different fingering"))
            val repo = SessionRepository(db)
            repo.addBlocks(listOf(first, reviewed, empty))
            Triple(repo.start(), reviewed, empty)
        }
        val vm = practice
        val audio = recordings
        compose.waitUntil(15_000) { audio.ready }
        compose.runOnIdle { vm.openSession(session) }
        waitFor("Prep Block")
        compose.onNodeWithText("Block 1 of 3").assertIsDisplayed()
        waitFor("Relax the left hand.")
        compose.onNodeWithText("Security rating · 3 / 5").assertExists()
        waitFor("Earlier free practice")
        assertTrue(compose.onNodeWithText("Last Reflection").fetchSemanticsNode().boundsInRoot.top >=
            compose.onNodeWithText("Prep Block").fetchSemanticsNode().boundsInRoot.bottom)
        screenshot("initial-block-history")
        click("Play recording")
        compose.waitUntil(10_000) { audio.playingId == firstTake.id }
        click("Start Block")
        compose.waitUntil(10_000) { audio.playingId == null }
        assertTrue(recordingFile(firstTake).delete())
        click("Skip")
        val gateLabels = listOf("Block 2 of 3", "Up Next: $title", "5:00 planned", "Start Next Block", "Prep Next Block")
        waitFor("Prep Next Block")
        assertNoHistory()
        compose.onNodeWithText("Last Free Recording").assertDoesNotExist()
        val before = gateLabels.map { compose.onNodeWithText(it).fetchSemanticsNode().boundsInRoot }
        screenshot("up-next-empty")

        val answer = List(9) { "Keep the top voice connected. Listen for an even release on the descending phrase." }
            .joinToString("\n") + "\nTry it again at 76."
        val base = System.currentTimeMillis() - 5 * 86_400_000L
        val reflectionTime = base + 86_400_000L
        val ratingTime = reflectionTime + 86_400_000L
        val take = runBlocking {
            complete(reviewed, base) { BlockReflection(it, 2, focus = "Older reflection") }
            complete(reviewed, reflectionTime) { BlockReflection(it, focus = answer, revisit = "The bridge.") }
            complete(reviewed, ratingTime) { BlockReflection(it, 4) }
            complete(reviewed, ratingTime + 86_400_000L) { null }
            insertRecording(null, base, "Older free recording", accessible = true)
            val playable = insertRecording(null, reflectionTime, "Latest accessible recording", accessible = true)
            insertRecording(null, ratingTime, "Missing standalone file", accessible = false)
            insertRecording(empty, ratingTime + 1, "Newer Block recording", accessible = true)
            playable
        }

        waitFor("Last Reflection")
        waitFor("Last Session Ratings")
        waitFor("Last Free Recording")
        assertEquals(before, gateLabels.map { compose.onNodeWithText(it).fetchSemanticsNode().boundsInRoot })
        assertTrue(compose.onNodeWithText("Last Reflection").fetchSemanticsNode().boundsInRoot.top >= before.last().bottom)
        screenshot("up-next-history")
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText(answer).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertTrue(layouts.single().lineCount > 10)
        assertFalse(layouts.single().hasVisualOverflow)
        compose.onNodeWithText("The bridge.").assertExists()
        compose.onNodeWithText("Older reflection").assertDoesNotExist()
        // Each section shows its own completion time, independent of the recording date.
        compose.onAllNodesWithText(sessionDate(reflectionTime)).assertCountEquals(1)
        compose.onNodeWithText(sessionDate(ratingTime)).assertExists()
        compose.onNodeWithText("Security rating · 4 / 5").assertExists()
        compose.onNodeWithText("Latest accessible recording").assertExists()
        compose.onNodeWithText("${sessionDate(take.createdAt)} · 0:08").assertExists()
        compose.onNodeWithText("Newer Block recording").assertDoesNotExist()
        compose.onNodeWithText("Missing standalone file").assertDoesNotExist()
        click("Play recording")
        compose.waitUntil(10_000) { audio.playingId == take.id }
        click("Stop")
        compose.waitUntil(10_000) { audio.playingId == null }

        // The row stays in Room when the file disappears. Retry falls back to the next accessible take.
        assertTrue(recordingFile(take).delete())
        click("Play recording")
        waitFor("Older free recording")
        compose.onNodeWithText("Latest accessible recording").assertDoesNotExist()
        val older = recordings.takes.value.first { it.label == "Older free recording" }
        click("Play recording")
        compose.waitUntil(10_000) { audio.playingId == older.id }
        click("Prep Next Block")
        waitFor("Start Practice")
        compose.runOnIdle { assertTrue(vm.preparing); assertNull(audio.playingId) }
        assertTrue(recordingFile(older).delete())
        click("Skip")

        waitFor("Block 3 of 3")
        compose.onNodeWithText("Up Next: $title").assertIsDisplayed()
        assertNoHistory()
        compose.onNodeWithText("Last Free Recording").assertDoesNotExist()
        click("Start Next Block")
        waitFor("Pause")
        compose.runOnIdle { assertTrue(vm.running); assertFalse(vm.preparing) }
    }

    private suspend fun complete(block: String, time: Long, review: (String) -> BlockReflection?) {
        val repository = SessionRepository(db) { time }
        val id = repository.startBlock(block)
        val placement = checkNotNull(db.sessions().find(id)?.current)
        repository.finishBlock(id, "COMPLETED", "An unrelated placement note", placementId = placement.id)
        review(placement.id)?.let { repository.saveBlockReflection(it) }
    }

    private suspend fun insertRecording(owner: String?, created: Long, label: String, accessible: Boolean): BlockTake {
        val id = UUID.randomUUID().toString()
        val take = BlockTake(id, owner, created, durationMs = 8_000, fileName = "$id.wav", label = label)
        if (accessible) {
            val file = recordingFile(take)
            file.parentFile!!.mkdirs()
            PhraseWave.write(file, ShortArray(8 * PhraseSpec.RATE) { if (it % 128 < 64) 256 else -256 })
        }
        db.takes().insert(take)
        return take
    }

    private fun recordingFile(take: BlockTake) = File(app.filesDir, "recordings/${take.fileName}")
    private fun assertNoHistory() {
        compose.onNodeWithText("Last Reflection").assertDoesNotExist()
        compose.onNodeWithText("Last Session Ratings").assertDoesNotExist()
    }
    private fun waitFor(text: String) = compose.waitUntil(15_000) {
        compose.onAllNodesWithText(text).fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()
    }
    private fun click(text: String) {
        waitFor(text)
        compose.onNodeWithText(text).scrollIfNeeded().assertIsDisplayed().performClick()
    }
    private fun screenshot(name: String) {
        val folder = File(app.getExternalFilesDir(null), "screenshots").apply { mkdirs() }
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).takeScreenshot(File(folder, "$name.png"))
    }
}
