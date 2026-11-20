package com.akkhara.keyboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RememberTypingTest {
    @Test
    fun `remember what I type is off until the user turns it on`() {
        val settings = KeyboardSettings(FakePreferences())
        assertFalse(settings.rememberTyping)
        settings.rememberTyping = true
        assertTrue(settings.rememberTyping)
    }

    @Test
    fun `the open keyboard hears the switch change, and no other`() {
        val settings = KeyboardSettings(FakePreferences())
        var changes = 0
        val stop = settings.watchRememberTyping { changes++ }
        settings.rememberTyping = true
        settings.keySound = true
        assertEquals(1, changes)
        stop()
        settings.rememberTyping = false
        assertEquals(1, changes)
    }
}
