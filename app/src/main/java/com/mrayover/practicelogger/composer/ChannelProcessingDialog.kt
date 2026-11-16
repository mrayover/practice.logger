package com.mrayover.practicelogger.composer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.roundToInt

@Composable internal fun ChannelProcessingDialog(track: CompositionTrack, value: ChannelProcessing, enabled: Boolean,
    change: (ChannelProcessing) -> Unit, finished: (ChannelProcessing) -> Unit, close: () -> Unit) {
    Dialog(onDismissRequest=close,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Surface(Modifier.widthIn(max=640.dp).fillMaxWidth(.94f).fillMaxHeight(.94f).testTag("channel-processing"),
            shape=MaterialTheme.shapes.medium,color=MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(horizontal=16.dp,vertical=8.dp)) {
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(track.title,style=MaterialTheme.typography.titleMedium,maxLines=1)
                        Text(ComposerInstruments.trackLabel(track,0).substringAfter(" · "),style=MaterialTheme.typography.bodySmall,maxLines=1)
                    }
                    TextButton(onClick=close) { Text("Close") }
                }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        Text("EQ bypass",Modifier.weight(1f))
                        Switch(checked=!value.eqEnabled,onCheckedChange={ finished(value.copy(eqEnabled=!it)) },enabled=enabled,
                            modifier=Modifier.testTag("eq-bypass"))
                    }
                    val names=listOf("Low · 100 Hz","Low Mid · 450 Hz","High Mid · 2,000 Hz","High · 8,000 Hz")
                    val gains=listOf(value.lowDb,value.lowMidDb,value.highMidDb,value.highDb)
                    for(row in 0..1) Row(horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                        for(column in 0..1) {
                            val band=row*2+column
                            Column(Modifier.weight(1f)) {
                                Text("${names[band]}   ${gains[band]} dB",style=MaterialTheme.typography.bodySmall)
                                fun updated(v: Float): ChannelProcessing {
                                    val db=(v*10).roundToInt()/10f
                                    return when(band) { 0 -> value.copy(lowDb=db);1 -> value.copy(lowMidDb=db);2 -> value.copy(highMidDb=db);else -> value.copy(highDb=db) }
                                }
                                var latest by remember(value) { mutableStateOf(value) }
                                Slider(gains[band],onValueChange={ latest=updated(it);change(latest) },
                                    onValueChangeFinished={ finished(latest) },valueRange=-12f..12f,enabled=enabled,
                                    modifier=Modifier.height(40.dp).testTag("eq-band-$band").semantics { contentDescription=names[band] })
                            }
                        }
                    }
                    Text("Stereo Width   ${(value.width*100).roundToInt()}%",style=MaterialTheme.typography.bodySmall)
                    var latestWidth by remember(value) { mutableStateOf(value) }
                    Slider(value.width,onValueChange={ latestWidth=value.copy(width=(it*100).roundToInt()/100f);change(latestWidth) },
                        onValueChangeFinished={ finished(latestWidth) },valueRange=0f..2f,enabled=enabled,
                        modifier=Modifier.height(40.dp).testTag("stereo-width"))
                }
            }
        }
    }
}
