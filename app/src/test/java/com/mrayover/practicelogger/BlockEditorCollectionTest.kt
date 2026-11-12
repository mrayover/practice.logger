package com.mrayover.practicelogger
import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
@RunWith(RobolectricTestRunner::class) @Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BlockEditorCollectionTest : BlockEditorCollectionChecks() {
    override val app get() = ApplicationProvider.getApplicationContext<Application>()
}
