package com.akkhara.keyboard.engine

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
