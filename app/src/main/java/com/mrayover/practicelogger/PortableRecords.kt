package com.mrayover.practicelogger

import android.content.Context
import android.database.Cursor
import androidx.room.Room
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

internal const val ZIP_RECORD_LIMIT = 20_000

/** Wire columns are frozen independently of future Room migrations. */
internal class PortableTable(val name: String, fields: String, val legacyKey: String? = null) {
    val columns = fields.split(' ').map { it.substringBefore(':') }
    private val types = fields.split(' ').map { it.substringAfter(':', "s") }
    private val projection = columns.joinToString { "`$it`" }
    private val select = "SELECT $projection FROM `$name` ORDER BY " +
        if (name == "block_media_links" && "position" in columns) "blockId,role,position,mediaId" else projection
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
                        is Number -> if (types[j] == "f") statement.bindDouble(j + 1, value.toDouble())
                            else statement.bindLong(j + 1, value.toLong())
                        else -> error("Invalid record cell.")
                    }
                }
                statement.executeInsert()
            }
        }
    }
}

private const val PLACEMENT_FIELDS = "id sessionId sourceBlockId name action tempo:i? referenceNotes position:i configuredMs:l activeMs:l outcome overtimeAccepted:b note lastUsedBpm:i?"

private val portableTablesV1 = listOf(
    PortableTable("blocks", "id name action durationMinutes:i tempo:i? notes archived:b createdAt:l updatedAt:l", "blocks"),
    PortableTable("sessions", "id startedAt:l endedAt:l? status currentIndex:i note", "sessions"),
    PortableTable("placements", PLACEMENT_FIELDS, "placements"),
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
private const val PRIMARY_TABLE = "block_primary_goals"
private const val SNAPSHOT_TABLE = "placement_goal_snapshots"
private const val WEEK_TABLE = "week_blocks"
private const val COMPLETION_TABLE = "block_completions"
private const val BLOCK_REFLECTION_TABLE = "block_reflections"
private const val SESSION_REFLECTION_TABLE = "session_reflections"
internal const val PORTABLE_VERSION = 9
private val portableTablesV2 = portableTablesV1 + PortableTable(OVERLAY_TABLE,
    "id blockId reference page:i type left:f top:f right:f bottom:f appearance:i drawOrder:i")
private val portableTablesV3 = portableTablesV2 + listOf(
    PortableTable(PRIMARY_TABLE, "blockId goalId"),
    PortableTable(SNAPSHOT_TABLE, "placementId goalId:s? priority:i?")
)

private val portableTablesV4 = portableTablesV3 + PortableTable(WEEK_TABLE, "weekStart blockId position:i")

private val portableTablesV5 = portableTablesV4.map { table ->
    if (table.name == "placements") PortableTable("placements", "$PLACEMENT_FIELDS prepMs:l", "placements") else table
}

private val portableTablesV6 = portableTablesV5 + listOf(
    PortableTable(COMPLETION_TABLE, "placementId sessionId sourceBlockId completedAt:l goalId:s? priority:i? colorArgb:i?"),
    PortableTable(BLOCK_REFLECTION_TABLE, "placementId security:i? focus difficulty progress revisit"),
    PortableTable(SESSION_REFLECTION_TABLE, "sessionId experience focus progress difficulty energy")
)

private val portableTablesV7 = portableTablesV6 + PortableTable(INSPIRATION_STORE,
    "id title author source category note fileName:s? mimeType:s? byteCount:l? sha256:s? mediaId:s? pageCount:i?")

private val portableTablesV8 = portableTablesV7.map { table -> when (table.name) {
    "block_media_links" -> PortableTable("block_media_links", "blockId role mediaId position:i")
    "block_images" -> PortableTable("block_images", "blockId fileName mimeType width:i height:i byteCount:l position:i")
    else -> table
} }

internal val portableTables = portableTablesV8 + listOf(
    PortableTable("learning_observations", "placementId outcome observedAt:l context criterionToken:s? criterion:s? anchorId:s? correctedAt:l?"),
    PortableTable("guidance_events", "id blockId revision:l recordedAt:l action returnSoon:b dueDate:s? baseDate:s? intervalDays:i? origin offeredSuggestion:s?")
)

internal fun portableTargetEmpty(sql: SupportSQLiteDatabase): Boolean = portableTables.all { table ->
    sql.query("SELECT 1 FROM ${table.name} LIMIT 1").use { !it.moveToFirst() }
}

internal class PortableRecords(val payload: JSONObject, private val version: Int) {
    private val formatTables = when (version) {
        1 -> portableTablesV1
        2 -> portableTablesV2
        3 -> portableTablesV3
        4 -> portableTablesV4
        5 -> portableTablesV5
        6 -> portableTablesV6
        7 -> portableTablesV7
        8 -> portableTablesV8
        PORTABLE_VERSION -> portableTables
        else -> error("Unsupported portable backup version.")
    }
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
        when (table) {
            OVERLAY_TABLE, PRIMARY_TABLE, SNAPSHOT_TABLE, WEEK_TABLE,
            COMPLETION_TABLE, BLOCK_REFLECTION_TABLE, SESSION_REFLECTION_TABLE, INSPIRATION_STORE,
            "learning_observations", "guidance_events" -> rows[table].orEmpty()
            else -> rows.getValue(table)
        }
    private val learningOrder: List<String>
    private val maxOverlayPageByReference: Map<String, Int>

    init {
        val base = setOf("tables", "preferences")
        when (version) {
            1 -> require(payload.keys().asSequence().toSet() == base)
            else -> {
                require(payload.keys().asSequence().toSet() == base + "version")
                val stored = payload.get("version")
                require(stored == version || stored == version.toLong()) { "Unsupported backup records version." }
            }
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
        require(overlays.groupingBy { Triple(it.getString("blockId"), it.getString("reference"), it.getInt("page")) }.eachCount()
            .values.all { it <= OVERLAYS_PER_PAGE }) { "A page has too many saved markings." }
        maxOverlayPageByReference = overlays.groupingBy { it.getString("reference") }
            .fold(-1) { previous, row -> maxOf(previous, row.getInt("page")) }
        val legacy = JSONObject().put("format", "practice.logger.backup").put("version", 7)
        formatTables.forEach { table -> table.legacyKey?.let { legacy.put(it, JSONArray(this[table.name])) } }
        val core = BackupCodec.decodeObject(legacy)
        if (version >= 5) {
            val sessions = core.sessions.associateBy { it.id }
            for (row in this["placements"]) {
                val session = sessions.getValue(row.getString("sessionId"))
                val prepMs = row.getLong("prepMs")
                require(prepMs >= 0 && (prepMs == 0L ||
                    session.status != "DRAFT" && row.getInt("position") <= session.currentIndex)) {
                    "Invalid placement preparation time."
                }
            }
        }
        val blockIds = core.blocks.mapTo(hashSetOf()) { it.id }
        for ((week, entries) in this[WEEK_TABLE].groupBy { it.getString("weekStart") }) {
            val date = LocalDate.parse(week)
            require(date.toString() == week && practiceWeek(date) == date) { "Invalid weekly plan start date." }
            val members = hashSetOf<String>()
            require(entries.all { row -> row.getString("blockId").let { it in blockIds && members.add(it) } } &&
                entries.map { it.getInt("position") }.sorted() == entries.indices.toList()) {
                "Invalid weekly plan membership or order."
            }
        }
        val placements = core.placements.associateBy { it.id }
        val goalIds = core.goals.mapTo(hashSetOf()) { it.id }
        fun optionalText(row: JSONObject, key: String) = if (row.isNull(key)) null else row.getString(key)
        val observations = this["learning_observations"].map { row ->
            LearningObservation(row.getString("placementId"), row.getString("outcome"), row.getLong("observedAt"),
                row.getString("context"), optionalText(row,"criterionToken"), optionalText(row,"criterion"),
                optionalText(row,"anchorId"), if (row.isNull("correctedAt")) null else row.getLong("correctedAt"))
        }
        validateLearningHistory(observations, core.placements, core.sessions)
        learningOrder = orderedLearningIds(observations)
        val guidance = this["guidance_events"].map { row ->
            GuidanceEvent(row.getString("id"), row.getString("blockId"), row.getLong("revision"), row.getLong("recordedAt"),
                row.getString("action"), row.getBoolean("returnSoon"), optionalText(row,"dueDate"), optionalText(row,"baseDate"),
                if (row.isNull("intervalDays")) null else row.getInt("intervalDays"), row.getString("origin"), optionalText(row,"offeredSuggestion"))
        }
        require(guidance.all { it.blockId in blockIds }) { "Guidance refers to a missing Block." }
        validateGuidanceHistory(guidance)
        val completions = hashSetOf<String>()
        for (row in this[COMPLETION_TABLE]) {
            val placementId = row.getString("placementId")
            val placement = placements[placementId]
            require(completions.add(placementId) && placement?.outcome == "COMPLETED" &&
                placement.sessionId == row.getString("sessionId") &&
                placement.sourceBlockId == row.getString("sourceBlockId") &&
                row.getLong("completedAt") >= 0L) { "Invalid Block completion." }
            val priority = if (row.isNull("priority")) null else row.getInt("priority")
            require(row.isNull("goalId") || row.getString("goalId").isNotBlank()) { "Invalid completion Goal." }
            require(if (priority == null) row.isNull("colorArgb") else
                priority in 1..5 && !row.isNull("goalId") && !row.isNull("colorArgb")) {
                "Invalid completion Goal priority or color."
            }
        }
        fun answers(row: JSONObject, vararg fields: String) = fields.map { row.getString(it) }.also {
            require(it.all { answer -> answer.length <= REFLECTION_ANSWER_LIMIT }) { "Reflection answer is too long." }
        }
        val blockReviews = hashSetOf<String>()
        for (row in this[BLOCK_REFLECTION_TABLE]) {
            val placementId = row.getString("placementId")
            val security = if (row.isNull("security")) null else row.getInt("security")
            val prompts = answers(row, "focus", "difficulty", "progress", "revisit")
            require(placementId in completions && blockReviews.add(placementId) &&
                (security == null || security in 1..5) &&
                (security != null || prompts.any { it.isNotBlank() })) { "Invalid Block reflection." }
        }
        val completedSessions = core.sessions.filter { it.status == "COMPLETED" }.mapTo(hashSetOf()) { it.id }
        val sessionReviews = hashSetOf<String>()
        for (row in this[SESSION_REFLECTION_TABLE]) {
            val sessionId = row.getString("sessionId")
            require(sessionId in completedSessions && sessionReviews.add(sessionId) &&
                answers(row, "experience", "focus", "progress", "difficulty", "energy").any { it.isNotBlank() }) {
                "Invalid session reflection."
            }
        }
        val links = core.goalBlocks.mapTo(hashSetOf()) { it.blockId to it.goalId }
        val primaryBlocks = hashSetOf<String>()
        for (row in this[PRIMARY_TABLE]) {
            val blockId = row.getString("blockId")
            require(primaryBlocks.add(blockId) && (blockId to row.getString("goalId")) in links) {
                "Invalid primary Goal association."
            }
        }
        val snapshots = hashSetOf<String>()
        for (row in this[SNAPSHOT_TABLE]) {
            val placementId = row.getString("placementId")
            require(snapshots.add(placementId) && (placements[placementId]?.activeMs ?: 0L) > 0L) {
                "A Goal snapshot requires a unique placement with committed practice time."
            }
            val goalId = if (row.isNull("goalId")) null else row.getString("goalId")
            require(goalId == null || goalId in goalIds) { "A Goal snapshot refers to a missing Goal." }
            require(row.isNull("priority") || goalId != null && row.getInt("priority") in 1..5) {
                "Invalid historical Goal priority."
            }
        }
        var total = core.placements.fold(0L) { sum, row -> Math.addExact(sum, row.activeMs) }
        val assets = this["media_assets"].associateBy { it.getString("id") }
        for (row in this[INSPIRATION_STORE]) {
            val id = row.getString("id")
            require(java.util.UUID.fromString(id).toString() == id)
            require(MediaLibraryRepository.title(row.getString("title")) == row.getString("title"))
            require(listOf("author", "source", "category", "note").all { row.getString(it).length <= INSPIRATION_METADATA_LIMIT })
            if (!row.isNull("mediaId")) {
                val asset = requireNotNull(assets[row.getString("mediaId")]) { "Missing Inspiration Library original." }
                require(asset.getString("category") in setOf("IMAGE", "PDF") &&
                    asset.getString("mimeType") == row.getString("mimeType") && asset.get("pageCount") == row.get("pageCount"))
                require(listOf("fileName", "byteCount", "sha256").all(row::isNull))
            } else if (row.isNull("fileName")) {
                require(listOf("mimeType", "byteCount", "sha256", "pageCount").all(row::isNull))
            } else {
                require(!row.isNull("byteCount") && !row.isNull("sha256") && !row.isNull("mimeType"))
            }
        }
        val legacyBlocks = this["block_images"].mapTo(hashSetOf()) { it.getString("blockId") }
        require(version >= 8 || this["block_media_links"].none { it.getString("role") == "VISUAL" && it.getString("blockId") in legacyBlocks }) {
            "A Block has conflicting visual references."
        }
        val mediaLinks = this["block_media_links"]
        val triples = mediaLinks.map { Triple(it.getString("blockId"), it.getString("role"), it.getString("mediaId")) }
        require(triples.toSet().size == triples.size) { "Duplicate reference association." }
        if (version < 8) {
            require(mediaLinks.map { it.getString("blockId") to it.getString("role") }.toSet().size == mediaLinks.size) {
                "Historical backups permit only one reference per role."
            }
        } else {
            val ordered = mediaLinks.map { Triple(it.getString("blockId"), it.getString("role"), it.getInt("position")) } +
                this["block_images"].map { Triple(it.getString("blockId"), "VISUAL", it.getInt("position")) }
            require(ordered.groupBy { it.first to it.second }.values.all { group ->
                group.map { it.third }.sorted() == group.indices.toList()
            }) { "Reference positions must be unique, nonnegative and dense." }
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
        for (table in formatTables) {
            if (table.name != "learning_observations") table.restore(sql, tables.getJSONArray(table.name))
            else {
                // Immediate restrictive self-FKs require anchors before their dependants.
                val values = tables.getJSONArray(table.name)
                val rowsById = (0 until values.length()).map { values.getJSONArray(it) }.associateBy { it.getString(0) }
                learningOrder.forEach { table.restore(sql, JSONArray().put(rowsById.getValue(it))) }
            }
        }
        // Historical single references have a single dense position supplied by the Room24 default.
        // Their old uniqueness and conflicting-visual rules were validated before this insertion.
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
        installReferencePositions(db)
    }
}
