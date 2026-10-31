package com.mrayover.practicelogger

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

@Composable fun PracticeHome(onPractice: () -> Unit, onBlocks: () -> Unit, onRoutines: () -> Unit,
    onTools: () -> Unit, onRecording: () -> Unit, onLog: () -> Unit, goals: List<GoalBundle>, onGoals: () -> Unit, onGoal: (String) -> Unit) {
    Column(Modifier.fillMaxSize().testTag("home-menu").background(PracticeCream)
        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))) {
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().navigationBarsPadding()) {
            val viewportHeight = maxHeight
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).heightIn(min = viewportHeight)
                .padding(horizontal = 24.dp, vertical = 12.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Box(Modifier.fillMaxWidth().heightIn(min = 44.dp)) {
                        Text("Goals", modifier = Modifier.align(Alignment.Center).semantics { heading() },
                            fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold, color = PracticeGreen)
                        IconButton(onClick = onGoals, modifier = Modifier.align(Alignment.CenterEnd)
                            .semantics { contentDescription = "Goal settings" }) {
                            Canvas(Modifier.size(24.dp)) {
                                val radius = size.minDimension / 2f
                                repeat(8) { index ->
                                    val angle = index * Math.PI / 4
                                    val direction = Offset(cos(angle).toFloat(), sin(angle).toFloat())
                                    drawLine(PracticeGreen, center + direction * (radius * 0.62f),
                                        center + direction * (radius * 0.94f), strokeWidth = 3.dp.toPx())
                                }
                                drawCircle(PracticeGreen, radius * 0.62f, style = Stroke(2.dp.toPx()))
                                drawCircle(PracticeGreen, radius * 0.22f, style = Stroke(2.dp.toPx()))
                            }
                        }
                    }
                    Column(Modifier.fillMaxWidth().border(1.dp, PracticeGreen, RoundedCornerShape(6.dp)).testTag("home-goals")) {
                        repeat(2) { index ->
                            if (index > 0) HorizontalDivider(color = PracticeGreen.copy(alpha = 0.35f))
                            val goal = homeGoals(goals).getOrNull(index)?.goal
                            Column(Modifier.fillMaxWidth().clickable(onClickLabel = if (goal == null) "Add a Goal" else "Open ${goal.title}") {
                                if (goal == null) onGoals() else onGoal(goal.id)
                            }.heightIn(min = 48.dp).padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(if (goal == null) "${index + 1}. Add a Goal" else "${index + 1}. ${goal.title} · ${goal.progress}%", color = PracticeGreen, fontSize = 16.sp)
                                if (goal != null) LinearProgressIndicator(
                                    progress = { goal.progress / 100f }, color = PracticeGreen,
                                    trackColor = PracticeGreen.copy(alpha = 0.18f),
                                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Butt, gapSize = 0.dp,
                                    drawStopIndicator = {},
                                    modifier = Modifier.fillMaxWidth().height(3.dp).testTag("home-goal-progress-${goal.id}"))
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onGoals,
                            colors = ButtonDefaults.textButtonColors(disabledContentColor = PracticeGreen)) {
                            Text("Edit", fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        HomeButton("Practice Now", onPractice)
                        HomeButton("Blocks", onBlocks)
                        HomeButton("Routines", onRoutines)
                        HomeButton("Tools", onTools)
                        HomeButton("Recording", onRecording)
                        HomeButton("Log", onLog)
                    }
                }
                Text("Data usage", modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 4.dp),
                    textAlign = TextAlign.Center, color = PracticeGreen, fontSize = 16.sp)
            }
        }
    }
}

@Composable fun HomeButton(label: String, action: () -> Unit) {
    Button(onClick = action, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = PracticeGreen, contentColor = Color.White)) {
        Text(label, modifier = Modifier.fillMaxWidth(), fontFamily = Lato, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp, textAlign = TextAlign.Center)
    }
}

@Composable fun ToolsHome(onHome: () -> Unit, onBackup: () -> Unit, onTool: (String) -> Unit) {
    BackHandler(onBack = onHome)
    Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Tools", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
        HomeButton("Metronome") { onTool("metronome") }
        HomeButton("Tuner") { onTool("tuner") }
        HomeButton("Timer") { onTool("timer") }
        HomeButton("Backup and restore", onBackup)
        HomeButton("Audio Settings") { onTool("audio-settings") }
    }
}

@Composable fun RecordingHome(blocks: List<Block>, onHome: () -> Unit, onBlock: (String) -> Unit, onFree: (Boolean) -> Unit) {
    BackHandler(onBack = onHome)
    Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Recording", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
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

@Composable fun PracticeBanner(onHome: () -> Unit = {}, enabled: Boolean = true) {
    Column {
        Box(Modifier.fillMaxWidth().background(PracticeGreen).statusBarsPadding().clickable(enabled = enabled, role = Role.Button, onClickLabel = "Back to Home", onClick = onHome).heightIn(min = 48.dp).padding(horizontal = 20.dp, vertical = 16.dp)) {
            Text("practice.logger", modifier = Modifier.fillMaxWidth().testTag("home-brand").semantics { heading() },
                fontFamily = Lato, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp,
                textAlign = TextAlign.Center, color = Color.White)
        }
        HorizontalDivider(thickness = 2.dp, color = Color(0xFF244A37))
    }
}
