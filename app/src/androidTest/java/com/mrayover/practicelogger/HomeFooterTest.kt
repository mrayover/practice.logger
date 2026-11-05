package com.mrayover.practicelogger

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.junit.Rule
import org.junit.Test
import kotlin.math.roundToInt

class HomeFooterTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun homeNavigationAndDurationFitOrScroll() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val resources = compose.activity.resources
        val enlarged = resources.configuration.fontScale > 1f
        val height = if (enlarged) 690 else 890
        val density = resources.displayMetrics.density
        val widthPx = (400 * density).roundToInt()
        val heightPx = (height * density).roundToInt()

        try {
            device.executeShellCommand("wm size ${widthPx}x$heightPx")
            compose.waitUntil(15_000) {
                val current = compose.activity.resources.configuration.screenHeightDp
                current in (height - 100)..height
            }
            val duration = "Total practice · 0 min 0 sec"
            val menu = compose.onNodeWithTag("home-menu")
            val destinations = listOf(
                "Practice Now" to "Add Blocks",
                "Blocks" to "Block Library",
                "Routines" to "Routines",
                "Tools" to "Metronome",
                "Recording" to "Choose a Block",
                "Log" to "Choose date",
                "Library" to "Media Library",
            )
            compose.waitUntil(15_000) {
                compose.onAllNodesWithText(duration).fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("Data usage").assertDoesNotExist()
            compose.onNodeWithTag("header-settings").assertIsDisplayed().assertIsEnabled()
            val footer = compose.onNodeWithText(duration)
            if (enlarged) {
                footer.assertIsNotDisplayed()
                footer.performScrollTo()
            }
            footer.assertIsDisplayed().assert(hasClickAction().not())
            val footerBounds = footer.getUnclippedBoundsInRoot()
            val menuBounds = menu.getUnclippedBoundsInRoot()
            check(footerBounds.bottom < menuBounds.bottom) { "Practice total extends below Home" }
            captureStage3(if (enlarged) "home-large-text-footer" else "home-portrait-footer")

            for ((label, destination) in destinations) {
                val button = compose.onNode(hasText(label) and hasClickAction())
                if (enlarged) button.performScrollTo()
                button.assertIsDisplayed().assertIsEnabled().performClick()
                compose.waitUntil(15_000) {
                    compose.onAllNodesWithText(destination).fetchSemanticsNodes().isNotEmpty()
                }
                compose.onNodeWithTag("home-menu").assertDoesNotExist()
                compose.onNodeWithTag("home-brand", useUnmergedTree = true).performClick()
                menu.assertExists()
            }
            compose.onNodeWithText(duration).assertExists()
        } finally {
            device.executeShellCommand("wm size reset")
        }
    }
}
