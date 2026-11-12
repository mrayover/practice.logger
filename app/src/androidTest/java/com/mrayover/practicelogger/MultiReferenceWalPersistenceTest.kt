package com.mrayover.practicelogger

import android.app.Application
import androidx.room.RoomDatabase
import androidx.test.platform.app.InstrumentationRegistry

class MultiReferenceWalPersistenceTest : MultiReferencePersistenceChecks() {
    override val context get() = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as Application
    override val journalMode get() = RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING
    override fun resource(path: String) = InstrumentationRegistry.getInstrumentation().context.assets.open(path)
}
