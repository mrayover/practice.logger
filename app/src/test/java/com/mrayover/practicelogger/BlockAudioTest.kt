package com.mrayover.practicelogger

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BlockAudioTest {
    @Test fun independentStatePromotionConflictAndCleanup(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "block-audio-test.db"
        var db = BlockDatabase.open(context, name)
        val dir = File(context.cacheDir, UUID.randomUUID().toString()).apply { mkdirs() }
        try {
            val a = BlockRepository(db).save(null, BlockDraft("A", "Listen"))
            val b = BlockRepository(db).save(null, BlockDraft("B", "Listen"))
            val original = File(dir, "${UUID.randomUUID()}.wav").apply { writeBytes(byteArrayOf(1, 2, 3)) }
            db.openHelper.writableDatabase.execSQL(
                "INSERT INTO media_assets VALUES ('audio','Shared','Shared.wav','audio/wav','AUDIO',?,3,1,NULL,NULL,NULL,4000,'hash')",
                arrayOf(original.name))
            db.blockMedia().save(BlockMediaLink(a, "AUDIO", "audio"))
            db.blockMedia().save(BlockMediaLink(b, "AUDIO", "audio"))
            val first = BlockAudioState(a, "audio", 1250, 65, 500, 2000, true, null)
            val second = BlockAudioState(b, "audio", 3000, 90, 2000, 3500, false, null)
            BlockAudioRepository(db).saveState(first)
            BlockAudioRepository(db).saveState(second)
            db.close()
            db = BlockDatabase.open(context, name)
            val repo = BlockAudioRepository(db)
            val master = MasterAudioMarkerRepository(db)
            suspend fun marker(block: String, id: String) =
                repo.observe(block, "audio").first().visible.single { it.marker.id == id }
            assertEquals(first, repo.state(a, "audio"))
            assertEquals(second, repo.state(b, "audio"))
            val phrase = master.save("audio", null, 500, 1800, "Phrase", "Master note")
            repo.saveMarker(a, "audio", marker(a, phrase.id), phrase.copy(label = "A version"))
            val hidden = marker(b, phrase.id)
            repo.saveMarker(b, "audio", hidden, hidden.marker, hide = true)
            assertTrue(repo.observe(b, "audio").first().visible.isEmpty())
            val changed = master.save("audio", phrase.id, 600, 1850, "Changed", "New note")
            val local = marker(a, phrase.id).local!!
            val conflict = runCatching { repo.publish(local) }.exceptionOrNull() as MasterMarkerConflict
            assertEquals(changed, conflict.current)
            assertEquals(local, marker(a, phrase.id).local)
            assertEquals(phrase.id, repo.publish(local, conflict.current))
            assertEquals("A version", marker(a, phrase.id).marker.label)
            assertEquals(BlockMarkerOwnership.MASTER, marker(a, phrase.id).ownership)
            val hiddenVersion = repo.observe(b, "audio").first().hidden.single().local!!
            repo.discard(hiddenVersion, reset = true)
            assertEquals("A version", marker(b, phrase.id).marker.label)
            repo.saveMarker(a, "audio", marker(a, phrase.id), phrase.copy(note = "Keep my edits"))
            repo.saveMarker(b, "audio", marker(b, phrase.id), phrase, hide = true)
            master.delete("audio", phrase.id)
            assertTrue(repo.observe(b, "audio").first().hidden.isEmpty())
            val independent = marker(a, phrase.id)
            assertEquals(BlockMarkerOwnership.LOCAL, independent.ownership)
            assertEquals("Keep my edits", independent.marker.note)
            assertTrue(repo.observe(b, "audio").first().visible.isEmpty())
            val published = repo.publish(independent.local!!)
            assertNotEquals(phrase.id, published)
            assertEquals("Keep my edits", marker(b, published).marker.note)
            assertTrue(runCatching { repo.publish(independent.local) }.isFailure)
            assertEquals(1, db.markers().read("audio").size)
            val links = BlockMediaRepository(db, MediaLibraryRepository(db, dir), ImageReferenceRepository(db, dir))
            assertTrue(runCatching { links.unlink(a, MediaRole.AUDIO) }.exceptionOrNull() is ConfirmAudioLoss)
            assertEquals(first, repo.state(a, "audio"))
            links.unlink(a, MediaRole.AUDIO, confirmedAudioId = "audio")
            repo.saveState(first)
            assertNull(repo.state(a, "audio"))
            assertEquals(second, repo.state(b, "audio"))
            assertArrayEquals(byteArrayOf(1, 2, 3), original.readBytes())
        } finally {
            db.close()
            context.deleteDatabase(name)
            dir.deleteRecursively()
        }
    }
}
