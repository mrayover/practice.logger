package com.mrayover.practicelogger

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import kotlin.math.abs

/** UI-C1 only: one device, shared alignment assertion, existing navigation/owners. */
class CosmicUiWorkflowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val device get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private fun waitTag(tag: String) = compose.waitUntil(15000) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
    private fun click(text: String) { compose.onNodeWithText(text).scrollIfNeeded().assertIsDisplayed().performClick() }
    private fun home() { compose.onNodeWithTag("home-brand", true).performClick(); waitTag("home-menu") }
    private fun shot(name: String) {
        compose.waitForIdle()
        device.takeScreenshot(File(File(compose.activity.getExternalFilesDir(null), "home-preview").apply { mkdirs() }, "cosmic-$name.png"))
    }
    private fun heading(title: String) {
        val node = compose.onNode(hasText(title) and SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading), true)
        node.scrollIfNeeded().assertIsDisplayed()
        val bounds = node.fetchSemanticsNode().boundsInRoot
        val screen = compose.onNodeWithTag("app-surface").fetchSemanticsNode().boundsInRoot
        assertTrue("Heading must center on page: $title $bounds / $screen", abs(bounds.center.x-screen.center.x) <= 2f)
        val layouts = mutableListOf<TextLayoutResult>()
        node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertTrue(layouts.isNotEmpty())
        for (layout in layouts) {
            assertEquals(TextAlign.Center, layout.layoutInput.style.textAlign)
            for (line in 0 until layout.lineCount) {
                assertTrue(layout.getLineRight(line)-layout.getLineLeft(line) <= layout.size.width+2)
                assertTrue(layout.getLineBottom(line) <= layout.size.height+2)
            }
        }
    }
    @Test fun cosmicSelectionRecreationAndRepresentativeCenteredPages() {
        val folder = File(compose.activity.getExternalFilesDir(null), "home-preview").apply { mkdirs() }
        try {
            waitTag("home-menu")
            compose.onNodeWithTag("header-settings").performClick()
            heading("Settings")
            compose.onNodeWithTag("theme-cosmic").performScrollTo().performClick()
            compose.waitUntil(10000) { compose.activity.themePreferences.choice.value == ThemeChoice.COSMIC }
            compose.onNodeWithTag("theme-cosmic").assertIsSelected()
            val cosmicBounds=compose.onNodeWithTag("theme-cosmic").fetchSemanticsNode().boundsInRoot
            compose.onNodeWithTag("theme-system").performScrollTo()
            assertTrue(compose.onNodeWithTag("theme-system").fetchSemanticsNode().boundsInRoot.top >= cosmicBounds.top)
            shot("settings")
            compose.activityRule.scenario.recreate(); waitTag("cosmic-background")
            assertEquals(ThemeChoice.COSMIC, compose.activity.themePreferences.choice.value)
            val nativeBackground=android.util.TypedValue()
            assertTrue(compose.activity.theme.resolveAttribute(android.R.attr.colorBackground, nativeBackground, true))
            assertEquals(CosmicPalette.scheme.background.toArgb(), nativeBackground.data)
            val bars=androidx.core.view.WindowCompat.getInsetsController(compose.activity.window, compose.activity.window.decorView)
            assertFalse(bars.isAppearanceLightStatusBars)
            assertFalse(bars.isAppearanceLightNavigationBars)
            assertEquals(CosmicPalette.scheme.background.value, resolvePalette(ThemeChoice.COSMIC, false).scheme.background.value)
            home(); shot("home")
            // Restore existing appearance through the same preference path without replacing audio owners.
            val owner = ViewModelProvider(compose.activity)[RecordingViewModel::class.java]
            compose.runOnIdle { compose.activity.themePreferences.save(ThemeChoice.LIGHT) }
            compose.onNodeWithTag("cosmic-background").assertDoesNotExist()
            assertSame(owner, ViewModelProvider(compose.activity)[RecordingViewModel::class.java])
            compose.runOnIdle { compose.activity.themePreferences.save(ThemeChoice.COSMIC) }
            waitTag("cosmic-background")
            val db = BlockDatabase.appInstance(compose.activity.application)
            val longGoal = "A long musical Goal for relaxed phrasing and clear articulation across the entire range"
            val longBlock = "Long Block name for balanced practice with a relaxed hand and consistent tone"
            val session=runBlocking {
                db.goals().insert(Goal("cosmic-goal", longGoal, createdAt=1, updatedAt=1))
                val block=BlockRepository(db).save(null, BlockDraft(longBlock, "Keep this instruction left aligned.", "5", "90"))
                db.sessions().insert(PracticeSession("cosmic-history", 1000, 2000, "COMPLETED", 1))
                db.sessions().insert(Placement("cosmic-earned", "cosmic-history", block, "Earned square", "Play", null, "", 0, 60000, 42000, "COMPLETED"))
                db.sessions().insert(BlockCompletion("cosmic-earned", "cosmic-history", block, 2000, "cosmic-goal", 1, 0xffD1262D.toInt()))
                db.playground().save(PlaygroundLayout("cosmic-earned", 0.0, 0.0, 0.0))
                val repo=SessionRepository(db); repo.addBlock(block); repo.start()
            }
            // Same real application at a bounded 320dp viewport and 200% text.
            compose.runOnIdle {
                compose.activity.setContent {
                    val base=LocalDensity.current
                    val preferences=compose.activity.themePreferences
                    val choice by preferences.choice.collectAsState()
                    CompositionLocalProvider(LocalDensity provides Density(base.density, 2f), LocalThemePreferences provides preferences) {
                        Box(Modifier.width(320.dp).fillMaxHeight()) { PracticeTheme(choice) { CatalogApp() } }
                    }
                }
            }
            waitTag("home-menu")
            click("Practice Now"); heading("Practice Now"); shot("practice-now-320-200")
            click("This Week"); heading("This Week"); home()
            click("Blocks"); heading("Block Library"); click("New Block"); heading("New Block"); shot("editor-320-200"); home()
            click("Edit"); heading("Manage Goals")
            compose.onNodeWithTag("open-goal-cosmic-goal").performScrollTo().performClick()
            heading(longGoal); shot("goal-320-200"); home()
            click("Tools"); heading("Tools"); click("Metronome"); heading("Metronome"); shot("tool-320-200"); home()
            click("Recording"); heading("Recording"); home()
            click("Library"); heading("Media Library"); shot("library-320-200"); home()
            compose.onNodeWithTag("home-statistics").performScrollTo().performClick(); heading("Statistics"); home()
            click("Log"); heading("Log"); click("Playground"); heading("Playground")
            val pixels=compose.onNodeWithTag("playground-canvas").captureToImage().toPixelMap()
            assertEquals(0xffD1262D.toInt(), pixels[pixels.width/2, pixels.height/2].toArgb())
            compose.onNodeWithTag("playground-canvas").performTouchInput { click(center) }
            compose.onNodeWithTag("playground-details").assertExists()
            compose.onNodeWithTag("playground-canvas").performTouchInput { swipe(androidx.compose.ui.geometry.Offset(width*.1f,height*.1f), androidx.compose.ui.geometry.Offset(width*.2f,height*.15f)) }
            shot("playground-320-200"); home()
            val practice = ViewModelProvider(compose.activity)[PracticeViewModel::class.java]
            compose.runOnIdle { practice.openSession(session) }
            waitTag("practice-ready"); heading(longBlock)
            compose.onNodeWithTag("practice-ready-prep").performScrollTo().performClick()
            waitTag("practice-block-heading"); heading(longBlock)
            compose.onNodeWithTag("practice-timer").assertIsDisplayed()
            shot("focused-320-200")
            // Changing decoration does not dispose the focused audio/recording owner.
            assertSame(owner, ViewModelProvider(compose.activity)[RecordingViewModel::class.java])
            // Representative shared waveform, warning, input and dialog surfaces, without audio tests.
            compose.runOnIdle {
                compose.activity.setContent {
                    PracticeTheme(ThemeChoice.COSMIC) {
                        var dialog by remember { mutableStateOf(false) }
                        if (dialog) AlertDialog(onDismissRequest={dialog=false}, title={Text("Cosmic dialog")}, text={Text("Readable dialog text on an opaque surface.")}, confirmButton={TextButton(onClick={dialog=false}) { Text("Close dialog") }})
                        ApplicationBackground {
                            Surface(Modifier.fillMaxSize(), color=androidx.compose.ui.graphics.Color.Transparent, contentColor=MaterialTheme.colorScheme.onBackground) {
                            Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement=Arrangement.spacedBy(12.dp)) {
                                PageHeading("Cosmic control surfaces")
                                CompactRecordingWaveform("cosmic-visual", AudioWaveform(1000, FloatArray(201) { if(it%5==0) .8f else .3f }), 400, 1000, true, null, Modifier.testTag("cosmic-waveform")) {}
                                Button(onClick={dialog=true}) { Text("Open dialog") }
                                OutlinedButton(onClick={}) { Text("Secondary action") }
                                Text("Recording warning", color=PracticeTheme.colors.recording)
                                OutlinedTextField("Readable input", {}, label={ Text("Label") })
                            }
                            }
                        }
                    }
                }
            }
            compose.onNodeWithTag("cosmic-waveform").assertIsDisplayed(); shot("waveform-input")
            click("Open dialog"); shot("dialog"); click("Close dialog")
        } catch (failure: Throwable) {
            shot("failure")
            File(folder, "cosmic-failure.txt").writeText(compose.onRoot().printToString())
            throw failure
        }
    }
}
