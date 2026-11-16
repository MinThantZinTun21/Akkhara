package com.akkhara.keyboard.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class EnglishConveniencesTest {
    private val editor = FakeEditor().apply { capitalisesSentences = true }
    private val clock = FakeClock()
    private val engine = KeyboardEngine(editor, clock)
    private val typist = Typist(engine)

    private fun start(
        field: Field = Field(),
        conveniences: Conveniences = Conveniences(),
    ) = engine.startInput(field, conveniences)

    // --- automatic capitalisation ---

    @Test
    fun `the first letter of the text is capitalised`() {
        start()
        typist.typeIgnoringCase("hello")

        assertEquals("Hello", editor.text)
    }

    @Test
    fun `the keyboard shows a one-shot shift when a capital is coming`() {
        start()

        assertEquals(ShiftState.OneShot, engine.shift)
    }

    @Test
    fun `the first letter after a full stop and a space is capitalised`() {
        start()
        typist.typeIgnoringCase("hi. there")

        assertEquals("Hi. There", editor.text)
    }

    @Test
    fun `a field that does not ask for capitals gets none`() {
        editor.capitalisesSentences = false
        start()
        typist.typeIgnoringCase("hi. there")

        assertEquals("hi. there", editor.text)
    }

    @Test
    fun `capitalisation is re-evaluated when the cursor moves`() {
        start()
        typist.typeIgnoringCase("hi. there")

        editor.moveCursorTo(4) // just after "Hi. "
        engine.onCursorMoved()
        assertEquals(ShiftState.OneShot, engine.shift)

        editor.moveCursorTo(6)
        engine.onCursorMoved()
        assertEquals(ShiftState.Off, engine.shift)
    }

    @Test
    fun `deleting back to the start of a sentence brings the capital back`() {
        start()
        typist.typeIgnoringCase("a")
        typist.tap(Action.Backspace)
        typist.typeIgnoringCase("b")

        assertEquals("B", editor.text)
    }

    @Test
    fun `tapping shift at the start of a sentence turns the capital off`() {
        start()
        clock.advance(1_000)
        typist.tap(Action.Shift)
        typist.typeIgnoringCase("iphone")

        assertEquals("iphone", editor.text)
    }

    @Test
    fun `a double tap on shift at the start of a sentence locks it`() {
        start()
        clock.advance(1_000)
        typist.tap(Action.Shift)
        clock.advance(100)
        typist.tap(Action.Shift)
        typist.typeIgnoringCase("nasa")

        assertEquals("NASA", editor.text)
    }

    @Test
    fun `shift lock is left alone in the middle of a sentence`() {
        start()
        typist.typeIgnoringCase("a ")
        typist.tap(Action.Shift)
        clock.advance(100)
        typist.tap(Action.Shift)
        typist.typeIgnoringCase("nasa")

        assertEquals("A NASA", editor.text)
        assertEquals(ShiftState.Locked, engine.shift)
    }

    @Test
    fun `no automatic capitals when the setting is off`() {
        start(conveniences = Conveniences(capitaliseSentences = false))
        typist.typeIgnoringCase("hi. there")

        assertEquals("hi. there", editor.text)
    }

    // --- double-space full stop ---

    @Test
    fun `a double space after a word becomes a full stop and a space`() {
        start()
        typist.typeIgnoringCase("ok  ")

        assertEquals("Ok. ", editor.text)
    }

    @Test
    fun `a double space after a digit becomes a full stop and a space`() {
        start()
        typist.tap("4", "2")
        typist.tap(Action.Space, times = 2)

        assertEquals("42. ", editor.text)
    }

    @Test
    fun `a double space after punctuation stays two spaces`() {
        start()
        typist.typeIgnoringCase("ok")
        engine.pressAlternate(typist.charKey("."), "!")
        typist.tap(Action.Space, times = 2)

        assertEquals("Ok!  ", editor.text)
    }

    @Test
    fun `a third space after a double-space full stop is just a space`() {
        start()
        typist.typeIgnoringCase("ok   ")

        assertEquals("Ok.  ", editor.text)
    }

    @Test
    fun `spaces at the start of the text stay spaces`() {
        start()
        typist.tap(Action.Space, times = 2)

        assertEquals("  ", editor.text)
    }

    @Test
    fun `no double-space full stop when the setting is off`() {
        start(conveniences = Conveniences(doubleSpaceFullStop = false))
        typist.typeIgnoringCase("ok  ")

        assertEquals("Ok  ", editor.text)
    }

    // --- where the conveniences must not apply ---

    @Test
    fun `Myanmar gets neither convenience`() {
        start()
        typist.tap(Action.Globe)

        assertEquals(ShiftState.Off, engine.shift)

        typist.tap("က")
        typist.tap(Action.Space, times = 2)

        assertEquals("က  ", editor.text)
    }

    @Test
    fun `switching back to English at the start of a sentence brings the capital back`() {
        start()
        typist.tap(Action.Globe)
        typist.tap(Action.Globe)

        assertEquals(ShiftState.OneShot, engine.shift)
    }

    @Test
    fun `password fields get neither convenience`() {
        start(Field(isPassword = true))
        typist.typeIgnoringCase("ab  ")

        assertEquals("ab  ", editor.text)
    }

    @Test
    fun `email and web address fields get neither convenience`() {
        start(Field(kind = FieldKind.Address))
        typist.typeIgnoringCase("ab  ")

        assertEquals("ab  ", editor.text)
    }
}
