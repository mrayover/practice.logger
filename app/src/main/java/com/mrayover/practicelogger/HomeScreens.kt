package com.mrayover.practicelogger

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable fun PracticeHome(onPractice: () -> Unit, onBlocks: () -> Unit, onRoutines: () -> Unit,
    onTools: () -> Unit, onRecording: () -> Unit, onLog: () -> Unit, onLibrary: () -> Unit, goals: List<GoalBundle>, onGoals: () -> Unit, onGoal: (String) -> Unit, practiceMs: Long? = null, onStatistics: () -> Unit = {}, weeklyProgress: WeeklyTargetProgress? = null, onWeekTarget: () -> Unit = {}) {
    Column(Modifier.fillMaxSize().testTag("home-menu").background(pageBackgroundColor())
        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))) {
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().navigationBarsPadding()) {
            // Keep the familiar vertical menu when it fits; shorter phones use paired rows.
            val enlargedText = LocalDensity.current.fontScale >= 1.5f
            val paired = maxHeight < 700.dp && !enlargedText
            val destinations = listOf(
                "Practice Now" to onPractice, "Blocks" to onBlocks, "Routines" to onRoutines,
                "Tools" to onTools, "Recording" to onRecording, "Log" to onLog, "Library" to onLibrary)
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).testTag("home-scroll")
                .padding(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text("Goals", modifier = Modifier.align(Alignment.Center).semantics { heading() },
                        fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    TextButton(onClick = onGoals, modifier = Modifier.align(Alignment.CenterEnd)) {
                        Text("Edit", fontWeight = FontWeight.Bold)
                    }
                }
                Column(Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp)).testTag("home-goals")) {
                    val indicators = LocalGoalIndicators.current
                    repeat(2) { index ->
                        if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        val goal = homeGoals(goals).getOrNull(index)?.goal
                        val marker = if (goal == null) Modifier else Modifier.goalEndCap(indicators.goal(goal.id))
                        Column(Modifier.fillMaxWidth().then(marker).clickable(onClickLabel = if (goal == null) "Add a Goal" else "Open ${goal.title}") {
                            if (goal == null) onGoals() else onGoal(goal.id)
                        }.heightIn(min = 48.dp).padding(horizontal = 12.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(if (goal == null) "${index + 1}. Add a Goal" else "${index + 1}. ${goal.title} · ${goal.progress}% manual",
                                color = MaterialTheme.colorScheme.primary, fontSize = 16.sp,
                                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.testTag("home-goal-title-$index"))
                            if (goal != null) LinearProgressIndicator(
                                progress = { goal.progress / 100f }, color = MaterialTheme.colorScheme.primary,
                                trackColor = PracticeTheme.colors.scrollTrack,
                                strokeCap = androidx.compose.ui.graphics.StrokeCap.Butt, gapSize = 0.dp,
                                drawStopIndicator = {},
                                modifier = Modifier.fillMaxWidth().height(3.dp).testTag("home-goal-progress-${goal.id}"))
                        }
                    }
                }
                if (paired) {
                    CompactHomeButton(destinations.first().first, destinations.first().second)
                    destinations.drop(1).chunked(2).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { (label, action) -> CompactHomeButton(label, action, Modifier.weight(1f)) }
                        }
                    }
                } else destinations.forEach { (label, action) -> CompactHomeButton(label, action) }
                if (enlargedText) {
                    TextButton(onClick = onStatistics, modifier = Modifier.fillMaxWidth().testTag("home-statistics")) {
                        Text("Total practice · ${practiceMs?.let(::practiceDuration) ?: "Loading…"}")
                    }
                    TextButton(onClick = onWeekTarget, modifier = Modifier.fillMaxWidth().testTag("home-weekly-target")) {
                        Text(if (weeklyProgress?.targetMinutes == null) "Set weekly target" else "Edit weekly target")
                    }
                } else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onStatistics, modifier = Modifier.weight(1f).testTag("home-statistics"),
                        contentPadding = PaddingValues(horizontal = 4.dp)) {
                        Text("Total practice · ${practiceMs?.let(::practiceDuration) ?: "Loading…"}", maxLines = 2)
                    }
                    TextButton(onClick = onWeekTarget, modifier = Modifier.weight(1f).testTag("home-weekly-target"),
                        contentPadding = PaddingValues(horizontal = 4.dp)) {
                        Text(if (weeklyProgress?.targetMinutes == null) "Set weekly target" else "Edit weekly target", maxLines = 2)
                    }
                }
                CompactHomeWeeklySummary(weeklyProgress)
            }
        }
    }
}

/** Home alone uses compact controls; Tools and other menus retain their established sizes. */
@Composable private fun CompactHomeButton(label: String, action: () -> Unit, modifier: Modifier = Modifier) {
    Button(onClick = action, modifier = modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("home-nav-$label"),
        shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
        Text(label, fontFamily = Lato, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 24.sp,
            textAlign = TextAlign.Center)
    }
}

@Composable private fun CompactHomeWeeklySummary(progress: WeeklyTargetProgress?) {
    Column(Modifier.fillMaxWidth().testTag("weekly-target-summary"), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(if (progress == null) "This Week · Loading…"
            else if (progress.targetMinutes == null) "This Week · ${targetMinuteText(progress.actualMs)} min practiced"
            else "This Week · ${targetMinuteText(progress.actualMs)} / ${progress.targetMinutes} min · ${progress.percentage}",
            style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (progress?.targetMinutes != null) LinearProgressIndicator(progress = { progress.fill },
            modifier = Modifier.fillMaxWidth().height(4.dp), color = MaterialTheme.colorScheme.primary,
            trackColor = PracticeTheme.colors.scrollTrack)
    }
}

@Composable fun HomeButton(label: String, action: () -> Unit) {
    HomeButton(label, ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary), action = action)
}

@Composable fun HomeButton(label: String, colors: ButtonColors, modifier: Modifier = Modifier, action: () -> Unit) {
    Button(onClick = action, modifier = modifier.fillMaxWidth().heightIn(min = 56.dp),
        shape = RoundedCornerShape(8.dp), colors = colors) {
        Text(label, modifier = Modifier.fillMaxWidth(), fontFamily = Lato, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp, textAlign = TextAlign.Center)
    }
}

@Composable fun ToolsHome(onHome: () -> Unit, onBackup: () -> Unit, onTool: (String) -> Unit) {
    BackHandler(onBack = onHome)
    Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        PageHeading("Tools", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
        HomeButton("Metronome") { onTool("metronome") }
        HomeButton("Tuner") { onTool("tuner") }
        HomeButton("Reference Piano") { onTool("piano") }
        HomeButton("Timer") { onTool("timer") }
        HomeButton("Backup and restore", onBackup)
        HomeButton("Audio Settings") { onTool("audio-settings") }
    }
}

@Composable fun RecordingHome(blocks: List<Block>, onHome: () -> Unit, onBlock: (String) -> Unit, onFree: (Boolean) -> Unit) {
    BackHandler(onBack = onHome)
    Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        PageHeading("Recording", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
        HomeButton("Free recording") { onFree(true) }
        TextButton(onClick = { onFree(false) }) { Text("Saved free recordings") }
        Text("Choose a Block")
        if (blocks.isEmpty()) Text("Your Blocks will appear here.")
        blocks.forEach { block ->
            OutlinedButton(onClick = { onBlock(block.id) }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                Column(Modifier.fillMaxWidth()) {
                    Text(block.name, style = MaterialTheme.typography.titleMedium)
                    if (block.archived) Text("Archived", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable fun PracticeBanner(onHome: () -> Unit = {}, enabled: Boolean = true, onSettings: (() -> Unit)? = null) {
    Column {
        Row(Modifier.fillMaxWidth().background(PracticeTheme.colors.banner).statusBarsPadding(), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.width(48.dp))
            Box(Modifier.weight(1f).clickable(enabled = enabled, role = Role.Button, onClickLabel = "Back to Home", onClick = onHome)
                .heightIn(min = 48.dp).padding(vertical = 16.dp)) {
                Text("practice.logger", modifier = Modifier.fillMaxWidth().testTag("home-brand").semantics { heading() },
                    fontFamily = Lato, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp,
                    textAlign = TextAlign.Center, color = PracticeTheme.colors.onBanner)
            }
            IconButton(onClick = { onSettings?.invoke() }, enabled = enabled && onSettings != null,
                modifier = Modifier.size(48.dp).testTag("header-settings").semantics { contentDescription = "Settings" }) {
                Text("⚙", color = PracticeTheme.colors.onBanner, fontSize = 26.sp)
            }
        }
        HorizontalDivider(thickness = 2.dp, color = PracticeTheme.colors.bannerEdge)
    }
}
