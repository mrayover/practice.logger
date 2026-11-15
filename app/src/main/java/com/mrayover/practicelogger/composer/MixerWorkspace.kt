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
    soloed: Map<String,Boolean>, volumes: Map<String,Float>, masterVolume: Float, enabled: Boolean,
    choosePattern: (CompositionTrack,String) -> Unit, mute: (String) -> Unit, solo: (String) -> Unit,
    volume: (String,Float) -> Unit, master: (Float) -> Unit, modifier: Modifier = Modifier
) {
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 40.dp) {
    Row(modifier.fillMaxWidth().padding(4.dp),horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.weight(1f).fillMaxHeight().horizontalScroll(rememberScrollState()),horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            project.tracks.forEachIndexed { index,track -> key(track.id) {
                val pattern = track.patterns.find { it.id == selections[track.id] } ?: track.patterns.firstOrNull()
                var menu by remember { mutableStateOf(false) }
                Column(Modifier.width(122.dp).fillMaxHeight().background(Color(0xFF20232D)).padding(4.dp).testTag("mixer-strip-${track.id}"),horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Track ${index+1}",style = MaterialTheme.typography.labelSmall)
                    Text(ComposerInstruments.trackLabel(track,index).substringAfter(" · "),style = MaterialTheme.typography.bodySmall.copy(lineHeight = 14.sp),maxLines = 2,overflow = TextOverflow.Ellipsis,modifier = Modifier.height(28.dp))
                    Box {
                        ComposerButton("Pattern ${pattern?.title ?: "None"}",enabled && track.patterns.isNotEmpty(),modifier = Modifier.testTag("mixer-pattern-${track.id}")) { menu = true }
                        DropdownMenu(expanded = menu,onDismissRequest = { menu = false }) {
                            track.patterns.forEach { p -> DropdownMenuItem(text = { Text(p.title) },onClick = { choosePattern(track,p.id); menu = false }) }
                        }
                    }
                    ComposerButton("Mute",enabled,selected = muted[track.id] == true,modifier = Modifier.testTag("mixer-mute-${track.id}")) { mute(track.id) }
                    ComposerButton("Solo",enabled,selected = soloed[track.id] == true,modifier = Modifier.testTag("mixer-solo-${track.id}")) { solo(track.id) }
                    MixerFader(volumes[track.id] ?: 1f,enabled,"Volume ${index+1}",Modifier.weight(1f).testTag("mixer-volume-${track.id}")) { volume(track.id,it) }
                    Text("${((volumes[track.id] ?: 1f)*100).roundToInt()}%",style = MaterialTheme.typography.bodySmall)
                }
            } }
            if(project.tracks.isEmpty()) Text("Add an instrument in Loop.",Modifier.padding(12.dp))
        }
        Column(Modifier.width(82.dp).fillMaxHeight().background(Color(0xFF343044)).border(1.dp,Color(0xFFBFA6E7)).padding(4.dp),horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Master",style = MaterialTheme.typography.titleSmall)
            MixerFader(masterVolume,enabled,"Master Volume",Modifier.weight(1f).testTag("mixer-master"),master)
            Text("${(masterVolume*100).roundToInt()}%",style = MaterialTheme.typography.bodySmall)
        }
    }
    }
}

/** A 48dp-wide direct vertical fader with accessible progress and no extra audio owner. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable private fun MixerFader(value: Float, enabled: Boolean, label: String, modifier: Modifier, change: (Float) -> Unit) {
    val touchInset = with(androidx.compose.ui.platform.LocalDensity.current) { 18.dp.toPx() }
    var height by remember { mutableIntStateOf(1) }
    Canvas(modifier.width(48.dp).onSizeChanged { height = it.height }.semantics {
        contentDescription = label
        progressBarRangeInfo = ProgressBarRangeInfo(value,0f..1f)
        if(!enabled) disabled()
        setProgress { requested -> if(enabled) change(requested.coerceIn(0f,1f)); enabled }
    }.pointerInteropFilter { event ->
        if(!enabled) false else {
            when(event.actionMasked) {
                MotionEvent.ACTION_DOWN,MotionEvent.ACTION_MOVE,MotionEvent.ACTION_UP -> {
                    val inset = minOf(touchInset,height/4f)
                    change((1f-(event.y-inset)/(height-2*inset).coerceAtLeast(1f)).coerceIn(0f,1f))
                }
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
