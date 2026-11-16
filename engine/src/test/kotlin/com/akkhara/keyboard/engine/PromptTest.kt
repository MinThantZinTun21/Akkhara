package com.akkhara.keyboard.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class PromptTest {
    private val translator = FakeTranslator()

    private fun engineWith(text: String, prompts: Map<ModelTask, String>) =
        KeyboardEngine(FakeEditor(text), FakeClock(), translator = translator).apply {
            isTranslationAvailable = true
            customPrompts = prompts
        }

    @Test
    fun `the user's prompt is sent for its own task, and the default for the others`() {
        val prompts = mapOf(ModelTask.EnglishToMyanmar to "Translate politely into Myanmar.")

        engineWith("Good morning", prompts).translate()
        engineWith("မင်္ဂလာပါ", prompts).translate()

        assertEquals(
            listOf("Translate politely into Myanmar.", ModelTask.MyanmarToEnglish.defaultPrompt),
            translator.requests.map { it.prompt },
        )
    }

    @Test
    fun `the user's grammar prompt is sent with a grammar check`() {
        engineWith("She go", mapOf(ModelTask.GrammarCheck to "Fix only spelling.")).checkGrammar()

        assertEquals("Fix only spelling.", translator.requests.single().prompt)
    }

    @Test
    fun `a blank prompt means the default`() {
        engineWith("Good morning", mapOf(ModelTask.EnglishToMyanmar to "  \n")).translate()

        assertEquals(ModelTask.EnglishToMyanmar.defaultPrompt, translator.requests.single().prompt)
    }

    @Test
    fun `a prompt is cut to 2,000 characters`() {
        engineWith("Good morning", mapOf(ModelTask.EnglishToMyanmar to "p".repeat(2_500))).translate()

        assertEquals("p".repeat(2_000), translator.requests.single().prompt)
    }
}
