package com.akkhara.keyboard.engine

/**
 * Presses keys the way a user does: by finding them on the layer that is
 * currently showing. A key that is not showing cannot be pressed.
 */
class Typist(private val engine: KeyboardEngine) {

    fun tap(vararg labels: String) = labels.forEach { label ->
        engine.press(charKey(label))
    }

    fun tap(action: Action, times: Int = 1) = repeat(times) {
        engine.press(actionKey(action))
    }

    /**
     * Taps each key in [labels], going through a one-shot shift for any key
     * that is on the shift layer.
     */
    fun reach(vararg labels: String) = labels.forEach { label ->
        if (label !in labels()) tap(Action.Shift)
        tap(label)
    }

    /**
     * Taps the letter keys for [letters] whichever case they are showing in,
     * as a user who is not using shift does. A space taps the space bar.
     */
    fun typeIgnoringCase(letters: String) = letters.forEach { c ->
        if (c == ' ') {
            tap(Action.Space)
        } else {
            val key = visibleKeys().filterIsInstance<CharKey>()
                .firstOrNull { it.char.equals(c.toString(), ignoreCase = true) }
                ?: error("No key '$c' on the showing layer: ${labels()}")
            engine.press(key)
        }
    }

    /** Taps each character of [text] as its own key; a space taps the space bar. */
    fun type(text: String) = text.forEach { c ->
        if (c == ' ') tap(Action.Space) else tap(c.toString())
    }

    fun charKey(label: String): CharKey =
        visibleKeys().filterIsInstance<CharKey>().firstOrNull { it.char == label }
            ?: error("No key '$label' on the showing layer: ${labels()}")

    fun actionKey(action: Action): ActionKey =
        visibleKeys().filterIsInstance<ActionKey>().firstOrNull { it.action == action }
            ?: error("No $action key on the showing layer")

    fun labels(): List<String> = visibleKeys().filterIsInstance<CharKey>().map { it.char }

    fun rowLabels(): List<String> = engine.rows.map { row ->
        row.filterIsInstance<CharKey>().joinToString(" ") { it.char }
    }

    private fun visibleKeys(): List<Key> = engine.rows.flatten()
}
