package com.mrayover.practicelogger

import android.content.Context
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
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
class AdaptiveSchedulingTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private val zone=ZoneId.of("America/Los_Angeles")
    private val date=LocalDate.of(2026,11,12)
    private var clock=date.atTime(12,0).atZone(zone).toInstant().toEpochMilli()
    private fun memory()=Room.inMemoryDatabaseBuilder(context,BlockDatabase::class.java).addCallback(mediaConstraintCallback).build()
    private suspend fun seed(db: BlockDatabase): String {
        val block=BlockRepository(db).save(null,BlockDraft("Adaptive return","Play target"))
        val end=clock-5*RETURN_DAY_MS
        val attempt=end+4*RETURN_DAY_MS
        val sessions=listOf(PracticeSession("sa",end-2000,end,"COMPLETED"),PracticeSession("sb",attempt-1000,attempt+2000,"COMPLETED"))
        sessions.forEach { db.sessions().insert(it) }
        for ((i,s) in sessions.withIndex()) {
            val id=if(i==0) "a" else "b"
            db.sessions().insert(Placement(id,s.id,block,"Adaptive return","Play target",null,"",0,60000,activeMs=1000,outcome="COMPLETED"))
            db.sessions().insert(BlockCompletion(id,s.id,block,s.endedAt!!,null,null,null))
        }
        db.learning().insert(LearningObservation("a","HELD",end-1000,"FIRST_ATTEMPT","token","Target"))
        db.learning().insert(LearningObservation("b","HELD",attempt,"FIRST_ATTEMPT","token","Target","a"))
        return block
    }
    private suspend fun suggestion(repo: LearningRepository, block: String): ReturnSuggestion {
        val i=repo.inputs()
        return returnSuggestion(block,i.observations,i.practices,date,clock)
    }
    @Test fun calculationDeclineAcceptanceRetryAndManualOverridePreserveAuthority()=runBlocking {
        val db=memory()
        try {
            val block=seed(db); val repo=LearningRepository(db) { clock }
            repo.schedule(block,"FLAG_SET",operationId="flag")
            repo.schedule(block,"DATE_SET",date.plusDays(20).toString(),date.toString(),"manual")
            val before=db.learning().events(block)
            val s=suggestion(repo,block)
            assertEquals(6,s.intervalDays)
            assertEquals(before,db.learning().events(block)) // Reads never write dates.
            repo.schedule(block,"DECLINED",operationId="decline")
            val current=db.learning().events(block).first()
            assertEquals(before.first().dueDate,current.dueDate)
            assertTrue(current.returnSoon)
            assertTrue(runCatching { repo.accept(s,2,"stale",zone) }.isFailure)
            val accepted=repo.accept(s,3,"accept",zone)
            assertEquals(accepted,repo.accept(s,3,"accept",zone))
            assertEquals("ACCEPTED_SUGGESTION",accepted.origin)
            assertEquals(date.plusDays(6).toString(),accepted.dueDate)
            assertTrue(accepted.returnSoon)
            assertEquals(s,readAcceptedSuggestion(accepted.offeredSuggestion!!))
            repo.schedule(block,"FLAG_CLEAR",operationId="clear-flag")
            repo.schedule(block,"DECLINED",operationId="decline-after")
            assertEquals(accepted.offeredSuggestion,db.learning().events(block).first().offeredSuggestion)
            repo.schedule(block,"DATE_SET",date.plusDays(10).toString(),date.toString(),"manual-again")
            assertEquals("MANUAL",db.learning().events(block).first().origin)
            assertNull(db.learning().events(block).first().offeredSuggestion)
            repo.schedule(block,"DATE_CLEAR",operationId="clear-date")
            assertNull(db.learning().events(block).first().dueDate)
            validateGuidanceHistory(db.learning().events(block))
            assertEquals(1000L,db.sessions().find("sa")!!.activeMs)
        } finally { db.close() }
    }
    @Test fun correctedEvidenceAndCalendarChangesRejectStaleAcceptance()=runBlocking {
        val db=memory()
        try {
            val block=seed(db); val repo=LearningRepository(db) { clock }
            val s=suggestion(repo,block)
            repo.saveObservation("b","PARTIAL","FIRST_ATTEMPT","Target","a")
            assertTrue(runCatching { repo.accept(s,0,"stale",zone) }.isFailure)
            val updated=suggestion(repo,block)
            assertEquals(3,updated.intervalDays)
            clock+=RETURN_DAY_MS
            assertTrue(runCatching { repo.accept(updated,0,"midnight",zone) }.isFailure)
            assertTrue(db.learning().events(block).isEmpty())
        } finally { db.close() }
    }
    @Test fun confidenceChangesHaveNoIntervalEffectAndEndpointIdentitiesMustMatch()=runBlocking {
        val db=memory()
        try {
            val block=seed(db); val repo=LearningRepository(db) { clock }
            val s=suggestion(repo,block)
            db.sessions().save(BlockReflection("b",security=1))
            assertEquals(s,suggestion(repo,block))
            db.sessions().save(BlockReflection("b",security=5))
            assertEquals(s,suggestion(repo,block))
            val inputs=repo.inputs()
            val wrong=inputs.practices.map { if(it.placementId=="b") it.copy(completionBlockId="other") else it }
            assertEquals("STARTING",returnSuggestion(block,inputs.observations,wrong,date,clock).reason)
            val earlyEnd=inputs.practices.map { if(it.placementId=="a") it.copy(endedAt=it.completedAt!!-1) else it }
            assertEquals("STARTING",returnSuggestion(block,inputs.observations,earlyEnd,date,clock).reason)
        } finally { db.close() }
    }
    @Test fun zip10RoundtripKeepsAcceptedHistoryAfterCorrectionsAndZip9StaysFrozen()=runBlocking {
        val db=memory(); val target=memory()
        val root=File(context.cacheDir,"adaptive-zip").apply { mkdirs() }
        try {
            val block=seed(db); val repo=LearningRepository(db) { clock }
            val accepted=repo.accept(suggestion(repo,block),0,"accept",zone)
            repo.saveObservation("b","NOT_YET","FIRST_ATTEMPT","Target","a")
            assertEquals(2,suggestion(repo,block).intervalDays)
            val zip=PortableZip(context,db,root); val file=File(root,"return.zip")
            zip.create(file)
            val verified=zip.verify(file)
            assertEquals(PORTABLE_VERSION,verified.records.payload.getInt("version"))
            target.withTransaction { verified.records.insertInto(target.openHelper.writableDatabase) }
            assertEquals(listOf(accepted),target.learning().events(block))
            assertEquals("NOT_YET",target.learning().observation("b")!!.outcome)
            val changed=JSONObject(verified.records.payload.toString()).put("version",10)
            changed.getJSONObject("tables").remove("weekly_practice_targets")
            changed.getJSONObject("tables").remove("playground_layouts")
            val guidanceColumns=portableTables.first { it.name=="guidance_events" }.columns
            val forged=JSONObject(accepted.offeredSuggestion!!).put("elapsedMs",4*RETURN_DAY_MS+1).put("intervalDays",7).put("dueDate",date.plusDays(7).toString())
            changed.getJSONObject("tables").getJSONArray("guidance_events").getJSONArray(0)
                .put(guidanceColumns.indexOf("offeredSuggestion"),forged.toString())
                .put(guidanceColumns.indexOf("intervalDays"),7).put(guidanceColumns.indexOf("dueDate"),date.plusDays(7).toString())
            assertTrue(runCatching { PortableRecords(changed,10) }.isFailure)
            val old=JSONObject(verified.records.payload.toString()).put("version",9)
            old.getJSONObject("tables").remove("weekly_practice_targets")
            old.getJSONObject("tables").remove("playground_layouts")
            assertTrue(runCatching { PortableRecords(old,9) }.isFailure)
            val manual=accepted.copy(origin="MANUAL",offeredSuggestion=null)
            validateGuidanceHistory(listOf(manual),adaptive=false)
            val table=old.getJSONObject("tables").getJSONArray("guidance_events").getJSONArray(0)
            val columns=portableTables.first { it.name=="guidance_events" }.columns
            table.put(columns.indexOf("origin"),"MANUAL").put(columns.indexOf("offeredSuggestion"),JSONObject.NULL)
            PortableRecords(old,9).validateRelationships(context)
        } finally { db.close();target.close();root.deleteRecursively() }
    }
    @Test fun malformedProvenanceBoundsAndForeignEvidenceAreRejected()=runBlocking {
        val db=memory()
        try {
            val block=seed(db);val repo=LearningRepository(db) { clock }
            val s=suggestion(repo,block)
            val raw=s.provenance()
            listOf(JSONObject(raw).put("policy","UNKNOWN"),JSONObject(raw).put("accepted",false),JSONObject(raw).put("intervalDays",31),
                JSONObject(raw).put("intervalDays",5),JSONObject(raw).put("elapsedMs",100),JSONObject(raw).put("dueDate","2026-02-30"),
                JSONObject(raw).put("extra",true),JSONObject(raw).put("anchorId","b")).forEach {
                assertTrue(runCatching { readAcceptedSuggestion(it.toString()) }.isFailure)
            }
            val accepted=repo.accept(s,0,"accept",zone)
            assertTrue(runCatching { validateGuidanceEvent(accepted.copy(blockId="other")) }.isFailure)
            assertTrue(runCatching { validateGuidanceEvent(accepted,adaptive=false) }.isFailure)
            repo.schedule(block,"DATE_CLEAR",operationId="clear")
            validateGuidanceHistory(db.learning().events(block))
        } finally { db.close() }
    }
    @Test fun exploratoryBlocksCanAcceptStartingSuggestionsWithoutAssessments()=runBlocking {
        val db=memory()
        try {
            val block=BlockRepository(db).save(null,BlockDraft("Explore","Listen and experiment"))
            val repo=LearningRepository(db) { clock }
            repo.schedule(block,"FLAG_SET",operationId="flag")
            val s=suggestion(repo,block)
            assertEquals("STARTING",s.reason)
            val accepted=repo.accept(s,1,"starting",zone)
            assertTrue(accepted.returnSoon)
            assertEquals(3,accepted.intervalDays)
            assertTrue(db.learning().history(block).isEmpty())
        } finally { db.close() }
    }
}
