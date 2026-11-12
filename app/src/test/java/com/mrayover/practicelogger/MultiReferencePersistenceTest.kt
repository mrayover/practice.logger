package com.mrayover.practicelogger

import android.app.Application
import androidx.room.RoomDatabase
import androidx.test.core.app.ApplicationProvider
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class) @Config(sdk = [35])
class MultiReferencePersistenceTest : MultiReferencePersistenceChecks() {
    override val context get() = ApplicationProvider.getApplicationContext<Application>()
    override val journalMode get() = RoomDatabase.JournalMode.TRUNCATE
}
