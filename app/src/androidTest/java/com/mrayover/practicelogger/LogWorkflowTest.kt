package com.mrayover.practicelogger

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class LogWorkflowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun shown(text: String) { compose.waitUntil(15000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() } }
    private fun click(text: String) { shown(text); val node = compose.onNodeWithText(text); if (!node.isDisplayed()) node.performScrollTo(); node.performClick() }
    private fun saved(db: BlockDatabase, day: LocalDate, text: String) { compose.waitUntil(15000) { runBlocking { db.logs().find(day.toString())?.text == text } } }
    @Test fun dailyWeeklyRapidNavigationRecreationAndNewDay() {
        val db = BlockDatabase.open(compose.activity.applicationContext)
        val today = LocalDate.now(); val yesterday = today.minusDays(1)
        try {
            click("Log"); shown("Choose date")
            compose.waitUntil(15000) { val vm = ViewModelProvider(compose.activity)[LogViewModel::class.java]; vm.ready && !vm.busy }
            compose.onNodeWithTag("log-editor").performTextReplacement("Today Log fixture\nIdeas 🎵")
            click("Back to Home"); click("Log"); shown("Choose date")
            compose.onNodeWithTag("log-editor").assertTextContains("Today Log fixture\nIdeas 🎵")
            click("Weekly View"); shown("Previous week")
            if (logWeek(yesterday) != logWeek(today)) click("Previous week")
            compose.onNodeWithTag("log-week").performScrollToNode(hasTestTag("log-day-$yesterday"))
            compose.onNodeWithTag("log-day-$yesterday").performClick(); shown("Choose date")
            compose.onNodeWithTag("log-editor").performTextReplacement("Yesterday Log fixture")
            click("Back to Home"); click("Log"); shown("Choose date")
            compose.onNodeWithTag("log-editor").assertTextContains("Today Log fixture\nIdeas 🎵")
            compose.onNodeWithTag("log-editor").performTextReplacement("Today Log edited fixture")
            compose.activityRule.scenario.recreate(); shown("Choose date")
            compose.onNodeWithTag("log-editor").assertTextContains("Today Log edited fixture")
            click("Choose date"); shown("Cancel"); click("Cancel")
            captureStage3("log-daily")
            saved(db, today, "Today Log edited fixture"); saved(db, yesterday, "Yesterday Log fixture")
            val vm = ViewModelProvider(compose.activity)[LogViewModel::class.java]
            compose.runOnIdle { vm.resume(today.plusDays(1)) }
            compose.waitUntil(15000) { vm.date == today.plusDays(1) && !vm.busy }
            compose.onNodeWithTag("log-editor").assertTextEquals("")
            assertEquals("Today Log edited fixture", runBlocking { db.logs().find(today.toString())!!.text })
            compose.runOnIdle { vm.resume(today) }
            compose.waitUntil(15000) { vm.date == today && !vm.busy }
            click("Weekly View"); shown("Previous week"); captureStage3("log-weekly")
            click("Next week"); click("Previous week")
            click("Back to Home")
        } finally { db.close() }
    }
}

class LogReopenTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun coldProcessRetainsBothDatesAndBackup() {
        val db = BlockDatabase.open(compose.activity.applicationContext)
        try { runBlocking {
            val today = LocalDate.now()
            assertEquals("Today Log edited fixture", db.logs().find(today.toString())!!.text)
            assertEquals("Yesterday Log fixture", db.logs().find(today.minusDays(1).toString())!!.text)
            val data = BackupRepository(db).snapshot()
            assertEquals(data, BackupCodec.decode(BackupCodec.encode(data)))
        } } finally { db.close() }
    }
}
