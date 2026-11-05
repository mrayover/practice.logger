package com.mrayover.practicelogger

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.*
import org.junit.Test

class ThemePaletteTest {
    @Test fun submarineDisabledControlsRemainDistinguishable() {
        val s = SubmarinePalette.scheme
        val disabledContainer = s.onSurface.copy(alpha = .12f).compositeOver(s.surface)
        val disabledText = s.onSurface.copy(alpha = .38f).compositeOver(disabledContainer)
        pair("submarine disabled label", disabledText, disabledContainer, 2.0)
        pair("submarine disabled outline", s.outlineVariant, s.surface, 1.5)
    }
    private fun contrast(a: Color, b: Color): Double {
        val x = a.luminance().toDouble(); val y = b.luminance().toDouble()
        return (maxOf(x,y)+.05)/(minOf(x,y)+.05)
    }
    private fun pair(name: String, a: Color, b: Color, minimum: Double = 4.5) {
        assertTrue("$name contrast ${contrast(a,b)} < $minimum", contrast(a,b) >= minimum)
    }
    @Test fun textAndControlPairsRemainReadableAcrossAllPalettes() {
        listOf(LightPalette,DarkPalette,SmoothPalette,SunnyPalette,OrchidPalette,SubmarinePalette).forEach { p ->
            val s=p.scheme; val c=p.colors
            listOf(s.onBackground to s.background, s.onSurface to s.surface,
                s.onSurfaceVariant to s.surfaceVariant, s.onPrimary to s.primary,
                s.onPrimaryContainer to s.primaryContainer, s.onSecondary to s.secondary,
                s.onSecondaryContainer to s.secondaryContainer, s.onTertiary to s.tertiary,
                s.onTertiaryContainer to s.tertiaryContainer, s.onError to s.error,
                s.onErrorContainer to s.errorContainer, s.inverseOnSurface to s.inverseSurface,
                c.onBanner to c.banner, c.onSelectionContainer to c.selectionContainer,
                c.onRecording to c.recording, c.recording to s.surface,
                s.primary to c.transportSurface, s.onSurface to c.transportSurface,
                s.onSurfaceVariant to c.transportSurface).forEachIndexed { i,(fg,bg) -> pair("${p.id} text $i",fg,bg) }
            listOf(s.surface,s.surfaceContainerLow,s.surfaceContainer,s.surfaceContainerHigh,s.surfaceContainerHighest,c.transportSurface).forEach { bg ->
                pair("${p.id} surface",s.onSurface,bg)
                pair("${p.id} scroll thumb",c.scrollThumb,bg,3.0)
                pair("${p.id} outline",s.outline,bg,3.0)
            }
            pair("${p.id} selected border",s.primary,c.selectionContainer,3.0)
            pair("${p.id} progress",s.primary,c.scrollTrack,3.0)
        }
    }
    @Test fun goalPriorityFillsAndCapOutlinesStayConsistentAcrossThemes() {
        val expected = listOf(Color(0xFFD1262D), Color(0xFFE9731C), Color(0xFFF1CB24),
            Color(0xFF34944E), Color(0xFF2D70CE))
        for (choice in ThemeChoice.entries) for (dark in listOf(false, true)) {
            val p = resolvePalette(choice, dark)
            val goal = Goal("goal", "Goal", createdAt = 0, updatedAt = 0)
            assertEquals(expected, (1..5).map { goalAccent(p.colors, GoalIndicator(goal, it)) })
            assertEquals(Color.Black, goalAccent(p.colors, null))
            assertNull(goalAccent(p.colors, GoalIndicator(goal.copy(completed = true), null)))
            pair("${p.id} cap outline", p.scheme.outline, p.scheme.surface, 3.0)
        }
    }
    @Test fun systemChoiceFollowsDeviceWhileExplicitChoicesOverrideIt() {
        for (dark in listOf(false,true)) {
            assertSame(LightPalette,resolvePalette(ThemeChoice.LIGHT,dark))
            assertSame(DarkPalette,resolvePalette(ThemeChoice.DARK,dark))
            assertSame(SmoothPalette,resolvePalette(ThemeChoice.SMOOTH,dark))
            assertSame(SunnyPalette,resolvePalette(ThemeChoice.SUNNY,dark))
            assertSame(OrchidPalette,resolvePalette(ThemeChoice.ORCHID,dark))
            assertSame(SubmarinePalette,resolvePalette(ThemeChoice.SUBMARINE,dark))
            assertSame(if(dark) DarkPalette else LightPalette,resolvePalette(ThemeChoice.SYSTEM,dark))
        }
        assertEquals(ThemeChoice.LIGHT,ThemeChoice.fromId(null))
        assertEquals(ThemeChoice.LIGHT,ThemeChoice.fromId("unknown-future-palette"))
    }
}
