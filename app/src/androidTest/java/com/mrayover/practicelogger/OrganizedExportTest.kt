package com.mrayover.practicelogger

import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.io.IOException
import java.util.UUID
import java.util.regex.Pattern

class OrganizedExportTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun localTreeExportsAndRecoversWithoutChangingSource() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val device = UiDevice.getInstance(instrumentation)
        val context = instrumentation.targetContext
        val storage = ExportStorage(context)
        val directory = "S1-" + UUID.randomUUID().toString().take(8)
        val downloads = "/sdcard/Download/$directory"
        val documents = "/sdcard/Documents/$directory"
        device.executeShellCommand("mkdir -p $downloads $documents")
        val source = File.createTempFile("export-source", ".bin", context.cacheDir)
        val bytes = ByteArray(9000) { (it % 251).toByte() }
        source.writeBytes(bytes)
        try {
            compose.onNodeWithContentDescription("Settings").performClick()
            compose.onNodeWithText("Choose Folder").scrollIfNeeded().performClick()
            val entry = By.text(directory)
            val documentsFolder = By.text("Documents")
            check(device.wait(Until.hasObject(entry), 1000) ||
                device.wait(Until.hasObject(documentsFolder), 3000) ||
                device.wait(Until.hasObject(By.desc("Show roots")), 5000)) { "Local folder picker did not open" }
            if (!device.hasObject(entry)) {
                if (device.hasObject(documentsFolder)) {
                    device.findObjects(documentsFolder).last().click()
                } else {
                    device.findObject(By.desc("Show roots")).click()
                    checkNotNull(device.wait(Until.findObject(By.text("Downloads")), 10000))
                    device.findObjects(By.text("Downloads")).last().click()
                }
            }
            checkNotNull(device.wait(Until.findObject(entry), 10000)) { "Fixture folder is not visible" }.click()
            checkNotNull(device.wait(Until.findObject(By.text(Pattern.compile("(?i)(use this folder|select)"))), 10000)).click()
            device.wait(Until.findObject(By.text(Pattern.compile("(?i)allow"))), 3000)?.click()
            compose.waitUntil(10000) { storage.selected() != null }
            val destination = checkNotNull(storage.selected())
            compose.activityRule.scenario.recreate()
            assertEquals(destination, ExportStorage(context).selected())
            runBlocking {
                assertTrue(storage.accessible(destination))
                val block = UUID.randomUUID().toString()
                val take = BlockTake(UUID.randomUUID().toString(), block, 1_730_750_400_000L,
                    durationMs = 1000, fileName = "fixture.m4a", label = "Warmup")
                val date = exportDate(take)
                suspend fun copy(item: BlockTake, name: String = "Same Block") =
                    storage.export(destination, item, name, source) { output -> source.inputStream().use { it.copyTo(output) } }
                val first = copy(take)
                val repeated = copy(take, "Renamed Block")
                val secondBlock = take.copy(id = UUID.randomUUID().toString(), blockId = UUID.randomUUID().toString())
                assertTrue(copy(secondBlock).contains("Same Block - ${secondBlock.blockId}/$date/"))
                assertTrue(first.contains("Recordings/Same Block - $block/$date/"))
                assertTrue(repeated.contains("Same Block - $block/$date/"))
                assertNotEquals(first, repeated)
                assertTrue(repeated.endsWith(" (2).m4a"))
                assertTrue(copy(take.copy(blockId = null)).contains("Free Recordings/$date/"))
                val loop = take.copy(id = UUID.randomUUID().toString(), mode = "LOOP", fileName = "fixture.wav",
                    recordedBpm = 120, beatCount = 64)
                assertTrue(copy(loop).contains("64beats 120bpm"))
                for (extension in listOf("mp3", "m4a", "wav")) {
                    val imported = take.copy(id = UUID.randomUUID().toString(), mode = "IMPORTED_LOOP", fileName = "fixture.$extension")
                    val relativePath = copy(imported).substringAfter('/')
                    val tree = destination.uri
                    var id = DocumentsContract.getTreeDocumentId(tree)
                    for (segment in relativePath.split('/')) {
                        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, id)
                        id = checkNotNull(context.contentResolver.query(children,
                            arrayOf(Document.COLUMN_DOCUMENT_ID, Document.COLUMN_DISPLAY_NAME), null, null, null)).use { cursor ->
                            val ids = mutableListOf<String>()
                            while (cursor.moveToNext()) {
                                if (cursor.getString(1) == segment) ids.add(cursor.getString(0))
                            }
                            ids.single()
                        }
                    }
                    val uri = DocumentsContract.buildDocumentUriUsingTree(tree, id)
                    val actual = checkNotNull(context.contentResolver.openInputStream(uri)).use { it.readBytes() }
                    assertArrayEquals(bytes, actual)
                }
                try {
                    storage.export(destination, take, "Same Block", source) { output ->
                        output.write(bytes, 0, 7)
                        throw IOException("Injected write failure")
                    }
                    fail("Partial write succeeded")
                } catch (failure: IllegalStateException) {
                    assertTrue(failure.message.orEmpty().contains("Incomplete copy removed"))
                }
                assertNull(storage.unfinishedCopy())
                assertTrue(copy(take).endsWith(" (3).m4a"))
                assertArrayEquals(bytes, source.readBytes())
                try {
                    storage.choose(Uri.parse("content://missing/tree/test"), Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    fail("Read-only grant accepted")
                } catch (_: IllegalArgumentException) {
                    assertEquals(destination, storage.selected())
                }
                source.delete()
                try {
                    copy(take)
                    fail("Missing source succeeded")
                } catch (failure: IllegalStateException) {
                    assertTrue(failure.message.orEmpty().contains("Saved audio is unavailable"))
                }
                storage.forget()
                assertNull(ExportStorage(context).selected())
                assertFalse(storage.accessible(destination))
            }
        } finally {
            runBlocking { storage.forget() }
            source.delete()
        }
    }
}