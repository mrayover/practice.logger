package com.mrayover.practicelogger

import android.content.ContentValues
import android.content.pm.ActivityInfo
import android.net.Uri
import android.provider.MediaStore
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

class MediaLibraryWorkflowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val app get() = compose.activity.application
    private val device get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val vm get() = ViewModelProvider(compose.activity)[MediaLibraryViewModel::class.java]
    private fun node(matcher: SemanticsMatcher): SemanticsNodeInteraction {
        val target = compose.onNode(matcher)
        try { return target.assertIsDisplayed() } catch (_: AssertionError) { }
        compose.onNodeWithTag("media-library-list").performScrollToNode(matcher)
        return target.assertIsDisplayed()
    }
    private fun tag(name: String) = node(hasTestTag(name))
    private fun click(text: String) = node(hasText(text)).performClick()
    private fun orient(requested: Int, expected: Int) {
        compose.runOnIdle { compose.activity.requestedOrientation = requested }
        compose.waitUntil(15000) {
            compose.activity.resources.configuration.orientation == expected &&
                compose.onAllNodesWithTag("media-library-list").fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()
        }
        compose.waitForIdle()
    }
    private fun shot(name: String) {
        val dir = File(app.getExternalFilesDir(null), "l1-native").apply { mkdirs() }
        device.takeScreenshot(File(dir, "$name.png"))
    }
    private fun media(name: String, mime: String, bytes: ByteArray): Uri {
        val modern = android.os.Build.VERSION.SDK_INT >= 29
        if (!modern) {
            device.executeShellCommand("pm grant ${app.packageName} android.permission.READ_EXTERNAL_STORAGE")
            device.executeShellCommand("pm grant ${app.packageName} android.permission.WRITE_EXTERNAL_STORAGE")
        }
        val collection = if (modern) MediaStore.Downloads.EXTERNAL_CONTENT_URI else MediaStore.Files.getContentUri("external")
        val uri = app.contentResolver.insert(collection, ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name); put(MediaStore.MediaColumns.MIME_TYPE, mime)
            if (!modern) {
                val downloads = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS).apply { mkdirs() }
                @Suppress("DEPRECATION") put(MediaStore.MediaColumns.DATA, File(downloads, name).absolutePath)
            }
        })!!
        app.contentResolver.openOutputStream(uri)!!.use { it.write(bytes) }; return uri
    }
    private fun finishImport(expected: Int) {
        compose.waitUntil(25000) { !vm.busy && vm.assets.value.size == expected }
    }
    @Test fun importSearchFilterSortRenameCancelDeleteRotationAndDurableFiles() {
        compose.waitUntil(15000) { vm.ready }
        compose.waitUntil(15000) { compose.onAllNodesWithTag("home-menu").fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty() }
        compose.onNodeWithText("Library").scrollIfNeeded().performClick()
        node(hasText("No files imported yet.", substring = true)).assertIsDisplayed()
        shot("empty")
        // Real document picker cancellation and success.
        tag("media-import").performClick()
        assertNotNull(device.wait(Until.findObject(By.desc("Show roots")), 10000)); device.pressBack()
        assertTrue(vm.assets.value.isEmpty())
        val png = media("l1-image.png", "image/png", l1Png())
        tag("media-import").performClick()
        device.wait(Until.findObject(By.desc("Show roots")), 10000)!!.click()
        assertTrue(device.wait(Until.hasObject(By.text("Downloads")), 10000))
        device.findObjects(By.text("Downloads")).last().click()
        device.wait(Until.findObject(By.text("l1-image.png")), 10000)!!.click()
        finishImport(1); app.contentResolver.delete(png, null, null)
        val originals = linkedMapOf<String, ByteArray>("l1-image.png" to l1Png())
        // Remaining formats use the same URI import entry point with actual platform validators.
        for ((name, mime, bytes) in listOf(
            Triple("l1-score.pdf", "application/pdf", l1Fixture("references/score.pdf")),
            Triple("l1-backing.mp3", "audio/mpeg", l1Fixture("audio/backing.mp3")),
            Triple("l1-backing.m4a", "audio/mp4", l1Fixture("audio/backing.m4a"))
        )) {
            val uri = media(name, mime, bytes); compose.runOnIdle { vm.import(uri) }
            finishImport(originals.size + 1); originals[name] = bytes; app.contentResolver.delete(uri, null, null)
        }
        val db = BlockDatabase.open(app)
        try {
            val wave = TakeRepository(db, app).audioFile(runBlocking { db.takes().readAll().first { it.kind == TakeKind.RECORDED_LOOP } }).readBytes()
            val wav = media("l1-backing.wav", "audio/wav", wave); compose.runOnIdle { vm.import(wav) }; finishImport(5)
            originals["l1-backing.wav"] = wave; app.contentResolver.delete(wav, null, null)
            val ids = vm.assets.value.map { it.id }.toSet()
            val bad = media("l1-bad.png", "image/png", "invalid".toByteArray()); compose.runOnIdle { vm.import(bad) }
            compose.waitUntil(15000) { !vm.busy }; assertEquals(ids, vm.assets.value.map { it.id }.toSet()); app.contentResolver.delete(bad, null, null)
            assertNotNull(vm.message)
            val pdf = vm.assets.value.single { it.category == "PDF" }
            click("PDFs")
            tag("media-item-${pdf.id}").assertIsDisplayed()
            assertTrue(compose.onAllNodesWithText("l1-score.pdf").fetchSemanticsNodes().isNotEmpty())
            assertEquals(0, compose.onAllNodesWithText("l1-image.png").fetchSemanticsNodes().size)
            click("Audio"); click("Name"); shot("audio-filter")
            click("All")
            tag("media-search").performTextInput("SCORE")
            tag("media-rename-${pdf.id}").performClick()
            compose.onNodeWithTag("media-rename-title").performTextReplacement("Renamed score")
            click("Save title"); compose.waitUntil(15000) { !vm.busy && vm.assets.value.any { it.title == "Renamed score" } }
            val file = File(app.filesDir, "media-library/${pdf.fileName}")
            assertArrayEquals(originals["l1-score.pdf"], file.readBytes())
            tag("media-search").performTextClearance()
            compose.onNodeWithTag("media-search").performTextInput("Renamed score")
            tag("media-delete-${pdf.id}").performClick(); click("Cancel")
            assertTrue(file.exists())
            compose.activityRule.scenario.recreate()
            compose.waitUntil(15000) { compose.onAllNodesWithTag("media-library-list").fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty() }
            tag("media-search").assertTextContains("Renamed score")
            tag("media-delete-${pdf.id}").performClick(); click("Delete item")
            compose.waitUntil(15000) { !vm.busy && vm.assets.value.size == 4 }; assertFalse(file.exists())
            tag("media-search").performTextClearance()
            orient(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, android.content.res.Configuration.ORIENTATION_LANDSCAPE)
            tag("media-import").assertIsDisplayed(); shot("landscape")
            orient(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, android.content.res.Configuration.ORIENTATION_PORTRAIT)
            shot("managed")
            val hashes = org.json.JSONObject()
            runBlocking { db.media().readAll() }.forEach { asset ->
                val source = originals[asset.originalName]!!
                assertArrayEquals(source, File(app.filesDir, "media-library/${asset.fileName}").readBytes())
                hashes.put(asset.id, l1Hash(File(app.filesDir, "media-library/${asset.fileName}")))
            }
            File(app.filesDir, "l1-canonical-manifest.json").writeText(hashes.toString())
            val legacy = org.json.JSONObject(File(app.filesDir, "l1-legacy-manifest.json").readText()).getJSONObject("hashes")
            legacy.keys().forEach { assertEquals(legacy.getString(it), l1Hash(File(app.filesDir, it))) }
        } finally { db.close() }
    }
}

class MediaLibraryReopenTest {
    @Test fun processRestartRetainsCanonicalAndLegacyBytes(): Unit = runBlocking {
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        val db = BlockDatabase.open(app)
        try {
            val repo = MediaLibraryRepository(db, File(app.filesDir, "media-library")); assertEquals(0, repo.recover())
            val hashes = org.json.JSONObject(File(app.filesDir, "l1-canonical-manifest.json").readText())
            val assets = db.media().readAll(); assertEquals(4, assets.size)
            assets.forEach { assertTrue(repo.available(it)); assertEquals(hashes.getString(it.id), l1Hash(File(app.filesDir, "media-library/${it.fileName}"))) }
            val legacy = org.json.JSONObject(File(app.filesDir, "l1-legacy-manifest.json").readText()).getJSONObject("hashes")
            TakeRepository(db, app).recover(); ImageReferenceRepository(db, File(app.filesDir, "block-images")).recover()
            legacy.keys().forEach { assertEquals(legacy.getString(it), l1Hash(File(app.filesDir, it))) }
            assertEquals(5, db.takes().readAll().size); assertEquals(2, db.images().readAll().size)
            try { BackupRepository(db).restore(BackupRepository(db).snapshot()); fail("Merged Library") } catch (_: NonEmptyRestoreException) { }
        } finally { db.close() }
    }
}
