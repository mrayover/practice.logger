package com.mrayover.practicelogger

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable fun MetronomeFlash(pulse: MetronomePulse, running: Boolean, tag: String) {
    val lit = running && pulse.lit
    val color = MaterialTheme.colorScheme.primary
    Box(Modifier.size(if (lit && pulse.config.accent && pulse.beat == 1) 14.dp else 12.dp)
        .background(if (lit) color else color.copy(alpha=.12f),CircleShape).border(1.dp,color,CircleShape)
        .testTag(tag).semantics {
            contentDescription = if (running) "Metronome beat ${pulse.beat} of ${pulse.config.meter.beats}" else "Metronome stopped"
            stateDescription = if (lit) "Beat" else "Between beats"
        })
}

@OptIn(ExperimentalLayoutApi::class)
@Composable fun MetronomeOptions(config: MetronomeConfig, pulse: MetronomePulse, bpm: Int,
    onConfig: (MetronomeConfig) -> Unit, onBpm: (Int) -> Unit, enabled: Boolean = true) {
    var entered by rememberSaveable(bpm) { mutableStateOf(bpm.toString()) }
    val parsed = parseWorkingBpm(entered)
    Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(entered,{ entered=it },label={ Text("BPM (30–300)") },singleLine=true,
            keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),isError=parsed==null,modifier=Modifier.testTag("metronome-panel-bpm"))
        TextButton(onClick={ parsed?.let(onBpm) },enabled=enabled && parsed!=null,modifier=Modifier.testTag("metronome-apply-bpm")) { Text("Apply BPM") }
        Text("Time signature",style=MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            MetronomeMeter.entries.forEach { meter ->
                FilterChip(selected=config.meter==meter,onClick={ onConfig(config.copy(meter=meter)) },enabled=enabled,
                    label={ Text(meter.label) },modifier=Modifier.testTag("meter-${meter.label}"))
            }
        }
        Row(Modifier.fillMaxWidth().heightIn(min=48.dp).toggleable(value=config.accent,role=Role.Checkbox,
            enabled=enabled,onValueChange={ onConfig(config.copy(accent=it)) }).testTag("metronome-accent")) {
            Checkbox(checked=config.accent,onCheckedChange=null);Text("Accent first beat",Modifier.padding(top=12.dp))
        }
        FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            MetronomeMode.entries.forEach { mode ->
                FilterChip(selected=config.mode==mode,onClick={ onConfig(config.copy(mode=mode)) },enabled=enabled,
                    label={ Text(mode.label) },modifier=Modifier.testTag("mode-${mode.name}"))
            }
        }
        Text(if (config.meter.unit==8) "${config.meter.label}: BPM counts each eighth note (${config.meter.beats} clicks per bar)."
            else "${config.meter.label}: BPM counts each quarter note (${config.meter.beats} clicks per bar).",style=MaterialTheme.typography.bodySmall)
        Text("Meter, accent and output changes apply on the next beat. Device display and audio latency may differ.",style=MaterialTheme.typography.bodySmall)
        if (pulse.sequence>0) Text("Beat ${pulse.beat} of ${pulse.config.meter.beats}",Modifier.testTag("metronome-beat-number"))
    }
}
