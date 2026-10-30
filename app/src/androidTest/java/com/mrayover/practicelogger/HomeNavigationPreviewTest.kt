package com.mrayover.practicelogger

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.junit.Rule
import org.junit.Test
import java.io.File

class HomeNavigationPreviewTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun shown(text: String) { compose.waitUntil(15000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() } }
    private fun click(text: String) { shown(text); compose.onNodeWithText(text).scrollIfNeeded().assertIsDisplayed().performClick() }
    private fun inspect(title: String, file: String) {
        shown(title); compose.onNodeWithText("Back to Home").assertDoesNotExist()
        compose.onNodeWithText(title).assertIsDisplayed()
        val folder = File(compose.activity.getExternalFilesDir(null), "home-preview").apply { mkdirs() }
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).takeScreenshot(File(folder, "$file.png"))
    }
    private fun home() { compose.onNodeWithTag("home-brand").performClick(); shown("Practice Now") }
    @Test fun sectionsUseBannerAndLogStillSaves() {
        shown("Practice Now")
        click("Blocks"); inspect("Block Library", "library"); home()
        click("Practice Now"); inspect("Build a Session", "builder"); home()
        openHomeRoutines(compose); inspect("Routines", "routines"); home()
        click("Recording"); inspect("Recording", "recording"); home()
        click("Edit"); inspect("Manage Goals", "goals"); home()
        openHomeTools(compose); inspect("Tools", "tools")
        click("Backup and restore"); inspect("Backup and restore", "backup"); home()
        click("Log"); inspect("Log", "log")
        compose.onNodeWithTag("log-editor").performTextReplacement("Banner return preserves Log")
        compose.activity.getSystemService(android.view.inputmethod.InputMethodManager::class.java).hideSoftInputFromWindow(compose.activity.window.decorView.windowToken, 0)
        home(); click("Log"); shown("Choose date")
        compose.onNodeWithTag("log-editor").assertTextContains("Banner return preserves Log")
        click("Weekly View"); inspect("Weekly View", "weekly"); home()
    }
}
