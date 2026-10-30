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

class FocusedLayoutTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private val device get()=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private fun shown(text:String) { compose.waitUntil(15000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() } }
    private fun click(text:String) { shown(text);compose.onNodeWithText(text).scrollIfNeeded().performClick() }
    private fun screenshot(name:String) { compose.waitForIdle();InstrumentationRegistry.getInstrumentation().waitForIdleSync(); val dir=File(compose.activity.getExternalFilesDir(null),"screenshots");dir.mkdirs();device.takeScreenshot(File(dir,"focused-$name.png")) }
    @Test fun longGoalInstructionsReferenceAndCompactLoop() {
        val app=compose.activity.application
        val db=BlockDatabase.open(app)
        val practice=ViewModelProvider(compose.activity)[PracticeViewModel::class.java]
        val recordings=ViewModelProvider(compose.activity)[RecordingViewModel::class.java]
        try {
            device.executeShellCommand("pm grant ${app.packageName} ${Manifest.permission.RECORD_AUDIO}")
            val session=runBlocking {
                val id=BlockRepository(db).save(null,BlockDraft("Long instructions","Play slowly, listen and relax. ".repeat(80),"5","120","Reference notes: breathe between phrases. ".repeat(8)))
                val goal=GoalRepository(db).save(null,"Sketch layout Goal","",0,"")
                GoalRepository(db).setBlocks(goal,listOf(id)); val sid=SessionRepository(db).startGoal(goal)
                ImageReferenceRepository(db,File(app.filesDir,"block-images")).import(id) { InstrumentationRegistry.getInstrumentation().context.assets.open("references/score.pdf") }
                sid
            }
            shown("Practice Now");compose.runOnIdle { practice.openSession(session) };shown("Goal: Sketch layout Goal")
            compose.onNodeWithTag("practice-description").performTouchInput { swipeUp() }
            compose.onNodeWithText("Complete").assertIsDisplayed();compose.onNodeWithText("Skip").assertIsDisplayed();screenshot("long-goal")
            click("Start");click("Start click");click("Show reference")
            compose.waitUntil(15000) { compose.onAllNodesWithTag("reference-viewer").fetchSemanticsNodes().isNotEmpty() }
            click("Next page");shown("Page 2 of 2")
            assertTrue(practice.running);assertTrue(practice.clicking);screenshot("bounded-pdf")
            click("Hide reference");click("Record Loop");shown("Record one phrase")
            compose.onNodeWithText("Phrase BPM").performTextReplacement("240")
            click("4 beats");click("Start 4-click count-in")
            compose.waitUntil(12000) { !recordings.recording && recordings.takes.value.any { it.mode=="LOOP" } }
            compose.waitUntil(10000) { recordings.playingId!=null }
            assertTrue(practice.running);screenshot("direct-loop")
            compose.onNodeWithContentDescription("Stop latest loop").scrollIfNeeded().performClick()
            compose.onNodeWithContentDescription("Play latest loop").scrollIfNeeded().performClick()
            compose.waitUntil(10000) { recordings.playingId!=null };compose.runOnIdle { recordings.stopPlayback() }
            click("Save for later");compose.onNodeWithText("Back to Main").performClick();shown("Practice Now")
        } catch(failure:Throwable) { screenshot("layout-failure");throw failure } finally { db.close() }
    }
}

class FocusedExpiredReopenTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Test fun savedExpiredPlacementCannotEmitAnAlarmOnOpen():Unit=runBlocking {
        val db=BlockDatabase.open(compose.activity.application)
        try {
            val id=BlockRepository(db).save(null,BlockDraft("Expired safety","Already reached duration","1"))
            val repo=SessionRepository(db);repo.addBlock(id);val session=repo.start();repo.checkpoint(session,60_000)
            val practice=ViewModelProvider(compose.activity)[PracticeViewModel::class.java]
            compose.waitUntil(15000) { practice.catalog.value.ready };compose.runOnIdle { practice.openSession(session) }
            compose.waitUntil(15000) { compose.onAllNodesWithText("Practice Description").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Continue").assertIsDisplayed();compose.runOnIdle { assertFalse(practice.running);assertEquals(0,practice.completionSignals);assertEquals(0L,practice.completionFramesWritten()) }
        } finally { db.close() }
    }
}
