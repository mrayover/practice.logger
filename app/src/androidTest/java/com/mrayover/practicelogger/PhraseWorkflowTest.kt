package com.mrayover.practicelogger

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.regex.Pattern

class PhraseWorkflowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val device get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private fun shown(text: String) { compose.waitUntil(15000) { compose.onAllNodesWithText(text, substring = true).fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty() } }
    private fun click(text: String) {
        // Room/Flow can publish after Compose is idle; wait for the actual enabled control.
        compose.waitUntil(15000) {
            compose.onAllNodes(hasText(text) and isEnabled()).fetchSemanticsNodes(atLeastOneRootRequired = false).size == 1
        }
        val node = compose.onNodeWithText(text)
        try { node.assertIsDisplayed() } catch (_: AssertionError) { node.performScrollTo() }
        node.performClick()
    }
    private fun sheet(text: String) {
        if (text !in setOf("Stop and save take", "Cancel phrase")) compose.onNodeWithTag("take-list").performScrollToNode(hasText(text))
        compose.waitUntil(15000) { compose.onAllNodes(hasText(text) and isEnabled()).fetchSemanticsNodes(atLeastOneRootRequired=false).isNotEmpty() }
        compose.onAllNodesWithText(text).onLast().assertIsDisplayed().assertIsEnabled().performClick()
    }
    @Test fun nativePhraseCountInExactLengthRepeatCancellationExportAndFreeTakeCoexistence() {
        val db = BlockDatabase.open(compose.activity.applicationContext)
        try {
            val previous = runBlocking { db.takes().readAll() }
            assertTrue(previous.count { it.mode == "FREE" } >= 2)
            val block = runBlocking { BlockRepository(db).save(null, BlockDraft("Loop fixture", "Play along", "5", "120")) }
            shown("Practice Now"); click("Practice Now"); click("Add Loop fixture"); click("Start Session"); shown("Ready when you are")
            click("Start"); click("Start click"); click("Recordings"); shown("Block recordings")
            val vm = ViewModelProvider(compose.activity)[RecordingViewModel::class.java]
            val practice = ViewModelProvider(compose.activity)[PracticeViewModel::class.java]
            compose.waitUntil(10000) { vm.ready }
            sheet("Record phrase"); shown("Record one phrase")
            compose.onNodeWithTag("phrase-recording-click").performScrollTo().assertIsOn().performClick().assertIsOff()
            compose.onNodeWithTag("phrase-recording-click").performClick().assertIsOn()
            compose.onNodeWithText("Phrase BPM").performTextReplacement("0")
            compose.onNodeWithText("Start 4-click count-in").assertIsNotEnabled()
            compose.onNodeWithText("Phrase BPM").performTextReplacement("120")
            compose.onNodeWithText("One phrase: 4.00 seconds").assertExists()
            compose.onNodeWithText("Phrase BPM").performImeAction()
            captureStage3("30-phrase-setup")
            click("Start 4-click count-in")
            compose.waitUntil(5000) { vm.recording && vm.countInBeat > 0 }
            compose.runOnIdle { assertFalse(practice.clicking); assertTrue(practice.running) }
            captureStage3("31-phrase-count-in")
            compose.waitUntil(10000) {
                vm.recording && vm.countInBeat == 0 && vm.elapsedMs > 250 && vm.phraseClickEnabled &&
                    !practice.clicking && vm.phraseGuideFramesPlayed() > PhraseSpec(120, 4).frames + 4800
            }
            compose.waitUntil(20000) { !vm.recording && vm.playingId != null }
            compose.runOnIdle { assertFalse(vm.phraseClickEnabled); assertEquals(0L, vm.phraseGuideFramesPlayed()) }
            val first = runBlocking { db.takes().readAll().single { it.blockId == block } }
            val repository = TakeRepository(db, compose.activity.applicationContext)
            assertEquals("LOOP", first.mode); assertEquals(120, first.recordedBpm); assertEquals(8, first.beatCount)
            assertEquals(192000, PhraseWave.read(repository.audioFile(first)).size)
            // Verify the actual native playback counter crosses two full phrase boundaries.
            compose.waitUntil(20000) { vm.loopFramesPlayed() > 2L * PhraseSpec(120, 8).frames }
            compose.runOnIdle { assertEquals(first.id, vm.playingId); practice.changeWorkingBpm(180) }
            assertEquals(120, runBlocking { db.takes().find(first.id) }!!.recordedBpm)
            sheet("Stop loop"); compose.runOnIdle { assertNull(vm.playingId); assertEquals(0L, vm.loopFramesPlayed()) }
            // Exercise count-in/capture at 60 and 180 BPM through the production native path.
            for (bpm in listOf(60, 180)) {
                val guideEnabled = bpm != 60
                compose.runOnIdle { vm.startPhrase(block, PhraseSpec(bpm, 4), guideEnabled) }
                // Check the live capture and native counter in the same poll. Waiting for UI idle
                // afterward can outlast a 1.33-second phrase and observe the cleared final state.
                compose.waitUntil(15000) {
                    vm.recording && vm.countInBeat == 0 && vm.elapsedMs > 250 &&
                        vm.phraseClickEnabled == guideEnabled &&
                        if (guideEnabled) vm.phraseGuideFramesPlayed() > PhraseSpec(bpm, 4).frames
                        else vm.phraseGuideFramesPlayed() == 0L
                }
                compose.waitUntil(25000) { !vm.recording && !vm.busy && vm.playingId != null }
                val take = runBlocking { db.takes().readAll().single { it.blockId == block && it.recordedBpm == bpm } }
                assertEquals(PhraseSpec(bpm, 4).frames, PhraseWave.read(repository.audioFile(take)).size)
                compose.waitUntil(15000) { vm.loopFramesPlayed() > 2L * PhraseSpec(bpm, 4).frames }
                compose.runOnIdle { vm.stopPlayback() }
            }
            compose.runOnIdle { vm.startPhrase(block, PhraseSpec(120, 8)) }
            compose.waitUntil(5000) { vm.recording && vm.countInBeat > 0 }
            compose.runOnIdle { vm.finishCapture() }
            compose.waitUntil(10000) { !vm.phrasing && !vm.busy }
            compose.runOnIdle { assertEquals(0L, vm.phraseGuideFramesPlayed()) }
            assertEquals(3, runBlocking { db.takes().readAll().count { it.blockId == block } })
            // A long capture gives real UI timer actions enough time on slow emulators.
            // The first saved take above still verifies the required 8 beats / 120 BPM exactly.
            compose.runOnIdle { vm.startPhrase(block, PhraseSpec(30, 32)) }
            compose.waitUntil(15000) { vm.recording && vm.countInBeat == 0 && vm.elapsedMs > 250 }
            sheet("Pause timer"); compose.runOnIdle { assertTrue(vm.recording); assertFalse(practice.running) }
            sheet("Resume timer")
            compose.waitUntil(5000) { vm.recording && practice.running }
            compose.runOnIdle {
                println("Background cancellation precondition: elapsed=${vm.elapsedMs}, duration=${vm.phraseDurationMs}")
                assertTrue("Capture must still be active before backgrounding", vm.recording && vm.phrasing)
                assertTrue("Leave cancellation time before automatic phrase completion", vm.elapsedMs < vm.phraseDurationMs - 5000)
            }
            compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
            compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
            compose.waitUntil(10000) { !vm.phrasing && !vm.busy }
            compose.runOnIdle { assertFalse(vm.recording); assertNull(vm.playingId) }
            assertEquals(3, runBlocking { db.takes().readAll().count { it.blockId == block } })
            compose.runOnIdle { vm.play(first) }; compose.waitUntil(10000) { vm.playingId == first.id }
            compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
            compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
            compose.runOnIdle { assertNull(vm.playingId) }
            // Actual WAV picker export; full bytes match, not merely an exported filename.
            compose.onNodeWithTag("take-list").performScrollToNode(hasTestTag("export-" + first.id))
            compose.onNodeWithTag("export-" + first.id).performClick()
            checkNotNull(device.wait(Until.findObject(By.desc("Show roots")), 10000)).click()
            checkNotNull(device.wait(Until.findObject(By.text("Downloads")), 10000))
            device.findObjects(By.text("Downloads")).last().click()
            checkNotNull(device.wait(Until.findObject(By.clazz("android.widget.EditText")), 10000)).text = "practice-logger-phrase.wav"
            checkNotNull(device.wait(Until.findObject(By.text(Pattern.compile("(?i)save"))), 10000)).click()
            shown("Audio exported.")
            val exported = device.executeShellCommand("sha256sum /sdcard/Download/practice-logger-phrase.wav").trim().split(" ")[0]
            val source = java.security.MessageDigest.getInstance("SHA-256").digest(repository.audioFile(first).readBytes()).joinToString("") { "%02x".format(it) }
            assertEquals(source, exported)
            compose.runOnIdle { vm.export(first, android.net.Uri.parse("content://not.a.provider/loop")) }; shown("Export failed.")
            assertTrue(repository.audioFile(first).exists())
            val tag = "rename-" + first.id
            compose.onNodeWithTag("take-list").performScrollToNode(hasTestTag(tag)); compose.onNodeWithTag(tag).performClick()
            compose.onNodeWithText("Take label (optional)").performTextReplacement("Eight beats at 120")
            click("Save take label"); shown("Take renamed.")
            captureStage3("32-saved-loops")
            // Delete one loop with confirmation; others and previous free takes stay byte-identical.
            val disposable = runBlocking { db.takes().readAll().single { it.blockId == block && it.recordedBpm == 60 } }
            compose.onNodeWithTag("take-list").performScrollToNode(hasTestTag("delete-" + disposable.id))
            compose.onNodeWithTag("delete-" + disposable.id).performClick(); click("Keep take")
            compose.onNodeWithTag("delete-" + disposable.id).performClick(); click("Delete take permanently")
            compose.waitUntil(10000) { !vm.busy && vm.takes.value.none { it.id == disposable.id } }
            assertFalse(java.io.File(compose.activity.filesDir, "recordings/" + disposable.fileName).exists())
            assertTrue(runBlocking { db.takes().readAll() }.containsAll(previous))
            sheet("Close recordings"); click("Save for later"); compose.onNodeWithText("Back to Main").performClick(); shown("Practice Now")
            runBlocking { BlockRepository(db).setArchived(block, true) }
        } catch (failure: Throwable) { captureStage3("phrase-failure"); throw failure }
        finally { db.close() }
    }
}

class PhraseReopenTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun click(text: String) {
        // Room/Flow can publish after Compose is idle; wait for the actual enabled control.
        compose.waitUntil(15000) {
            compose.onAllNodes(hasText(text) and isEnabled()).fetchSemanticsNodes(atLeastOneRootRequired = false).size == 1
        }
        val node = compose.onNodeWithText(text)
        try { node.assertIsDisplayed() } catch (_: AssertionError) { node.performScrollTo() }
        node.performClick()
    }
    @Test fun nativeLoopsReopenArchivedBlockPlayAndRecreationStops() {
        val db = BlockDatabase.open(compose.activity.applicationContext)
        try {
            compose.waitUntil(15000) { compose.onAllNodesWithText("Practice Now").fetchSemanticsNodes().isNotEmpty() }
            val block = runBlocking { db.blocks().readAll().single { it.name == "Loop fixture" } }
            val takes = runBlocking { db.takes().readAll().filter { it.blockId == block.id } }
            assertTrue(block.archived); assertEquals(2, takes.size); assertTrue(takes.all { it.mode == "LOOP" })
            val first = takes.single { it.recordedBpm == 120 }; assertEquals("Eight beats at 120", first.label)
            click("Blocks"); click("Archived"); click("Loop fixture"); click("Recordings")
            val vm = ViewModelProvider(compose.activity)[RecordingViewModel::class.java]
            compose.waitUntil(10000) { vm.ready && vm.takes.value.any { it.id == first.id } }
            compose.runOnIdle { assertFalse(vm.recording); assertNull(vm.playingId) }
            compose.onNodeWithTag("take-list").performScrollToNode(hasTestTag("play-" + first.id))
            compose.onNodeWithTag("play-" + first.id).performClick()
            compose.waitUntil(10000) { vm.playingId == first.id && vm.loopFramesPlayed() > 1000 }
            compose.activityRule.scenario.recreate()
            compose.runOnIdle { assertNull(vm.playingId); assertFalse(vm.recording) }
            compose.onNodeWithTag("take-list").performScrollToNode(hasText("Close recordings")); click("Close recordings")
            click("Cancel"); click("Back to Home")
        } finally { db.close() }
    }
}
