package com.mrayover.practicelogger

import android.graphics.Bitmap
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.*

// Run the seed against the retained build27 debug APK, then install the new APK with -r.
// Seed uses only baseline APIs, and stores a byte snapshot for every table plus private media.
class LegacyReferenceSeedTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Test fun seedBaselineWithLegacyImageAndEveryStructuredTable(): Unit = runBlocking {
        val app=compose.activity.application; val db=BlockDatabase.open(app)
        try {
            val id=BlockRepository(db).save(null,BlockDraft("Legacy upgrade fixture","Keep all data","5","96","Original notes"))
            val bitmap=Bitmap.createBitmap(400,600,Bitmap.Config.ARGB_8888); bitmap.eraseColor(android.graphics.Color.GREEN)
            val png=ByteArrayOutputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it); it.toByteArray() }; bitmap.recycle()
            ImageReferenceRepository(db,File(app.filesDir,"block-images")).import(id) { ByteArrayInputStream(png) }
            db.sessions().insert(PracticeSession("legacy-session",1,status="IN_PROGRESS",note="Session note"))
            db.sessions().insert(Placement("legacy-placement","legacy-session",id,"Legacy","Keep",96,"Snapshot",0,300000,1234,note="Quick note",lastUsedBpm=111))
            db.routines().insert(SavedRoutine("legacy-routine","Legacy routine",1,2))
            db.routines().insert(RoutinePlacement("legacy-rp","legacy-routine",id,0,300000))
            db.goals().insert(Goal("legacy-goal","Legacy goal",progress=35,notes="Goal notes",createdAt=1,updatedAt=2))
            db.goals().insert(GoalBlock("legacy-goal",id,0)); db.goals().insert(GoalSession("legacy-session","legacy-goal"))
            db.logs().save(DailyLog("2026-10-29","Legacy daily Log"))
            val repo=TakeRepository(db,app); val spec=PhraseSpec(120,4); val pending=repo.prepare(id,spec)
            PhraseWave.write(pending.file,ShortArray(spec.frames)); val take=repo.save(pending); repo.rename(take.id,"Legacy take")
            BlockRepository(db).setArchived(id,true)
            File(app.filesDir,"legacy-structured.json").writeBytes(BackupCodec.encode(BackupRepository(db).snapshot()))
            val tables=listOf("blocks","sessions","placements","takes","routines","routine_placements","goals","goal_blocks","session_goals","daily_logs","block_images")
            val snapshot=org.json.JSONObject()
            tables.forEach { table -> db.openHelper.writableDatabase.query("SELECT * FROM $table").use { cursor ->
                val rows=org.json.JSONArray(); while(cursor.moveToNext()) { val row=org.json.JSONArray(); repeat(cursor.columnCount) { row.put(if(cursor.isNull(it)) org.json.JSONObject.NULL else cursor.getString(it)) }; rows.put(row) }; snapshot.put(table,rows)
            } }
            File(app.filesDir,"legacy-tables.json").writeText(snapshot.toString())
            listOf("block-images","recordings").forEach { folder -> File(app.filesDir,folder).listFiles()!!.forEach { file -> File(app.filesDir,"legacy-${file.name}").writeBytes(file.readBytes()) } }
        } finally { db.close() }
    }
}
class LegacyReferenceUpgradeTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Test fun installOverBuild27PreservesAllRowsOriginalImageAndAudioBytes(): Unit=runBlocking {
        val app=compose.activity.application; val db=BlockDatabase.open(app)
        try {
            assertEquals(BackupCodec.decode(File(app.filesDir,"legacy-structured.json").readBytes()),BackupRepository(db).snapshot())
            val snapshot=org.json.JSONObject(File(app.filesDir,"legacy-tables.json").readText())
            snapshot.keys().forEach { table -> db.openHelper.writableDatabase.query("SELECT * FROM $table").use { cursor ->
                val rows=org.json.JSONArray(); while(cursor.moveToNext()) { val row=org.json.JSONArray(); repeat(cursor.columnCount) { row.put(if(cursor.isNull(it)) org.json.JSONObject.NULL else cursor.getString(it)) }; rows.put(row) }
                assertEquals(table,snapshot.getJSONArray(table).toString(),rows.toString())
            } }
            val image=db.images().readAll().single(); ImageReferenceRepository(db,File(app.filesDir,"block-images")).decode(image).recycle()
            listOf("block-images","recordings").forEach { folder -> File(app.filesDir,folder).listFiles()!!.forEach { file -> assertArrayEquals(File(app.filesDir,"legacy-${file.name}").readBytes(),file.readBytes()) } }
            db.openHelper.writableDatabase.query("PRAGMA foreign_key_check").use { assertEquals(0,it.count) }
        } finally { db.close() }
    }
}

class PdfValidationNativeTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Test fun nativeRendererRejectsLockedCorruptAndTooManyPagesWithoutLosingAttachment(): Unit=runBlocking {
        val app=compose.activity.application; val db=BlockDatabase.open(app)
        try {
            val id=BlockRepository(db).save(null,BlockDraft("PDF validation","Read")); val repo=ImageReferenceRepository(db,File(app.filesDir,"block-images"))
            val assets=InstrumentationRegistry.getInstrumentation().context.assets
            val first=repo.import(id) { assets.open("references/single.pdf") }
            val page=repo.loadPage(first); assertEquals(1,page.pageCount); page.bitmap.recycle()
            for (name in listOf("locked.pdf", "owner-locked.pdf","corrupt.pdf","too-many.pdf")) {
                var rejected=false
                try { repo.import(id) { assets.open("references/$name") } } catch (_: IllegalArgumentException) { rejected=true }
                assertTrue(name,rejected); assertEquals(first,db.images().find(id))
                val original=assets.open("references/single.pdf").use { it.readBytes() }
                assertArrayEquals("Original bytes changed after $name",original,File(app.filesDir,"block-images/${first.fileName}").readBytes())
                println("Reopening intact original after rejection: $name")
                repo.decode(first).recycle()
            }
            repo.remove(id)
        } finally { db.close() }
    }
}
