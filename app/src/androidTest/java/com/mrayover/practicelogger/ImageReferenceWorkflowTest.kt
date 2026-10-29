package com.mrayover.practicelogger

import android.graphics.Bitmap
import android.util.Base64
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
import java.io.ByteArrayOutputStream
import java.io.ByteArrayInputStream
import java.io.File

class ImageReferenceWorkflowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val device get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private fun shown(text: String) { compose.waitUntil(15000) { compose.onAllNodesWithText(text).fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty() } }
    private fun click(text: String) { shown(text); val node = compose.onNodeWithText(text); if (!node.isDisplayed()) node.performScrollTo(); node.performClick() }
    private fun editor(name: String) {
        compose.onNodeWithTag("block-library-list").performScrollToNode(hasText(name)); click(name); shown("Edit Block")
    }
    private fun downloads() {
        checkNotNull(device.wait(Until.findObject(By.desc("Show roots")), 10000)).click()
        assertTrue(device.wait(Until.hasObject(By.text("Downloads")), 10000))
        device.findObjects(By.text("Downloads")).last().click(); device.waitForIdle()
    }
    @Test fun pickerViewerTimerClickReplacementRemovalAndArchive() {
        val db = BlockDatabase.open(compose.activity.applicationContext)
        try {
            click("Blocks"); click("New Block")
            compose.onNodeWithTag("Name").performScrollTo().performTextInput("Image reference fixture")
            compose.onNodeWithTag("Action").performScrollTo().performTextInput("Study and memorize")
            click("Save"); shown("Block Library"); editor("Image reference fixture")
            val id = runBlocking { db.blocks().readAll().single { it.name == "Image reference fixture" }.id }
            val bitmap = Bitmap.createBitmap(1200, 800, Bitmap.Config.ARGB_8888).apply { eraseColor(android.graphics.Color.GREEN) }
            val jpeg = ByteArrayOutputStream().use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out); out.toByteArray() }; bitmap.recycle()
            val fixture = File(compose.activity.getExternalFilesDir(null), "reference-fixture.jpg")
            fixture.writeBytes(jpeg)
            device.executeShellCommand("mkdir -p /sdcard/Download")
            device.executeShellCommand("cp ${fixture.absolutePath} /sdcard/Download/reference-fixture.jpg")
            val listing = device.executeShellCommand("ls -l /sdcard/Download/reference-fixture.jpg")
            assertTrue("Picker fixture was not copied: $listing", listing.contains(jpeg.size.toString()))
            device.executeShellCommand("am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file:///sdcard/Download/reference-fixture.jpg")
            click("Add reference image"); downloads()
            checkNotNull(device.wait(Until.findObject(By.descStartsWith("reference-fixture.jpg")), 10000)
                ?: device.wait(Until.findObject(By.text("reference-fixture.jpg")), 3000)).click()
            shown("Replace reference image")
            val first = runBlocking { db.images().find(id)!! }
            click("Show reference"); compose.waitUntil(15000) { compose.onAllNodesWithTag("reference-viewer").fetchSemanticsNodes().isNotEmpty() }
            captureStage3("image-editor")
            click("Hide reference"); click("Replace reference image"); downloads()
            repeat(3) { if (device.currentPackageName.contains("documentsui")) device.pressBack() }
            shown("Replace reference image"); assertEquals(first, runBlocking { db.images().find(id) })
            compose.onNodeWithTag("block-delete-action").performScrollTo().performClick(); shown("This Block cannot be deleted yet.")
            compose.onNodeWithText("Remove the image first", substring = true).assertExists(); click("OK")
            click("Cancel"); editor("Image reference fixture"); click("Show reference"); click("Hide reference"); click("Cancel")
            click("Back to Home")
            val session = runBlocking {
                val second = BlockRepository(db).save(null, BlockDraft("Image second fixture", "Without image"))
                SessionRepository(db).let { it.addBlock(id); it.addBlock(second); it.start() }
            }
            val practice = ViewModelProvider(compose.activity)[PracticeViewModel::class.java]
            compose.runOnIdle { practice.openSession(session) }; shown("Start")
            compose.onNodeWithText("Hide reference").assertDoesNotExist()
            val images = ViewModelProvider(compose.activity)[ImageReferenceViewModel::class.java]
            compose.waitUntil(15000) { images.images.value.any { it.blockId == id } }
            click("Start"); compose.waitUntil(15000) { practice.running && !practice.busy }
            compose.runOnIdle { practice.changeWorkingBpm(111); practice.startClick() }
            compose.waitUntil(15000) { practice.clicking }
            val before = runBlocking { db.sessions().find(session)!!.current!!.activeMs }
            repeat(3) {
                click("Show reference")
                compose.waitUntil(15000) { compose.onAllNodesWithTag("reference-viewer").fetchSemanticsNodes().isNotEmpty() }
                compose.onNodeWithTag("reference-viewer").performScrollTo().performTouchInput {
                    pinch(Offset(width * .4f, height * .4f), Offset(width * .6f, height * .6f),
                        Offset(width * .15f, height * .15f), Offset(width * .85f, height * .85f))
                }
                shown("Reset image zoom")
                compose.onNodeWithTag("reference-viewer").performTouchInput { swipe(center, center + Offset(50f, 30f)) }
                if (it == 0) captureStage3("image-focus-zoom")
                click("Reset image zoom"); click("Hide reference")
                compose.onNodeWithTag("reference-viewer").assertDoesNotExist()
                assertTrue(practice.running); assertTrue(practice.clicking); assertEquals(111, practice.workingBpm)
            }
            assertTrue(runBlocking { db.sessions().find(session)!!.current!!.activeMs } > before)
            compose.onNodeWithText("Quick note (optional)").performScrollTo().performTextInput("Reference practice note")
            click("Save Block note"); compose.waitUntil(15000) { !practice.busy }
            click("Complete"); shown("Without image")
            compose.onNodeWithText("Show reference").assertDoesNotExist(); assertFalse(practice.clicking)
            assertEquals("Reference practice note", runBlocking { db.sessions().find(session)!!.ordered.first().note })
            click("Save for later"); click("Blocks"); editor("Image reference fixture")
            click("Remove reference image"); shown("Remove reference image?")
            compose.onNode(hasText("Cancel") and hasAnyAncestor(isDialog())).performClick()
            assertEquals(first, runBlocking { db.images().find(id) })
            click("Remove reference image"); click("Remove image")
            compose.waitUntil(15000) { runBlocking { db.images().find(id) == null } }; shown("Add reference image")
            assertFalse(File(compose.activity.filesDir, "block-images/${first.fileName}").exists())
            // Leave an actual replacement attached to an archived Block for cold-process and upgrade checks.
            val repo = ImageReferenceRepository(db, File(compose.activity.filesDir, "block-images"))
            runBlocking { repo.import(id) { ByteArrayInputStream(jpeg) } }
            shown("Replace reference image")
            val old = runBlocking { db.images().find(id)!! }
            runBlocking { repo.import(id) { ByteArrayInputStream(jpeg) } }
            assertFalse(File(compose.activity.filesDir, "block-images/${old.fileName}").exists())
            click("Archive Block"); click("Archive"); shown("Block Library")
            assertTrue(runBlocking { db.blocks().find(id)!!.archived }); assertNotNull(runBlocking { db.images().find(id) })
            captureStage3("image-archived")
        } catch (failure: Throwable) {
            val out = ByteArrayOutputStream(); device.dumpWindowHierarchy(out); android.util.Log.e("ImageReferenceTest", out.toString("UTF-8"))
            captureStage3("image-failure"); throw failure
        } finally { db.close() }
    }
}

class ImageReferenceReopenTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun coldRestartRetainsReplacementArchivedOwnershipAndUnchangedPracticeData(): Unit = runBlocking {
        val db = BlockDatabase.open(compose.activity.applicationContext)
        try {
            val block = db.blocks().readAll().single { it.name == "Image reference fixture" }
            assertTrue(block.archived)
            val image = db.images().find(block.id)!!
            ImageReferenceRepository(db, File(compose.activity.filesDir, "block-images")).decode(image).recycle()
            val entry = db.sessions().readAll().flatMap { it.ordered }.single { it.sourceBlockId == block.id }
            assertEquals("Reference practice note", entry.note); assertEquals(111, entry.lastUsedBpm); assertTrue(entry.activeMs > 0)
            val data = BackupRepository(db).snapshot(); val encoded = BackupCodec.encode(data)
            assertEquals(data, BackupCodec.decode(encoded)); assertFalse(String(encoded).contains(image.fileName))
        } finally { db.close() }
    }
}
