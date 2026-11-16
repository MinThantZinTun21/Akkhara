package com.akkhara.keyboard.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TranslateStripTest {
    private val editor = FakeEditor()
    private val translator = FakeTranslator()
    private val engine = KeyboardEngine(editor, FakeClock(), translator = translator)
    private val typist = Typist(engine)

    private fun bottomRow(): List<Key> = engine.rows.last()

    private fun hasTranslateKey() = bottomRow().any { it is ActionKey && it.action == Action.Translate }

    private fun spaceWidth() = bottomRow().filterIsInstance<ActionKey>().single { it.action == Action.Space }.width

    @Test
    fun `there is no translate key until a translation model is installed`() {
        assertFalse(hasTranslateKey())
        assertEquals(4f, spaceWidth())
    }

    @Test
    fun `with a translation model the translate key sits between the globe key and a narrower space bar`() {
        engine.isTranslationAvailable = true

        val actions = bottomRow().filterIsInstance<ActionKey>().map { it.action }
        val globe = actions.indexOf(Action.Globe)
        assertEquals(Action.Translate, actions[globe + 1])
        assertEquals(Action.Space, actions[globe + 2])
        assertEquals(3f, spaceWidth())
    }

    private fun openStrip() {
        engine.isTranslationAvailable = true
        typist.tap(Action.Translate)
    }

    @Test
    fun `the translate key opens the strip, translating from the input language`() {
        assertNull(engine.strip)

        typist.tap(Action.Globe)
        openStrip()

        val strip = engine.strip!!
        assertEquals("", strip.text)
        assertEquals(TranslationDirection.MyanmarToEnglish, strip.direction)
        assertEquals(StripState.Idle, strip.state)
    }

    @Test
    fun `typing goes into the strip and leaves the text field alone`() {
        editor.commit("Hi ")
        openStrip()

        typist.type("cat")
        typist.tap(Action.Backspace)

        assertEquals("ca", engine.strip!!.text)
        assertEquals("Hi ", editor.text)
    }

    @Test
    fun `Myanmar typed into the strip is reordered just as in a text field`() {
        typist.tap(Action.Globe)
        openStrip()

        typist.tap("ေ", "က", "ာ", "်")

        assertEquals("ကော်", engine.strip!!.text)
    }

    @Test
    fun `closing the strip clears it and sends typing back to the text field`() {
        openStrip()
        typist.type("cat")

        engine.closeStrip()
        typist.type("dog")

        assertNull(engine.strip)
        assertEquals("dog", editor.text)

        typist.tap(Action.Translate)
        assertEquals("", engine.strip!!.text)
    }

    @Test
    fun `Translate with an empty strip does nothing`() {
        openStrip()

        engine.translateStrip()

        assertTrue(translator.requests.isEmpty())
        assertEquals(StripState.Idle, engine.strip!!.state)
    }

    @Test
    fun `Translate shows the strip translating, then the translation`() {
        openStrip()
        typist.type("good morning")

        engine.translateStrip()

        assertEquals(listOf(FakeTranslator.Request("good morning", TranslationDirection.EnglishToMyanmar)), translator.requests)
        assertEquals(StripState.Translating, engine.strip!!.state)

        translator.answer("မင်္ဂလာနံနက်ခင်းပါ")

        assertEquals(StripState.Translated("မင်္ဂလာနံနက်ခင်းပါ"), engine.strip!!.state)
    }

    @Test
    fun `the app hears when a translation arrives`() {
        var changes = 0
        engine.onStripChanged = { changes++ }
        openStrip()
        typist.type("hi")
        engine.translateStrip()

        translator.answer("ဟိုင်း")

        assertEquals(1, changes)
    }

    @Test
    fun `Translate does nothing while a translation is running`() {
        openStrip()
        typist.type("hi")

        engine.translateStrip()
        engine.translateStrip()

        assertEquals(1, translator.requests.size)
    }

    @Test
    fun `enter in the strip translates instead of starting a new line`() {
        openStrip()
        typist.type("hi")

        typist.tap(Action.Enter)

        assertEquals(1, translator.requests.size)
        assertEquals("hi", engine.strip!!.text)
        assertEquals(0, editor.enterActionsPerformed)
    }

    @Test
    fun `tapping the translation puts it into the text field at the cursor and closes the strip`() {
        editor.commit("Note: ")
        openStrip()
        typist.type("thank you")
        engine.translateStrip()
        translator.answer("ကျေးဇူးတင်ပါတယ်")

        engine.insertTranslation()

        assertEquals("Note: ကျေးဇူးတင်ပါတယ်", editor.text)
        assertNull(engine.strip)
    }

    @Test
    fun `there is nothing to insert before a translation has arrived`() {
        openStrip()
        typist.type("hi")
        engine.translateStrip()

        engine.insertTranslation()

        assertEquals("", editor.text)
        assertEquals(StripState.Translating, engine.strip!!.state)
    }

    @Test
    fun `typing after a translation takes the old translation away`() {
        openStrip()
        typist.type("hi")
        engine.translateStrip()
        translator.answer("ဟိုင်း")

        typist.type("s")
        engine.insertTranslation()

        assertEquals(StripState.Idle, engine.strip!!.state)
        assertEquals("", editor.text)
    }

    @Test
    fun `a failed translation says it could not translate`() {
        openStrip()
        typist.type("hi")
        engine.translateStrip()

        translator.fail()

        assertEquals(StripState.Failed(TranslationFailure.Failed), engine.strip!!.state)
    }

    @Test
    fun `a missing model says the translation model is not installed`() {
        openStrip()
        typist.type("hi")
        engine.translateStrip()

        translator.fail(TranslationFailure.NotInstalled)

        assertEquals(StripState.Failed(TranslationFailure.NotInstalled), engine.strip!!.state)
    }

    @Test
    fun `a translation that arrives after the strip closed is dropped`() {
        openStrip()
        typist.type("hi")
        engine.translateStrip()
        engine.closeStrip()

        translator.answer("ဟိုင်း")

        assertNull(engine.strip)
        assertEquals("", editor.text)
    }

    @Test
    fun `the strip takes at most 500 characters`() {
        openStrip()

        typist.type("a".repeat(499) + "bc")

        assertEquals("a".repeat(499) + "b", engine.strip!!.text)
    }

    @Test
    fun `the English conveniences do not apply in the strip`() {
        engine.startInput(Field(), Conveniences(capitaliseSentences = true, doubleSpaceFullStop = true))
        editor.capitalisesSentences = true
        openStrip()

        assertEquals(ShiftState.Off, engine.shift)
        typist.type("hi  there")

        assertEquals("hi  there", engine.strip!!.text)
    }
}
