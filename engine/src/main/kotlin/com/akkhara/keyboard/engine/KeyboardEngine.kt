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

    /** When shift was last tapped, if no other key has been pressed since. */
    private var lastShiftTap: Long? = null

    /** Whether the current one-shot shift was put there by automatic capitalisation. */
    private var shiftIsAutomatic = false

    private var textField = Field()
    private var conveniences = Conveniences()

    /** What the enter key does in the current text field, for its label. */
    val enterAction: EnterAction get() = textField.enterAction

    /** False while typing a password, where no typed character may be shown enlarged. */
    val allowsKeyPreview: Boolean get() = !textField.isPassword

    /** The keys of the layer that is showing, top row first. */
    val rows: List<List<Key>>
        get() = when (layer) {
            Layer.Letters -> layoutFor(inputLanguage).let { if (shift == ShiftState.Off) it.base else it.shift }
            Layer.Symbols -> SymbolsLayout.first
            Layer.MoreSymbols -> SymbolsLayout.second
        }

    /** The name of the current input language, for the space bar. */
    val spaceLabel: String get() = layoutFor(inputLanguage).name

    /** A few letters of the current input language, for the key that leaves the symbols layers. */
    val lettersLabel: String get() = layoutFor(inputLanguage).lettersLabel

    /** The label of the key that opens the symbols layers, in the digits of the current input language. */
    val symbolsLabel: String get() = layoutFor(inputLanguage).symbolsLabel

    /**
     * Typing is starting in [field]. The input language carries over from the
     * last text field; the layer and shift do not.
     */
    fun startInput(field: Field, conveniences: Conveniences = this.conveniences) {
        textField = field
        this.conveniences = conveniences
        layer = if (field.kind == FieldKind.Number) Layer.Symbols else Layer.Letters
        setShift(ShiftState.Off)
        lastShiftTap = null
        updateAutomaticCapital()
    }

    /** Typing in the current text field is over, as when the keyboard closes. */
    fun finishInput() = finishSyllable()

    /** The cursor went somewhere the keyboard did not put it. */
    fun onCursorMoved() = updateAutomaticCapital()

    /** What holding [key] down should do. Holding never types or changes state by itself. */
    fun hold(key: Key): Hold = when {
        key is ActionKey && key.action == Action.Globe -> Hold.KeyboardPicker
        key is ActionKey && key.action == Action.Backspace -> Hold.Repeat
        key is CharKey && key.alternates.isNotEmpty() -> Hold.Alternates(key.alternates)
        else -> Hold.None
    }

    /** The user held [key], slid to [alternate] and lifted. */
    fun pressAlternate(key: CharKey, alternate: String) {
        require(alternate in key.alternates) { "'${key.text}' does not offer '$alternate'" }
        type(alternate)
        afterEdit()
    }

    fun press(key: Key) {
        when (key) {
            is CharKey -> type(key.text)
            is ActionKey -> perform(key.action)
        }
        // A tap on shift is the user's own choice of shift, so it is left as they set it.
        val isShift = key is ActionKey && key.action == Action.Shift
        if (!isShift) afterEdit()
    }

    private fun afterEdit() {
        lastShiftTap = null
        updateAutomaticCapital()
    }

    private fun type(text: String) {
        insert(text)
        if (shift == ShiftState.OneShot) setShift(ShiftState.Off)
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
            Action.Space -> space()
            Action.Enter -> enter()
            Action.Backspace -> backspace()
            Action.Shift -> tapShift()
            Action.Globe -> switchInputLanguage()
            Action.Symbols -> show(Layer.Symbols)
            Action.MoreSymbols -> show(Layer.MoreSymbols)
            Action.Letters -> show(Layer.Letters)
        }
    }

    private fun space() {
        if (conveniencesApply && conveniences.doubleSpaceFullStop) {
            val before = editor.textBeforeCursor(2)
            if (before.length == 2 && before[0].isLetterOrDigit() && before[1] == ' ') {
                editor.deleteBeforeCursor(1)
                editor.commit(". ")
                return
            }
        }
        insert(" ")
    }

    private fun enter() {
        if (textField.enterAction == EnterAction.NewLine) {
            insert("\n")
        } else {
            finishSyllable()
            editor.performEnterAction()
        }
    }

    /** Leaves nothing invisible in the text when a Myanmar syllable is left unfinished. */
    private fun finishSyllable() {
        val edit = MyanmarReordering.finish(editor.textBeforeCursor(MyanmarReordering.WINDOW))
        if (edit.delete > 0) {
            editor.deleteBeforeCursor(edit.delete)
            editor.commit(edit.insert)
        }
    }

    private fun tapShift() {
        val now = clock()
        val previous = lastShiftTap
        val isDoubleTap = previous != null && now - previous <= DOUBLE_TAP_MILLIS
        lastShiftTap = null
        when {
            shift == ShiftState.Locked -> setShift(ShiftState.Off)
            isDoubleTap -> setShift(ShiftState.Locked)
            else -> {
                setShift(if (shift == ShiftState.Off) ShiftState.OneShot else ShiftState.Off)
                lastShiftTap = now
            }
        }
    }

    /** Any change of shift that automatic capitalisation did not make. */
    private fun setShift(state: ShiftState) {
        shift = state
        shiftIsAutomatic = false
    }

    /** The English conveniences apply only to ordinary writing in English. */
    private val conveniencesApply: Boolean
        get() = inputLanguage == InputLanguage.English &&
            textField.kind == FieldKind.Text &&
            !textField.isPassword

    /**
     * Shows one-shot shift when a capital is coming, and takes it away again
     * when it is not. A shift the user chose is never taken away.
     */
    private fun updateAutomaticCapital() {
        val wanted = conveniencesApply && conveniences.capitaliseSentences && editor.wantsCapital()
        if (wanted && shift == ShiftState.Off) {
            shift = ShiftState.OneShot
            shiftIsAutomatic = true
        } else if (!wanted && shiftIsAutomatic) {
            setShift(ShiftState.Off)
        }
    }

    private fun switchInputLanguage() {
        inputLanguage = when (inputLanguage) {
            InputLanguage.English -> InputLanguage.Myanmar
            InputLanguage.Myanmar -> InputLanguage.English
        }
        layer = Layer.Letters
        setShift(ShiftState.Off)
    }

    /** Every visit to another layer starts from, and comes back to, the base layer. */
    private fun show(target: Layer) {
        layer = target
        setShift(ShiftState.Off)
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
