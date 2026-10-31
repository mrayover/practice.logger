package com.mrayover.practicelogger

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class MetronomeButtonPulseTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun shown(text: String) = compose.waitUntil(15000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    private fun click(text: String) { shown(text); compose.onNodeWithText(text).scrollIfNeeded().performClick() }
    private fun button(tag: String) = compose.onNodeWithTag(tag).scrollIfNeeded().assertIsDisplayed()
    // Count surface pixels, so changing just text or an outline cannot pass.
    private fun surface(tag: String, color: Color): Boolean {
        val image = button(tag).captureToImage().toPixelMap()
        var matching = 0; var total = 0
        for (y in 0 until image.height step 4) for (x in 0 until image.width step 4) {
            val pixel = image[x,y]; total++
            if (kotlin.math.abs(pixel.red-color.red)<.04f && kotlin.math.abs(pixel.green-color.green)<.04f && kotlin.math.abs(pixel.blue-color.blue)<.04f) matching++
        }
        return matching > total * .45f
    }
    private fun cycles(tag: String, lit: Color, normal: Color) {
        repeat(2) { index ->
            compose.waitUntil(15000) { surface(tag,lit) }
            button(tag).assertTextContains("Stop click")
            val dir = File(compose.activity.getExternalFilesDir(null),"metronome-pulse").apply { mkdirs() }
            // Keep a full-button screenshot of the live beat for visual review.
            val bitmap = button(tag).captureToImage().asAndroidBitmap()
            File(dir,"$tag-$index.png").outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) }
            compose.waitUntil(15000) { surface(tag,normal) }
        }
        button(tag).performClick(); shown("Start click")
        assertTrue(surface(tag,normal))
        Thread.sleep(350)
        assertTrue(surface(tag,normal))
        button(tag).assertTextContains("Start click")
    }
    @Test fun standaloneWholeButtonFollowsLiveBeatsAndStops() {
        shown("Practice Now"); openHomeTools(compose); click("Metronome")
        val vm=ViewModelProvider(compose.activity)[ToolsViewModel::class.java]
        compose.runOnIdle { vm.configureMetronome(vm.metronomeConfig.copy(mode=MetronomeMode.SOUND_FLASH)); vm.applyMetronomeBpm(90) }
        compose.onNodeWithContentDescription("Increase BPM by 1").scrollIfNeeded().performClick()
        compose.runOnIdle { assertEquals(91,vm.bpm) }
        click("Start click"); cycles("tool-metronome-pulse",PracticeCream,PracticeGreen)
        compose.runOnIdle { assertFalse(vm.clicking); assertFalse(vm.metronomePulse.lit); assertTrue(vm.metronomeNonZeroFramesWritten()>0) }
        click("Start click"); shown("Stop click"); click("Stop click"); shown("Start click")
    }
    @Test fun practiceWholeButtonFollowsLiveBeatsAndStops() {
        val db=BlockDatabase.open(compose.activity.application)
        val vm=ViewModelProvider(compose.activity)[PracticeViewModel::class.java]
        try {
            val session=runBlocking {
                val b=BlockRepository(db).save(null,BlockDraft("Button pulse check","Practice","5","90"))
                val repo=SessionRepository(db);repo.addBlock(b);repo.start()
            }
            shown("Practice Now");compose.runOnIdle { vm.openSession(session) };shown("90 BPM")
            compose.runOnIdle { vm.configureMetronome(vm.metronomeConfig.copy(mode=MetronomeMode.SOUND_FLASH)) }
            compose.onNodeWithContentDescription("Increase click BPM by 1").scrollIfNeeded().performClick(); shown("91 BPM")
            click("Start click"); cycles("practice-metronome-pulse",PracticeGreen,PracticeCream)
            compose.runOnIdle { assertFalse(vm.clicking); assertFalse(vm.metronomePulse.lit); assertTrue(vm.metronomeNonZeroFramesWritten()>0) }
            click("Start click");shown("Stop click");click("Stop click");shown("Start click")
        } finally { compose.runOnIdle { vm.stopClick() };db.close() }
    }
}
