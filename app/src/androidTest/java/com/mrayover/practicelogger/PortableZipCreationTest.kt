package com.mrayover.practicelogger

import android.graphics.Bitmap
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.UUID
import java.util.zip.ZipFile

class PortableZipCreationTest {
    @Test fun writesOneVerifiedOriginalForTwoLinksAndRejectsMissingSource() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "s2-create-${UUID.randomUUID()}"
        val root = File(context.filesDir, name).apply { check(mkdir()) }
        val db = BlockDatabase.open(context, "$name.db")
        try {
            val library = MediaLibraryRepository(db, File(root, "media-library"))
            val legacy = ImageReferenceRepository(db, File(root, "block-images"))
            val png = File(root, "source.png")
            val bitmap = Bitmap.createBitmap(12, 16, Bitmap.Config.ARGB_8888)
            png.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            bitmap.recycle()
            val original = library.import("sample.png", "image/png", open = png::inputStream)
            val links = BlockMediaRepository(db, library, legacy)
            for (id in listOf("first", "second")) {
                db.blocks().insert(Block(id, id, "Practice", createdAt = 1, updatedAt = 1))
                links.link(id, MediaRole.VISUAL, original.id)
            }
            val archive = File(root, "verified.zip")
            val codec = PortableZip(context, db, root)
            val verified = codec.create(archive)
            assertEquals(2, verified.records["block_media_links"].size)
            assertEquals(1, verified.originals.size)
            assertEquals(original.id, verified.originals.single().owner)
            assertEquals(png.length(), verified.mediaBytes)
            ZipFile(archive).use { zip ->
                assertEquals(3, zip.size())
                val entry = zip.getEntry(verified.originals.single().path)
                assertArrayEquals(png.readBytes(), zip.getInputStream(entry).use { it.readBytes() })
            }
            val legacyData = BackupRepository(db).snapshot()
            assertEquals(legacyData, BackupCodec.decode(BackupCodec.encode(legacyData)))
            val truncated = File(root, "truncated.zip")
            archive.copyTo(truncated)
            java.io.RandomAccessFile(truncated, "rw").use { it.setLength(it.length() - 7) }
            assertTrue(runCatching { codec.verify(truncated) }.isFailure)

            assertTrue(File(root, "media-library/${original.fileName}").delete())
            val failed = File(root, "failed.zip")
            assertTrue(runCatching { codec.create(failed) }.isFailure)
            assertFalse(failed.exists())
            assertEquals(2, db.blockMedia().readAll().size)
            assertTrue(archive.exists())
        } finally {
            db.close()
            context.deleteDatabase("$name.db")
            root.deleteRecursively()
        }
        Unit
    }
}
