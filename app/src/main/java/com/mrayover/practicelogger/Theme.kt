package com.mrayover.practicelogger

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.*

val Lato = FontFamily(Font(R.font.lato_regular, FontWeight.Normal), Font(R.font.lato_bold, FontWeight.Bold))
// Raw palette primitives stay here; screens consume semantic roles.
private val PracticeCream = Color(0xFFF1E6D3)
private val PracticeGreen = Color(0xFF315C47)
@Immutable data class GoalColors(val accent: Color, val container: Color, val onContainer: Color)
@Immutable data class PracticeColors(
    val banner: Color, val onBanner: Color, val bannerEdge: Color,
    val transportSurface: Color, val selectionContainer: Color, val onSelectionContainer: Color,
    val recording: Color, val onRecording: Color, val scrollThumb: Color, val scrollTrack: Color,
    // Reserved for G1; no Goal allocation or persistence changes here.
    val goals: List<GoalColors>, val unassignedGoalAccent: Color
)
@Immutable data class PracticePalette(val id: String, val scheme: ColorScheme, val colors: PracticeColors)
private val lightScheme = lightColorScheme(
    primary = PracticeGreen, onPrimary = Color.White,
    primaryContainer = PracticeCream, onPrimaryContainer = PracticeGreen, inversePrimary = Color(0xFFA5D2B2),
    secondary = Color(0xFF4E6355), onSecondary = Color.White,
    secondaryContainer = PracticeCream, onSecondaryContainer = PracticeGreen,
    tertiary = Color(0xFF52634D), onTertiary = Color.White,
    tertiaryContainer = PracticeCream, onTertiaryContainer = PracticeGreen,
    background = PracticeCream, onBackground = Color(0xFF1D1B20),
    surface = PracticeCream, onSurface = Color(0xFF1D1B20),
    surfaceVariant = PracticeCream, onSurfaceVariant = Color(0xFF4C514C),
    surfaceDim = PracticeCream, surfaceBright = PracticeCream,
    surfaceContainerLowest = PracticeCream, surfaceContainerLow = PracticeCream,
    surfaceContainer = PracticeCream, surfaceContainerHigh = PracticeCream, surfaceContainerHighest = PracticeCream,
    inverseSurface = Color(0xFF2D332F), inverseOnSurface = Color(0xFFF1F0E9),
    outline = Color(0xFF697469), outlineVariant = Color(0xFFC0C8BA),
    error = Color(0xFFBA1A1A), onError = Color.White,
    errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002),
    scrim = Color.Black, surfaceTint = PracticeGreen
)
private val darkScheme = darkColorScheme(
    primary = Color(0xFFA5D2B2), onPrimary = Color(0xFF103823),
    primaryContainer = Color(0xFF284D37), onPrimaryContainer = Color(0xFFC0EFCD), inversePrimary = PracticeGreen,
    secondary = Color(0xFFB6CCBA), onSecondary = Color(0xFF223528),
    secondaryContainer = Color(0xFF384C3E), onSecondaryContainer = Color(0xFFD2E8D6),
    tertiary = Color(0xFFBCCDAD), onTertiary = Color(0xFF28351F),
    tertiaryContainer = Color(0xFF3E4C34), onTertiaryContainer = Color(0xFFD8E9C8),
    background = Color(0xFF171C19), onBackground = Color(0xFFE6E8E1),
    surface = Color(0xFF171C19), onSurface = Color(0xFFE6E8E1),
    surfaceVariant = Color(0xFF3B4940), onSurfaceVariant = Color(0xFFCDD5CB),
    surfaceDim = Color(0xFF111613), surfaceBright = Color(0xFF363C37),
    surfaceContainerLowest = Color(0xFF0E130F), surfaceContainerLow = Color(0xFF1B211C),
    surfaceContainer = Color(0xFF202620), surfaceContainerHigh = Color(0xFF2A302B), surfaceContainerHighest = Color(0xFF343B35),
    inverseSurface = Color(0xFFE0E5DB), inverseOnSurface = Color(0xFF283028),
    outline = Color(0xFF95A292), outlineVariant = Color(0xFF465146),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
    scrim = Color.Black, surfaceTint = Color(0xFFA5D2B2)
)
val LightPalette = PracticePalette("light", lightScheme, PracticeColors(
    banner = PracticeGreen, onBanner = Color.White, bannerEdge = Color(0xFF244A37),
    transportSurface = lerp(PracticeCream, PracticeGreen, .085f),
    selectionContainer = Color(0xFFD2DDCE), onSelectionContainer = Color(0xFF193B29),
    recording = lightScheme.error, onRecording = lightScheme.onError,
    scrollThumb = PracticeGreen, scrollTrack = Color(0xFFCBD0BF),
    goals = listOf(
        GoalColors(PracticeGreen, Color(0xFFD2E8D6), Color(0xFF183B27)),
        GoalColors(Color(0xFF244E83), Color(0xFFD7E5FA), Color(0xFF153459)),
        GoalColors(Color(0xFF785414), Color(0xFFF6E3B8), Color(0xFF513600)),
        GoalColors(Color(0xFF6D3E73), Color(0xFFF0DCF2), Color(0xFF4C2551)),
        GoalColors(Color(0xFF8A3D2C), Color(0xFFF8DED5), Color(0xFF602518))
    ), unassignedGoalAccent = Color(0xFF434B43)
))
val DarkPalette = PracticePalette("dark", darkScheme, PracticeColors(
    banner = Color(0xFF223D2E), onBanner = Color(0xFFEDF5EB), bannerEdge = Color(0xFF14291D),
    transportSurface = Color(0xFF202C23),
    selectionContainer = Color(0xFF3E6149), onSelectionContainer = Color(0xFFE0F5DF),
    recording = darkScheme.error, onRecording = darkScheme.onError,
    scrollThumb = darkScheme.primary, scrollTrack = Color(0xFF465146),
    goals = listOf(
        GoalColors(Color(0xFFA5D2B2), Color(0xFF284D37), Color(0xFFC0EFCD)),
        GoalColors(Color(0xFFA8C8FA), Color(0xFF26476D), Color(0xFFD7E5FA)),
        GoalColors(Color(0xFFECC779), Color(0xFF574013), Color(0xFFF6E3B8)),
        GoalColors(Color(0xFFDAB3E0), Color(0xFF523557), Color(0xFFF0DCF2)),
        GoalColors(Color(0xFFF0B6A4), Color(0xFF66392D), Color(0xFFF8DED5))
    ), unassignedGoalAccent = Color(0xFFC0C8BE)
))
// New palettes need only a central definition and resolver/selector entry.
fun resolvePalette(choice: ThemeChoice, systemDark: Boolean): PracticePalette = when (choice) {
    ThemeChoice.LIGHT -> LightPalette
    ThemeChoice.DARK -> DarkPalette
    ThemeChoice.SYSTEM -> if (systemDark) DarkPalette else LightPalette
}
private val LocalPracticeColors = staticCompositionLocalOf { LightPalette.colors }
object PracticeTheme {
    val colors: PracticeColors @Composable get() = LocalPracticeColors.current
}
private val base = Typography()
val LatoTypography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = Lato), displayMedium = base.displayMedium.copy(fontFamily = Lato), displaySmall = base.displaySmall.copy(fontFamily = Lato),
    headlineLarge = base.headlineLarge.copy(fontFamily = Lato), headlineMedium = base.headlineMedium.copy(fontFamily = Lato), headlineSmall = base.headlineSmall.copy(fontFamily = Lato),
    titleLarge = base.titleLarge.copy(fontFamily = Lato), titleMedium = base.titleMedium.copy(fontFamily = Lato), titleSmall = base.titleSmall.copy(fontFamily = Lato),
    bodyLarge = base.bodyLarge.copy(fontFamily = Lato), bodyMedium = base.bodyMedium.copy(fontFamily = Lato), bodySmall = base.bodySmall.copy(fontFamily = Lato),
    labelLarge = base.labelLarge.copy(fontFamily = Lato), labelMedium = base.labelMedium.copy(fontFamily = Lato), labelSmall = base.labelSmall.copy(fontFamily = Lato)
)
@Composable fun PracticeTheme(choice: ThemeChoice = ThemeChoice.LIGHT,
    systemDark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val palette = resolvePalette(choice, systemDark)
    CompositionLocalProvider(LocalPracticeColors provides palette.colors) {
        MaterialTheme(colorScheme = palette.scheme, typography = LatoTypography, content = content)
    }
}
