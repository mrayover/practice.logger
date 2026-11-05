package com.mrayover.practicelogger

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import android.os.Process
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.room.withTransaction
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.regex.Pattern

private const val ZIP_NAME = "practice-s2-roundtrip.zip"
private const val EXPECTED = "s2-expected.zip"
private const val TOTAL_TEXT = "Total practice · 1 min 12 sec"

class PortableZipRoundTripTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val context get() = compose.activity.applicationContext
    private val db get() = BlockDatabase.appInstance(context)
    private val device get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @Test fun exportFromSourceThroughLocalSaveAs() {
        runBlocking { seed() }
        openBackup()
        click("Prepare complete ZIP")
        shown("Private ZIP verified with 7 originals.")
        click("Save ZIP as…")
        pickerDownloads()
        checkNotNull(device.wait(Until.findObject(By.clazz("android.widget.EditText")), 10_000)).text = ZIP_NAME
        checkNotNull(device.wait(Until.findObject(By.text(Pattern.compile("save", Pattern.CASE_INSENSITIVE))), 10_000)).click()
        shown("Backup saved and read-back verified")
    }

    @Test fun restoreIntoSeparateEmptyInstallation() {
        assertTrue(runBlocking { db.withTransaction { portableTargetEmpty(db.openHelper.writableDatabase) } })
        openBackup()
        selectZip()
        shown("Restore this complete backup?")
        shown("3 Library assets, 3 saved recordings and 1 legacy visual references.")
        click("Cancel")
        assertTrue(runBlocking { db.withTransaction { portableTargetEmpty(db.openHelper.writableDatabase) } })
        selectZip()
        click("Restore complete backup")
        shown("Complete backup restored.")
        assertTrue(runBlocking { !db.withTransaction { portableTargetEmpty(db.openHelper.writableDatabase) } })
    }

    @Test fun verifyAfterProcessRestart() {
        shown(TOTAL_TEXT)
        val expected = File(context.filesDir, EXPECTED)
        val library = MediaLibraryRepository(db, File(context.filesDir, "media-library"))
        val legacy = ImageReferenceRepository(db, File(context.filesDir, "block-images"))
        val originals = runBlocking {
            val verified = PortableZip(context, db).verify(expected)
            val expectedRecords = verified.records.tables
            db.withTransaction {
                val sql = db.openHelper.writableDatabase
                for (table in portableTables) {
                    assertEquals(table.name, expectedRecords.getJSONArray(table.name).toString(), table.snapshot(sql) {}.toString())
                }
            }
            assertEquals(verified.preferences, PortablePreferences.snapshot(context))
            for (original in verified.originals) {
                val checksum = original.file(context.filesDir).inputStream().use { copyPortable(it, null, original.limit) }
                val expectedEntry = verified.inventory.getValue(original.path)
                assertEquals(original.path, expectedEntry.bytes, checksum.bytes)
                assertEquals(original.path, expectedEntry.sha256, checksum.sha256)
            }
            assertEquals(72_666L, db.transcribeActivities().observePracticeTime().first().sumOf { it.activeMs })
            assertEquals(6000L, db.sessions().find("interrupted")!!.activeMs)
            assertEquals("goal", db.goalAttribution().primaryGoalId("first"))
            assertEquals(PlacementGoalSnapshot("completed-placement", "goal", 3),
                db.goalAttribution().snapshot("completed-placement"))
            assertEquals(PlacementGoalSnapshot("interrupted-placement", null, null),
                db.goalAttribution().snapshot("interrupted-placement"))
            assertNull(db.goalAttribution().snapshot("draft-placement"))
            assertEquals(4000L, db.transcribeActivities().unfinished()!!.activeMs)
            val image = db.media().readAll().single { it.category == "IMAGE" }
            val pdf = db.media().readAll().single { it.category == "PDF" }
            val audio = db.media().readAll().single { it.category == "AUDIO" }
            library.visualPage(image.id, 0).let { assertEquals(1, it.pageCount); it.bitmap.recycle() }
            library.visualPage(pdf.id, 1).let { assertEquals(2, it.pageCount); it.bitmap.recycle() }
            legacy.loadPage(db.images().find("archived")!!, 1).let { assertEquals(2, it.pageCount); it.bitmap.recycle() }
            val media = db.blockMedia().readAll()
            assertEquals(setOf("first", "second"), media.filter { it.mediaId == audio.id }.map { it.blockId }.toSet())
            val masters = db.markers().read(audio.id)
            val localOne = effectiveBlockMarkers(masters, db.blockAudio().read("first", audio.id))
            val localTwo = effectiveBlockMarkers(masters, db.blockAudio().read("second", audio.id))
            assertTrue(localOne.visible.any { it.ownership == BlockMarkerOwnership.CUSTOMIZED })
            assertTrue(localTwo.visible.any { it.ownership == BlockMarkerOwnership.LOCAL })
            assertTrue(localTwo.visible.any { it.ownership == BlockMarkerOwnership.MASTER })
            assertEquals(75, db.blockAudio().state("first", audio.id)!!.speedPercent)
            assertEquals(90, db.blockAudio().state("second", audio.id)!!.speedPercent)
            audio to db.takes().readAll()
        }
        lateinit var recording: RecordingViewModel
        compose.runOnIdle {
            val models = ViewModelProvider(compose.activity)
            assertFalse(models[PracticeViewModel::class.java].running)
            assertFalse(models[TranscribeViewModel::class.java].running)
            recording = models[RecordingViewModel::class.java]
            assertEquals(9L, recording.defaultCountIn)
        }
        for (take in originals.second) {
            compose.runOnIdle { recording.enterBlock(take.blockId); recording.play(take) }
            compose.waitUntil(10_000) { recording.playingId == take.id }
            compose.runOnIdle { recording.leaveBlock() }
        }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        lateinit var controller: LibraryAudioController
        compose.runOnIdle { controller = LibraryAudioController(context, library, scope); controller.open(originals.first.id) }
        try {
            compose.waitUntil(10_000) { !controller.state.value.preparing && controller.state.value.durationMs > 0 }
            compose.runOnIdle { controller.play() }
            compose.waitUntil(10_000) { controller.state.value.playing && controller.state.value.positionMs > 100 }
        } finally {
            compose.runOnIdle { controller.dispose(); scope.cancel() }
        }
        runBlocking {
            db.logs().save(DailyLog("2026-11-05", "An ordinary edit after restore"))
            assertEquals("An ordinary edit after restore", db.logs().readAll().single().text)
            val session = SessionRepository(db)
            assertEquals(7000L, session.checkpoint("interrupted", 1000).activeMs)
            assertEquals(73_666L, db.transcribeActivities().observePracticeTime().first().sumOf { it.activeMs })
        }
        click("Blocks")
        compose.onNodeWithTag("block-library-list").performScrollToNode(hasText("Score study"))
        shown("Priority 1 · Study the piece")
        click("Score study")
        click("Show reference")
        compose.waitUntil(20_000) {
            compose.onAllNodesWithTag("overlay-layer").fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()
        }
        val layer = compose.onNodeWithTag("overlay-layer")
        layer.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "1 markings"))
        click("Next page")
        shown("Page 2 of 2")
        layer.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "1 markings"))
        compose.onNodeWithTag("overlay-tools").scrollIfNeeded().performClick()
        click("Edit markings")
        compose.onNodeWithTag("overlay-tools").scrollIfNeeded().performClick()
        click("Select Cover 1")
        compose.overlayAction("Move marking right")
        compose.waitUntil(15_000) { runBlocking { db.overlays().find("first-cover")?.left?.let { it > .25f } == true } }
    }

    @Test fun verifyRestoredOverlayEditAfterAnotherRestart() {
        runBlocking {
            val shape = db.overlays().find("first-cover")!!
            assertEquals(.275f, shape.left, .000001f)
            assertEquals("first", shape.blockId)
            assertEquals(1, shape.page)
            val verified = PortableZip(context, db).verify(File(context.filesDir, EXPECTED))
            assertEquals(5, db.overlays().count())
            for (original in verified.originals) {
                val actual = original.file(context.filesDir).inputStream().use { copyPortable(it, null, original.limit) }
                assertEquals(verified.inventory.getValue(original.path).sha256, actual.sha256)
            }
        }
    }

    private fun openBackup() {
        openHomeTools(compose)
        click("Backup and restore")
    }

    private fun selectZip() {
        click("Choose complete ZIP to restore")
        pickerDownloads()
        checkNotNull(device.wait(Until.findObject(By.text(ZIP_NAME)), 10_000)).click()
    }

    private fun shown(text: String) {
        compose.waitUntil(30_000) {
            compose.onAllNodesWithText(text, substring = true).fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()
        }
    }

    private fun click(text: String) {
        shown(text)
        compose.onNodeWithText(text).scrollIfNeeded().performClick()
    }

    private suspend fun seed() {
        val source = File(context.cacheDir, "portable-fixture").apply { check(mkdir()) }
        val png = File(source, "sample.png")
        val bitmap = Bitmap.createBitmap(32, 48, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.RED)
        png.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        bitmap.recycle()
        val pdf = File(source, "sample.pdf")
        val doc = PdfDocument()
        try {
            for (index in 1..2) {
                val page = doc.startPage(PdfDocument.PageInfo.Builder(120, 160, index).create())
                page.canvas.drawColor(if (index == 1) Color.WHITE else Color.BLUE)
                doc.finishPage(page)
            }
            pdf.outputStream().use(doc::writeTo)
        } finally { doc.close() }
        val assets = InstrumentationRegistry.getInstrumentation().context.assets
        val m4a = File(source, "backing.m4a")
        assets.open("audio/backing.m4a").use { input -> m4a.outputStream().use { input.copyTo(it) } }
        val library = MediaLibraryRepository(db, File(context.filesDir, "media-library"))
        val legacy = ImageReferenceRepository(db, File(context.filesDir, "block-images"))
        val takes = TakeRepository(db, context)
        val links = BlockMediaRepository(db, library, legacy)
        val first = Block("first", "Score study", "Play from memory", 5, 90, "Original directions", createdAt = 100, updatedAt = 200)
        val second = Block("second", "Listening", "Find the phrase", createdAt = 100, updatedAt = 200)
        val archived = Block("archived", "Archived source", "Keep history", archived = true, createdAt = 100, updatedAt = 200)
        val imageBlock = Block("image", "Image reference", "Read photo", createdAt = 100, updatedAt = 200)
        for (block in listOf(first, second, archived, imageBlock)) db.blocks().insert(block)
        val image = library.import("sample.png", "image/png", open = png::inputStream)
        val score = library.import("sample.pdf", "application/pdf", open = pdf::inputStream)
        val audio = library.importRecordedReference("Microphone reference.m4a", m4a) {}
        links.link(first.id, MediaRole.VISUAL, score.id)
        links.link(second.id, MediaRole.VISUAL, score.id)
        links.link(imageBlock.id, MediaRole.VISUAL, image.id)
        links.link(first.id, MediaRole.AUDIO, audio.id)
        links.link(second.id, MediaRole.AUDIO, audio.id)
        val old = legacy.import(archived.id, pdf::inputStream)
        val overlays = PracticeOverlayRepository(db)
        suspend fun mark(id: String, block: Block, reference: String, page: Int, tool: OverlayTool, pages: Int) {
            val shape = PracticeOverlay(id, block.id, reference, page, tool.name, .25f, .2f, .7f, .65f, 0, 0)
            overlays.edit(OverlayEdit(null, shape), pages)
        }
        mark("first-highlight", first, "library:${score.id}", 0, OverlayTool.HIGHLIGHT, 2)
        mark("first-cover", first, "library:${score.id}", 1, OverlayTool.COVER, 2)
        mark("second-focus", second, "library:${score.id}", 1, OverlayTool.FOCUS, 2)
        mark("image-highlight", imageBlock, "library:${image.id}", 0, OverlayTool.HIGHLIGHT, 1)
        mark("legacy-cover", archived, "legacy:${old.fileName}", 1, OverlayTool.COVER, 2)
        val master = MasterAudioMarkerRepository(db).save(audio.id, null, 150, 800, "Master section", "Global note")
        val inherited = BlockMarker(master, BlockMarkerOwnership.MASTER)
        val local = BlockAudioRepository(db)
        local.saveMarker(first.id, audio.id, inherited, master.copy(startMs = 200, label = "Block one override"))
        val marker = local.saveMarker(second.id, audio.id, null,
            master.copy(startMs = 950, type = "POINT", endMs = null, label = "Block two point", note = "Independent"))
        local.saveState(BlockAudioState(first.id, audio.id, 321, 75, 200, 800, true, master.id))
        local.saveState(BlockAudioState(second.id, audio.id, 500, 90, 100, 1200, false, marker))
        val free = takes.prepare(null)
        m4a.copyTo(free.file, overwrite = true)
        val freeTake = takes.save(free)
        takes.rename(freeTake.id, "Free record")
        val spec = PhraseSpec(120, 2)
        val loop = takes.prepare(first.id, spec)
        PhraseWave.write(loop.file, ShortArray(spec.frames) { (it % 100).toShort() })
        takes.save(loop)
        val backing = takes.prepareImport(second.id, "mp3")
        assets.open("audio/backing.mp3").use { input -> backing.file.outputStream().use { input.copyTo(it) } }
        takes.save(backing.copy(imported = ImportedAudioInfo("Original backing.mp3", inspectImportedAudio(backing.file, "mp3"))))

        db.withTransaction {
            db.goals().insert(Goal("goal", "Study the piece", "Goal description", 45, "Goal note", createdAt = 100, updatedAt = 200))
            db.goals().insert(GoalBlock("goal", first.id, 0))
            db.goalAttribution().setPrimary(BlockPrimaryGoal(first.id, "goal"))
            db.routines().insert(SavedRoutine("routine", "Warm-up sequence", 100, 200))
            db.routines().insert(RoutinePlacement("routine-placement", "routine", second.id, 0, 120_000))
            db.sessions().insert(PracticeSession("completed", 1000, 30_000, "COMPLETED", note = "Session note"))
            db.sessions().insert(Placement("completed-placement", "completed", first.id, first.name, first.action,
                90, "Reference note", 0, 300_000, 12_345, "COMPLETED", note = "Placement note", lastUsedBpm = 110))
            db.goals().insert(GoalSession("completed", "goal"))
            db.goalAttribution().insert(PlacementGoalSnapshot("completed-placement", "goal", 3))
            db.practiceNotes().insert(PracticeNote("practice-note", first.id, "completed", "completed-placement", 5000, "Practice note"))
            db.sessions().insert(PracticeSession("interrupted", 32_000, status = "IN_PROGRESS"))
            db.sessions().insert(Placement("interrupted-placement", "interrupted", second.id, second.name, second.action,
                null, "", 0, 120_000, 6000))
            db.goalAttribution().insert(PlacementGoalSnapshot("interrupted-placement", null, null))
            db.sessions().insert(PracticeSession("draft", 33_000, note = "Persisted draft"))
            db.sessions().insert(Placement("draft-placement", "draft", first.id, first.name, first.action, 90, "", 0, 120_000))
            db.logs().insert(DailyLog("2026-11-05", "Saved Daily Log"))
            db.transcribeActivities().insert(TranscribeActivity("activity", 4000, 70_000, 54_321, "goal", audio.id))
            db.transcribeActivities().insert(TranscribeActivity("paused-activity", 80_000, null, 4000, "goal", audio.id))
        }
        ThemePreferences(context).save(ThemeChoice.ORCHID)
        CountInPreferences(context).save(9)
        AudioPreferences(context).saveMode(MetronomeMode.FLASH_ONLY)
        AudioPreferences(context).saveSound(ClickSound.WOOD)
        AudioPreferences(context).saveAlarm(AlarmSound.BELL)
    }
}

/** Only the guarded emulator script calls this method against a fresh, disposable install. */
class PortableRestoreProcessDeathTest {
    @Test fun dieAfterDatabaseCommit() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = BlockDatabase.appInstance(context)
        val restore = PortableRestore(context, db) {
            if (it == RestoreBoundary.COMMITTED) Process.killProcess(Process.myPid())
        }
        restore.restore(restore.stage(File(context.filesDir, EXPECTED)::inputStream))
        fail("The test process did not terminate.")
    }
}
