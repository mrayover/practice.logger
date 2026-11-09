package com.mrayover.practicelogger

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.UUID

class InspirationBackupTest {
    @Test fun completeZipRestoresTextAndVisualsWithoutCopyingSharedOriginal() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val id = UUID.randomUUID().toString()
        val root = File(context.cacheDir, id).apply { check(mkdir()) }
        val sourceRoot = File(root, "source").apply { check(mkdir()) }
        val targetRoot = File(root, "target").apply { check(mkdir()) }
        val sourceName = "$id-source.db"
        val targetName = "$id-target.db"
        try {
            val png = File(root, "test.png")
            val bitmap = Bitmap.createBitmap(32, 20, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.MAGENTA)
            png.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            bitmap.recycle()
            val contents = "Music and memory\n" + "Long notebook text\n".repeat(100_000)
            val input = InspirationInput("test.png", png::inputStream)
            val archive = File(root, "complete.zip")
            val savedRows = BlockDatabase.open(context, sourceName).useDatabase { db ->
                val repository = InspirationRepository(db, sourceRoot)
                val written = repository.save(null, Inspiration("", "Notebook", "A musician", "A source", "Other", "A note"),
                    InspirationInput("notebook.txt") { contents.byteInputStream() })
                val owned = repository.save(null, Inspiration("", "Owned image"), input)
                val library = MediaLibraryRepository(db, File(sourceRoot, "media-library"))
                val asset = library.import("test.png", "image/png", open = png::inputStream)
                val shared = repository.save(null, Inspiration("", "Shared image"), input)
                assertNull(shared.fileName)
                assertEquals(asset.id, shared.mediaId)
                assertTrue(runCatching { library.delete(asset.id) }.isFailure)
                assertEquals(3, PortableZip(context, db, sourceRoot).create(archive).originals.size)
                listOf(written, owned, shared)
            }
            png.delete()
            BlockDatabase.open(context, targetName).useDatabase { db ->
                val restore = PortableRestore(context, db, targetRoot)
                restore.restore(restore.stage(archive::inputStream))
            }
            BlockDatabase.open(context, targetName).useDatabase { db ->
                val repository = InspirationRepository(db, targetRoot)
                for (row in savedRows) assertEquals(row, db.inspirations().find(row.id))
                val (written, owned, shared) = savedRows
                assertEquals(contents, repository.text(written))
                for (visual in listOf(owned, shared)) {
                    val page = repository.visualPage(visual, 0)
                    assertEquals(32, page.bitmap.width)
                    page.bitmap.recycle()
                }
                repository.delete(owned)
                repository.delete(shared)
                val assetId = checkNotNull(shared.mediaId)
                assertNotNull(db.media().find(assetId))
                MediaLibraryRepository(db, File(targetRoot, "media-library")).delete(assetId)
                assertFalse(db.withTransaction { portableTargetEmpty(db.openHelper.writableDatabase) })
                assertTrue(runCatching { BackupRepository(db).restore(BackupData(emptyList(), emptyList(), emptyList())) }
                    .exceptionOrNull() is NonEmptyRestoreException)
                assertTrue(runCatching { PortableRestore(context, db, targetRoot).stage(archive::inputStream) }
                    .exceptionOrNull() is NonEmptyRestoreException)
            }
        } finally {
            context.deleteDatabase(sourceName)
            context.deleteDatabase(targetName)
            root.deleteRecursively()
        }
    }
}

private inline fun <T> BlockDatabase.useDatabase(action: (BlockDatabase) -> T): T =
    try { action(this) } finally { close() }
