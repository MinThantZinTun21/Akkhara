package com.akkhara.keyboard.engine

internal typealias Row = List<Key>

/** Turns a space-separated string into a row of character keys. */
internal fun chars(keys: String): Row = keys.split(' ').map { CharKey(it) }

internal object EnglishLayout {
    val base: List<Row> = listOf(
        chars("q w e r t y u i o p"),
        chars("a s d f g h j k l"),
        chars("z x c v b n m") + ActionKey(Action.Backspace, width = 1.5f),
        listOf(
            CharKey(","),
            ActionKey(Action.Space, width = 5f),
            CharKey("."),
            ActionKey(Action.Enter, width = 1.5f),
        ),
    )
}
