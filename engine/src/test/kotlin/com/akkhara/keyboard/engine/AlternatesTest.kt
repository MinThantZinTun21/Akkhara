package com.akkhara.keyboard.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class AlternatesTest {
    private val editor = FakeEditor()
    private val clock = FakeClock()
    private val engine = KeyboardEngine(editor, clock, inputLanguage = InputLanguage.Myanmar)
    private val typist = Typist(engine)

    private fun codePoints(text: String): String =
        text.map { "%04X".format(it.code) }.joinToString(" ")

    /** Holds the key labelled [label], slides to [alternate] and lifts. */
    private fun chooseAlternate(label: String, alternate: String) {
        val key = typist.charKey(label)
        val offered = (engine.hold(key) as Hold.Alternates).alternates
        check(alternate in offered) { "'$label' offers $offered, not '$alternate'" }
        engine.pressAlternate(key, alternate)
    }

    @Test
    fun `holding a Myanmar digit offers its Latin digit`() {
        val latin = "၁ ၂ ၃ ၄ ၅ ၆ ၇ ၈ ၉ ၀".split(' ').map { digit ->
            (engine.hold(typist.charKey(digit)) as Hold.Alternates).alternates
        }

        assertEquals("1 2 3 4 5 6 7 8 9 0".split(' ').map { listOf(it) }, latin)
    }

    @Test
    fun `choosing an alternate types it instead of the key's own character`() {
        typist.tap("၂")
        chooseAlternate("၅", "5")

        assertEquals("၂5", editor.text)
    }

    @Test
    fun `holding the Myanmar full stop offers other sentence punctuation`() {
        assertEquals(Hold.Alternates(listOf("၊", "?", "!")), engine.hold(typist.charKey("။")))
    }

    @Test
    fun `holding the English full stop offers common punctuation`() {
        typist.tap(Action.Globe)

        assertEquals(
            Hold.Alternates(listOf(",", "?", "!", "'", "\"", ":", ";")),
            engine.hold(typist.charKey(".")),
        )
    }

    @Test
    fun `kinzi is an alternate of nga and sits above the consonant typed next`() {
        typist.reach("အ")
        chooseAlternate("င", "င်္")
        typist.reach("ဂ", "လ", "ိ", "ပ", "်")

        // အင်္ဂလိပ်
        assertEquals(
            codePoints("အင်္ဂလိပ်"),
            codePoints(editor.text),
        )
    }

    @Test
    fun `kinzi chosen after an e-vowel still ends up in canonical order`() {
        typist.reach("ေ")
        chooseAlternate("င", "င်္")
        typist.reach("ဂ")

        assertEquals(codePoints("င်္ဂေ"), codePoints(editor.text))
    }

    @Test
    fun `only the agreed keys have alternates`() {
        val withAlternates = mutableSetOf<String>()
        fun collect() {
            withAlternates += engine.rows.flatten()
                .filter { engine.hold(it) is Hold.Alternates }
                .map { (it as CharKey).char }
        }
        repeat(2) {
            collect()                       // base layer
            typist.tap(Action.Shift)
            collect()                       // shift layer
            typist.tap(Action.Symbols)
            collect()
            typist.tap(Action.MoreSymbols)
            collect()
            typist.tap(Action.Letters)
            typist.tap(Action.Globe)
        }

        assertEquals(
            "၁ ၂ ၃ ၄ ၅ ၆ ၇ ၈ ၉ ၀ င ။ .".split(' ').toSet(),
            withAlternates,
        )
    }

    @Test
    fun `holding backspace asks for it to repeat`() {
        assertEquals(Hold.Repeat, engine.hold(typist.actionKey(Action.Backspace)))
    }

    @Test
    fun `an alternate uses up a one-shot shift like any other character`() {
        typist.tap(Action.Globe)
        typist.tap(Action.Shift)
        chooseAlternate(".", "?")

        assertEquals(ShiftState.Off, engine.shift)
    }
}
