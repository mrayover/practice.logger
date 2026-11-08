package com.mrayover.practicelogger

import android.graphics.Bitmap
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.time.LocalDate
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

class PortableArchiveValidationTest {
    @Test fun rejectsMalformedArchivesWithoutMutatingEmptyTarget() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "s2-invalid-${UUID.randomUUID()}"
        val root = File(context.filesDir, name).apply { check(mkdir()) }
        val targetFiles = File(root, "target").apply { check(mkdir()) }
        val source = BlockDatabase.open(context, "$name-source.db")
        val target = BlockDatabase.open(context, "$name-target.db")
        try {
            source.blocks().insert(Block("block", "Block", "Play", createdAt = 1, updatedAt = 1))
            val png = File(root, "valid.png")
            val bitmap = Bitmap.createBitmap(16, 24, Bitmap.Config.ARGB_8888)
            png.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            bitmap.recycle()
            val library = MediaLibraryRepository(source, File(root, "media-library"))
            val original = library.import(png.name, "image/png", open = png::inputStream)
            BlockMediaRepository(source, library, ImageReferenceRepository(source, File(root, "block-images")))
                .link("block", MediaRole.VISUAL, original.id)
            source.blocks().insert(Block("old", "Legacy score", "Play", createdAt = 1, updatedAt = 1))
            val legacy = ImageReferenceRepository(source, File(root, "block-images"))
                .import("old") { l1Fixture("references/score.pdf").inputStream() }
            val overlays = PracticeOverlayRepository(source)
            overlays.edit(OverlayEdit(null, PracticeOverlay("image", "block", "library:${original.id}", 0,
                "HIGHLIGHT", .1f, .2f, .7f, .8f, 1, 0)), 1)
            overlays.edit(OverlayEdit(null, PracticeOverlay("pdf", "old", "legacy:${legacy.fileName}", 1,
                "COVER", .15f, .25f, .55f, .65f, 0, 0)), 2)
            val goal = GoalRepository(source).save(null, "Archive Goal", "", 0, "")
            val draft = BlockDraft.from(source.blocks().find("block")!!)
            BlockRepository(source).save("block", draft.copy(primaryGoalId = goal, primaryGoalChanged = true))
            val sessions = SessionRepository(source)
            sessions.addBlock("block")
            val sessionId = sessions.start()
            val placementId = source.sessions().find(sessionId)!!.current!!.id
            sessions.checkpointPrep(sessionId, placementId, 1200)
            sessions.checkpoint(sessionId, 2000)
            sessions.finishBlock(sessionId, "COMPLETED", "", placementId = placementId)
            sessions.saveBlockReflection(BlockReflection(placementId, 5, focus = "Steady entrances"))
            sessions.saveSessionReflection(SessionReflection(sessionId, energy = "Good"), "Session note")
            val week = LocalDate.of(2026, 11, 8)
            BlockRepository(source).addToWeek(week, "block")
            BlockRepository(source).addToWeek(week, "old")
            BlockRepository(source).setArchived("old", true)
            val input = File(root, "valid.zip")
            val verified = PortableZip(context, source, root).create(input)
            val path = verified.originals.single { it.store == "media-library" }.path
            val entries = ZipFile(input).use { zip ->
                zip.entries().asSequence().associateTo(linkedMapOf()) { entry ->
                    entry.name to zip.getInputStream(entry).use { it.readBytes() }
                }
            }
            val restore = PortableRestore(context, target, targetFiles)
            val bad = File(root, "bad.zip")
            suspend fun reject(label: String) {
                assertTrue(label, runCatching { restore.stage(bad::inputStream) }.isFailure)
                assertTrue(target.blocks().readAll().isEmpty())
                assertTrue(target.media().readAll().isEmpty())
                assertEquals(0, target.overlays().count())
                assertFalse(File(targetFiles, "media-library").exists())
                assertTrue(File(targetFiles, "portable-restore").list().orEmpty().isEmpty())
            }
            suspend fun mutate(label: String, change: suspend MutableMap<String, ByteArray>.() -> Unit) {
                val modified = entries.toMutableMap()
                modified.change()
                write(bad, modified)
                reject(label)
            }

            mutate("wrong media digest") { this[path] = getValue(path).copyOf().also { it[4] = (it[4].toInt() xor 1).toByte() } }
            mutate("missing original") { remove(path) }
            mutate("missing manifest") { remove("manifest.json") }
            mutate("traversal") { this["../outside.png"] = getValue(path) }
            mutate("backslash path") { this["originals\\invalid.png"] = getValue(path) }
            assertFalse(File(root, "outside.png").exists())
            mutate("unsupported version") { changeManifest { put("version", 100) } }
            mutate("invalid security rating") {
                changeRecords { getJSONObject("tables").getJSONArray("block_reflections").getJSONArray(0).put(1, 6) }
            }
            mutate("invalid completion source") {
                changeRecords { getJSONObject("tables").getJSONArray("block_completions").getJSONArray(0).put(2, "old") }
            }
            mutate("invalid size type") { changeManifest { getJSONArray("entries").getJSONObject(0).put("bytes", "1") } }
            mutate("duplicate manifest field") {
                val originalJson = getValue("manifest.json").toString(Charsets.UTF_8)
                this["manifest.json"] = ("{\"version\":1," + originalJson.drop(1)).toByteArray()
            }
            mutate("duplicate IDs") {
                changeRecords {
                    val blocks = getJSONObject("tables").getJSONArray("blocks")
                    blocks.put(blocks.getJSONArray(0))
                }
                changeManifest { getJSONObject("counts").put("blocks", 3) }
            }
            mutate("broken relationship") {
                changeRecords { getJSONObject("tables").getJSONArray("block_media_links").getJSONArray(0).put(2, "missing") }
            }
            mutate("unknown record type") {
                changeRecords { getJSONObject("tables").put("unknown", org.json.JSONArray()) }
            }

            mutate("primary without Goal link") {
                changeRecords { getJSONObject("tables").getJSONArray("block_primary_goals").getJSONArray(0).put(0, "old") }
            }
            for (priority in listOf(0, 6)) {
                mutate("historical priority $priority") {
                    changeRecords { getJSONObject("tables").getJSONArray("placement_goal_snapshots").getJSONArray(0).put(2, priority) }
                }
            }
            mutate("rank without a Goal") {
                changeRecords { getJSONObject("tables").getJSONArray("placement_goal_snapshots").getJSONArray(0).put(1, JSONObject.NULL) }
            }
            mutate("historical Goal missing") {
                changeRecords { getJSONObject("tables").getJSONArray("placement_goal_snapshots").getJSONArray(0).put(1, "missing") }
            }
            mutate("snapshot on zero time") {
                changeRecords { getJSONObject("tables").getJSONArray("placements").getJSONArray(0).put(9, 0) }
            }

            suspend fun badMark(label: String, row: Int, column: Int, value: Any) = mutate(label) {
                changeRecords { getJSONObject("tables").getJSONArray("practice_overlays").getJSONArray(row).put(column, value) }
            }
            badMark("inverted overlay rectangle", 0, 5, .99)
            badMark("out of range overlay rectangle", 0, 8, 1.01)
            badMark("string overlay coordinate", 0, 5, "0.1")
            badMark("missing overlay owner", 0, 1, "missing")
            badMark("overlay reference mismatch", 0, 2, "library:other")
            badMark("image overlay page", 0, 3, 1)
            badMark("legacy overlay page beyond the verified original", 1, 3, 2)
            badMark("unsupported tool", 0, 4, "UNKNOWN")
            badMark("unsupported appearance", 0, 9, 5)
            badMark("negative order", 0, 10, -1)
            badMark("duplicate overlay ID", 0, 0, "pdf")
            mutate("non-Sunday week") {
                changeRecords { getJSONObject("tables").getJSONArray("week_blocks").getJSONArray(0).put(0, "2026-11-09") }
            }
            mutate("missing weekly Block") {
                changeRecords { getJSONObject("tables").getJSONArray("week_blocks").getJSONArray(0).put(1, "missing") }
            }
            mutate("unsupported records version") { changeRecords { put("version", 100) } }
            mutate("missing required overlay records") { changeRecords { getJSONObject("tables").remove("practice_overlays") } }
            mutate("too many overlays on one page") {
                changeRecords {
                    val marks = getJSONObject("tables").getJSONArray("practice_overlays")
                    val template = marks.getJSONArray(0).toString()
                    repeat(OVERLAYS_PER_PAGE) { index ->
                        marks.put(JSONArray(template).put(0, "extra-$index").put(10, index + 1))
                    }
                }
                changeManifest { getJSONObject("counts").put("practice_overlays", OVERLAYS_PER_PAGE + 2) }
            }

            val alias = path.dropLast(1) + "z"
            write(bad, entries + (alias to entries.getValue(path)))
            val duplicate = bad.readBytes()
            val from = alias.toByteArray()
            val to = path.toByteArray()
            for (i in 0..duplicate.size - from.size) {
                if (from.indices.all { duplicate[i + it] == from[it] }) to.copyInto(duplicate, i)
            }
            bad.writeBytes(duplicate)
            reject("duplicate ZIP paths")

            write(bad, entries)
            val bytes = bad.readBytes()
            val little = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            val directoryOffset = little.getInt(bytes.size - 6)
            little.putInt(directoryOffset + 24, 0xc0000001.toInt())
            bad.writeBytes(bytes)
            reject("oversized expanded archive")

            val low = PortableRestore(context, target, targetFiles, freeBytes = { 0 })
            val noSpace = runCatching { low.stage(input::inputStream) }.exceptionOrNull()
            assertTrue(noSpace?.message.orEmpty().contains("Free private storage"))
            assertTrue(File(targetFiles, "portable-restore").list().orEmpty().isEmpty())
            assertTrue(target.blocks().readAll().isEmpty())

            val data = BackupRepository(source).snapshot().let { it.copy(placements = it.placements.map { row -> row.copy(prepMs = 0) }) }
            for (version in 1..7) {
                val json = JSONObject(BackupCodec.encode(data).toString(Charsets.UTF_8)).put("version", version)
                if (version < 7) json.remove("practiceNotes")
                if (version < 6) json.remove("logs")
                if (version < 5) for (key in listOf("goals", "goalBlocks", "sessionGoals")) json.remove(key)
                if (version < 4) for (key in listOf("routines", "routinePlacements")) json.remove(key)
                val expected = if (version < 5) data.copy(goals = emptyList(), goalBlocks = emptyList()) else data
                assertEquals("Legacy version $version", expected, BackupCodec.decode(json.toString().toByteArray()))
            }

            // Downgrade the wire records and recompute the digest and inventory for each prior format.
            for (version in 1..PORTABLE_VERSION) {
                val excluded = buildList {
                    if (version < 6) addAll(listOf("block_completions", "block_reflections", "session_reflections"))
                    if (version < 4) add("week_blocks")
                    if (version < 3) addAll(listOf("block_primary_goals", "placement_goal_snapshots"))
                    if (version < 2) add("practice_overlays")
                }
                val dbName = "$name-v$version.db"
                val destination = if (version == 1) target else BlockDatabase.open(context, dbName)
                try {
                    val prior = entries.toMutableMap().apply {
                        changeRecords {
                            if (version == 1) remove("version") else put("version", version)
                            val tables = getJSONObject("tables")
                            excluded.forEach { tables.remove(it) }
                            if (version < 5) tables.getJSONArray("placements").getJSONArray(0).remove(14)
                        }
                        changeManifest {
                            put("version", version)
                            put("roomSchema", 16 + version)
                            val counts = getJSONObject("counts")
                            excluded.forEach { counts.remove(it) }
                        }
                    }
                    val archive = File(root, "compatible-v$version.zip")
                    write(archive, prior)
                    val files = if (version == 1) targetFiles else File(root, "v$version-target").apply { mkdir() }
                    val reader = PortableRestore(context, destination, files)
                    reader.restore(reader.stage(archive::inputStream))
                    val restored = BackupRepository(destination).snapshot()
                    assertEquals(if (version >= 5) 1200L else 0L, restored.placements.single().prepMs)
                    assertEquals(data, restored.copy(placements = restored.placements.map { it.copy(prepMs = 0) }))
                    val history = destination.sessions().find(sessionId)!!
                    val originalHistory = source.sessions().find(sessionId)!!
                    assertEquals(originalHistory.completions.takeIf { version >= 6 }.orEmpty(), history.completions)
                    assertEquals(originalHistory.reflection.takeIf { version >= 6 }, history.reflection)
                    assertEquals(if (version == 1) 0 else 2, destination.overlays().count())
                    assertEquals(goal.takeIf { version >= 3 }, destination.goalAttribution().primaryGoalId("block"))
                    assertEquals(source.goalAttribution().snapshot(data.placements.single().id).takeIf { version >= 3 },
                        destination.goalAttribution().snapshot(data.placements.single().id))
                    assertEquals(source.weekPlan().read(week.toString()).takeIf { version >= 4 }.orEmpty(),
                        destination.weekPlan().read(week.toString()))
                } finally {
                    if (version != 1) {
                        destination.close()
                        context.deleteDatabase(dbName)
                    }
                }
            }
            assertEquals(original, target.media().find(original.id))
            assertEquals(legacy, target.images().find("old"))
        } finally {
            source.close()
            target.close()
            context.deleteDatabase("$name-source.db")
            context.deleteDatabase("$name-target.db")
            root.deleteRecursively()
        }
        Unit
    }

    @Test fun destinationCopyDistinguishesWriteCloseAndReadBackFailures() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File.createTempFile("s2-write", ".zip", context.cacheDir)
        try {
            val bytes = ByteArray(128 * 1024) { (it % 239).toByte() }
            file.writeBytes(bytes)
            val partial = object : OutputStream() {
                private var written = 0
                override fun write(b: Int) {
                    if (written++ == 512) throw IOException("Provider ran out of storage")
                }
            }
            val closeFailure = object : ByteArrayOutputStream() {
                override fun close() { throw IOException("Provider rejected close") }
            }
            for (output in listOf(partial, closeFailure)) {
                var reopened = false
                val failed = runCatching {
                    copyZipToDestination(file, { output }) { reopened = true; bytes.inputStream() }
                }.exceptionOrNull()
                assertTrue(failed is IOException)
                assertFalse(reopened)
            }
            assertFalse(copyZipToDestination(file, { ByteArrayOutputStream() }) { null })
            assertTrue(runCatching {
                copyZipToDestination(file, { ByteArrayOutputStream() }) { ByteArray(bytes.size).inputStream() }
            }.isFailure)
            assertTrue(copyZipToDestination(file, { ByteArrayOutputStream() }) { bytes.inputStream() })
            assertArrayEquals(bytes, file.readBytes())
        } finally { file.delete() }
        Unit
    }

    private fun write(file: File, entries: Map<String, ByteArray>) {
        ZipOutputStream(file.outputStream()).use { zip ->
            entries.forEach { (name, bytes) ->
                zip.putNextEntry(ZipEntry(name)); zip.write(bytes); zip.closeEntry()
            }
        }
    }

    private fun MutableMap<String, ByteArray>.changeManifest(update: JSONObject.() -> Unit) {
        this["manifest.json"] = JSONObject(getValue("manifest.json").toString(Charsets.UTF_8)).apply(update).toString().toByteArray()
    }

    private suspend fun MutableMap<String, ByteArray>.changeRecords(update: JSONObject.() -> Unit) {
        val revised = JSONObject(getValue("records.json").toString(Charsets.UTF_8)).apply(update).toString().toByteArray()
        this["records.json"] = revised
        val digest = revised.inputStream().use { copyPortable(it, null, revised.size.toLong()) }
        changeManifest {
            val entries = getJSONArray("entries")
            for (index in 0 until entries.length()) {
                val entry = entries.getJSONObject(index)
                if (entry.getString("path") == "records.json") entry.put("bytes", digest.bytes).put("sha256", digest.sha256)
            }
        }
    }
}
