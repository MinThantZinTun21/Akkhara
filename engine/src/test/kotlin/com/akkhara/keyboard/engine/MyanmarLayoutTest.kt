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
    fun `the base layer has the keys of the Myanmar layout people know from their phone`() {
        assertEquals(
            listOf(
                "ဈ ဝ ဋ ုံ ော ော် ရ ဂ ဟ ၏",
                "ဆ တ န မ အ ပ က င သ စ",
                "ေ ျ ိ ် ါ ့ ြ ု ူ း",
                "ဖ ထ ခ လ ဘ ည ာ ယ",
                "၊ ။",
            ),
            typist.rowLabels(),
        )
    }

    @Test
    fun `the shift layer has the Myanmar digits on its top row and the rarer letters below`() {
        typist.tap(Action.Shift)

        assertEquals(
            listOf(
                "၁ ၂ ၃ ၄ ၅ ၆ ၇ ၈ ၉ ၀",
                "ဍ ဏ္ဍ ဣ ၎င်း ဤ ၌ ဥ ၍ ဿ ဏ",
                "ဗ ှ ီ ္ ွ ံ ဲ ဒ ဓ ဏ္ဌ",
                "ဇ ဌ ဃ ဠ ဎ ဉ ဦ ဧ",
                "၊ ။",
            ),
            typist.rowLabels(),
        )
    }

    @Test
    fun `the comma sits between the symbols key and the globe key`() {
        val bottomRow = engine.rows.last()

        assertEquals(
            listOf(ActionKey(Action.Symbols, 1.5f), CharKey("၊"), ActionKey(Action.Globe)),
            bottomRow.take(3),
        )
    }

    @Test
    fun `the top row shows a Latin digit as the hint on each key, and holding the key offers it`() {
        val topRow = engine.rows.first().filterIsInstance<CharKey>()

        assertEquals("1 2 3 4 5 6 7 8 9 0", topRow.joinToString(" ") { it.hint.orEmpty() })
        assertEquals(Hold.Alternates(listOf("7")), engine.hold(typist.charKey("ရ")))
    }

    @Test
    fun `the Myanmar digits on the shift layer show their Latin digit as the hint`() {
        typist.tap(Action.Shift)
        val topRow = engine.rows.first().filterIsInstance<CharKey>()

        assertEquals("1 2 3 4 5 6 7 8 9 0", topRow.joinToString(" ") { it.hint.orEmpty() })
    }

    @Test
    fun `every Burmese letter, mark, digit and punctuation sign can be typed with a tap or a hold`() {
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

        val offered = offeredOnShowingLayer().toMutableSet()
        typist.tap(Action.Shift)
        offered += offeredOnShowingLayer()

        assertEquals(emptyList<String>(), burmese.filterNot { it in offered })
    }

    /** Every character a key on the showing layer types by itself, by a tap or as an alternate. */
    private fun offeredOnShowingLayer(): List<String> =
        engine.rows.flatten().filterIsInstance<CharKey>().flatMap { listOf(it.text) + it.alternates }

    @Test
    fun `a key that shows a vowel with asat types all of it onto the consonant`() {
        typist.tap("က", "ော်")

        assertEquals(codePoints("ကော်"), codePoints(editor.text))
    }

    @Test
    fun `the key for ော types both vowel signs in canonical order`() {
        typist.tap("မ", "ြ", "ော")

        assertEquals(codePoints("မြော"), codePoints(editor.text))
    }

    @Test
    fun `the key for ော after an e-vowel typed ahead of its consonant does not double the e-vowel`() {
        typist.tap("ေ", "ော", "က")

        assertEquals(codePoints("ကော"), codePoints(editor.text))
    }

    @Test
    fun `the key for ော after an e-vowel already on the consonant does not double the e-vowel`() {
        typist.tap("ေ", "က", "ော")

        assertEquals(codePoints("ကော"), codePoints(editor.text))
    }

    @Test
    fun `the key for ော် after kinzi and its consonant keeps its own asat`() {
        engine.pressAlternate(typist.charKey("င"), KINZI)
        typist.tap("ဂ", "ော်")

        assertEquals(codePoints("င်္ဂော်"), codePoints(editor.text))
    }

    @Test
    fun `a key for a stacked consonant after a virama keeps its own virama`() {
        typist.tap("က")
        typist.tapThroughShift("္", "ဏ္ဍ")

        assertEquals(codePoints("က္ဏ္ဍ"), codePoints(editor.text))
    }

    @Test
    fun `the key for ုံ types the vowel and the anusvara`() {
        typist.tap("တ", "ုံ", "း")

        assertEquals(codePoints("တုံး"), codePoints(editor.text))
    }

    @Test
    fun `the keys for whole stacked consonants type them stacked`() {
        typist.tap("က")
        typist.tapThroughShift("ဏ္ဍ")

        assertEquals(codePoints("ကဏ္ဍ"), codePoints(editor.text))
    }

    @Test
    fun `the key for ၎င်း types all of it`() {
        typist.tapThroughShift("၎င်း")

        assertEquals(codePoints("၎င်း"), codePoints(editor.text))
    }

    @Test
    fun `backspace after a key that typed several characters takes them away one at a time`() {
        typist.tap("က", "ော်")
        typist.tap(Action.Backspace)

        assertEquals(codePoints("ကော"), codePoints(editor.text))
    }

    @Test
    fun `the key that opens the symbols layer is labelled in Myanmar digits`() {
        assertEquals("?၁၂၃", engine.symbolsLabel)
    }

    @Test
    fun `Myanmar keys type Myanmar characters`() {
        typist.tap("မ", "န")

        assertEquals("မန", editor.text)
    }

    @Test
    fun `one-shot shift works on the Myanmar layout`() {
        typist.tap(Action.Shift)
        typist.tap("ဗ")
        typist.tap("က")

        assertEquals("ဗက", editor.text)
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
