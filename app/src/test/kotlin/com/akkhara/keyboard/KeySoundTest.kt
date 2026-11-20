package com.akkhara.keyboard

import android.media.AudioManager
import com.akkhara.keyboard.engine.Action
import com.akkhara.keyboard.engine.ActionKey
import com.akkhara.keyboard.engine.CharKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeySoundTest {
    @Test
    fun `space, delete and enter each play their own sound`() {
        assertEquals(KeySound.Space, KeySound.of(ActionKey(Action.Space)))
        assertEquals(KeySound.Delete, KeySound.of(ActionKey(Action.Backspace)))
        assertEquals(KeySound.Enter, KeySound.of(ActionKey(Action.Enter)))
    }

    @Test
    fun `every other key plays the letter sound`() {
        assertEquals(KeySound.Letter, KeySound.of(CharKey("a")))
        assertEquals(KeySound.Letter, KeySound.of(CharKey("က")))
        for (action in listOf(Action.Shift, Action.Globe, Action.Symbols, Action.Emoji, Action.Translate)) {
            assertEquals(action.name, KeySound.Letter, KeySound.of(ActionKey(action)))
        }
    }

    @Test
    fun `a key sound plays only when it is on and the phone is not silenced`() {
        assertTrue(KeySound.plays(isOn = true, ringerMode = AudioManager.RINGER_MODE_NORMAL))
        assertFalse(KeySound.plays(isOn = false, ringerMode = AudioManager.RINGER_MODE_NORMAL))
        assertFalse(KeySound.plays(isOn = true, ringerMode = AudioManager.RINGER_MODE_SILENT))
        assertFalse(KeySound.plays(isOn = true, ringerMode = AudioManager.RINGER_MODE_VIBRATE))
    }

    @Test
    fun `key sound is off until the user turns it on`() {
        val settings = KeyboardSettings(FakePreferences())
        assertFalse(settings.keySound)
        settings.keySound = true
        assertTrue(settings.keySound)
    }

    @Test
    fun `volume starts at 30 percent and stays within 0 to 100`() {
        val settings = KeyboardSettings(FakePreferences())
        assertEquals(30, settings.keySoundVolume)
        settings.keySoundVolume = 75
        assertEquals(75, settings.keySoundVolume)
        settings.keySoundVolume = 140
        assertEquals(100, settings.keySoundVolume)
        settings.keySoundVolume = -5
        assertEquals(0, settings.keySoundVolume)
    }

    @Test
    fun `the tone is Click until the user picks another`() {
        val settings = KeyboardSettings(FakePreferences())
        assertEquals(KeySoundTone.Click, settings.keySoundTone)
        settings.keySoundTone = KeySoundTone.Typewriter
        assertEquals(KeySoundTone.Typewriter, settings.keySoundTone)
    }

    @Test
    fun `there are four tones, each with its own four sounds`() {
        assertEquals(
            listOf(KeySoundTone.Click, KeySoundTone.SoftTap, KeySoundTone.Bubble, KeySoundTone.Typewriter),
            KeySoundTone.entries,
        )
        val sounds = KeySoundTone.entries.flatMap { tone -> KeySound.entries.map(tone::sound) }
        assertEquals(16, sounds.toSet().size)
    }
}
