package com.akkhara.keyboard.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class ShiftTest {
    private val editor = FakeEditor()
    private val clock = FakeClock()
    private val engine = KeyboardEngine(editor, clock)
    private val typist = Typist(engine)

    /** A tap on shift, then long enough that the next tap is not a double tap. */
    private fun tapShiftOnce() {
        typist.tap(Action.Shift)
        clock.advance(1_000)
    }

    private fun doubleTapShift() {
        typist.tap(Action.Shift)
        clock.advance(100)
        typist.tap(Action.Shift)
        clock.advance(1_000)
    }

    @Test
    fun `one tap on shift capitalises exactly the next letter`() {
        tapShiftOnce()
        typist.tap("H")
        typist.tap("i")

        assertEquals("Hi", editor.text)
        assertEquals(ShiftState.Off, engine.shift)
    }

    @Test
    fun `letter keys show capitals while the shift layer is active`() {
        tapShiftOnce()

        assertEquals("Q W E R T Y U I O P", typist.rowLabels()[1])
        assertEquals(ShiftState.OneShot, engine.shift)
    }

    @Test
    fun `a double tap on shift locks it until shift is tapped again`() {
        doubleTapShift()
        typist.tap("O", "K")

        assertEquals(ShiftState.Locked, engine.shift)

        typist.tap(Action.Shift)
        typist.tap("o", "k")

        assertEquals("OKok", editor.text)
        assertEquals(ShiftState.Off, engine.shift)
    }

    @Test
    fun `a second slow tap on shift returns to the base layer`() {
        tapShiftOnce()
        tapShiftOnce()

        assertEquals(ShiftState.Off, engine.shift)
        assertEquals("q w e r t y u i o p", typist.rowLabels()[1])
    }

    @Test
    fun `two quick taps separated by a typed letter do not lock shift`() {
        typist.tap(Action.Shift)
        typist.tap("A")
        typist.tap(Action.Shift)
        clock.advance(1_000)

        assertEquals(ShiftState.OneShot, engine.shift)
    }

    @Test
    fun `space and backspace do not use up a one-shot shift`() {
        tapShiftOnce()
        typist.tap(Action.Space)
        typist.tap(Action.Backspace)
        typist.tap("A")

        assertEquals("A", editor.text)
    }

    @Test
    fun `a digit uses up a one-shot shift`() {
        tapShiftOnce()
        typist.tap("1")
        typist.tap("a")

        assertEquals("1a", editor.text)
    }
}
