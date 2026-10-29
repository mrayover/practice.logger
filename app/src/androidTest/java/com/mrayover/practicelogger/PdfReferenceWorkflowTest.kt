package com.mrayover.practicelogger

import android.Manifest
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
import java.io.*

class PdfReferenceWorkflowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val device get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private fun shown(text: String) { compose.waitUntil(15000) { compose.onAllNodesWithText(text).fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty() } }
    private fun click(text: String) {
        if (compose.onAllNodesWithTag("take-list").fetchSemanticsNodes().isNotEmpty() &&
            (compose.onAllNodesWithText(text).fetchSemanticsNodes().isEmpty() || !compose.onAllNodesWithText(text).onLast().isDisplayed())) {
            compose.onNodeWithTag("take-list").performScrollToNode(hasText(text))
        }
        shown(text); val node = compose.onAllNodesWithText(text).onLast(); if (!node.isDisplayed()) node.performScrollTo(); node.performClick() }
    private fun viewer() = compose.onAllNodesWithTag("reference-viewer").onLast()
    private fun rendered() { compose.waitUntil(15000) { compose.onAllNodesWithTag("reference-viewer").fetchSemanticsNodes().isNotEmpty() } }
    private fun zoom() {
        viewer().performScrollTo().performTouchInput { pinch(Offset(width*.4f,height*.4f),Offset(width*.6f,height*.6f),Offset(width*.15f,height*.15f),Offset(width*.85f,height*.85f)) }
        shown("Reset PDF zoom"); viewer().performTouchInput { swipe(center,center+Offset(40f,25f)) }; click("Reset PDF zoom")
    }
    @Test fun actualPickerPdfPracticeAndRecordingContinuity() {
        val app = compose.activity.application
        val db = BlockDatabase.open(app)
        val recordings = ViewModelProvider(compose.activity)[RecordingViewModel::class.java]
        val practice = ViewModelProvider(compose.activity)[PracticeViewModel::class.java]
        try {
            device.executeShellCommand("pm grant ${app.packageName} ${Manifest.permission.RECORD_AUDIO}")
            click("Blocks"); click("New Block")
            compose.onNodeWithTag("Name").performScrollTo().performTextInput("PDF reference fixture")
            compose.onNodeWithTag("Action").performScrollTo().performTextInput("Read score while recording")
            click("Save"); shown("Block Library")
            compose.onNodeWithTag("block-library-list").performScrollToNode(hasText("PDF reference fixture")); click("PDF reference fixture"); shown("Edit Block")
            val id = runBlocking { db.blocks().readAll().single { it.name == "PDF reference fixture" }.id }
            val source = File(app.getExternalFilesDir(null),"score-fixture.pdf")
            InstrumentationRegistry.getInstrumentation().context.assets.open("references/score.pdf").use { input -> source.outputStream().use { input.copyTo(it) } }
            device.executeShellCommand("mkdir -p /sdcard/Download")
            device.executeShellCommand("cp ${source.absolutePath} /sdcard/Download/score-fixture.pdf")
            click("Add image or PDF")
            checkNotNull(device.wait(Until.findObject(By.desc("Show roots")),10000)).click()
            assertTrue(device.wait(Until.hasObject(By.text("Downloads")),10000))
            device.waitForIdle()
            device.findObjects(By.text("Downloads")).last().click(); device.waitForIdle()
            checkNotNull(device.wait(Until.findObject(By.descStartsWith("score-fixture.pdf")),10000) ?: device.wait(Until.findObject(By.text("score-fixture.pdf")),3000)).click()
            shown("Replace reference PDF")
            val pdf = runBlocking { db.images().find(id)!! }; assertEquals("application/pdf",pdf.mimeType)
            assertArrayEquals(source.readBytes(),File(app.filesDir,"block-images/${pdf.fileName}").readBytes())
            click("Show reference"); rendered(); shown("Page 1 of 2")
            zoom(); click("Next page"); shown("Page 2 of 2"); rendered()
            val landscape = viewer().getUnclippedBoundsInRoot(); assertTrue(landscape.right-landscape.left > landscape.bottom-landscape.top)
            click("Previous page"); shown("Page 1 of 2"); rendered(); captureStage3("pdf-editor-portrait")
            click("Hide reference"); click("Replace reference PDF")
            repeat(3) { if (device.currentPackageName.contains("documentsui")) device.pressBack() }
            shown("Replace reference PDF"); assertEquals(pdf,runBlocking { db.images().find(id) })
            click("Remove reference PDF"); shown("Remove reference PDF?")
            compose.onNode(hasText("Cancel") and hasAnyAncestor(isDialog())).performClick(); assertEquals(pdf,runBlocking { db.images().find(id) })
            compose.onNodeWithTag("block-delete-action").performScrollTo().performClick(); shown("This Block cannot be deleted yet.")
            compose.onNodeWithText("Remove the reference first",substring=true).assertExists(); click("OK"); click("Cancel"); click("Back to Home")
            val session = runBlocking { SessionRepository(db).let { it.addBlock(id); it.start() } }
            compose.runOnIdle { practice.openSession(session) }; shown("Start"); click("Start")
            compose.waitUntil(15000) { practice.running && !practice.busy }
            compose.runOnIdle { practice.changeWorkingBpm(111); practice.startClick() }
            compose.waitUntil(15000) { practice.clicking }
            compose.onNodeWithText("Quick note (optional)").performScrollTo().performTextInput("PDF recording practice note")
            click("Save Block note"); compose.waitUntil(15000) { !practice.busy }
            click("Show reference"); rendered(); zoom(); click("Next page"); shown("Page 2 of 2"); click("Hide reference")
            assertTrue(practice.running); assertTrue(practice.clicking); assertEquals(111,practice.workingBpm)
            click("Recordings"); shown("Block recordings")
            compose.waitUntil(15000) { recordings.ready && !recordings.busy }
            click("Record take"); compose.waitUntil(10000) { recordings.recording && recordings.elapsedMs >= 1000 }
            val before = recordings.elapsedMs
            repeat(2) {
                click("Show reference"); rendered(); click("Next page"); shown("Page 2 of 2"); rendered(); zoom()
                compose.onNodeWithText("Stop and save take").assertIsDisplayed()
                assertTrue(recordings.recording); assertFalse(recordings.phrasing)
                assertTrue(practice.running); assertTrue(practice.clicking); assertEquals(111,practice.workingBpm)
                click("Previous page"); shown("Page 1 of 2"); click("Hide reference")
            }
            assertTrue(recordings.elapsedMs > before); captureStage3("pdf-active-recording")
            click("Stop and save take"); compose.waitUntil(15000) { !recordings.busy && recordings.takes.value.any { it.blockId == id } }
            val free = recordings.takes.value.single { it.blockId == id }
            assertTrue(free.durationMs >= before); assertTrue(runBlocking { db.sessions().find(session)!!.current!!.activeMs } > 0)
            compose.onNodeWithTag("play-${free.id}").performScrollTo().performClick()
            compose.waitUntil(15000) { recordings.playingId == free.id }; compose.runOnIdle { recordings.stopPlayback() }
            click("Close recordings"); shown("Recordings"); assertTrue(practice.running)
            click("Recordings"); shown("Block recordings")
            // Real phrase capture/count-in shares the same sheet and viewer; existing phrase tests cover setup UI.
            compose.runOnIdle { practice.stopClick(); recordings.startPhrase(id,PhraseSpec(240,32),true) }
            compose.waitUntil(5000) { recordings.phrasing && recordings.countInBeat > 0 }
            click("Show reference"); rendered(); click("Next page"); shown("Page 2 of 2")
            assertTrue(recordings.recording); assertTrue(recordings.phrasing); assertTrue(practice.running)
            compose.waitUntil(20000) { recordings.takes.value.any { it.blockId == id && it.mode == "LOOP" } && !recordings.recording }
            compose.waitUntil(10000) { recordings.playingId != null }; captureStage3("pdf-phrase-loop")
            compose.runOnIdle { recordings.stopPlayback() }; click("Hide reference")
            // Deliberate dismissal retains the existing synchronous stop/save behavior.
            click("Record take"); compose.waitUntil(10000) { recordings.recording && recordings.elapsedMs >= 1500 }
            click("Close recordings"); compose.waitUntil(15000) { !recordings.recording && !recordings.busy && recordings.takes.value.count { it.blockId == id && it.mode == "FREE" } == 2 }
            assertEquals("PDF recording practice note",runBlocking { db.sessions().find(session)!!.current!!.note })
            click("Save for later"); click("Blocks")
            compose.onNodeWithTag("block-library-list").performScrollToNode(hasText("PDF reference fixture")); click("PDF reference fixture")
            click("Archive Block"); click("Archive"); shown("Block Library")
            assertTrue(runBlocking { db.blocks().find(id)!!.archived }); assertEquals(pdf,runBlocking { db.images().find(id) })
        } catch (failure: Throwable) {
            val out=ByteArrayOutputStream(); device.dumpWindowHierarchy(out); android.util.Log.e("PdfReferenceTest",out.toString("UTF-8"))
            captureStage3("pdf-failure"); throw failure
        } finally { db.close() }
    }
}

class PdfReferenceReopenTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun coldReopenRetainsPdfNotesBpmAndSavedPlayableTakes(): Unit = runBlocking {
        val app=compose.activity.application; val db=BlockDatabase.open(app)
        try {
            val block=db.blocks().readAll().single { it.name == "PDF reference fixture" }; assertTrue(block.archived)
            val pdf=db.images().find(block.id)!!; val repo=ImageReferenceRepository(db,File(app.filesDir,"block-images"))
            val page=repo.loadPage(pdf,1); assertEquals(2,page.pageCount); page.bitmap.recycle()
            val entry=db.sessions().readAll().flatMap { it.ordered }.single { it.sourceBlockId == block.id }
            assertEquals("PDF recording practice note",entry.note); assertEquals(111,entry.lastUsedBpm); assertTrue(entry.activeMs>0)
            val takes=db.takes().readAll().filter { it.blockId == block.id }; assertEquals(3,takes.size)
            val takeRepo=TakeRepository(db,app)
            takes.forEach { takeRepo.audioFile(it) }
            val backup=BackupCodec.encode(BackupRepository(db).snapshot()); assertFalse(String(backup).contains(pdf.fileName))
        } finally { db.close() }
    }
}
