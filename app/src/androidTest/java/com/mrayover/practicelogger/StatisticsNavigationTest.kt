package com.mrayover.practicelogger

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.junit.Rule
import org.junit.Test
import java.io.File

class StatisticsNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun homeTotalOpensStatisticsAndReturnsWithBackAndBanner() {
        compose.waitUntil(15000) { compose.onAllNodesWithTag("home-statistics").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("home-statistics").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithTag("statistics-screen").assertIsDisplayed()
        compose.waitUntil(15000) { compose.onAllNodesWithTag("statistics-total").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("No recorded practice yet.").assertIsDisplayed()
        compose.onNodeWithText("This week").assertIsDisplayed()
        val folder = File(compose.activity.getExternalFilesDir(null), "home-preview").apply { mkdirs() }
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.takeScreenshot(File(folder, "statistics.png"))
        compose.onNodeWithText("Recent activity (latest 20)").performScrollTo().assertIsDisplayed()
        device.pressBack()
        compose.onNodeWithTag("home-menu").assertIsDisplayed()
        compose.onNodeWithTag("home-statistics").performScrollTo().performClick()
        compose.onNodeWithTag("statistics-screen").assertIsDisplayed()
        compose.onNodeWithTag("home-brand", useUnmergedTree = true).performClick()
        compose.onNodeWithTag("home-menu").assertIsDisplayed()
    }
}
