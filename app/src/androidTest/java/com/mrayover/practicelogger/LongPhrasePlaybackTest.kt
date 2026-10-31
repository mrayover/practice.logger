package com.mrayover.practicelogger

import android.Manifest
import android.os.Build
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class LongPhrasePlaybackTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Test fun maximum128SecondLoopCapturesOrLoadsAndCrossesNativeLoopBoundary() {
        val app=compose.activity.application;val db=BlockDatabase.open(app);val device=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val audio=ViewModelProvider(compose.activity)[RecordingViewModel::class.java]
        val spec=PhraseSpec(30,64,1)
        fun click(text:String) { compose.waitUntil(15000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() };compose.onNodeWithText(text).scrollIfNeeded().performClick() }
        try {
            device.executeShellCommand("pm grant ${app.packageName} ${Manifest.permission.RECORD_AUDIO}")
            compose.waitUntil(15000) { audio.ready && compose.onAllNodesWithText("Practice Now").fetchSemanticsNodes().isNotEmpty() }
            val block=runBlocking { BlockRepository(db).save(null,BlockDraft("P2 maximum loop","Play")) }
            click("Recording");click("P2 maximum loop")
            val repo=TakeRepository(db,app)
            val take=if(Build.VERSION.SDK_INT>=35) {
                compose.runOnIdle { audio.startPhrase(block,spec) }
                compose.waitUntil(155000) { !audio.recording && !audio.busy && audio.reviewTake!=null }
                assertEquals(128000L,audio.reviewTake!!.durationMs)
                val id=audio.reviewTake!!.id;compose.onNodeWithTag("review-save").scrollIfNeeded().performClick()
                compose.waitUntil(15000) { !audio.busy && audio.reviewTake==null }
                runBlocking { db.takes().find(id)!! }
            } else runBlocking {
                val draft=repo.prepare(block,spec);PhraseWave.write(draft.file,ShortArray(spec.frames) { (it%97).toShort() });repo.save(draft)
            }
            assertEquals(12_288_044L,repo.audioFile(take).length());assertEquals(spec.frames,PhraseWave.frames(repo.audioFile(take)))
            compose.runOnIdle { audio.play(take) }
            compose.waitUntil(150000) { audio.loopFramesPlayed()>spec.frames+4800L }
            assertEquals(take.id,audio.playingId)
            val dir=File(app.getExternalFilesDir(null),"p2-phrase");dir.mkdirs();device.takeScreenshot(File(dir,"maximum-loop-api${Build.VERSION.SDK_INT}.png"))
            println("P2 LONG LOOP PASS api=${Build.VERSION.SDK_INT} frames=${spec.frames} bytes=${repo.audioFile(take).length()} nativeHead=${audio.loopFramesPlayed()}")
            compose.runOnIdle { audio.stopPlayback() };assertNull(audio.playingId)
            compose.onNodeWithTag("take-list").performScrollToNode(hasText("Close recordings"));click("Close recordings")
        } finally { compose.runOnIdle { audio.stopPlayback();if(audio.recording)audio.finishCapture(discard=true) };db.close() }
    }
}
