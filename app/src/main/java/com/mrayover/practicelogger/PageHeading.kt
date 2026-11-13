package com.mrayover.practicelogger

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow

/** Full content width, independent of navigation controls; retains full text in semantics. */
@Composable internal fun PageHeading(
    text: String, modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.headlineMedium, maxLines: Int = Int.MAX_VALUE
) {
    Text(text, modifier.fillMaxWidth().semantics { heading() }, style = style,
        textAlign = TextAlign.Center, maxLines = maxLines, overflow = TextOverflow.Ellipsis)
}
