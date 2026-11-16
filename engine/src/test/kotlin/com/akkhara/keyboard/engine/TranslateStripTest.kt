package com.akkhara.keyboard.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class TranslateStripTest {
    private val editor = FakeEditor("Good morning")
    private val translator = FakeTranslator()
    private val engine = KeyboardEngine(editor, FakeClock(), translator = translator)
    private val typist = Typist(engine)

    private fun spaceWidth() =
        engine.rows.last().filterIsInstance<ActionKey>().single { it.action == Action.Space }.width

    @Test
    fun `there is no translate strip until a translation model is installed`() {
        assertNull(engine.strip)
    }

    private fun hasTranslateKey() =
        engine.rows.last().any { it is ActionKey && it.action == Action.Translate }

    @Test
    fun `there is no translate key until a translation model is installed`() {
        assertFalse(hasTranslateKey())
        assertEquals(4f, spaceWidth())
    }

    @Test
    fun `with a translation model the translate key sits between the globe key and a narrower space bar`() {
        engine.isTranslationAvailable = true

        val actions = engine.rows.last().filterIsInstance<ActionKey>().map { it.action }
        val globe = actions.indexOf(Action.Globe)
        assertEquals(Action.Translate, actions[globe + 1])
        assertEquals(Action.Space, actions[globe + 2])
        assertEquals(3f, spaceWidth())
        assertEquals(StripState.Idle, engine.strip)
    }

    @Test
    fun `there is no translate key or strip in a password field`() {
        engine.isTranslationAvailable = true
        engine.startInput(Field(isPassword = true))

        assertFalse(hasTranslateKey())
        assertNull(engine.strip)
    }

    @Test
    fun `the translate key translates the text field`() {
        engine.isTranslationAvailable = true

        typist.tap(Action.Translate)

        assertEquals(listOf(request("Good morning", ModelTask.EnglishToMyanmar)), translator.requests)
        assertEquals("Good morning", editor.text)
    }

    private fun request(text: String, task: ModelTask) = FakeTranslator.Request(text, task.defaultPrompt)

    @Test
    fun `Translate translates everything in the text field, wherever the cursor is`() {
        engine.isTranslationAvailable = true
        editor.moveCursorTo(4)

        engine.translate()

        assertEquals(listOf(request("Good morning", ModelTask.EnglishToMyanmar)), translator.requests)
        assertEquals(StripState.Translating, engine.strip)
    }

    @Test
    fun `Myanmar text is translated into English, whatever the input language`() {
        val editor = FakeEditor("မင်္ဂလာပါ")
        val engine = KeyboardEngine(editor, FakeClock(), translator = translator)
        engine.isTranslationAvailable = true

        engine.translate()

        assertEquals(listOf(request("မင်္ဂလာပါ", ModelTask.MyanmarToEnglish)), translator.requests)
    }

    @Test
    fun `tapping the translation replaces the text field's text with it`() {
        engine.isTranslationAvailable = true
        editor.moveCursorTo(4)
        engine.translate()
        translator.answer("မင်္ဂလာနံနက်ခင်းပါ")
        assertEquals(StripState.Translated("မင်္ဂလာနံနက်ခင်းပါ"), engine.strip)

        engine.replaceWithResult()

        assertEquals("မင်္ဂလာနံနက်ခင်းပါ", editor.text)
        assertEquals(StripState.Idle, engine.strip)
    }

    @Test
    fun `with text selected, only the selection is translated and replaced`() {
        engine.isTranslationAvailable = true
        editor.select(5, 12)

        engine.translate()
        translator.answer("နံနက်")
        engine.replaceWithResult()

        assertEquals(listOf(request("morning", ModelTask.EnglishToMyanmar)), translator.requests)
        assertEquals("Good နံနက်", editor.text)
    }

    @Test
    fun `a translation is not put in once the text field has changed`() {
        engine.isTranslationAvailable = true
        engine.translate()
        translator.answer("မင်္ဂလာနံနက်ခင်းပါ")
        editor.commit("!") // the app changed the text by itself

        engine.replaceWithResult()

        assertEquals("Good morning!", editor.text)
        assertEquals(StripState.Idle, engine.strip)
    }

    @Test
    fun `Translate does nothing in an empty text field`() {
        val engine = KeyboardEngine(FakeEditor("  "), FakeClock(), translator = translator)
        engine.isTranslationAvailable = true

        engine.translate()

        assertEquals(emptyList<FakeTranslator.Request>(), translator.requests)
        assertEquals(StripState.Idle, engine.strip)
    }

    @Test
    fun `Translate again while translating asks only once`() {
        engine.isTranslationAvailable = true

        engine.translate()
        engine.translate()

        assertEquals(1, translator.requests.size)
    }

    @Test
    fun `text longer than the translator takes is not sent`() {
        val engine = KeyboardEngine(FakeEditor("a".repeat(501)), FakeClock(), translator = translator)
        engine.isTranslationAvailable = true

        engine.translate()

        assertEquals(emptyList<FakeTranslator.Request>(), translator.requests)
        assertEquals(StripState.Failed(TranslationFailure.TooLong), engine.strip)
    }

    @Test
    fun `a failed translation says why`() {
        engine.isTranslationAvailable = true
        engine.translate()

        translator.fail(TranslationFailure.NotInstalled)

        assertEquals(StripState.Failed(TranslationFailure.NotInstalled), engine.strip)
    }

    @Test
    fun `a translation arriving later is reported once`() {
        engine.isTranslationAvailable = true
        var reports = 0
        engine.onStripChanged = { reports++ }
        engine.translate()

        translator.answer("မင်္ဂလာနံနက်ခင်းပါ")

        assertEquals(1, reports)
    }

    @Test
    fun `typing after a translation takes the translation away, but shift does not`() {
        engine.isTranslationAvailable = true
        engine.translate()
        translator.answer("မင်္ဂလာနံနက်ခင်းပါ")

        typist.tap(Action.Shift)
        assertEquals(StripState.Translated("မင်္ဂလာနံနက်ခင်းပါ"), engine.strip)

        typist.type("S")
        assertEquals(StripState.Idle, engine.strip)
    }

    @Test
    fun `a translation of text that changed while it ran is not shown`() {
        engine.isTranslationAvailable = true
        engine.translate()
        typist.type("s")

        translator.answer("မင်္ဂလာနံနက်ခင်းပါ")

        assertEquals(StripState.Idle, engine.strip)
    }

    @Test
    fun `a translation arriving after the keyboard closed is dropped`() {
        engine.isTranslationAvailable = true
        engine.translate()
        engine.finishInput()
        engine.startInput(Field())

        translator.answer("မင်္ဂလာနံနက်ခင်းပါ")

        assertEquals(StripState.Idle, engine.strip)
    }

    @Test
    fun `a pause in typing translates nothing while translate as you type is off`() {
        engine.isTranslationAvailable = true

        engine.onTypingPaused()

        assertEquals(emptyList<FakeTranslator.Request>(), translator.requests)
    }

    @Test
    fun `with translate as you type on, a pause translates, and only once for the same text`() {
        engine.isTranslationAvailable = true
        engine.translateAsYouType = true

        engine.onTypingPaused()
        translator.answer("မင်္ဂလာနံနက်ခင်းပါ")
        engine.onTypingPaused()

        assertEquals(1, translator.requests.size)
        assertEquals(StripState.Translated("မင်္ဂလာနံနက်ခင်းပါ"), engine.strip)
        assertEquals("Good morning", editor.text) // shown only: nothing is replaced until tapped
    }

    @Test
    fun `with translate as you type on, text typed while a translation runs is translated when it returns`() {
        engine.isTranslationAvailable = true
        engine.translateAsYouType = true
        engine.onTypingPaused()
        typist.type("s")
        engine.onTypingPaused() // still translating: nothing to do yet

        translator.answer("မင်္ဂလာနံနက်ခင်းပါ")

        assertEquals(listOf("Good morning", "Good mornings"), translator.requests.map { it.text })
        assertEquals(StripState.Translating, engine.strip)
    }
}
