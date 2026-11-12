package com.mrayover.practicelogger

import android.os.Bundle
import android.os.Build
import android.app.Activity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.isSystemInDarkTheme
import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import androidx.core.view.WindowCompat
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    lateinit var themePreferences: ThemePreferences
        private set
    override fun onCreate(savedInstanceState: Bundle?) {
        themePreferences = ThemePreferences(applicationContext)
        val initial = resolvePalette(themePreferences.choice.value,
            resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES)
        // Apply explicit preference before Activity inflation; native resources mirror these palettes.
        setTheme(nativeTheme(initial))
        super.onCreate(savedInstanceState)
        updateThemeWindow(initial)
        setContent {
            var ready by remember { mutableStateOf(false) }
            var recoveryError by remember { mutableStateOf(false) }
            var attempt by remember { mutableIntStateOf(0) }
            LaunchedEffect(attempt) {
                recoveryError = false
                try {
                    PortableRestore.beforeAppStarts(applicationContext)
                    themePreferences.reload()
                    ready = true
                } catch (cancelled: kotlinx.coroutines.CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    recoveryError = true
                }
            }
            val choice by themePreferences.choice.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val palette = resolvePalette(choice, systemDark)
            SideEffect { updateThemeWindow(palette) }
            CompositionLocalProvider(LocalThemePreferences provides themePreferences) {
                PracticeTheme(choice, systemDark) {
                    if (ready) CatalogApp(onRestoreAppearance = themePreferences::reload)
                    else Column(Modifier.fillMaxSize().background(palette.scheme.background)
                        .safeDrawingPadding().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        if (recoveryError) {
                            Text("Restore recovery needs attention. Check that private storage is available, then retry.")
                            Button(onClick = { attempt++ }) { Text("Retry recovery") }
                        } else Text("Opening practice.logger…")
                    }
                }
            }
        }
    }
    private fun nativeTheme(palette: PracticePalette): Int = when (palette.id) {
        SmoothPalette.id -> R.style.Theme_PracticeLogger_Smooth
        SunnyPalette.id -> R.style.Theme_PracticeLogger_Sunny
        OrchidPalette.id -> R.style.Theme_PracticeLogger_Orchid
        SubmarinePalette.id -> R.style.Theme_PracticeLogger_Submarine
        LightPalette.id -> R.style.Theme_PracticeLogger_Light
        else -> R.style.Theme_PracticeLogger_Dark
    }
    @Suppress("DEPRECATION")
    private fun updateThemeWindow(palette: PracticePalette) {
        // Keep native dialog/decor defaults aligned during an in-place Compose palette switch.
        // Updating the resource theme does not recreate the Activity or its audio owners.
        setTheme(nativeTheme(palette))
        val banner = palette.colors.banner.toArgb()
        val surface = palette.scheme.background.toArgb()
        val lightStatus = palette.colors.banner.luminance() > .5f
        val lightNavigation = palette.scheme.background.luminance() > .5f
        enableEdgeToEdge(
            statusBarStyle = if (lightStatus) SystemBarStyle.light(banner, banner) else SystemBarStyle.dark(banner),
            navigationBarStyle = if (lightNavigation) SystemBarStyle.light(surface, surface) else SystemBarStyle.dark(surface))
        window.setBackgroundDrawable(ColorDrawable(surface))
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = lightStatus
            isAppearanceLightNavigationBars = lightNavigation
        }
        if (Build.VERSION.SDK_INT < 35) {
            window.statusBarColor = banner
            window.navigationBarColor = surface
        }
    }
}

@Suppress("DEPRECATION")
@Composable fun CatalogApp(vm: CatalogViewModel = viewModel(), practice: PracticeViewModel = viewModel(), recordings: RecordingViewModel = viewModel(), tools: ToolsViewModel = viewModel(), routines: RoutineViewModel = viewModel(), goals: GoalViewModel = viewModel(), log: LogViewModel = viewModel(), images: ImageReferenceViewModel = viewModel(), media: MediaLibraryViewModel = viewModel(), transcribe: TranscribeViewModel = viewModel(), onRestoreAppearance: () -> Unit = {}) {
    val backup: BackupViewModel = viewModel()
    val inspirations: InspirationsViewModel = viewModel()
    val goalRows by goals.goals.collectAsStateWithLifecycle()
    val goalIndicators = remember(goalRows) { GoalIndicators(goalRows) }
    val goalLinks by goals.links.collectAsStateWithLifecycle()
    var goalId by rememberSaveable { mutableStateOf<String?>(null) }
    var goalEditorId by rememberSaveable { mutableStateOf<String?>(null) }
    val sessions by practice.catalog.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val view = LocalView.current
    val focus = androidx.compose.ui.platform.LocalFocusManager.current
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    var freeRecordingStart by rememberSaveable { mutableStateOf(false) }
    var recordingsBlockId by rememberSaveable { mutableStateOf<String?>(null) }
    DisposableEffect(lifecycle, recordings) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) recordings.setForeground(true)
            if (event == Lifecycle.Event.ON_PAUSE) recordings.setForeground(false)
        }
        lifecycle.addObserver(observer)
        recordings.setForeground(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
        onDispose { lifecycle.removeObserver(observer); recordings.setForeground(false) }
    }
    DisposableEffect(lifecycle, practice, transcribe) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                practice.setForeground(true)
                transcribe.setForeground(true)
            }
            if (event == Lifecycle.Event.ON_PAUSE) {
                practice.setForeground(false)
                transcribe.setForeground(false)
            }
        }
        lifecycle.addObserver(observer)
        val resumed = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        practice.setForeground(resumed)
        transcribe.setForeground(resumed)
        onDispose {
            lifecycle.removeObserver(observer)
            practice.setForeground(false)
            transcribe.setForeground(false)
        }
    }
    DisposableEffect(practice.running, transcribe.running, recordings.recording, tools.clicking, tools.listening, tools.stopwatchRunning, tools.countdownRunning, view) { view.keepScreenOn = practice.running || transcribe.running || recordings.recording || tools.clicking || tools.listening || tools.stopwatchRunning || tools.countdownRunning; onDispose { view.keepScreenOn = false } }
    val catalog by vm.catalog.collectAsStateWithLifecycle()
    val weekBlocks by vm.weekBlocks.collectAsStateWithLifecycle()
    val currentWeek by produceState(practiceWeek(), lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                value = practiceWeek()
                delay(15_000)
            }
        }
    }
    val currentWeekIds = remember(weekBlocks, currentWeek) {
        weekBlocks.filter { it.weekStart == currentWeek.toString() }.mapTo(hashSetOf()) { it.blockId }
    }
    var viewedWeek by rememberSaveable { mutableStateOf<String?>(null) }
    var editorWeek by rememberSaveable { mutableStateOf<String?>(null) }
    var builderBack by rememberSaveable { mutableStateOf("practice-now") }
    val selectedWeek = viewedWeek?.let(LocalDate::parse) ?: currentWeek
    var page by rememberSaveable { mutableStateOf("home") }
    var mediaWorkspace by remember { mutableStateOf(false) }
    var settingsReturn by rememberSaveable { mutableStateOf("tools") }
    var headerHomeRequested by remember { mutableStateOf(false) }
    var headerDestination by rememberSaveable { mutableStateOf("home") }
    val backDispatcher = androidx.activity.compose.LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    LaunchedEffect(page) { headerHomeRequested = false }
    var editorReturn by rememberSaveable { mutableStateOf("library") }
    var summaryReturn by rememberSaveable { mutableStateOf("home") }
    var summaryId by rememberSaveable { mutableStateOf<String?>(null) }
    var editorToken by rememberSaveable { mutableStateOf(java.util.UUID.randomUUID().toString()) }
    var editorId by rememberSaveable { mutableStateOf<String?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var archived by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(vm.destinationArchived) {
        vm.destinationArchived?.let { destination ->
            if (vm.deleted) images.editor.discard()
            focus.clearFocus(force = true); keyboard?.hide()
            if (vm.deleted || editorReturn == "library") {
                archived = destination
                query = ""
            }
            page = if (vm.deleted) "library" else editorReturn
            vm.navigationHandled()
        }
    }
    LaunchedEffect(practice.destination) {
        practice.destination?.let { destination ->
            if (destination == "summary") { summaryId = practice.completedId; summaryReturn = "home" }
            page = when {
                destination == "home" && headerHomeRequested -> headerDestination
                destination == "home" && page == "builder" -> builderBack
                else -> destination
            }; practice.navigationHandled()
        }
    }
    LaunchedEffect(routines.editorSaved) {
        if (routines.editorSaved) { focus.clearFocus(force = true); keyboard?.hide(); page = "routines"; routines.navigationHandled() }
    }
    LaunchedEffect(goals.savedId) {
        goals.savedId?.let { id ->
            focus.clearFocus(force = true); keyboard?.hide(); goalId = id; page = "goal-detail"; goals.navigationHandled()
        }
    }
    fun goalName(sessionId: String?): String? = goalLinks.find { it.sessionId == sessionId }?.let { link -> goalRows.find { it.goal.id == link.goalId }?.goal?.title }
    fun openRoutine(id: String?) { if (routines.beginEditor(id)) page = "routine-editor" }
    fun openEditor(id: String?, returnTo: String = "library") {
        editorToken = java.util.UUID.randomUUID().toString()
        editorId = id
        editorReturn = returnTo
        editorWeek = if (id == null && returnTo == "prepare-week") selectedWeek.toString() else null
        page = "editor"
    }
    BackHandler(page == "library") { page = "home" }
    CompositionLocalProvider(LocalGoalIndicators provides goalIndicators) {
    Surface(Modifier.fillMaxSize().testTag("app-surface")) {
      Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))) {
        fun openFromHeader(destination: String) {
            if (page == "backup") {
                if (backup.navigationBlocked) return
                backup.cancel()
                backup.cancelZip()
            }
            headerDestination = destination
            if (destination == "audio-settings" && page != "audio-settings") settingsReturn = page
            if (page == "log") {
                focus.clearFocus(force = true); keyboard?.hide(); log.leave { page = destination }
            } else if (page == "inspirations") {
                focus.clearFocus(force = true); keyboard?.hide()
                inspirations.leave { page = destination }
            } else if (page in setOf("focus", "editor", "routine-editor", "goal-editor", "summary")) {
                headerHomeRequested = true; backDispatcher?.onBackPressed()
            } else { focus.clearFocus(force = true); keyboard?.hide(); page = destination }
        }
        if (page != "transcribe" && (page != "media-library" || !mediaWorkspace)) PracticeBanner(onHome = { openFromHeader("home") }, onSettings = { openFromHeader("audio-settings") },
            enabled = (page == "focus" || (!recordings.recording && !recordings.busy)) && recordingsBlockId == null && page != "free-recording" && (page != "backup" || !backup.navigationBlocked))
        Box(Modifier.weight(1f).fillMaxWidth().then(
            if (page == "transcribe" || (page == "media-library" && mediaWorkspace)) Modifier
            else Modifier.consumeWindowInsets(WindowInsets.safeDrawing.only(WindowInsetsSides.Top)))) {
        if (catalog.error != null || sessions.error != null) {
            Column(Modifier.safeDrawingPadding().padding(24.dp)) { Text(catalog.error ?: sessions.error!!) }
        } else if (!catalog.ready || !sessions.ready || !goals.ready) {
            Box(Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp)) { Text("Loading Blocks…") }
        } else when (page) {
            "home" -> {
                val time by transcribe.practiceTime.collectAsStateWithLifecycle()
                PracticeHome(practiceMs = time?.sumOf { it.activeMs }, onStatistics = { page = "statistics" },
                    onPractice = { page = "practice-now" }, onBlocks = { page = "library" },
                    onRoutines = { page = "routines" }, onTools = { page = "tools" }, onRecording = { page = "recording" }, goals = goalRows,
                    onLog = { log.open(); page = "log" }, onLibrary = { page = "media-library" },
                    onGoals = { page = "goals" }, onGoal = { goalId = it; page = "goal-detail" })
            }
            "statistics" -> {
                val activity by goals.activity.collectAsStateWithLifecycle()
                StatisticsScreen(activity, goalRows, goals.error, onHome = { page = "home" }, onGoal = { goalId = it; page = "goal-detail" },
                    onSession = { summaryId = it; summaryReturn = "statistics"; page = "summary" })
            }
            "media-library" -> {
                BackHandler { page = "home" }
                MediaLibraryScreen(media, onBack = { page = "home" }, onWorkspaceChanged = { mediaWorkspace = it },
                    destinationPills = { DestinationPills("Media", "Inspirations", false) { if (it && !media.busy) page = "inspirations" } })
            }
            "inspirations" -> InspirationsScreen(inspirations, onMedia = { page = "media-library" }, onHome = { page = "home" })
            "playground" -> {
                BackHandler { page = "log" }
                Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    DestinationPills("Journal", "Playground", true) { if (!it) page = "log" }
                    Text("Playground", style = MaterialTheme.typography.headlineMedium)
                    Text("Coming Soon")
                    TextButton(onClick = { page = "log" }) { Text("Back to Log") }
                }
            }
            "log" -> {
                val time by transcribe.practiceTime.collectAsStateWithLifecycle()
                LogScreen(log, onHome = { page = "home" }, practiceTime = time, onPlayground = { page = "playground" })
            }
            "goals" -> GoalManagement(goalRows, goals, onHome = { page = "home" },
                onNew = { goalEditorId = null; page = "goal-editor" }, onGoal = { goalId = it; page = "goal-detail" })
            "goal-detail" -> {
                val goalTimes by goals.times.collectAsStateWithLifecycle()
                GoalDetail(goalRows.find { it.goal.id == goalId }, catalog.blocks, goalTimes.find { it.goalId == goalId }?.activeMs ?: 0L,
                goals, practice, onGoals = { page = "goals" }, onEdit = { goalEditorId = goalId; page = "goal-editor" },
                onBlocks = { page = "goal-blocks" }, onBlock = { openEditor(it, "goal-detail") },
                onSession = { summaryId = it; summaryReturn = "goal-detail"; page = "summary" })
            }
            "goal-editor" -> key(goalEditorId) { GoalEditor(goalRows.find { it.goal.id == goalEditorId }?.goal, goals,
                onCancel = { page = if (headerHomeRequested) headerDestination else if (goalEditorId == null) "goals" else "goal-detail" }, onNavigationCancelled = { headerHomeRequested = false }) }
            "goal-blocks" -> GoalBlocks(goalRows.find { it.goal.id == goalId }, catalog.blocks, goals,
                onBack = { page = "goal-detail" }, onLibrary = { page = "library" })
            "routines" -> PracticeOverview(catalog, sessions, onPractice = { page = "practice-now" },
                onBackup = { page = "backup" }, onLibrary = { page = "library" }, onNew = { openEditor(null) },
                onSummary = { summaryId = it; summaryReturn = "home"; page = "summary" }, onHome = { page = "home" }, routines = routines, practice = practice, onNewRoutine = { openRoutine(null) }, onEditRoutine = { openRoutine(it) })
            "routine-editor" -> RoutineEditor(catalog.blocks, routines, onRoutines = { page = if (headerHomeRequested) headerDestination else "routines" }, onNavigationCancelled = { headerHomeRequested = false })
            "tools" -> ToolsHome(onHome = { page = "home" }, onBackup = { page = "backup" }, onTool = { settingsReturn = "tools"; page = it })
            "audio-settings" -> CountInSettings(recordings, onTools = { page = settingsReturn }, fromHome = settingsReturn == "home", backLabel = if (settingsReturn == "tools") "Back to Tools" else "Back")
            "metronome", "tuner", "timer" -> ToolScreen(page, tools, onTools = { page = "tools" })
            "piano" -> ReferencePianoScreen(onTools = { page = "tools" })
            "recording" -> RecordingHome(catalog.blocks, onHome = { page = "home" }, onBlock = { recordingsBlockId = it }, onFree = { freeRecordingStart = it; page = "free-recording" })
            "free-recording" -> BlockRecordings(null, "Free recording", recordings, practice, null, 120,
                onBeforePlayback = practice::stopClick, onDismiss = { page = "recording" }, startOnOpen = freeRecordingStart)
            "backup" -> BackupScreen(onHome = { page = "home" }, vm = backup, onRestored = { onRestoreAppearance(); recordings.reloadCountIn() })
            "practice-now" -> PracticeNowScreen(sessions, onBack = { page = "home" },
                onWeek = { viewedWeek = practiceWeek().takeUnless { it == currentWeek }?.toString(); page = "week" }, onUnfinished = { page = "unfinished" },
                onBuilder = { builderBack = "practice-now"; page = "builder" },
                onRoutines = { page = "practice-routines" }, onTranscribe = { page = "transcribe" })
            "unfinished" -> UnfinishedSessions(sessions, practice, onBack = { page = "practice-now" })
            "week", "prepare-week" -> key(page, selectedWeek) {
                val preparing = page == "prepare-week"
                val completions = remember(sessions.bundles, selectedWeek) {
                    weekCompletions(sessions.bundles, selectedWeek)
                }
                WeekWorkspace(selectedWeek, selectedWeek == currentWeek, weekBlocks, catalog, preparing,
                    vm, practice, sessions.draft?.placements?.isNotEmpty() == true,
                    completions, blockReferenceSummaries(images),
                    onNavigate = { offset ->
                        val next = selectedWeek.plusWeeks(offset)
                        viewedWeek = next.takeUnless { it == currentWeek }?.toString()
                    },
                    onBack = {
                        if (preparing && selectedWeek == currentWeek) viewedWeek = null
                        page = if (preparing) "week" else "practice-now"
                    },
                    onPrepare = {
                        viewedWeek = selectedWeek.toString()
                        page = "prepare-week"
                    },
                    onCreate = { openEditor(null, "prepare-week") },
                    onEdit = { openEditor(it, if (preparing) "prepare-week" else "week") },
                    onBuild = { ids ->
                        builderBack = "week"
                        practice.addBlocksToBuilder(ids)
                    })
            }
            "practice-routines" -> PracticeRoutinesScreen(catalog, routines, practice, onBack = { page = "practice-now" })
            "transcribe" -> {
                DisposableEffect(transcribe) {
                    transcribe.setVisible(true)
                    onDispose { transcribe.setVisible(false) }
                }
                StandaloneTranscribeScreen(media, transcribe, goalRows, onBack = { page = "practice-now" })
            }
            "builder" -> SessionBuilder(catalog.blocks, sessions.draft, practice, routines, onBack = { page = builderBack }, onCreate = { openEditor(null, "builder") })
            "focus" -> FocusedPractice(sessions.active, practice, images, onHome = { page = "home" }, onRecordings = { recordingsBlockId = it }, recordingMessage = recordings.message, goalName = goalName(sessions.active?.session?.id), recordings = recordings, onNavigationCancelled = { headerHomeRequested = false })
            "summary" -> SessionSummary(sessions.bundles.find { it.session.id == summaryId }, practice, onHome = { page = if (headerHomeRequested) headerDestination else summaryReturn }, goalName = goalName(summaryId), onNavigationCancelled = { headerHomeRequested = false })
            "library" -> {
                val libraryScroll = rememberLazyListState()
                var searchFocused by remember { mutableStateOf(false) }
                var restoreId by rememberSaveable { mutableStateOf<String?>(null) }
                val references = blockReferenceSummaries(images)
                LaunchedEffect(query, archived, searchFocused) {
                    libraryScroll.scrollToItem(if (searchFocused || query.isNotBlank()) 2 else 0)
                }
                var goalFilter by rememberBlockGoalFilter(goalIndicators)
                val matches = matchingBlocks(catalog.blocks, query, archived, goalIndicators, goalFilter)
                LazyColumn(Modifier.fillMaxSize().safeDrawingPadding().imePadding().padding(horizontal = 24.dp).testTag("block-library-list"), state = libraryScroll,
                    verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                    item { Text("Block Library", style = MaterialTheme.typography.headlineMedium) }
                    item { Button(onClick = { openEditor(null) }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("New Block") } }
                    item { OutlinedTextField(query, { query = it }, label = { Text("Search Blocks") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth().onFocusChanged { searchFocused = it.isFocused }) }
                    item { BlockGoalFilterPicker(goalFilter) { goalFilter = it } }
                    item { Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedSelectionChip(selected = !archived, onClick = { archived = false }, label = { Text("Active") })
                        OutlinedSelectionChip(selected = archived, onClick = { archived = true }, label = { Text("Archived") })
                    } }
                    if (matches.isEmpty()) item { Text(if (query.isNotBlank() || goalFilter.availableIn(goalIndicators) != BlockGoalFilter.All) "No matching Blocks." else if (archived) "No archived Blocks." else "No Blocks yet. Create your first Block.") }
                    items(matches, key = { it.id }) { block ->
                        BlockPill(block, goalIndicators.block(block.id), references[block.id].orEmpty(),
                            modifier = Modifier.testTag("library-block-${block.id}")) {
                            TextButton(onClick = { openEditor(block.id) }, enabled = !vm.busy) { Text("Edit") }
                            TextButton(onClick = { recordingsBlockId = block.id }, enabled = !vm.busy) { Text("Recordings") }
                            if (block.archived) {
                                TextButton(onClick = { restoreId = block.id }, enabled = !vm.busy) { Text("Restore Block") }
                            } else {
                                AddToThisWeek(block.id in currentWeekIds, false, vm.busy) {
                                    vm.addToWeek(practiceWeek(), block.id)
                                }
                                TextButton(onClick = {
                                    focus.clearFocus(force = true); keyboard?.hide()
                                    builderBack = "library"
                                    practice.addBlocksToBuilder(listOf(block.id))
                                }, enabled = !vm.busy && !practice.busy) { Text("Add to Practice") }
                            }
                        }
                    }
                }
                restoreId?.let { id ->
                    ConfirmBlockArchive(archived = true, onDismiss = { restoreId = null }, onConfirm = {
                        restoreId = null
                        editorReturn = "library"
                        vm.archive(id, false)
                    })
                }
            }
            "editor" -> {
                val block = editorId?.let { id -> catalog.blocks.find { it.id == id } }
                if (editorId != null && block == null) {
                    Column(Modifier.safeDrawingPadding().padding(24.dp)) {
                        Text("This Block is no longer available.")
                        Button(onClick = { page = "library" }) { Text("Back to Library") }
                    }
                } else key(editorToken) {
                    LaunchedEffect(editorToken) { images.openEditor(editorToken, editorId) { vm.recoveredEditorSaved(it, block?.archived ?: false) } }
                    BlockEditor(block, vm.busy || images.busy, images, onNavigationCancelled = { headerHomeRequested = false }, onDelete = { block?.let { vm.reviewDeletion(it.id) } }, onCancel = { page = if (headerHomeRequested) headerDestination else editorReturn },
                        onSave = { draft -> vm.save(editorId, draft, block?.archived ?: false, editorWeek?.let(LocalDate::parse), images.editor, editorReturn == "builder") },
                        onArchive = { value -> block?.let { vm.archive(it.id, value) } }, onRecordings = { block?.let { recordingsBlockId = it.id } })
                }
            }
        }
        }
      }
        recordingsBlockId?.let { id ->
            catalog.blocks.find { it.id == id }?.let { block ->
                BlockRecordings(id, block.name, recordings, practice, if (page == "focus") sessions.active else null,
                    initialBpm = if (page == "focus") practice.workingBpm else block.tempo?.takeIf { it in 30..300 } ?: 120,
                    onBeforePlayback = practice::stopClick, onDismiss = {
                        if (page == "focus") { recordings.stopPlayback(); recordings.finishCapture() }
                        recordingsBlockId = null
                    }, references = images, manageLifecycle = page != "focus")
            }
        }
        practice.error?.let { message ->
            AlertDialog(onDismissRequest = practice::dismissError, title = { Text("Practice paused") }, text = { Text(message) }, confirmButton = { TextButton(onClick = practice::dismissError) { Text("OK") } })
        }
        goals.error?.let { message ->
            AlertDialog(onDismissRequest = goals::dismissError, title = { Text("Goal could not be updated") }, text = { Text(message) },
                confirmButton = { TextButton(onClick = goals::dismissError) { Text("OK") } })
        }
        routines.error?.let { message ->
            AlertDialog(onDismissRequest = routines::dismissError, title = { Text("Routine could not be updated") }, text = { Text(message) },
                confirmButton = { TextButton(onClick = routines::dismissError) { Text("OK") } })
        }
        vm.deletionReview?.let { review ->
            val eligible = review.dependencies.canDelete
            AlertDialog(onDismissRequest = vm::dismissDeletion,
                title = { Text(if (eligible) "Delete \"${review.block.name}\" permanently?" else "This Block cannot be deleted yet.") },
                text = { Text(if (eligible) "This removes the saved Block from your library and cannot be undone. Unsaved edits will be discarded."
                    else review.dependencies.explanation(), modifier = Modifier.verticalScroll(rememberScrollState())) },
                confirmButton = {
                    TextButton(onClick = if (eligible) vm::confirmDeletion else vm::dismissDeletion, enabled = !vm.busy,
                        modifier = Modifier.testTag("block-delete-confirm")) { Text(if (eligible) "Delete Permanently" else "OK") }
                },
                dismissButton = { if (eligible) TextButton(onClick = vm::dismissDeletion, enabled = !vm.busy) { Text("Cancel") } })
        }
        vm.error?.let { message ->
            AlertDialog(onDismissRequest = vm::dismissError, title = { Text("Changes not saved") }, text = { Text(message) }, confirmButton = { TextButton(onClick = vm::dismissError) { Text("OK") } })
        }
    }
}

}

@Composable private fun BlockEditor(block: Block?, busy: Boolean, images: ImageReferenceViewModel, onDelete: () -> Unit, onCancel: () -> Unit, onNavigationCancelled: () -> Unit = {}, onSave: (BlockDraft) -> Unit, onArchive: (Boolean) -> Unit, onRecordings: () -> Unit) {
    val initial = remember { block?.let(BlockDraft::from) ?: BlockDraft() }
    var name by rememberSaveable { mutableStateOf(initial.name) }
    var action by rememberSaveable { mutableStateOf(initial.action) }
    var duration by rememberSaveable { mutableStateOf(initial.duration) }
    var tempo by rememberSaveable { mutableStateOf(initial.tempo) }
    var notes by rememberSaveable { mutableStateOf(initial.notes) }
    var submitted by rememberSaveable { mutableStateOf(false) }
    var discard by rememberSaveable { mutableStateOf(false) }
    var archiveConfirm by rememberSaveable { mutableStateOf(false) }
    val savedPrimary = LocalGoalIndicators.current.block(block?.id)?.goal?.id
    var pickedPrimary by rememberSaveable { mutableStateOf<String?>(null) }
    var primaryEdited by rememberSaveable { mutableStateOf(false) }
    val primary = if (primaryEdited) pickedPrimary else savedPrimary
    val draft = BlockDraft(name, action, duration, tempo, notes, primary.takeIf { primaryEdited }, primaryEdited)
    val errors = if (submitted) draft.errors() else emptyMap()
    fun leave() { if (!busy) { if (draft != initial || images.editor.dirty) discard = true else images.discardEditor(onCancel) } }
    BackHandler { leave() }
    var notesExpanded by rememberSaveable { mutableStateOf(false) }
    var overflow by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text(if (block == null) "New Block" else "Edit Block", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            if (block != null) Box {
                TextButton(onClick = { overflow = true }, enabled = !busy, modifier = Modifier.testTag("block-overflow")) { Text("More") }
                DropdownMenu(expanded = overflow, onDismissRequest = { overflow = false }) {
                    DropdownMenuItem(text = { Text("Recordings") }, onClick = { overflow = false; onRecordings() })
                    DropdownMenuItem(text = { Text(if (block.archived) "Restore Block" else "Archive Block") }, onClick = { overflow = false; archiveConfirm = true })
                    DropdownMenuItem(text = { Text("Delete Permanently") }, onClick = { overflow = false; onDelete() }, modifier = Modifier.testTag("block-delete-action"))
                }
            }
        }
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (block?.archived == true) Text("Archived", style = MaterialTheme.typography.bodySmall)
            EditorField("Name", name, { name = it }, errors["name"], busy, singleLine = true)
            EditorField("Action", action, { action = it }, errors["action"], busy)
            BoxWithConstraints {
                val largeText = androidx.compose.ui.platform.LocalDensity.current.fontScale >= 1.5f
                if (maxWidth >= 360.dp && !largeText) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f)) { EditorField("Default duration (minutes)", duration, { duration = it }, errors["duration"], busy, singleLine = true, numeric = true) }
                    Box(Modifier.weight(1f)) { EditorField("Tempo reference (BPM, optional)", tempo, { tempo = it }, errors["tempo"], busy, singleLine = true, numeric = true) }
                } else Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    EditorField("Default duration (minutes)", duration, { duration = it }, errors["duration"], busy, singleLine = true, numeric = true)
                    EditorField("Tempo reference (BPM, optional)", tempo, { tempo = it }, errors["tempo"], busy, singleLine = true, numeric = true)
                }
            }
            PrimaryGoalPicker(primary, enabled = !busy) { selected ->
                pickedPrimary = selected; primaryEdited = selected != savedPrimary
            }
            block?.let { LearningButton(it.id) }
            BlockImageEditor(block?.id, images, busy)
            TextButton(onClick = { notesExpanded = !notesExpanded }, modifier = Modifier.fillMaxWidth()) {
                Text(if (notesExpanded) "Hide Notes / Source" else "Notes / Source" + if (notes.isNotBlank()) " · saved text" else " (optional)")
            }
            if (notesExpanded) EditorField("Notes / source (optional)", notes, { notes = it }, null, busy)
            Spacer(Modifier.height(8.dp))
        }
        HorizontalDivider()
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = ::leave, enabled = !busy) { Text("Cancel") }
            Button(onClick = { submitted = true; if (draft.errors().isEmpty()) onSave(draft) }, enabled = !busy && images.editor.ready,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("block-save")) { Text(if (busy) "Saving…" else "Save Block") }
        }
    }
    if (discard) AlertDialog(onDismissRequest = { discard = false; onNavigationCancelled() }, title = { Text("Discard unsaved changes?") }, text = { Text("Your saved Block will stay unchanged.") },
        confirmButton = { TextButton(onClick = { discard = false; images.discardEditor(onCancel) }) { Text("Discard") } },
        dismissButton = { TextButton(onClick = { discard = false; onNavigationCancelled() }) { Text("Keep editing") } })
    if (archiveConfirm && block != null) ConfirmBlockArchive(block.archived, dirty = draft != initial || images.editor.dirty,
        onDismiss = { archiveConfirm = false }, onConfirm = { archiveConfirm = false; images.discardEditor { onArchive(!block.archived) } })
}

@Composable
private fun ConfirmBlockArchive(archived: Boolean, dirty: Boolean = false, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(if (archived) "Restore this Block?" else "Archive this Block?") },
        text = {
            Text((if (dirty) "Unsaved edits will be discarded. " else "") +
                "The saved Block and its history are kept.")
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text(if (archived) "Restore" else "Archive") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable private fun EditorField(label: String, value: String, changed: (String) -> Unit, error: String?, busy: Boolean, singleLine: Boolean = false, numeric: Boolean = false) {
    OutlinedTextField(value, changed, label = { Text(label) }, enabled = !busy, isError = error != null,
        supportingText = { if (error != null) Text(error) }, singleLine = singleLine, minLines = 1,
        keyboardOptions = KeyboardOptions(keyboardType = if (numeric) KeyboardType.Number else KeyboardType.Text),
        modifier = Modifier.fillMaxWidth().testTag(label))
}
