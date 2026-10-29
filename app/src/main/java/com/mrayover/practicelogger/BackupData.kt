package com.mrayover.practicelogger

import androidx.room.withTransaction
import org.json.JSONArray
import org.json.JSONObject

const val BACKUP_LIMIT = 8 * 1024 * 1024
data class BackupData(val blocks: List<Block>, val sessions: List<PracticeSession>, val placements: List<Placement>, val routines: List<SavedRoutine> = emptyList(), val routinePlacements: List<RoutinePlacement> = emptyList(), val goals: List<Goal> = emptyList(), val goalBlocks: List<GoalBlock> = emptyList(), val sessionGoals: List<GoalSession> = emptyList(), val logs: List<DailyLog> = emptyList())

/** Format version is independent of Room's internal schema version. No running timer anchor is exported. */
object BackupCodec {
    private fun obj(vararg pairs: Pair<String, Any?>) = JSONObject().apply { pairs.forEach { (k, v) -> put(k, v ?: JSONObject.NULL) } }
    private fun array(rows: List<JSONObject>) = JSONArray().apply { rows.forEach { put(it) } }
    fun encode(data: BackupData): ByteArray {
        validate(data)
        val root = obj("format" to "practice.logger.backup", "version" to 6,
            "blocks" to array(data.blocks.map { b -> obj("id" to b.id, "name" to b.name, "action" to b.action, "durationMinutes" to b.durationMinutes, "tempo" to b.tempo, "notes" to b.notes, "archived" to b.archived, "createdAt" to b.createdAt, "updatedAt" to b.updatedAt) }),
            "sessions" to array(data.sessions.map { s -> obj("id" to s.id, "startedAt" to s.startedAt, "endedAt" to s.endedAt, "status" to s.status, "currentIndex" to s.currentIndex, "note" to s.note) }),
            "placements" to array(data.placements.map { p -> obj("id" to p.id, "sessionId" to p.sessionId, "sourceBlockId" to p.sourceBlockId, "name" to p.name, "action" to p.action, "tempo" to p.tempo, "referenceNotes" to p.referenceNotes, "position" to p.position, "configuredMs" to p.configuredMs, "activeMs" to p.activeMs, "outcome" to p.outcome, "overtimeAccepted" to p.overtimeAccepted, "note" to p.note, "lastUsedBpm" to p.lastUsedBpm) }),
            "routines" to array(data.routines.map { r -> obj("id" to r.id, "name" to r.name, "createdAt" to r.createdAt, "updatedAt" to r.updatedAt) }),
            "routinePlacements" to array(data.routinePlacements.map { p -> obj("id" to p.id, "routineId" to p.routineId, "sourceBlockId" to p.sourceBlockId, "position" to p.position, "configuredMs" to p.configuredMs) }),
            "goals" to array(data.goals.map { g -> obj("id" to g.id, "title" to g.title, "description" to g.description, "progress" to g.progress, "notes" to g.notes, "completed" to g.completed, "priority" to g.priority, "createdAt" to g.createdAt, "updatedAt" to g.updatedAt) }),
            "goalBlocks" to array(data.goalBlocks.map { b -> obj("goalId" to b.goalId, "blockId" to b.blockId, "position" to b.position) }),
            "logs" to array(data.logs.map { obj("date" to it.date, "text" to it.text) }),
            "sessionGoals" to array(data.sessionGoals.map { s -> obj("sessionId" to s.sessionId, "goalId" to s.goalId) }))
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
        require(root.text("format") == "practice.logger.backup" && root.int("version") in 1..6) { "Unsupported backup format/version." }
        return BackupData(
            root.rows("blocks").map { b -> Block(b.text("id"), b.text("name"), b.text("action"), b.int("durationMinutes"), b.nullInt("tempo"), b.text("notes"), b.bool("archived"), b.number("createdAt"), b.number("updatedAt")) },
            root.rows("sessions").map { s -> PracticeSession(s.text("id"), s.number("startedAt"), s.nullLong("endedAt"), s.text("status"), s.int("currentIndex"), s.text("note")) },
            root.rows("placements").map { p -> Placement(p.text("id"), p.text("sessionId"), p.text("sourceBlockId"), p.text("name"), p.text("action"), p.nullInt("tempo"), p.text("referenceNotes"), p.int("position"), p.number("configuredMs"), p.number("activeMs"), p.text("outcome"), p.bool("overtimeAccepted"), p.text("note"), if (root.int("version") >= 3) p.nullInt("lastUsedBpm") else null) },
            if (root.int("version") >= 4) root.rows("routines").map { r -> SavedRoutine(r.text("id"), r.text("name"), r.number("createdAt"), r.number("updatedAt")) } else emptyList(),
            if (root.int("version") >= 4) root.rows("routinePlacements").map { p -> RoutinePlacement(p.text("id"), p.text("routineId"), p.text("sourceBlockId"), p.int("position"), p.number("configuredMs")) } else emptyList(),
            if (root.int("version") >= 5) root.rows("goals").map { g -> Goal(g.text("id"), g.text("title"), g.text("description"), g.int("progress"), g.text("notes"), g.bool("completed"), g.int("priority"), g.number("createdAt"), g.number("updatedAt")) } else emptyList(),
            if (root.int("version") >= 5) root.rows("goalBlocks").map { b -> GoalBlock(b.text("goalId"), b.text("blockId"), b.int("position")) } else emptyList(),
            if (root.int("version") >= 5) root.rows("sessionGoals").map { s -> GoalSession(s.text("sessionId"), s.text("goalId")) } else emptyList(),
            if (root.int("version") >= 6) root.rows("logs").map { DailyLog(it.text("date"), it.text("text")) } else emptyList()
        ).also {
            validate(it)
            if (root.int("version") == 1) require(it.sessions.count { s -> s.status != "COMPLETED" } <= 1) { "Invalid legacy unfinished sessions." }
        }
    }
    fun validate(data: BackupData) {
        fun ids(values: List<String>) { require(values.size <= 20_000 && values.all { it.isNotBlank() } && values.toSet().size == values.size) { "Missing or duplicate IDs." } }
        ids(data.logs.map { it.date }); data.logs.forEach { logDate(it.date) }
        ids(data.blocks.map { it.id }); ids(data.sessions.map { it.id }); ids(data.placements.map { it.id })
        ids(data.routines.map { it.id }); ids(data.routinePlacements.map { it.id })
        ids(data.goals.map { it.id }); ids(data.sessionGoals.map { it.sessionId })
        require(data.goalBlocks.size <= 20_000 && data.goalBlocks.map { it.goalId to it.blockId }.toSet().size == data.goalBlocks.size) { "Duplicate Goal Block links." }
        val goalIds = data.goals.map { it.id }.toSet()
        val active = data.goals.filter { !it.completed }.sortedBy { it.priority }
        require(active.size <= 5 && active.map { it.priority } == active.indices.toList()) { "Invalid active Goal limit/order." }
        val goalRows = data.goalBlocks.groupBy { it.goalId }
        val goalSessions = data.sessions.associateBy { it.id }
        val goalBlockIds = data.blocks.map { it.id }.toSet()
        data.goals.forEach { g ->
            require(g.title.isNotBlank() && g.progress in 0..100 && g.createdAt >= 0 && g.updatedAt >= g.createdAt && (!g.completed || g.priority == -1)) { "Invalid Goal." }
            val rows = goalRows[g.id].orEmpty().sortedBy { it.position }
            require(rows.map { it.position } == rows.indices.toList()) { "Invalid Goal Block order." }
        }
        data.goalBlocks.forEach { require(it.goalId in goalIds && it.blockId in goalBlockIds) { "Broken Goal Block reference." } }
        data.sessionGoals.forEach { require(it.goalId in goalIds && goalSessions[it.sessionId]?.status in setOf("IN_PROGRESS", "COMPLETED")) { "Broken Goal session reference." } }
        val timeBySession = data.placements.groupBy { it.sessionId }.mapValues { (_, rows) -> rows.fold(0L) { sum, p -> Math.addExact(sum, p.activeMs) } }
        data.sessionGoals.groupBy { it.goalId }.values.forEach { links -> links.fold(0L) { sum, link -> Math.addExact(sum, timeBySession[link.sessionId] ?: 0L) } }
        val routineIds = data.routines.map { it.id }.toSet()
        val blockIds = data.blocks.map { it.id }.toSet()
        val routineRows = data.routinePlacements.groupBy { it.routineId }
        data.routinePlacements.forEach { require(it.routineId in routineIds && it.sourceBlockId in blockIds && it.configuredMs > 0) { "Broken Routine placement reference or duration." } }
        data.routines.forEach { routine ->
            require(routineNameValid(routine.name) && routine.createdAt >= 0 && routine.updatedAt >= routine.createdAt) { "Invalid Routine." }
            val rows = routineRows[routine.id].orEmpty().sortedBy { it.position }
            require(rows.isNotEmpty() && rows.map { it.position } == rows.indices.toList()) { "Invalid Routine placement order." }
        }
        val blocks = data.blocks.map { it.id }.toSet(); val sessions = data.sessions.map { it.id }.toSet()
        require(data.sessions.count { it.status == "DRAFT" } <= 1) { "Multiple draft sessions." }
        data.blocks.forEach { require(it.name.isNotBlank() && it.action.isNotBlank() && it.durationMinutes > 0 && (it.tempo == null || it.tempo > 0)) }
        data.placements.forEach { p ->
            require(p.sessionId in sessions && p.sourceBlockId in blocks && p.name.isNotBlank() && p.action.isNotBlank()) { "Broken placement reference." }
            require(p.configuredMs > 0 && p.activeMs >= 0 && (p.tempo == null || p.tempo > 0))
            require(p.lastUsedBpm == null || p.lastUsedBpm in 30..300)
            require(p.outcome in setOf("PENDING", "COMPLETED", "SKIPPED"))
            require(if (p.overtimeAccepted) p.activeMs >= p.configuredMs else p.activeMs <= p.configuredMs)
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
        val routines = db.routines().readAll()
        val goals = db.goals().readAll()
        BackupData(db.blocks().readAll(), bundles.map { it.session }, bundles.flatMap { it.ordered }, routines.map { it.routine }, routines.flatMap { it.ordered }, goals.map { it.goal }, goals.flatMap { it.ordered }, db.goals().readSessions(), db.logs().readAll())
    }
    suspend fun restore(data: BackupData) = db.withTransaction {
        BackupCodec.validate(data)
        if (db.logs().hasEntries() || db.goals().readAll().isNotEmpty() || db.blocks().readAll().isNotEmpty() || db.sessions().readAll().isNotEmpty() || db.routines().readAll().isNotEmpty() || db.takes().readAll().isNotEmpty()) throw NonEmptyRestoreException()
        data.blocks.forEach { db.blocks().insert(it) }
        data.sessions.forEach { db.sessions().insert(it) }
        data.placements.forEach { db.sessions().insert(it) }
        data.routines.forEach { db.routines().insert(it) }
        data.routinePlacements.forEach { db.routines().insert(it) }
        data.goals.forEach { db.goals().insert(it) }
        data.goalBlocks.forEach { db.goals().insert(it) }
        data.sessionGoals.forEach { db.goals().insert(it) }
        data.logs.forEach { db.logs().insert(it) }
    }
}
