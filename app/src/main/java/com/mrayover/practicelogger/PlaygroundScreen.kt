package com.mrayover.practicelogger

import android.app.Application
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.*

internal data class PlaygroundContent(val sessions: List<SessionBundle>, val layouts: List<PlaygroundLayout>, val notes: List<PracticeNote> = emptyList()) {
    val completions get() = sessions.flatMap { it.completions }.associateBy { it.completion.placementId }
}
class PlaygroundViewModel(app: Application) : AndroidViewModel(app) {
    private val db = BlockDatabase.appInstance(app)
    private val repository = PlaygroundRepository(db)
    internal var content by mutableStateOf<PlaygroundContent?>(null); private set
    var error by mutableStateOf<String?>(null); private set
    var saving by mutableStateOf(false); private set
    init {
        viewModelScope.launch {
            combine(db.sessions().observeAll(), db.playground().observeAll()) { sessions, _ -> sessions }.collect { sessions ->
                try {
                    val layouts = withContext(Dispatchers.IO) { repository.ensureLayouts() }
                    content = PlaygroundContent(sessions, layouts, db.practiceNotes().readAll()); error = null
                } catch (e: CancellationException) { throw e }
                catch (_: Exception) { error = "Your arrangement could not be loaded. Retry to find clear space; saved positions are retained." }
            }
        }
    }
    fun retry() { viewModelScope.launch {
        try { content = PlaygroundContent(db.sessions().readAll(), withContext(Dispatchers.IO) { repository.ensureLayouts() }, db.practiceNotes().readAll()); error = null }
        catch (e: CancellationException) { throw e }
        catch (_: Exception) { error = "Playground could not be loaded. Please retry." }
    } }
    internal fun save(row: PlaygroundLayout, done: () -> Unit) {
        if (saving) return
        saving = true
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { repository.save(row) }
                content = content?.let { it.copy(layouts = it.layouts.map { old -> if (old.placementId == row.placementId) row else old }) }
                error = null
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { error = "That move could not be saved. The last saved arrangement is retained; please retry." }
            finally { saving = false; done() }
        }
    }
}

@Composable internal fun PlaygroundScreen(onJournal: () -> Unit, vm: PlaygroundViewModel = viewModel()) {
    val content = vm.content
    var selected by remember { mutableStateOf<String?>(null) }
    var expanded by remember { mutableStateOf(false) }
    var preview by remember { mutableStateOf<PlaygroundLayout?>(null) }
    var camera by remember { mutableStateOf<PlaygroundCamera?>(null) }
    val layouts = content?.layouts.orEmpty().map { if (it.placementId == preview?.placementId) preview!! else it }
    val completions = remember(content?.sessions) { content?.completions.orEmpty() }
    val world = playgroundWorld(completions.size)
    val currentLayouts by rememberUpdatedState(layouts)
    val currentWorld by rememberUpdatedState(world)
    val currentSelected by rememberUpdatedState(selected)
    val canGesture by rememberUpdatedState(!vm.saving && vm.error == null)
    val outline = MaterialTheme.colorScheme.outlineVariant
    val neutral = MaterialTheme.colorScheme.surfaceVariant
    val glow = MaterialTheme.colorScheme.primary
    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            DestinationPills("Journal", "Playground", true) { if (!it) onJournal() }
            Text("Playground", style = MaterialTheme.typography.titleLarge)
        }
        vm.error?.let { message -> Row(Modifier.padding(horizontal = 16.dp)) {
            Text(message, Modifier.weight(1f)); TextButton(onClick = vm::retry) { Text("Retry") }
        } }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            Canvas(Modifier.fillMaxSize().clipToBounds().testTag("playground-canvas").pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val cam = camera ?: return@awaitEachGesture
                    if (!canGesture) return@awaitEachGesture
                    fun Offset.point() = WorldPoint(x.toDouble(), y.toDouble())
                    val hit = currentLayouts.lastOrNull { playgroundContains(it, cam.world(down.position.point())) }
                    val moving = hit?.takeIf { it.placementId == currentSelected }
                    var candidate = moving
                    var multi = false; var moved = false; var cancelled = true
                    var travel = 0f
                    try {
                        do {
                            val event = awaitPointerEvent()
                            if (event.changes.any { it.isConsumed }) break
                            val pressed = event.changes.count { it.pressed }
                            multi = multi || pressed >= 2
                            val pan = event.calculatePan()
                            travel += pan.getDistance()
                            if (travel > viewConfiguration.touchSlop || multi) moved = true
                            if (moved) {
                                val c = camera ?: cam
                                val obstacles = currentLayouts.filter { it.placementId != moving?.placementId }
                                if (moving != null) {
                                    if (multi) {
                                        if (pressed >= 2) candidate = rotatePlaygroundSquare(candidate!!, event.calculateRotation().toDouble(), obstacles, currentWorld)
                                    } else candidate = movePlaygroundSquare(candidate!!, pan.point() * (1 / c.scale), obstacles, currentWorld)
                                    preview = candidate
                                } else if (hit == null) {
                                    val minimum = min(size.width / currentWorld.width, size.height / currentWorld.height) * 0.9
                                    if (pressed >= 2) {
                                        val focus = event.calculateCentroid(useCurrent = false)
                                        if (focus != Offset.Unspecified) camera = c.zoom(focus.point(), event.calculateZoom().toDouble(), minimum, max(160.0, minimum * 12)).let {
                                            it.copy(offset = it.offset + pan.point())
                                        }
                                    } else camera = c.copy(offset = c.offset + pan.point())
                                }
                                event.changes.forEach { it.consume() }
                            }
                            if (pressed == 0) { cancelled = false; break }
                        } while (true)
                        if (!cancelled) {
                            if (!moved) { selected = hit?.placementId; expanded = false }
                            else if (candidate != null && candidate != moving) vm.save(candidate!!) { preview = null }
                        }
                    } finally { if (cancelled || !vm.saving) preview = null }
                }
            }) {
                val overview = min(size.width / world.width, size.height / world.height) * 0.9
                val c = camera ?: PlaygroundCamera(overview, WorldPoint(size.width / 2.0, size.height / 2.0)).also { camera = it }
                withTransform({ translate(c.offset.x.toFloat(), c.offset.y.toFloat()); scale(c.scale.toFloat(), c.scale.toFloat(), Offset.Zero) }) {
                    drawRect(outline, Offset((-world.width / 2).toFloat(), (-world.height / 2).toFloat()),
                        Size(world.width.toFloat(), world.height.toFloat()), style = Stroke((1 / c.scale).toFloat()))
                    layouts.forEach { row ->
                        val center = Offset(row.x.toFloat(), row.y.toFloat())
                        rotate(row.rotationDegrees.toFloat(), center) {
                            if (row.placementId == selected) {
                                for (i in 3 downTo 1) drawRect(glow.copy(alpha = 0.08f), center - Offset(0.5f + i * 0.025f, 0.5f + i * 0.025f), Size(1f + i * 0.05f, 1f + i * 0.05f))
                            }
                            val color = completions[row.placementId]?.completion?.colorArgb?.let { Color(it) } ?: neutral
                            drawRect(color, center - Offset(0.5f, 0.5f), Size(1f, 1f))
                        }
                    }
                }
            }
            if (content != null && layouts.isEmpty()) Text("Completed practice will earn squares here.", Modifier.padding(24.dp))
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(if (selected == null) "Tap a square · Drag space to pan" else "Drag selected square · Twist to rotate", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            TextButton(onClick = { camera = null }, modifier = Modifier.testTag("playground-overview")) { Text("Overview") }
        }
        val earned = completions[selected]
        val placement = content?.sessions?.flatMap { it.placements }?.find { it.id == selected }
        if (earned != null && placement != null) {
            Surface(tonalElevation = 3.dp, modifier = Modifier.fillMaxWidth().heightIn(max = if (expanded) 300.dp else 150.dp).testTag("playground-details")) {
                Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(placement.name, style = MaterialTheme.typography.titleMedium)
                    val completion = earned.completion
                    val date = Instant.ofEpochMilli(completion.completedAt).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("MMM d, yyyy · HH:mm"))
                    Text("$date · ${placement.activeMs / 60000} min ${placement.activeMs / 1000 % 60} sec")
                    Text(completion.goalId?.let { "Goal $it" + (completion.priority?.let { p -> " · Priority $p at completion" } ?: " · Historical priority unavailable") }
                        ?: "No recorded Goal")
                    TextButton(onClick = { expanded = !expanded }, modifier = Modifier.testTag("playground-expand")) { Text(if (expanded) "Less" else "More") }
                    if (expanded) {
                        Text(placement.action)
                        Text("Completed · Session ${completion.sessionId}")
                        if (placement.referenceNotes.isNotBlank()) Text(placement.referenceNotes)
                        if (placement.note.isNotBlank()) Text(placement.note)
                        content.notes.filter { it.placementId == selected }.forEach { Text(it.text) }
                        earned.reflection?.let { r ->
                            r.security?.let { Text("Security / confidence: $it / 5") }
                            listOf("Focus" to r.focus, "Difficulty" to r.difficulty, "Progress" to r.progress, "Revisit" to r.revisit).filter { it.second.isNotBlank() }.forEach { Text("${it.first}: ${it.second}") }
                        }
                    }
                }
            }
        }
    }
}
