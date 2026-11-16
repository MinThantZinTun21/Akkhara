package com.akkhara.keyboard.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class EnglishTypingTest {
    private val editor = FakeEditor()
    private val engine = KeyboardEngine(editor)
    private val typist = Typist(engine)

    @Test
    fun `letter keys type their letters`() {
        typist.tap("h", "i")

        assertEquals("hi", editor.text)
    }

    @Test
    fun `the space bar types a space`() {
        typist.type("a b")

        assertEquals("a b", editor.text)
    }

    @Test
    fun `backspace deletes the character before the cursor`() {
        typist.type("cat")
        typist.tap(Action.Backspace)

        assertEquals("ca", editor.text)
    }

    @Test
    fun `backspace on an empty text field does nothing`() {
        typist.tap(Action.Backspace)

        assertEquals("", editor.text)
    }

    @Test
    fun `backspace deletes at the cursor, not at the end`() {
        typist.type("cat")
        editor.moveCursorTo(1)
        typist.tap(Action.Backspace)

        assertEquals("at", editor.text)
    }

    @Test
    fun `backspace deletes the selection when there is one`() {
        typist.type("carpet")
        editor.select(1, 5)
        typist.tap(Action.Backspace)

        assertEquals("ct", editor.text)
    }

    @Test
    fun `backspace deletes a whole emoji, not half of one`() {
        val withEmoji = FakeEditor("ok\uD83D\uDE00")
        Typist(KeyboardEngine(withEmoji)).tap(Action.Backspace)

        assertEquals("ok", withEmoji.text)
    }

    @Test
    fun `typing replaces the selection`() {
        typist.type("cat")
        editor.select(0, 1)
        typist.tap("b")

        assertEquals("bat", editor.text)
    }

    @Test
    fun `enter starts a new line`() {
        typist.tap("a")
        typist.tap(Action.Enter)
        typist.tap("b")

        assertEquals("a\nb", editor.text)
    }

    @Test
    fun `the base layer is QWERTY`() {
        assertEquals(
            listOf("q w e r t y u i o p", "a s d f g h j k l", "z x c v b n m"),
            typist.rowLabels().subList(1, 4),
        )
    }
}
