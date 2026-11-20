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
    fun `the ten classic themes come first, the light ones first`() {
        val classic = Theme.entries.takeWhile { it.group == Theme.Group.Classic }
        assertEquals(
            listOf("Light", "Sand", "Rose", "Sky", "Lavender", "Dark", "Midnight", "Forest", "Plum", "Black"),
            classic.map { it.name },
        )
        assertEquals(listOf(false, true), classic.map { it.isDark }.distinct())
        assertEquals(5, classic.count { it.isDark })
    }

    @Test
    fun `the trending themes follow the classic ones, the light ones first`() {
        val trending = Theme.entries.dropWhile { it.group == Theme.Group.Classic }.takeWhile { it.group == Theme.Group.Trending }
        assertEquals(
            listOf(
                "Moss", "Prism", "Marquee", "Saffron", "Jade", "Sonata", "Encore", "Crayon", "Wander", "Pond",
                "Cinder", "Lamplight", "Neon", "Lagoon", "Comet", "Umber", "Potion", "Lumen", "Nightshift", "Blaze",
            ),
            trending.map { it.name },
        )
        assertTrue(trending.all { it.group == Theme.Group.Trending })
        assertEquals(listOf(false, true), trending.map { it.isDark }.distinct())
        assertEquals(10, trending.count { it.isDark })
    }

    @Test
    fun `the twenty designed themes come last`() {
        val designed = Theme.entries.dropWhile { it.group != Theme.Group.Designed }
        assertEquals(
            listOf(
                "Atelier", "Gasp", "Rally", "Mesa", "Pasture", "Hearth", "Bayou", "Kiln", "Lacquer", "Orbit",
                "Sherbet", "Scroll", "Caper", "Borough", "Sepia", "Zenith", "Atomic", "Overcast", "Sirocco", "Petal",
            ),
            designed.map { it.name },
        )
        assertTrue(designed.all { it.group == Theme.Group.Designed })
    }

    @Test
    fun `every key style is the own style of at least two designed themes`() {
        val designed = Theme.entries.filter { it.group == Theme.Group.Designed }
        for (style in KeyStyle.entries) {
            assertTrue("$style", designed.count { it.keyStyle == style } >= 2)
        }
    }

    @Test
    fun `no two themes look the same`() {
        assertEquals(Theme.entries.size, Theme.entries.map { it.palette }.distinct().size)
    }

    @Test
    fun `a gradient background is checked at its top, middle and bottom`() {
        val palette = Theme.Light.palette.copy(background = WHITE, backgroundBottom = BLACK)
        assertEquals(listOf(WHITE, 0xFF808080.toInt(), BLACK), palette.backgroundsAlongHeight)
    }

    @Test
    fun `a one-colour background is checked once`() {
        assertEquals(listOf(Theme.Light.palette.background), Theme.Light.palette.backgroundsAlongHeight)
    }

    @Test
    fun `Black has a pure black background`() {
        assertEquals(BLACK, Theme.Black.palette.background)
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
    fun `every label is readable in every key style, in every theme`() {
        for (theme in Theme.entries) {
            val palette = theme.palette
            for (style in KeyStyle.entries) {
                // In a see-through style an action key is drawn in the letter keys' colours.
                val action = if (style.isSeeThrough) palette.label to palette.key else palette.actionLabel to palette.actionKey
                val pairs = mapOf(
                    "label on key" to (palette.label to palette.key),
                    "hint on key" to (palette.hint to palette.key),
                    "action label on action key" to action,
                )
                for ((name, colours) in pairs) for (background in palette.backgroundsAlongHeight) {
                    val face = style.face(colours.second, background)
                    val ratio = contrast(colours.first, face)
                    assertTrue("$theme, $style: $name is %.2f:1, under 4.5:1".format(ratio), ratio >= 4.5)
                }
            }
        }
    }

    @Test
    fun `the accent stands out from the keyboard's background in every theme`() {
        // Marks that are not text, such as the underline of an emoji tab, need 3:1 (WCAG 1.4.11).
        for (theme in Theme.entries) for (background in theme.palette.backgroundsAlongHeight) {
            val ratio = contrast(theme.palette.accent, background)
            assertTrue("$theme: accent on background is %.2f:1, under 3:1".format(ratio), ratio >= 3.0)
        }
    }

    @Test
    fun `a dark theme has a dark background and a light theme a light one`() {
        for (theme in Theme.entries) for (background in theme.palette.backgroundsAlongHeight) {
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
