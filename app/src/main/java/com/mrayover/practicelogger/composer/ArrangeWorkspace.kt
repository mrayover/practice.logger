package com.mrayover.practicelogger.composer

import android.graphics.Paint
import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import kotlin.math.*

private class ArrangeTouch {
    var down=Offset.Zero
    var last=Offset.Zero
    var clip: ArrangementClip?=null
    var dragged=false
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable internal fun ArrangeWorkspace(project: CompositionProject, enabled: Boolean, canUndo: Boolean,
    undo: () -> Unit, editLatest: ((CompositionProject) -> CompositionProject) -> Unit,
    ready: Boolean, playing: Boolean, playheadTick: Double?, play: () -> Unit, stop: () -> Unit,
    beginning: () -> Unit, transportMessage: String?, modifier: Modifier=Modifier) {
    var bar by rememberSaveable { mutableLongStateOf(0) }
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    var hand by rememberSaveable { mutableStateOf(false) }
    var panX by rememberSaveable { mutableFloatStateOf(0f) }
    var panY by rememberSaveable { mutableFloatStateOf(0f) }
    var zoom by rememberSaveable { mutableFloatStateOf(1f) }
    var adding by remember { mutableStateOf(false) }
    var actions by remember { mutableStateOf(false) }
    var repeating by remember { mutableStateOf(false) }
    var instrument by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var preview by remember { mutableStateOf<Long?>(null) }
    var viewportHeight by remember { mutableIntStateOf(0) }
    val touch=remember { ArrangeTouch() }
    val density=LocalDensity.current.density
    val context=LocalContext.current
    val labelWidth=156f*density
    val ruler=36f*density
    val laneHeight=52f*density
    val barWidth=88f*density*zoom
    val maxBar=ArrangementEdits.MAX_START_TICK/project.barTicks()
    val lanes=project.tracks
    LaunchedEffect(lanes.size,viewportHeight) {
        panY=panY.coerceIn(0f,maxOf(0f,lanes.size*laneHeight-(viewportHeight-ruler)))
    }
    fun mutate(transform: (CompositionProject) -> CompositionProject) {
        editLatest { current ->
            try { transform(current).also { message=null } }
            catch(e: Exception) { message=e.message; current }
        }
    }
    fun barAt(x: Float) = floor((x-labelWidth+panX)/barWidth).toLong().coerceIn(0,maxBar)
    val paint=remember { Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface=androidx.core.content.res.ResourcesCompat.getFont(context,com.mrayover.practicelogger.R.font.lato_regular)
    } }
    val owner=project.tracks.find { it.id==instrument }
    if(adding && owner!=null) AlertDialog(onDismissRequest={ adding=false },
        title={ Text("${ComposerInstruments.identity(owner.instrument)} · Bar ${bar+1}") },
        text={ Column(Modifier.heightIn(max=280.dp).verticalScroll(rememberScrollState())) {
            val patterns=owner.patterns.filter { it.notes.isNotEmpty() }
            if(patterns.isEmpty()) Text("Create or populate a pattern for this instrument in Loop first.")
            patterns.forEach { p ->
                TextButton(onClick={
                    mutate { ArrangementEdits.add(it,owner.id,p.id,bar*it.barTicks()) }; adding=false
                },enabled=enabled,modifier=Modifier.fillMaxWidth()) { Text("${p.title} · ${p.lengthBars} ${if(p.lengthBars==1) "bar" else "bars"}") }
            }
        } },confirmButton={ TextButton(onClick={ adding=false }) { Text("Cancel") } })
    if(repeating) AlertDialog(onDismissRequest={ repeating=false },title={ Text("Repeat Clip") },text={
        Column(Modifier.heightIn(max=260.dp).verticalScroll(rememberScrollState())) {
            Text("Additional consecutive instances")
            (2..8).forEach { count -> TextButton(enabled=enabled,onClick={
                selected?.let { id -> mutate { ArrangementEdits.repeat(it,id,count) } }; repeating=false
            }) { Text("$count additional") } }
        }
    },confirmButton={ TextButton(onClick={ repeating=false }) { Text("Cancel") } })
    Column(modifier.fillMaxWidth()) {
        ComposerToolbar("arrange-controls",listOf(
            ComposerControl("Play",10) { ComposerButton("Play",enabled && ready && !playing && project.clips.any { ArrangementEdits.pattern(project,it).notes.isNotEmpty() },click=play) },
            ComposerControl("Stop",10) { ComposerButton("Stop",click=stop) },
            ComposerControl("Bar ${(preview?.div(project.barTicks()) ?: bar)+1}",10) { Text("Bar ${(preview?.div(project.barTicks()) ?: bar)+1}",style=MaterialTheme.typography.bodySmall) },
            ComposerControl("Undo",10) { ComposerButton("Undo",enabled && canUndo,click=undo) },
            ComposerControl("Delete Clip",9) { ComposerButton("Delete Clip",enabled && project.clips.any { it.id==selected }) { selected?.let { id -> mutate { ArrangementEdits.delete(it,id) }; selected=null } } },
            ComposerControl(if(hand) "Hand ✓" else "Select ✓",8) { ComposerButton(if(hand) "Hand ✓" else "Select ✓",enabled) { hand=!hand; preview=null; touch.clip=null } },
            ComposerControl("Actions",7) { Box {
                ComposerButton("Actions",enabled) { actions=true }
                DropdownMenu(expanded=actions,onDismissRequest={ actions=false }) {
                    DropdownMenuItem(text={ Text("Duplicate Clip") },enabled=project.clips.any { it.id==selected },onClick={
                        selected?.let { id -> mutate { ArrangementEdits.repeat(it,id,1) } }; actions=false
                    })
                    DropdownMenuItem(text={ Text("Repeat Clip…") },enabled=project.clips.any { it.id==selected },onClick={ repeating=true; actions=false })
                    DropdownMenuItem(text={ Text("Beginning") },onClick={ beginning(); bar=0; panX=0f; actions=false })
                }
            } },
            ComposerControl("Zoom −",1) { ComposerButton("Zoom −",zoom>.5f) { panX=panX*.8f; zoom=(zoom*.8f).coerceAtLeast(.5f) } },
            ComposerControl("Zoom +",1) { ComposerButton("Zoom +",zoom<2f) { panX=panX*1.25f; zoom=(zoom*1.25f).coerceAtMost(2f) } }
        ))
        Text(message ?: transportMessage ?: if(hand) "Hand: drag to scroll." else "Tap an empty lane to place a pattern. Drag clips to move. Hand scrolls.",
            color=if(message!=null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            style=MaterialTheme.typography.bodySmall,modifier=Modifier.padding(horizontal=8.dp,vertical=2.dp))
        Canvas(Modifier.weight(1f).fillMaxWidth().clipToBounds().background(Color(0xFF11151C)).testTag("arrange-timeline").onSizeChanged { viewportHeight=it.height }
            .pointerInteropFilter { e ->
                if(!enabled) return@pointerInteropFilter true
                when(e.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        touch.down=Offset(e.x,e.y); touch.last=touch.down; touch.dragged=false; preview=null
                        val lane=lanes.getOrNull(floor((e.y-ruler+panY)/laneHeight).toInt())
                        touch.clip=if(!hand && e.x>=labelWidth && e.y>=ruler) project.clips.lastOrNull {
                            it.trackId==lane?.id && e.x>=labelWidth+it.startTick.toDouble()/project.barTicks()*barWidth-panX &&
                                e.x<labelWidth+ArrangementEdits.end(project,it).toDouble()/project.barTicks()*barWidth-panX
                        } else null
                        touch.clip?.let { selected=it.id }
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val delta=Offset(e.x,e.y)-touch.down
                        if(delta.getDistance()>8*density) touch.dragged=true
                        if(touch.dragged && hand) {
                            panX=(panX-(e.x-touch.last.x)).coerceIn(0f,maxBar*barWidth)
                            panY=(panY-(e.y-touch.last.y)).coerceIn(0f,maxOf(0f,lanes.size*laneHeight-(viewportHeight-ruler)))
                        } else if(touch.dragged) touch.clip?.let { c ->
                            preview=((c.startTick.toDouble()/project.barTicks()+delta.x/barWidth).roundToLong().coerceIn(0,maxBar))*project.barTicks()
                        }
                        touch.last=Offset(e.x,e.y)
                    }
                    MotionEvent.ACTION_UP -> {
                        val c=touch.clip; val tick=preview
                        if(c!=null && tick!=null && tick!=c.startTick) mutate { ArrangementEdits.move(it,c.id,tick) }
                        else if(!touch.dragged && e.x>=labelWidth) {
                            bar=barAt(e.x)
                            if(c==null) {
                                selected=null
                                if(!hand && e.y>=ruler) lanes.getOrNull(floor((e.y-ruler+panY)/laneHeight).toInt())?.let {
                                    instrument=it.id; adding=true
                                }
                            }
                        }
                        preview=null; touch.clip=null
                    }
                    MotionEvent.ACTION_CANCEL -> { preview=null; touch.clip=null }
                }
                true
            }) {
            fun text(value: String,x: Float,y: Float,color: Int=android.graphics.Color.LTGRAY,sp: Float=13f) {
                paint.color=color; paint.textSize=sp*density
                drawContext.canvas.nativeCanvas.drawText(value,x,y,paint)
            }
            clipRect(left=labelWidth,top=0f) {
                val first=floor(panX/barWidth).toLong()
                val count=ceil((size.width-labelWidth)/barWidth).toInt()+2
                for(b in first..minOf(maxBar,first+count)) {
                    val x=labelWidth+b*barWidth-panX
                    drawLine(Color(0xFF303543),Offset(x,0f),Offset(x,size.height))
                    text("${b+1}",x+6*density,24*density)
                }
                val cursor=labelWidth+bar*barWidth-panX
                drawLine(Color(0xFFE2C6FF),Offset(cursor,0f),Offset(cursor,size.height),2*density)
                lanes.forEachIndexed { index,t ->
                    val y=ruler+index*laneHeight-panY
                    if(y+laneHeight>=ruler && y<size.height) clipRect(top=ruler) {
                        drawLine(Color(0xFF303543),Offset(labelWidth,y+laneHeight),Offset(size.width,y+laneHeight))
                        project.clips.filter { it.trackId==t.id }.forEach { c ->
                            val p=ArrangementEdits.pattern(project,c)
                            val tick=if(c.id==touch.clip?.id) preview ?: c.startTick else c.startTick
                            val x=labelWidth+tick.toDouble().div(project.barTicks()).toFloat()*barWidth-panX
                            val w=p.lengthBars*barWidth
                            if(x+w>=labelWidth && x<size.width) {
                                val invalid=preview!=null && c.id==touch.clip?.id && runCatching { ArrangementEdits.destination(project,c,tick) }.isFailure
                                drawRect(if(invalid) Color(0xFF8E3C50) else Color(0xFF655685),Offset(x+2*density,y+3*density),Size(w-4*density,laneHeight-6*density))
                                if(c.id==selected) drawRect(Color(0xFFF0DFFF),Offset(x+2*density,y+3*density),Size(w-4*density,laneHeight-6*density),style=Stroke(2*density))
                                clipRect(left=maxOf(labelWidth,x+6*density),right=x+w-6*density) {
                                    text(p.title,maxOf(labelWidth,x)+9*density,y+22*density,android.graphics.Color.WHITE)
                                    text("${p.lengthBars} ${if(p.lengthBars==1) "bar" else "bars"}",maxOf(labelWidth,x)+9*density,y+41*density)
                                }
                            }
                        }
                    }
                }
            }
            playheadTick?.let { tick -> clipRect(left=labelWidth) {
                val x=labelWidth+(tick/project.barTicks()*barWidth).toFloat()-panX
                drawLine(Color(0xFF78E4C3),Offset(x,0f),Offset(x,size.height),2*density)
            } }
            drawRect(Color(0xFF202430),size=Size(labelWidth,size.height))
            text("INSTRUMENT",8*density,24*density,sp=11f)
            clipRect(top=ruler,right=labelWidth) {
                lanes.forEachIndexed { index,t ->
                    val y=ruler+index*laneHeight-panY
                    text(ComposerInstruments.identity(t.instrument),8*density,y+22*density,sp=12f)
                    ComposerInstruments.descriptiveName(t)?.let { text(it,8*density,y+41*density,sp=12f) }
                }
            }
            if(lanes.isEmpty()) {
                text("Add instruments in Loop",labelWidth+16*density,ruler+40*density)
                text("Their tracks will appear here.",labelWidth+16*density,ruler+65*density,sp=12f)
            }
            drawLine(Color(0xFF676070),Offset(0f,ruler),Offset(size.width,ruler))
        }
    }
}
