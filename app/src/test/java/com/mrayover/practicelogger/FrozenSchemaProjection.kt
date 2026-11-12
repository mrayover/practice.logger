package com.mrayover.practicelogger

import org.json.JSONObject

/** Compare every historical column, allowing only explicitly added columns in later schemas. */
internal fun frozenColumns(version: Int, table: String): String {
    val json = BlockDatabase::class.java.classLoader!!.getResourceAsStream("com.mrayover.practicelogger.BlockDatabase/$version.json")!!
        .bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
    val tables = json.getJSONArray("entities")
    val entity = (0 until tables.length()).map(tables::getJSONObject).single { it.getString("tableName") == table }
    val fields = entity.getJSONArray("fields")
    return (0 until fields.length()).joinToString { "`" + fields.getJSONObject(it).getString("columnName") + "`" }
}
