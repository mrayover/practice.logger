package com.mrayover.practicelogger

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.*

val Lato = FontFamily(Font(R.font.lato_regular, FontWeight.Normal), Font(R.font.lato_bold, FontWeight.Bold))
val PracticeCream = Color(0xFFF1E6D3)
val PracticeGreen = Color(0xFF315C47)
private val base = Typography()
val LatoTypography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = Lato), displayMedium = base.displayMedium.copy(fontFamily = Lato), displaySmall = base.displaySmall.copy(fontFamily = Lato),
    headlineLarge = base.headlineLarge.copy(fontFamily = Lato), headlineMedium = base.headlineMedium.copy(fontFamily = Lato), headlineSmall = base.headlineSmall.copy(fontFamily = Lato),
    titleLarge = base.titleLarge.copy(fontFamily = Lato), titleMedium = base.titleMedium.copy(fontFamily = Lato), titleSmall = base.titleSmall.copy(fontFamily = Lato),
    bodyLarge = base.bodyLarge.copy(fontFamily = Lato), bodyMedium = base.bodyMedium.copy(fontFamily = Lato), bodySmall = base.bodySmall.copy(fontFamily = Lato),
    labelLarge = base.labelLarge.copy(fontFamily = Lato), labelMedium = base.labelMedium.copy(fontFamily = Lato), labelSmall = base.labelSmall.copy(fontFamily = Lato)
)
@Composable fun PracticeTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = lightColorScheme(primary = PracticeGreen, onPrimary = Color.White,
        background = PracticeCream, surface = PracticeCream,
        surfaceDim = PracticeCream, surfaceBright = PracticeCream, surfaceVariant = PracticeCream,
        surfaceContainerLowest = PracticeCream, surfaceContainerLow = PracticeCream,
        surfaceContainer = PracticeCream, surfaceContainerHigh = PracticeCream, surfaceContainerHighest = PracticeCream,
        primaryContainer = PracticeCream, onPrimaryContainer = PracticeGreen,
        secondaryContainer = PracticeCream, onSecondaryContainer = PracticeGreen,
        tertiaryContainer = PracticeCream, onTertiaryContainer = PracticeGreen), typography = LatoTypography, content = content)
}
