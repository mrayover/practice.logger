package com.mrayover.practicelogger

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LearningRepositoryTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private fun memory() = Room.inMemoryDatabaseBuilder(context, BlockDatabase::class.java).addCallback(mediaConstraintCallback).build()
    private suspend fun seed(db: BlockDatabase): Pair<String,List<Placement>> {
        val block = BlockRepository(db).save(null, BlockDraft("Repeated", "Play passage at 80"))
        val sessions = SessionRepository(db) { 1000 }
        sessions.addBlocks(listOf(block,block,block))
        val id = sessions.start()
        return block to db.sessions().find(id)!!.ordered
    }
    private fun fails(action: () -> Unit) { assertTrue(runCatching(action).isFailure) }

    @Test fun exactPlacementsCriteriaCorrectionsAndPracticeTimeAreIndependent() = runBlocking {
        val db=memory()
        try {
            val (block,p)=seed(db)
            var time=2000L
            val repo=LearningRepository(db) { time++ }
            val sessions=SessionRepository(db)
            sessions.checkpoint(p[0].sessionId, 1200)
            val first=repo.saveObservation(p[0].id,"HELD","FIRST_ATTEMPT","Bars 1–4 at 80, no score")
            assertEquals(first,repo.saveObservation(p[0].id,"HELD","FIRST_ATTEMPT",first.criterion))
            val second=repo.saveObservation(p[1].id,"PARTIAL","LATER_RECOLLECTION",first.criterion,first.placementId)
            assertEquals(first.criterionToken,second.criterionToken)
            assertEquals(first.placementId,second.anchorId)
            val third=repo.saveObservation(p[2].id,"HELD","UNKNOWN",first.criterion)
            assertNotEquals(first.criterionToken,third.criterionToken)
            val corrected=repo.saveObservation(p[0].id,"NOT_YET","LATER_RECOLLECTION",first.criterion)
            assertEquals(first.observedAt,corrected.observedAt)
            assertNotNull(corrected.correctedAt)
            assertEquals(first.criterionToken,corrected.criterionToken)
            assertTrue(runCatching { repo.saveObservation(p[0].id,"HELD","UNKNOWN","Changed target") }.isFailure)
            assertEquals(second,db.learning().observation(p[1].id))
            assertEquals(1200L,db.sessions().find(p[0].sessionId)!!.activeMs)
            assertTrue(db.sessions().find(p[0].sessionId)!!.completions.isEmpty())
            assertEquals(3,db.learning().history(block).size)
        } finally { db.close() }
    }

    @Test fun optionalUnassessedUnfinishedAndSkippedObservationsDoNotInventCompletion() = runBlocking {
        val db=memory()
        try {
            val (block,p)=seed(db)
            val repo=LearningRepository(db) { 2000 }
            assertTrue(db.learning().history(block).isEmpty())
            val o=repo.saveObservation(p[0].id,"NOT_ASSESSED","UNKNOWN",null)
            assertNull(o.criterionToken)
            assertNull(o.anchorId)
            assertEquals("IN_PROGRESS",db.sessions().find(p[0].sessionId)!!.session.status)
            SessionRepository(db).finishBlock(p[0].sessionId,"SKIPPED","",placementId=p[0].id)
            assertEquals(o,db.learning().observation(p[0].id))
            assertEquals("SKIPPED",db.sessions().find(p[0].sessionId)!!.ordered[0].outcome)
            assertTrue(db.sessions().find(p[0].sessionId)!!.completions.isEmpty())
            assertNull(db.learning().observation(p[1].id))
        } finally { db.close() }
    }

    @Test fun manualSchedulingClearingDecliningAndRetriesRetainIndependentState() = runBlocking {
        val db=memory()
        try {
            val (block,p)=seed(db)
            val repo=LearningRepository(db) { 2000 }
            val first=repo.schedule(block,"FLAG_SET",operationId="one")
            assertEquals(first,repo.schedule(block,"FLAG_SET",operationId="one"))
            assertTrue(runCatching { repo.schedule(block,"FLAG_CLEAR",operationId="one") }.isFailure)
            val date=repo.schedule(block,"DATE_SET","2026-11-19","2026-11-12","two")
            assertEquals(7,date.intervalDays)
            assertEquals("MANUAL",date.origin)
            assertNull(date.offeredSuggestion)
            val declined=repo.schedule(block,"DECLINED",operationId="three")
            assertEquals(date.dueDate,declined.dueDate)
            assertTrue(declined.returnSoon)
            val cleared=repo.schedule(block,"FLAG_CLEAR",operationId="four")
            assertFalse(cleared.returnSoon)
            assertEquals(date.dueDate,cleared.dueDate)
            val noDate=repo.schedule(block,"DATE_CLEAR",operationId="five")
            assertNull(noDate.dueDate)
            assertNull(noDate.intervalDays)
            assertEquals("NONE",noDate.origin)
            repo.schedule(block,"FLAG_SET",operationId="six")
            SessionRepository(db).finishBlock(p[0].sessionId,"COMPLETED","",placementId=p[0].id)
            assertTrue(db.learning().events(block).first().returnSoon)
            validateGuidanceHistory(db.learning().events(block))
            assertEquals(6,db.learning().events(block).size)
            assertTrue(runCatching { repo.schedule(block,"DATE_SET","2026-11-11","2026-11-12") }.isFailure)
            assertTrue(runCatching { repo.schedule(block,"DATE_SET","2026-02-30","2026-11-12") }.isFailure)
        } finally { db.close() }
    }

    @Test fun restrictiveHistorySurvivesArchiveAndPreventsBlockAndSessionDeletion() = runBlocking {
        val db=memory()
        try {
            val block=BlockRepository(db).save(null,BlockDraft("Flag only","Explore"))
            val repo=LearningRepository(db)
            repo.schedule(block,"FLAG_SET")
            repo.schedule(block,"FLAG_CLEAR")
            assertTrue(BlockRepository(db).deletePermanently(block) is BlockDeletionResult.Blocked)
            assertEquals(2,db.blocks().dependencies(block).guidanceEvents)
            db.blocks().update(db.blocks().find(block)!!.copy(archived=true))
            assertEquals(2,db.learning().events(block).size)
            val (_,p)=seed(db)
            repo.saveObservation(p[0].id,"HELD","FIRST_ATTEMPT","Criterion")
            assertTrue(runCatching { db.sessions().removePlacement(p[0].id) }.isFailure)
            assertTrue(runCatching { db.openHelper.writableDatabase.execSQL("DELETE FROM sessions WHERE id=?",arrayOf(p[0].sessionId)) }.isFailure)
            assertNotNull(db.learning().observation(p[0].id))
        } finally { db.close() }
    }

    @Test fun reopenAndZip9PreserveAnchorsSchedulingAndOldZip8HasNoFabricatedEvidence() = runBlocking {
        val name="lge-reopen.db"
        context.deleteDatabase(name)
        var db=BlockDatabase.open(context,name)
        val restored=memory()
        val old=memory()
        val folder=File(context.cacheDir,"lge-zip").apply { mkdirs() }
        try {
            val (block,p)=seed(db)
            val repo=LearningRepository(db) { 2000 }
            val a=repo.saveObservation(p[0].id,"HELD","FIRST_ATTEMPT","Same target")
            repo.saveObservation(p[1].id,"PARTIAL","FIRST_ATTEMPT",a.criterion,a.placementId)
            repo.schedule(block,"DATE_SET","2026-12-01","2026-11-12","stable")
            repo.schedule(block,"FLAG_SET")
            val before=db.learning().history(block)
            val events=db.learning().events(block)
            val session=db.sessions().find(p[0].sessionId)
            db.close(); db=BlockDatabase.open(context,name)
            assertEquals(before,db.learning().history(block))
            assertEquals(events,db.learning().events(block))
            val zip=PortableZip(context,db,folder)
            val file=File(folder,"learning.zip")
            zip.create(file)
            val verified=zip.verify(file)
            restored.withTransaction { verified.records.insertInto(restored.openHelper.writableDatabase) }
            assertEquals(before,restored.learning().history(block))
            assertEquals(events,restored.learning().events(block))
            assertEquals(session!!.session,restored.sessions().find(p[0].sessionId)!!.session)
            assertEquals(session.ordered,restored.sessions().find(p[0].sessionId)!!.ordered)
            assertFalse(portableTargetEmpty(restored.openHelper.writableDatabase))
            val payload=JSONObject(verified.records.payload.toString())
            payload.put("version",8)
            payload.getJSONObject("tables").remove("weekly_practice_targets")
            payload.getJSONObject("tables").remove("learning_observations")
            payload.getJSONObject("tables").remove("guidance_events")
            val historical=PortableRecords(payload,8)
            historical.validateRelationships(context)
            old.withTransaction { historical.insertInto(old.openHelper.writableDatabase) }
            assertTrue(old.learning().history(block).isEmpty())
            assertTrue(old.learning().events(block).isEmpty())
            assertEquals(session.session,old.sessions().find(p[0].sessionId)!!.session)
            assertEquals(session.ordered,old.sessions().find(p[0].sessionId)!!.ordered)
            val json=BackupCodec.encode(BackupRepository(db).snapshot())
            assertFalse(json.toString(Charsets.UTF_8).contains("learning_observations"))
            assertFalse(json.toString(Charsets.UTF_8).contains("guidance_events"))
            assertTrue(runCatching { BackupRepository(restored).restore(BackupCodec.decode(json)) }.exceptionOrNull() is NonEmptyRestoreException)
        } finally {
            db.close(); restored.close(); old.close()
            folder.deleteRecursively(); context.deleteDatabase(name)
        }
    }

    @Test fun malformedComparisonsAndSchedulingHistoryAreRejected() {
        val p=Placement("p","s","b","Block","Act",null,"",0,60000)
        val q=p.copy(id="q",position=1)
        val s=PracticeSession("s",0,status="IN_PROGRESS")
        val a=LearningObservation("p","HELD",100,"FIRST_ATTEMPT","token","Target")
        val b=LearningObservation("q","PARTIAL",200,"UNKNOWN","token","Target","p")
        validateLearningHistory(listOf(a,b),listOf(p,q),listOf(s))
        fails { validateLearningHistory(listOf(a,b.copy(criterion="Changed")),listOf(p,q),listOf(s)) }
        fails { validateLearningHistory(listOf(a.copy(anchorId="q"),b),listOf(p,q),listOf(s)) }
        fails { validateLearningHistory(listOf(a,b.copy(anchorId="missing")),listOf(p,q),listOf(s)) }
        fails { validateLearningHistory(listOf(a,b),listOf(p,q.copy(sourceBlockId="other")),listOf(s)) }
        fails { validateLearningHistory(listOf(a),listOf(p),listOf(s.copy(status="DRAFT"))) }
        fails { validateLearningObservation(a.copy(outcome="MASTERY")) }
        fails { validateLearningObservation(a.copy(context="INFERRED")) }
        val e=GuidanceEvent("id","b",1,0,"FLAG_SET",true,null,null,null,"NONE")
        validateGuidanceHistory(listOf(e))
        fails { validateGuidanceHistory(listOf(e.copy(revision=2))) }
        fails { validateGuidanceHistory(listOf(e.copy(returnSoon=false))) }
        fails { validateGuidanceEvent(e.copy(offeredSuggestion="unsupported")) }
        fails { validateGuidanceEvent(e.copy(dueDate="2026-11-19",baseDate="2026-11-12",intervalDays=8,origin="MANUAL")) }
    }

    @Test fun additiveRoom24UpgradePreservesExistingPracticeAndHasNoLearningBackfill() = runBlocking {
        val name="lge-room24.db"
        context.deleteDatabase(name)
        val file=context.getDatabasePath(name).apply { parentFile!!.mkdirs() }
        val schema=javaClass.classLoader!!.getResourceAsStream("com.mrayover.practicelogger.BlockDatabase/24.json")!!
            .bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
        SQLiteDatabase.openOrCreateDatabase(file,null).use { sql ->
            val entities=schema.getJSONArray("entities")
            repeat(entities.length()) { i ->
                val e=entities.getJSONObject(i)
                fun execute(query: String) = sql.execSQL(query.replace("$"+"{TABLE_NAME}",e.getString("tableName")))
                execute(e.getString("createSql"))
                e.optJSONArray("indices")?.let { indexes -> repeat(indexes.length()) { execute(indexes.getJSONObject(it).getString("createSql")) } }
            }
            sql.execSQL("INSERT INTO blocks VALUES ('b','Block','Act',5,NULL,'Keep',1,1,2)")
            sql.execSQL("INSERT INTO sessions VALUES ('s',1,NULL,'IN_PROGRESS',0,'Session')")
            sql.execSQL("INSERT INTO placements VALUES ('p','s','b','Block','Act',NULL,'Ref',0,300000,1200,'PENDING',0,'Note',120,300)")
            schema.getJSONArray("setupQueries").let { setup -> repeat(setup.length()) { sql.execSQL(setup.getString(it)) } }
            sql.version=24
        }
        val db=BlockDatabase.open(context,name)
        try {
            assertEquals(26,db.openHelper.writableDatabase.version)
            assertEquals("Keep",db.blocks().find("b")!!.notes)
            val p=db.sessions().find("s")!!.current!!
            assertEquals(1200L,p.activeMs)
            assertEquals(300L,p.prepMs)
            assertEquals("Note",p.note)
            assertTrue(db.learning().history("b").isEmpty())
            assertTrue(db.learning().events("b").isEmpty())
            db.openHelper.writableDatabase.query("PRAGMA foreign_key_check").use { assertFalse(it.moveToFirst()) }
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
