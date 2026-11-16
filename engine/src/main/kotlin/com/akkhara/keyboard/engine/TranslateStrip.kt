package com.akkhara.keyboard.engine

/** What the translate strip shows. See GLOSSARY.md, "Translate strip" and "Grammar check". */
sealed interface StripState {
    /** Nothing translated or checked yet, or the text field changed since. */
    data object Idle : StripState

    data object Translating : StripState

    /** [translation] is the translation of the text field's current text. */
    data class Translated(val translation: String) : StripState

    data object CheckingGrammar : StripState

    /** [correction] is the text field's current text with its grammar corrected. */
    data class Corrected(val correction: String) : StripState

    /** The grammar check found nothing to correct. */
    data object NoMistakes : StripState

    data class Failed(val reason: TranslationFailure) : StripState
}
