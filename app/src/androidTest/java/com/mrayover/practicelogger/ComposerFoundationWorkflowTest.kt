package com.mrayover.practicelogger

import android.app.Activity
import android.content.res.Configuration
import androidx.activity.compose.setContent
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import com.mrayover.practicelogger.composer.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

/** One API35 workflow: real Tools/Activities, storage identity, dirty recreation and navigation. */
class ComposerFoundationWorkflowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val device get() = UiDevice.getInstance(instrumentation)
    private fun waitText(text: String) {
        try {
            compose.waitUntil(15000) { compose.onAllNodesWithText(text).fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty() }
        } catch (failure: Throwable) {
            val dir = File(compose.activity.getExternalFilesDir(null), "composer-c1").apply { mkdirs() }
            device.takeScreenshot(File(dir, "failure.png"))
            device.dumpWindowHierarchy(File(dir, "failure-window.xml"))
            File(dir, "failure-compose.txt").writeText(compose.onRoot().printToString())
            throw failure
        }
        compose.onNodeWithText(text).assertIsDisplayed()
    }
    private fun dismissImmersiveTip() {
        device.wait(Until.findObject(By.text("Got it")), 1500)?.click()
        device.waitForIdle()
    }
    private fun tap(text: String) { waitText(text); compose.onNodeWithText(text).performClick() }
    private fun field(label: String, text: String) {
        val node = compose.onNodeWithText(label)
        if (!node.isDisplayed()) node.performScrollTo()
        node.performClick().performTextReplacement(text)
    }
    private fun hideIme() {
        val activity = resumed()
        var visible = false
        instrumentation.runOnMainSync {
            activity.window.decorView.rootWindowInsets?.let {
                visible = androidx.core.view.WindowInsetsCompat.toWindowInsetsCompat(it).isVisible(androidx.core.view.WindowInsetsCompat.Type.ime())
            }
        }
        if (visible) device.pressBack()
        compose.waitForIdle()
    }
    private fun resumed(): Activity {
        var result: Activity? = null
        instrumentation.runOnMainSync { result = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).single() }
        return requireNotNull(result)
    }
    private fun screenshot(name: String) {
        compose.waitForIdle(); device.waitForIdle()
        val dir = File(compose.activity.getExternalFilesDir(null), "composer-c1").apply { mkdirs() }
        assertTrue(device.takeScreenshot(File(dir, "$name.png")))
    }
    @Test fun launcherPersistenceLandscapeRecreationAndSafeReturn() {
        waitText("Tools"); tap("Tools")
        compose.onNodeWithText("compose.logger").performScrollTo().performClick()
        waitText("Create New Composition")
        assertTrue(resumed() is ComposerLibraryActivity)
        assertEquals(Configuration.ORIENTATION_PORTRAIT, resumed().resources.configuration.orientation)
        screenshot("library-empty")
        tap("Create New Composition")
        compose.onNodeWithText("Create").assertIsNotEnabled()
        field("Composition title", "C1 native etude")
        field("Tempo (30–300 BPM)", "137")
        field("Time signature (e.g. 4/4)", "7/8")
        hideIme()
        tap("Create")
        waitText("Project settings")
        dismissImmersiveTip()
        assertTrue(resumed() is ComposerActivity)
        assertEquals(Configuration.ORIENTATION_LANDSCAPE, resumed().resources.configuration.orientation)
        val store = CompositionStore(File(compose.activity.filesDir, "composer/projects"))
        val original = store.list().projects.single()
        assertEquals("C1 native etude", original.title)
        assertEquals(137, original.tempoBpm); assertEquals(7, original.numerator); assertEquals(8, original.denominator)
        assertTrue(original.tracks.isEmpty()); assertTrue(original.clips.isEmpty())
        compose.onNodeWithText("Undo").assertIsNotEnabled()
        screenshot("editor-loop")
        tap("Arrange"); waitText("Arrange workspace · C1 foundation")
        tap("Project settings")
        field("Composition title", "Retained draft")
        field("Tempo (30–300 BPM)", "146")
        hideIme(); tap("Apply")
        waitText("Unsaved settings")
        val editor = resumed()
        instrumentation.runOnMainSync { editor.recreate() }
        waitText("Project settings"); waitText("Retained draft"); waitText("Arrange workspace · C1 foundation")
        dismissImmersiveTip()
        device.pressBack(); waitText("Unsaved composition settings"); tap("Keep editing")
        tap("Save"); waitText("Saved on this device")
        assertEquals(original.id, store.list().projects.single().id)
        assertEquals(146, store.read(original.id).tempoBpm)
        tap("Back to composition library"); waitText("Create New Composition")
        waitText("Retained draft")
        val library = resumed()
        instrumentation.runOnMainSync { library.recreate() }
        waitText("Retained draft")
        tap("Rename"); field("Composition title", "Renamed native etude")
        hideIme()
        compose.onAllNodesWithText("Rename").onLast().performClick()
        waitText("Renamed native etude")
        screenshot("library-saved")
        assertEquals(original.id, store.list().projects.single().id)
        tap("Open"); waitText("Project settings"); waitText("Renamed native etude")
        waitText("146 BPM · 7/8")
        tap("Project settings"); field("Composition title", "Discard this draft")
        hideIme(); tap("Apply")
        device.pressBack(); tap("Discard changes")
        waitText("Create New Composition"); waitText("Renamed native etude")
        tap("Delete"); waitText("Delete composition?"); tap("Cancel")
        assertEquals(original.id, store.list().projects.single().id)
        tap("Delete"); tap("Delete composition")
        waitText("No saved compositions. Create an empty project to begin.")
        assertTrue(store.list().projects.isEmpty())
        tap("Back to Tools"); waitText("Reference Piano")
        assertTrue(resumed() is MainActivity)
        assertEquals(Configuration.ORIENTATION_PORTRAIT, resumed().resources.configuration.orientation)
        val cold = store.create(CompositionProject.empty("Cold recovery fixture", 91, 3, 4))
        File(compose.activity.getExternalFilesDir(null), "composer-c1/cold-id.txt").writeText(cold.id)
        // Capture the exact transition composable without racing its intentionally short readiness interval.
        compose.runOnIdle { compose.activity.setContent { PracticeTheme(ThemeChoice.DARK) { BrandedTransition() } } }
        waitText("compose.logger")
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        assertEquals(android.graphics.Color.BLACK, bitmap.getPixel(1, 1))
        File(compose.activity.getExternalFilesDir(null), "composer-c1/transition-render.png").outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    }
}
