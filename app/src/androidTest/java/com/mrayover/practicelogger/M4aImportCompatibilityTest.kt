package com.mrayover.practicelogger

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.UUID

class M4aImportCompatibilityTest {
    @Test fun alternativeProviderMimeStillRequiresValidAacContent(): Unit = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val db = Room.inMemoryDatabaseBuilder(context, BlockDatabase::class.java).build()
        val directory = File(context.cacheDir, "m4a-compat-${UUID.randomUUID()}")
        try {
            val repository = MediaLibraryRepository(db, directory)
            val bytes = instrumentation.context.assets.open("audio/composer-c6a.m4a").use { it.readBytes() }
            val asset = repository.stage("Bbmaj11 groove Copy.m4a", "video/mp4", MediaRole.AUDIO) { bytes.inputStream() }
            assertEquals("audio/mp4", asset.mimeType)
            assertTrue(asset.durationMs!! > 0)
            assertArrayEquals(bytes, File(directory, asset.fileName).readBytes())

            // Same relaxed provider hint: malformed bytes must reach and fail content inspection.
            var opened = false
            val failure = runCatching {
                repository.stage("Renamed.m4a", "video/mp4", MediaRole.AUDIO) {
                    opened = true
                    "This is not AAC audio.".byteInputStream()
                }
            }.exceptionOrNull()
            assertTrue("Provider hint must reach content validation", opened)
            assertNotNull("Invalid audio must be rejected", failure)
            assertEquals(listOf(asset.fileName), directory.list()!!.toList())
            assertTrue(db.media().readAll().isEmpty()) // Editor staging does not commit rows.
        } finally {
            db.close()
            directory.deleteRecursively()
        }
    }
}
