package com.mrayover.practicelogger

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

/** RW2 only: actual shared sheets, validated mic reviews and existing promotion workflows. */
class RecordingCoverageWorkflowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val app get() = compose.activity.application
    private val vm get() = ViewModelProvider(compose.activity)[RecordingViewModel::class.java]
    private val db get() = BlockDatabase.appInstance(app)
    private val device get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @Test fun collectionsAndTemporaryFreeLoopAndReferenceReviews() {
        device.executeShellCommand("pm grant ${app.packageName} android.permission.RECORD_AUDIO")
        val repo = TakeRepository(db, app)
        val block = "rw2-${java.util.UUID.randomUUID()}"
        val saved = runBlocking {
            db.blocks().insert(Block(block, "RW2 collection", "Play", createdAt = 1, updatedAt = 1))
            listOf(block, null).flatMap { owner ->
                (1..2).map {
                    val draft = repo.prepare(owner)
                    draft.file.writeBytes(InstrumentationRegistry.getInstrumentation().context.assets.open("audio/backing.m4a").use { it.readBytes() })
                    repo.save(draft)
                }
            }
        }
        compose.waitUntil(15000) { vm.ready }
        val practice = ViewModelProvider(compose.activity)[PracticeViewModel::class.java]
        // Each collection uses the actual shared sheet. Selection alone must never autoplay.
        for (owner in listOf(block, null)) {
            compose.runOnIdle {
                compose.activity.setContent {
                    val density = LocalDensity.current
                    CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                        PracticeTheme { BlockRecordings(owner, "RW2 collection", vm, practice, null, 120, {}, {}, manageLifecycle = false) }
                    }
                }
            }
            compose.runOnIdle { vm.enterBlock(owner); vm.setForeground(true) }
            val choices = saved.filter { it.blockId == owner }
            for (take in choices) {
                compose.onNodeWithTag("take-list").performScrollToNode(hasTestTag("inspect-${take.id}"))
                compose.onNodeWithTag("inspect-${take.id}").performClick()
                compose.runOnIdle { assertNull(vm.playingId); assertNull(vm.preparingId) }
                compose.onNodeWithTag("take-list").performScrollToIndex(0)
                val wave = compose.onNodeWithTag("practice-collection-waveform")
                wave.assertIsDisplayed()
                wave.performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.SetProgress) { it(1000f) }
                compose.runOnIdle { assertEquals(1000L, vm.playbackPosition(take)); assertNull(vm.playingId) }
                compose.onNodeWithTag("collection-play").performClick()
                compose.waitUntil(15000) { vm.playingId == take.id && vm.playbackPosition(take) > 1050 }
                // Scroll away while playing: the single shared waveform may be disposed, ownership stays.
                compose.onNodeWithTag("take-list").performScrollToNode(hasTestTag("inspect-${choices.last().id}"))
                val other = choices.first { it.id != take.id }
                compose.onNodeWithTag("take-list").performScrollToNode(hasTestTag("inspect-${other.id}"))
                compose.onNodeWithTag("inspect-${other.id}").performClick()
                compose.runOnIdle {
                    assertNull(vm.playingId); assertNull(vm.preparingId)
                    val before = vm.committedSeeks
                    vm.seekSaved("collection", take, 2000)
                    assertEquals(before, vm.committedSeeks)
                }
            }
            compose.onNodeWithTag("take-list").performScrollToIndex(0)
            shot(if (owner == null) "rw2-standalone-200" else "rw2-block-200")
            compose.runOnIdle { vm.leaveBlock() }
        }
        reviewSurface(block)
        val baseline = runBlocking { db.takes().readAll() }
        val free = capture(block)
        checkReviewSeeking(free)
        val bytes = repo.audioFile(free).readBytes()
        // Real promotion rollback: same validated pending identity/bytes remain previewable.
        runBlocking { db.openHelper.writableDatabase.execSQL("CREATE TRIGGER rw2_fail_save BEFORE INSERT ON takes BEGIN SELECT RAISE(ABORT, 'RW2 save failure'); END") }
        try {
            compose.runOnIdle { vm.saveReview() }
            compose.waitUntil(15000) { !vm.busy }
            assertSame(free, vm.reviewTake)
            assertArrayEquals(bytes, repo.audioFile(free).readBytes())
            checkReviewSeeking(free)
        } finally { runBlocking { db.openHelper.writableDatabase.execSQL("DROP TRIGGER rw2_fail_save") } }
        compose.runOnIdle { vm.saveReview() }
        compose.waitUntil(15000) { !vm.busy && vm.reviewTake == null }
        val kept = runBlocking { db.takes().find(free.id)!! }
        assertArrayEquals(bytes, repo.audioFile(kept).readBytes())
        compose.runOnIdle {
            val before = vm.committedSeeks
            vm.seekReview(free, 900); vm.play(free)
            assertEquals(before, vm.committedSeeks)
        }
        compose.waitUntil(10000) { !vm.busy }
        compose.runOnIdle { assertNull(vm.playingId); vm.startPhrase(block, PhraseSpec(120, 6, 1)) }
        compose.waitUntil(20000) { vm.reviewTake?.kind == TakeKind.RECORDED_LOOP && !vm.recording && !vm.busy }
        val loop = vm.reviewTake!!
        checkReviewSeeking(loop)
        compose.runOnIdle {
            vm.rerecordReview()
            val before = vm.committedSeeks
            vm.seekReview(loop, 2000)
            assertEquals(before, vm.committedSeeks)
            assertFalse(repo.audioFileOrNull(loop))
        }
        compose.waitUntil(20000) { vm.reviewTake != null && vm.reviewTake?.id != loop.id && !vm.recording && !vm.busy }
        val replacement = vm.reviewTake!!
        compose.runOnIdle { vm.discardReview() }
        compose.runOnIdle {
            val before = vm.committedSeeks
            vm.seekReview(replacement, 1000)
            assertEquals(before, vm.committedSeeks); assertNull(vm.playingId)
        }
        assertEquals(baseline.size + 1, runBlocking { db.takes().readAll().size })

        // Both Reference hosts use the same real dialog and existing Keep ownership.
        for (owner in listOf<String?>(null, block)) {
            compose.runOnIdle {
                vm.leaveBlock()
                compose.activity.setContent { PracticeTheme { RecordReferenceDialog(owner, null, {}, {}, reviewedOrder = emptyList(), recorder = vm) } }
            }
            compose.runOnIdle { vm.enterReference(owner); vm.setForeground(true) }
            val reference = capture(owner, reference = true)
            checkReviewSeeking(reference)
            val original = repo.audioFile(reference).readBytes()
            runBlocking { db.openHelper.writableDatabase.execSQL("CREATE TRIGGER rw2_fail_keep BEFORE INSERT ON media_assets BEGIN SELECT RAISE(ABORT, 'RW2 keep failure'); END") }
            try {
                compose.runOnIdle { vm.keepReference(null, reviewedOrder = emptyList(), append = owner != null) }
                compose.waitUntil(15000) { !vm.busy }
                assertSame(reference, vm.reviewTake)
                assertArrayEquals(original, repo.audioFile(reference).readBytes())
                checkReviewSeeking(reference)
            } finally { runBlocking { db.openHelper.writableDatabase.execSQL("DROP TRIGGER rw2_fail_keep") } }
            compose.runOnIdle { vm.keepReference(null, reviewedOrder = emptyList(), append = owner != null) }
            compose.waitUntil(20000) { !vm.busy && vm.reviewTake == null && vm.keptReferenceId != null }
            val media = runBlocking { db.media().find(vm.keptReferenceId!!)!! }
            assertArrayEquals(original, File(app.filesDir, "media-library/${media.fileName}").readBytes())
            if (owner != null) assertEquals(media.id, runBlocking { db.blockMedia().find(owner, MediaRole.AUDIO.name)!!.mediaId })
            assertNull(runBlocking { db.takes().find(reference.id) })
            compose.runOnIdle { vm.referenceHandled() }
        }
        reviewSurface(block)
        val leaving = capture(block)
        compose.runOnIdle {
            val decoding = CoroutineScope(Dispatchers.Main.immediate).async(start = CoroutineStart.UNDISPATCHED) { vm.reviewWaveform(leaving) }
            vm.previewReview(); vm.leaveBlock()
            assertTrue("Leaving cancels pending review decoding", decoding.isCancelled)
        }
        compose.waitUntil(10000) { !vm.busy }
        compose.runOnIdle { vm.enterBlock(block); vm.setForeground(true); vm.seekReview(leaving, 500); assertNull(vm.reviewTake); assertNull(vm.playingId) }
    }

    private fun reviewSurface(block: String) {
        compose.runOnIdle {
            compose.activity.setContent { PracticeTheme { Column(Modifier.width(320.dp).verticalScroll(rememberScrollState())) { TakeReviewControls(vm, {}) } } }
        }
        compose.runOnIdle { vm.enterBlock(block); vm.setForeground(true) }
    }
    private fun capture(owner: String?, reference: Boolean = false): BlockTake {
        compose.runOnIdle { vm.startCapture(owner, reference) }
        compose.waitUntil(12000) { vm.recording && vm.elapsedMs >= 1700 }
        compose.onNodeWithTag("practice-review-waveform").assertDoesNotExist()
        compose.runOnIdle { vm.finishCapture() }
        compose.waitUntil(15000) { !vm.busy && vm.reviewTake != null }
        return vm.reviewTake!!
    }
    private fun checkReviewSeeking(take: BlockTake) {
        compose.waitUntil(10000) { compose.onAllNodesWithTag("practice-review-waveform").fetchSemanticsNodes().isNotEmpty() }
        compose.runOnIdle { vm.stopPlayback() }
        compose.onNodeWithTag("practice-review-waveform").performScrollTo().assertIsDisplayed()
            .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.SetProgress) { it(500f) }
        compose.runOnIdle {
            assertEquals(500L, vm.playbackPosition(take)); assertNull(vm.playingId)
            val before = vm.committedSeeks
            vm.bindScrubber("collection", take.id)
            vm.seekSaved("collection", take, 900) // The saved-take path stays closed during review.
            vm.seekReview(take.copy(), 900) // Matching fields are insufficient: exact validated object required.
            assertEquals(before, vm.committedSeeks)
            vm.previewReview()
        }
        compose.waitUntil(12000) { vm.playingId == take.id && vm.playbackPosition(take) > 550 }
        compose.runOnIdle { vm.seekReview(take, 1000) }
        compose.waitUntil(5000) { vm.playbackPosition(take) in 950..1600 }
        shot("rw2-review-${take.kind}-${take.blockId ?: "standalone"}")
        compose.runOnIdle { vm.setForeground(false); vm.seekReview(take, 300); vm.setForeground(true); assertNull(vm.playingId); assertNull(vm.preparingId) }
        assertTrue(runBlocking { withContext(Dispatchers.Main) { vm.reviewWaveform(take).peaks.isNotEmpty() } })
        compose.runOnIdle { vm.stopPlayback() }
    }
    private fun TakeRepository.audioFileOrNull(take: BlockTake): Boolean = runCatching { audioFile(take).exists() }.getOrDefault(false)
    private fun shot(name: String) {
        val folder = File(compose.activity.getExternalFilesDir(null), "home-preview").apply { mkdirs() }
        device.takeScreenshot(File(folder, "$name.png"))
    }
}
