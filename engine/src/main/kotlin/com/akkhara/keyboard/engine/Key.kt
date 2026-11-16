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

enum class Action { Backspace, Enter, Space }
