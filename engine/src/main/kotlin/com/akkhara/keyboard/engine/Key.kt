package com.akkhara.keyboard.engine

/** One touchable cell on a layer. [width] is relative to an ordinary character key. */
sealed interface Key {
    val width: Float
}

/**
 * A key that types [text]: one character, or a short fixed sequence such as
 * kinzi. Held down, it offers its [alternates]. [hint] is the alternate shown
 * small in the key's corner, for the keys that show one.
 */
data class CharKey(
    val text: String,
    val alternates: List<String> = emptyList(),
    override val width: Float = 1f,
    val hint: String? = null,
) : Key

/**
 * A blank on a pad: a place with no key, which keeps the keys in their
 * columns. It is drawn as nothing, and a touch on it does nothing.
 * See GLOSSARY.md, "Blank".
 */
data class Blank(override val width: Float) : Key

/** Stands for a space where a label must show one, as on a pad's space key. */
const val SPACE_SIGN = "\u2423"

/** A key that performs [action] instead of typing a character. */
data class ActionKey(
    val action: Action,
    override val width: Float = 1f,
) : Key

enum class Action {
    Shift,
    Backspace,
    Enter,
    Space,

    /** Switch the input language. */
    Globe,

    /** Show the first symbols layer. */
    Symbols,

    /** Show the second symbols layer. */
    MoreSymbols,

    /** Leave the symbols layers for the letters of the current layout. */
    Letters,

    /** Translate the text field's text, showing the translation in the translate strip. */
    Translate,

    /** Show the emoji panel. */
    Emoji,
}

/** What holding a key down should do, for the View to carry out. */
sealed interface Hold {
    /** Nothing special: the key behaves as it does for a tap. */
    data object None : Hold

    /** Open Android's keyboard picker. */
    data object KeyboardPicker : Hold

    /** Offer these characters; sliding to one and lifting types it. */
    data class Alternates(val alternates: List<String>) : Hold

    /** Keep pressing the key for as long as it is held. */
    data object Repeat : Hold

    /** Offer the grammar key; lifting on it checks grammar, and lifting anywhere else does nothing. */
    data object GrammarKey : Hold

    /** Offer the emoji key; lifting on it opens the emoji panel, and lifting anywhere else does nothing. */
    data object EmojiKey : Hold
}
