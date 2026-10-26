package com.mrayover.practicelogger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PracticeTheme {
                Surface(Modifier.fillMaxSize()) {
                    Column(Modifier.safeDrawingPadding().padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        Text("practice.logger", style = MaterialTheme.typography.headlineLarge)
                        Text("A little structure for your practice.", style = MaterialTheme.typography.titleMedium)
                        Text("Foundation build. Block management comes next.")
                    }
                }
            }
        }
    }
}
