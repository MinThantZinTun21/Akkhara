package com.akkhara.keyboard.engine

/** What the translate strip shows. See GLOSSARY.md, "Translate strip". */
data class TranslateStrip(
    /** The text typed into the strip. */
    val text: String,
    val direction: TranslationDirection,
    val state: StripState,
)

sealed interface StripState {
    /** Nothing translated yet, or the text changed since the last translation. */
    data object Idle : StripState

    data object Translating : StripState

    /** [translation] is the translation of the strip's current text. */
    data class Translated(val translation: String) : StripState

    data class Failed(val reason: TranslationFailure) : StripState
}

/** The strip's text, held in memory with the cursor always at the end. */
internal class StripEditor : Editor {
    private val buffer = StringBuilder()

    val text: String get() = buffer.toString()

    override fun textBeforeCursor(maxChars: Int): String = buffer.substring(maxOf(0, buffer.length - maxChars))

    override fun commit(text: String) {
        buffer.append(text)
    }

    override fun deleteBeforeCursor(chars: Int) {
        buffer.setLength(maxOf(0, buffer.length - chars))
    }

    override fun deleteSelection(): Boolean = false

    override fun wantsCapital(): Boolean = false

    override fun performEnterAction() = Unit
}
