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
            val viewportHeight = maxHeight
            val navigationGap = if (maxHeight >= 700.dp) 10.dp else 8.dp
            val destinations = listOf(
                "Practice Now" to onPractice, "Blocks" to onBlocks, "Routines" to onRoutines,
                "Tools" to onTools, "Recording" to onRecording, "Log" to onLog, "Library" to onLibrary)
            // Intrinsic minimum preserves touch sizes; weight gives spare height to buttons, not empty gaps.
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).testTag("home-scroll")
                .heightIn(min = viewportHeight).height(IntrinsicSize.Min).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Column(Modifier.fillMaxWidth()) {
                    Box(Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Text("Goals", modifier = Modifier.align(Alignment.Center).padding(horizontal = 80.dp).semantics { heading() },
                            fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        TextButton(onClick = onGoals, modifier = Modifier.align(Alignment.CenterEnd)) {
                            Text("Edit", fontWeight = FontWeight.Bold)
                        }
                    }
                    Column(Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)).testTag("home-goals")) {
                        val indicators = LocalGoalIndicators.current
                        repeat(2) { index ->
                            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            val goal = homeGoals(goals).getOrNull(index)?.goal
                            val marker = if (goal == null) Modifier else Modifier.goalEndCap(indicators.goal(goal.id))
                            Column(Modifier.fillMaxWidth().then(marker).clickable(onClickLabel = if (goal == null) "Add a Goal" else "Open ${goal.title}") {
                                if (goal == null) onGoals() else onGoal(goal.id)
                            }.heightIn(min = 56.dp).padding(start = 12.dp, end = 24.dp, top = 8.dp, bottom = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(if (goal == null) "${index + 1}. Add a Goal" else "${index + 1}. ${goal.title}",
                                    color = MaterialTheme.colorScheme.primary, fontSize = 16.sp, lineHeight = 20.sp,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.testTag("home-goal-title-$index"))
                                if (goal != null) {
                                    Text("${goal.progress}% · Manual progress", style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(navigationGap)) {
                    destinations.forEachIndexed { index, (label, action) ->
                        HomeNavigationButton(label, action, prominent = index == 0, modifier = Modifier.weight(1f))
                    }
                }
                HomeWeeklySummary(weeklyProgress, onStatistics, onWeekTarget)
            }
        }
    }
}

/** Home spacing and colors stay local; the other menus retain their established controls. */
@Composable private fun HomeNavigationButton(label: String, action: () -> Unit, prominent: Boolean, modifier: Modifier) {
    val secondary = PracticeTheme.colors.homeSecondaryAction
    Button(onClick = action, modifier = modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("home-nav-$label"),
        shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = if (prominent || secondary == Color.Unspecified)
            MaterialTheme.colorScheme.primary else secondary, contentColor = MaterialTheme.colorScheme.onPrimary)) {
        Text(label, modifier = Modifier.fillMaxWidth(), fontFamily = Lato, fontWeight = FontWeight.Bold,
            fontSize = 20.sp, lineHeight = 24.sp, textAlign = TextAlign.Center)
    }
}

/** Unclipped, square-edged footer; richer details remain behind the existing actions. */
@OptIn(ExperimentalLayoutApi::class)
@Composable private fun HomeWeeklySummary(progress: WeeklyTargetProgress?, onStatistics: () -> Unit,
    onWeekTarget: () -> Unit) {
    Column(Modifier.fillMaxWidth().testTag("weekly-target-summary"), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("This Week", style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f).clickable(role = Role.Button, onClickLabel = "Open practice statistics",
                    onClick = onStatistics).heightIn(min = 48.dp).wrapContentHeight(Alignment.CenterVertically)
                    .testTag("home-statistics"))
            TextButton(onClick = onWeekTarget, modifier = Modifier.testTag("home-weekly-target"),
                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp)) {
                Text(if (progress?.targetMinutes == null) "Set weekly target" else "Edit weekly target",
                    style = MaterialTheme.typography.bodySmall)
            }
        }
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(if (progress == null) "Loading…"
                else if (progress.targetMinutes == null) "${targetMinuteText(progress.actualMs)} min practiced"
                else "${targetMinuteText(progress.actualMs)} / ${progress.targetMinutes} min",
                style = MaterialTheme.typography.bodySmall)
            if (progress?.targetMinutes != null) Text(progress.percentage.orEmpty(),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (progress?.targetMinutes != null) LinearProgressIndicator(progress = { progress.fill },
            modifier = Modifier.fillMaxWidth().height(4.dp), color = MaterialTheme.colorScheme.primary,
            trackColor = PracticeTheme.colors.scrollTrack,
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Butt, gapSize = 0.dp, drawStopIndicator = {})
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
        val context = androidx.compose.ui.platform.LocalContext.current
        HomeButton("compose.logger") {
            context.startActivity(android.content.Intent(context, com.mrayover.practicelogger.composer.ComposerLibraryActivity::class.java))
        }
        HomeButton("Timer") { onTool("timer") }
        HomeButton("Backup and restore", onBackup)
        HomeButton("Audio Settings") { onTool("audio-settings") }
    }
}

@Composable fun RecordingHome(blocks: List<Block>, onHome: () -> Unit, onBlock: (String) -> Unit, onFree: (Boolean) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf<String?>(null) }
    var source by rememberSaveable { mutableStateOf<String?>(null) }
    val org = LocalOrganization.current
    BackHandler(onBack = onHome)
    Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        PageHeading("Recording", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
        HomeButton("Free recording") { onFree(true) }
        TextButton(onClick = { onFree(false) }) { Text("Saved free recordings") }
        Text("Choose a Block")
        OutlinedTextField(query, { query = it }, label = { Text("Find Block or Source") }, singleLine = true,
            modifier = Modifier.fillMaxWidth())
        OrganizationFilterPicker(category, source, onCategory = { category = it }, onSource = { source = it })
        if (blocks.isEmpty()) Text("Your Blocks will appear here.")
        blocks.filter { org.matches(it, query) && (category == null || it.categoryId == category) &&
            (source == null || it.sourceId == source) }.forEach { block ->
            OutlinedButton(onClick = { onBlock(block.id) }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                Column(Modifier.fillMaxWidth()) {
                    Text(block.name, style = MaterialTheme.typography.titleMedium)
                    if (block.archived) Text("Archived", style = MaterialTheme.typography.bodySmall)
                    BlockOrganizationLabel(block)
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
