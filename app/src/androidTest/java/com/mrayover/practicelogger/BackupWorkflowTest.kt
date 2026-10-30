package com.mrayover.practicelogger

import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class BackupWorkflowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun shown(text: String) { compose.waitUntil(15000) { compose.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty() } }
    @Test fun backupScreenExportPreviewCancelAndProtectedRestore() {
        openHomeTools(compose)
        shown("Backup and restore")
        compose.onNodeWithText("Backup and restore").scrollIfNeeded().performClick()
        shown("Export backup")
        compose.activityRule.scenario.recreate()
        shown("Export backup")
        val context = compose.activity.applicationContext
        val file = java.io.File(context.cacheDir, "device-backup.json")
        lateinit var vm: BackupViewModel
        compose.runOnIdle { vm = ViewModelProvider(compose.activity)[BackupViewModel::class.java]; vm.export(Uri.fromFile(file)) }
        shown("Backup saved.")
        val db = BlockDatabase.open(context)
        val restoredName = "device-restore-test.db"
        context.deleteDatabase(restoredName)
        var restored = BlockDatabase.open(context, restoredName)
        try {
            runBlocking {
                val data = file.inputStream().use { BackupCodec.decode(readBackup(it)) }
                assertEquals(BackupRepository(db).snapshot(), data)
                assertTrue(data.sessions.any { it.status == "COMPLETED" })
                BackupRepository(restored).restore(data)
                restored.close(); restored = BlockDatabase.open(context, restoredName)
                assertEquals(data, BackupRepository(restored).snapshot())
            }
            compose.runOnIdle { vm.inspect(Uri.fromFile(file)) }
            shown("Restore this backup?")
            compose.onNodeWithText("Cancel").performClick()
            compose.onNodeWithText("Restore this backup?").assertDoesNotExist()
            compose.runOnIdle { vm.inspect(Uri.fromFile(file)) }
            shown("Restore this backup?")
            compose.activityRule.scenario.recreate()
            shown("Restore this backup?")
            compose.onNodeWithText("Restore backup").performClick()
            shown("Restore needs an empty app")
            runBlocking {
                assertEquals(file.inputStream().use { BackupCodec.decode(readBackup(it)) }, BackupRepository(db).snapshot())
            }
            file.writeText("{\"format\":\"broken\"}")
            compose.runOnIdle { vm.inspect(Uri.fromFile(file)) }
            shown("Unsupported backup format/version.")
            compose.onNodeWithText("Restore this backup?").assertDoesNotExist()
            compose.onNodeWithTag("home-brand", useUnmergedTree = true).scrollIfNeeded().performClick()
            openHomeRoutines(compose); shown("Recent Practice")
        } finally { db.close(); restored.close(); context.deleteDatabase(restoredName); file.delete() }
    }
}
