package com.akkhara.keyboard.engine

/**
 * Which screenful of keys is showing. On [Emoji], the emoji panel takes the
 * place of the keys. [NumberPad] shows in a field that takes only a number,
 * and nothing on it leads to another layer.
 */
enum class Layer {
    Letters,
    Symbols,
    MoreSymbols,
    Emoji,
    NumberPad,
    ;

    /** Whether this is a pad for a field that takes only numbers, as tall as the letters but with fewer rows. */
    val isPad: Boolean get() = this == NumberPad
}

enum class ShiftState {
    Off,

    /** The shift layer shows for one key press that types. */
    OneShot,

    /** The shift layer shows until the user turns it off. */
    Locked,
}

/** The language the keyboard is typing. Exactly one is active at a time. */
enum class InputLanguage { English, Myanmar }
