package com.mrayover.practicelogger

import android.content.Context
import org.json.JSONObject

/** Allowlist of portable settings. Recording export URIs and grants stay on the device. */
internal data class PortablePreferences(
    val theme: ThemeChoice,
    val click: ClickSound,
    val output: MetronomeMode,
    val alarm: AlarmSound,
    val countIn: Long
) {
    fun encode(): JSONObject = JSONObject()
        .put("theme", theme.id).put("click", click.name).put("output", output.name)
        .put("alarm", alarm.name).put("countIn", countIn)

    fun apply(context: Context) {
        check(context.getSharedPreferences("audio-settings", Context.MODE_PRIVATE).edit()
            .putString("click-sound", click.name).putString("output-mode", output.name)
            .putString("completion-alarm", alarm.name).putLong("count-in-beats", countIn).commit()) {
            "Audio settings could not be finalized."
        }
        ThemePreferences(context).save(theme)
    }

    companion object {
        fun snapshot(context: Context): PortablePreferences {
            val audio = AudioPreferences(context)
            return PortablePreferences(ThemePreferences(context).choice.value, audio.sound, audio.mode,
                audio.alarm, CountInPreferences(context).read())
        }

        fun decode(json: JSONObject): PortablePreferences {
            require(json.keys().asSequence().toSet() == setOf("theme", "click", "output", "alarm", "countIn"))
            val theme = ThemeChoice.entries.first { it.id == json.get("theme") }
            val count = json.get("countIn")
            require(count is Int || count is Long)
            val countIn = (count as Number).toLong()
            PhraseSpec(300, 1, countIn)
            return PortablePreferences(theme, ClickSound.valueOf(json.get("click") as String),
                MetronomeMode.valueOf(json.get("output") as String), AlarmSound.valueOf(json.get("alarm") as String), countIn)
        }
    }
}
