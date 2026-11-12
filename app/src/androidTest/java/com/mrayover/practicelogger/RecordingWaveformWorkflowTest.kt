package com.mrayover.practicelogger

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class RecordingWaveformWorkflowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun savedWaveformsSeekScrubRepeatSwitchAndBackground() {
        val app = compose.activity.application
        val vm = ViewModelProvider(compose.activity)[RecordingViewModel::class.java]
        val db = BlockDatabase.appInstance(app)
        val repo = TakeRepository(db, app)
        val block = "rw1-${java.util.UUID.randomUUID()}"
        val takes = runBlocking {
            db.blocks().insert(Block(block, "Waveform check", "Play", createdAt=1, updatedAt=1))
            val free = repo.prepare(block)
            free.file.writeBytes(InstrumentationRegistry.getInstrumentation().context.assets.open("audio/backing.m4a").use { it.readBytes() })
            val savedFree = repo.save(free)
            val loops = listOf("mp3", "m4a", "wav").map { ext ->
                val pending = repo.prepareImport(block, ext)
                pending.file.writeBytes(InstrumentationRegistry.getInstrumentation().context.assets.open("audio/backing.$ext").use { it.readBytes() })
                val row = BlockTake(pending.id, block, 1, "IMPORTED_LOOP", 3000, pending.file.name)
                // Native fixture rows preserve the same private ownership/name rules as saved imports.
                db.takes().insert(row)
                row
            }
            val spec = PhraseSpec(120, 6)
            val phrase = repo.prepare(block, spec)
            PhraseWave.write(phrase.file, ShortArray(spec.frames) { (kotlin.math.sin(it * .05) * 12000).toInt().toShort() })
            listOf(savedFree) + loops + repo.save(phrase)
        }
        val selected = mutableStateOf(takes.first())
        compose.runOnIdle {
            vm.enterBlock(block); vm.setForeground(true)
            compose.activity.setContent {
                MaterialTheme { Column { RecordingScrubber(selected.value, "test", vm, true) } }
            }
        }
        compose.waitUntil(15000) { vm.ready }
        compose.runOnIdle {
            // Replacing MainActivity content disposes its normal foreground observer.
            vm.enterBlock(block); vm.setForeground(true)
            vm.play(takes.first())
            vm.bindScrubber("test", takes[1].id)
            selected.value = takes[1]
        }
        compose.waitUntil(10000) { !vm.busy }
        compose.runOnIdle { assertNull(vm.playingId); assertNull(vm.preparingId) }
        for (take in takes) {
            println("RW1 native seek source=${take.kind} file=${take.fileName}")
            compose.runOnIdle { selected.value = take }
            val wave = runBlocking { vm.waveform(take) }
            assertTrue("Nonempty actual amplitudes ${take.fileName}", wave.peaks.any { it > .01f })
            compose.runOnIdle {
                assertNull(vm.playingId)
                vm.seekSaved("test", take, 1000)
                assertEquals(1000L, vm.playbackPosition(take))
                vm.play(take)
            }
            compose.waitUntil(15000) { !vm.busy && vm.playingId == take.id && vm.playbackPosition(take) > 1050 }
            compose.runOnIdle { vm.seekSaved("test", take, 2100) }
            compose.waitUntil(5000) { vm.playbackPosition(take) in 2050..2800 }
            if (take.isLoop) {
                compose.runOnIdle { vm.seekSaved("test", take, take.durationMs - 250) }
                compose.waitUntil(5000) { vm.playbackPosition(take) in 50..1000 }
                assertEquals(take.id, vm.playingId)
            }
            compose.runOnIdle { vm.stopPlayback() }
        }
        val take = takes.last()
        compose.runOnIdle { selected.value = take }
        val node = compose.onNodeWithTag("practice-test-waveform")
        val before = vm.committedSeeks
        node.performTouchInput { down(Offset(width * .2f, height * .5f)); moveTo(Offset(width * .75f, height * .5f)) }
        compose.runOnIdle { assertEquals(before, vm.committedSeeks) }
        assertTrue(node.fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo].current in 2220f..2280f)
        node.performTouchInput { up() }
        compose.runOnIdle { assertEquals(before + 1, vm.committedSeeks); assertTrue(kotlin.math.abs(2250L - vm.playbackPosition(take)) <= 30) }
        node.performTouchInput { click(Offset(width * .4f, height * .5f)) }
        compose.runOnIdle { assertEquals(before + 2, vm.committedSeeks) }
        node.performTouchInput { down(center); moveTo(Offset(width * .8f, center.y)) }
        compose.runOnIdle { selected.value = takes.first() }
        node.performTouchInput { up() }
        compose.runOnIdle {
            assertEquals(before + 2, vm.committedSeeks)
            vm.seekSaved("test", take, 2000)
            assertEquals(before + 2, vm.committedSeeks)
            vm.play(takes.first())
        }
        compose.waitUntil(10000) { vm.playingId == takes.first().id }
        compose.runOnIdle { vm.setForeground(false) }
        compose.runOnIdle { vm.setForeground(true); assertNull(vm.playingId); assertNull(vm.preparingId) }
        runBlocking {
            val missing = takes.first().copy(id="missing", fileName="${java.util.UUID.randomUUID()}.m4a")
            try { vm.waveform(missing); fail("Missing file accepted") } catch (_: Exception) {}
            val slow = launch { vm.waveform(takes.first()) }; slow.cancelAndJoin()
            assertTrue(vm.waveform(takes.last()).peaks.any { it > 0 })
        }
        val corruptFile = File(app.filesDir, "recordings/${java.util.UUID.randomUUID()}.m4a").apply { writeBytes(byteArrayOf(1,2,3)) }
        compose.runOnIdle { selected.value = takes.first().copy(id="corrupt", fileName=corruptFile.name) }
        compose.waitUntil(10000) { compose.onAllNodesWithText("Waveform unavailable").fetchSemanticsNodes().isNotEmpty() }
        compose.runOnIdle { selected.value = takes.first() }
        compose.waitUntil(10000) { compose.onAllNodesWithText("Waveform unavailable").fetchSemanticsNodes().isEmpty() }
        compose.runOnIdle {
            compose.activity.setContent {
                PracticeTheme {
                    Column(Modifier.width(320.dp).verticalScroll(rememberScrollState())) {
                        CompactRecordingControls(block, takes, takes.first(), takes.last(), vm, {}, {}, {})
                    }
                }
            }
        }
        compose.waitUntil(10000) { compose.onAllNodesWithText("Loading waveform…").fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithTag("practice-free-waveform").assertIsDisplayed()
        compose.onNodeWithTag("practice-loop-waveform").assertIsDisplayed()
        val shots = File(compose.activity.getExternalFilesDir(null), "home-preview").apply { mkdirs() }
        androidx.test.uiautomator.UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).takeScreenshot(File(shots, "rw1-compact-320dp.png"))
        compose.runOnIdle { vm.leaveBlock() }
    }
}
