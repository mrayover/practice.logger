package com.mrayover.practicelogger
import android.app.Application
import androidx.test.platform.app.InstrumentationRegistry
class BlockEditorCollectionNativeTest : BlockEditorCollectionChecks() {
    override val app get() = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as Application
    override fun resource(path: String) = InstrumentationRegistry.getInstrumentation().context.assets.open(path)
}
