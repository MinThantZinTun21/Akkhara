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
}
