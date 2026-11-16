package com.akkhara.keyboard.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TextFieldsTest {
    private val editor = FakeEditor()
    private val clock = FakeClock()
    private val engine = KeyboardEngine(editor, clock)
    private val typist = Typist(engine)

    @Test
    fun `enter performs the field's action when it has one`() {
        engine.startInput(Field(enterAction = EnterAction.Search))
        typist.tap("a")
        typist.tap(Action.Enter)

        assertEquals(1, editor.enterActionsPerformed)
        assertEquals("a", editor.text)
    }

    @Test
    fun `enter starts a new line when the field has no action`() {
        engine.startInput(Field(enterAction = EnterAction.NewLine))
        typist.tap(Action.Enter)

        assertEquals(0, editor.enterActionsPerformed)
        assertEquals("\n", editor.text)
    }

    @Test
    fun `the enter key reflects the field's action`() {
        engine.startInput(Field(enterAction = EnterAction.Send))

        assertEquals(EnterAction.Send, engine.enterAction)
    }

    @Test
    fun `number and phone fields open on the symbols layer`() {
        engine.startInput(Field(kind = FieldKind.Number))

        assertEquals(Layer.Symbols, engine.layer)

        typist.tap("0", "9")

        assertEquals("09", editor.text)
    }

    @Test
    fun `a text field after a number field opens on the letters`() {
        engine.startInput(Field(kind = FieldKind.Number))
        engine.startInput(Field())

        assertEquals(Layer.Letters, engine.layer)
    }

    @Test
    fun `key preview is suppressed in password fields only`() {
        engine.startInput(Field())
        assertTrue(engine.allowsKeyPreview)

        engine.startInput(Field(isPassword = true))
        assertFalse(engine.allowsKeyPreview)
    }

    @Test
    fun `a numeric password field opens on the symbols layer with no key preview`() {
        engine.startInput(Field(kind = FieldKind.Number, isPassword = true))

        assertEquals(Layer.Symbols, engine.layer)
        assertFalse(engine.allowsKeyPreview)
    }

    @Test
    fun `a new text field starts on the base layer`() {
        typist.tap(Action.Shift)
        clock.advance(100)
        typist.tap(Action.Shift)

        engine.startInput(Field())

        assertEquals(ShiftState.Off, engine.shift)
    }

    @Test
    fun `the input language carries over to the next text field`() {
        typist.tap(Action.Globe)

        engine.startInput(Field())

        assertEquals(InputLanguage.Myanmar, engine.inputLanguage)
    }
}
