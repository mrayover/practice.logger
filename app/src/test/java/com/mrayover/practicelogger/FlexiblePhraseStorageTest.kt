package com.mrayover.practicelogger

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
class FlexiblePhraseStorageTest {
    @Test fun storageBudgetAccountsForMaximumPcmAndKeepsReserve() {
        val spec=PhraseSpec(30,64);val need=recordingSpaceRequired(spec)
        assertEquals(2*1024*1024L+12_288_000+44,need)
        requireRecordingSpace(need,spec)
        for(space in listOf(0L,512*1024L,need-1)) {
            try { requireRecordingSpace(space,spec);fail("Insufficient space accepted") } catch(expected:IllegalStateException) { assertTrue(expected.message!!.contains("storage")) }
        }
    }
    @Test fun sharedCountInDefaultPersistsAndRejectsInvalidResourceRequests() {
        val context=ApplicationProvider.getApplicationContext<android.content.Context>()
        val prefs=context.getSharedPreferences("audio-settings",0);prefs.edit().clear().commit()
        try {
            assertEquals(4L,CountInPreferences(context).read())
            CountInPreferences(context).save(7);assertEquals(7L,CountInPreferences(context).read())
            for(value in listOf(0L,-1L,Long.MAX_VALUE)) {
                try { CountInPreferences(context).save(value);fail("Invalid default persisted") } catch(_:IllegalArgumentException) { }
                assertEquals(7L,CountInPreferences(context).read())
            }
            prefs.edit().putLong("count-in-beats",Long.MAX_VALUE).commit();assertEquals(4L,CountInPreferences(context).read())
        } finally { prefs.edit().clear().commit() }
    }
}
