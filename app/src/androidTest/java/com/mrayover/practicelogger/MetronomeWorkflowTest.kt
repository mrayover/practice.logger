package com.mrayover.practicelogger

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class MetronomeWorkflowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun shown(text: String) { compose.waitUntil(15000) { compose.onAllNodesWithText(text, substring = false).fetchSemanticsNodes().isNotEmpty() } }
    private fun click(text: String) { compose.onNodeWithText(text, substring = false).scrollIfNeeded().performClick() }
    @Test fun clickTempoTimerAndLifecycleStayIndependent() {
        val db = BlockDatabase.open(compose.activity.applicationContext)
        try {
            runBlocking { BlockRepository(db).save(null, BlockDraft("Metronome test", "Play with click", "5", "90")) }
            shown("Practice Now"); click("Practice Now"); shown("Add Metronome test"); click("Add Metronome test")
            click("Start Session"); shown("Metronome · 90 BPM")
            compose.onNodeWithContentDescription("Increase click BPM by 1").scrollIfNeeded().performClick()
            shown("Metronome · 91 BPM")
            val unused = runBlocking { db.sessions().readAll().single { it.current?.name == "Metronome test" } }
            assertNull(unused.current!!.lastUsedBpm)
            compose.onNodeWithContentDescription("Decrease click BPM by 1").scrollIfNeeded().performClick()
            shown("Metronome · 90 BPM")
            repeat(3) { click("Start click"); shown("Stop click"); click("Stop click"); shown("Start click") }
            click("Start click"); shown("Stop click")
            compose.onNodeWithText("Ready when you are").assertExists()
            compose.onNodeWithContentDescription("Set BPM").scrollIfNeeded().performClick(); compose.onNodeWithText("BPM (30–300)").performTextReplacement("301")
            compose.onNodeWithText("Apply BPM").assertIsNotEnabled()
            compose.onNodeWithText("BPM (30–300)").performTextReplacement("126"); compose.onNodeWithText("Apply BPM").performClick()
            shown("Metronome · 126 BPM"); shown("Stop click")
            click("Start"); shown("Practicing"); click("Stop click"); shown("Practicing")
            click("Pause"); click("Start click"); shown("Stop click")
            val vm = ViewModelProvider(compose.activity)[PracticeViewModel::class.java]
            compose.runOnIdle { assertTrue(vm.clicking); assertFalse(vm.running) }
            compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
            compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
            shown("Start click"); compose.runOnIdle { assertFalse(vm.clicking); assertFalse(vm.running) }
            click("Start click"); shown("Stop click")
            compose.activityRule.scenario.recreate(); shown("Start click")
            click("Save for later"); compose.onNodeWithText("Back to Main").performClick(); openHomePractice(compose); shown("Continue Session"); click("Continue Session"); shown("Metronome · 126 BPM"); shown("Start click")
            val session = runBlocking { db.sessions().readAll().single { it.current?.name == "Metronome test" } }
            assertEquals(126, session.current!!.lastUsedBpm)
            assertEquals(90, runBlocking { db.blocks().find(session.current!!.sourceBlockId) }!!.tempo)
            click("Start click"); shown("Stop click"); click("Complete"); shown("Session Summary")
            compose.onNodeWithText("Last used click: 126 BPM").assertExists()
            val nextVm = ViewModelProvider(compose.activity)[PracticeViewModel::class.java]
            compose.runOnIdle { assertFalse(nextVm.clicking) }
            click("Home"); shown("Practice Now")
        } finally { db.close() }
    }
}
