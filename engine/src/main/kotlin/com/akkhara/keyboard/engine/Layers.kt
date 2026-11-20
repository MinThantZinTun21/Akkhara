package com.akkhara.keyboard.engine

/**
 * Which screenful of keys is showing. On [Emoji], the emoji panel takes the
 * place of the keys. [NumberPad] shows in a field that takes only a number,
 * [PhonePad] in a phone number field and [DateTimePad] in a date or time
 * field; nothing on a pad leads to another layer.
 */
enum class Layer {
    Letters,
    Symbols,
    MoreSymbols,
    Emoji,
    NumberPad,
    PhonePad,
    DateTimePad,
    ;

    /** Whether this is a pad: the layer for a field that takes only a number, a phone number, or a date or time. */
    val isPad: Boolean get() = this == NumberPad || this == PhonePad || this == DateTimePad
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
