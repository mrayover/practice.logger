package com.mrayover.practicelogger

import androidx.compose.foundation.layout.*
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

// Reuse the consumed-audio beat's existing 90ms decay; no UI beat clock.
@Composable fun metronomeButtonColors(pulse: MetronomePulse, running: Boolean, outlined: Boolean = false): ButtonColors {
    val lit = running && pulse.lit
    val palette = MaterialTheme.colorScheme
    return if (outlined) ButtonDefaults.outlinedButtonColors(
        containerColor = if (lit) palette.primary else androidx.compose.ui.graphics.Color.Transparent,
        contentColor = if (lit) palette.onPrimary else palette.primary)
    else ButtonDefaults.buttonColors(
        containerColor = if (lit) palette.primaryContainer else palette.primary,
        contentColor = if (lit) palette.onPrimaryContainer else palette.onPrimary)
}

fun Modifier.metronomeBeat(pulse: MetronomePulse, running: Boolean, tag: String): Modifier =
    testTag(tag).semantics { stateDescription = if (running && pulse.lit) "Beat" else "Between beats" }

@OptIn(ExperimentalLayoutApi::class)
@Composable fun MetronomeOptions(config: MetronomeConfig, pulse: MetronomePulse, bpm: Int,
    onConfig: (MetronomeConfig) -> Unit, onBpm: (Int) -> Unit, enabled: Boolean = true, compact: Boolean = false) {
    var entered by rememberSaveable(bpm) { mutableStateOf(bpm.toString()) }
    val parsed = parseWorkingBpm(entered)
    Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(if (compact) 2.dp else 8.dp)) {
        val bpmField: @Composable (Modifier) -> Unit = { modifier ->
            OutlinedTextField(entered,{ entered=it },label={ Text("BPM (30–300)") },singleLine=true, enabled=enabled,
                keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),isError=parsed==null,
                modifier=modifier.testTag("metronome-panel-bpm"))
        }
        val apply: @Composable () -> Unit = {
            TextButton(onClick={ parsed?.let(onBpm) },enabled=enabled && parsed!=null,
                modifier=Modifier.heightIn(min=48.dp).testTag("metronome-apply-bpm")) { Text("Apply BPM") }
        }
        if (compact && androidx.compose.ui.platform.LocalDensity.current.fontScale < 1.5f) Row(Modifier.fillMaxWidth(), verticalAlignment=androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(4.dp)) { bpmField(Modifier.weight(1f)); apply() }
        else { bpmField(if(compact) Modifier.fillMaxWidth() else Modifier); apply() }
        Text("Time signature",style=MaterialTheme.typography.labelLarge)
        if (compact) BoxWithConstraints(Modifier.fillMaxWidth()) {
            val columns = if (maxWidth >= 288.dp && androidx.compose.ui.platform.LocalDensity.current.fontScale < 1.5f) 6 else 3
            Column {
                MetronomeMeter.entries.chunked(columns).forEach { meters ->
                    Row(Modifier.fillMaxWidth()) {
                        meters.forEach { meter ->
                            OutlinedButton(onClick={ onConfig(config.copy(meter=meter)) },enabled=enabled,
                                contentPadding=PaddingValues(horizontal=2.dp), shape=MaterialTheme.shapes.small,
                                colors=ButtonDefaults.outlinedButtonColors(containerColor=if(config.meter==meter)
                                    PracticeTheme.colors.selectionContainer else androidx.compose.ui.graphics.Color.Transparent,
                                    contentColor=if(config.meter==meter) PracticeTheme.colors.onSelectionContainer else MaterialTheme.colorScheme.primary),
                                    border=androidx.compose.foundation.BorderStroke(1.dp, if(config.meter==meter) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                                modifier=Modifier.weight(1f).heightIn(min=48.dp).testTag("meter-${meter.label}")
                                    .semantics { selected=config.meter==meter }) { Text(meter.label,style=MaterialTheme.typography.labelMedium) }
                        }
                    }
                }
            }
        } else FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            MetronomeMeter.entries.forEach { meter ->
                FilterChip(selected=config.meter==meter,onClick={ onConfig(config.copy(meter=meter)) },enabled=enabled,
                    label={ Text(meter.label) },modifier=Modifier.testTag("meter-${meter.label}"))
            }
        }
        Row(Modifier.fillMaxWidth().heightIn(min=48.dp).toggleable(value=config.accent,role=Role.Checkbox,
            enabled=enabled,onValueChange={ onConfig(config.copy(accent=it)) }).testTag("metronome-accent")) {
            Checkbox(checked=config.accent,onCheckedChange=null);Text("Accent first beat",Modifier.padding(top=12.dp))
        }
        if (compact) Row(Modifier.fillMaxWidth().testTag("practice-metronome-modes"),
            horizontalArrangement=Arrangement.spacedBy(4.dp), verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
            MetronomeMode.entries.forEach { mode ->
                OutlinedButton(onClick={ onConfig(config.copy(mode=mode)) },enabled=enabled,
                    shape=MaterialTheme.shapes.small,contentPadding=PaddingValues(horizontal=2.dp,vertical=4.dp),
                    colors=ButtonDefaults.outlinedButtonColors(containerColor=if(config.mode==mode)
                        PracticeTheme.colors.selectionContainer else androidx.compose.ui.graphics.Color.Transparent,
                                    contentColor=if(config.mode==mode) PracticeTheme.colors.onSelectionContainer else MaterialTheme.colorScheme.primary),
                                    border=androidx.compose.foundation.BorderStroke(1.dp, if(config.mode==mode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                    modifier=Modifier.weight(1f).heightIn(min=48.dp).testTag("mode-${mode.name}")
                        .semantics { selected=config.mode==mode }) {
                    Text(if(mode==MetronomeMode.FLASH_ONLY) "Flash Only" else mode.label,
                        style=MaterialTheme.typography.labelMedium,textAlign=androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        } else FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            MetronomeMode.entries.forEach { mode ->
                FilterChip(selected=config.mode==mode,onClick={ onConfig(config.copy(mode=mode)) },enabled=enabled,
                    label={ Text(mode.label) },modifier=Modifier.testTag("mode-${mode.name}"))
            }
        }
        if (!compact) {
        Text(if (config.meter.unit==8) "${config.meter.label}: BPM counts each eighth note (${config.meter.beats} clicks per bar)."
            else "${config.meter.label}: BPM counts each quarter note (${config.meter.beats} clicks per bar).",style=MaterialTheme.typography.bodySmall)
        Text("Meter, accent and output changes apply on the next beat. Device display and audio latency may differ.",style=MaterialTheme.typography.bodySmall)
        if (pulse.sequence>0) Text("Beat ${pulse.beat} of ${pulse.config.meter.beats}",Modifier.testTag("metronome-beat-number"))
        }
    }
}
