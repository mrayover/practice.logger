package com.mrayover.practicelogger

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

class CountInPreferences(context: Context) {
    private val preferences = context.getSharedPreferences("audio-settings", Context.MODE_PRIVATE)
    fun read(): Long = runCatching { preferences.getLong("count-in-beats",4).also { PhraseSpec(300,1,it) } }.getOrDefault(4)
    fun save(value: Long) { PhraseSpec(300,1,value); check(preferences.edit().putLong("count-in-beats",value).commit()) { "Default count-in could not be saved." } }
}

@Composable fun CountInSettings(vm: RecordingViewModel, onTools: () -> Unit) {
    BackHandler(onBack = onTools)
    var value by rememberSaveable(vm.defaultCountIn) { mutableStateOf(vm.defaultCountIn.toString()) }
    val parsed = value.toLongOrNull()
    Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Audio Settings", style = MaterialTheme.typography.headlineMedium)
        Text("Default loop count-in")
        OutlinedTextField(value, { value = it }, label = { Text("Default count-in beats") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), isError = parsed == null || parsed <= 0,
            modifier = Modifier.testTag("default-count-in"))
        Text("Positive whole beats. Each loop can override this default. Count-ins must fit a 15-minute execution budget at the selected BPM.")
        Button(onClick = { vm.saveDefaultCountIn(value) }, enabled = !vm.busy && parsed != null && parsed > 0) { Text("Save default count-in") }
        vm.message?.let { Text(it) }
        TextButton(onClick = onTools) { Text("Back to Tools") }
    }
}
