package com.akkhara.keyboard.engine

/** Which screenful of keys is showing. */
enum class Layer { Letters, Symbols, MoreSymbols }

enum class ShiftState {
    Off,

    /** The shift layer shows for one key press that types. */
    OneShot,

    /** The shift layer shows until the user turns it off. */
    Locked,
}

/** The language the keyboard is typing. Exactly one is active at a time. */
enum class InputLanguage { English, Myanmar }
