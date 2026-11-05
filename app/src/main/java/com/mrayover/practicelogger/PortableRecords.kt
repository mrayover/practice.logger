package com.mrayover.practicelogger

import android.content.Context
import android.database.Cursor
import androidx.room.Room
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import org.json.JSONArray
import org.json.JSONObject

internal const val ZIP_RECORD_LIMIT = 20_000

/** Version 1 wire columns are frozen independently of future Room migrations. */
internal class PortableTable(val name: String, fields: String, val legacyKey: String? = null) {
    val columns = fields.split(' ').map { it.substringBefore(':') }
    private val types = fields.split(' ').map { it.substringAfter(':', "s") }
    private val projection = columns.joinToString { "`$it`" }
    private val select = "SELECT $projection FROM `$name` ORDER BY $projection"
    private val insert = "INSERT INTO `$name` ($projection) VALUES (${columns.joinToString { "?" }})"

    fun snapshot(sql: SupportSQLiteDatabase, onRow: (JSONArray) -> Unit): JSONArray =
        sql.query(select).use { cursor ->
            JSONArray().apply {
                while (cursor.moveToNext()) {
                    require(length() < ZIP_RECORD_LIMIT) { "Too many $name records for portable backup." }
                    put(JSONArray().apply {
                        for (index in columns.indices) put(readCell(cursor, index))
                    }.also(onRow))
                }
            }
        }

    private fun readCell(cursor: Cursor, index: Int): Any = if (cursor.isNull(index)) JSONObject.NULL else
        when (types[index].removeSuffix("?")) {
            "s" -> cursor.getString(index)
            "b" -> cursor.getLong(index) != 0L
            else -> cursor.getLong(index)
        }

    fun row(values: JSONArray): JSONObject {
        require(values.length() == columns.size) { "Unexpected columns in $name." }
        return JSONObject().apply {
            columns.forEachIndexed { i, column ->
                val value = values.get(i)
                val type = types[i]
                require(if (value === JSONObject.NULL) type.endsWith('?') else when (type.removeSuffix("?")) {
                    "s" -> value is String
                    "b" -> value is Boolean
                    "i" -> value is Int || value is Long && value in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()
                    else -> value is Int || value is Long
                }) { "Invalid $name field $column." }
                put(column, value)
            }
        }
    }

    fun restore(sql: SupportSQLiteDatabase, rows: JSONArray) {
        sql.compileStatement(insert).use { statement ->
            for (i in 0 until rows.length()) {
                val values = rows.getJSONArray(i)
                for (j in columns.indices) {
                    when (val value = values.get(j)) {
                        JSONObject.NULL -> statement.bindNull(j + 1)
                        is String -> statement.bindString(j + 1, value)
                        is Boolean -> statement.bindLong(j + 1, if (value) 1 else 0)
                        is Number -> statement.bindLong(j + 1, value.toLong())
                        else -> error("Invalid record cell.")
                    }
                }
                statement.executeInsert()
            }
        }
    }
}

internal val portableTables = listOf(
    PortableTable("blocks", "id name action durationMinutes:i tempo:i? notes archived:b createdAt:l updatedAt:l", "blocks"),
    PortableTable("sessions", "id startedAt:l endedAt:l? status currentIndex:i note", "sessions"),
    PortableTable("placements", "id sessionId sourceBlockId name action tempo:i? referenceNotes position:i configuredMs:l activeMs:l outcome overtimeAccepted:b note lastUsedBpm:i?", "placements"),
    PortableTable("takes", "id blockId:s? createdAt:l mode durationMs:l fileName label recordedBpm:i? beatCount:i?"),
    PortableTable("routines", "id name createdAt:l updatedAt:l", "routines"),
    PortableTable("routine_placements", "id routineId sourceBlockId position:i configuredMs:l", "routinePlacements"),
    PortableTable("goals", "id title description progress:i notes completed:b priority:i createdAt:l updatedAt:l", "goals"),
    PortableTable("goal_blocks", "goalId blockId position:i", "goalBlocks"),
    PortableTable("session_goals", "sessionId goalId", "sessionGoals"),
    PortableTable("daily_logs", "date text", "logs"),
    PortableTable("block_images", "blockId fileName mimeType width:i height:i byteCount:l"),
    PortableTable("practice_notes", "id blockId sessionId placementId createdAt:l text", "practiceNotes"),
    PortableTable("media_assets", "id title originalName mimeType category fileName byteCount:l importedAt:l width:i? height:i? pageCount:i? durationMs:l? sha256"),
    PortableTable("media_markers", "id mediaId type startMs:l endMs:l? label note"),
    PortableTable("block_media_links", "blockId role mediaId"),
    PortableTable("block_audio_state", "blockId mediaId positionMs:l speedPercent:i aMs:l bMs:l repeat:b selectedMarkerId:s? role"),
    PortableTable("block_audio_markers", "blockId mediaId id startMs:l endMs:l? label note originalMaster:s? hidden:b role"),
    PortableTable("transcribe_activities", "id startedAt:l completedAt:l? activeMs:l goalId:s? audioId:s?")
)

internal fun portableTargetEmpty(sql: SupportSQLiteDatabase): Boolean = portableTables.all { table ->
    sql.query("SELECT 1 FROM ${table.name} LIMIT 1").use { !it.moveToFirst() }
}

internal fun insertPortableRecords(sql: SupportSQLiteDatabase, records: JSONObject) {
    for (table in portableTables) table.restore(sql, records.getJSONArray(table.name))
}

internal class PortableRecords(val payload: JSONObject) {
    val tables: JSONObject = payload.getJSONObject("tables")
    val preferences: JSONObject = payload.getJSONObject("preferences")
    private val rows = portableTables.associate { table ->
        val values = tables.getJSONArray(table.name)
        require(values.length() <= ZIP_RECORD_LIMIT) { "Too many ${table.name} records." }
        table.name to List(values.length()) { table.row(values.getJSONArray(it)) }
    }
    val counts = JSONObject().apply { rows.forEach { (name, values) -> put(name, values.size) } }
    operator fun get(table: String) = rows.getValue(table)

    init {
        require(payload.keys().asSequence().toSet() == setOf("tables", "preferences"))
        require(tables.keys().asSequence().toSet() == rows.keys) { "Unexpected backup record type." }
        val legacy = JSONObject().put("format", "practice.logger.backup").put("version", 7)
        portableTables.forEach { table -> table.legacyKey?.let { legacy.put(it, JSONArray(this[table.name])) } }
        val core = BackupCodec.decodeObject(legacy)
        var total = core.placements.fold(0L) { sum, row -> Math.addExact(sum, row.activeMs) }
        val assets = this["media_assets"].associateBy { it.getString("id") }
        val legacyBlocks = this["block_images"].mapTo(hashSetOf()) { it.getString("blockId") }
        require(this["block_media_links"].none { it.getString("role") == "VISUAL" && it.getString("blockId") in legacyBlocks }) {
            "A Block has conflicting visual references."
        }
        var unfinished = 0
        for (row in this["transcribe_activities"]) {
            val started = row.getLong("startedAt")
            val time = row.getLong("activeMs")
            require(started >= 0 && time >= 0)
            if (row.isNull("completedAt")) unfinished++ else require(row.getLong("completedAt") >= started)
            require(row.isNull("audioId") || assets[row.getString("audioId")]?.getString("category") == "AUDIO")
            total = Math.addExact(total, time)
        }
        require(unfinished <= 1) { "Multiple unfinished Transcribe activities." }

        val masterById = this["media_markers"].associateBy { it.getString("id") }
        fun duration(id: String): Long {
            val asset = requireNotNull(assets[id]) { "Missing audio reference." }
            require(asset.getString("category") == "AUDIO")
            return asset.getLong("durationMs")
        }
        fun marker(row: JSONObject, type: String) = MasterAudioMarker(
            row.getString("id"), row.getString("mediaId"), type, row.getLong("startMs"),
            if (row.isNull("endMs")) null else row.getLong("endMs"), row.getString("label"), row.getString("note"))
        for (row in this["media_markers"]) {
            val mark = marker(row, row.getString("type"))
            require(mark.id.isNotBlank())
            validateMasterMarker(mark, duration(mark.mediaId))
        }
        for (row in this["block_audio_markers"]) {
            require(row.getString("role") == "AUDIO")
            val mark = marker(row, if (row.isNull("endMs")) "POINT" else "SECTION")
            require(mark.id.isNotBlank())
            validateMasterMarker(mark, duration(mark.mediaId))
            require(if (row.isNull("originalMaster")) !row.getBoolean("hidden") else masterById[mark.id]?.getString("mediaId") == mark.mediaId)
        }
        for (row in this["block_audio_state"]) {
            require(row.getString("role") == "AUDIO")
            val length = duration(row.getString("mediaId"))
            val a = row.getLong("aMs")
            val b = row.getLong("bMs")
            require(row.getLong("positionMs") in 0..length && row.getInt("speedPercent") in 50..100 && a in 0..b && b <= length)
        }
    }

    /** Validate all PKs, unique indexes, FKs and role triggers on a separate empty database. */
    suspend fun validateRelationships(context: Context) {
        val scratch = Room.inMemoryDatabaseBuilder(context, BlockDatabase::class.java)
            .addCallback(portableConstraintCallback).build()
        try {
            scratch.withTransaction {
                val sql = scratch.openHelper.writableDatabase
                insertPortableRecords(sql, tables)
                sql.query("PRAGMA foreign_key_check").use { require(!it.moveToFirst()) { "Broken backup relationship." } }
            }
        } finally { scratch.close() }
    }
}

internal val portableConstraintCallback = object : androidx.room.RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        installBlockMediaConstraints(db)
        installBlockAudioConstraints(db)
    }
}
