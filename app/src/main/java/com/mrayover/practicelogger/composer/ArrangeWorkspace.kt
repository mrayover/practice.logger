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
    undo: () -> Unit, editLatest: ((CompositionProject) -> CompositionProject) -> Unit, modifier: Modifier=Modifier) {
    var bar by rememberSaveable { mutableLongStateOf(0) }
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    var hand by rememberSaveable { mutableStateOf(false) }
    var panX by rememberSaveable { mutableFloatStateOf(0f) }
    var panY by rememberSaveable { mutableFloatStateOf(0f) }
    var zoom by rememberSaveable { mutableFloatStateOf(1f) }
    var adding by remember { mutableStateOf(false) }
    var instrument by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var preview by remember { mutableStateOf<Long?>(null) }
    var viewportHeight by remember { mutableIntStateOf(0) }
    val touch=remember { ArrangeTouch() }
    val density=LocalDensity.current.density
    val context=LocalContext.current
    val labelWidth=156f*density
    val ruler=36f*density
    val laneHeight=72f*density
    val barWidth=88f*density*zoom
    val maxBar=ArrangementEdits.MAX_START_TICK/project.barTicks()
    val lanes=project.tracks.filter { t -> project.clips.any { it.trackId==t.id } }
    val choices=project.tracks.filter { t -> t.patterns.any { it.notes.isNotEmpty() } }
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
    if(adding) AlertDialog(onDismissRequest={ adding=false },title={ Text(if(instrument==null) "Add Pattern · Bar ${bar+1}" else "Choose pattern · Bar ${bar+1}") },
        text={ Column(Modifier.heightIn(max=280.dp).verticalScroll(rememberScrollState())) {
            if(choices.isEmpty()) Text("Create notes in Loop, then add that pattern here.")
            if(instrument==null) choices.forEach { t ->
                TextButton(onClick={ instrument=t.id }) { Text(ComposerInstruments.compactTrackLabel(t,project.tracks.indexOf(t))) }
            } else {
                TextButton(onClick={ instrument=null }) { Text("All instruments") }
                choices.find { it.id==instrument }?.let { t ->
                    t.patterns.filter { it.notes.isNotEmpty() }.forEach { p ->
                        TextButton(onClick={
                            mutate { ArrangementEdits.add(it,t.id,p.id,bar*it.barTicks()) }
                            adding=false
                        },modifier=Modifier.fillMaxWidth()) { Text("${p.title} · ${p.lengthBars} bars") }
                    }
                }
            }
        } }, confirmButton={ TextButton(onClick={ adding=false }) { Text("Cancel") } })
    Column(modifier.fillMaxWidth()) {
        ComposerToolbar("arrange-controls",listOf(
            ComposerControl("Add Pattern",10) { ComposerButton("Add Pattern",enabled) { instrument=null; adding=true } },
            ComposerControl("Bar ${(preview?.div(project.barTicks()) ?: bar)+1}",10) { Text("Bar ${(preview?.div(project.barTicks()) ?: bar)+1}",style=MaterialTheme.typography.bodySmall) },
            ComposerControl("Undo",10) { ComposerButton("Undo",enabled && canUndo,click=undo) },
            ComposerControl("Delete Clip",9) { ComposerButton("Delete Clip",enabled && project.clips.any { it.id==selected }) { selected?.let { id -> mutate { ArrangementEdits.delete(it,id) }; selected=null } } },
            ComposerControl(if(hand) "Hand ✓" else "Select ✓",8) { ComposerButton(if(hand) "Hand ✓" else "Select ✓",enabled) { hand=!hand; preview=null; touch.clip=null } },
            ComposerControl("Zoom −",1) { ComposerButton("Zoom −",zoom>.5f) { panX=panX*.8f; zoom=(zoom*.8f).coerceAtLeast(.5f) } },
            ComposerControl("Zoom +",1) { ComposerButton("Zoom +",zoom<2f) { panX=panX*1.25f; zoom=(zoom*1.25f).coerceAtMost(2f) } }
        ))
        Text(message ?: if(hand) "Hand: drag to scroll. Tap to choose a bar. Playback arrives in C5B." else "Select: tap a bar or drag a clip. Use Hand to scroll.",
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
                        else if(!touch.dragged && e.x>=labelWidth) { bar=barAt(e.x); if(c==null) selected=null }
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
                                drawRect(if(invalid) Color(0xFF8E3C50) else Color(0xFF655685),Offset(x+2*density,y+6*density),Size(w-4*density,laneHeight-12*density))
                                if(c.id==selected) drawRect(Color(0xFFF0DFFF),Offset(x+2*density,y+6*density),Size(w-4*density,laneHeight-12*density),style=Stroke(2*density))
                                clipRect(left=maxOf(labelWidth,x+6*density),right=x+w-6*density) {
                                    text(p.title,maxOf(labelWidth,x)+9*density,y+29*density,android.graphics.Color.WHITE)
                                    text("${p.lengthBars} bars",maxOf(labelWidth,x)+9*density,y+49*density)
                                }
                            }
                        }
                    }
                }
            }
            drawRect(Color(0xFF202430),size=Size(labelWidth,size.height))
            text("INSTRUMENT",8*density,24*density,sp=11f)
            clipRect(top=ruler,right=labelWidth) {
                lanes.forEachIndexed { index,t ->
                    val y=ruler+index*laneHeight-panY
                    val label=ComposerInstruments.compactTrackLabel(t,project.tracks.indexOf(t))
                    val lines=label.split(" · ",limit=2)
                    text(lines.getOrElse(1) { label }.take(22),8*density,y+28*density,sp=12f)
                    text(t.title.take(22),8*density,y+48*density,sp=12f)
                }
            }
            if(lanes.isEmpty()) {
                text("Your arrangement starts here",labelWidth+16*density,ruler+40*density)
                text("Choose a bar, then Add Pattern",labelWidth+16*density,ruler+65*density,sp=12f)
            }
            drawLine(Color(0xFF676070),Offset(0f,ruler),Offset(size.width,ruler))
        }
    }
}
