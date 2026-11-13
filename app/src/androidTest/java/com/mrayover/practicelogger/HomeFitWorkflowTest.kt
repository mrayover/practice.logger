package com.mrayover.practicelogger

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.asAndroidBitmap
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
    @Test fun longGoalsAndAllHomeActionsFitAtNormalTextAndRemainReachableAtLargeText() {
        val device=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val folder=File(compose.activity.getExternalFilesDir(null),"home-preview").apply { mkdirs() }
        val goals=(1..2).map { GoalBundle(Goal("fit-$it", "A very long musical Goal name that must stay on exactly one line on Home $it", progress=45, priority=it-1, createdAt=1,updatedAt=1), emptyList()) }
        var clicks=0
        fun mount(width: Int, height: Int, scale: Float, theme: ThemeChoice, target: Int?) {
            compose.runOnIdle {
                compose.activity.setContent {
                    CompositionLocalProvider(LocalDensity provides Density(2f,scale), LocalGoalIndicators provides GoalIndicators(goals)) {
                        Box(Modifier.width(width.dp).height(height.dp).testTag("home-fit-viewport")) {
                            PracticeTheme(theme) { ApplicationBackground {
                                androidx.compose.material3.Surface(color=pageBackgroundColor(),contentColor=androidx.compose.material3.MaterialTheme.colorScheme.onBackground) {
                                    Column(Modifier.fillMaxSize()) {
                                        PracticeBanner(onHome={},onSettings={})
                                        PracticeHome(onPractice={clicks++},onBlocks={clicks++},onRoutines={clicks++},onTools={clicks++},onRecording={clicks++},onLog={clicks++},onLibrary={clicks++},goals=goals,onGoals={clicks++},onGoal={clicks++},practiceMs=60000,onStatistics={clicks++},weeklyProgress=WeeklyTargetProgress(120000,target),onWeekTarget={clicks++})
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
                Case(320,640,ThemeChoice.COSMIC,60), Case(411,731,ThemeChoice.LIGHT,null), Case(360,900,ThemeChoice.COSMIC,60))) {
                mount(width,height,1f,theme,target)
                val scroll=compose.onNodeWithTag("home-scroll").fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange]
                assertEquals("Home must fit $width x $height without scrolling",0f,scroll.maxValue(),1f)
                for (i in 0..1) {
                    val layouts=mutableListOf<TextLayoutResult>()
                    compose.onNodeWithTag("home-goal-title-$i",true).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
                    assertEquals(1,layouts.single().lineCount)
                    assertTrue(layouts.single().isLineEllipsized(0))
                }
                val view=compose.onNodeWithTag("home-fit-viewport").fetchSemanticsNode().boundsInRoot
                for (label in listOf("Practice Now","Blocks","Routines","Tools","Recording","Log","Library")) {
                    val node=compose.onNodeWithTag("home-nav-$label").assertIsDisplayed()
                    val bounds=node.fetchSemanticsNode().boundsInRoot
                    assertTrue("$label fully within Home", bounds.top>=view.top && bounds.bottom<=view.bottom)
                    node.performClick()
                }
                compose.onNodeWithTag("home-statistics").assertIsDisplayed().performClick()
                compose.onNodeWithTag("home-weekly-target").assertIsDisplayed().performClick()
                compose.onNodeWithText("Edit").assertIsDisplayed().performClick()
                compose.onNodeWithTag("home-goal-title-0",true).performTouchInput { click() }
                compose.onNodeWithTag("home-goal-title-1",true).performTouchInput { click() }
                compose.onNodeWithTag("weekly-target-summary").assertIsDisplayed()
                // Capture the Compose root after its rendered frame, avoiding a stale native frame.
                File(folder,"home-fit-$width-$height.png").outputStream().use { output ->
                    compose.onRoot().captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG,100,output)
                }
            }
            assertEquals(36,clicks)
            mount(320,640,2f,ThemeChoice.COSMIC,60)
            for(i in 0..1) {
                val layouts=mutableListOf<TextLayoutResult>()
                compose.onNodeWithTag("home-goal-title-$i",true).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
                assertEquals(1,layouts.single().lineCount)
            }
            File(folder,"home-fit-320-200-goals.png").outputStream().use { output ->
                compose.onRoot().captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG,100,output)
            }
            // Large text switches to full-width labels to avoid mid-word breaks and clipped footer text.
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
            File(folder,"home-fit-320-200.png").outputStream().use { output ->
                compose.onRoot().captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG,100,output)
            }
        } catch(failure:Throwable) {
            device.takeScreenshot(File(folder,"home-fit-failure.png"))
            File(folder,"home-fit-failure.txt").writeText(compose.onRoot().printToString())
            throw failure
        }
    }
    private data class Case(val width:Int,val height:Int,val theme:ThemeChoice,val target:Int?)
}
