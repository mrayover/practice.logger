package com.mrayover.practicelogger

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

internal val multiReferenceOldTriggers = setOf("block_media_type_insert", "block_media_type_update",
    "linked_media_type_update", "retire_block_master", "practice_overlay_insert", "practice_overlay_update",
    "practice_overlay_library_delete", "practice_overlay_library_update", "practice_overlay_legacy_delete",
    "practice_overlay_legacy_update", "practice_overlay_legacy_replacement")

internal fun installReferencePositions(db: SupportSQLiteDatabase) {
    for (table in listOf("block_media_links", "block_images")) for (event in listOf("INSERT", "UPDATE")) {
        db.execSQL("""CREATE TRIGGER IF NOT EXISTS ${table}_position_${event.lowercase()}
            BEFORE $event ON $table WHEN typeof(NEW.position) != 'integer' OR NEW.position < 0 OR NEW.position > 2147483647
            BEGIN SELECT RAISE(ABORT, 'Invalid reference position'); END""")
    }
    for (table in listOf("block_audio_state", "block_audio_markers")) for (event in listOf("INSERT", "UPDATE")) {
        db.execSQL("""CREATE TRIGGER IF NOT EXISTS ${table}_role_${event.lowercase()}
            BEFORE $event ON $table WHEN NEW.role != 'AUDIO'
            BEGIN SELECT RAISE(ABORT, 'Invalid local audio role'); END""")
    }
}

/** Room owns the upgrade transaction. Children are copied and dropped BEFORE their CASCADE parent. */
val MULTI_REFERENCE_MIGRATION_23_24 = object : Migration(23, 24) {
    override fun migrate(db: SupportSQLiteDatabase) {
        fun empty(sql: String) = db.query(sql).use { check(!it.moveToFirst()) { "Invalid existing reference relationships" } }
        fun count(table: String) = db.query("SELECT count(*) FROM `$table`").use { it.moveToFirst(); it.getLong(0) }
        empty("PRAGMA foreign_key_check")
        empty("SELECT 1 FROM block_audio_state WHERE role!='AUDIO' UNION ALL SELECT 1 FROM block_audio_markers WHERE role!='AUDIO'")
        empty("""SELECT 1 FROM block_media_links l JOIN media_assets a ON a.id=l.mediaId WHERE NOT
            ((l.role='VISUAL' AND a.category IN ('IMAGE','PDF')) OR (l.role='AUDIO' AND a.category='AUDIO'))""")
        empty("""SELECT 1 FROM practice_overlays o WHERE NOT (
            EXISTS(SELECT 1 FROM block_media_links l JOIN media_assets a ON a.id=l.mediaId WHERE l.blockId=o.blockId
                AND l.role='VISUAL' AND o.reference='library:'||a.id AND
                ((a.category='IMAGE' AND o.page=0) OR (a.category='PDF' AND o.page>=0 AND o.page<a.pageCount))) OR
            EXISTS(SELECT 1 FROM block_images i WHERE i.blockId=o.blockId AND o.reference='legacy:'||i.fileName
                AND o.page>=0 AND o.page<100 AND (i.mimeType='application/pdf' OR o.page=0)))""")
        db.query("SELECT name, sql FROM sqlite_master WHERE type='trigger'").use { c ->
            while (c.moveToNext()) if (listOf("block_media_links", "block_audio_state", "block_audio_markers")
                .any { Regex("\\b$it\\b", RegexOption.IGNORE_CASE).containsMatchIn(c.getString(1)) }) {
                check(c.getString(0) in multiReferenceOldTriggers) { "Unexpected trigger ${c.getString(0)}" }
            }
        }
        val columns = linkedMapOf(
            "block_media_links" to "blockId,role,mediaId",
            "block_audio_state" to "blockId,mediaId,positionMs,speedPercent,aMs,bMs,repeat,selectedMarkerId,role",
            "block_audio_markers" to "blockId,mediaId,id,startMs,endMs,label,note,originalMaster,hidden,role")
        val childSql = columns.keys.drop(1).associateWith { table ->
            db.query("SELECT sql FROM sqlite_master WHERE type='table' AND name=?", arrayOf(table)).use {
                check(it.moveToFirst()); it.getString(0)
            }
        }
        for ((table, fields) in columns) {
            db.execSQL("CREATE TEMP TABLE stage3b_$table AS SELECT $fields FROM $table")
            check(count(table) == count("stage3b_$table"))
        }
        val legacyCount = count("block_images")
        val overlayCount = count("practice_overlays")
        multiReferenceOldTriggers.forEach { db.execSQL("DROP TRIGGER IF EXISTS `$it`") }
        db.execSQL("DROP TABLE block_audio_markers")
        db.execSQL("DROP TABLE block_audio_state")
        db.execSQL("DROP TABLE block_media_links")
        db.execSQL("ALTER TABLE block_images ADD COLUMN position INTEGER NOT NULL DEFAULT 0")
        db.execSQL("""CREATE TABLE block_media_links (blockId TEXT NOT NULL, role TEXT NOT NULL, mediaId TEXT NOT NULL,
            position INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(blockId,role,mediaId),
            FOREIGN KEY(blockId) REFERENCES blocks(id) ON UPDATE NO ACTION ON DELETE RESTRICT,
            FOREIGN KEY(mediaId) REFERENCES media_assets(id) ON UPDATE NO ACTION ON DELETE RESTRICT)""")
        db.execSQL("CREATE INDEX index_block_media_links_mediaId ON block_media_links(mediaId)")
        db.execSQL("CREATE UNIQUE INDEX index_block_media_links_blockId_role_mediaId ON block_media_links(blockId,role,mediaId)")
        db.execSQL("CREATE INDEX index_block_media_links_blockId_role_position_mediaId ON block_media_links(blockId,role,position,mediaId)")
        db.execSQL("""INSERT INTO block_media_links SELECT blockId,role,mediaId,
            CASE WHEN role='VISUAL' AND EXISTS(SELECT 1 FROM block_images i WHERE i.blockId=l.blockId) THEN 1 ELSE 0 END
            FROM stage3b_block_media_links l""")
        childSql.values.forEach(db::execSQL)
        for ((table, fields) in columns.entries.drop(1)) db.execSQL("INSERT INTO $table ($fields) SELECT $fields FROM stage3b_$table")
        installBlockMediaConstraints(db)
        installBlockAudioConstraints(db)
        installPracticeOverlayConstraints(db)
        installReferencePositions(db)
        for ((table, fields) in columns) {
            check(count(table) == count("stage3b_$table"))
            empty("SELECT $fields FROM $table EXCEPT SELECT $fields FROM stage3b_$table")
            empty("SELECT $fields FROM stage3b_$table EXCEPT SELECT $fields FROM $table")
        }
        check(legacyCount == count("block_images") && overlayCount == count("practice_overlays"))
        empty("PRAGMA foreign_key_check")
        db.query("PRAGMA integrity_check").use { check(it.moveToFirst() && it.getString(0) == "ok") }
        columns.keys.forEach { db.execSQL("DROP TABLE stage3b_$it") }
    }
}
