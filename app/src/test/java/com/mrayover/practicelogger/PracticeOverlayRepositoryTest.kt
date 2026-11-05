package com.mrayover.practicelogger

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PracticeOverlayRepositoryTest {
    @Test fun copyAndConfirmedChangesKeepOtherBlocksAndOriginals(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "overlay-${UUID.randomUUID()}"
        val root = File(context.cacheDir, name).apply { check(mkdir()) }
        val db = BlockDatabase.open(context, name)
        try {
            for (id in listOf("source", "target", "legacy", "unlinked")) {
                db.blocks().insert(Block(id, id, "Play", createdAt = 1, updatedAt = 1))
            }
            fun png(color: Int): ByteArray {
                val bitmap = Bitmap.createBitmap(24, 32, Bitmap.Config.ARGB_8888)
                try {
                    bitmap.eraseColor(color)
                    return ByteArrayOutputStream().use {
                        check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
                        it.toByteArray()
                    }
                } finally { bitmap.recycle() }
            }
            val originals = File(root, "media-library").apply { check(mkdir()) }
            val attachments = File(root, "block-images").apply { check(mkdir()) }
            val library = MediaLibraryRepository(db, originals)
            val legacy = ImageReferenceRepository(db, attachments)
            val links = BlockMediaRepository(db, library, legacy)
            suspend fun seedAsset(id: String, bytes: ByteArray): MediaAsset {
                val file = File(originals, "${UUID.randomUUID()}.png").apply { writeBytes(bytes) }
                val hash = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
                return MediaAsset(id, "$id.png", "$id.png", "image/png", "IMAGE", file.name,
                    file.length(), 1, width = 24, height = 32, sha256 = hash).also { db.media().insert(it) }
            }
            val sourceBytes = png(android.graphics.Color.BLACK)
            val media = seedAsset("source", sourceBytes)
            val other = seedAsset("other", png(android.graphics.Color.WHITE))
            links.link("source", MediaRole.VISUAL, media.id)
            links.link("target", MediaRole.VISUAL, media.id)
            val oldFile = File(attachments, "${UUID.randomUUID()}.img").apply { writeBytes(sourceBytes) }
            val old = BlockImage("legacy", oldFile.name, "image/png", 24, 32, oldFile.length())
            db.images().save(old)
            val reference = BlockVisualReference("source", "library:${media.id}", media.mimeType, media.id)
            val shape = PracticeOverlay("original", "source", reference.identity, 0, "COVER",
                .25f, .25f, .5f, .5f, 0, 0)
            val overlays = PracticeOverlayRepository(db)
            overlays.edit(OverlayEdit(null, shape), 1)
            assertEquals(1, overlays.copy(reference, "target", 1))
            val copied = db.overlays().read("target", reference.identity).single()
            assertNotEquals(shape.id, copied.id)
            assertEquals(shape, copied.copy(id = shape.id, blockId = shape.blockId))
            assertTrue(runCatching { overlays.copy(reference, "unlinked", 1) }.isFailure)
            assertTrue(runCatching { overlays.copy(reference, "target", 0) }.isFailure)
            assertEquals(listOf(copied), db.overlays().read("target", reference.identity))
            val change = overlays.edit(OverlayEdit(copied, copied.copy(right = .6f)), 1)
            assertEquals(shape, db.overlays().find(shape.id))
            overlays.edit(change.inverse(), 1)
            assertEquals(copied, db.overlays().find(copied.id))

            val reviewed = db.reviewVisualChange("source")
            assertTrue(reviewed.hasMarkings)
            assertTrue(runCatching { links.unlink("source", MediaRole.VISUAL) }.isFailure)
            assertTrue(runCatching { links.link("source", MediaRole.VISUAL, other.id) }.isFailure)
            assertEquals(shape, db.overlays().find(shape.id))
            val sql = db.openHelper.writableDatabase
            sql.execSQL("CREATE TRIGGER fail_unlink BEFORE DELETE ON block_media_links BEGIN SELECT RAISE(ABORT, 'test'); END")
            assertTrue(runCatching { links.unlinkChecked("source", MediaRole.VISUAL, reviewedVisual = reviewed) }.isFailure)
            assertEquals(shape, db.overlays().find(shape.id))
            sql.execSQL("DROP TRIGGER fail_unlink")
            links.linkChecked("source", MediaRole.VISUAL, other.id, reviewedVisual = reviewed)
            assertNull(db.overlays().find(shape.id))
            assertEquals(copied, db.overlays().find(copied.id))
            assertTrue(runCatching { links.unlinkChecked("source", MediaRole.VISUAL, reviewedVisual = reviewed) }.isFailure)
            assertEquals(other.id, db.blockMedia().find("source", "VISUAL")?.mediaId)
            assertArrayEquals(sourceBytes, library.original(media.id).second.readBytes())

            val legacyShape = shape.copy(id = "legacy-mark", blockId = "legacy", reference = "legacy:${old.fileName}")
            overlays.edit(OverlayEdit(null, legacyShape), 1)
            assertTrue(runCatching { legacy.remove("legacy") }.isFailure)
            assertTrue(runCatching { links.link("legacy", MediaRole.VISUAL, media.id, replaceLegacy = true) }.isFailure)
            assertArrayEquals(sourceBytes, oldFile.readBytes())
            assertEquals(legacyShape, db.overlays().find(legacyShape.id))
            links.linkChecked("legacy", MediaRole.VISUAL, media.id, replaceLegacy = true,
                reviewedVisual = db.reviewVisualChange("legacy"))
            assertNull(db.overlays().find(legacyShape.id))
            assertNull(db.images().find("legacy"))
            assertFalse(oldFile.exists())
            assertEquals(copied, db.overlays().find(copied.id))
        } finally {
            db.close()
            context.deleteDatabase(name)
            root.deleteRecursively()
        }
    }
}
