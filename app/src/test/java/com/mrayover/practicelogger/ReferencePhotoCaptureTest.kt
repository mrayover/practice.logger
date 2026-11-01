package com.mrayover.practicelogger

import android.content.Context
import android.content.Intent
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContracts
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class) @Config(sdk = [35])
class ReferencePhotoCaptureTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()

    @Test fun cameraUriRoundTripsFullBytesAndCancellationDeletesOnlyStaging() {
        val capture = ReferencePhotoCapture(context)
        val first = capture.create()
        val second = capture.create()
        try {
            val uri = capture.uri(first)
            assertEquals("content", uri.scheme)
            assertEquals("${context.packageName}.reference-camera", uri.authority)
            val bytes = ByteArray(100_000) { (it % 251).toByte() }
            context.contentResolver.openOutputStream(uri)!!.use { it.write(bytes) }
            assertArrayEquals(bytes, capture.file(first).readBytes())
            capture.discard(first)
            assertFalse(capture.file(first).exists())
            assertTrue(capture.file(second).exists())
        } finally { capture.discard(first); capture.discard(second) }
    }

    @Test fun cameraContractUsesFullResolutionOutputAndReportsCancellation() {
        val capture = ReferencePhotoCapture(context)
        val name = capture.create()
        try {
            val contract = ActivityResultContracts.TakePicture()
            val intent = contract.createIntent(context, capture.uri(name))
            assertEquals(MediaStore.ACTION_IMAGE_CAPTURE, intent.action)
            assertEquals(capture.uri(name), intent.getParcelableExtra<android.net.Uri>(MediaStore.EXTRA_OUTPUT))
            assertFalse(contract.parseResult(android.app.Activity.RESULT_CANCELED, Intent()))
            assertTrue(contract.parseResult(android.app.Activity.RESULT_OK, Intent()))
        } finally { capture.discard(name) }
    }

    @Test fun providerIsPrivateAndCanGrantOnlySelectedFiles() {
        val provider = context.packageManager.resolveContentProvider("${context.packageName}.reference-camera", 0)!!
        assertFalse(provider.exported)
        assertTrue(provider.grantUriPermissions)
        assertFalse(context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.GET_PERMISSIONS)
            .requestedPermissions.orEmpty().contains("android.permission.CAMERA"))
    }

    @Test fun stagingRejectsPathsOutsideCameraDirectory() {
        val capture = ReferencePhotoCapture(context)
        for (name in listOf("../reference.jpg", "/data/private.jpg", "a.jpg", "../../block-images/keep.jpg")) {
            assertThrows(IllegalArgumentException::class.java) { capture.file(name) }
        }
    }
}
