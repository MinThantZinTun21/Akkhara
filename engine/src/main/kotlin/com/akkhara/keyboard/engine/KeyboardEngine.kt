package com.akkhara.keyboard.engine

/**
 * All keyboard behaviour: which keys are showing and what pressing one does to
 * the text field. It knows nothing about Android.
 *
 * [clock] gives the current time in milliseconds and only needs to be
 * monotonic; it is used to recognise a double tap on shift.
 */
class KeyboardEngine(
    private val editor: Editor,
    private val clock: () -> Long = { System.nanoTime() / 1_000_000 },
) {
    var layer: Layer = Layer.Letters
        private set

    var shift: ShiftState = ShiftState.Off
        private set

    /** When the tap that turned on the current one-shot shift happened. */
    private var oneShotSince = 0L

    /** The keys of the layer that is showing, top row first. */
    val rows: List<List<Key>>
        get() = when (layer) {
            Layer.Letters -> if (shift == ShiftState.Off) EnglishLayout.base else EnglishLayout.shift
            Layer.Symbols -> SymbolsLayout.first
            Layer.MoreSymbols -> SymbolsLayout.second
        }

    fun press(key: Key) {
        when (key) {
            is CharKey -> type(key.char)
            is ActionKey -> perform(key.action)
        }
    }

    private fun type(text: String) {
        editor.commit(text)
        if (shift == ShiftState.OneShot) shift = ShiftState.Off
    }

    private fun perform(action: Action) {
        when (action) {
            Action.Space -> editor.commit(" ")
            Action.Enter -> editor.commit("\n")
            Action.Backspace -> backspace()
            Action.Shift -> tapShift()
            Action.Symbols -> show(Layer.Symbols)
            Action.MoreSymbols -> show(Layer.MoreSymbols)
            Action.Letters -> show(Layer.Letters)
        }
    }

    private fun tapShift() {
        val now = clock()
        shift = when (shift) {
            ShiftState.Off -> {
                oneShotSince = now
                ShiftState.OneShot
            }
            ShiftState.OneShot ->
                if (now - oneShotSince <= DOUBLE_TAP_MILLIS) ShiftState.Locked else ShiftState.Off
            ShiftState.Locked -> ShiftState.Off
        }
    }

    private fun show(target: Layer) {
        layer = target
        if (shift == ShiftState.OneShot) shift = ShiftState.Off
    }

    private fun backspace() {
        if (editor.deleteSelection()) return
        val before = editor.textBeforeCursor(2)
        if (before.isEmpty()) return
        val endsWithSurrogatePair = before.length == 2 &&
            Character.isSurrogatePair(before[0], before[1])
        editor.deleteBeforeCursor(if (endsWithSurrogatePair) 2 else 1)
    }

    private companion object {
        /** Two taps on shift this close together are a double tap. */
        const val DOUBLE_TAP_MILLIS = 300L
    }
}
