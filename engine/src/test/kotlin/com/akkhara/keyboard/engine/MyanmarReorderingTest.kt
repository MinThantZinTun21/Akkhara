package com.akkhara.keyboard.engine

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Key sequences as a Myanmar typist presses them, and the text that must be
 * stored. Expected text is written as code points so that the order under
 * test is the order on the page, whatever an editor's font does with it.
 */
class MyanmarReorderingTest {
    private val editor = FakeEditor()
    private val clock = FakeClock()
    private val engine = KeyboardEngine(editor, clock, inputLanguage = InputLanguage.Myanmar)
    private val typist = Typist(engine)

    private fun assertTyping(keys: String, expected: String) {
        val fresh = FakeEditor()
        val freshTypist = Typist(KeyboardEngine(fresh, FakeClock(), inputLanguage = InputLanguage.Myanmar))
        freshTypist.reach(*keys.split(' ').toTypedArray())
        assertEquals("typing $keys", codePoints(expected), codePoints(fresh.text))
    }

    private fun codePoints(text: String): String =
        text.map { "%04X".format(it.code) }.joinToString(" ")

    // --- ေ in visual order ---

    @Test
    fun `e-vowel typed before a consonant is stored after it`() {
        assertTyping("ေ က", "ကေ")
    }

    @Test
    fun `a medial typed after consonant and e-vowel goes before the e-vowel`() {
        assertTyping("ေ က ျ", "ကျေ")
        assertTyping("ေ မ ြ", "မြေ")
    }

    @Test
    fun `e-vowel typed after a consonant belongs to the next consonant`() {
        assertTyping("က ေ မ", "ကမေ")
    }

    @Test
    fun `e-vowel on its own is stored standing apart from the previous syllable`() {
        assertTyping("က ေ", "က​ေ")
    }

    @Test
    fun `a medial typed between e-vowel and its consonant still ends up in canonical order`() {
        assertTyping("ေ ြ က", "ကြေ")
    }

    // --- canonical order of marks ---

    @Test
    fun `medials are stored in canonical order whichever order they are typed`() {
        assertTyping("က ျ ွ", "ကျွ")
        assertTyping("က ွ ျ", "ကျွ")
        assertTyping("က ှ ြ", "ကြှ")
        assertTyping("က ှ ွ ြ ျ", "ကျြွှ")
    }

    @Test
    fun `vowel signs and tone marks are stored in canonical order whichever order they are typed`() {
        assertTyping("က ိ ု", "ကို")
        assertTyping("က ု ိ", "ကို")
        assertTyping("န ် ့", "န့်")
        assertTyping("န ့ ်", "န့်")
        assertTyping("က ံ ု", "ကုံ")
        assertTyping("က း ာ", "ကား")
        assertTyping("တ ့ ု ိ", "တို့")
    }

    @Test
    fun `a vowel typed before a medial goes after it`() {
        assertTyping("က ာ ြ", "ကြာ")
    }

    // --- syllable boundaries ---

    @Test
    fun `a new consonant starts a new syllable and earlier syllables are untouched`() {
        assertTyping("ေ က ာ င ် း", "ကောင်း")
    }

    @Test
    fun `a mark only reorders within the syllable it is typed into`() {
        val existing = FakeEditor("ကု") // ကု
        val laterTypist = Typist(KeyboardEngine(existing, clock, inputLanguage = InputLanguage.Myanmar))

        laterTypist.reach("န", "ု", "ိ")

        assertEquals(codePoints("ကုနို"), codePoints(existing.text))
    }

    @Test
    fun `only the text before the cursor is reordered`() {
        val existing = FakeEditor("ကု") // ကု, with the cursor moved between the two
        existing.moveCursorTo(1)
        val laterTypist = Typist(KeyboardEngine(existing, clock, inputLanguage = InputLanguage.Myanmar))

        laterTypist.reach("ိ")

        assertEquals(codePoints("ကို"), codePoints(existing.text))
    }

    @Test
    fun `reordering gives the same result after the cursor has moved away and back`() {
        typist.reach("ေ", "က")
        editor.moveCursorTo(0)
        editor.moveCursorTo(editor.text.length)
        typist.reach("ျ")

        assertEquals(codePoints("ကျေ"), codePoints(editor.text))
    }

    // --- stacked consonants and kinzi ---

    @Test
    fun `virama then a consonant stacks it under the one before`() {
        assertTyping("ပ စ ္ စ ည ် း", "ပစ္စည်း")
        assertTyping("က မ ္ ဘ ာ", "ကမ္ဘာ")
    }

    @Test
    fun `a stacked consonant in a syllable that has e-vowel lands before the e-vowel`() {
        assertTyping("ေ သ ္ တ", "သ္တေ")
    }

    @Test
    fun `marks typed after a stacked consonant attach to the stack`() {
        assertTyping("သ တ ္ တ ိ", "သတ္တိ")
    }

    @Test
    fun `kinzi typed key by key is stored as kinzi`() {
        assertTyping("အ င ် ္ ဂ ါ", "အင်္ဂါ")
    }

    // --- asat inside a syllable (contractions) ---

    @Test
    fun `asat followed by a medial or vowel stays in front of it`() {
        // ယောက်ျား and ကျွန်ုပ်
        assertTyping("ေ ယ ာ က ် ျ ာ း", "ယောက်ျား")
        assertTyping("က ျ ွ န ် ု ပ ်", "ကျွန်ုပ်")
    }

    // --- everyday words, typed in visual order ---

    @Test
    fun `everyday words come out in canonical order`() {
        assertTyping("မ ြ န ် မ ာ", "မြန်မာ")                 // မြန်မာ
        assertTyping("ေ က ျ း ဇ ူ း", "ကျေးဇူး")           // ကျေးဇူး
        assertTyping("မ င ် ္ ဂ လ ာ ပ ါ", "မင်္ဂလာပါ") // မင်္ဂလာပါ
        assertTyping("ေ န ေ က ာ င ် း", "နေကောင်း")     // နေကောင်း
    }

    // --- backspace ---

    @Test
    fun `backspace deletes one mark, not the whole syllable`() {
        typist.reach("က", "ိ", "ု")
        typist.tap(Action.Backspace)

        assertEquals(codePoints("ကိ"), codePoints(editor.text))
    }

    @Test
    fun `backspace on a lone e-vowel leaves nothing invisible behind`() {
        typist.reach("က", "ေ")
        typist.tap(Action.Backspace)

        assertEquals(codePoints("က"), codePoints(editor.text))
    }

    @Test
    fun `backspacing over marks typed ahead of their consonant leaves nothing invisible behind`() {
        typist.reach("ေ", "ြ")
        typist.tap(Action.Backspace, times = 2)

        assertEquals("", editor.text)
    }

    // --- leaving a lone ေ ---

    @Test
    fun `typing something else after a lone e-vowel leaves nothing invisible behind`() {
        typist.reach("ေ")
        typist.tap(Action.Space)

        assertEquals(codePoints("ေ "), codePoints(editor.text))
    }

    @Test
    fun `switching to English after a lone e-vowel leaves nothing invisible behind`() {
        typist.reach("ေ")
        typist.tap(Action.Globe)
        typist.tap("a")

        assertEquals(codePoints("ေa"), codePoints(editor.text))
    }

    @Test
    fun `a Myanmar digit after a lone e-vowel leaves nothing invisible behind`() {
        typist.reach("ေ", "၁")

        assertEquals(codePoints("ေ၁"), codePoints(editor.text))
    }
}
