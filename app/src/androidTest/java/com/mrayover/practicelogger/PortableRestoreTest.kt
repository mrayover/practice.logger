package com.mrayover.practicelogger

import android.graphics.Bitmap
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.UUID

class PortableRestoreTest {
    private class Interrupted : Error("Simulated process interruption")
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun rollsBackFailedCommitAndRecoversInterruptedPromotion() = runBlocking {
        fixture { backup, _, name, files ->
            val db = BlockDatabase.open(context, name)
            try {
                val beforeCommit = PortableRestore(context, db, files) { boundary ->
                    if (boundary == RestoreBoundary.BEFORE_COMMIT) error("Injected commit failure")
                }
                val staged = beforeCommit.stage(backup::inputStream)
                assertEquals("Injected commit failure", runCatching { beforeCommit.restore(staged) }.exceptionOrNull()?.message)
                assertTrue(db.blocks().readAll().isEmpty())
                assertTrue(File(files, "media-library").list().orEmpty().isEmpty())
                beforeCommit.discard(staged)

                val interrupted = PortableRestore(context, db, files) { boundary ->
                    if (boundary == RestoreBoundary.PROMOTED) throw Interrupted()
                }
                assertTrue(runCatching { interrupted.restore(interrupted.stage(backup::inputStream)) }
                    .exceptionOrNull() is Interrupted)
                assertTrue(File(files, "media-library").list().orEmpty().isNotEmpty())
            } finally { db.close() }
            usingDatabase(name) { reopened ->
                assertFalse(PortableRestore(context, reopened, files).recover())
                assertTrue(reopened.blocks().readAll().isEmpty())
                assertTrue(File(files, "media-library").list().orEmpty().isEmpty())
                assertFalse(File(files, "portable-restore/journal.json").exists())
            }
        }
    }

    @Test fun reopensCommittedRowsAndFinalizesPreferencesExactlyOnce() = runBlocking {
        for (stop in listOf(RestoreBoundary.COMMITTED, RestoreBoundary.PREFERENCES_SAVED)) {
            fixture { backup, original, name, files ->
                usingDatabase(name) { db ->
                    val interrupted = PortableRestore(context, db, files) { if (it == stop) throw Interrupted() }
                    assertTrue(runCatching { interrupted.restore(interrupted.stage(backup::inputStream)) }
                        .exceptionOrNull() is Interrupted)
                }
                usingDatabase(name) { db ->
                    val recovery = PortableRestore(context, db, files)
                    assertTrue(recovery.recover())
                    assertFalse(recovery.recover())
                    assertEquals(1, db.blocks().readAll().size)
                    assertEquals(original, db.media().find(original.id))
                    assertTrue(MediaLibraryRepository(db, File(files, "media-library")).available(original))
                    assertEquals(ThemeChoice.ORCHID, ThemePreferences(context).choice.value)
                    assertEquals(7L, CountInPreferences(context).read())
                }
            }
        }
    }

    @Test fun rechecksEmptinessAfterPreviewAndCanDiscardStaging() = runBlocking {
        fixture { backup, _, name, files ->
            usingDatabase(name) { db ->
                val restore = PortableRestore(context, db, files)
                val cancelled = restore.stage(backup::inputStream)
                restore.discard(cancelled)
                assertFalse(cancelled.directory.exists())
                assertTrue(db.blocks().readAll().isEmpty())

                val staged = restore.stage(backup::inputStream)
                db.logs().save(DailyLog("2026-11-05", "Keep this existing log"))
                assertTrue(runCatching { restore.restore(staged) }.exceptionOrNull() is NonEmptyRestoreException)
                assertTrue(runCatching { restore.stage(backup::inputStream) }.exceptionOrNull() is NonEmptyRestoreException)
                assertEquals("Keep this existing log", db.logs().readAll().single().text)
                assertTrue(db.blocks().readAll().isEmpty())
                assertFalse(File(files, "media-library").exists())
            }
        }
    }

    private suspend fun <T> usingDatabase(name: String, run: suspend (BlockDatabase) -> T): T {
        val db = BlockDatabase.open(context, name)
        try { return run(db) } finally { db.close() }
    }

    private suspend fun fixture(test: suspend (File, MediaAsset, String, File) -> Unit) {
        val key = "s2-restore-${UUID.randomUUID()}"
        val root = File(context.filesDir, key).apply { check(mkdir()) }
        val sourceFiles = File(root, "source").apply { check(mkdir()) }
        val targetFiles = File(root, "target").apply { check(mkdir()) }
        val sourceName = "$key-source.db"
        val targetName = "$key-target.db"
        val prior = PortablePreferences.snapshot(context)
        try {
            val archive = File(root, "archive.zip")
            val original = usingDatabase(sourceName) { db ->
                val library = MediaLibraryRepository(db, File(sourceFiles, "media-library"))
                val png = File(sourceFiles, "source.png")
                val bitmap = Bitmap.createBitmap(12, 16, Bitmap.Config.ARGB_8888)
                png.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
                bitmap.recycle()
                val asset = library.import("source.png", "image/png", open = png::inputStream)
                db.blocks().insert(Block("one", "One", "Play", createdAt = 1, updatedAt = 1))
                BlockMediaRepository(db, library, ImageReferenceRepository(db, File(sourceFiles, "block-images")))
                    .link("one", MediaRole.VISUAL, asset.id)
                ThemePreferences(context).save(ThemeChoice.ORCHID)
                CountInPreferences(context).save(7)
                PortableZip(context, db, sourceFiles).create(archive)
                asset
            }
            prior.apply(context)
            test(archive, original, targetName, targetFiles)
        } finally {
            prior.apply(context)
            context.deleteDatabase(sourceName)
            context.deleteDatabase(targetName)
            root.deleteRecursively()
        }
    }
}
