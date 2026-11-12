package com.mrayover.practicelogger

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
import java.time.LocalDate

class LearningWorkflowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val app get() = compose.activity.application
    private val db get() = BlockDatabase.appInstance(app)
    private val practice get() = ViewModelProvider(compose.activity)[PracticeViewModel::class.java]
    private fun click(text: String) = compose.onNodeWithText(text).scrollIfNeeded().assertIsDisplayed().performClick()
    private fun shown(text: String) = compose.waitUntil(15000) {
        compose.onAllNodesWithText(text).fetchSemanticsNodes(atLeastOneRootRequired=false).isNotEmpty()
    }
    private fun settleKeyboard() {
        compose.runOnIdle {
            (compose.activity.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager)
                .hideSoftInputFromWindow(compose.activity.window.decorView.windowToken,0)
        }
        compose.waitUntil(5000) {
            androidx.core.view.ViewCompat.getRootWindowInsets(compose.activity.window.decorView)
                ?.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime()) != true
        }
        compose.waitForIdle()
    }
    private fun waitForDate(blockId: String) {
        try { compose.waitUntil(15000) { runBlocking { db.learning().events(blockId).firstOrNull()?.dueDate != null } } }
        catch (failure: Throwable) {
            val folder=File(app.getExternalFilesDir(null),"home-preview").apply { mkdirs() }
            UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).takeScreenshot(File(folder,"learning-date-failure.png"))
            File(folder,"learning-date-semantics.txt").writeText(compose.onNode(isDialog()).printToString())
            throw failure
        }
    }
    @Test fun optionalObservationAndManualRevisitsLeavePracticeAndClickRunning() {
        val block=runBlocking {
            val b=BlockRepository(db).save(null,BlockDraft("Learning preview","Play bars 1–4 at 80"))
            BlockRepository(db).save(null,BlockDraft("Unassessed exploration","Explore freely"))
            LearningRepository(db).schedule(b,"FLAG_SET",operationId="picker-flag")
            b
        }
        openHomePractice(compose)
        click("Add Blocks")
        shown("Build Session")
        compose.onNodeWithTag("builder-lineup").performScrollToNode(hasText("+ Add Blocks"))
        click("+ Add Blocks")
        compose.onNodeWithTag("builder-picker").performScrollToNode(hasText("Unassessed exploration"))
        compose.onNodeWithText("Unassessed exploration").assertExists()
        compose.onNodeWithTag("builder-picker").performScrollToNode(hasTestTag("suggested-view"))
        compose.onNodeWithTag("suggested-view").performClick()
        shown("Return Soon · chosen by you")
        assertNull(runBlocking { db.sessions().withStatus("DRAFT") })
        assertEquals(1,runBlocking { db.learning().events(block).size })
        compose.onNodeWithTag("builder-picker").performScrollToNode(hasTestTag("builder-add-$block"))
        compose.onNodeWithTag("builder-add-$block").performClick()
        compose.waitUntil(15000) { runBlocking { db.sessions().withStatus("DRAFT") != null } }
        click("Done adding Blocks")
        val id=runBlocking { db.sessions().withStatus("DRAFT")!!.id }
        click("Start Session")
        val p=runBlocking { db.sessions().find(id)!!.current!! }
        compose.runOnIdle { practice.openSession(id) }
        shown("Start Block"); click("Start Block"); shown("Pause")
        click("Start click")
        compose.runOnIdle { assertTrue(practice.running); assertTrue(practice.clicking) }
        compose.onNodeWithTag("learning-open").scrollIfNeeded().performClick()
        shown("First-attempt observation")
        click("Close")
        assertNull(runBlocking { db.learning().observation(p.id) })
        compose.onNodeWithTag("learning-open").scrollIfNeeded().performClick()
        shown("First-attempt observation")
        compose.onNodeWithTag("learning-outcome-HELD").scrollIfNeeded().performClick()
        click("First attempt, before rehearsing this task")
        compose.onNodeWithTag("learning-observation-save").scrollIfNeeded().performClick()
        shown("Observation saved.")
        val saved=runBlocking { db.learning().observation(p.id)!! }
        assertEquals("FIRST_ATTEMPT",saved.context)
        assertEquals("HELD",saved.outcome)
        assertNotNull(saved.criterionToken)
        // The picker fixture is already explicitly flagged; exercise both independent actions.
        click("Clear flag")
        compose.waitUntil(15000) { runBlocking { db.learning().events(p.sourceBlockId).firstOrNull()?.returnSoon == false } }
        click("Return Soon")
        shown("Revisit choice saved.")
        compose.waitUntil(15000) { runBlocking { db.learning().events(p.sourceBlockId).firstOrNull()?.returnSoon == true } }
        compose.onNodeWithTag("learning-date").scrollIfNeeded().performTextReplacement(LocalDate.now().plusDays(7).toString())
        compose.onNodeWithTag("learning-date").assertTextContains(LocalDate.now().plusDays(7).toString())
        settleKeyboard()
        compose.onNodeWithTag("learning-date-save").scrollIfNeeded().assertIsDisplayed().assertIsEnabled().performClick()
        waitForDate(p.sourceBlockId)
        // An ordinary optional panel action never changes the live timing/audio owner.
        compose.runOnIdle { assertTrue(practice.running); assertTrue(practice.clicking) }
        val folder=File(app.getExternalFilesDir(null),"home-preview").apply { mkdirs() }
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).takeScreenshot(File(folder,"learning-revisits.png"))
        click("No scheduling")
        compose.waitUntil(15000) { runBlocking { db.learning().events(p.sourceBlockId).firstOrNull()?.action == "DECLINED" } }
        val declined=runBlocking { db.learning().events(p.sourceBlockId).first() }
        assertTrue(declined.returnSoon)
        assertEquals(7,declined.intervalDays)
        compose.onNodeWithTag("learning-suggestion-accept").scrollIfNeeded().performClick()
        shown("Suggested revisit accepted.")
        val choices=runBlocking { db.learning().events(p.sourceBlockId) }
        assertEquals("ACCEPTED_SUGGESTION",choices.first().origin)
        assertEquals(3,choices.first().intervalDays)
        assertTrue(choices.first().returnSoon)
        assertEquals("STARTING",readAcceptedSuggestion(choices.first().offeredSuggestion!!).reason)
        click("Close")
        click("Pause"); shown("Resume")
        val committed=runBlocking { db.sessions().find(id)!! }
        assertTrue(committed.activeMs>0)
        assertEquals(0,committed.completions.size)
        assertEquals("PENDING",committed.current!!.outcome)
        compose.runOnIdle { practice.stopClick() }
        compose.activityRule.scenario.recreate()
        compose.waitUntil(15000) { runBlocking { db.learning().observation(p.id) == saved } }
        assertEquals(choices,runBlocking { db.learning().events(p.sourceBlockId) })
        // Real Android media decoding/file promotion complements the JVM record checks.
        runBlocking {
            val bytes=java.io.ByteArrayOutputStream().also { output ->
                android.graphics.Bitmap.createBitmap(8,8,android.graphics.Bitmap.Config.ARGB_8888).also {
                    it.eraseColor(android.graphics.Color.GREEN)
                    it.compress(android.graphics.Bitmap.CompressFormat.PNG,100,output); it.recycle()
                }
            }.toByteArray()
            val asset=MediaLibraryRepository(db,File(app.filesDir,"media-library")).import("Learning reference.png","image/png") { bytes.inputStream() }
            val scratch=androidx.room.Room.inMemoryDatabaseBuilder(app,BlockDatabase::class.java).addCallback(mediaConstraintCallback).build()
            val root=File(app.cacheDir,"learning-native-roundtrip").apply { mkdirs() }
            val destination=File(root,"restored").apply { mkdirs() }
            try {
                val archive=File(root,"learning.zip")
                PortableZip(app,db).create(archive)
                val failed=PortableRestore(app,scratch,destination,atBoundary={ if(it==RestoreBoundary.BEFORE_COMMIT) error("Injected failure") })
                assertTrue(runCatching { failed.restore(failed.stage { archive.inputStream() }) }.isFailure)
                assertTrue(portableTargetEmpty(scratch.openHelper.writableDatabase))
                assertFalse(File(destination,"media-library/"+asset.fileName).exists())
                val restore=PortableRestore(app,scratch,destination)
                restore.restore(restore.stage { archive.inputStream() })
                assertEquals(saved,scratch.learning().observation(p.id))
                assertEquals(choices,scratch.learning().events(p.sourceBlockId))
                assertEquals(committed.ordered,scratch.sessions().find(id)!!.ordered)
                assertArrayEquals(bytes,File(destination,"media-library/"+asset.fileName).readBytes())
                assertEquals(asset,scratch.media().find(asset.id))
            } finally { scratch.close(); root.deleteRecursively() }
        }
    }
}
