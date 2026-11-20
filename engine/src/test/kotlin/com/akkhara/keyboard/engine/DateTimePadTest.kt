package com.akkhara.keyboard.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DateTimePadTest {
    private val editor = FakeEditor()
    private val engine = KeyboardEngine(editor, FakeClock(), translator = FakeTranslator())
    private val typist = Typist(engine)

    @Test
    fun `a date field shows the digits with a slash and a dash`() {
        engine.startInput(Field(kind = FieldKind.Date))

        assertEquals((DIGITS + "/" + "-").sorted(), typist.labels().sorted())
        assertEquals(setOf(Action.Backspace, Action.Enter), typist.actions())
    }

    @Test
    fun `a date field types a date`() {
        engine.startInput(Field(kind = FieldKind.Date))
        typist.tap("2", "0", "/", "1", "1", "-", "2", "6")

        assertEquals("20/11-26", editor.text)
    }

    @Test
    fun `a time field shows the digits with a colon`() {
        engine.startInput(Field(kind = FieldKind.Time))
        typist.tap("9", ":", "3", "0")

        assertEquals((DIGITS + ":").sorted(), typist.labels().sorted())
        assertEquals(setOf(Action.Backspace, Action.Enter), typist.actions())
        assertEquals("9:30", editor.text)
    }

    @Test
    fun `a field for both a date and a time shows every separator and a space key`() {
        engine.startInput(Field(kind = FieldKind.DateAndTime))
        typist.tap("2", "0", "/", "1", "1")
        typist.tap(Action.Space)
        typist.tap("9", ":", "3", "0")

        assertEquals((DIGITS + "/" + "-" + ":").sorted(), typist.labels().sorted())
        assertEquals(setOf(Action.Backspace, Action.Enter, Action.Space), typist.actions())
        assertEquals("20/11 9:30", editor.text)
    }

    @Test
    fun `the date pad types ordinary digits with Myanmar showing`() {
        engine.startInput(Field())
        typist.tap(Action.Globe)
        engine.startInput(Field(kind = FieldKind.Date))
        typist.tap("2", "0", "/", "1", "1")

        assertEquals("20/11", editor.text)
    }

    @Test
    fun `the space key on a pad shows the sign for a space, not a language`() {
        engine.startInput(Field(kind = FieldKind.DateAndTime))

        assertEquals("\u2423", engine.spaceLabel)
    }

    @Test
    fun `the date and time pads are grids of four equal columns`() {
        for (kind in listOf(FieldKind.Date, FieldKind.Time, FieldKind.DateAndTime)) {
            engine.startInput(Field(kind = kind))

            assertEquals(4, engine.rows.size)
            for (row in engine.rows) {
                assertEquals(4, row.size)
                assertEquals(1, row.map { it.width }.distinct().size)
            }
        }
    }

    @Test
    fun `the date and time pads offer no translation`() {
        engine.isTranslationAvailable = true
        engine.startInput(Field(kind = FieldKind.DateAndTime))

        assertNull(engine.strip)
    }
}
