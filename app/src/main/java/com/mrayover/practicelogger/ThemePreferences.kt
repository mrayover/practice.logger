package com.mrayover.practicelogger

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeChoice(val id: String, val label: String) {
    LIGHT("light", "Light"), DARK("dark", "Dark"), SYSTEM("system", "System Default");
    companion object {
        fun fromId(id: String?): ThemeChoice = entries.find { it.id == id } ?: LIGHT
    }
}
/** One activity-owned source of truth; UI observes only successfully committed choices. */
class ThemePreferences(private val preferences: SharedPreferences) {
    constructor(context: Context) : this(context.applicationContext.getSharedPreferences("appearance", Context.MODE_PRIVATE))
    private val saved = MutableStateFlow(read())
    val choice = saved.asStateFlow()
    private fun read() = ThemeChoice.fromId(runCatching { preferences.getString("palette", null) }.getOrNull())
    @Synchronized fun save(value: ThemeChoice) {
        val previous = saved.value
        if (!preferences.edit().putString("palette", value.id).commit()) {
            // A failed disk commit can still update Android's in-memory preferences.
            // Retain the committed UI choice and attempt rollback for subsequent readers.
            preferences.edit().putString("palette", previous.id).commit()
            error("Appearance could not be saved. Try again.")
        }
        saved.value = value
    }
}
val LocalThemePreferences = staticCompositionLocalOf<ThemePreferences> { error("Provide theme preferences at the app root") }
