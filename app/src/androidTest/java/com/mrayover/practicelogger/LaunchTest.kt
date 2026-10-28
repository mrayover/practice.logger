package com.mrayover.practicelogger

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test

class LaunchTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @get:Rule val diagnostic = object : org.junit.rules.TestWatcher() {
        private var watcher: Thread? = null
        override fun starting(description: org.junit.runner.Description) {
            watcher = Thread({
                try {
                    Thread.sleep(45000)
                    Thread.getAllStackTraces().forEach { (thread, frames) ->
                        android.util.Log.e("CatalogDiagnostic", "THREAD ${thread.name}: ${thread.state}\n" + frames.take(24).joinToString("\n"))
                    }
                    val instrumentation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
                    val context = instrumentation.targetContext
                    val folder = java.io.File(context.getExternalFilesDir(null), "screenshots").apply { mkdirs() }
                    instrumentation.uiAutomation.takeScreenshot()?.let { bitmap ->
                        java.io.FileOutputStream(java.io.File(folder, "catalog-stall.png")).use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
                        bitmap.recycle()
                    }
                } catch (_: InterruptedException) { }
                catch (error: Exception) { android.util.Log.e("CatalogDiagnostic", "Diagnostic failed", error) }
            }, "CatalogDiagnostic").apply { isDaemon = true; start() }
        }
        override fun finished(description: org.junit.runner.Description) { watcher?.interrupt() }
    }
    private fun shown(text: String) { compose.waitUntil(10000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() } }
    private fun displayed(text: String) {
        try {
            compose.waitUntil(15000) {
                try { compose.onNodeWithText(text).assertIsDisplayed(); true }
                catch (_: AssertionError) { false }
            }
        } catch (error: androidx.compose.ui.test.ComposeTimeoutException) {
            compose.runOnIdle {
                val decor = compose.activity.window.decorView
                val insets = androidx.core.view.ViewCompat.getRootWindowInsets(decor)
                println("PLATFORM INSETS: ime=" + insets?.getInsets(androidx.core.view.WindowInsetsCompat.Type.ime()) + "; system=" + insets?.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars()) + "; decor=" + decor.width + "x" + decor.height)
            }
            println("CATALOG VISIBILITY FAILURE: " + text + "\n" + compose.onRoot(useUnmergedTree = true).printToString())
            captureStage3("catalog-visibility-failure")
            throw error
        }
    }
    @Test fun catalogWorkflowAndDraftRecovery() {
        shown("Practice Now")
        compose.onNodeWithText("Goals").assertExists()
        compose.onNodeWithTag("home-goals").assertExists()
        compose.onNodeWithText("Edit").assertIsEnabled()
        captureStage3("00-home")
        compose.onNodeWithText("Recording").performScrollTo().performClick()
        shown("Choose a Block")
        compose.onNodeWithText("Your Blocks will appear here.").assertExists()
        compose.onNodeWithText("Back to Home").performClick()
        openHomeTools(compose)
        compose.onNodeWithText("Back to Home").performClick()
        openHomeRoutines(compose)
        compose.onNodeWithText("Back to Home").performScrollTo().performClick()
        compose.onNodeWithText("Practice Now").performScrollTo().performClick()
        shown("Build a Session")
        compose.onNodeWithText("Back to Home").performScrollTo().performClick()
        shown("Blocks")
        compose.onNodeWithText("Blocks").performScrollTo().performClick()
        compose.onNodeWithText("New Block").performClick()
        compose.onNodeWithText("Save").performScrollTo().performClick()
        compose.onNodeWithText("Enter a name.").assertExists()
        compose.onNodeWithTag("Name").performScrollTo().performTextInput("Restart smoke")
        compose.onNodeWithTag("Action").performScrollTo().performTextInput("Play the bridge from memory")
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("Name").assertTextContains("Restart smoke")
        compose.onNodeWithText("Save").performScrollTo().performClick()
        // Commit and settle navigation before verifying saved data across recreation.
        shown("Block Library"); shown("Restart smoke")
        displayed("Restart smoke")
        compose.waitForIdle()
        compose.activityRule.scenario.recreate()
        shown("Block Library")
        shown("Restart smoke")
        displayed("Restart smoke")
        compose.onNodeWithText("Restart smoke").assertIsDisplayed().performClick()
        shown("Edit Block"); shown("Default duration (minutes)")
        compose.onNodeWithTag("Default duration (minutes)").performScrollTo().performTextReplacement("8")
        compose.onNodeWithText("Save").performScrollTo().performClick()
        shown("Block Library")
        shown("8 min")
        compose.onNodeWithText("Search Blocks").performTextInput("nonexistent")
        compose.onNodeWithText("No matching Blocks.").assertIsDisplayed()
        compose.onNodeWithText("Search Blocks").performTextReplacement("bridge")
        compose.onNodeWithText("Restart smoke").assertExists().performClick()
        compose.onNodeWithText("Archive Block").performScrollTo().performClick()
        compose.onNodeWithText("Archive").performClick()
        shown("Block Library")
        shown("Restart smoke")
        compose.onNodeWithText("Active", useUnmergedTree = true).performClick()
        compose.onNodeWithText("No Blocks yet. Create your first Block.").assertExists()
        compose.onNodeWithText("Archived", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Restart smoke").performClick()
        compose.onNodeWithText("Restore Block").performScrollTo().performClick()
        compose.onNodeWithText("Restore", substring = false).performClick()
        shown("Block Library")
        shown("Restart smoke")
        compose.onNodeWithText("Restart smoke").performClick()
        compose.onNodeWithTag("Name").performTextReplacement("Discard me")
        compose.onNodeWithText("Cancel").performScrollTo().performClick()
        compose.onNodeWithText("Keep editing").performClick()
        compose.onNodeWithTag("Name").performScrollTo().assertTextContains("Discard me")
        compose.onNodeWithText("Cancel").performScrollTo().performClick()
        compose.onNodeWithText("Discard", substring = false).performClick()
        shown("Block Library")
        compose.onNodeWithText("Restart smoke").assertExists()
        compose.onNodeWithText("Discard me").assertDoesNotExist()
        compose.onNodeWithText("Back to Home").performClick()
        compose.onNodeWithText("Recording").performScrollTo().performClick()
        shown("Choose a Block")
        compose.onNodeWithText("Restart smoke").performScrollTo().performClick()
        shown("Block recordings")
        compose.onNodeWithText("Close recordings").performScrollTo().performClick()
        compose.onNodeWithText("Back to Home").performScrollTo().performClick()
    }
}
