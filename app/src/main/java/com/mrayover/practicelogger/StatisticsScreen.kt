package com.mrayover.practicelogger

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable internal fun StatisticsScreen(facts: List<PracticeFact>?, goals: List<GoalBundle>, error: String?,
    onHome: () -> Unit, onGoal: (String) -> Unit, onSession: (String) -> Unit) {
    BackHandler(onBack = onHome)
    val rows = facts.orEmpty()
    val recent = rows.filter { it.activeMs > 0 || it.outcome in listOf("SKIPPED", "COMPLETED") }
    val byBlock = rows.filter { it.blockId != null && it.activeMs > 0 }.groupBy { it.blockId }
        .values.sortedByDescending { list -> list.sumOf { it.activeMs } }
    val byGoal = rows.filter { it.goalId != null && it.activeMs > 0 }.groupBy { it.goalId }
        .entries.sortedByDescending { (_, list) -> list.sumOf { it.activeMs } }
    LazyColumn(Modifier.fillMaxSize().safeDrawingPadding().testTag("statistics-screen").padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 16.dp)) {
        item { TextButton(onClick = onHome) { Text("Back to Home") }; StatisticsHeading("Statistics") }
        when {
            error != null -> item { Text(error) }
            facts == null -> item { Text("Loading recorded practice…") }
            else -> {
                item {
                    StatisticsHeading("Total recorded practice")
                    Text(practiceDuration(rows.sumOf { it.activeMs }), Modifier.testTag("statistics-total"))
                    StatisticsHeading("This week")
                    Text(practiceDuration(statisticsWeekMs(rows, LocalDate.now())))
                    Text("Monday–Sunday, local time (as in the Journal weekly view). Block time is assigned to the session start date; Transcribe uses its activity start date. Time is not split across midnight.")
                    if (rows.none { it.activeMs > 0 }) Text("No recorded practice yet.")
                    Text("Committed active time includes unfinished sessions and overtime. Prep, pauses and ordinary playback are excluded. Standalone Transcribe counts after completion.")
                    StatisticsHeading("Practice by Block")
                }
                items(byBlock, key = { "block:${it.first().blockId}" }) { list ->
                    Text("${list.first().name} · ${practiceDuration(list.sumOf { it.activeMs })}")
                }
                val standalone = rows.filter { it.blockId == null }.sumOf { it.activeMs }
                if (standalone > 0) item { Text("Standalone Transcribe · ${practiceDuration(standalone)}") }
                item { StatisticsHeading("Practice by Goal") }
                items(byGoal, key = { "goal:${it.key}" }) { (id, list) ->
                    TextButton(onClick = { onGoal(checkNotNull(id)) }) {
                        Text("${goals.find { it.goal.id == id }?.goal?.title ?: "Historical Goal"} · ${practiceDuration(list.sumOf { it.activeMs })}")
                    }
                    val legacy = list.filter { it.attribution == "LEGACY_SESSION" }.sumOf { it.activeMs }
                    if (legacy > 0) Text("Includes ${practiceDuration(legacy)} from explicit legacy Goal-session links.")
                }
                item {
                    val noGoal = rows.filter { it.attribution == "NO_GOAL" }.sumOf { it.activeMs }
                    val unknown = rows.filter { it.attribution == "UNKNOWN" }.sumOf { it.activeMs }
                    if (noGoal > 0) Text("Recorded No Goal · ${practiceDuration(noGoal)}")
                    if (unknown > 0) Text("Unattributed historical time · ${practiceDuration(unknown)} — no saved Goal snapshot or explicit session link.")
                    Text("Practice time measures effort. Musical progress is assessed by you.")
                    StatisticsHeading("Recent activity (latest 20)")
                }
                items(recent.take(20), key = { it.id }) { PracticeActivityRow(it, onSession) }
            }
        }
    }
}

@Composable internal fun StatisticsHeading(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
}

@Composable internal fun PracticeActivityRow(fact: PracticeFact, onSession: (String) -> Unit = {}) {
    val date = Instant.ofEpochMilli(fact.startedAt).atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm"))
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("${fact.name} · ${practiceDuration(fact.activeMs)}")
        Text("$date · ${practiceOutcome(fact)}" + if (fact.sessionStatus == "IN_PROGRESS") " · Session unfinished" else "")
        if (fact.attribution == "LEGACY_SESSION") Text("Legacy Goal-session attribution")
        fact.security?.let { Text("Confidence / security: $it") }
        if (fact.note.isNotBlank()) Text(fact.note)
        fact.sessionId?.let { id -> TextButton(onClick = { onSession(id) }) { Text("Session notes and reflections") } }
    }
}
