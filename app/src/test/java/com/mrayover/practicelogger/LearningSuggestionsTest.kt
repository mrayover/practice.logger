package com.mrayover.practicelogger

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class LearningSuggestionsTest {
    private val today = LocalDate.of(2026, 11, 12)
    private val zone = ZoneId.of("America/Los_Angeles")
    private val now = today.atTime(12,0).atZone(zone).toInstant().toEpochMilli()
    private val end = now - 5 * RETURN_DAY_MS
    private val attempt = end + 4 * RETURN_DAY_MS
    private val a = LearningObservation("a", "HELD", end - 1000, "FIRST_ATTEMPT", "token", "Bars 1–4 at 80")
    private val b = a.copy(placementId = "b", observedAt = attempt, anchorId = "a")
    private fun fact(id: String, start: Long, finish: Long?) = ReturnPractice(id, "s$id", "block", start, finish, "COMPLETED", 1000, "COMPLETED", finish, "s$id", "block")
    private val facts get() = listOf(fact("a",end-2000,end), fact("b",attempt-1000,attempt+2000))
    private fun suggest(obs: List<LearningObservation> = listOf(a,b), rows: List<ReturnPractice> = facts) =
        returnSuggestion("block",obs,rows,today,now)
    private fun starting(s: ReturnSuggestion) { assertEquals("STARTING",s.reason); assertEquals(3,s.intervalDays); assertNull(s.elapsedMs); assertNotNull(s.insufficient) }
    private fun block(id: String, title: String = id, archived: Boolean = false) = Block(id,title,"Explore",archived=archived,createdAt=0,updatedAt=0)
    private fun event(id: String, time: Long = 0, date: String? = null, flag: Boolean = true) = GuidanceEvent("e$id",id,1,time,
        if (flag) "FLAG_SET" else "DATE_SET",flag,date,if(date!=null) today.toString() else null,if(date!=null) 0 else null,if(date!=null) "MANUAL" else "NONE")

    @Test fun actualDelayAdjustmentsAndClamps() {
        assertEquals(6,suggest().intervalDays)
        assertEquals(3,suggest(listOf(a,b.copy(outcome="PARTIAL"))).intervalDays)
        assertEquals(2,suggest(listOf(a,b.copy(outcome="NOT_YET"))).intervalDays)
        assertEquals(1,adjustedReturnDays(RETURN_DAY_MS,"PARTIAL"))
        assertEquals(1,adjustedReturnDays(RETURN_DAY_MS,"NOT_YET"))
        assertEquals(2,adjustedReturnDays(RETURN_DAY_MS,"HELD"))
        assertEquals(7,adjustedReturnDays(4*RETURN_DAY_MS+1,"HELD"))
        assertEquals(30,adjustedReturnDays(Long.MAX_VALUE,"HELD"))
        assertEquals(30,adjustedReturnDays(100*RETURN_DAY_MS,"PARTIAL"))
    }
    @Test fun minimumDelayIsExactAndSameSessionNeverQualifies() {
        val early=b.copy(observedAt=end+RETURN_DAY_MS-1)
        starting(suggest(listOf(a,early), listOf(facts[0],facts[1].copy(startedAt=early.observedAt-1,completedAt=early.observedAt+1))))
        val exact=early.copy(observedAt=end+RETURN_DAY_MS)
        assertEquals(2,suggest(listOf(a,exact),listOf(facts[0],facts[1].copy(startedAt=exact.observedAt-1,completedAt=exact.observedAt+1))).intervalDays)
        starting(suggest(rows=facts.map { it.copy(sessionId="same") }))
    }
    @Test fun criteriaAnchorAndCorrectedContextMustMatch() {
        listOf(b.copy(criterionToken="changed"),b.copy(criterion="Changed passage"),b.copy(anchorId=null),b.copy(context="UNKNOWN"),
            b.copy(context="LATER_RECOLLECTION"),b.copy(outcome="NOT_ASSESSED")).forEach { starting(suggest(listOf(a,it))) }
        starting(suggest(listOf(a.copy(context="UNKNOWN",correctedAt=now),b)))
        starting(suggest(listOf(a.copy(outcome="NOT_ASSESSED",correctedAt=now),b)))
        assertEquals(2,suggest(listOf(a,b.copy(outcome="NOT_YET",correctedAt=now))).intervalDays)
    }
    @Test fun skippedUnfinishedMissingAndZeroTimeAreNotPositiveEvidence() {
        for (i in 0..1) {
            listOf(facts[i].copy(outcome="SKIPPED"),facts[i].copy(sessionStatus="IN_PROGRESS"),facts[i].copy(completedAt=null),facts[i].copy(activeMs=0)).forEach { f ->
                starting(suggest(rows=facts.toMutableList().also { it[i]=f }))
            }
        }
        starting(suggest(rows=listOf(facts[1])))
        starting(suggest(obs=emptyList()))
    }
    @Test fun clockAnomaliesAndInterveningPracticeAreConservative() {
        starting(suggest(listOf(a.copy(observedAt=end+1),b)))
        starting(suggest(rows=listOf(facts[0],facts[1].copy(completedAt=now+1))))
        starting(suggest(listOf(a,b.copy(correctedAt=now+1))))
        starting(suggest(rows=facts+fact("c",end+1000,end+2000)))
        starting(suggest(rows=facts+fact("c",end-RETURN_DAY_MS,null)))
        starting(suggest(rows=facts+fact("c",end+1000,end-1)))
        assertEquals("HELD",suggest(rows=facts+fact("c",end-RETURN_DAY_MS,end-RETURN_DAY_MS+1000)).reason)
        assertEquals("HELD",suggest(rows=facts+fact("future",now+1000,null)).reason)
    }
    @Test fun practiceDurationCannotIncreaseIntervals() {
        val expected=suggest()
        assertEquals(expected,suggest(rows=facts.map { it.copy(activeMs=10000000) }))
        assertTrue(expected.explanation.contains("4 days"))
    }
    @Test fun precedenceDeduplicationReasonsAndStableTies() {
        val goals=listOf(GoalBundle(Goal("g","Goal",createdAt=0,updatedAt=0),listOf(GoalBlock("g","block",0),GoalBlock("g","attention",1))))
        val blocks=listOf(block("attention"),block("due"),block("block"),block("flag"),block("archived",archived=true))
        val events=listOf(event("flag",100),event("due",date=today.toString(),flag=false),event("block",200,date=today.toString()),event("archived",300))
        val obs=listOf(a,b.copy(outcome="PARTIAL"))
        val r=recommendedBlocks(blocks,goals,events,obs,facts,emptyList(),today,now,zone)
        assertEquals(listOf("block","flag","due","attention"),r.map { it.blockId })
        assertEquals(4,r.first().reasons.size)
        assertEquals(r,recommendedBlocks(blocks,goals,events.reversed(),obs.reversed(),facts.reversed(),emptyList(),today,now,zone))
    }
    @Test fun priorityAndTitlesResolveFlagTiesAndDueDatesOrderBeforePriority() {
        val g=GoalBundle(Goal("g","Goal",priority=0,createdAt=0,updatedAt=0),listOf(GoalBlock("g","z",0)),listOf(BlockPrimaryGoal("z","g")))
        val blocks=listOf(block("a"),block("z"),block("b"))
        val r=recommendedBlocks(blocks,listOf(g),blocks.map { event(it.id,10) },emptyList(),emptyList(),emptyList(),today,now,zone)
        assertEquals(listOf("z","a","b"),r.map { it.blockId })
        val due=listOf(event("z",date=today.toString(),flag=false),event("a",date=today.minusDays(1).toString(),flag=false))
        assertEquals(listOf("a","z"),recommendedBlocks(blocks,listOf(g),due,emptyList(),emptyList(),emptyList(),today,now,zone).map { it.blockId })
    }
    @Test fun attentionUsesPreviousSevenLocalDatesAndOnlyTrustworthyAttribution() {
        val g=GoalBundle(Goal("g","Goal",createdAt=0,updatedAt=0),listOf(GoalBlock("g","attention",0)))
        fun recommendations(f: PracticeFact, goal: GoalBundle=g) = recommendedBlocks(listOf(block("attention")),listOf(goal),emptyList(),emptyList(),emptyList(),listOf(f),today,now,zone)
        val start=today.minusDays(7).atStartOfDay(zone).toInstant().toEpochMilli()
        val f=PracticeFact("time",null,null,null,"Recorded",start,null,1000,"COMPLETED","COMPLETED","g","SNAPSHOT",null,"")
        assertTrue(recommendations(f).isEmpty())
        assertEquals(1,recommendations(f.copy(startedAt=start-1)).size)
        assertEquals(1,recommendations(f.copy(attribution="UNKNOWN")).size)
        assertEquals(1,recommendations(f.copy(goalId=null)).size)
        assertEquals(1,recommendations(f.copy(activeMs=0)).size)
        assertEquals(1,recommendations(f.copy(startedAt=now)).size)
        assertTrue(recommendations(f.copy(activeMs=0),g.copy(goal=g.goal.copy(completed=true))).isEmpty())
    }
    @Test fun difficultiesExpireAfterFourteenLocalDatesAndNoGoalStillQualifies() {
        val blocks=listOf(block("block"))
        val obs=listOf(a,b.copy(outcome="PARTIAL"))
        val r=recommendedBlocks(blocks,emptyList(),emptyList(),obs,facts,emptyList(),today,now,zone)
        assertEquals(2,r.single().tier)
        assertTrue(recommendedBlocks(blocks,emptyList(),emptyList(),obs,facts,emptyList(),today.plusDays(14),now+14*RETURN_DAY_MS,zone).isEmpty())
        assertTrue(recommendedBlocks(blocks,emptyList(),emptyList(),listOf(a,b.copy(outcome="NOT_ASSESSED")),facts,emptyList(),today,now,zone).isEmpty())
    }
}
