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
    inputLanguage: InputLanguage = InputLanguage.English,
) {
    var inputLanguage: InputLanguage = inputLanguage
        private set

    var layer: Layer = Layer.Letters
        private set

    var shift: ShiftState = ShiftState.Off
        private set

    /** When the tap that turned on the current one-shot shift happened. */
    private var oneShotSince = 0L

    /** The keys of the layer that is showing, top row first. */
    val rows: List<List<Key>>
        get() = when (layer) {
            Layer.Letters -> layoutFor(inputLanguage).let { if (shift == ShiftState.Off) it.base else it.shift }
            Layer.Symbols -> SymbolsLayout.first
            Layer.MoreSymbols -> SymbolsLayout.second
        }

    /** The name of the current input language, for the space bar. */
    val spaceLabel: String get() = layoutFor(inputLanguage).name

    /** What holding [key] down should do. Holding never types or changes state by itself. */
    fun hold(key: Key): Hold = when {
        key is ActionKey && key.action == Action.Globe -> Hold.KeyboardPicker
        key is ActionKey && key.action == Action.Backspace -> Hold.Repeat
        key is CharKey && key.alternates.isNotEmpty() -> Hold.Alternates(key.alternates)
        else -> Hold.None
    }

    /** The user held [key], slid to [alternate] and lifted. */
    fun pressAlternate(key: CharKey, alternate: String) {
        require(alternate in key.alternates) { "'${key.char}' does not offer '$alternate'" }
        type(alternate)
    }

    fun press(key: Key) {
        when (key) {
            is CharKey -> type(key.char)
            is ActionKey -> perform(key.action)
        }
    }

    private fun type(text: String) {
        insert(text)
        if (shift == ShiftState.OneShot) shift = ShiftState.Off
    }

    /** Puts [text] into the text field, reordering the Myanmar syllable at the cursor if need be. */
    private fun insert(text: String) {
        val before = editor.textBeforeCursor(MyanmarReordering.WINDOW)
        val edit = MyanmarReordering.type(before, text)
        if (edit.delete > 0) editor.deleteBeforeCursor(edit.delete)
        editor.commit(edit.insert)
    }

    private fun perform(action: Action) {
        when (action) {
            Action.Space -> insert(" ")
            Action.Enter -> insert("\n")
            Action.Backspace -> backspace()
            Action.Shift -> tapShift()
            Action.Globe -> switchInputLanguage()
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

    private fun switchInputLanguage() {
        inputLanguage = when (inputLanguage) {
            InputLanguage.English -> InputLanguage.Myanmar
            InputLanguage.Myanmar -> InputLanguage.English
        }
        layer = Layer.Letters
        shift = ShiftState.Off
    }

    private fun show(target: Layer) {
        layer = target
        if (shift == ShiftState.OneShot) shift = ShiftState.Off
    }

    private fun backspace() {
        if (editor.deleteSelection()) return
        val units = MyanmarReordering.backspace(editor.textBeforeCursor(2))
        if (units > 0) editor.deleteBeforeCursor(units)
    }

    private companion object {
        /** Two taps on shift this close together are a double tap. */
        const val DOUBLE_TAP_MILLIS = 300L
    }
}
