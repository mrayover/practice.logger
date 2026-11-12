package com.mrayover.practicelogger

import android.os.Process
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/** Invoked in separate instrumentation processes only by the disposable-emulator check script. */
class MultiReferenceProcessDeathTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val name = "stage3b-kill.db"
    private val files get() = File(context.filesDir, "stage3b-kill-originals")
    @Test fun dieWhileParentAndChildrenAreRebuilt() {
        assertEquals("true", InstrumentationRegistry.getArguments().getString("stage3b_emulator_only"))
        context.deleteDatabase(name); files.deleteRecursively()
        MultiReferenceLegacyFixture(context, name, files) { instrumentation.context.assets.open(it) }.create()
        val interrupted = object : Migration(23, 24) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MULTI_REFERENCE_MIGRATION_23_24.migrate(object : SupportSQLiteDatabase by db {
                    override fun execSQL(sql: String) {
                        db.execSQL(sql)
                        if (sql == "DROP TABLE block_media_links") Process.killProcess(Process.myPid())
                    }
                })
            }
        }
        BlockDatabase.builder(context, name).addMigrations(interrupted).build().openHelper.writableDatabase
        fail("The migration process did not terminate")
    }
    @Test fun recoverRoom23ThenMigrateAndColdReopenWithoutLoss() {
        val expectedName = "stage3b-kill-expected.db"
        val expectedFiles = File(context.filesDir, "stage3b-kill-expected-originals")
        val expected = MultiReferenceLegacyFixture(context, expectedName, expectedFiles) { instrumentation.context.assets.open(it) }
        try {
            expected.create()
            val old = FrameworkSQLiteOpenHelperFactory().create(SupportSQLiteOpenHelper.Configuration.builder(context).name(name)
                .callback(object : SupportSQLiteOpenHelper.Callback(23) {
                    override fun onConfigure(db: SupportSQLiteDatabase) { db.setForeignKeyConstraintsEnabled(true) }
                    override fun onCreate(db: SupportSQLiteDatabase) = error("Interrupted database missing")
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = error("Unexpected upgrade")
                }).build())
            try {
                assertEquals(23, old.writableDatabase.version)
                expected.assertPreserved(old.writableDatabase, originalRoot = files)
            } finally { old.close() }
            repeat(2) {
                val db = BlockDatabase.open(context, name)
                try {
                    assertEquals(24, db.openHelper.writableDatabase.version)
                    expected.assertPreserved(db.openHelper.writableDatabase, originalRoot = files)
                } finally { db.close() }
            }
        } finally {
            context.deleteDatabase(name); context.deleteDatabase(expectedName)
            files.deleteRecursively(); expectedFiles.deleteRecursively()
        }
    }
}
