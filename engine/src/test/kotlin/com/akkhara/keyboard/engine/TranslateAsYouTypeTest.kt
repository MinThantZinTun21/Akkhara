package com.akkhara.keyboard.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class TranslateAsYouTypeTest {
    private val editor = FakeEditor()
    private val translator = FakeTranslator()
    private val engine = KeyboardEngine(editor, FakeClock(), translator = translator)
    private val typist = Typist(engine)

    private fun openStrip() {
        engine.isTranslationAvailable = true
        typist.tap(Action.Translate)
    }

    @Test
    fun `a pause in typing translates nothing while translate as you type is off`() {
        openStrip()
        typist.type("cat")

        engine.onTypingPaused()

        assertEquals(emptyList<FakeTranslator.Request>(), translator.requests)
    }

    @Test
    fun `with translate as you type on, a pause translates the strip`() {
        engine.translateAsYouType = true
        openStrip()
        typist.type("cat")

        engine.onTypingPaused()

        assertEquals(listOf(FakeTranslator.Request("cat", TranslationDirection.EnglishToMyanmar)), translator.requests)
        assertEquals(StripState.Translating, engine.strip!!.state)
    }

    @Test
    fun `a pause does not translate the same text again`() {
        engine.translateAsYouType = true
        openStrip()
        typist.type("cat")
        engine.onTypingPaused()
        translator.answer("ကြောင်")

        engine.onTypingPaused()

        assertEquals(1, translator.requests.size)
        assertEquals(StripState.Translated("ကြောင်"), engine.strip!!.state)
    }

    @Test
    fun `text typed while a translation runs is translated once that translation returns`() {
        engine.translateAsYouType = true
        openStrip()
        typist.type("cat")
        engine.onTypingPaused()
        typist.type("s")
        engine.onTypingPaused() // still translating "cat": nothing to do yet

        translator.answer("ကြောင်")

        assertEquals(listOf("cat", "cats"), translator.requests.map { it.text })
        assertEquals(StripState.Translating, engine.strip!!.state)
    }
}
