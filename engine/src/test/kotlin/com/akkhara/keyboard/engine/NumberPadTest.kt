package com.akkhara.keyboard.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NumberPadTest {
    private val editor = FakeEditor()
    private val engine = KeyboardEngine(editor, FakeClock(), translator = FakeTranslator())
    private val typist = Typist(engine)

    @Test
    fun `a number field shows the DIGITS, backspace and enter, and nothing else`() {
        engine.startInput(Field(kind = FieldKind.Number()))

        assertEquals(DIGITS, typist.labels().sorted())
        assertEquals(setOf(Action.Backspace, Action.Enter), typist.actions())
    }

    @Test
    fun `a field that takes negative numbers adds a minus key`() {
        engine.startInput(Field(kind = FieldKind.Number(allowsNegative = true)))

        assertEquals((DIGITS + "-").sorted(), typist.labels().sorted())
    }

    @Test
    fun `a field that takes decimals adds a decimal point`() {
        engine.startInput(Field(kind = FieldKind.Number(allowsDecimal = true)))

        assertEquals((DIGITS + ".").sorted(), typist.labels().sorted())
    }

    @Test
    fun `a field that takes both shows both`() {
        engine.startInput(Field(kind = FieldKind.Number(allowsNegative = true, allowsDecimal = true)))
        typist.tap("-", "1", "2", ".", "5")

        assertEquals("-12.5", editor.text)
    }

    @Test
    fun `the pad is as tall as the letters keyboard though it has fewer rows`() {
        engine.startInput(Field())
        val letters = engine.heightInRows
        engine.startInput(Field(kind = FieldKind.Number()))

        assertEquals(5, letters)
        assertEquals(letters, engine.heightInRows)
    }

    @Test
    fun `the pad is a grid of four equal columns`() {
        engine.startInput(Field(kind = FieldKind.Number()))

        assertEquals(4, engine.rows.size)
        for (row in engine.rows) {
            assertEquals(4, row.size)
            assertEquals(1, row.map { it.width }.distinct().size)
        }
        assertEquals(1, engine.rows.map { row -> row.sumOf { it.width.toDouble() } }.distinct().size)
    }

    @Test
    fun `the pad types ordinary DIGITS whichever input language is showing`() {
        typist.tap(Action.Globe)
        assertEquals(InputLanguage.Myanmar, engine.inputLanguage)

        engine.startInput(Field(kind = FieldKind.Number()))
        typist.tap("2", "0", "2", "6")

        assertEquals("2026", editor.text)
    }

    @Test
    fun `a PIN field shows the number pad with no key preview`() {
        engine.startInput(Field(kind = FieldKind.Number(), isPassword = true))

        assertEquals(DIGITS, typist.labels().sorted())
        assertFalse(engine.allowsKeyPreview)
    }

    @Test
    fun `backspace deletes and repeats while held, and enter performs the field's action`() {
        engine.startInput(Field(kind = FieldKind.Number(), enterAction = EnterAction.Done))
        typist.tap("4", "2")
        typist.tap(Action.Backspace)

        assertEquals("4", editor.text)
        assertEquals(Hold.Repeat, engine.hold(typist.actionKey(Action.Backspace)))
        assertEquals(EnterAction.Done, engine.enterAction)

        typist.tap(Action.Enter)
        assertEquals(1, editor.enterActionsPerformed)
    }

    @Test
    fun `there is no translate strip on the number pad`() {
        engine.isTranslationAvailable = true
        engine.startInput(Field(kind = FieldKind.Number()))

        assertNull(engine.strip)
    }

    @Test
    fun `a text field after a number field opens on the letters`() {
        engine.startInput(Field(kind = FieldKind.Number()))
        engine.startInput(Field())

        assertEquals(Layer.Letters, engine.layer)
        assertTrue("a" in typist.labels())
    }

    @Test
    fun `a text field after a number field brings back the Myanmar letters if they were showing`() {
        engine.startInput(Field())
        typist.tap(Action.Globe)
        engine.startInput(Field(kind = FieldKind.Number()))
        engine.startInput(Field())

        assertEquals(InputLanguage.Myanmar, engine.inputLanguage)
        assertTrue("က" in typist.labels())
    }

    @Test
    fun `a touch on a blank goes to the nearest key in its row`() {
        engine.startInput(Field(kind = FieldKind.Number()))
        val grid = KeyGrid(engine.rows, width = 400f, rowHeight = 50f, padding = 0f, gap = 0f, rowGap = 0f)

        // Bottom row, right-hand end: the cells beside 0 are empty.
        val touched = grid.cellAt(x = 399f, y = 175f)!!.key

        assertEquals(CharKey("0", width = touched.width), touched)
    }

    @Test
    fun `an empty cell types nothing`() {
        engine.startInput(Field(kind = FieldKind.Number()))
        engine.rows.flatten().filterIsInstance<Blank>().forEach(engine::press)

        assertEquals("", editor.text)
    }
}
