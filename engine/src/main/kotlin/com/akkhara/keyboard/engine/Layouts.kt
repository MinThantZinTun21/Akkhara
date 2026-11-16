package com.akkhara.keyboard.engine

internal typealias Row = List<Key>

/** Turns a space-separated string into a row of character keys. */
internal fun chars(keys: String): Row = keys.split(' ').map { CharKey(it) }

private const val WIDE = 1.5f

private val shiftKey = ActionKey(Action.Shift, WIDE)
private val backspaceKey = ActionKey(Action.Backspace, WIDE)
private val enterKey = ActionKey(Action.Enter, WIDE)

/** The row under the letters or symbols; [first] is the key that changes layer. */
private fun bottomRow(first: Action): Row = listOf(
    ActionKey(first, WIDE),
    CharKey(","),
    ActionKey(Action.Space, width = 5f),
    CharKey("."),
    enterKey,
)

internal object EnglishLayout {
    private val digits = chars("1 2 3 4 5 6 7 8 9 0")

    val base: List<Row> = listOf(
        digits,
        chars("q w e r t y u i o p"),
        chars("a s d f g h j k l"),
        listOf(shiftKey) + chars("z x c v b n m") + backspaceKey,
        bottomRow(Action.Symbols),
    )

    val shift: List<Row> = listOf(
        digits,
        chars("Q W E R T Y U I O P"),
        chars("A S D F G H J K L"),
        listOf(shiftKey) + chars("Z X C V B N M") + backspaceKey,
        bottomRow(Action.Symbols),
    )
}

/** The two layers of digits and punctuation shared by every layout. */
internal object SymbolsLayout {
    val first: List<Row> = listOf(
        chars("1 2 3 4 5 6 7 8 9 0"),
        chars("@ # $ % & - + ( ) /"),
        chars("* \" ' : ; ! ? _ = ~"),
        listOf(ActionKey(Action.MoreSymbols, WIDE)) + chars("\\ | < > [ ] ^") + backspaceKey,
        bottomRow(Action.Letters),
    )

    val second: List<Row> = listOf(
        chars("` • √ π ÷ × ¶ ∆ ° ¿"),
        chars("£ ¢ € ¥ ₩ ₹ ฿ { } §"),
        chars("© ® ™ ✓ « » ‹ › … ¡"),
        listOf(ActionKey(Action.Symbols, WIDE)) + chars("‘ ’ “ ” — – ·") + backspaceKey,
        bottomRow(Action.Letters),
    )
}
