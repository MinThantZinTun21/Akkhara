package com.akkhara.keyboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyStyleTest {

    @Test
    fun `the keyboard draws the theme's own key style until the user picks one`() {
        assertEquals(Theme.Light.keyStyle, KeyStyle.shown(chosen = null, theme = Theme.Light))
    }

    @Test
    fun `a key style the user picks is drawn in every theme`() {
        for (theme in Theme.entries) {
            assertEquals(KeyStyle.Square, KeyStyle.shown(chosen = KeyStyle.Square, theme = theme))
        }
    }

    @Test
    fun `a new install draws each theme's own key style`() {
        assertNull(KeyboardSettings(FakePreferences()).keyStyle)
    }

    @Test
    fun `a picked key style is kept, and going back to the theme's own forgets it`() {
        val preferences = FakePreferences()
        val settings = KeyboardSettings(preferences)
        settings.keyStyle = KeyStyle.Glass
        assertEquals(KeyStyle.Glass, KeyboardSettings(preferences).keyStyle)
        settings.keyStyle = null
        assertNull(KeyboardSettings(preferences).keyStyle)
        assertTrue(preferences.values.isEmpty())
    }

    @Test
    fun `a stored key style is read back by its name`() {
        for (style in KeyStyle.entries) assertEquals(style, KeyStyle.named(style.name))
    }

    @Test
    fun `nothing stored, or a name Akkhara no longer has, means the theme's own key style`() {
        assertNull(KeyStyle.named(null))
        assertNull(KeyStyle.named("Bubbly"))
    }

    @Test
    fun `the classic and trending themes keep today's rounded keys`() {
        for (theme in Theme.entries.filter { it.group == Theme.Group.Classic || it.group == Theme.Group.Trending }) {
            assertEquals("$theme", KeyStyle.Rounded, theme.keyStyle)
        }
    }
}
