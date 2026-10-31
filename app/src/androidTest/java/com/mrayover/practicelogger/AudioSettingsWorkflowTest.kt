package com.mrayover.practicelogger
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import java.io.File
import androidx.lifecycle.ViewModelProvider
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class AudioSettingsWorkflowTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Test fun settingsPersistAcrossNewReadersAndRecreationWithSharedPracticeToolsDefaults() {
        val app=compose.activity.application;val prefs=AudioPreferences(app)
        try {
            compose.runOnIdle { prefs.saveSound(ClickSound.WOOD);prefs.saveMode(MetronomeMode.FLASH_ONLY);prefs.saveAlarm(AlarmSound.BELL) }
            compose.activityRule.scenario.recreate();compose.waitForIdle()
            compose.runOnIdle {
                val fresh=AudioPreferences(compose.activity.application)
                assertEquals(ClickSound.WOOD,fresh.sound);assertEquals(AlarmSound.BELL,fresh.alarm)
                val tools=ViewModelProvider(compose.activity)[ToolsViewModel::class.java]
                val practice=ViewModelProvider(compose.activity)[PracticeViewModel::class.java]
                assertEquals(ClickSound.WOOD,tools.metronomeConfig.sound);assertEquals(MetronomeMode.FLASH_ONLY,practice.metronomeConfig.mode)
                tools.configureMetronome(tools.metronomeConfig.copy(mode=MetronomeMode.SOUND_ONLY))
                assertEquals(MetronomeMode.SOUND_ONLY,AudioPreferences(app).mode)
            }
            compose.waitUntil(15000) { compose.onAllNodesWithText("Tools").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Tools").scrollIfNeeded().performClick()
            compose.onNodeWithText("Audio Settings").scrollIfNeeded().performClick()
            compose.onNodeWithText("Preview metronome sound").scrollIfNeeded().assertIsDisplayed().performClick()
            compose.onNodeWithText("Preview completion alarm").scrollIfNeeded().assertIsDisplayed().performClick()
            compose.onNodeWithText("Stop preview").scrollIfNeeded().performClick()
            compose.onNodeWithTag("default-count-in").scrollIfNeeded().assertExists()
            val device=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            device.executeShellCommand("settings put system font_scale 2.0")
            compose.activityRule.scenario.recreate();compose.waitForIdle()
            compose.onNodeWithText("Stop preview").scrollIfNeeded().assertIsDisplayed()
            compose.onNodeWithTag("audio-settings-scroll-scrollbar").assertExists()
            val dir=File(compose.activity.getExternalFilesDir(null),"p4-settings");dir.mkdirs()
            device.takeScreenshot(File(dir,"settings-large-text.png"))
            compose.runOnIdle { compose.activity.requestedOrientation=android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
            compose.waitUntil(15000) { compose.activity.resources.configuration.orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE }
            compose.waitForIdle();compose.onNodeWithTag("default-count-in").scrollIfNeeded().assertIsDisplayed()
            device.takeScreenshot(File(dir,"settings-landscape.png"))
        } finally {
            UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).executeShellCommand("settings put system font_scale 1.0")
            compose.runOnIdle { compose.activity.requestedOrientation=android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT }
            compose.runOnIdle { prefs.saveSound(ClickSound.CLASSIC);prefs.saveMode(MetronomeMode.SOUND_FLASH);prefs.saveAlarm(AlarmSound.TWO_TONE) } }
    }
    @Test fun foregroundCountdownAlarmsOnceOffIsSilentAndBackgroundCannotFire() {
        val app=compose.activity.application;val prefs=AudioPreferences(app)
        lateinit var tools:ToolsViewModel;var now=0L
        try {
            compose.runOnIdle {
                tools=ViewModelProvider(compose.activity)[ToolsViewModel::class.java];tools.clockNow={ now }
                tools.enter("timer");tools.setForeground(true);tools.selectMinutes(1);prefs.saveAlarm(AlarmSound.OFF)
                tools.toggleCountdown();now=60001;tools.updateClocks()
                assertFalse(tools.countdownRunning);assertEquals(0L,tools.remainingMs);assertEquals(1,tools.completionSignals);assertEquals(0L,tools.completionFramesWritten())
                repeat(3) { tools.updateClocks() };assertEquals(1,tools.completionSignals)
                prefs.saveAlarm(AlarmSound.BELL);tools.resetCountdown();tools.toggleCountdown();now+=60001;tools.updateClocks()
                assertEquals(2,tools.completionSignals)
            }
            compose.waitUntil(10000) { tools.completionFramesWritten()>0 }
            compose.runOnIdle {
                tools.resetCountdown();tools.toggleCountdown();tools.setForeground(false);now+=60001;tools.updateClocks()
                assertEquals(2,tools.completionSignals);assertFalse(tools.countdownRunning)
                tools.setForeground(true);tools.updateClocks();assertEquals(2,tools.completionSignals);tools.leave()
            }
        } finally { compose.runOnIdle { prefs.saveAlarm(AlarmSound.TWO_TONE) } }
    }
}
