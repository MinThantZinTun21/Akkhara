package com.akkhara.keyboard.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class LanguageSwitchTest {
    private val editor = FakeEditor()
    private val clock = FakeClock()
    private val engine = KeyboardEngine(editor, clock)
    private val typist = Typist(engine)

    @Test
    fun `the keyboard starts in English unless told otherwise`() {
        assertEquals(InputLanguage.English, engine.inputLanguage)
    }

    @Test
    fun `the globe key switches between English and Myanmar`() {
        typist.tap("a")
        typist.tap(Action.Globe)
        typist.tap("က")
        typist.tap(Action.Globe)
        typist.tap("b")

        assertEquals("aကb", editor.text)
    }

    @Test
    fun `the space bar shows the current input language`() {
        assertEquals("English", engine.spaceLabel)

        typist.tap(Action.Globe)

        assertEquals("မြန်မာ", engine.spaceLabel)
    }

    @Test
    fun `a keyboard opened in the input language last used shows that layout`() {
        typist.tap(Action.Globe)
        val lastUsed = engine.inputLanguage

        val reopened = KeyboardEngine(FakeEditor(), clock, inputLanguage = lastUsed)

        assertEquals(InputLanguage.Myanmar, reopened.inputLanguage)
        assertEquals("ဆ တ န မ အ ပ က င သ စ", Typist(reopened).rowLabels()[1])
    }

    @Test
    fun `the symbols layers return to the input language they were opened from`() {
        typist.tap(Action.Globe)
        typist.tap(Action.Symbols)
        typist.tap("1")
        typist.tap(Action.MoreSymbols)
        typist.tap(Action.Letters)
        typist.tap("က")

        assertEquals(InputLanguage.Myanmar, engine.inputLanguage)
        assertEquals("1က", editor.text)
    }

    @Test
    fun `switching input language does not change the number of rows`() {
        val english = engine.rows.size
        typist.tap(Action.Globe)

        assertEquals(english, engine.rows.size)
    }

    @Test
    fun `switching input language returns to the base layer`() {
        typist.tap(Action.Shift)
        clock.advance(100)
        typist.tap(Action.Shift)
        typist.tap(Action.Globe)

        assertEquals(ShiftState.Off, engine.shift)
    }

    @Test
    fun `holding the globe key asks for Android's keyboard picker`() {
        assertEquals(Hold.KeyboardPicker, engine.hold(typist.actionKey(Action.Globe)))
    }

    @Test
    fun `holding the globe key does not switch the input language`() {
        engine.hold(typist.actionKey(Action.Globe))

        assertEquals(InputLanguage.English, engine.inputLanguage)
    }

    @Test
    fun `holding an ordinary key does nothing special`() {
        assertEquals(Hold.None, engine.hold(typist.charKey("a")))
    }
}
