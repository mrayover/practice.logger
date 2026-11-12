package com.mrayover.practicelogger

import android.content.pm.PackageManager
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.security.MessageDigest

/** Only the disposable CI emulator installs these permanent-key APKs. No owner data is touched. */
class SignedRoom24UpgradeTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val name = "practice.logger.db"
    private fun identity(version: Int) {
        assertEquals("true", InstrumentationRegistry.getArguments().getString("stage3d_emulator_only"))
        @Suppress("DEPRECATION") val info = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
        @Suppress("DEPRECATION") assertEquals(version, info.versionCode)
        @Suppress("DEPRECATION") val certificate = checkNotNull(info.signatures).single().toByteArray()
        assertEquals("f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830", hash(certificate))
    }
    private fun hash(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    @Test fun seedPopulatedSignedBuild110() {
        identity(110)
        assertFalse("Seed only an empty disposable installation", context.getDatabasePath(name).exists())
        MultiReferenceLegacyFixture(context, name, context.filesDir) { instrumentation.context.assets.open(it) }.create()
    }

    private fun compare(actual: SupportSQLiteDatabase) {
        val expectedName = "stage3d-signed-expected.db"
        val originals = File(context.cacheDir, "stage3d-signed-expected")
        context.deleteDatabase(expectedName); originals.deleteRecursively()
        try {
            val frozen = MultiReferenceLegacyFixture(context, expectedName, originals) { instrumentation.context.assets.open(it) }
            frozen.create()
            frozen.assertPreserved(actual, checkOriginals = false)
            val files = originals.walkTopDown().filter { it.isFile }.toList()
            assertEquals(5, files.size)
            files.forEach { expected ->
                val relative = expected.relativeTo(originals)
                val retained = File(context.filesDir, relative.path)
                assertTrue("Original retained: $relative", retained.isFile)
                assertEquals("Original hash retained: $relative", hash(expected.readBytes()), hash(retained.readBytes()))
            }
        } finally { context.deleteDatabase(expectedName); originals.deleteRecursively() }
    }

    @Test fun verifyPopulatedSignedBuild110BeforeUpgrade() {
        identity(110)
        val old = FrameworkSQLiteOpenHelperFactory().create(SupportSQLiteOpenHelper.Configuration.builder(context).name(name)
            .callback(object : SupportSQLiteOpenHelper.Callback(23) {
                override fun onCreate(db: SupportSQLiteDatabase) = error("Populated baseline missing")
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = error("Expected Room23")
            }).build())
        try { assertEquals(23, old.writableDatabase.version); compare(old.writableDatabase) }
        finally { old.close() }
    }

    @Test fun verifySignedRoom24AfterInstallOverAndColdReopen() {
        val version = InstrumentationRegistry.getArguments().getString("expected_build")!!.toInt()
        identity(version)
        val upgraded = BlockDatabase.open(context, name)
        try { assertEquals(24, upgraded.openHelper.writableDatabase.version); compare(upgraded.openHelper.writableDatabase) }
        finally { upgraded.close() }
    }
}
