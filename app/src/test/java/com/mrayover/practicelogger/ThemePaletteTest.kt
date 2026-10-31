package com.mrayover.practicelogger

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.*
import org.junit.Test

class ThemePaletteTest {
    private fun contrast(a: Color, b: Color): Double {
        val x = a.luminance().toDouble(); val y = b.luminance().toDouble()
        return (maxOf(x,y)+.05)/(minOf(x,y)+.05)
    }
    private fun pair(name: String, a: Color, b: Color, minimum: Double = 4.5) {
        assertTrue("$name contrast ${contrast(a,b)} < $minimum", contrast(a,b) >= minimum)
    }
    @Test fun textAndControlPairsRemainReadableAcrossBothPalettes() {
        listOf(LightPalette,DarkPalette).forEach { p ->
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
    @Test fun goalSlotsStayDistinctAndReadableWithoutAllocatingGoalIdentity() {
        listOf(LightPalette,DarkPalette).forEach { p ->
            assertEquals(5,p.colors.goals.size)
            assertEquals(5,p.colors.goals.map { it.accent }.distinct().size)
            p.colors.goals.forEach { g ->
                pair("${p.id} Goal label",g.onContainer,g.container)
                pair("${p.id} Goal cap",g.accent,p.scheme.surface,3.0)
            }
            pair("${p.id} unassigned",p.colors.unassignedGoalAccent,p.scheme.surface,3.0)
        }
    }
    @Test fun systemChoiceFollowsDeviceWhileExplicitChoicesOverrideIt() {
        for (dark in listOf(false,true)) {
            assertSame(LightPalette,resolvePalette(ThemeChoice.LIGHT,dark))
            assertSame(DarkPalette,resolvePalette(ThemeChoice.DARK,dark))
            assertSame(if(dark) DarkPalette else LightPalette,resolvePalette(ThemeChoice.SYSTEM,dark))
        }
        assertEquals(ThemeChoice.LIGHT,ThemeChoice.fromId(null))
        assertEquals(ThemeChoice.LIGHT,ThemeChoice.fromId("unknown-future-palette"))
    }
}
