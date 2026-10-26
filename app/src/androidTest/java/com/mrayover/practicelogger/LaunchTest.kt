package com.mrayover.practicelogger

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test

class LaunchTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun installedApkLaunchesHome() {
        compose.onNodeWithText("practice.logger").assertIsDisplayed()
        compose.onNodeWithText("Foundation build. Block management comes next.").assertIsDisplayed()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("practice.logger").assertIsDisplayed()
    }
}
