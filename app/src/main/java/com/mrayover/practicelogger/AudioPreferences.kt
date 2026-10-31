package com.mrayover.practicelogger

import android.content.Context
import android.content.SharedPreferences

enum class ClickSound(val label: String, val frequency: Double, val amplitude: Int) {
    CLASSIC("Classic click",1800.0,20000), WOOD("Woodblock",900.0,20000),
    SOFT("Soft tick",1200.0,10000), ELECTRONIC("Electronic pulse",3000.0,16000)
}
enum class AlarmSound(val label: String) {
    TWO_TONE("Two-tone"), BELL("Bell"), RISING("Rising chime"), BEEP("Short beep"), OFF("Off")
}
class AudioPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("audio-settings",Context.MODE_PRIVATE)
    private inline fun <reified T: Enum<T>> read(key: String, fallback: T): T =
        runCatching { enumValueOf<T>(prefs.getString(key,fallback.name)!!) }.getOrDefault(fallback)
    val sound get() = read("click-sound",ClickSound.CLASSIC)
    val mode get() = read("output-mode",MetronomeMode.SOUND_FLASH)
    val alarm get() = read("completion-alarm",AlarmSound.TWO_TONE)
    private fun save(key: String, value: String) { check(prefs.edit().putString(key,value).commit()) { "Audio setting could not be saved. Try again." } }
    fun saveSound(value: ClickSound) = save("click-sound",value.name)
    fun saveMode(value: MetronomeMode) = save("output-mode",value.name)
    fun saveAlarm(value: AlarmSound) = save("completion-alarm",value.name)
    fun applyTo(config: MetronomeConfig) = config.copy(mode=mode,sound=sound)
    fun observe(changed: () -> Unit): () -> Unit {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "click-sound" || key == "output-mode") changed()
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        return { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
}
