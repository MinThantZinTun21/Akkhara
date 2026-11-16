package com.akkhara.keyboard.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MyanmarLayoutTest {
    private val editor = FakeEditor()
    private val clock = FakeClock()
    private val engine = KeyboardEngine(editor, clock, inputLanguage = InputLanguage.Myanmar)
    private val typist = Typist(engine)

    @Test
    fun `the base layer is the standard Myanmar layout`() {
        assertEquals(
            listOf(
                "၁ ၂ ၃ ၄ ၅ ၆ ၇ ၈ ၉ ၀",
                "ဆ တ န မ အ ပ က င သ စ",
                "ေ ျ ိ ် ါ ့ ြ ု ူ း",
                "ဖ ထ ခ လ ဘ ည ာ",
                "၊ ။",
            ),
            typist.rowLabels(),
        )
    }

    @Test
    fun `the shift layer is the standard Myanmar shift layer, with the overflow letters on its number row`() {
        typist.tap(Action.Shift)

        assertEquals(
            listOf(
                "ဍ ဎ ဋ ဟ ဧ ဩ ရ ဪ ၏ ?",
                "ဈ ဝ ဣ ၎ ဤ ၌ ဥ ၍ ဿ ဏ",
                "ဗ ှ ီ ္ ွ ံ ဲ ဒ ဓ ဂ",
                "ဇ ဌ ဃ ဠ ယ ဉ ဦ",
                "၊ ။",
            ),
            typist.rowLabels(),
        )
    }

    @Test
    fun `ရ sits above ၇`() {
        val digitRow = typist.rowLabels()[0].split(' ')
        typist.tap(Action.Shift)
        val shiftedRow = typist.rowLabels()[0].split(' ')

        assertEquals(digitRow.indexOf("၇"), shiftedRow.indexOf("ရ"))
    }

    @Test
    fun `every Burmese letter, mark, digit and punctuation sign is reachable`() {
        val burmese = buildList {
            addAll(0x1000..0x1021)          // consonants က to အ
            addAll(listOf(0x1023, 0x1024, 0x1025, 0x1026, 0x1027, 0x1029, 0x102A)) // independent vowels
            addAll(0x102B..0x1032)          // vowel signs
            addAll(0x1036..0x103A)          // anusvara, dot below, visarga, virama, asat
            addAll(0x103B..0x103E)          // medials
            add(0x103F)                     // great sa
            addAll(0x1040..0x1049)          // digits
            addAll(0x104A..0x104F)          // punctuation and symbols
        }.map { String(Character.toChars(it)) }

        val reachable = typist.labels().toMutableSet()
        typist.tap(Action.Shift)
        reachable += typist.labels()

        assertEquals(emptyList<String>(), burmese.filterNot { it in reachable })
    }

    @Test
    fun `Myanmar keys type Myanmar characters`() {
        typist.tap("မ", "န")

        assertEquals("မန", editor.text)
    }

    @Test
    fun `one-shot shift works on the Myanmar layout`() {
        typist.tap(Action.Shift)
        typist.tap("ဂ")
        typist.tap("က")

        assertEquals("ဂက", editor.text)
        assertEquals(ShiftState.Off, engine.shift)
    }

    @Test
    fun `shift lock works on the Myanmar layout`() {
        typist.tap(Action.Shift)
        clock.advance(100)
        typist.tap(Action.Shift)
        typist.tap("ဒ", "ဓ")

        assertEquals("ဒဓ", editor.text)
        assertEquals(ShiftState.Locked, engine.shift)
    }

    @Test
    fun `Myanmar punctuation is on the base layer`() {
        typist.tap("၊", "။")

        assertEquals("၊။", editor.text)
    }

    @Test
    fun `every Myanmar layer is ten keys wide at most`() {
        val layers = mutableListOf(engine.rows)
        typist.tap(Action.Shift)
        layers += engine.rows

        assertTrue(layers.flatten().all { row -> row.sumOf { it.width.toDouble() } <= 10.0 })
    }
}
