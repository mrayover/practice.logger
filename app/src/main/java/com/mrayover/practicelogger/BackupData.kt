package com.mrayover.practicelogger

import androidx.room.withTransaction
import org.json.JSONArray
import org.json.JSONObject

const val BACKUP_LIMIT = 8 * 1024 * 1024
data class BackupData(val blocks: List<Block>, val sessions: List<PracticeSession>, val placements: List<Placement>)

/** Format version is independent of Room's internal schema version. No running timer anchor is exported. */
object BackupCodec {
    private fun obj(vararg pairs: Pair<String, Any?>) = JSONObject().apply { pairs.forEach { (k, v) -> put(k, v ?: JSONObject.NULL) } }
    private fun array(rows: List<JSONObject>) = JSONArray().apply { rows.forEach { put(it) } }
    fun encode(data: BackupData): ByteArray {
        validate(data)
        val root = obj("format" to "practice.logger.backup", "version" to 1,
            "blocks" to array(data.blocks.map { b -> obj("id" to b.id, "name" to b.name, "action" to b.action, "durationMinutes" to b.durationMinutes, "tempo" to b.tempo, "notes" to b.notes, "archived" to b.archived, "createdAt" to b.createdAt, "updatedAt" to b.updatedAt) }),
            "sessions" to array(data.sessions.map { s -> obj("id" to s.id, "startedAt" to s.startedAt, "endedAt" to s.endedAt, "status" to s.status, "currentIndex" to s.currentIndex, "note" to s.note) }),
            "placements" to array(data.placements.map { p -> obj("id" to p.id, "sessionId" to p.sessionId, "sourceBlockId" to p.sourceBlockId, "name" to p.name, "action" to p.action, "tempo" to p.tempo, "referenceNotes" to p.referenceNotes, "position" to p.position, "configuredMs" to p.configuredMs, "activeMs" to p.activeMs, "outcome" to p.outcome, "overtimeAccepted" to p.overtimeAccepted, "note" to p.note) }))
        return root.toString(2).toByteArray(Charsets.UTF_8).also { require(it.size <= BACKUP_LIMIT) { "Backup exceeds the 8 MiB limit." } }
    }
    private fun JSONObject.text(key: String): String = (get(key) as? String) ?: error("Invalid text: $key")
    private fun JSONObject.number(key: String): Long = when (val value = get(key)) {
        is Int -> value.toLong(); is Long -> value; else -> error("Invalid whole number: $key")
    }
    private fun JSONObject.int(key: String): Int = number(key).also { require(it in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()) }.toInt()
    private fun JSONObject.bool(key: String): Boolean = (get(key) as? Boolean) ?: error("Invalid boolean: $key")
    private fun JSONObject.nullInt(key: String): Int? = if (get(key) == JSONObject.NULL) null else int(key)
    private fun JSONObject.nullLong(key: String): Long? = if (get(key) == JSONObject.NULL) null else number(key)
    private fun JSONObject.rows(key: String): List<JSONObject> = getJSONArray(key).let { rows ->
        require(rows.length() <= 20_000) { "Too many records." }; (0 until rows.length()).map { rows.getJSONObject(it) }
    }
    fun decode(bytes: ByteArray): BackupData {
        require(bytes.size <= BACKUP_LIMIT) { "Backup exceeds the 8 MiB limit." }
        val text = Charsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(bytes)).toString()
        // Bound nesting before invoking the recursive Android JSON parser.
        var depth = 0; var quoted = false; var escaped = false
        text.forEach { ch ->
            if (quoted) {
                if (escaped) escaped = false else if (ch == '\\') escaped = true else if (ch == '"') quoted = false
            } else when (ch) {
                '"' -> quoted = true
                '{', '[' -> { depth++; require(depth <= 32) { "Backup nesting is too deep." } }
                '}', ']' -> { depth--; require(depth >= 0) }
            }
        }
        require(!quoted && depth == 0)
        val tokener = org.json.JSONTokener(text)
        val root = tokener.nextValue() as? JSONObject ?: error("Invalid backup object.")
        require(tokener.nextClean() == '\u0000') { "Trailing backup content." }
        require(root.text("format") == "practice.logger.backup" && root.int("version") == 1) { "Unsupported backup format/version." }
        return BackupData(
            root.rows("blocks").map { b -> Block(b.text("id"), b.text("name"), b.text("action"), b.int("durationMinutes"), b.nullInt("tempo"), b.text("notes"), b.bool("archived"), b.number("createdAt"), b.number("updatedAt")) },
            root.rows("sessions").map { s -> PracticeSession(s.text("id"), s.number("startedAt"), s.nullLong("endedAt"), s.text("status"), s.int("currentIndex"), s.text("note")) },
            root.rows("placements").map { p -> Placement(p.text("id"), p.text("sessionId"), p.text("sourceBlockId"), p.text("name"), p.text("action"), p.nullInt("tempo"), p.text("referenceNotes"), p.int("position"), p.number("configuredMs"), p.number("activeMs"), p.text("outcome"), p.bool("overtimeAccepted"), p.text("note")) }
        ).also(::validate)
    }
    fun validate(data: BackupData) {
        fun ids(values: List<String>) { require(values.size <= 20_000 && values.all { it.isNotBlank() } && values.toSet().size == values.size) { "Missing or duplicate IDs." } }
        ids(data.blocks.map { it.id }); ids(data.sessions.map { it.id }); ids(data.placements.map { it.id })
        val blocks = data.blocks.map { it.id }.toSet(); val sessions = data.sessions.map { it.id }.toSet()
        require(data.sessions.count { it.status != "COMPLETED" } <= 1) { "Multiple unfinished sessions." }
        data.blocks.forEach { require(it.name.isNotBlank() && it.action.isNotBlank() && it.durationMinutes > 0 && (it.tempo == null || it.tempo > 0)) }
        data.placements.forEach { p ->
            require(p.sessionId in sessions && p.sourceBlockId in blocks && p.name.isNotBlank() && p.action.isNotBlank()) { "Broken placement reference." }
            require(p.configuredMs > 0 && p.activeMs >= 0 && (p.tempo == null || p.tempo > 0))
            require(p.outcome in setOf("PENDING", "COMPLETED", "SKIPPED"))
            require(p.overtimeAccepted || p.activeMs <= p.configuredMs)
        }
        val bySession = data.placements.groupBy { it.sessionId }
        data.sessions.forEach { s ->
            require(s.status in setOf("DRAFT", "IN_PROGRESS", "COMPLETED"))
            val ordered = bySession[s.id].orEmpty().sortedBy { it.position }
            require(ordered.map { it.position } == ordered.indices.toList()) { "Invalid placement order." }
            // Prevent overflow in totals rendered by Home and Summary.
            ordered.fold(0L) { total, p -> Math.addExact(total, p.activeMs) }
            when (s.status) {
                "DRAFT" -> require(s.endedAt == null && s.currentIndex == 0 && ordered.all { it.outcome == "PENDING" && it.activeMs == 0L && !it.overtimeAccepted })
                "IN_PROGRESS" -> require(s.endedAt == null && s.currentIndex in ordered.indices && ordered.withIndex().all { (i, p) ->
                    if (i < s.currentIndex) p.outcome != "PENDING" else p.outcome == "PENDING" && (i == s.currentIndex || (p.activeMs == 0L && !p.overtimeAccepted))
                })
                "COMPLETED" -> require(s.endedAt != null && ordered.isNotEmpty() && s.currentIndex == ordered.lastIndex && ordered.all { it.outcome != "PENDING" })
            }
        }
    }
}

class NonEmptyRestoreException : IllegalStateException("Restore needs an empty app. Existing data was kept.")

class BackupRepository(private val db: BlockDatabase) {
    suspend fun snapshot(): BackupData = db.withTransaction {
        val bundles = db.sessions().readAll()
        BackupData(db.blocks().readAll(), bundles.map { it.session }, bundles.flatMap { it.ordered })
    }
    suspend fun restore(data: BackupData) = db.withTransaction {
        BackupCodec.validate(data)
        if (db.blocks().readAll().isNotEmpty() || db.sessions().readAll().isNotEmpty()) throw NonEmptyRestoreException()
        data.blocks.forEach { db.blocks().insert(it) }
        data.sessions.forEach { db.sessions().insert(it) }
        data.placements.forEach { db.sessions().insert(it) }
    }
}
