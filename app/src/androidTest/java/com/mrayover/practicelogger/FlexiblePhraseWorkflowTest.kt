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

class FlexiblePhraseWorkflowTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private val device get()=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val audio get()=ViewModelProvider(compose.activity)[RecordingViewModel::class.java]
    private fun shown(text:String)=compose.waitUntil(15000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    private fun click(text:String) { shown(text);compose.onNodeWithText(text).scrollIfNeeded().performClick() }
    private fun tag(value:String)=compose.onNodeWithTag(value).scrollIfNeeded().assertIsDisplayed()
    private fun keyboard()=compose.runOnIdle { compose.activity.currentFocus?.clearFocus();compose.activity.getSystemService(android.view.inputmethod.InputMethodManager::class.java).hideSoftInputFromWindow(compose.activity.window.decorView.windowToken,0) }
    private fun ready() { shown("Practice Now");compose.waitUntil(15000) { audio.ready && !audio.busy };device.executeShellCommand("pm grant ${compose.activity.packageName} ${Manifest.permission.RECORD_AUDIO}") }
    private fun review(timeout:Long=45000) { compose.waitUntil(timeout) { !audio.recording && !audio.busy && audio.reviewTake!=null } }
    private fun shot(name:String) { compose.waitForIdle();val dir=File(compose.activity.getExternalFilesDir(null),"p2-phrase");dir.mkdirs();device.takeScreenshot(File(dir,"$name.png")) }
    @Test fun customUiDefaultPersistsAndCountInIsExcludedFromReviewedWav() {
        val app=compose.activity.application;val db=BlockDatabase.open(app)
        val practice=ViewModelProvider(compose.activity)[PracticeViewModel::class.java]
        try {
            ready();click("Tools");click("Audio Settings");tag("default-count-in").performTextReplacement("7");keyboard();click("Save default count-in")
            compose.waitUntil(10000) { audio.defaultCountIn==7L && !audio.busy }
            assertEquals(7L,CountInPreferences(app).read());compose.activityRule.scenario.recreate();tag("default-count-in").assertTextContains("7")
            click("Back to Tools");compose.onNodeWithTag("home-brand",useUnmergedTree=true).performClick();shown("Practice Now")
            val (block,session)=runBlocking { val b=BlockRepository(db).save(null,BlockDraft("P2 custom loop","Play","5","120"));val repo=SessionRepository(db);repo.addBlock(b);b to repo.start() }
            compose.runOnIdle { practice.openSession(session) };shown("Practice Description");click("Record Loop")
            tag("phrase-count-in").assertTextContains("7")
            for(value in listOf("0","65","128","bad")) { tag("phrase-beats").performTextReplacement(value);tag("phrase-start").assertIsNotEnabled() }
            tag("phrase-beats").performTextReplacement("3");tag("phrase-count-in").performTextReplacement("9223372036854775807");keyboard();tag("phrase-start").assertIsNotEnabled()
            tag("phrase-count-in").performTextReplacement("7");keyboard();shot("custom-loop-setup");tag("phrase-start").performClick()
            compose.waitUntil(10000) { audio.phrasing && audio.countInBeat>0 && audio.countInTotal==7L }
            compose.waitUntil(15000) { audio.phrasing && audio.countInBeat==0 && audio.elapsedMs>250 && audio.phraseGuideFramesPlayed()>PhraseSpec(120,3,7).countInFrames }
            review();val pending=audio.reviewTake!!;assertEquals(3,pending.beatCount);assertEquals(1500L,pending.durationMs)
            assertNull(runBlocking { db.takes().find(pending.id) });assertEquals(PhraseSpec(120,3).frames,PhraseWave.frames(File(app.filesDir,"recordings/"+pending.fileName)))
            tag("review-save").performClick();compose.waitUntil(15000) { !audio.busy && audio.reviewTake==null }
            val saved=runBlocking { db.takes().find(pending.id)!! };assertEquals(block,saved.blockId)
            compose.runOnIdle { practice.stopClick();audio.play(saved) };compose.waitUntil(15000) { audio.loopFramesPlayed()>PhraseSpec(120,3).frames*2L }
            compose.runOnIdle { audio.stopPlayback() };shot("custom-loop-saved")
            device.pressBack();click("Back to Main")
        } finally { compose.runOnIdle { audio.stopPlayback();practice.stopClick() };db.close() }
    }
    @Test fun nonPresetLengthsAndCountInsWorkAtTempoEdges() {
        val app=compose.activity.application;val db=BlockDatabase.open(app)
        try {
            ready();val block=runBlocking { BlockRepository(db).save(null,BlockDraft("P2 boundary loops","Play")) }
            click("Recording");click("P2 boundary loops");shown("Block recordings")
            val old=runBlocking { db.takes().readAll() }
            for(spec in listOf(PhraseSpec(300,5,1),PhraseSpec(120,7,3),PhraseSpec(30,11,1))) {
                compose.runOnIdle { audio.startPhrase(block,spec,false) };review()
                val take=audio.reviewTake!!;assertEquals(spec.beats,take.beatCount);assertEquals(spec.durationMs,take.durationMs)
                assertEquals(spec.frames,PhraseWave.frames(File(app.filesDir,"recordings/"+take.fileName)))
                compose.runOnIdle { audio.discardReview() }
                assertEquals(old,runBlocking { db.takes().readAll() })
            }
            compose.onNodeWithTag("take-list").performScrollToNode(hasText("Close recordings"));click("Close recordings")
        } finally { compose.runOnIdle { audio.stopPlayback() };db.close() }
    }
    @Test fun countInAndPartialCaptureCancellationNeverPublishIncompleteAudio() {
        val app=compose.activity.application;val db=BlockDatabase.open(app)
        try {
            ready();val block=runBlocking { BlockRepository(db).save(null,BlockDraft("P2 cancel","Play")) }
            click("Recording");click("P2 cancel");shown("Block recordings")
            val old=runBlocking { db.takes().readAll() }
            compose.runOnIdle { audio.startPhrase(block,PhraseSpec(30,64,12),true) }
            compose.waitUntil(10000) { audio.countInBeat>0 && audio.countInTotal==12L };click("Cancel phrase")
            compose.waitUntil(15000) { !audio.phrasing && !audio.busy };assertNull(audio.reviewTake)
            compose.runOnIdle { audio.startPhrase(block,PhraseSpec(120,64,1),true) }
            compose.waitUntil(10000) { audio.countInBeat==0 && audio.elapsedMs>250 }
            device.pressHome();compose.waitUntil(15000) { !audio.phrasing && !audio.busy }
            device.executeShellCommand("am start -W -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -f 0x10200000 -n ${app.packageName}/com.mrayover.practicelogger.MainActivity")
            shown("Block recordings");assertNull(audio.reviewTake);assertNull(audio.playingId);assertEquals(old,runBlocking { db.takes().readAll() })
            compose.onNodeWithTag("take-list").performScrollToNode(hasText("Close recordings"));click("Close recordings")
        } finally { compose.runOnIdle { audio.stopPlayback() };db.close() }
    }
}
