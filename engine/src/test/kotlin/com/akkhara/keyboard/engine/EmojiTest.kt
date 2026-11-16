package com.akkhara.keyboard.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class EmojiTest {
    private val editor = FakeEditor()
    private val engine = KeyboardEngine(editor, FakeClock())
    private val typist = Typist(engine)

    @Test
    fun `the symbols layers have the emoji key between the comma and a narrower space bar`() {
        typist.tap(Action.Symbols)
        assertEquals(listOf(",", "Emoji", "Space 4.0", "."), bottomRowMiddle())

        typist.tap(Action.MoreSymbols)
        assertEquals(listOf(",", "Emoji", "Space 4.0", "."), bottomRowMiddle())
    }

    @Test
    fun `the emoji key opens the emoji panel, and its letters key goes back to the letters`() {
        typist.tap(Action.Globe)
        typist.tap(Action.Symbols)
        typist.tap(Action.Emoji)

        assertEquals(Layer.Emoji, engine.layer)

        typist.tap(Action.Letters)

        assertEquals(Layer.Letters, engine.layer)
        assertEquals(InputLanguage.Myanmar, engine.inputLanguage)
    }

    @Test
    fun `backspace on the emoji panel deletes`() {
        editor.commit("ab")
        typist.tap(Action.Symbols)
        typist.tap(Action.Emoji)

        typist.tap(Action.Backspace)

        assertEquals("a", editor.text)
    }

    @Test
    fun `an emoji is typed as it is in either input language`() {
        openPanel()
        engine.typeEmoji("👍🏽")
        typist.tap(Action.Letters)
        typist.tap(Action.Globe)
        typist.tap("က")
        openPanel()
        engine.typeEmoji("🇲🇲")

        assertEquals(codePoints("👍🏽က🇲🇲"), codePoints(editor.text))
        assertEquals(Layer.Emoji, engine.layer)
    }

    @Test
    fun `backspace deletes a whole emoji however many code points it has`() {
        for (emoji in listOf("🇲🇲", "👨‍👩‍👧‍👦", "1️⃣", "👍🏽", "❤️", "🏳️‍🌈", "🧑🏻‍❤️‍💋‍🧑🏼", "🏴󠁧󠁢󠁳󠁣󠁴󠁿", "😀")) {
            val editor = FakeEditor("a$emoji")
            val typist = Typist(KeyboardEngine(editor, FakeClock()))

            typist.tap(Action.Backspace)

            assertEquals("after $emoji", "a", editor.text)
        }
    }

    @Test
    fun `backspace deletes one flag of two side by side`() {
        val editor = FakeEditor("🇲🇲🇹🇭")
        Typist(KeyboardEngine(editor, FakeClock())).tap(Action.Backspace)

        assertEquals("🇲🇲", editor.text)
    }

    @Test
    fun `backspace deletes a letter after an emoji, then the emoji`() {
        val editor = FakeEditor("👍🏽b")
        val typist = Typist(KeyboardEngine(editor, FakeClock()))

        typist.tap(Action.Backspace)
        assertEquals("👍🏽", editor.text)

        typist.tap(Action.Backspace)
        assertEquals("", editor.text)
    }

    @Test
    fun `holding the comma key on the letters offers the emoji key in both input languages`() {
        assertEquals(Hold.EmojiKey, engine.hold(typist.charKey(",")))

        typist.tap(Action.Globe)

        assertEquals(Hold.EmojiKey, engine.hold(typist.charKey("၊")))
    }

    @Test
    fun `the comma key on the symbols layers offers nothing, as the emoji key is beside it`() {
        typist.tap(Action.Symbols)

        assertEquals(Hold.None, engine.hold(typist.charKey(",")))
    }

    @Test
    fun `lifting on the offered emoji key opens the emoji panel and types nothing`() {
        engine.hold(typist.charKey(","))
        engine.showEmojiPanel()

        assertEquals(Layer.Emoji, engine.layer)
        assertEquals("", editor.text)
    }

    @Test
    fun `a tap on the comma key still types the comma`() {
        typist.tap(",")
        typist.tap(Action.Globe)
        typist.tap("၊")

        assertEquals(",၊", editor.text)
    }

    private fun openPanel() {
        typist.tap(Action.Symbols)
        typist.tap(Action.Emoji)
    }

    /** The bottom row without the keys at its two ends, a space bar shown with its width. */
    private fun bottomRowMiddle(): List<String> = engine.rows.last().drop(1).dropLast(1).map { key ->
        when {
            key is CharKey -> key.text
            key is ActionKey && key.action == Action.Space -> "Space ${key.width}"
            else -> (key as ActionKey).action.name
        }
    }
}
