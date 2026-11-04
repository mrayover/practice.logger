package com.mrayover.practicelogger

import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.input.pointer.RequestDisallowInterceptTouchEvent
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Embeddable instrument; caller owns audio and foreground/reset signals. Dispose on collapse.
 * Native pointer IDs support chords, unison, glissando and ACTION_CANCEL. Accessible key
 * actions audition for 250ms using the same callbacks, with no microphone requirement.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable fun PianoKeyboard(onNoteOn: (Long, Int) -> Unit, onNoteOff: (Long) -> Unit,
    onStopAll: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, resetToken: Int = 0, keyHeight: androidx.compose.ui.unit.Dp = 184.dp) {
    var first by rememberSaveable { mutableIntStateOf(60) }
    val latestOn by rememberUpdatedState(onNoteOn)
    val latestOff by rememberUpdatedState(onNoteOff)
    val latestStop by rememberUpdatedState(onStopAll)
    val scope = rememberCoroutineScope()
    var auditionToken by remember { mutableLongStateOf(1_000_000L) }
    var eventWidth by remember { mutableFloatStateOf(1f) }
    var eventHeight by remember { mutableFloatStateOf(1f) }
    val disallowIntercept = remember { RequestDisallowInterceptTouchEvent() }
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val last = minOf(96, first + if (maxWidth >= 600.dp) 17 else 12)
        val geometry = remember(first, last) { PianoGeometry(first, last) }
        val held = remember(first, last, enabled, resetToken) { mutableStateMapOf<Long, Int>() }
        DisposableEffect(first, last, enabled, resetToken) { onDispose { latestStop(); held.clear() } }
        fun release(id: Long) { if (held.remove(id) != null) latestOff(id) }
        fun press(id: Long, note: Int?) {
            if (held[id] == note) return
            release(id)
            if (note != null && enabled) { held[id] = note; latestOn(id, note) }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { latestStop(); held.clear(); first -= 12 }, enabled = first > 24,
                    modifier = Modifier.semantics { contentDescription = "Previous octave" }) { Text("−8ve") }
                Text("${pianoNoteName(first)}–${pianoNoteName(last)}", Modifier.testTag("piano-register"), style = MaterialTheme.typography.labelLarge)
                TextButton(onClick = { latestStop(); held.clear(); first += 12 }, enabled = first < 84,
                    modifier = Modifier.semantics { contentDescription = "Next octave" }) { Text("+8ve") }
            }
            Text(held.values.distinct().sorted().joinToString(" · ", transform = ::pianoNoteName).ifEmpty { "—" },
                Modifier.testTag("piano-notes"), style = MaterialTheme.typography.labelLarge)
            key(first, last, enabled, resetToken) {
                BoxWithConstraints(Modifier.fillMaxWidth().height(keyHeight).testTag("piano-keyboard")
                    .pointerInteropFilter(disallowIntercept) { event ->
                        if (event.actionMasked == MotionEvent.ACTION_DOWN) disallowIntercept(true)
                        when (event.actionMasked) {
                            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_MOVE -> {
                                for (i in 0 until event.pointerCount) press(event.getPointerId(i).toLong(), geometry.hit(event.getX(i), event.getY(i), eventWidth, eventHeight))
                            }
                            MotionEvent.ACTION_POINTER_UP -> release(event.getPointerId(event.actionIndex).toLong())
                            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { held.keys.toList().forEach(::release) }
                        }
                        true
                    }.onSizeChanged { eventWidth = it.width.toFloat(); eventHeight = it.height.toFloat() }) {
                    val unit = maxWidth / geometry.whites.size
                    val pressedColor = MaterialTheme.colorScheme.primary
                    fun keyModifier(note: Int, black: Boolean): Modifier = Modifier
                        .background(if (held.values.contains(note)) pressedColor else if (black) Color(0xFF242424) else Color(0xFFFFFDF6))
                        .border(1.dp, Color(0xFF555555)).testTag("piano-key-$note")
                        .semantics {
                            contentDescription = pianoNoteName(note); role = Role.Button
                            selected = held.values.contains(note)
                            if (enabled) onClick("Play ${pianoNoteName(note)}") {
                                val token = auditionToken++
                                scope.launch { press(token, note); try { delay(250) } finally { release(token) } }
                                true
                            }
                        }
                    Row(Modifier.fillMaxSize()) {
                        geometry.whites.forEach { note ->
                            Box(Modifier.weight(1f).fillMaxHeight().then(keyModifier(note, false)), contentAlignment = Alignment.BottomCenter) {
                                if (note % 12 == 0) Text(pianoNoteName(note), Modifier.padding(bottom = 8.dp), color = Color(0xFF242424), fontSize = 12.sp)
                            }
                        }
                    }
                    geometry.blacks.forEach { note ->
                        Box(Modifier.offset(x = unit * geometry.blackLeft(note)).width(unit * .62f).fillMaxHeight(.62f).then(keyModifier(note, true)))
                    }
                }
            }
        }
    }
}
