package com.mrayover.practicelogger

import android.Manifest
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class TakeReviewWorkflowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val device get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val audio get() = ViewModelProvider(compose.activity)[RecordingViewModel::class.java]
    private fun shown(text: String) = compose.waitUntil(15000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    private fun click(text: String) { shown(text);compose.onNodeWithText(text).scrollIfNeeded().assertIsDisplayed().performClick() }
    private fun tag(name: String) = compose.onNodeWithTag(name).scrollIfNeeded().assertIsDisplayed()
    private fun review() { compose.waitUntil(15000) { !audio.busy && !audio.recording && audio.reviewTake != null && compose.onAllNodesWithTag("take-review").fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty() };tag("take-review").assertExists() }
    private fun ready() { compose.waitUntil(15000) { audio.ready && !audio.busy };device.executeShellCommand("pm grant ${compose.activity.packageName} ${Manifest.permission.RECORD_AUDIO}") }
    private fun shot(name: String) { compose.waitForIdle(); val dir=File(compose.activity.getExternalFilesDir(null),"take-review");dir.mkdirs();device.takeScreenshot(File(dir,"$name.png")) }
    private fun free(stop: String = "Stop and review take") {
        click("Record take");compose.waitUntil(15000) { audio.recording && audio.elapsedMs >= 1500 };click(stop);review()
    }
    @Test fun focusReviewPreservesNotesReferenceTimerAndExistingTakes() {
        val app=compose.activity.application; val db=BlockDatabase.open(app)
        val practice=ViewModelProvider(compose.activity)[PracticeViewModel::class.java]
        val notes=ViewModelProvider(compose.activity)[BlockNotesViewModel::class.java]
        try {
            val (block,session)=runBlocking {
                val b=BlockRepository(db).save(null,BlockDraft("P1 focus review","Keep instructions","5","120"))
                ImageReferenceRepository(db,File(app.filesDir,"block-images")).import(b) { InstrumentationRegistry.getInstrumentation().context.assets.open("references/score.pdf") }
                val repo=SessionRepository(db);repo.addBlock(b);b to repo.start()
            }
            shown("Practice Now");ready();compose.runOnIdle { practice.openSession(session) };shown("Practice Description")
            val prior=runBlocking { db.takes().readAll() }
            tag("practice-reference-toggle").performClick();shown("Page 1 of 2");click("Next page");shown("Page 2 of 2")
            click("Start");click("Notes")
            tag("practice-new-note").performTextReplacement("Keep review draft")
            compose.runOnIdle { compose.activity.currentFocus?.clearFocus();compose.activity.getSystemService(android.view.inputmethod.InputMethodManager::class.java).hideSoftInputFromWindow(compose.activity.window.decorView.windowToken,0) }
            tag("practice-free-record").performClick();compose.waitUntil(15000) { audio.recording && audio.elapsedMs >= 1500 }
            tag("practice-free-record").performClick();review();shot("focus-review")
            assertEquals(prior,runBlocking { db.takes().readAll() });compose.runOnIdle { assertTrue(practice.running) }
            tag("review-preview").performClick();compose.waitUntil(10000) { audio.playingId == audio.reviewTake?.id }
            tag("review-preview").performClick();compose.runOnIdle { assertNull(audio.playingId) }
            tag("review-rerecord").performClick();compose.waitUntil(15000) { audio.recording && audio.elapsedMs >= 1500 }
            tag("practice-free-record").performClick();review();tag("review-save").performClick()
            compose.waitUntil(15000) { !audio.busy && audio.reviewTake == null && audio.takes.value.count { it.blockId == block } == 1 }
            tag("practice-new-note").assertTextContains("Keep review draft")
            compose.runOnIdle { notes.edit(practice.catalog.value.active!!.current!!.id, "") }
            click("Return to Practice");shown("Page 2 of 2")
            tag("practice-free-record").performClick();compose.waitUntil(10000) { audio.elapsedMs >= 1500 }
            tag("practice-free-record").performClick();review();tag("review-discard").performClick()
            assertEquals(1,runBlocking { db.takes().readAll().count { it.blockId == block } })
            compose.runOnIdle { assertTrue(practice.running) };shot("focus-restored")
            device.pressBack();click("Back to Main");shown("Practice Now")
        } finally { compose.runOnIdle { audio.stopPlayback();practice.stopClick() };db.close() }
    }
    @Test fun savedBlockFreeAndCompletedPhraseReviewAndIncompleteCancel() {
        val db=BlockDatabase.open(compose.activity.application)
        try {
            val block=runBlocking { BlockRepository(db).save(null,BlockDraft("P1 saved Block","Record")) }
            shown("Practice Now");ready();click("Blocks");click("P1 saved Block");click("Recordings");shown("Block recordings")
            val before=runBlocking { db.takes().readAll() }
            free();tag("review-save").performClick();compose.waitUntil(15000) { !audio.busy && audio.reviewTake == null }
            assertEquals(before.size+1,runBlocking { db.takes().readAll().size })
            compose.onNodeWithTag("take-list").performScrollToNode(hasText("Record phrase"));click("Record phrase")
            compose.onNodeWithText("Phrase BPM").performTextReplacement("240");click("4 beats");click("Start count-in")
            review();val loop=audio.reviewTake!!;assertEquals("LOOP",loop.mode)
            assertNull(runBlocking { db.takes().find(loop.id) })
            compose.waitUntil(10000) { audio.playingId == loop.id };compose.waitUntil(10000) { audio.loopFramesPlayed() > 0 }
            shot("loop-review");tag("review-save").performClick();compose.waitUntil(15000) { !audio.busy && audio.reviewTake == null }
            assertNotNull(runBlocking { db.takes().find(loop.id) });assertNull(audio.playingId)
            compose.onNodeWithTag("take-list").performScrollToNode(hasText("Record phrase"));click("Record phrase");click("Start count-in")
            compose.waitUntil(10000) { audio.phrasing && audio.countInBeat > 0 };click("Cancel phrase")
            compose.waitUntil(15000) { !audio.recording && !audio.busy && !audio.phrasing }
            assertNull(audio.reviewTake);assertEquals(2,runBlocking { db.takes().readAll().count { it.blockId == block } })
            compose.onNodeWithTag("take-list").performScrollToNode(hasText("Close recordings"));click("Close recordings")
        } finally { compose.runOnIdle { audio.stopPlayback() };db.close() }
    }
    @Test fun standaloneFailedSaveRetryBackgroundAndDeliberateDiscard() {
        val app=compose.activity.application;val db=BlockDatabase.open(app)
        try {
            shown("Practice Now");ready();val blocks=runBlocking { db.blocks().readAll() };val sessions=runBlocking { BackupRepository(db).snapshot().sessions }
            val old=runBlocking { db.takes().readAll() };val oldBytes=old.associate { it.id to TakeRepository(db,app).audioFile(it).readBytes() }
            click("Recording");click("Free recording");compose.waitUntil(15000) { audio.recording && audio.elapsedMs >= 1500 }
            click("Stop and review take");review();val pending=audio.reviewTake!!
            tag("review-preview").performClick();compose.waitUntil(10000) { audio.playingId == pending.id }
            device.pressHome();compose.waitUntil(10000) { audio.playingId == null && !audio.recording }
            device.executeShellCommand("am start -W -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -f 0x10200000 -n ${app.packageName}/com.mrayover.practicelogger.MainActivity");review()
            assertNull(audio.playingId);assertEquals(old,runBlocking { db.takes().readAll() })
            db.openHelper.writableDatabase.execSQL("CREATE TRIGGER p1_fail BEFORE INSERT ON takes BEGIN SELECT RAISE(ABORT, 'P1 test failure'); END")
            tag("review-save").performClick();compose.waitUntil(15000) { !audio.busy && audio.message?.startsWith("Take could not be saved") == true }
            assertEquals(pending.id,audio.reviewTake!!.id);assertNull(runBlocking { db.takes().find(pending.id) });shot("failed-save-retry")
            db.openHelper.writableDatabase.execSQL("DROP TRIGGER p1_fail")
            tag("review-save").performClick();compose.waitUntil(15000) { !audio.busy && audio.reviewTake == null }
            assertNotNull(runBlocking { db.takes().find(pending.id) })
            compose.onNodeWithTag("take-list").performScrollToNode(hasText("Record take"));free()
            compose.onNodeWithTag("take-list").performScrollToNode(hasText("Close recordings"));click("Close recordings");shown("Discard temporary take?");click("Return to review")
            tag("review-discard").performClick();assertNull(audio.reviewTake)
            compose.onNodeWithTag("take-list").performScrollToNode(hasText("Record take"));click("Record take")
            compose.waitUntil(10000) { audio.recording && audio.elapsedMs >= 1500 }
            device.pressHome();compose.waitUntil(10000) { !audio.recording && !audio.busy }
            device.executeShellCommand("am start -W -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -f 0x10200000 -n ${app.packageName}/com.mrayover.practicelogger.MainActivity")
            review();assertNull(audio.playingId);tag("review-discard").performClick()
            assertEquals(old.size+1,runBlocking { db.takes().readAll().size })
            assertEquals(blocks,runBlocking { db.blocks().readAll() });assertEquals(sessions,runBlocking { BackupRepository(db).snapshot().sessions })
            old.forEach { assertEquals(it,runBlocking { db.takes().find(it.id) });assertArrayEquals(oldBytes[it.id],TakeRepository(db,app).audioFile(it).readBytes()) }
            compose.onNodeWithTag("take-list").performScrollToNode(hasText("Close recordings"));click("Close recordings")
        } finally { runCatching { db.openHelper.writableDatabase.execSQL("DROP TRIGGER IF EXISTS p1_fail") };compose.runOnIdle { audio.stopPlayback() };db.close() }
    }
}
