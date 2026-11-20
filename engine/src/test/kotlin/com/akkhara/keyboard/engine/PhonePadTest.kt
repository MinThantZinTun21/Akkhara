package com.akkhara.keyboard.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhonePadTest {
    private val editor = FakeEditor()
    private val engine = KeyboardEngine(editor, FakeClock(), translator = FakeTranslator())
    private val typist = Typist(engine)

    @Test
    fun `a phone field shows the digits with plus, star and hash`() {
        engine.startInput(Field(kind = FieldKind.Phone))

        assertEquals(
            listOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "#", "+").sorted(),
            typist.labels().sorted(),
        )
        assertEquals(
            setOf(Action.Backspace, Action.Enter),
            engine.rows.flatten().filterIsInstance<ActionKey>().map { it.action }.toSet(),
        )
    }

    @Test
    fun `star and hash sit either side of zero`() {
        engine.startInput(Field(kind = FieldKind.Phone))

        assertEquals(listOf("*", "0", "#"), engine.rows.last().filterIsInstance<CharKey>().map { it.text })
    }

    @Test
    fun `tapping the pad types a phone number`() {
        engine.startInput(Field(kind = FieldKind.Phone))
        typist.tap("+", "9", "5", "1", "*", "#")

        assertEquals("+951*#", editor.text)
    }

    @Test
    fun `holding zero offers plus`() {
        engine.startInput(Field(kind = FieldKind.Phone))
        val zero = typist.charKey("0")

        assertEquals(Hold.Alternates(listOf("+")), engine.hold(zero))
        engine.pressAlternate(zero, "+")
        assertEquals("+", editor.text)
    }

    @Test
    fun `holding hash offers a space`() {
        engine.startInput(Field(kind = FieldKind.Phone))
        typist.tap("1")
        val hash = typist.charKey("#")

        assertEquals(Hold.Alternates(listOf(" ")), engine.hold(hash))
        engine.pressAlternate(hash, " ")
        typist.tap("2")
        assertEquals("1 2", editor.text)
    }

    @Test
    fun `the phone pad types ordinary digits with Myanmar showing`() {
        engine.startInput(Field())
        typist.tap(Action.Globe)
        engine.startInput(Field(kind = FieldKind.Phone))
        typist.tap("0", "9")

        assertEquals("09", editor.text)
    }

    @Test
    fun `the phone pad offers no translation`() {
        engine.isTranslationAvailable = true
        engine.startInput(Field(kind = FieldKind.Phone))

        assertNull(engine.strip)
    }
}
