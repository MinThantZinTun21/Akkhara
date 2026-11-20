package com.akkhara.keyboard.engine

/** What sort of text a text field takes, as far as the keyboard treats it differently. */
sealed interface FieldKind {
    /** Ordinary writing. */
    data object Text : FieldKind

    /** An email or web address: no English conveniences. */
    data object Address : FieldKind

    /** A number only: the keyboard shows the number pad. See GLOSSARY.md, "Number pad". */
    data class Number(val allowsNegative: Boolean = false, val allowsDecimal: Boolean = false) : FieldKind

    /** A phone number: the keyboard shows the phone pad. See GLOSSARY.md, "Phone pad". */
    data object Phone : FieldKind

    /** A date, a time, or both: the keyboard shows the date/time pad. See GLOSSARY.md, "Date/time pad". */
    sealed interface DateOrTime : FieldKind {
        val takesDate: Boolean
        val takesTime: Boolean
    }

    /** A date only: the pad offers `/` and `-`. */
    data object Date : DateOrTime {
        override val takesDate = true
        override val takesTime = false
    }

    /** A time only: the pad offers `:`. */
    data object Time : DateOrTime {
        override val takesDate = false
        override val takesTime = true
    }

    /** A date and a time together: the pad offers `/`, `-`, `:` and a space between them. */
    data object DateAndTime : DateOrTime {
        override val takesDate = true
        override val takesTime = true
    }
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
