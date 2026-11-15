package com.mrayover.practicelogger.composer

import android.graphics.Paint
import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clipToBounds
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.*

private class RollTouch {
    var down = Offset.Zero
    var last = Offset.Zero
    var original: CompositionPattern? = null
    var note: CompositionNote? = null
    var resize = false
    var moved = false
    var navigation = false
    var keyboard = false
    var keyboardScroll = false
    var ruler = false
    var distance = 0f
    var lastTap: String? = null
    var lastTapTime = 0L
    val voices = mutableMapOf<Long,Int>()
}

@Composable private fun RollChoice(label: String, values: List<String>, selected: Int, choose: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { open = true }) { Text("$label ${values.getOrElse(selected) { "—" }}") }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            values.forEachIndexed { i, value -> DropdownMenuItem(text = { Text(value) }, onClick = { choose(i); open = false }) }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable internal fun PianoRollWorkspace(project: CompositionProject, enabled: Boolean, edit: (CompositionProject) -> Unit, modifier: Modifier = Modifier) {
    var trackId by rememberSaveable { mutableStateOf<String?>(null) }
    var patternSelections by rememberSaveable { mutableStateOf(ArrayList(List(8) { "" })) }
    val track = project.tracks.find { it.id == trackId } ?: project.tracks.firstOrNull()
    val trackIndex = project.tracks.indexOf(track).coerceAtLeast(0)
    val pattern = track?.patterns?.find { it.id == patternSelections[trackIndex] } ?: track?.patterns?.firstOrNull()
    fun choosePattern(id: String) { patternSelections = ArrayList(patternSelections).also { it[trackIndex] = id } }
    val bank = track?.let { ComposerInstruments.index(it.instrument) } ?: 0
    var muted by rememberSaveable { mutableStateOf(BooleanArray(8)) }
    var solo by rememberSaveable { mutableStateOf(BooleanArray(8)) }
    var volumes by rememberSaveable { mutableStateOf(FloatArray(8) { 1f }) }
    var repeat by rememberSaveable { mutableStateOf(true) }
    var playing by remember { mutableStateOf(false) }
    var soundsReady by remember { mutableStateOf(false) }
    var transportFrame by remember { mutableLongStateOf(0) }
    var transportPlan by remember { mutableStateOf<LoopPlan?>(null) }
    var gridIndex by rememberSaveable { mutableIntStateOf(2) }
    var durationIndex by rememberSaveable { mutableIntStateOf(2) }
    var snap by rememberSaveable { mutableStateOf(true) }
    var tool by rememberSaveable { mutableStateOf("Draw") }
    var actionsOpen by remember { mutableStateOf(false) }
    var cursor by rememberSaveable { mutableLongStateOf(0) }
    var zoom by rememberSaveable { mutableFloatStateOf(1f) }
    var panX by rememberSaveable { mutableFloatStateOf(0f) }
    var panY by rememberSaveable { mutableFloatStateOf(0f) }
    var selected by remember { mutableStateOf(setOf<String>()) }
    val hasSelection = pattern?.notes?.any { it.id in selected } == true
    var clipboard by remember { mutableStateOf(emptyList<CompositionNote>()) }
    var preview by remember { mutableStateOf<List<CompositionNote>?>(null) }
    var marquee by remember { mutableStateOf<Pair<Offset,Offset>?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    val touch = remember { RollTouch() }
    var auditionSerial by remember { mutableLongStateOf(100000L) }
    val density = LocalDensity.current.density
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val audio = remember { ComposerAudio(context, interrupted = { message = it }) }
    SideEffect { (context as? ComposerActivity)?.auditionEngine = audio }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(audio,lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) audio.setForeground(true)
            if (event == Lifecycle.Event.ON_PAUSE) { audio.setForeground(false); touch.voices.clear(); preview = null; marquee = null; touch.original = null }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); audio.close() }
    }
    fun audition(pitch: Int) {
        audio.noteOff(auditionSerial)
        val token = ++auditionSerial
        audio.noteOn(token,pitch,bank,trackIndex); scope.launch { delay(180); audio.noteOff(token) }
    }
    fun commitNotes(notes: List<CompositionNote>) {
        if (pattern == null) return
        try { edit(project.withNotes(pattern,notes)); message = null }
        catch (failure: Exception) { message = failure.message }
    }
    LaunchedEffect(pattern?.id) { selected = emptySet(); preview = null; marquee = null; cursor = 0; panX = 0f; touch.original = null; touch.lastTap = null; audio.stopAll() }
    LaunchedEffect(audio) {
        while(true) { soundsReady = audio.ready; playing = audio.playing; transportFrame = audio.positionFrame(); transportPlan = audio.playbackPlan; delay(30) }
    }
    LaunchedEffect(muted,solo,volumes) { audio.mix(BooleanArray(8) { !muted[it] && (!solo.any { value -> value } || solo[it]) },volumes.copyOf()) }
    LaunchedEffect(track?.instrument) { audio.stopAll(); touch.voices.clear() }
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            TextButton(enabled = enabled && soundsReady && !playing && project.tracks.isNotEmpty(),onClick = {
                scope.launch {
                    try { val plan = withContext(Dispatchers.Default) { LoopPlan.snapshot(project,patternSelections.toList()) }; audio.play(plan,repeat); message = null }
                    catch(failure: Exception) { message = failure.message }
                }
            }) { Text("Play") }
            TextButton(onClick = { audio.stop() }) { Text("Stop") }
            TextButton(onClick = { audio.beginning() }) { Text("Beginning") }
            TextButton(onClick = { repeat = !repeat; audio.setLoop(repeat) }) { Text(if(repeat) "Repeat ☑" else "Repeat ☐") }
            val plan = transportPlan
            val shownTick = if(plan == null) 0L else plan.tick(transportFrame).toLong()
            val region = plan?.regionTicks ?: project.tracks.mapIndexed { i,t ->
                t.patterns.find { it.id == patternSelections[i] }?.let(project::patternTicks) ?: t.patterns.firstOrNull()?.let(project::patternTicks) ?: project.barTicks()
            }.maxOrNull() ?: project.barTicks()
            val local = shownTick % region
            val displayBar = plan?.barTicks ?: project.barTicks()
            val displayBeat = plan?.beatTicks ?: (3840L/project.denominator)
            Text("${local/displayBar+1}.${(local%displayBar)/displayBeat+1} · Region ${region/displayBar} bars",style = MaterialTheme.typography.bodySmall,modifier = Modifier.padding(horizontal = 8.dp))
            Text(if(!soundsReady) "Loading sounds…" else if(playing) "Playing ${plan?.tempo} BPM · edits apply next Play" else "Ready",style = MaterialTheme.typography.bodySmall)
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            if (track != null) RollChoice("Instrument",project.tracks.map { it.title },project.tracks.indexOf(track)) { trackId = project.tracks[it].id }
            if (pattern != null && track != null) RollChoice("Pattern",track.patterns.map { it.title },track.patterns.indexOf(pattern)) { choosePattern(track.patterns[it].id) }
            if (track != null && track.patterns.size < 3) TextButton(enabled = enabled,onClick = {
                val title = listOf("A","B","C").first { slot -> track.patterns.none { it.title == slot } }
                val added = CompositionPattern(newCompositionId(),title,4)
                edit(project.copy(tracks = project.tracks.map { if (it.id == track.id) it.copy(patterns = it.patterns+added) else it }))
                choosePattern(added.id)
            }) { Text("+ Pattern") }
            TextButton(enabled = enabled && project.tracks.size < 8, onClick = {
                val id = newCompositionId()
                val patterns = listOf("A","B").map { CompositionPattern(newCompositionId(),it,4) }
                edit(project.copy(tracks = project.tracks + CompositionTrack(id,"Piano ${project.tracks.size+1}",InstrumentReference(1,"reference-piano","default"),patterns)))
                trackId = id; patternSelections = ArrayList(patternSelections).also { it[project.tracks.size] = patterns.first().id }
            }) { Text(if (track == null) "Create piano patterns A/B" else "+ Instrument") }
            if (pattern != null) RollChoice("Bars",listOf("1","2","4","8","16"),listOf(1,2,4,8,16).indexOf(pattern.lengthBars)) { index ->
                val bars = listOf(1,2,4,8,16)[index]
                if (pattern.notes.any { it.startTick + it.durationTick > bars * project.barTicks() }) message = "Length kept: move or delete notes beyond the new end first."
                else { edit(project.replacePattern(pattern.copy(lengthBars = bars))); cursor = cursor.coerceAtMost(bars * project.barTicks()-1) }
            }
            if(track != null) {
                RollChoice("Sound",ComposerInstruments.all.map { it.title },bank) { index ->
                    edit(project.copy(tracks = project.tracks.map { if(it.id == track.id) it.copy(instrument = ComposerInstruments.all[index].reference) else it }))
                }
                TextButton(onClick = { muted = muted.copyOf().also { it[trackIndex] = !it[trackIndex] } }) { Text(if(muted[trackIndex]) "Mute ☑" else "Mute ☐") }
                TextButton(onClick = { solo = solo.copyOf().also { it[trackIndex] = !it[trackIndex] } }) { Text(if(solo[trackIndex]) "Solo ☑" else "Solo ☐") }
                Text("Volume",style = MaterialTheme.typography.bodySmall)
                Slider(value = volumes[trackIndex],onValueChange = { value -> volumes = volumes.copyOf().also { it[trackIndex] = value } },modifier = Modifier.width(100.dp).testTag("composer-volume"))
            }
            Text("Cursor $cursor ticks", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp))
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            RollChoice("Tool:",listOf("Draw","Select","Hand"),listOf("Draw","Select","Hand").indexOf(tool)) { tool = listOf("Draw","Select","Hand")[it] }
            RollChoice("Grid",RollGrid.entries.map { it.label },gridIndex) { gridIndex = it }
            RollChoice("Duration",RollGrid.entries.map { it.label },durationIndex) { durationIndex = it }
            TextButton(onClick = { snap = !snap }) { Text(if (snap) "Snap ☑" else "Snap ☐") }
            TextButton(onClick = { zoom = (zoom / 1.5f).coerceAtLeast(.5f) }) { Text("Zoom −") }
            TextButton(onClick = { zoom = (zoom * 1.5f).coerceAtMost(12f) }) { Text("Zoom +") }
            Box {
            TextButton(onClick = { actionsOpen = true }) { Text("Actions") }
            DropdownMenu(expanded = actionsOpen,onDismissRequest = { actionsOpen = false }) {
            DropdownMenuItem(text = { Text("Select All") },enabled = pattern?.notes?.isNotEmpty() == true,onClick = { selected = pattern!!.notes.map { it.id }.toSet(); actionsOpen = false })
            DropdownMenuItem(text = { Text("Copy") },enabled = hasSelection,onClick = { clipboard = pattern?.notes?.filter { it.id in selected } ?: emptyList(); actionsOpen = false })
            DropdownMenuItem(text = { Text("Paste") },enabled = enabled && clipboard.isNotEmpty() && pattern != null,onClick = {
                val copies = pastedNotes(clipboard,cursor)
                commitNotes(pattern!!.notes + copies); selected = copies.map { it.id }.toSet(); actionsOpen = false
            })
            DropdownMenuItem(text = { Text("Delete Selected") },enabled = enabled && hasSelection,onClick = { commitNotes(pattern!!.notes.filterNot { it.id in selected }); selected = emptySet(); actionsOpen = false })
            DropdownMenuItem(text = { Text("Quantize Selected") },enabled = enabled && hasSelection,onClick = {
                commitNotes(pattern!!.notes.map { n -> if (n.id in selected) n.copy(startTick = boundedTick(n.startTick.toDouble(),0,project.patternTicks(pattern)-n.durationTick,RollGrid.entries[gridIndex],true)) else n })
                actionsOpen = false
            })
            }
            }
        }
        if (pattern == null) { Text("Create a piano instrument to begin drawing notes.",Modifier.padding(16.dp)); return@Column }
        val length = project.patternTicks(pattern)
        val keyWidth = 62f*density
        val rulerHeight = 28f*density
        val rowHeight = 26f*density
        val pixelsPerTick = .11f*density*zoom
        // One viewport transform keeps keys, labels, hit testing and notes aligned for MIDI 0–127.
        fun pitch(y: Float) = (84 - floor((y-rulerHeight+panY)/rowHeight).toInt()).coerceIn(0,127)
        fun tick(x: Float) = boundedTick(((x-keyWidth+panX)/pixelsPerTick).toDouble(),0,length-1,RollGrid.entries[gridIndex],snap)
        fun noteLeft(n: CompositionNote) = keyWidth+n.startTick*pixelsPerTick-panX
        fun noteTop(n: CompositionNote) = rulerHeight+(84-n.pitch)*rowHeight-panY
        val textPaint = remember { Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.LTGRAY; typeface = androidx.core.content.res.ResourcesCompat.getFont(context,com.mrayover.practicelogger.R.font.lato_regular) } }
        Canvas(Modifier.weight(1f).fillMaxWidth().clipToBounds().background(Color(0xFF11151C)).testTag("piano-roll").pointerInteropFilter { event ->
            if (!enabled) return@pointerInteropFilter true
            fun centroid() = Offset((0 until event.pointerCount).sumOf { event.getX(it).toDouble() }.toFloat()/event.pointerCount,(0 until event.pointerCount).sumOf { event.getY(it).toDouble() }.toFloat()/event.pointerCount)
            fun distance() = if (event.pointerCount >= 2) hypot(event.getX(0)-event.getX(1),event.getY(0)-event.getY(1)) else 0f
            fun keys() {
                val active = mutableSetOf<Long>()
                for (i in 0 until event.pointerCount) {
                    if ((event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_POINTER_UP) && i == event.actionIndex) continue
                    val token = event.getPointerId(i).toLong()+1
                    if (event.getX(i) < keyWidth && event.getY(i) >= rulerHeight) {
                        active += token; val p = pitch(event.getY(i))
                        if (touch.voices[token] != p) { audio.noteOff(token); touch.voices[token] = p; audio.noteOn(token,p,bank,trackIndex) }
                    }
                }
                touch.voices.keys.toList().filterNot { it in active }.forEach { audio.noteOff(it); touch.voices.remove(it) }
            }
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    touch.down = Offset(event.x,event.y); touch.last = touch.down; touch.original = pattern; touch.moved = false; touch.navigation = false; touch.keyboardScroll = false
                    touch.keyboard = event.x < keyWidth && event.y >= rulerHeight; touch.ruler = event.y < rulerHeight
                    touch.note = null; preview = null; marquee = null
                    if (touch.keyboard) keys()
                    else if (touch.ruler) cursor = tick(event.x)
                    else if (tool == "Hand") touch.navigation = true
                    else {
                        val hit = pattern.notes.asReversed().find { n -> event.y in noteTop(n)..(noteTop(n)+rowHeight) && event.x >= noteLeft(n) && event.x <= noteLeft(n)+n.durationTick*pixelsPerTick+12*density }
                        touch.note = hit
                        touch.resize = hit != null && event.x > noteLeft(hit)+max(hit.durationTick*pixelsPerTick*.55f,hit.durationTick*pixelsPerTick-22*density)
                        if (hit != null) { if (hit.id !in selected) selected = setOf(hit.id); audition(hit.pitch) }
                        else if (tool == "Draw") selected = emptySet()
                    }
                }
                MotionEvent.ACTION_POINTER_DOWN -> {
                    if (touch.keyboard && !touch.keyboardScroll && (0 until event.pointerCount).all { event.getX(it) < keyWidth }) keys()
                    else { touch.navigation = true; preview = null; marquee = null; audio.stopAll(); touch.voices.clear(); touch.last = centroid(); touch.distance = distance(); touch.lastTap = null }
                }
                MotionEvent.ACTION_MOVE -> {
                    if (touch.navigation) {
                        val center = centroid(); val d = distance()
                        if (event.pointerCount >= 2) {
                            val oldScale = pixelsPerTick
                            val newZoom = (zoom * if (touch.distance > 0) d/touch.distance else 1f).coerceIn(.5f,12f)
                            val anchor = (touch.last.x-keyWidth+panX)/oldScale
                            panX = (anchor*(.11f*density*newZoom) - (center.x-keyWidth)).coerceAtLeast(0f)
                            zoom = newZoom; panY += touch.last.y-center.y
                        } else if (!touch.keyboard) {
                            panX = (panX+touch.last.x-center.x).coerceAtLeast(0f); panY += touch.last.y-center.y
                        }
                        touch.last = center; touch.distance = d
                    } else if (touch.keyboard) {
                        if (!touch.keyboardScroll && abs(event.y-touch.down.y) > android.view.ViewConfiguration.get(context).scaledTouchSlop) {
                            touch.keyboardScroll = true; touch.moved = true
                            touch.voices.keys.toList().forEach(audio::noteOff); touch.voices.clear()
                        }
                        if (touch.keyboardScroll) panY += touch.last.y-event.y
                        touch.last = Offset(event.x,event.y)
                    }
                    else if (touch.ruler) { cursor = tick(event.x); touch.moved = true }
                    else if ((Offset(event.x,event.y)-touch.down).getDistance() > 6*density || touch.moved) {
                        touch.moved = true; touch.lastTap = null
                        val original = touch.original ?: pattern
                        val note = touch.note
                        if (note != null) {
                            if (touch.resize) {
                                val duration = boundedTick(note.durationTick+(event.x-touch.down.x)/pixelsPerTick.toDouble(),1,length-note.startTick,RollGrid.entries[gridIndex],snap,length-note.startTick)
                                preview = original.notes.map { if (it.id == note.id) it.copy(durationTick = duration) else it }
                            } else {
                                val group = original.notes.filter { it.id in selected }
                                val target = boundedTick(note.startTick+(event.x-touch.down.x)/pixelsPerTick.toDouble(),note.startTick-group.minOf { it.startTick },note.startTick+length-group.maxOf { it.startTick+it.durationTick },RollGrid.entries[gridIndex],snap,note.startTick)
                                val dt = target-note.startTick
                                val dp = (pitch(event.y)-pitch(touch.down.y)).coerceIn(-group.minOf { it.pitch },127-group.maxOf { it.pitch })
                                val changedPitch = note.pitch+dp
                                if (preview?.find { it.id == note.id }?.pitch != changedPitch) audition(changedPitch)
                                preview = original.notes.map { if (it.id in selected) it.copy(startTick = it.startTick+dt,pitch = it.pitch+dp) else it }
                            }
                        } else if (tool == "Select") marquee = touch.down to Offset(event.x,event.y)
                    }
                }
                MotionEvent.ACTION_POINTER_UP -> { if (touch.keyboard && !touch.navigation && !touch.keyboardScroll) keys() else { touch.last = centroid(); touch.distance = 0f } }
                MotionEvent.ACTION_UP -> {
                    if (touch.keyboard) { touch.voices.keys.toList().forEach(audio::noteOff); touch.voices.clear() }
                    else if (!touch.navigation && !touch.ruler && touch.original?.id == pattern.id) {
                        val note = touch.note
                        if (touch.moved) {
                            preview?.let(::commitNotes)
                            marquee?.let { (a,b) -> selected = pattern.notes.filter { n ->
                                noteLeft(n)+n.durationTick*pixelsPerTick >= min(a.x,b.x) && noteLeft(n) <= max(a.x,b.x) && noteTop(n)+rowHeight >= min(a.y,b.y) && noteTop(n) <= max(a.y,b.y)
                            }.map { it.id }.toSet() }
                        } else if (note != null) {
                            if (touch.lastTap == note.id && event.eventTime-touch.lastTapTime in 1..300) { commitNotes(pattern.notes.filterNot { it.id == note.id }); selected -= note.id; touch.lastTap = null }
                            else { touch.lastTap = note.id; touch.lastTapTime = event.eventTime }
                        } else if (tool == "Draw") {
                            val start = tick(event.x); val n = CompositionNote(newCompositionId(),pitch(event.y),start,RollGrid.entries[durationIndex].ticks.coerceAtMost(length-start),100)
                            commitNotes(pattern.notes+n); selected = setOf(n.id); audition(n.pitch); touch.lastTap = null
                        } else selected = emptySet()
                    }
                    preview = null; marquee = null; touch.original = null
                }
                MotionEvent.ACTION_CANCEL -> { audio.stopAll(); touch.voices.clear(); preview = null; marquee = null; touch.original = null }
            }
            true
        }) {
            panX = panX.coerceIn(0f,max(0f,length*pixelsPerTick-(size.width-keyWidth)))
            panY = panY.coerceIn(-43*rowHeight,max(-43*rowHeight,85*rowHeight-(size.height-rulerHeight)))
            val firstTick = max(0L,((panX/pixelsPerTick)/RollGrid.entries[gridIndex].ticks).toLong()*RollGrid.entries[gridIndex].ticks)
            val endTick = min(length,((panX+size.width)/pixelsPerTick).toLong())
            val step = RollGrid.entries[gridIndex].ticks
            for (t in firstTick..endTick step step) {
                val x = keyWidth+t*pixelsPerTick-panX
                if (x >= keyWidth) drawLine(if (t%project.barTicks()==0L) Color(0xFF667180) else Color(0xFF2C3542),Offset(x,rulerHeight),Offset(x,size.height),if(t%project.barTicks()==0L) 2f else 1f)
            }
            for (p in 0..127) {
                val y = rulerHeight+(84-p)*rowHeight-panY
                if (y+rowHeight < rulerHeight || y>size.height) continue
                val black = p%12 in listOf(1,3,6,8,10)
                if (black) drawRect(Color(0x221F2D42),Offset(keyWidth,max(y,rulerHeight)),Size(max(0f,size.width-keyWidth),min(y+rowHeight,size.height)-max(y,rulerHeight)))
                drawLine(Color(0xFF26303D),Offset(keyWidth,y),Offset(size.width,y))
                drawRect(if(black) Color(0xFF171C24) else Color(0xFFCCD2DC),Offset(0f,y),Size(keyWidth-2,rowHeight-1))
                textPaint.textSize = 12*density; textPaint.color = if(black) android.graphics.Color.WHITE else android.graphics.Color.BLACK
                val name = if(ComposerInstruments.all.getOrNull(bank)?.drums == true) ComposerInstruments.drumName(p) else listOf("C","C♯","D","D♯","E","F","F♯","G","G♯","A","A♯","B")[p%12]+(p/12-1)
                drawContext.canvas.nativeCanvas.drawText(name,6*density,y+18*density,textPaint)
            }
            (preview ?: pattern.notes).forEach { n ->
                val x = noteLeft(n); val y = noteTop(n)
                if (x+n.durationTick*pixelsPerTick >= keyWidth && x<size.width && y+rowHeight>rulerHeight && y<size.height) {
                    val left = max(x,keyWidth); val width = max(2f,x+n.durationTick*pixelsPerTick-left)
                    drawRect(if(n.id in selected) Color(0xFFFFD475) else Color(0xFF64BBDF),Offset(left,y+2),Size(width,rowHeight-4))
                    if(n.id in selected) drawRect(Color.White,Offset(left,y+2),Size(width,rowHeight-4),style = Stroke(2*density))
                    drawLine(Color(0xFF242C38),Offset(left+width-3*density,y+6),Offset(left+width-3*density,y+rowHeight-6),2*density)
                }
            }
            marquee?.let { (a,b) -> drawRect(Color(0x99EACB77),Offset(min(a.x,b.x),min(a.y,b.y)),Size(abs(a.x-b.x),abs(a.y-b.y)),style = Stroke(2*density)) }
            drawRect(Color(0xFF202936),Offset.Zero,Size(size.width,rulerHeight))
            val beatTicks = 3840L/project.denominator
            for (t in 0L..length step beatTicks) {
                val x = keyWidth+t*pixelsPerTick-panX
                if(x>=keyWidth && x<size.width) {
                    drawLine(Color.Gray,Offset(x,rulerHeight-6*density),Offset(x,rulerHeight))
                    if (t%project.barTicks()==0L || beatTicks*pixelsPerTick>45*density) {
                        textPaint.color = android.graphics.Color.LTGRAY; textPaint.textSize = 11*density
                        drawContext.canvas.nativeCanvas.drawText("${t/project.barTicks()+1}.${(t%project.barTicks())/beatTicks+1}",x+3,17*density,textPaint)
                    }
                }
            }
            if(playing) {
                val ticks = transportPlan?.tick(transportFrame) ?: 0.0
                val x = keyWidth+(ticks%length)*pixelsPerTick-panX
                if(x>=keyWidth && x<=size.width) drawLine(Color(0xFF7BF5B1),Offset(x.toFloat(),0f),Offset(x.toFloat(),size.height),2*density)
            }
            val playX = keyWidth+cursor*pixelsPerTick-panX
            if(playX>=keyWidth && playX<=size.width) drawLine(Color(0xFFFF7777),Offset(playX,0f),Offset(playX,size.height),2*density)
        }
        message?.let { Text(it,style = MaterialTheme.typography.bodySmall,color = MaterialTheme.colorScheme.error) }
    }
}
