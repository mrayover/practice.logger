package com.mrayover.practicelogger

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization

/** Ask the user's Android keyboard for writing assistance; never rewrite stored/draft text. */
internal val ProseKeyboardOptions = KeyboardOptions(
    capitalization = KeyboardCapitalization.Sentences,
    autoCorrectEnabled = true
)
