package com.mrayover.practicelogger

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.*

val Lato = FontFamily(Font(R.font.lato_regular, FontWeight.Normal), Font(R.font.lato_bold, FontWeight.Bold))
// Raw palette primitives stay here; screens consume semantic roles.
private val PracticeCream = Color(0xFFF1E6D3)
private val PracticeGreen = Color(0xFF315C47)
// Priority order is also the historical v1 color mapping.
private val GoalPriorityColors = listOf(
    Color(0xFFD1262D), Color(0xFFE9731C), Color(0xFFF1CB24), Color(0xFF34944E), Color(0xFF2D70CE)
)

internal fun goalCompletionColor(priority: Int?): Int? = priority?.let { GoalPriorityColors[it - 1].toArgb() }

@Immutable data class PracticeColors(
    val banner: Color, val onBanner: Color, val bannerEdge: Color,
    val transportSurface: Color, val selectionContainer: Color, val onSelectionContainer: Color,
    val recording: Color, val onRecording: Color, val scrollThumb: Color, val scrollTrack: Color,
    val goalPriorities: List<Color> = GoalPriorityColors,
    val unassignedGoalAccent: Color = Color.Black
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
    scrollThumb = PracticeGreen, scrollTrack = Color(0xFFCBD0BF)
))
val DarkPalette = PracticePalette("dark", darkScheme, PracticeColors(
    banner = Color(0xFF223D2E), onBanner = Color(0xFFEDF5EB), bannerEdge = Color(0xFF14291D),
    transportSurface = Color(0xFF202C23),
    selectionContainer = Color(0xFF3E6149), onSelectionContainer = Color(0xFFE0F5DF),
    recording = darkScheme.error, onRecording = darkScheme.onError,
    scrollThumb = darkScheme.primary, scrollTrack = Color(0xFF465146)
))
// Smooth shares semantic state behavior with Dark, using a blue-green palette.
private val smoothScheme = darkScheme.copy(
    primary = Color(0xFF8BD8D1), onPrimary = Color(0xFF003A38),
    primaryContainer = Color(0xFF15544F), onPrimaryContainer = Color(0xFFB5F2EB), inversePrimary = Color(0xFF006A64),
    secondary = Color(0xFFADCCC8), onSecondary = Color(0xFF183835),
    secondaryContainer = Color(0xFF304F4B), onSecondaryContainer = Color(0xFFD0E8E4),
    tertiary = Color(0xFFA5CDDC), onTertiary = Color(0xFF103744),
    tertiaryContainer = Color(0xFF294F5D), onTertiaryContainer = Color(0xFFC3EAF7),
    background = Color(0xFF101E20), onBackground = Color(0xFFE1ECEA),
    surface = Color(0xFF101E20), onSurface = Color(0xFFE1ECEA),
    surfaceVariant = Color(0xFF354A49), onSurfaceVariant = Color(0xFFC0D3D0),
    surfaceDim = Color(0xFF0A1719), surfaceBright = Color(0xFF304042),
    surfaceContainerLowest = Color(0xFF081416), surfaceContainerLow = Color(0xFF142527),
    surfaceContainer = Color(0xFF192C2E), surfaceContainerHigh = Color(0xFF233638), surfaceContainerHighest = Color(0xFF2E4143),
    inverseSurface = Color(0xFFD7E7E4), inverseOnSurface = Color(0xFF213331),
    outline = Color(0xFF8AA6A2), outlineVariant = Color(0xFF3C5551), surfaceTint = Color(0xFF8BD8D1)
)
val SmoothPalette = PracticePalette("smooth", smoothScheme, PracticeColors(
    banner = Color(0xFF123F40), onBanner = Color(0xFFDFF7F3), bannerEdge = Color(0xFF092C2E),
    transportSurface = Color(0xFF172D2E),
    selectionContainer = Color(0xFF315B58), onSelectionContainer = Color(0xFFD5F5EE),
    recording = smoothScheme.error, onRecording = smoothScheme.onError,
    scrollThumb = smoothScheme.primary, scrollTrack = Color(0xFF35534F)
))
// Sunny uses warm light surfaces with amber/orange actions and readable dark text.
private val sunnyScheme = lightScheme.copy(
    primary = Color(0xFF8C4300), onPrimary = Color(0xFFFFF8E8),
    primaryContainer = Color(0xFFFFD875), onPrimaryContainer = Color(0xFF482200), inversePrimary = Color(0xFFFFB86B),
    secondary = Color(0xFF715500), onSecondary = Color(0xFFFFF8E8),
    secondaryContainer = Color(0xFFFFE699), onSecondaryContainer = Color(0xFF3B2C00),
    tertiary = Color(0xFF8A3D24), onTertiary = Color(0xFFFFF8F0),
    tertiaryContainer = Color(0xFFFFD9BC), onTertiaryContainer = Color(0xFF48200D),
    background = Color(0xFFFFF4CD), onBackground = Color(0xFF332413),
    surface = Color(0xFFFFF4CD), onSurface = Color(0xFF332413),
    surfaceVariant = Color(0xFFF8E4AC), onSurfaceVariant = Color(0xFF604C2E),
    surfaceDim = Color(0xFFECD9A6), surfaceBright = Color(0xFFFFF8E3),
    surfaceContainerLowest = Color(0xFFFFFBED), surfaceContainerLow = Color(0xFFFFF0C0),
    surfaceContainer = Color(0xFFFFEAB0), surfaceContainerHigh = Color(0xFFFFE39B), surfaceContainerHighest = Color(0xFFF6D78F),
    inverseSurface = Color(0xFF3D2E1C), inverseOnSurface = Color(0xFFFFEFCC),
    outline = Color(0xFF80643A), outlineVariant = Color(0xFFD6BB7B), surfaceTint = Color(0xFF8C4300)
)
val SunnyPalette = PracticePalette("sunny", sunnyScheme, PracticeColors(
    banner = Color(0xFF994800), onBanner = Color(0xFFFFF8E8), bannerEdge = Color(0xFF733500),
    transportSurface = Color(0xFFFFE5A3),
    selectionContainer = Color(0xFFFFD875), onSelectionContainer = Color(0xFF482200),
    recording = sunnyScheme.error, onRecording = sunnyScheme.onError,
    scrollThumb = sunnyScheme.primary, scrollTrack = Color(0xFFE9C981)
))
// Orchid pairs dark plum surfaces with light lavender actions.
private val orchidScheme = darkScheme.copy(
    primary = Color(0xFFD8B9FF), onPrimary = Color(0xFF3D1D5C),
    primaryContainer = Color(0xFF533573), onPrimaryContainer = Color(0xFFF0DDFF), inversePrimary = Color(0xFF70508F),
    secondary = Color(0xFFD3C0E0), onSecondary = Color(0xFF382A45),
    secondaryContainer = Color(0xFF50405D), onSecondaryContainer = Color(0xFFEFDCF9),
    tertiary = Color(0xFFF0B7D3), onTertiary = Color(0xFF4B243A),
    tertiaryContainer = Color(0xFF653A51), onTertiaryContainer = Color(0xFFFFD8EA),
    background = Color(0xFF1B1622), onBackground = Color(0xFFECE3F2),
    surface = Color(0xFF1B1622), onSurface = Color(0xFFECE3F2),
    surfaceVariant = Color(0xFF49404F), onSurfaceVariant = Color(0xFFD2C4DC),
    surfaceDim = Color(0xFF15101C), surfaceBright = Color(0xFF3B3342),
    surfaceContainerLowest = Color(0xFF100C16), surfaceContainerLow = Color(0xFF211B29),
    surfaceContainer = Color(0xFF271F30), surfaceContainerHigh = Color(0xFF31283B), surfaceContainerHighest = Color(0xFF3C3346),
    inverseSurface = Color(0xFFE8DEEF), inverseOnSurface = Color(0xFF342B3E),
    outline = Color(0xFFA394B0), outlineVariant = Color(0xFF51465D), surfaceTint = Color(0xFFD8B9FF)
)
val OrchidPalette = PracticePalette("orchid", orchidScheme, PracticeColors(
    banner = Color(0xFF392647), onBanner = Color(0xFFF5E8FF), bannerEdge = Color(0xFF271A33),
    transportSurface = Color(0xFF2A2034),
    selectionContainer = Color(0xFF57406F), onSelectionContainer = Color(0xFFF5E5FF),
    recording = orchidScheme.error, onRecording = orchidScheme.onError,
    scrollThumb = orchidScheme.primary, scrollTrack = Color(0xFF51405F)
))
// Red night lighting; pale accents preserve selected outlines on instrument-red fills.
private val submarineScheme = darkScheme.copy(
    primary = Color(0xFFF0B1A5), onPrimary = Color(0xFF3B1017),
    primaryContainer = Color(0xFFB44A4D), onPrimaryContainer = Color(0xFFFFF5F0), inversePrimary = Color(0xFF8B303A),
    secondary = Color(0xFFE9AAA5), onSecondary = Color(0xFF3B1017),
    secondaryContainer = Color(0xFF743139), onSecondaryContainer = Color(0xFFFFDAD2),
    tertiary = Color(0xFFF4BEA4), onTertiary = Color(0xFF401B12),
    tertiaryContainer = Color(0xFF663029), onTertiaryContainer = Color(0xFFFFDCCB),
    background = Color(0xFF140C0D), onBackground = Color(0xFFF0B1A5),
    surface = Color(0xFF140C0D), onSurface = Color(0xFFF0B1A5),
    surfaceVariant = Color(0xFF2D1518), onSurfaceVariant = Color(0xFFE5ABA5),
    surfaceDim = Color(0xFF100809), surfaceBright = Color(0xFF432126),
    surfaceContainerLowest = Color(0xFF0E0708), surfaceContainerLow = Color(0xFF211013),
    surfaceContainer = Color(0xFF2D1518), surfaceContainerHigh = Color(0xFF371B20), surfaceContainerHighest = Color(0xFF432126),
    inverseSurface = Color(0xFFF0B1A5), inverseOnSurface = Color(0xFF3B1017),
    outline = Color(0xFFCD797A), outlineVariant = Color(0xFF743139),
    error = Color(0xFFFFC29A), onError = Color(0xFF4A1700),
    errorContainer = Color(0xFF6B291B), onErrorContainer = Color(0xFFFFDCCB),
    surfaceTint = Color(0xFFF0B1A5)
)
val SubmarinePalette = PracticePalette("submarine", submarineScheme, PracticeColors(
    banner = Color(0xFFB44A4D), onBanner = Color(0xFFFFF5F0), bannerEdge = Color(0xFF743139),
    transportSurface = Color(0xFF2D1518),
    selectionContainer = Color(0xFF743139), onSelectionContainer = Color(0xFFFFDAD2),
    recording = submarineScheme.error, onRecording = submarineScheme.onError,
    scrollThumb = submarineScheme.primary, scrollTrack = Color(0xFF743139)
))
// New palettes need only a central definition and resolver/selector entry.
fun resolvePalette(choice: ThemeChoice, systemDark: Boolean): PracticePalette = when (choice) {
    ThemeChoice.LIGHT -> LightPalette
    ThemeChoice.DARK -> DarkPalette
    ThemeChoice.SMOOTH -> SmoothPalette
    ThemeChoice.SUNNY -> SunnyPalette
    ThemeChoice.ORCHID -> OrchidPalette
    ThemeChoice.SUBMARINE -> SubmarinePalette
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
