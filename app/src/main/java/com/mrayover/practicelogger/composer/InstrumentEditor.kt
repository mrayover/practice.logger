package com.mrayover.practicelogger.composer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mrayover.practicelogger.PianoKeyboard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.util.Locale
import kotlin.math.*

private data class SoundDial(val name: String,val min: Float,val max: Float,val default: Float,val unit: String,val logarithmic: Boolean=false) {
    fun fraction(v: Float)=if(logarithmic) (ln(v/min)/ln(max/min)).coerceIn(0f,1f) else ((v-min)/(max-min)).coerceIn(0f,1f)
    fun value(f: Float)=if(logarithmic) min*(max/min).pow(f.coerceIn(0f,1f)) else min+(max-min)*f.coerceIn(0f,1f)
    fun display(v: Float): String = when(name) {
        "Pan" -> if(abs(v)<.005f) "Center" else "${if(v<0) "L" else "R"} ${abs(v*100).roundToInt()}"
        "Sustain" -> "${(v*100).roundToInt()} %"
        "Resonance" -> String.format(Locale.US,"%.2f Q",v)
        else -> "${v.roundToInt()} $unit"
    }
}
private val soundDials=listOf(SoundDial("Attack",0f,3000f,10f,"ms"),SoundDial("Release",10f,5000f,300f,"ms"),
    SoundDial("Cutoff",40f,18000f,18000f,"Hz",true),SoundDial("Resonance",.5f,4f,.707f,"Q"),
    SoundDial("Sustain",0f,1f,.8f,"%"),SoundDial("Pan",-1f,1f,0f,""))
private fun ComposerSound.values()=listOf(attackMs,releaseMs,cutoffHz,resonanceQ,sustain,pan)
private fun ComposerSound.withValue(i: Int,v: Float): ComposerSound = when(i) {
    0->copy(attackMs=v); 1->copy(releaseMs=v); 2->copy(cutoffHz=v); 3->copy(resonanceQ=v); 4->copy(sustain=v); else->copy(pan=v)
}

@Composable private fun InstrumentDial(dial: SoundDial,value: Float,change: (Float)->Unit,precise: ()->Unit,modifier: Modifier=Modifier,compact: Boolean=false) {
    val latestValue by rememberUpdatedState(value)
    val latestChange by rememberUpdatedState(change)
    val travel=with(LocalDensity.current) { 160.dp.toPx() }
    Column(modifier, horizontalAlignment=Alignment.CenterHorizontally) {
        Text(dial.name,style=MaterialTheme.typography.labelLarge)
        Canvas(Modifier.size(if(compact) 52.dp else 64.dp).testTag("dial-${dial.name}")
            .semantics {
                contentDescription=dial.name; stateDescription=dial.display(value)
                progressBarRangeInfo=ProgressBarRangeInfo(dial.fraction(value),0f..1f)
                setProgress { latestChange(dial.value(it)); true }
                customActions=listOf(CustomAccessibilityAction("Restore default") { latestChange(dial.default); true },CustomAccessibilityAction("Enter value") { precise(); true })
            }.pointerInput(dial) {
                detectVerticalDragGestures { event,delta -> event.consume(); latestChange(dial.value(dial.fraction(latestValue)-delta/travel)) }
            }) {
            val pad=7.dp.toPx(); val arc=Size(size.width-pad*2,size.height-pad*2)
            drawCircle(Color(0xFF2D303B),radius=size.minDimension/2-4.dp.toPx())
            drawArc(Color(0xFF505363),135f,270f,false,Offset(pad,pad),arc,style=Stroke(4.dp.toPx(),cap=StrokeCap.Round))
            val f=dial.fraction(value)
            drawArc(Color(0xFFC9B6FF),135f,270f*f,false,Offset(pad,pad),arc,style=Stroke(4.dp.toPx(),cap=StrokeCap.Round))
            val angle=(135+270*f)*PI.toFloat()/180
            val radius=size.minDimension*.28f
            drawLine(Color(0xFFE7DDFF),center,center+Offset(cos(angle)*radius,sin(angle)*radius),3.dp.toPx(),StrokeCap.Round)
        }
        Text(dial.display(value),Modifier.heightIn(min=28.dp).clickable(onClick=precise).testTag("value-${dial.name}").padding(4.dp),style=MaterialTheme.typography.bodyMedium)
    }
}

/** Overlay keeps the roll, selection, scroll and sole audio owner mounted. */
@Composable internal fun InstrumentEditor(reference: InstrumentReference,trackTitle: String,trackIndex: Int,audio: ComposerAudio,ready: Boolean,
    onUse: (InstrumentReference)->Unit,onClose: ()->Unit) {
    val context=LocalContext.current
    val store=remember { ComposerPresetStore(File(context.filesDir,"composer/presets")) }
    val scope=rememberCoroutineScope()
    var resetSignal by remember { mutableIntStateOf(audio.interruptionToken) }
    LaunchedEffect(audio) { while(true) { resetSignal=audio.interruptionToken; kotlinx.coroutines.delay(30) } }
    var presets by remember { mutableStateOf(emptyList<ComposerPreset>()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var base by rememberSaveable { mutableStateOf(ComposerInstruments.all.getOrElse(ComposerInstruments.index(reference)) { ComposerInstruments.all.first() }.id) }
    var presetId by rememberSaveable { mutableStateOf(reference.presetId) }
    var presetTitle by rememberSaveable { mutableStateOf(reference.presetTitle ?: ComposerInstruments.title(reference)) }
    var savedSound by rememberSaveable { mutableStateOf(reference.sound?.json()?.toString()) }
    var draftSound by rememberSaveable { mutableStateOf(savedSound) }
    val sound=draftSound?.let { ComposerSound.decode(JSONObject(it)) }
    val dirty=draftSound!=savedSound
    var menu by remember { mutableStateOf(false) }
    var naming by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var numeric by rememberSaveable { mutableIntStateOf(-1) }
    var number by rememberSaveable { mutableStateOf("") }
    var confirm by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf<(() -> Unit)?>(null) }
    fun guarded(action: () -> Unit) { if(dirty) { pending=action; confirm=true } else action() }
    fun close() { if(!saving) guarded { audio.stopAll(); onClose() } }
    fun select(ref: InstrumentReference) {
        audio.stopAll(); base=ref.instrumentId; presetId=ref.presetId; presetTitle=ComposerInstruments.title(ref)
        savedSound=ref.sound?.json()?.toString(); draftSound=savedSound; error=null
    }
    LaunchedEffect(store) {
        try { val result=withContext(Dispatchers.IO) { store.list() }; presets=result.first
            if(result.second>0) error="${result.second} unreadable presets preserved." }
        catch(e: Exception) { error="Could not read presets: ${e.message}" }
        finally { loading=false }
    }
    LaunchedEffect(draftSound,base) { audio.previewSound(trackIndex,sound) }
    DisposableEffect(audio) { onDispose { audio.stopAll(); audio.previewSound(trackIndex,reference.sound) } }
    Dialog(onDismissRequest=::close,properties=DialogProperties(usePlatformDefaultWidth=false,decorFitsSystemWindows=false,dismissOnClickOutside=false)) {
        val window=(LocalView.current.parent as? DialogWindowProvider)?.window
        LaunchedEffect(window) { window?.decorView?.post { WindowCompat.getInsetsController(window,window.decorView).apply { systemBarsBehavior=WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE; hide(WindowInsetsCompat.Type.systemBars()) } } }
        BackHandler { close() }
        Surface(Modifier.fillMaxSize().testTag("instrument-editor"),color=Color(0xFF101216)) {
            BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
                val editorHeight=maxHeight
                val narrow=maxWidth<760.dp
                val largeText=LocalDensity.current.fontScale>1.3f
                val fallback=maxWidth<maxHeight || largeText || maxHeight<330.dp
                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides if(editorHeight<400.dp && !largeText) 40.dp else 48.dp) {
                Column(Modifier.fillMaxSize().padding(horizontal=8.dp)) {
                  Column(Modifier.weight(1f).fillMaxWidth().then(if(fallback) Modifier.verticalScroll(rememberScrollState()) else Modifier).testTag("instrument-editor-body")) {
                    Column(Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                            TextButton(onClick=::close,enabled=!saving) { Text("Back") }
                            Column(Modifier.weight(1f).padding(horizontal=6.dp)) {
                                Text(presetTitle,style=MaterialTheme.typography.titleMedium,maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                Text(if(dirty) "Unsaved sound changes" else if(sound==null) "Factory · dials show editor defaults" else "My Presets · saved · next Play",style=MaterialTheme.typography.bodySmall,color=Color(0xFFC9B6FF),maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            }
                            if(!narrow && !fallback) EditorActions(!saving && !loading,dirty, { name="$presetTitle copy".take(60); naming=true }, { audio.stopAll(); onUse(InstrumentReference(1,base,presetId,sound,if(sound==null) null else presetTitle)) })
                        }
                        FlowRow(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.Center) {
                            Box {
                                TextButton(onClick={ menu=true },enabled=!loading && !saving) { Text("Presets ▾") }
                                DropdownMenu(expanded=menu,onDismissRequest={ menu=false }) {
                                    Text("Factory Presets",Modifier.padding(12.dp),color=Color(0xFFC9B6FF))
                                    ComposerInstruments.all.forEach { factory -> DropdownMenuItem(text={ Text(factory.title) },onClick={ menu=false; guarded { select(factory.reference) } }) }
                                    HorizontalDivider()
                                    Text("My Presets",Modifier.padding(12.dp),color=Color(0xFFC9B6FF))
                                    if(presets.isEmpty()) Text("No saved presets yet",Modifier.padding(12.dp))
                                    presets.forEach { p -> DropdownMenuItem(text={ Text(p.title) },onClick={ menu=false; guarded { select(p.reference) } }) }
                                }
                            }
                            if(narrow || fallback) EditorActions(!saving && !loading,dirty,{ name="$presetTitle copy".take(60); naming=true },{ audio.stopAll(); onUse(InstrumentReference(1,base,presetId,sound,if(sound==null) null else presetTitle)) })
                            else Text("$trackTitle · Live preview; track uses saved sound on next Play",style=MaterialTheme.typography.bodySmall)
                        }
                        error?.let { Text(it,color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall) }
                    }
                    val shown=sound ?: ComposerSound()
                    Column(Modifier.fillMaxWidth().then(if(fallback) Modifier else Modifier.weight(1f).verticalScroll(rememberScrollState())).testTag("instrument-knobs"),verticalArrangement=Arrangement.Center) {
                        val columns=if(fallback) 3 else 6
                        soundDials.chunked(columns).forEachIndexed { row,items ->
                            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly) {
                                items.forEachIndexed { col,d -> val i=row*columns+col
                                    InstrumentDial(d,shown.values()[i],{ value -> draftSound=shown.withValue(i,value).json().toString() },{
                                        numeric=i; val raw=shown.values()[i]; number=String.format(Locale.US,"%.3f",if(i==4) raw*100 else raw)
                                    },Modifier.weight(1f),compact=editorHeight<400.dp)
                                }
                            }
                        }
                    }
                  }
                    // Same geometry/multitouch/range gestures as Reference Piano, Composer callbacks only.
                    PianoKeyboard(onNoteOn={ token,pitch -> audio.noteOn(token,pitch,ComposerInstruments.all.indexOfFirst { it.id==base },trackIndex) },
                        onNoteOff=audio::noteOff,onStopAll=audio::stopAll,enabled=ready,resetToken=resetSignal,compact=true,
                        keyHeight=if(fallback) 80.dp else (editorHeight*.29f).coerceIn(96.dp,180.dp),modifier=Modifier.testTag("instrument-keyboard"))
                }
                }
            }
        }
    }
    if(confirm) AlertDialog(onDismissRequest={ confirm=false; pending=null },title={ Text("Discard unsaved sound changes?") },text={ Text("Save New Preset first to keep this sound. The track has not changed.") },
        confirmButton={ TextButton(onClick={ confirm=false; pending?.invoke(); pending=null }) { Text("Discard changes") } },dismissButton={ TextButton(onClick={ confirm=false; pending=null }) { Text("Keep editing") } })
    if(naming) AlertDialog(modifier=Modifier.imePadding(),onDismissRequest={ if(!saving) naming=false },title={ Text("Save New Preset") },text={ Column(Modifier.verticalScroll(rememberScrollState())) {
        OutlinedTextField(value=name,onValueChange={ name=it },singleLine=true,label={ Text("Preset name") },modifier=Modifier.testTag("preset-name"))
        Text("Creates a new sound; factory presets stay read-only.",style=MaterialTheme.typography.bodySmall)
        error?.let { Text(it,color=MaterialTheme.colorScheme.error) }
    } },confirmButton={ TextButton(enabled=!saving && name.trim().isNotEmpty() && name.trim().length<=60 && name.none { it.isISOControl() },onClick={
        saving=true; scope.launch {
            try { val p=withContext(Dispatchers.IO) { store.create(name,base,sound ?: ComposerSound()) }; presets=(presets+p).sortedBy { it.title.lowercase() }; select(p.reference); naming=false }
            catch(e: Exception) { error="Preset not saved: ${e.message}" } finally { saving=false }
        }
    }) { Text(if(saving) "Saving…" else "Save preset") } },dismissButton={ TextButton(onClick={ naming=false },enabled=!saving) { Text("Cancel") } })
    if(numeric>=0) {
        val d=soundDials[numeric]; val percent=numeric==4
        val raw=number.toFloatOrNull()?.let { if(percent) it/100 else it }
        val valid=raw!=null && raw.isFinite() && raw in d.min..d.max
        AlertDialog(modifier=Modifier.imePadding(),onDismissRequest={ numeric=-1 },title={ Text("${d.name} · precise value") },text={ Column(Modifier.verticalScroll(rememberScrollState())) {
            OutlinedTextField(value=number,onValueChange={ number=it },singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=if(numeric==5) KeyboardType.Ascii else KeyboardType.Decimal),label={ Text(if(percent) "Percent (0–100)" else "${d.min}–${d.max} ${d.unit}") },modifier=Modifier.testTag("parameter-number"))
            Text(if(numeric==0) "Attack starts on the next note." else if(numeric==4) "Held amplitude after attack/decay; not a sustain pedal." else "Changes are heard in the preview keyboard.",style=MaterialTheme.typography.bodySmall)
        } },confirmButton={ TextButton(onClick={ draftSound=(sound ?: ComposerSound()).withValue(numeric,raw!!).json().toString(); numeric=-1 },enabled=valid) { Text("Set value") } },dismissButton={ Row {
            TextButton(onClick={ draftSound=(sound ?: ComposerSound()).withValue(numeric,d.default).json().toString(); numeric=-1 }) { Text("Default") }
            TextButton(onClick={ numeric=-1 }) { Text("Cancel") }
        } })
    }
}
@Composable private fun EditorActions(enabled: Boolean,dirty: Boolean,save: ()->Unit,use: ()->Unit) {
    TextButton(onClick=save,enabled=enabled) { Text("Save New Preset") }
    TextButton(onClick=use,enabled=enabled && !dirty) { Text("Use on Track") }
}
