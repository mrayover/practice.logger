package com.mrayover.practicelogger

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class FoundationTest {
    @Test fun fileDatabaseSurvivesCloseAndReopen() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        context.deleteDatabase("foundation-test.db")
        val original = Block("stable-id", "Bridge", "Play from memory", createdAt = 1, updatedAt = 1)
        BlockDatabase.open(context, "foundation-test.db").let { db ->
            try { db.blocks().insert(original) } finally { db.close() }
        }
        BlockDatabase.open(context, "foundation-test.db").let { db ->
            try { assertEquals(original, db.blocks().find("stable-id")) } finally { db.close() }
        }
        context.deleteDatabase("foundation-test.db")
        Unit
    }
    @Test fun everyMaterialTextStyleUsesBundledLato() {
        with(LatoTypography) {
            listOf(displayLarge, displayMedium, displaySmall, headlineLarge, headlineMedium, headlineSmall,
                titleLarge, titleMedium, titleSmall, bodyLarge, bodyMedium, bodySmall, labelLarge, labelMedium, labelSmall)
                .forEach { assertEquals(Lato, it.fontFamily) }
        }
    }
}
