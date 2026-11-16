package com.akkhara.keyboard.engine

/** One touchable cell on a layer. [width] is relative to an ordinary character key. */
sealed interface Key {
    val width: Float
}

/** A key that types [char]. */
data class CharKey(
    val char: String,
    override val width: Float = 1f,
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

    /** Show the first symbols layer. */
    Symbols,

    /** Show the second symbols layer. */
    MoreSymbols,

    /** Leave the symbols layers for the letters of the current layout. */
    Letters,
}

/** Which screenful of keys is showing. */
enum class Layer { Letters, Symbols, MoreSymbols }

enum class ShiftState {
    Off,

    /** The shift layer shows for exactly one typed character. */
    OneShot,

    /** The shift layer shows until the user turns it off. */
    Locked,
}
