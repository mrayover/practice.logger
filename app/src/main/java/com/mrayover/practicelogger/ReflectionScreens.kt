package com.mrayover.practicelogger

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

private val blockPrompts = listOf("Focus", "Difficulty", "Progress", "What to revisit")
private val sessionPrompts = listOf("Overall experience", "Focus", "Progress", "Difficulty", "Energy")

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun BlockReflectionScreen(
    placementId: String,
    busy: Boolean,
    onContinue: (BlockReflection?) -> Unit,
    onNavigationCancelled: () -> Unit
) {
    var security by rememberSaveable(placementId) { mutableStateOf<Int?>(null) }
    var expanded by rememberSaveable(placementId) { mutableStateOf(false) }
    var answers by rememberSaveable(placementId) { mutableStateOf(ArrayList(List(blockPrompts.size) { "" })) }
    fun skip() {
        if (busy) return
        onNavigationCancelled()
        onContinue(null)
    }
    BackHandler { skip() }
    ReflectionPage("How secure did that feel?") {
        Text("Optional · 1 = not secure, 5 = very secure")
        FlowRow(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (rating in 1..5) {
                val description = when (rating) {
                    1 -> "Security rating 1, not secure"
                    5 -> "Security rating 5, very secure"
                    else -> "Security rating $rating"
                }
                FilterChip(
                    selected = security == rating, onClick = { security = rating }, enabled = !busy,
                    label = { Text(rating.toString()) },
                    modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        .semantics { contentDescription = description }
                )
            }
        }
        TextButton(onClick = { expanded = !expanded }, enabled = !busy) {
            Text(if (expanded) "Hide optional questions" else "More")
        }
        if (expanded) ReflectionPrompts(blockPrompts, answers, busy) { answers = it }
        val review = BlockReflection(placementId, security, answers[0], answers[1], answers[2], answers[3])
        Button(onClick = { onContinue(review) }, enabled = !busy && review.hasAnswers) { Text("Continue") }
        TextButton(onClick = ::skip, enabled = !busy) { Text("Skip") }
    }
}

@Composable
internal fun SessionReflectionScreen(
    sessionId: String,
    busy: Boolean,
    onFinish: (String, SessionReflection?, String) -> Unit,
    onNavigationCancelled: () -> Unit
) {
    var note by rememberSaveable(sessionId) { mutableStateOf("") }
    var expanded by rememberSaveable(sessionId) { mutableStateOf(false) }
    var answers by rememberSaveable(sessionId) { mutableStateOf(ArrayList(List(sessionPrompts.size) { "" })) }
    fun skip() {
        if (busy) return
        onNavigationCancelled()
        onFinish(sessionId, null, "")
    }
    BackHandler { skip() }
    ReflectionPage("Session Reflection") {
        Text("Your practice is saved. Answer any questions you like.")
        OutlinedTextField(note, { note = it }, enabled = !busy, label = { Text("Session note (optional)") },
            modifier = Modifier.fillMaxWidth())
        TextButton(onClick = { expanded = !expanded }, enabled = !busy) {
            Text(if (expanded) "Hide optional questions" else "Show optional questions")
        }
        if (expanded) ReflectionPrompts(sessionPrompts, answers, busy) { answers = it }
        Button(enabled = !busy, onClick = {
            onFinish(sessionId, SessionReflection(sessionId, answers[0], answers[1], answers[2], answers[3], answers[4]), note)
        }) { Text("Finish Session") }
        TextButton(onClick = ::skip, enabled = !busy) { Text("Skip Reflection") }
    }
}

@Composable
private fun ReflectionPage(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
        content()
    }
}

@Composable
private fun ReflectionPrompts(
    labels: List<String>, answers: List<String>, busy: Boolean, onChange: (ArrayList<String>) -> Unit
) {
    labels.forEachIndexed { index, label ->
        OutlinedTextField(answers[index], { value ->
            if (value.length <= REFLECTION_ANSWER_LIMIT) {
                onChange(ArrayList(answers).apply { this[index] = value })
            }
        }, enabled = !busy, label = { Text(label) }, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
internal fun BlockReflectionHistory(entry: CompletedPlacement) {
    val completion = entry.completion
    val priority = completion.priority
    if (priority != null && completion.colorArgb != null) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(12.dp).background(Color(completion.colorArgb), CircleShape))
            Text("Goal priority at completion · $priority")
        }
    }
    entry.reflection?.let { review ->
        review.security?.let { Text("Security rating · $it / 5") }
        ReflectionAnswers(blockPrompts, listOf(review.focus, review.difficulty, review.progress, review.revisit))
    }
}

@Composable
internal fun SessionReflectionHistory(review: SessionReflection) {
    Text("Session Reflection", style = MaterialTheme.typography.titleLarge)
    ReflectionAnswers(sessionPrompts, listOf(review.experience, review.focus, review.progress, review.difficulty, review.energy))
}

@Composable
private fun ReflectionAnswers(labels: List<String>, answers: List<String>) {
    labels.zip(answers).forEach { (label, answer) ->
        if (answer.isNotBlank()) {
            Column {
                Text(label, style = MaterialTheme.typography.titleSmall)
                Text(answer)
            }
        }
    }
}
