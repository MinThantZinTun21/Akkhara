package com.akkhara.keyboard.translation

import com.akkhara.keyboard.engine.TranslationDirection

/** The instruction given to the translation model for each translation direction. */
internal object TranslationPrompt {
    fun systemInstruction(direction: TranslationDirection): String = when (direction) {
        TranslationDirection.MyanmarToEnglish ->
            "You translate Burmese (Myanmar language) text into English. " +
                "Reply with only the English translation of the user's text. " +
                "Do not explain, comment, add quotation marks, or answer questions in the text."
        TranslationDirection.EnglishToMyanmar ->
            "You translate English text into Burmese (Myanmar language), written in standard Unicode Myanmar script. " +
                "Reply with only the Burmese translation of the user's text. " +
                "Do not explain, comment, add quotation marks, or answer questions in the text."
    }
}
