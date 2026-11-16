package com.mrayover.practicelogger.composer

import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.input.pointer.RequestDisallowInterceptTouchEvent
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/** Only this channel area scrolls; transport and the master remain pinned. */
@Composable internal fun MixerWorkspace(
    project: CompositionProject, selections: Map<String,String>, muted: Map<String,Boolean>,
    soloed: Map<String,Boolean>, volumes: Map<String,Float>, pans: Map<String,Float>, masterVolume: Float, enabled: Boolean,
    choosePattern: (CompositionTrack,String) -> Unit, mute: (String) -> Unit, solo: (String) -> Unit,
    volume: (String,Float) -> Unit, volumeFinished: (String,Float) -> Unit,
    pan: (String,Float) -> Unit, panFinished: (String,Float) -> Unit,
    master: (Float) -> Unit, masterFinished: (Float) -> Unit, modifier: Modifier = Modifier
) {
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 40.dp) {
    Row(modifier.fillMaxWidth().padding(4.dp),horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.weight(1f).fillMaxHeight().horizontalScroll(rememberScrollState()),horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            project.tracks.forEachIndexed { index,track -> key(track.id) {
                val pattern = track.patterns.find { it.id == selections[track.id] } ?: track.patterns.firstOrNull()
                var menu by remember { mutableStateOf(false) }
                Column(Modifier.width(122.dp).fillMaxHeight().background(Color(0xFF20232D)).padding(4.dp).testTag("mixer-strip-${track.id}"),horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { Text("Track ${index+1}",style = MaterialTheme.typography.labelSmall); Text("EQ later",color=Color(0xFF939099),style=MaterialTheme.typography.labelSmall) }
                    Text(ComposerInstruments.trackLabel(track,index).substringAfter(" · "),style = MaterialTheme.typography.bodySmall.copy(lineHeight = 14.sp),maxLines = 2,overflow = TextOverflow.Ellipsis,modifier = Modifier.height(28.dp))
                    Box {
                        ComposerButton("Pattern ${pattern?.title ?: "None"}",enabled && track.patterns.isNotEmpty(),modifier = Modifier.testTag("mixer-pattern-${track.id}")) { menu = true }
                        DropdownMenu(expanded = menu,onDismissRequest = { menu = false }) {
                            track.patterns.forEach { p -> DropdownMenuItem(text = { Text(p.title) },onClick = { choosePattern(track,p.id); menu = false }) }
                        }
                    }
                    Row {
                        ComposerButton("M",enabled,selected = muted[track.id] == true,modifier = Modifier.weight(1f).testTag("mixer-mute-${track.id}")) { mute(track.id) }
                        ComposerButton("S",enabled,selected = soloed[track.id] == true,modifier = Modifier.weight(1f).testTag("mixer-solo-${track.id}")) { solo(track.id) }
                    }
                    val p = pans[track.id] ?: 0f
                    Row(Modifier.height(40.dp),verticalAlignment=Alignment.CenterVertically) {
                        Text("Pan " + if(kotlin.math.abs(p)<.005f) "C" else (if(p<0) "L" else "R")+kotlin.math.abs(p*100).roundToInt(),style=MaterialTheme.typography.labelSmall,modifier=Modifier.width(58.dp))
                        MixerPan(p,enabled,"Pan ${index+1}",Modifier.weight(1f).testTag("mixer-pan-${track.id}"),
                            change={ pan(track.id,it) },finished={ panFinished(track.id,it) })
                    }
                    MixerFader(volumes[track.id] ?: 1f,enabled,"Volume ${index+1}",Modifier.weight(1f).testTag("mixer-volume-${track.id}"),finished={ volumeFinished(track.id,it) }) { volume(track.id,it) }
                    Text("${((volumes[track.id] ?: 1f)*100).roundToInt()}%",style = MaterialTheme.typography.bodySmall)
                }
            } }
            if(project.tracks.isEmpty()) Text("Add an instrument in Loop.",Modifier.padding(12.dp))
        }
        Column(Modifier.width(82.dp).fillMaxHeight().background(Color(0xFF343044)).border(1.dp,Color(0xFFBFA6E7)).padding(4.dp),horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Master",style = MaterialTheme.typography.titleSmall)
            MixerFader(masterVolume,enabled,"Master Volume",Modifier.weight(1f).testTag("mixer-master"),finished=masterFinished,change=master)
            Text("${(masterVolume*100).roundToInt()}%",style = MaterialTheme.typography.bodySmall)
        }
    }
    }
}

/** A 48dp-wide direct vertical fader with accessible progress and no extra audio owner. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable private fun MixerFader(value: Float, enabled: Boolean, label: String, modifier: Modifier, finished: (Float) -> Unit, change: (Float) -> Unit) {
    val touchInset = with(androidx.compose.ui.platform.LocalDensity.current) { 18.dp.toPx() }
    var height by remember { mutableIntStateOf(1) }
    var gestureValue by remember { mutableFloatStateOf(value) }
    Canvas(modifier.width(48.dp).onSizeChanged { height = it.height }.semantics {
        contentDescription = label
        progressBarRangeInfo = ProgressBarRangeInfo(value,0f..1f)
        if(!enabled) disabled()
        setProgress { requested -> if(enabled) { val v=requested.coerceIn(0f,1f); change(v); finished(v) }; enabled }
    }.pointerInteropFilter { event ->
        if(!enabled) false else {
            when(event.actionMasked) {
                MotionEvent.ACTION_DOWN,MotionEvent.ACTION_MOVE,MotionEvent.ACTION_UP -> {
                    val inset = minOf(touchInset,height/4f)
                    val v=(1f-(event.y-inset)/(height-2*inset).coerceAtLeast(1f)).coerceIn(0f,1f)
                    gestureValue=v
                    change(v)
                    if(event.actionMasked==MotionEvent.ACTION_UP) finished(v)
                }
                MotionEvent.ACTION_CANCEL -> finished(gestureValue)
            }
            true
        }
    }) {
        val inset = minOf(18.dp.toPx(),size.height/4f)
        val y = inset+(1-value)*(size.height-2*inset)
        drawLine(Color(0xFF11131B),Offset(center.x,inset),Offset(center.x,size.height-inset),6.dp.toPx())
        drawLine(Color(0xFFBFA6E7),Offset(center.x,y),Offset(center.x,size.height-inset),4.dp.toPx())
        drawRoundRect(Color(0xFFE3D7F4),topLeft = Offset(center.x-16.dp.toPx(),y-7.dp.toPx()),size = androidx.compose.ui.geometry.Size(32.dp.toPx(),14.dp.toPx()),cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()))
    }
}

/** Horizontal 40dp touch target; accessible changes also commit a single edit. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable private fun MixerPan(value: Float, enabled: Boolean, label: String, modifier: Modifier, change: (Float) -> Unit, finished: (Float) -> Unit) {
    var width by remember { mutableIntStateOf(1) }
    var gestureValue by remember { mutableFloatStateOf(value) }
    val disallowIntercept=remember { RequestDisallowInterceptTouchEvent() }
    Canvas(modifier.height(40.dp).onSizeChanged { width=it.width }.semantics {
        contentDescription=label; progressBarRangeInfo=ProgressBarRangeInfo(value,-1f..1f)
        if(!enabled) disabled()
        setProgress { requested -> if(enabled && requested.isFinite()) { val v=requested.coerceIn(-1f,1f);change(v);finished(v) }; enabled }
    }.pointerInteropFilter(disallowIntercept) { event ->
        if(!enabled) false else {
            if(event.actionMasked==MotionEvent.ACTION_DOWN) disallowIntercept(true)
            when(event.actionMasked) {
                MotionEvent.ACTION_DOWN,MotionEvent.ACTION_MOVE,MotionEvent.ACTION_UP -> {
                    val inset=minOf(12f,width/4f)
                    val raw=(2*(event.x-inset)/(width-2*inset).coerceAtLeast(1f)-1).coerceIn(-1f,1f)
                    val v=if(kotlin.math.abs(raw)<.04f) 0f else raw
                    gestureValue=v;change(v)
                    if(event.actionMasked==MotionEvent.ACTION_UP) finished(v)
                }
                MotionEvent.ACTION_CANCEL -> finished(gestureValue)
            }
            true
        }
    }) {
        val inset=minOf(12f,size.width/4f)
        drawLine(Color(0xFFBFA6E7),Offset(inset,center.y),Offset(size.width-inset,center.y),3.dp.toPx())
        drawLine(Color(0xFF939099),Offset(center.x,center.y-5.dp.toPx()),Offset(center.x,center.y+5.dp.toPx()),1.dp.toPx())
        drawCircle(Color(0xFFE3D7F4),7.dp.toPx(),Offset(inset+(value+1)*.5f*(size.width-2*inset),center.y))
    }
}
