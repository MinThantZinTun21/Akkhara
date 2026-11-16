package com.akkhara.keyboard.engine

/**
 * What the translation model is asked to do, each with Akkhara's default
 * prompt. See GLOSSARY.md, "Prompt" and "Translation direction".
 */
enum class ModelTask(val defaultPrompt: String) {
    EnglishToMyanmar(
        "You translate English text into Burmese (Myanmar language), written in standard Unicode Myanmar script. " +
            "Reply with only the Burmese translation of the user's text. " +
            "Do not explain, comment, add quotation marks, or answer questions in the text.",
    ),
    MyanmarToEnglish(
        "You translate Burmese (Myanmar language) text into English. " +
            "Reply with only the English translation of the user's text. " +
            "Do not explain, comment, add quotation marks, or answer questions in the text.",
    ),
}
