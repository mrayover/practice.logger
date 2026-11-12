package com.mrayover.practicelogger

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.UUID
import java.util.zip.ZipFile

/** Fixed bytes checked into assets; no current writer participates in these historical inputs. */
class FrozenPortableCompatibilityTest {
    @Test fun allFrozenHistoricalVersionsRestoreOriginalsAndColdReopen() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        for (version in 1..7) {
            val name = "frozen-$version-${UUID.randomUUID()}.db"
            val files = File(context.filesDir, name)
            try {
                val archive = File(context.cacheDir, "$name.zip")
                instrumentation.context.assets.open("stage3b/portable-v$version.zip").use { input -> archive.outputStream().use { input.copyTo(it) } }
                var db = BlockDatabase.open(context, name)
                val verified = try {
                    val restore = PortableRestore(context, db, files)
                    val staged = restore.stage(archive::inputStream)
                    restore.restore(staged)
                    staged.verified
                } finally { db.close() }
                db = BlockDatabase.open(context, name)
                try {
                    val sql = db.openHelper.writableDatabase
                    for (table in portableTables) {
                        val expected = verified.records.tables.optJSONArray(table.name) ?: JSONArray()
                        val normalized = JSONArray(expected.toString())
                        repeat(normalized.length()) { i ->
                            val row = normalized.getJSONArray(i)
                            if (table.name in setOf("block_images", "block_media_links")) row.put(0)
                            if (version < 5 && table.name == "placements") row.put(0)
                        }
                        assertEquals("v$version/${table.name}", normalized.toString(), table.snapshot(sql) {}.toString())
                    }
                    ZipFile(archive).use { zip -> for (original in verified.originals) {
                        assertArrayEquals("v$version/${original.path}", zip.getInputStream(zip.getEntry(original.path)).use { it.readBytes() }, original.file(files).readBytes())
                    } }
                    assertEquals(0, db.images().readAll().single().position)
                    assertTrue(db.blockMedia().readAll().all { it.position == 0 })
                    assertFalse(portableTargetEmpty(sql))
                } finally { db.close(); archive.delete() }
            } finally { context.deleteDatabase(name); files.deleteRecursively() }
        }
    }
}
