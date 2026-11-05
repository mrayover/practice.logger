package com.mrayover.practicelogger

import android.graphics.Bitmap
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.OutputStream
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
            val input = File(root, "valid.zip")
            val verified = PortableZip(context, source, root).create(input)
            val path = verified.originals.single().path
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
                changeManifest { getJSONObject("counts").put("blocks", 2) }
            }
            mutate("broken relationship") {
                changeRecords { getJSONObject("tables").getJSONArray("block_media_links").getJSONArray(0).put(2, "missing") }
            }
            mutate("unknown record type") {
                changeRecords { getJSONObject("tables").put("unknown", org.json.JSONArray()) }
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

            val data = BackupRepository(source).snapshot()
            for (version in 1..7) {
                val json = JSONObject(BackupCodec.encode(data).toString(Charsets.UTF_8)).put("version", version)
                if (version < 7) json.remove("practiceNotes")
                if (version < 6) json.remove("logs")
                if (version < 5) for (key in listOf("goals", "goalBlocks", "sessionGoals")) json.remove(key)
                if (version < 4) for (key in listOf("routines", "routinePlacements")) json.remove(key)
                assertEquals("Legacy version $version", data, BackupCodec.decode(json.toString().toByteArray()))
            }
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
