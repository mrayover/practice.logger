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

/** One scoped real-engine P4 layout/review path; no full regression fixture dependencies. */
class LoopRecorderPreviewTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val device get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private fun shown(text: String) = compose.waitUntil(15000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    private fun tag(name: String) = compose.onNodeWithTag(name).assertIsDisplayed()
    private fun shot(name: String) {
        compose.waitForIdle()
        val dir = File(compose.activity.getExternalFilesDir(null), "loop-refinement").apply { mkdirs() }
        device.takeScreenshot(File(dir, "$name.png"))
    }
    private fun keyboard() {
        compose.runOnIdle {
            compose.activity.getSystemService(android.view.inputmethod.InputMethodManager::class.java)
                .hideSoftInputFromWindow(compose.activity.window.decorView.windowToken, 0)
            compose.activity.currentFocus?.clearFocus()
        }
        compose.waitForIdle()
    }
    private fun layout(review: Boolean = false) {
        compose.onNodeWithTag("practice-loop-setup-content-scrollbar").assertDoesNotExist()
        val panel = tag("practice-loop-setup-content").fetchSemanticsNode().boundsInRoot
        val names = listOf("phrase-bpm", "phrase-beats", "phrase-count-in", "phrase-duration", "phrase-budget-row") +
            listOf(4,8,16,32,6,12,24,48).map { "phrase-shortcut-$it" } +
            if (review) listOf("review-save", "review-rerecord", "review-preview", "review-discard") else listOf("phrase-start")
        names.forEach { name ->
            val bounds = tag(name).fetchSemanticsNode().boundsInRoot
            assertTrue("$name clipped: $bounds / $panel", bounds.top >= panel.top-1 && bounds.bottom <= panel.bottom+1)
        }
        val fields = listOf("phrase-bpm", "phrase-beats", "phrase-count-in").map { tag(it).fetchSemanticsNode().boundsInRoot }
        assertTrue(fields.zipWithNext().all { (a,b) -> a.right<=b.left+1 && kotlin.math.abs(a.top-b.top)<=1 })
        for (action in listOf("record", "play")) {
            val free = tag("practice-free-$action").fetchSemanticsNode().boundsInRoot
            val loop = tag("practice-loop-$action").fetchSemanticsNode().boundsInRoot
            assertEquals(free.left,loop.left,1f); assertEquals(free.right,loop.right,1f); assertEquals(free.height,loop.height,1f)
        }
        val footer = listOf("Start", "Complete", "Notes", "Skip").map { compose.onNodeWithText(it).assertIsDisplayed().fetchSemanticsNode().boundsInRoot }
        footer.zipWithNext().forEach { (a,b) -> assertEquals(a.top,b.top,1f); assertEquals(a.bottom,b.bottom,1f) }
        compose.onNodeWithTag("phrase-recording-click").assertDoesNotExist()
        if (review) listOf("review-save", "review-rerecord", "review-discard", "review-preview").forEach { compose.onAllNodesWithTag(it).assertCountEquals(1) }
    }
    @Test fun compactSetupPermanentClickAndInlineReview() {
        val app = compose.activity.application
        val db = BlockDatabase.open(app)
        val practice = ViewModelProvider(compose.activity)[PracticeViewModel::class.java]
        val audio = ViewModelProvider(compose.activity)[RecordingViewModel::class.java]
        try {
            device.executeShellCommand("pm grant ${app.packageName} ${Manifest.permission.RECORD_AUDIO}")
            val (block,session) = runBlocking {
                val b = BlockRepository(db).save(null,BlockDraft("Loop preview","Listen and play.","5","155"))
                ImageReferenceRepository(db,File(app.filesDir,"block-images")).import(b) {
                    InstrumentationRegistry.getInstrumentation().context.assets.open("references/score.pdf")
                }
                val repo=SessionRepository(db); repo.addBlock(b); b to repo.start()
            }
            shown("Practice Now"); compose.runOnIdle { practice.openSession(session) }; shown("Practice Description")
            compose.waitUntil(15000) { audio.ready }
            tag("practice-loop-record").performClick()
            layout(); compose.onNodeWithText("One phrase: 3.10 seconds").assertExists(); shot("setup-portrait")
            tag("phrase-beats").performTextReplacement("65"); keyboard(); tag("phrase-start").assertIsNotEnabled()
            tag("phrase-beats").performTextReplacement("7")
            tag("phrase-count-in").performTextReplacement("9223372036854775807"); keyboard(); tag("phrase-start").assertIsNotEnabled()
            tag("phrase-bpm").performTextReplacement("300"); tag("phrase-beats").performTextReplacement("16")
            tag("phrase-count-in").performTextReplacement("4"); keyboard()
            val savedBefore=runBlocking { db.takes().readAll() }
            tag("phrase-start").performClick()
            compose.waitUntil(10000) { audio.phrasing && audio.countInBeat>0 && audio.countInTotal==4L }
            compose.waitUntil(10000) { audio.phrasing && audio.countInBeat==0 && audio.phraseClickEnabled && audio.phraseGuideFramesPlayed()>PhraseSpec(300,16,4).countInFrames }
            compose.waitUntil(15000) { !audio.recording && audio.reviewTake!=null }
            layout(true); shot("review-inline")
            val first=audio.reviewTake!!
            assertEquals(savedBefore,runBlocking { db.takes().readAll() })
            assertEquals(PhraseSpec(300,16,4).frames,PhraseWave.frames(File(app.filesDir,"recordings/"+first.fileName)))
            tag("review-preview").performClick(); compose.waitUntil(10000) { audio.playingId==first.id }
            tag("review-preview").performClick(); compose.runOnIdle { assertNull(audio.playingId) }
            tag("practice-reference-toggle").performClick()
            compose.onNodeWithTag("practice-loop-panel").assertDoesNotExist()
            tag("practice-review-expand").performClick()
            layout(true); tag("phrase-bpm").assertTextContains("300"); tag("phrase-beats").assertTextContains("16")
            tag("phrase-count-in").assertTextContains("4")
            tag("review-rerecord").performClick()
            compose.waitUntil(10000) { audio.phrasing && audio.countInTotal==4L && audio.phraseClickEnabled }
            assertFalse(File(app.filesDir,"recordings/"+first.fileName).exists())
            compose.waitUntil(15000) { !audio.recording && audio.reviewTake!=null }
            val second=audio.reviewTake!!; assertNotEquals(first.id,second.id)
            tag("review-save").performClick(); compose.waitUntil(15000) { !audio.busy && audio.reviewTake==null }
            tag("phrase-start").assertIsEnabled(); assertNotNull(runBlocking { db.takes().find(second.id) }); layout()
            tag("phrase-start").performClick(); compose.waitUntil(15000) { !audio.recording && audio.reviewTake!=null }
            val discarded=audio.reviewTake!!; tag("review-discard").performClick()
            tag("phrase-start").assertIsEnabled(); assertNull(audio.reviewTake)
            assertFalse(File(app.filesDir,"recordings/"+discarded.fileName).exists())
            assertNotNull(runBlocking { db.takes().find(second.id) }); layout(); shot("after-discard")
            tag("practice-free-record").performClick(); compose.waitUntil(10000) { audio.recording && audio.elapsedMs>1200 }
            compose.runOnIdle { assertFalse(audio.phrasing); assertFalse(audio.phraseClickEnabled) }
            tag("practice-free-record").performClick(); compose.waitUntil(10000) { audio.reviewTake!=null && !audio.busy }
            assertEquals("FREE",audio.reviewTake!!.mode)
            compose.onNodeWithTag("practice-loop-panel").assertDoesNotExist()
            compose.onAllNodesWithTag("review-discard").assertCountEquals(1); tag("review-discard").performClick()
        } finally {
            shot("final")
            compose.runOnIdle { audio.stopPlayback(); if(audio.recording) audio.finishCapture(); practice.stopClick() }
            db.close()
        }
    }
}
