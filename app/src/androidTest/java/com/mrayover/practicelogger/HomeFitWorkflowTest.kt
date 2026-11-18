package com.mrayover.practicelogger

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class HomeFitWorkflowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun referenceHomeFitsAtNormalTextAndRemainsReachableAtLargeText() {
        val device=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val folder=File(compose.activity.getExternalFilesDir(null),"home-preview").apply { mkdirs() }
        val goals=(1..2).map { GoalBundle(Goal("fit-$it", "A very long musical Goal name available in full when opened $it", progress=45, priority=it-1, createdAt=1,updatedAt=1), emptyList()) }
        var clicks=0
        val navigation = listOf("Practice Now", "Blocks", "Routines", "Tools", "Recording", "Log", "Library")
        fun evidence(case: String, stage: String) {
            android.util.Log.i("HomeGeometry", "Collecting $case $stage")
            val tags = listOf("home-brand", "header-settings", "home-scroll", "home-goals", "home-goal-title-0",
                "home-goal-title-1", "weekly-target-summary", "home-statistics") + navigation.map { "home-nav-$it" }
            val scroll = compose.onNodeWithTag("home-scroll").fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange]
            File(folder, "$case-$stage.txt").writeText(buildString {
                appendLine("scroll=${scroll.value()} / ${scroll.maxValue()}")
                tags.forEach { tag ->
                    appendLine("$tag=${compose.onNodeWithTag(tag, true).getUnclippedBoundsInRoot()}")
                }
                val bar = compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo), true)
                if (bar.fetchSemanticsNodes().isNotEmpty()) appendLine("bar=${bar.onFirst().getUnclippedBoundsInRoot()}")
            })
            android.util.Log.i("HomeGeometry", "Capturing $case $stage")
            assertTrue("Android screenshot for $case $stage", device.takeScreenshot(File(folder, "$case-$stage.png")))
        }
        fun mount(width: Int, height: Int, scale: Float, theme: ThemeChoice, target: Int?) {
            compose.runOnIdle {
                compose.activity.setContent {
                    CompositionLocalProvider(LocalDensity provides Density(2f,scale), LocalGoalIndicators provides GoalIndicators(goals)) {
                        Box(Modifier.width(width.dp).height(height.dp).testTag("home-fit-viewport")) {
                            PracticeTheme(theme) { ApplicationBackground {
                                androidx.compose.material3.Surface(color=pageBackgroundColor(),contentColor=androidx.compose.material3.MaterialTheme.colorScheme.onBackground) {
                                    Column(Modifier.fillMaxSize().padding(top = 24.dp, bottom = 24.dp)) {
                                        PracticeBanner(onHome={},onSettings={})
                                        PracticeHome(onPractice={clicks++},onBlocks={clicks++},onRoutines={clicks++},onTools={clicks++},onRecording={clicks++},onLog={clicks++},onLibrary={clicks++},goals=goals,onGoals={clicks++},onGoal={clicks++},onStatistics={clicks++},
                                            weeklyProgress=WeeklyPlanProgress(PracticeMinutes(120_000, target?.toLong() ?: 0),
                                                goals.associate { it.goal.id to if (scale > 1f) PracticeMinutes(602_345_000_000, 5_234_567_890)
                                                    else PracticeMinutes(60_000, 30) }), onWeekPlan={clicks++})
                                    }
                                }
                            } }
                        }
                    }
                }
            }
            compose.onNodeWithTag("home-scroll").assertExists()
        }
        try {
            for ((width,height,theme,target) in listOf(
                Case(360,800,ThemeChoice.COSMIC,60), Case(411,886,ThemeChoice.LIGHT,null), Case(411,886,ThemeChoice.COSMIC,60))) {
                mount(width,height,1f,theme,target)
                val scroll=compose.onNodeWithTag("home-scroll").fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange]
                assertEquals("Home opens at the top",0f,scroll.value(),0f)
                evidence("home-fit-$width-$height-${theme.name.lowercase()}", "opening")
                for (i in 0..1) {
                    val layouts=mutableListOf<TextLayoutResult>()
                    compose.onNodeWithTag("home-goal-title-$i",true).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
                    assertEquals(1,layouts.single().lineCount)
                    assertTrue("Title ink stays within its line", layouts.single().getLineBottom(0) <= layouts.single().size.height + 1f)
                }
                compose.onNodeWithTag("home-goal-progress-fit-1").assertDoesNotExist()
                compose.onNodeWithText("Total practice").assertDoesNotExist()
                val view=compose.onNodeWithTag("home-scroll").fetchSemanticsNode().boundsInRoot
                for (label in listOf("Practice Now","Blocks","Routines","Tools","Recording","Log","Library")) {
                    val node=compose.onNodeWithTag("home-nav-$label").performScrollTo().assertIsDisplayed()
                    val bounds=node.fetchSemanticsNode().boundsInRoot
                    assertTrue("$label fully within Home", bounds.top>=view.top && bounds.bottom<=view.bottom)
                    assertTrue("Full width navigation", bounds.width >= view.width - 65f)
                    assertTrue("Comfortable touch height", bounds.height >= 112f)
                    node.performClick()
                }
                compose.onNodeWithTag("home-statistics").performScrollTo().assertIsDisplayed()
                    .performClick()
                compose.onNodeWithTag("home-weekly-target").performScrollTo().assertIsDisplayed().performClick()
                compose.onNodeWithText("Edit").performScrollTo().assertIsDisplayed().performClick()
                compose.onNodeWithTag("home-goal-title-0",true).performScrollTo().performTouchInput { click() }
                compose.onNodeWithTag("home-goal-title-1",true).performScrollTo().performTouchInput { click() }
                compose.onNodeWithTag("home-weekly-target").performScrollTo().assertIsDisplayed()
                val bar = compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
                if (bar.fetchSemanticsNodes().isNotEmpty()) bar.onFirst().performScrollTo()
                evidence("home-fit-$width-$height-${theme.name.lowercase()}", "footer")
            }
            assertEquals(36,clicks)
            mount(320,640,2f,ThemeChoice.COSMIC,60)
            evidence("home-fit-320-200", "opening")
            for(i in 0..1) {
                val layouts=mutableListOf<TextLayoutResult>()
                compose.onNodeWithTag("home-goal-title-$i",true).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
                assertEquals(1,layouts.single().lineCount)
            }
            run {
                val lines = mutableListOf<TextLayoutResult>()
                compose.onNodeWithTag("home-goal-minutes-0", true).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(lines) }
                assertEquals(1, lines.single().lineCount)
                assertFalse(lines.single().hasVisualOverflow)
            }

            // Full-width controls grow naturally; enlarged text scrolls without clipping.
            for (label in listOf("Practice Now","Blocks","Routines","Tools","Recording","Log","Library")) {
                val node=compose.onNodeWithTag("home-nav-$label").performScrollTo().assertIsDisplayed()
                val layouts=mutableListOf<TextLayoutResult>()
                compose.onNodeWithText(label,useUnmergedTree=true).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
                assertEquals("Large-text navigation remains one line: $label",1,layouts.single().lineCount)
                val layout=layouts.single()
                val detail="$label size=${layout.size} paragraph=${layout.multiParagraph.width}x${layout.multiParagraph.height} overflow=${layout.hasVisualOverflow} line=${layout.getLineLeft(0)}..${layout.getLineRight(0)} bottom=${layout.getLineBottom(0)}"
                println("HOME_FIT: $detail")
                assertFalse(detail,layout.isLineEllipsized(0))
                // Use rendered line bounds with physical-pixel rounding, as in existing compact UI checks.
                assertTrue(detail,layout.getLineRight(0)-layout.getLineLeft(0)<=layout.size.width+1f)
                assertTrue(detail,layout.getLineBottom(0)<=layout.size.height+1f)
                val textBounds=compose.onNodeWithText(label,useUnmergedTree=true).fetchSemanticsNode().boundsInRoot
                val buttonBounds=node.fetchSemanticsNode().boundsInRoot
                assertTrue(detail,textBounds.left>=buttonBounds.left-1 && textBounds.right<=buttonBounds.right+1 &&
                    textBounds.top>=buttonBounds.top-1 && textBounds.bottom<=buttonBounds.bottom+1)
            }
            compose.onNodeWithTag("home-weekly-target").performScrollTo().assertIsDisplayed()
            compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo)).performScrollTo()
            evidence("home-fit-320-200", "footer")
        } catch(failure:Throwable) {
            device.takeScreenshot(File(folder,"home-fit-failure.png"))
            File(folder,"home-fit-failure.txt").writeText(compose.onRoot().printToString())
            throw failure
        }
    }
    private data class Case(val width:Int,val height:Int,val theme:ThemeChoice,val target:Int?)
}
