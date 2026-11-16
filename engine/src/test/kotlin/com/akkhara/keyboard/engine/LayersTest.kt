package com.akkhara.keyboard.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LayersTest {
    private val editor = FakeEditor()
    private val clock = FakeClock()
    private val engine = KeyboardEngine(editor, clock)
    private val typist = Typist(engine)

    @Test
    fun `English has a number row above the letters`() {
        assertEquals("1 2 3 4 5 6 7 8 9 0", typist.rowLabels()[0])

        typist.tap("4", "2")

        assertEquals("42", editor.text)
    }

    @Test
    fun `the number row stays on the shift layer`() {
        typist.tap(Action.Shift)

        assertEquals("1 2 3 4 5 6 7 8 9 0", typist.rowLabels()[0])
    }

    @Test
    fun `the symbols key opens the symbols layer`() {
        typist.tap(Action.Symbols)
        typist.tap("@", "#")

        assertEquals(Layer.Symbols, engine.layer)
        assertEquals("@#", editor.text)
    }

    @Test
    fun `typing a symbol does not leave the symbols layer`() {
        typist.tap(Action.Symbols)
        typist.tap("(", ")")
        typist.tap(Action.Space)

        assertEquals(Layer.Symbols, engine.layer)
    }

    @Test
    fun `the second symbols layer is reached from the first and leads back to it`() {
        typist.tap(Action.Symbols)
        typist.tap(Action.MoreSymbols)
        typist.tap("€")

        assertEquals(Layer.MoreSymbols, engine.layer)

        typist.tap(Action.Symbols)

        assertEquals(Layer.Symbols, engine.layer)
        assertEquals("€", editor.text)
    }

    @Test
    fun `both symbols layers return to the letters`() {
        typist.tap(Action.Symbols)
        typist.tap(Action.Letters)
        assertEquals(Layer.Letters, engine.layer)

        typist.tap(Action.Symbols)
        typist.tap(Action.MoreSymbols)
        typist.tap(Action.Letters)
        assertEquals(Layer.Letters, engine.layer)

        typist.tap("a")
        assertEquals("a", editor.text)
    }

    @Test
    fun `a one-shot shift does not survive a visit to the symbols layer`() {
        typist.tap(Action.Shift)
        typist.tap(Action.Symbols)
        typist.tap(Action.Letters)

        assertEquals(ShiftState.Off, engine.shift)
    }

    @Test
    fun `returning from the symbols layer shows the base layer even after shift lock`() {
        typist.tap(Action.Shift)
        clock.advance(100)
        typist.tap(Action.Shift)
        typist.tap(Action.Symbols)
        typist.tap("1")
        typist.tap(Action.Letters)
        typist.tap("a")

        assertEquals(ShiftState.Off, engine.shift)
        assertEquals("1a", editor.text)
    }

    @Test
    fun `every layer has the same number of rows, each ten keys wide at most`() {
        val layers = mutableListOf(engine.rows)
        typist.tap(Action.Shift)
        layers += engine.rows
        typist.tap(Action.Symbols)
        layers += engine.rows
        typist.tap(Action.MoreSymbols)
        layers += engine.rows

        assertEquals(setOf(5), layers.map { it.size }.toSet())
        assertTrue(layers.flatten().all { row -> row.sumOf { it.width.toDouble() } <= 10.0 })
    }

    @Test
    fun `space, backspace and enter work on the symbols layers`() {
        typist.tap(Action.Symbols)
        typist.tap("1")
        typist.tap(Action.Space)
        typist.tap("2", "3")
        typist.tap(Action.Backspace)
        typist.tap(Action.Enter)

        assertEquals("1 2\n", editor.text)
    }
}
