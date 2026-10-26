package com.mrayover.practicelogger

import android.app.Application
import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Looper
import android.os.ParcelFileDescriptor
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowContentResolver

/** Models a removed/offline document provider at the real ContentResolver boundary. */
class UnavailableBackupProvider : ContentProvider() {
    override fun onCreate() = true
    override fun getType(uri: Uri) = "application/json"
    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor = throw java.io.FileNotFoundException("Provider unavailable")
    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = 0
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@LooperMode(LooperMode.Mode.PAUSED)
class BackupFailureTest {
    @Test fun unavailableProviderExportAndImportLeaveDataAndAllowRetry(): Unit = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        app.deleteDatabase("practice.logger.db")
        val provider = org.robolectric.Robolectric.buildContentProvider(UnavailableBackupProvider::class.java).create().get()
        ShadowContentResolver.registerProviderInternal("test.unavailable", provider)
        val db = BlockDatabase.open(app)
        val store = ViewModelStore()
        val vm = BackupViewModel(app); store.put("backup", vm)
        fun waitUntilIdle() {
            val deadline = System.nanoTime() + 10_000_000_000L
            while (vm.busy && System.nanoTime() < deadline) { shadowOf(Looper.getMainLooper()).idle(); Thread.sleep(10) }
            shadowOf(Looper.getMainLooper()).idle()
            assertFalse("Operation remained stuck", vm.busy)
        }
        try {
            BlockRepository(db).save(null, BlockDraft("Retain", "Keep this"))
            val original = BackupRepository(db).snapshot()
            vm.export(Uri.parse("content://test.unavailable/backup")); waitUntilIdle()
            assertTrue(vm.message!!.startsWith("Could not complete")); assertNull(vm.pending)
            assertEquals(original, BackupRepository(db).snapshot())
            vm.inspect(Uri.parse("content://test.unavailable/backup")); waitUntilIdle()
            assertTrue(vm.message!!.startsWith("Could not complete")); assertNull(vm.pending)
            val goodFile = java.io.File(app.cacheDir, "retry-backup.json")
            vm.export(Uri.fromFile(goodFile)); waitUntilIdle()
            assertTrue(vm.message!!.startsWith("Backup saved"))
            assertEquals(original, BackupCodec.decode(goodFile.readBytes()))
            assertEquals(original, BackupRepository(db).snapshot())
            goodFile.delete()
        } finally { store.clear(); db.close(); app.deleteDatabase("practice.logger.db") }
    }
    @Test fun interruptedReadRejectsTruncatedData() {
        val broken = object : java.io.InputStream() {
            override fun read(): Int = throw java.io.IOException("Interrupted")
        }
        try { readBackup(broken); fail("Read failure ignored") } catch (_: java.io.IOException) { }
    }
}
