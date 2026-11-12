package com.mrayover.practicelogger

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

private val outcomeLabels = listOf("Held up / Met intended target", "Partially held up", "Didn't hold up yet", "Not assessed")
private val contextLabels = listOf("First attempt, before rehearsing this task", "Later recollection", "Unknown context")

/** Presentation only: never changes timing, recording, playback or workspace ownership. */
@Composable
internal fun LearningButton(blockId: String, placement: Placement? = null) {
    var open by rememberSaveable(blockId, placement?.id) { mutableStateOf(false) }
    TextButton(onClick = { open = true }, modifier = Modifier.heightIn(min = 48.dp).testTag("learning-open")) {
        Text("Learning & revisits")
    }
    if (open) LearningDialog(blockId, placement) { open = false }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LearningDialog(blockId: String, placement: Placement?, onClose: () -> Unit) {
    val app = LocalContext.current.applicationContext
    val db = remember(app) { BlockDatabase.appInstance(app) }
    val repository = remember(db) { LearningRepository(db) }
    val scope = rememberCoroutineScope()
    val historyFlow = remember(blockId, db) { db.learning().observeHistory(blockId) }
    val eventsFlow = remember(blockId, db) { db.learning().observeEvents(blockId) }
    val history by historyFlow.collectAsStateWithLifecycle(initialValue = null)
    val events by eventsFlow.collectAsStateWithLifecycle(initialValue = null)
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var due by rememberSaveable(blockId) { mutableStateOf("") }
    var operationId by rememberSaveable(blockId) { mutableStateOf(UUID.randomUUID().toString()) }
    var pendingAction by rememberSaveable(blockId) { mutableStateOf<String?>(null) }
    var pendingDate by rememberSaveable(blockId) { mutableStateOf<String?>(null) }
    var pendingBase by rememberSaveable(blockId) { mutableStateOf<String?>(null) }
    val ready = history != null && events != null && !busy
    fun schedule(action: String, date: String? = null) {
        if (busy || history == null || events == null) return
        if (action != pendingAction || date != pendingDate) {
            operationId = UUID.randomUUID().toString(); pendingBase = null
        }
        pendingAction = action
        pendingDate = date
        if (pendingBase == null) pendingBase = LocalDate.now().toString()
        val requestId = operationId
        val requestBase = pendingBase!!
        busy = true; message = null
        scope.launch {
            try {
                repository.schedule(blockId, action, date, requestBase, requestId)
                operationId = UUID.randomUUID().toString(); pendingAction = null; pendingDate = null; pendingBase = null
                message = "Revisit choice saved."
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { message = "Could not save. Check the date (YYYY-MM-DD, today or later) and available storage, then retry." }
            finally { busy = false }
        }
    }
    Dialog(onDismissRequest = { if (!busy) onClose() }) {
        Surface(shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f)) {
            Column(Modifier.padding(16.dp).imePadding()) {
                Text("Learning & revisits", style = MaterialTheme.typography.titleLarge)
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Optional. Confidence and practice time do not measure retention.", style = MaterialTheme.typography.bodySmall)
                    if (history == null || events == null) Text("Loading saved choices…")
                    if (placement != null && history != null) {
                        val saved = history!!.find { it.placementId == placement.id }
                        key(placement.id, saved?.placementId) {
                            ObservationEditor(placement, saved, history!!.filter { it.placementId != placement.id && it.criterionToken != null }, ready) { outcome, context, criterion, anchor ->
                                if (busy) return@ObservationEditor
                                busy = true; message = null
                                scope.launch {
                                    try {
                                        repository.saveObservation(placement.id, outcome, context, criterion, anchor)
                                        message = "Observation saved."
                                    } catch (cancelled: CancellationException) { throw cancelled }
                                    catch (_: Exception) { message = "Could not save the observation. Keep saved criteria unchanged, check storage and retry." }
                                    finally { busy = false }
                                }
                            }
                        }
                    }
                    HorizontalDivider()
                    Text("Optional revisits", style = MaterialTheme.typography.titleMedium)
                    val latest = events?.firstOrNull()
                    Text(if (latest?.returnSoon == true) "Return Soon is set" else "Return Soon is not set")
                    OutlinedButton(enabled = ready, onClick = { schedule(if (latest?.returnSoon == true) "FLAG_CLEAR" else "FLAG_SET") }) {
                        Text(if (latest?.returnSoon == true) "Clear flag" else "Return Soon")
                    }
                    Text(latest?.dueDate?.let { "Revisit: $it · chosen manually" } ?: "No revisit date")
                    OutlinedTextField(due, { due = it }, enabled = ready, singleLine = true,
                        label = { Text("Revisit date (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth().testTag("learning-date"))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(1L, 2L, 3L, 4L, 5L, 7L, 14L, 30L).forEach { days ->
                            TextButton(enabled = ready, onClick = { due = LocalDate.now().plusDays(days).toString() }) { Text("$days days") }
                        }
                    }
                    Button(enabled = ready && due.isNotBlank(), onClick = { schedule("DATE_SET", due.trim()) },
                        modifier = Modifier.testTag("learning-date-save")) { Text("Save revisit date") }
                    TextButton(enabled = ready && latest?.dueDate != null, onClick = { schedule("DATE_CLEAR") }) { Text("Clear date") }
                    TextButton(enabled = ready, onClick = { schedule("DECLINED") }) { Text("No scheduling") }
                    Text("No scheduling leaves any existing flag or date unchanged. Clear them separately. Dates are optional opportunities, without reminders.",
                        style = MaterialTheme.typography.bodySmall)
                    if (!history.isNullOrEmpty()) {
                        HorizontalDivider()
                        Text("Learning history", style = MaterialTheme.typography.titleMedium)
                        history!!.forEach { o ->
                            LearningHistoryEntry(o)
                        }
                    }
                    if (!events.isNullOrEmpty()) {
                        HorizontalDivider()
                        Text("Revisit history", style = MaterialTheme.typography.titleMedium)
                        events!!.take(20).forEach { e ->
                            Text(sessionDate(e.recordedAt) + " · " + when(e.action) {
                                "FLAG_SET" -> "Return Soon set"
                                "FLAG_CLEAR" -> "Return Soon cleared"
                                "DATE_SET" -> "Manual revisit: " + e.dueDate
                                "DATE_CLEAR" -> "Revisit date cleared"
                                else -> "Scheduling declined; previous choices retained"
                            }, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                message?.let { Text(it, modifier = Modifier.testTag("learning-message")) }
                TextButton(onClick = onClose, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Close") }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ObservationEditor(placement: Placement, saved: LearningObservation?, earlier: List<LearningObservation>, enabled: Boolean,
    onSave: (String, String, String?, String?) -> Unit) {
    var outcome by rememberSaveable(placement.id) { mutableStateOf(saved?.outcome) }
    var context by rememberSaveable(placement.id) { mutableStateOf(saved?.context ?: "UNKNOWN") }
    var criterion by rememberSaveable(placement.id) { mutableStateOf(saved?.criterion ?: if (saved == null) placement.action.take(LEARNING_TEXT_LIMIT) else "") }
    var anchor by rememberSaveable(placement.id) { mutableStateOf(saved?.anchorId) }
    Text("First-attempt observation", style = MaterialTheme.typography.titleMedium)
    Text("Only confirm first attempt if it was before rehearsal or warm-up of this task. A later recollection remains labeled.")
    learningOutcomes.zip(outcomeLabels).forEach { (value, label) ->
        FilterChip(selected = outcome == value, onClick = { outcome = value }, enabled = enabled, label = { Text(label) },
            modifier = Modifier.testTag("learning-outcome-$value"))
    }
    learningContexts.zip(contextLabels).forEach { (value, label) ->
        FilterChip(selected = context == value, onClick = { context = value }, enabled = enabled, label = { Text(label) })
    }
    OutlinedTextField(criterion, { if (it.length <= LEARNING_TEXT_LIMIT) { criterion = it; anchor = null } },
        enabled = enabled && saved == null, label = { Text("Intended target / criterion (optional)") },
        modifier = Modifier.fillMaxWidth().testTag("learning-criterion"))
    if (saved == null) {
        Text("Changed passage, tempo, difficulty, assistance or success threshold needs a new criterion. Matching text alone does not confirm comparability.",
            style = MaterialTheme.typography.bodySmall)
        earlier.firstOrNull()?.let { previous ->
            Text("Earlier criterion: " + previous.criterion, style = MaterialTheme.typography.bodySmall)
            TextButton(enabled = enabled, onClick = { criterion = previous.criterion!!; anchor = previous.placementId }) {
                Text("Confirm same target and conditions")
            }
        }
        Text(if (anchor == null) "New criterion, or unclassified if left blank" else "Earlier criterion explicitly confirmed")
        if (anchor != null) TextButton(enabled = enabled, onClick = { anchor = null }) { Text("Use a new criterion") }
    } else Text("Saved criterion and observation time are preserved. Corrections change only outcome/context.", style = MaterialTheme.typography.bodySmall)
    Button(enabled = enabled && outcome != null, onClick = { onSave(outcome!!, context, criterion.takeIf { it.isNotBlank() }, anchor) },
        modifier = Modifier.testTag("learning-observation-save")) { Text(if (saved == null) "Save observation" else "Save correction") }
    Text("Close to skip. Nothing is recorded unless you save.", style = MaterialTheme.typography.bodySmall)
}

@Composable
internal fun LearningHistoryEntry(o: LearningObservation) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(sessionDate(o.observedAt) + " · " + outcomeLabels[learningOutcomes.indexOf(o.outcome)], style = MaterialTheme.typography.bodyMedium)
        Text(contextLabels[learningContexts.indexOf(o.context)], style = MaterialTheme.typography.bodySmall)
        Text(o.criterion ?: "No comparable criterion recorded", style = MaterialTheme.typography.bodySmall)
        o.correctedAt?.let { Text("Corrected " + sessionDate(it), style = MaterialTheme.typography.bodySmall) }
    }
}
