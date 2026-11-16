package com.akkhara.keyboard.engine

/** One touchable cell on a layer. [width] is relative to an ordinary character key. */
sealed interface Key {
    val width: Float
}

/** A key that types [char]. Held down, it offers its [alternates]. */
data class CharKey(
    val char: String,
    val alternates: List<String> = emptyList(),
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

    /** Switch the input language. */
    Globe,

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

/** The language the keyboard is typing. Exactly one is active at a time. */
enum class InputLanguage { English, Myanmar }

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

/** What sort of text a text field takes, as far as the keyboard treats it differently. */
enum class FieldKind {
    /** Ordinary writing. */
    Text,

    /** A number or a phone number: the keyboard opens on the symbols layer. */
    Number,

    /** An email or web address: no English conveniences. */
    Address,
}

/** What the enter key does in a text field. */
enum class EnterAction { NewLine, Go, Search, Send, Next, Done }

/** The text field the keyboard is typing into. */
data class Field(
    val kind: FieldKind = FieldKind.Text,
    val isPassword: Boolean = false,
    val enterAction: EnterAction = EnterAction.NewLine,
)

/** Which of the English conveniences the user has left switched on. */
data class Conveniences(
    val capitaliseSentences: Boolean = true,
    val doubleSpaceFullStop: Boolean = true,
)
