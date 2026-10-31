package com.mrayover.practicelogger

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class) @Config(sdk=[35])
class ThemePreferencesTest {
    private fun raw(): SharedPreferences = ApplicationProvider.getApplicationContext<Context>()
        .getSharedPreferences("appearance-test",Context.MODE_PRIVATE).also { it.edit().clear().commit() }
    @Test fun existingInstallsStayLightAndEveryChoiceSurvivesNewReader() {
        val raw=raw(); val prefs=ThemePreferences(raw)
        assertEquals(ThemeChoice.LIGHT,prefs.choice.value)
        ThemeChoice.entries.forEach { choice ->
            prefs.save(choice)
            assertEquals(choice,prefs.choice.value)
            assertEquals(choice,ThemePreferences(raw).choice.value)
        }
        raw.edit().putString("palette","unrecognized").commit()
        assertEquals(ThemeChoice.LIGHT,ThemePreferences(raw).choice.value)
    }
    @Test fun failedCommitKeepsCommittedChoiceAndAllowsRetry() {
        val raw=raw(); var fail=true
        val failing=object: SharedPreferences by raw {
            override fun edit(): SharedPreferences.Editor {
                val real=raw.edit()
                return object: SharedPreferences.Editor by real {
                    override fun putString(key: String?, value: String?): SharedPreferences.Editor { real.putString(key,value); return this }
                    override fun commit(): Boolean { real.commit(); return if(fail) { fail=false; false } else true }
                }
            }
        }
        val prefs=ThemePreferences(failing)
        assertThrows(IllegalStateException::class.java) { prefs.save(ThemeChoice.DARK) }
        assertEquals(ThemeChoice.LIGHT,prefs.choice.value)
        assertEquals(ThemeChoice.LIGHT,ThemePreferences(raw).choice.value)
        prefs.save(ThemeChoice.DARK)
        assertEquals(ThemeChoice.DARK,ThemePreferences(raw).choice.value)
    }
}
