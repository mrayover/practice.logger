package com.mrayover.practicelogger

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class NestedReferenceDiagnosticTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Test fun savedNoteCanScrollBackToReferenceWithRealGestures() {
        val app=compose.activity.application;val db=BlockDatabase.open(app)
        try {
            val sid=runBlocking {
                val id=BlockRepository(db).save(null,BlockDraft("Scroll fixture","Read score while recording","5"))
                ImageReferenceRepository(db,File(app.filesDir,"block-images")).import(id) { InstrumentationRegistry.getInstrumentation().context.assets.open("references/score.pdf") }
                val repo=SessionRepository(db);repo.addBlock(id);repo.start()
            }
            val practice=ViewModelProvider(compose.activity)[PracticeViewModel::class.java]
            compose.waitUntil(15000) { practice.catalog.value.ready };compose.runOnIdle { practice.openSession(sid) }
            compose.waitUntil(15000) { compose.onAllNodesWithText("Notes").fetchSemanticsNodes().isNotEmpty() }
            revealPracticeNotes(compose)
            compose.onNodeWithText("Quick note (optional)").performScrollTo().performTextInput("A saved note")
            compose.onNodeWithText("Save Block note").scrollIfNeeded().performClick();compose.waitUntil(15000) { !practice.busy }
            scrollPracticeWorkspace(compose,hasText("Show reference"))
            compose.onNodeWithText("Show reference").assertIsDisplayed().performClick()
            compose.waitUntil(15000) { compose.onAllNodesWithTag("reference-viewer").fetchSemanticsNodes().isNotEmpty() }
            revealPracticeReference(compose);compose.onNodeWithTag("reference-viewer").assertIsDisplayed()
            compose.onNodeWithTag("reference-viewer").performTouchInput {
                pinch(androidx.compose.ui.geometry.Offset(width*.4f,height*.4f),androidx.compose.ui.geometry.Offset(width*.6f,height*.6f),
                    androidx.compose.ui.geometry.Offset(width*.15f,height*.15f),androidx.compose.ui.geometry.Offset(width*.85f,height*.85f))
            }
            compose.onNodeWithText("Reset PDF zoom").assertIsDisplayed().performClick()
            compose.onNodeWithText("Reset PDF zoom").assertDoesNotExist()
            assertEquals("A saved note",runBlocking { db.sessions().find(sid)!!.current!!.note })
        } finally { db.close() }
    }
}
