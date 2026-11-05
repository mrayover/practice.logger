package com.mrayover.practicelogger

import android.content.Context
import android.database.Cursor
import androidx.room.Room
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import org.json.JSONArray
import org.json.JSONObject

internal const val ZIP_RECORD_LIMIT = 20_000

/** Wire columns are frozen independently of future Room migrations. */
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
            "f" -> cursor.getDouble(index)
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
                    "f" -> value is Number && value.toDouble().isFinite()
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
                        is Number -> if (types[j] == "f") statement.bindDouble(j + 1, value.toFloat().toDouble())
                            else statement.bindLong(j + 1, value.toLong())
                        else -> error("Invalid record cell.")
                    }
                }
                statement.executeInsert()
            }
        }
    }
}

private val portableTablesV1 = listOf(
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

private const val OVERLAY_TABLE = "practice_overlays"
internal const val PORTABLE_VERSION = 2
internal val portableTables = portableTablesV1 + PortableTable(OVERLAY_TABLE,
    "id blockId reference page:i type left:f top:f right:f bottom:f appearance:i drawOrder:i")

internal fun portableTargetEmpty(sql: SupportSQLiteDatabase): Boolean = portableTables.all { table ->
    sql.query("SELECT 1 FROM ${table.name} LIMIT 1").use { !it.moveToFirst() }
}

internal class PortableRecords(val payload: JSONObject, version: Int) {
    private val formatTables = if (version == 1) portableTablesV1 else portableTables
    val tables: JSONObject = payload.getJSONObject("tables")
    val preferences: JSONObject = payload.getJSONObject("preferences")
    private val rows = formatTables.associate { table ->
        val values = tables.getJSONArray(table.name)
        val limit = if (table.name == OVERLAY_TABLE) OVERLAYS_TOTAL else ZIP_RECORD_LIMIT
        require(values.length() <= limit) { "Too many ${table.name} records." }
        table.name to List(values.length()) { table.row(values.getJSONArray(it)) }
    }
    val counts = JSONObject().apply { rows.forEach { (name, values) -> put(name, values.size) } }
    operator fun get(table: String): List<JSONObject> =
        if (table == OVERLAY_TABLE) rows[table].orEmpty() else rows.getValue(table)
    private val maxOverlayPageByReference: Map<String, Int>

    init {
        val base = setOf("tables", "preferences")
        when (version) {
            1 -> require(payload.keys().asSequence().toSet() == base)
            PORTABLE_VERSION -> {
                require(payload.keys().asSequence().toSet() == base + "version")
                val stored = payload.get("version")
                require(stored == PORTABLE_VERSION || stored == PORTABLE_VERSION.toLong()) { "Unsupported backup records version." }
            }
            else -> error("Unsupported portable backup version.")
        }
        require(tables.keys().asSequence().toSet() == rows.keys) { "Unexpected backup record type." }
        val overlays = this[OVERLAY_TABLE]
        val ids = hashSetOf<String>()
        for (row in overlays) {
            val left = row.getDouble("left")
            val top = row.getDouble("top")
            val right = row.getDouble("right")
            val bottom = row.getDouble("bottom")
            require(validOverlayBounds(left, top, right, bottom)) { "Invalid overlay rectangle." }
            val shape = PracticeOverlay(row.getString("id"), row.getString("blockId"),
                row.getString("reference"), row.getInt("page"), row.getString("type"),
                left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat(),
                row.getInt("appearance"), row.getInt("drawOrder"))
            validateOverlay(shape, 100)
            require(ids.add(shape.id)) { "Duplicate overlay identity." }
        }
        require(overlays.groupingBy { it.getString("blockId") }.eachCount().values.all { it <= OVERLAYS_PER_BLOCK }) {
            "A Block has too many saved markings."
        }
        require(overlays.groupingBy { it.getString("blockId") to it.getInt("page") }.eachCount()
            .values.all { it <= OVERLAYS_PER_PAGE }) { "A page has too many saved markings." }
        maxOverlayPageByReference = overlays.groupingBy { it.getString("reference") }
            .fold(-1) { previous, row -> maxOf(previous, row.getInt("page")) }
        val legacy = JSONObject().put("format", "practice.logger.backup").put("version", 7)
        formatTables.forEach { table -> table.legacyKey?.let { legacy.put(it, JSONArray(this[table.name])) } }
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

    fun validateOverlayPages(original: PortableOriginal, pages: Int?) {
        val identity = original.overlayIdentity ?: return
        val lastPage = maxOverlayPageByReference[identity] ?: return
        require(pages != null && lastPage < pages) { "A marking refers to a page missing from its original." }
    }

    fun insertInto(sql: SupportSQLiteDatabase) {
        for (table in formatTables) table.restore(sql, tables.getJSONArray(table.name))
    }

    /** Validate all PKs, unique indexes, FKs and role triggers on a separate empty database. */
    suspend fun validateRelationships(context: Context) {
        val scratch = Room.inMemoryDatabaseBuilder(context, BlockDatabase::class.java)
            .addCallback(mediaConstraintCallback).build()
        try {
            scratch.withTransaction {
                val sql = scratch.openHelper.writableDatabase
                insertInto(sql)
                sql.query("PRAGMA foreign_key_check").use { require(!it.moveToFirst()) { "Broken backup relationship." } }
            }
        } finally { scratch.close() }
    }
}

internal val mediaConstraintCallback = object : androidx.room.RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        installBlockMediaConstraints(db)
        installBlockAudioConstraints(db)
        installPracticeOverlayConstraints(db)
    }
}
