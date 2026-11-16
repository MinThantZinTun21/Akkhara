package com.akkhara.keyboard.engine

/** What the translate strip shows. See GLOSSARY.md, "Translate strip". */
sealed interface StripState {
    /** Nothing translated yet, or the text field changed since the last translation. */
    data object Idle : StripState

    data object Translating : StripState

    /** [translation] is the translation of the text field's current text. */
    data class Translated(val translation: String) : StripState

    data class Failed(val reason: TranslationFailure) : StripState
}
