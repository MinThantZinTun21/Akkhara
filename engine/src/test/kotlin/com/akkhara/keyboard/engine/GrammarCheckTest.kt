package com.akkhara.keyboard.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class GrammarCheckTest {
    private val editor = FakeEditor("She go to school")
    private val translator = FakeTranslator()
    private val engine = KeyboardEngine(editor, FakeClock(), translator = translator).apply { isTranslationAvailable = true }
    private val typist = Typist(engine)

    private fun KeyboardEngine.translateKey() = rows.last().single { it is ActionKey && it.action == Action.Translate }

    private fun grammarRequest(text: String) = FakeTranslator.Request(text, ModelTask.GrammarCheck, ModelTask.GrammarCheck.defaultPrompt)

    private fun engineWith(text: String) =
        KeyboardEngine(FakeEditor(text), FakeClock(), translator = translator).apply { isTranslationAvailable = true }

    @Test
    fun `holding the translate key on English text offers the grammar key`() {
        assertEquals(Hold.GrammarKey, engine.hold(engine.translateKey()))
    }

    @Test
    fun `holding the translate key on Myanmar text offers nothing, so lifting translates as a tap does`() {
        val engine = engineWith("မင်္ဂလာပါ")

        assertEquals(Hold.None, engine.hold(engine.translateKey()))
    }

    @Test
    fun `holding the translate key in an empty text field offers nothing`() {
        val engine = engineWith("")

        assertEquals(Hold.None, engine.hold(engine.translateKey()))
    }

    @Test
    fun `with English text selected among Myanmar, the grammar key is offered for the selection`() {
        val editor = FakeEditor("မင်္ဂလာပါ She go")
        val engine = KeyboardEngine(editor, FakeClock(), translator = translator).apply { isTranslationAvailable = true }
        editor.select(10, 16)

        assertEquals(Hold.GrammarKey, engine.hold(engine.translateKey()))
    }

    @Test
    fun `a grammar check sends the text field's text with the grammar prompt and leaves the field alone`() {
        engine.checkGrammar()

        assertEquals(listOf(grammarRequest("She go to school")), translator.requests)
        assertEquals(StripState.CheckingGrammar, engine.strip)
        assertEquals("She go to school", editor.text)
    }

    @Test
    fun `the correction shows, and tapping it puts it in place of the text`() {
        engine.checkGrammar()
        translator.answer("She goes to school.")
        assertEquals(StripState.Corrected("She goes to school."), engine.strip)

        engine.replaceWithResult()

        assertEquals("She goes to school.", editor.text)
        assertEquals(StripState.Idle, engine.strip)
    }

    @Test
    fun `with text selected, only the selection is checked and replaced`() {
        editor.select(0, 6)

        engine.checkGrammar()
        translator.answer("She goes")
        engine.replaceWithResult()

        assertEquals(listOf(grammarRequest("She go")), translator.requests)
        assertEquals("She goes to school", editor.text)
    }

    @Test
    fun `when nothing needs correcting the strip says so, and tapping it changes nothing`() {
        val editor = FakeEditor("She goes to school.")
        val engine = KeyboardEngine(editor, FakeClock(), translator = translator).apply { isTranslationAvailable = true }

        engine.checkGrammar()
        translator.answer(" She goes to school.\n")
        assertEquals(StripState.NoMistakes, engine.strip)

        engine.replaceWithResult()
        assertEquals("She goes to school.", editor.text)
    }

    @Test
    fun `Myanmar text is never sent for a grammar check`() {
        val engine = engineWith("မင်္ဂလာပါ")

        engine.checkGrammar()

        assertEquals(emptyList<FakeTranslator.Request>(), translator.requests)
        assertEquals(StripState.Idle, engine.strip)
    }

    @Test
    fun `text longer than is checked at once is not sent`() {
        val engine = engineWith("a ".repeat(251))

        engine.checkGrammar()

        assertEquals(emptyList<FakeTranslator.Request>(), translator.requests)
        assertEquals(StripState.Failed(TranslationFailure.TooLong), engine.strip)
    }

    @Test
    fun `nothing more is asked while a grammar check runs`() {
        engine.checkGrammar()
        engine.translate()
        engine.checkGrammar()

        assertEquals(1, translator.requests.size)
    }

    @Test
    fun `typing after a correction takes the correction away`() {
        engine.checkGrammar()
        translator.answer("She goes to school.")

        typist.type("s")

        assertEquals(StripState.Idle, engine.strip)
    }

    @Test
    fun `a correction of text that changed while it ran is not shown`() {
        engine.checkGrammar()
        typist.type("s")

        translator.answer("She goes to school.")

        assertEquals(StripState.Idle, engine.strip)
    }

    @Test
    fun `a correction is not put in once the text field has changed`() {
        engine.checkGrammar()
        translator.answer("She goes to school.")
        editor.commit("!") // the app changed the text by itself

        engine.replaceWithResult()

        assertEquals("She go to school!", editor.text)
    }
}

class GrammarCheckWithoutModelTest {
    private val editor = FakeEditor("She go to school")
    private val translator = FakeTranslator()
    private val engine = KeyboardEngine(editor, FakeClock(), translator = translator).apply {
        isTranslationAvailable = true
        isGrammarCheckAvailable = false
    }

    private fun translateKey() = engine.rows.last().single { it is ActionKey && it.action == Action.Translate }

    @Test
    fun `with no model on the phone that checks grammar, holding the translate key on English text offers nothing`() {
        assertEquals(Hold.None, engine.hold(translateKey()))
    }

    @Test
    fun `with no model on the phone that checks grammar, a grammar check asks for nothing`() {
        engine.checkGrammar()

        assertEquals(emptyList<FakeTranslator.Request>(), translator.requests)
        assertEquals(StripState.Idle, engine.strip)
    }

    @Test
    fun `with no model on the phone that checks grammar, English text is still translated`() {
        engine.translate()

        assertEquals(listOf(ModelTask.EnglishToMyanmar), translator.requests.map { it.task })
    }
}
