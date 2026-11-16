package com.akkhara.keyboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

class ThemesTest {

    @Test
    fun `following the system draws Light in light mode and Dark in dark mode`() {
        assertEquals(Theme.Light, Theme.shown(chosen = null, systemIsDark = false))
        assertEquals(Theme.Dark, Theme.shown(chosen = null, systemIsDark = true))
    }

    @Test
    fun `a chosen theme stays the same in light mode and dark mode`() {
        assertEquals(Theme.Dark, Theme.shown(chosen = Theme.Dark, systemIsDark = false))
        assertEquals(Theme.Light, Theme.shown(chosen = Theme.Light, systemIsDark = true))
    }

    @Test
    fun `there are ten themes, the light ones first`() {
        assertEquals(
            listOf("Light", "Sand", "Rose", "Sky", "Lavender", "Dark", "Midnight", "Forest", "Plum", "Black"),
            Theme.entries.map { it.name },
        )
        assertEquals(listOf(false, true), Theme.entries.map { it.isDark }.distinct())
    }

    @Test
    fun `no two themes look the same`() {
        assertEquals(Theme.entries.size, Theme.entries.map { it.palette }.distinct().size)
    }

    @Test
    fun `Black has a pure black background`() {
        assertEquals(BLACK, Theme.valueOf("Black").palette.background)
    }

    @Test
    fun `every label is readable on what it sits on, in every theme`() {
        for (theme in Theme.entries) {
            val palette = theme.palette
            val pairs = mapOf(
                "label on key" to (palette.label to palette.key),
                "label on pressed key" to (palette.label to palette.keyPressed),
                "label on callout" to (palette.label to palette.callout),
                "action label on action key" to (palette.actionLabel to palette.actionKey),
                "hint on key" to (palette.hint to palette.key),
                "label on accent" to (palette.onAccent to palette.accent),
            )
            for ((name, colours) in pairs) {
                val ratio = contrast(colours.first, colours.second)
                assertTrue("$theme: $name is %.2f:1, under 4.5:1".format(ratio), ratio >= 4.5)
            }
        }
    }

    @Test
    fun `the accent stands out from the keyboard's background in every theme`() {
        // Marks that are not text, such as the underline of an emoji tab, need 3:1 (WCAG 1.4.11).
        for (theme in Theme.entries) {
            val ratio = contrast(theme.palette.accent, theme.palette.background)
            assertTrue("$theme: accent on background is %.2f:1, under 3:1".format(ratio), ratio >= 3.0)
        }
    }

    @Test
    fun `a dark theme has a dark background and a light theme a light one`() {
        for (theme in Theme.entries) {
            val background = theme.palette.background
            val isDark = contrast(background, WHITE) > contrast(background, BLACK)
            assertEquals("$theme", theme.isDark, isDark)
        }
    }

    /** WCAG 2's contrast ratio of two opaque colours, from 1 to 21. */
    private fun contrast(a: Int, b: Int): Double {
        val lighter = maxOf(luminance(a), luminance(b))
        val darker = minOf(luminance(a), luminance(b))
        return (lighter + 0.05) / (darker + 0.05)
    }

    /** WCAG 2's relative luminance of an sRGB colour. */
    private fun luminance(argb: Int): Double {
        fun channel(shift: Int): Double {
            val value = (argb shr shift and 0xFF) / 255.0
            return if (value <= 0.03928) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
    }

    private companion object {
        const val WHITE = 0xFFFFFFFF.toInt()
        const val BLACK = 0xFF000000.toInt()
    }
}
