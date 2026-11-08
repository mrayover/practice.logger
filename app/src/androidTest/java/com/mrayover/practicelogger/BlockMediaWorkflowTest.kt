package com.mrayover.practicelogger

import android.Manifest
import android.content.pm.ActivityInfo
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class BlockMediaWorkflowTest {
    val compose = createAndroidComposeRule<MainActivity>()
    @get:Rule val rules: org.junit.rules.TestRule = org.junit.rules.RuleChain.outerRule(compose).around(object : org.junit.rules.TestWatcher() {
        override fun failed(error: Throwable, description: org.junit.runner.Description) {
            runCatching {
                val directory = File(app.getExternalFilesDir(null), "l3-native").apply { mkdirs() }
                device.takeScreenshot(File(directory, "failure.png"))
                device.dumpWindowHierarchy(File(directory, "failure.xml"))
                File(directory, "semantics.txt").writeText(compose.onAllNodes(isRoot(), useUnmergedTree = true).onLast().printToString())
            }
        }
    })
    private val app get() = compose.activity.application
    private val device get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val vm get() = ViewModelProvider(compose.activity)[ImageReferenceViewModel::class.java]
    private val db get() = BlockDatabase.appInstance(app)
    private fun shown(tag: String) = compose.waitUntil(20000) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty() }
    private fun tag(tag: String) = compose.onNodeWithTag(tag).scrollIfNeeded().assertIsDisplayed()
    private fun click(text: String) {
        val inLibrary = compose.onAllNodesWithTag("media-library-list").fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()
        if (inLibrary) {
            val matcher = hasText(text) and hasAnyAncestor(hasTestTag("media-library-list"))
            compose.onNodeWithTag("media-library-list").performScrollToNode(matcher)
            compose.onNode(matcher).scrollIfNeeded().assertIsDisplayed().performClick()
        } else if (text in setOf("Record take", "Show reference", "Hide reference", "Next page", "Previous page", "Reset PDF zoom", "Close recordings") &&
            compose.onAllNodesWithTag("take-list").fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()) {
            val matcher = hasText(text) and hasAnyAncestor(hasTestTag("take-list"))
            compose.onNodeWithTag("take-list").performScrollToNode(matcher)
            compose.onNode(matcher).scrollIfNeeded().assertIsDisplayed().performClick()
        } else compose.onNodeWithText(text).scrollIfNeeded().assertIsDisplayed().performClick()
    }
    private fun page(expected: String) {
        compose.waitUntil(15000) { compose.onAllNodes(hasTestTag("reference-page") and hasText(expected)).fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty() }
        compose.onNode(hasTestTag("reference-page") and hasText(expected)).scrollIfNeeded().assertIsDisplayed().assertTextEquals(expected)
    }
    private fun waitLink(id: String, role: MediaRole, media: String?) {
        compose.waitUntil(20000) { !vm.busy && runBlocking { db.blockMedia().find(id, role.name)?.mediaId == media } }
    }
    private fun choose(role: MediaRole, id: String) {
        tag("block-${role.name}-choose").performClick(); shown("media-library-list")
        compose.onNodeWithTag("media-library-list").performScrollToNode(hasTestTag("media-search"))
        compose.onNodeWithTag("media-search").performTextInput(runBlocking { db.media().find(id)!!.title })
        compose.runOnIdle {
            compose.activity.currentFocus?.clearFocus()
            compose.activity.getSystemService(android.view.inputmethod.InputMethodManager::class.java).hideSoftInputFromWindow(compose.activity.window.decorView.windowToken, 0)
        }
        device.waitForIdle()
        click("Name")
        compose.onNodeWithTag("media-library-list").performScrollToNode(hasTestTag("media-choose-$id"))
        compose.onNodeWithTag("media-choose-$id").assertIsDisplayed().performClick()
    }
    private fun editor(name: String) {
        compose.onNodeWithTag("block-library-list").performScrollToNode(hasText(name)); click(name)
        shown("block-VISUAL-choose")
    }
    private fun shot(name: String) { val dir = File(app.getExternalFilesDir(null), "l3-native").apply { mkdirs() }; device.takeScreenshot(File(dir, "$name.png")) }
    @Test fun independentPickImportLegacyReplacementPracticeAndRecordingContinuity() {
        val (a, b, pdf, audio, image) = runBlocking {
            val a = BlockRepository(db).save(null, BlockDraft("L3 editor A", "Read shared score", "5", "120"))
            val b = BlockRepository(db).save(null, BlockDraft("L3 editor B", "Read shared score", "5", "120"))
            val pdf = vm.library.import("L3 Score.pdf", null) { l1Fixture("references/score.pdf").inputStream() }
            val audio = vm.library.import("L3 Audio.wav", null) { l1Fixture("audio/backing.wav").inputStream() }
            val image = vm.library.import("L3 Photo.png", null) { l1Png().inputStream() }
            listOf(a, b, pdf.id, audio.id, image.id)
        }
        val originals = runBlocking { db.media().readAll().associate { it.id to l1Hash(File(app.filesDir, "media-library/${it.fileName}")) } }
        val legacy = runBlocking { vm.repository.import(b) { l1Png().inputStream() } }
        val legacyFile = File(app.filesDir, "block-images/${legacy.fileName}"); val legacyHash = l1Hash(legacyFile)
        shown("home-menu"); click("Blocks"); editor("L3 editor A")
        tag("block-VISUAL-choose").performClick(); shown("media-library-list")
        compose.onNodeWithTag("media-choose-$audio").assertDoesNotExist()
        click("Cancel selection"); waitLink(a, MediaRole.VISUAL, null)
        choose(MediaRole.VISUAL, pdf); waitLink(a, MediaRole.VISUAL, pdf)
        choose(MediaRole.AUDIO, audio); waitLink(a, MediaRole.AUDIO, audio)
        tag("block-AUDIO-choose").performClick(); shown("media-library-list")
        compose.onNodeWithTag("media-choose-$pdf").assertDoesNotExist()
        compose.onNodeWithTag("media-library-list").performScrollToNode(hasTestTag("media-selected-$audio"))
        compose.onNodeWithTag("media-selected-$audio").assertExists(); click("Cancel selection"); waitLink(a, MediaRole.AUDIO, audio)
        choose(MediaRole.VISUAL, image); waitLink(a, MediaRole.VISUAL, image); waitLink(a, MediaRole.AUDIO, audio)
        val imageAsset = runBlocking { db.media().find(image)!! }
        val imageFile = File(app.filesDir, "media-library/${imageAsset.fileName}"); val held = File(imageFile.parentFile, "l3-held-original")
        assertTrue(imageFile.renameTo(held))
        try {
            click("Show reference")
            compose.waitUntil(15000) { compose.onAllNodesWithText("Retry reference").fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty() }
            assertTrue(held.renameTo(imageFile)); click("Retry reference"); shown("reference-viewer"); click("Hide reference")
        } finally { if (held.exists()) held.renameTo(imageFile) }
        tag("block-VISUAL-remove").performClick(); click("Remove reference"); waitLink(a, MediaRole.VISUAL, null); waitLink(a, MediaRole.AUDIO, audio)
        // Actual system document picker: cancel preserves audio, successful import links only the visual.
        tag("block-AUDIO-import").performClick(); assertNotNull(device.wait(Until.findObject(By.desc("Show roots")), 10000)); device.pressBack()
        waitLink(a, MediaRole.AUDIO, audio)
        val source = File(app.getExternalFilesDir(null), "l3-picker.pdf").apply { writeBytes(l1Fixture("references/score.pdf")) }
        device.executeShellCommand("mkdir -p /sdcard/Download")
        device.executeShellCommand("cp ${source.absolutePath} /sdcard/Download/l3-picker.pdf")
        tag("block-VISUAL-import").performClick()
        checkNotNull(device.wait(Until.findObject(By.desc("Show roots")), 10000)).click()
        assertTrue(device.wait(Until.hasObject(By.text("Downloads")), 10000)); device.findObjects(By.text("Downloads")).last().click(); device.waitForIdle()
        checkNotNull(device.wait(Until.findObject(By.descStartsWith("l3-picker.pdf")), 10000) ?: device.wait(Until.findObject(By.text("l3-picker.pdf")), 3000)).click()
        compose.waitUntil(25000) { !vm.busy && runBlocking { db.blockMedia().find(a, "VISUAL")?.mediaId?.let { it != image && it != pdf } == true } }
        val imported = runBlocking { db.media().find(db.blockMedia().find(a, "VISUAL")!!.mediaId)!! }
        assertArrayEquals(source.readBytes(), File(app.filesDir, "media-library/${imported.fileName}").readBytes()); waitLink(a, MediaRole.AUDIO, audio)
        val audioSource = File(app.getExternalFilesDir(null), "l3-picker.wav").apply { writeBytes(l1Fixture("audio/backing.wav")) }
        device.executeShellCommand("cp ${audioSource.absolutePath} /sdcard/Download/l3-picker.wav")
        tag("block-AUDIO-import").performClick()
        checkNotNull(device.wait(Until.findObject(By.desc("Show roots")), 10000)).click()
        assertTrue(device.wait(Until.hasObject(By.text("Downloads")), 10000)); device.findObjects(By.text("Downloads")).last().click(); device.waitForIdle()
        checkNotNull(device.wait(Until.findObject(By.descStartsWith("l3-picker.wav")), 10000) ?: device.wait(Until.findObject(By.text("l3-picker.wav")), 3000)).click()
        compose.waitUntil(25000) { !vm.busy && runBlocking { db.blockMedia().find(a, "AUDIO")?.mediaId?.let { it != audio } == true } }
        val importedAudio = runBlocking { db.media().find(db.blockMedia().find(a, "AUDIO")!!.mediaId)!! }
        assertArrayEquals(audioSource.readBytes(), File(app.filesDir, "media-library/${importedAudio.fileName}").readBytes())
        choose(MediaRole.VISUAL, pdf); waitLink(a, MediaRole.VISUAL, pdf)
        tag("block-AUDIO-remove").performClick(); click("Remove reference"); waitLink(a, MediaRole.AUDIO, null); waitLink(a, MediaRole.VISUAL, pdf)
        choose(MediaRole.AUDIO, audio); waitLink(a, MediaRole.AUDIO, audio)
        click("Cancel"); editor("L3 editor B")
        click("Show reference"); shown("reference-viewer")
        tag("overlay-tools").performClick(); click("Edit markings")
        tag("overlay-tools").performClick(); click("Focus")
        compose.overlayAction("Place Focus in the center")
        val legacyIdentity = "legacy:${legacy.fileName}"
        compose.waitUntil(15000) { runBlocking { db.overlays().read(b, legacyIdentity).size == 1 } }
        click("Done")
        choose(MediaRole.VISUAL, pdf); click("Keep old reference")
        assertEquals(1, runBlocking { db.overlays().read(b, legacyIdentity).size })
        assertEquals(legacyHash, l1Hash(legacyFile)); assertNotNull(runBlocking { db.images().find(b) })
        choose(MediaRole.VISUAL, pdf); click("Replace old reference"); waitLink(b, MediaRole.VISUAL, pdf)
        assertTrue(runBlocking { db.overlays().read(b, legacyIdentity).isEmpty() })
        assertFalse(legacyFile.exists()); choose(MediaRole.AUDIO, audio); waitLink(b, MediaRole.AUDIO, audio)
        compose.runOnIdle { compose.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
        compose.waitUntil(15000) { compose.activity.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE }
        tag("block-AUDIO-choose").assertIsDisplayed(); shot("editor-landscape")
        compose.runOnIdle { compose.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT }
        compose.waitUntil(15000) { compose.activity.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT }
        tag("block-AUDIO-title").assertTextEquals("L3 Audio.wav"); shot("editor-portrait")
        click("Cancel"); compose.onNodeWithTag("home-brand", useUnmergedTree = true).performClick()
        val practice = ViewModelProvider(compose.activity)[PracticeViewModel::class.java]
        val recordings = ViewModelProvider(compose.activity)[RecordingViewModel::class.java]
        device.executeShellCommand("pm grant ${app.packageName} ${Manifest.permission.RECORD_AUDIO}")
        val session = runBlocking { SessionRepository(db).let { it.addBlock(a); it.start() } }
        compose.runOnIdle { practice.openSession(session) }; prepReadyBlock(compose)
        compose.waitUntil(15000) { !practice.busy && compose.onAllNodesWithText("Start Practice").fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty() }
        click("Start Practice"); compose.waitUntil(15000) { practice.running && !practice.busy }
        compose.onNodeWithTag("practice-reference-toggle").performClick(); shown("reference-viewer")
        compose.onNodeWithText("Next page").scrollIfNeeded().performClick(); page("Page 2 of 2")
        shown("reference-viewer")
        val referenceBounds = compose.onNodeWithTag("reference-viewer").getUnclippedBoundsInRoot()
        assertTrue("PDF retains a usable bitmap viewport at enlarged text", (referenceBounds.bottom - referenceBounds.top).value >= 179f)
        tag("practice-free-record").performClick(); compose.waitUntil(15000) { recordings.recording && recordings.elapsedMs >= 1500 }
        val elapsed = recordings.elapsedMs
        compose.onNodeWithTag("practice-description-toggle").performClick(); compose.onNodeWithTag("practice-reference-toggle").performClick()
        shown("reference-viewer")
        val activeReference = compose.onNodeWithTag("practice-reference-area").getUnclippedBoundsInRoot()
        assertTrue("Reference stays reachable during enlarged-text capture", (activeReference.bottom - activeReference.top).value >= 48f)
        page("Page 2 of 2"); assertTrue(recordings.recording); assertTrue(practice.running)
        compose.onNodeWithText("Previous page").scrollIfNeeded().performClick(); assertTrue(recordings.recording)
        tag("overlay-tools").performClick(); click("Edit markings")
        tag("overlay-tools").performClick(); click("Highlight")
        compose.overlayAction("Place Highlight in the center")
        compose.waitUntil(15000) { runBlocking { db.overlays().read(a, "library:$pdf").singleOrNull()?.page == 0 } }
        click("Done"); assertTrue(recordings.recording); assertTrue(practice.running)
        tag("practice-recording-stop").performClick(); compose.waitUntil(15000) { recordings.reviewTake != null && !recordings.busy }
        assertTrue(recordings.reviewTake!!.durationMs >= elapsed); shot("practice-review")
        acceptTemporaryTake(compose, recordings); leavePracticeForLater(compose)
        click("Blocks"); editor("L3 editor A"); click("Recordings")
        compose.waitUntil(15000) { recordings.ready && !recordings.busy }; compose.onNodeWithTag("take-list").performScrollToNode(hasText("Record take")); click("Record take")
        compose.waitUntil(15000) { recordings.recording && recordings.elapsedMs >= 1500 }
        compose.onNodeWithTag("take-list").performScrollToNode(hasText("Show reference")); click("Show reference"); shown("reference-viewer")
        compose.onNodeWithTag("take-list").performScrollToNode(hasText("Next page")); click("Next page"); assertTrue(recordings.recording)
        compose.onNodeWithTag("take-list").performScrollToNode(hasTestTag("reference-viewer"))
        compose.onNodeWithTag("reference-viewer").performTouchInput { pinch(Offset(width*.4f,height*.4f), Offset(width*.6f,height*.6f), Offset(width*.15f,height*.15f), Offset(width*.85f,height*.85f)) }
        compose.onNodeWithText("Stop and review take").assertIsDisplayed(); assertTrue(recordings.recording); shot("recording-reference")
        click("Stop and review take"); acceptTemporaryTake(compose, recordings)
        for ((id, hash) in originals) assertEquals(hash, l1Hash(File(app.filesDir, "media-library/${runBlocking { db.media().find(id)!! }.fileName}")))
        app.getSharedPreferences("l3-test", 0).edit().putString("a", a).putString("b", b).putString("pdf", pdf).putString("audio", audio).commit()
    }
}

class BlockMediaReopenTest {
    @Test fun processRestartRetainsLinksAndEveryOriginal(): Unit = runBlocking {
        val app = InstrumentationRegistry.getInstrumentation().targetContext; val prefs = app.getSharedPreferences("l3-test", 0)
        val db = BlockDatabase.open(app)
        try {
            val library = MediaLibraryRepository(db, File(app.filesDir, "media-library"))
            for (key in listOf("a", "b")) {
                val id = checkNotNull(prefs.getString(key, null))
                assertEquals(prefs.getString("pdf", null), db.blockMedia().find(id, "VISUAL")!!.mediaId)
                assertEquals(prefs.getString("audio", null), db.blockMedia().find(id, "AUDIO")!!.mediaId)
            }
            assertEquals(0, library.recover()); db.media().readAll().forEach { assertTrue(library.available(it)) }
            val page = library.visualPage(prefs.getString("pdf", null)!!, 1); assertEquals(2, page.pageCount); page.bitmap.recycle()
            assertEquals(2, db.takes().readAll().count { it.blockId == prefs.getString("a", null) })
        } finally { db.close() }
    }
}
