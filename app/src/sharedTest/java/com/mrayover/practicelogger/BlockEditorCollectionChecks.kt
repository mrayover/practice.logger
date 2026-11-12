package com.mrayover.practicelogger

import android.app.Application
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.UUID

/** Same preservation, collection and manifest checks on host and Android SQLite. */
abstract class BlockEditorCollectionChecks {
    protected abstract val app: Application
    protected open val journalMode = RoomDatabase.JournalMode.TRUNCATE
    protected open fun resource(path: String): java.io.InputStream = javaClass.classLoader!!.getResourceAsStream(path)!!
    private suspend fun fixture(action: suspend (BlockDatabase, MediaLibraryRepository, BlockMediaRepository, () -> BlockEditorMedia) -> Unit) {
        val name = "c-${UUID.randomUUID().toString().take(8)}.db"
        MultiReferenceLegacyFixture(app, name, app.filesDir, ::resource).create()
        val db = Room.databaseBuilder(app, BlockDatabase::class.java, name).addMigrations(MULTI_REFERENCE_MIGRATION_23_24).setJournalMode(journalMode).build()
        val library = MediaLibraryRepository(db, File(app.filesDir, "media-library"))
        val legacy = ImageReferenceRepository(db, File(app.filesDir, "block-images"))
        val links = BlockMediaRepository(db, library, legacy)
        val editors = mutableListOf<BlockEditorMedia>()
        try { action(db, library, links) { BlockEditorMedia(app, db, library, legacy, links).also(editors::add) } }
        finally {
            editors.forEach { runCatching { it.discard() } }
            db.media().readAll().forEach { File(app.filesDir, "media-library/${it.fileName}").delete() }
            db.images().readAll().forEach { File(app.filesDir, "block-images/${it.fileName}").delete() }
            db.close(); app.deleteDatabase(name)
        }
    }
    private suspend fun rejected(action: suspend () -> Unit) { var failed = false; try { action() } catch (_: Exception) { failed = true }; assertTrue("Unsafe operation succeeded", failed) }
    private fun id(n: Int) = "00000000-0000-4000-8000-${n.toString().padStart(12, '0')}"
    private suspend fun extra(db: BlockDatabase, from: String): MediaAsset {
        val original = db.media().find(from)!!
        val uuid = UUID.randomUUID().toString()
        val asset = original.copy(id = uuid, fileName = "$uuid.${original.fileName.substringAfterLast('.')}")
        File(app.filesDir, "media-library/${original.fileName}").copyTo(File(app.filesDir, "media-library/${asset.fileName}"))
        db.media().insert(asset); return asset
    }
    @Test fun mixedCollectionsIndependentOrderDuplicateCancelAndExactlyOnceFirstSave(): Unit = runBlocking { fixture { db, library, _, editor ->
        val audio2 = extra(db, id(3))
        val token = UUID.randomUUID().toString(); var draft = editor(); draft.open(token, null)
        assertTrue(draft.choose(MediaRole.VISUAL, id(1))); draft.choose(MediaRole.VISUAL, id(2))
        draft.choose(MediaRole.AUDIO, id(3)); draft.choose(MediaRole.AUDIO, audio2.id)
        assertFalse(draft.choose(MediaRole.VISUAL, id(1)))
        draft.move(MediaRole.VISUAL, "library:${id(2)}", -1)
        val visuals = listOf("library:${id(2)}", "library:${id(1)}")
        val audios = listOf("library:${id(3)}", "library:${audio2.id}")
        draft = editor(); draft.open(token, null)
        assertEquals(visuals, draft.visualReferences.map { it.identity }); assertEquals(audios, draft.audioReferences.map { it.identity })
        val week = practiceWeek(java.time.LocalDate.of(2026, 11, 11))
        val beforeBlocks = db.blocks().readAll(); val beforeSessions = db.sessions().readAll()
        rejected { draft.save(null, BlockDraft(), week, true) }
        rejected { draft.save(null, BlockDraft("New collection", "Play", primaryGoalId = "goal", primaryGoalChanged = true), week, true) { error("Injected full collection rollback") } }
        assertEquals(beforeBlocks, db.blocks().readAll()); assertEquals(beforeSessions, db.sessions().readAll())
        assertEquals(2, draft.visualReferences.size); assertEquals(2, draft.audioReferences.size)
        val saved = draft.save(null, BlockDraft("New collection", "Play"), week, true)
        assertEquals(visuals, db.referenceOrder(saved, MediaRole.VISUAL)); assertEquals(audios, db.referenceOrder(saved, MediaRole.AUDIO))
        assertTrue(db.weekPlan().read(week.toString()).any { it.blockId == saved })
        val sessions = db.sessions().readAll()
        val recovered = editor(); recovered.open(token, null)
        assertEquals(saved, recovered.completedId); assertEquals(saved, recovered.save(null, BlockDraft("Ignored replay", "Play"), week, true))
        assertEquals(sessions, db.sessions().readAll()); assertTrue(library.available(audio2))
        // Even before the post-commit receipt was synced, the creation UUID witnesses the transaction.
        val manifest = File(app.filesDir, "block-editor-staging/$token/draft.json")
        manifest.writeText(JSONObject(manifest.readText()).put("savedId", "").toString())
        val interrupted = editor(); interrupted.open(token, null); assertEquals(saved, interrupted.completedId)
        assertEquals(sessions, db.sessions().readAll())
    } }
    @Test fun legacyCoexistenceAndReorderKeepEveryUnchangedLocalRow(): Unit = runBlocking { fixture { db, _, _, editor ->
        val old = db.images().find("legacy")!!
        val overlays = db.overlays().read("legacy", "legacy:${old.fileName}")
        val audioState = db.blockAudio().state("image", id(3)); val markers = db.blockAudio().read("image", id(3))
        val draft = editor(); draft.open(UUID.randomUUID().toString(), "legacy")
        draft.choose(MediaRole.VISUAL, id(1)); draft.choose(MediaRole.VISUAL, id(2)); draft.choose(MediaRole.AUDIO, id(3))
        draft.move(MediaRole.VISUAL, "library:${id(2)}", -1); draft.move(MediaRole.VISUAL, "library:${id(2)}", -1)
        draft.save("legacy", BlockDraft.from(db.blocks().find("legacy")!!), null)
        assertEquals(listOf("library:${id(2)}", "legacy:${old.fileName}", "library:${id(1)}"), db.referenceOrder("legacy", MediaRole.VISUAL))
        assertEquals(old.copy(position = 1), db.images().find("legacy")); assertEquals(overlays, db.overlays().read("legacy", "legacy:${old.fileName}"))
        assertEquals(audioState, db.blockAudio().state("image", id(3))); assertEquals(markers, db.blockAudio().read("image", id(3)))
        // Minor field edit does not delete/reinsert a single unchanged link.
        val before = db.blockMedia().readAll(); val edit = editor(); edit.open(UUID.randomUUID().toString(), "image")
        edit.save("image", BlockDraft("Renamed", "Still play"), null)
        assertEquals(before, db.blockMedia().readAll()); assertEquals(audioState, db.blockAudio().state("image", id(3))); assertEquals(markers, db.blockAudio().read("image", id(3)))
    } }
    @Test fun exactRemovalRequiresConfirmationAndPreservesOtherSourcesAndSharedOriginals(): Unit = runBlocking { fixture { db, library, links, editor ->
        val audio2 = extra(db, id(3)); links.add("image", MediaRole.AUDIO, audio2.id)
        links.add("image", MediaRole.VISUAL, id(2))
        val originalState = db.blockAudio().state("image", id(3)); val markers = db.blockAudio().read("image", id(3))
        val overlays = db.overlays().read("image", "library:${id(1)}")
        val edit = editor(); edit.open(UUID.randomUUID().toString(), "image")
        rejected { edit.remove(MediaRole.AUDIO, "library:${audio2.id}") }
        edit.remove(MediaRole.AUDIO, "library:${audio2.id}", true); edit.remove(MediaRole.VISUAL, "library:${id(2)}", true)
        edit.save("image", BlockDraft.from(db.blocks().find("image")!!), null)
        assertEquals(listOf("library:${id(3)}"), db.referenceOrder("image", MediaRole.AUDIO))
        assertEquals(originalState, db.blockAudio().state("image", id(3))); assertEquals(markers, db.blockAudio().read("image", id(3)))
        assertEquals(overlays, db.overlays().read("image", "library:${id(1)}")); assertTrue(library.available(audio2))
        assertTrue(db.blockMedia().contains("archived", "AUDIO", id(3)))
        // Exact-source annotated removal discards only that Block's work after explicit review.
        val remove = editor(); remove.open(UUID.randomUUID().toString(), "image")
        remove.remove(MediaRole.VISUAL, "library:${id(1)}", true); remove.remove(MediaRole.AUDIO, "library:${id(3)}", true)
        remove.save("image", BlockDraft.from(db.blocks().find("image")!!), null)
        assertTrue(db.overlays().read("image", "library:${id(1)}").isEmpty()); assertNull(db.blockAudio().state("image", id(3)))
        assertNotNull(db.blockAudio().state("archived", id(3))); assertTrue(library.available(db.media().find(id(1))!!)); assertTrue(library.available(db.media().find(id(3))!!))
    } }
    @Test fun staleCollectionAndLateAudioWorkRejectWithoutPartialWrites(): Unit = runBlocking { fixture { db, _, links, editor ->
        val edit = editor(); edit.open(UUID.randomUUID().toString(), "image")
        links.add("image", MediaRole.VISUAL, id(2))
        val before = db.blocks().find("image")
        rejected { edit.save("image", BlockDraft("Stale name", "Play"), null) }
        assertEquals(before, db.blocks().find("image")); assertTrue(edit.ready)
        val removal = editor(); removal.open(UUID.randomUUID().toString(), "image"); removal.remove(MediaRole.AUDIO, "library:${id(3)}", true)
        val updated = db.blockAudio().state("image", id(3))!!.copy(speedPercent = 90)
        db.blockAudio().save(updated)
        rejected { removal.save("image", BlockDraft("No partial name", "Play"), null) }
        assertEquals(before, db.blocks().find("image")); assertEquals(updated, db.blockAudio().state("image", id(3)))
    } }
    @Test fun scalarManifestUpgradeRetainsIntentAndOtherAssociations(): Unit = runBlocking { fixture { db, _, links, editor ->
        links.add("image", MediaRole.VISUAL, id(2))
        val token = UUID.randomUUID().toString(); val draft = editor(); draft.open(token, "image")
        val manifest = File(app.filesDir, "block-editor-staging/$token/draft.json")
        val json = JSONObject(manifest.readText()).put("version", 1).put("visualChanged", true).put("audioChanged", false)
            .put("identity", "library:${id(1)}").put("markings", true).put("originalVisual", id(1)).put("originalAudio", id(3))
            .put("visual", "").put("audio", id(3))
        json.remove("visuals"); json.remove("audios"); manifest.writeText(json.toString())
        val restored = editor(); restored.open(token, "image")
        assertEquals(listOf("library:${id(2)}"), restored.visualReferences.map { it.identity }); assertEquals(id(3), restored.audio!!.id)
        restored.save("image", BlockDraft.from(db.blocks().find("image")!!), null)
        assertEquals(listOf("library:${id(2)}"), db.referenceOrder("image", MediaRole.VISUAL))
        assertEquals(2, JSONObject(manifest.readText()).getInt("version"))
    } }
    @Test fun existingAllItemReceiptRecoversCommitWithoutStagedSubsetInference(): Unit = runBlocking { fixture { db, _, _, editor ->
        val token = UUID.randomUUID().toString(); val edit = editor(); edit.open(token, "legacy")
        edit.choose(MediaRole.VISUAL, id(1)); edit.choose(MediaRole.AUDIO, id(3))
        edit.save("legacy", BlockDraft("Edited collection", "Play"), null)
        val manifest = File(app.filesDir, "block-editor-staging/$token/draft.json")
        manifest.writeText(JSONObject(manifest.readText()).put("savedId", "").toString())
        val reopened = editor(); reopened.open(token, "legacy"); assertEquals("legacy", reopened.completedId)
        val before = db.blocks().find("legacy")
        assertEquals("legacy", reopened.save("legacy", BlockDraft("Must not replay", "Play"), null)); assertEquals(before, db.blocks().find("legacy"))
    } }
}
