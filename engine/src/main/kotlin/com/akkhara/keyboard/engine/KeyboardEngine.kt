package com.akkhara.keyboard.engine

/**
 * All keyboard behaviour: which keys are showing and what pressing one does to
 * the text field. It knows nothing about Android.
 *
 * [clock] gives the current time in milliseconds and only needs to be
 * monotonic; it is used to recognise a double tap on shift. [translator]
 * translates the text typed into the translate strip.
 */
class KeyboardEngine(
    private val editor: Editor,
    private val clock: () -> Long = { System.nanoTime() / 1_000_000 },
    inputLanguage: InputLanguage = InputLanguage.English,
    private val translator: Translator? = null,
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

    /** Whether a translation model is installed, which puts the translate key on the keyboard. */
    var isTranslationAvailable = false

    /** Whether the translate strip translates by itself when typing pauses. See GLOSSARY.md, "Translate as you type". */
    var translateAsYouType = false

    /** The translate strip's text while it is open; typing goes there instead of the text field. */
    private var stripEditor: StripEditor? = null
    private var stripDirection = TranslationDirection.EnglishToMyanmar
    private var stripState: StripState = StripState.Idle

    /** Counts translation requests, so a result for an earlier one can be recognised and dropped. */
    private var stripRequest = 0

    /**
     * Called when the strip changes by itself, as when a translation arrives.
     * Changes caused by the engine's own methods are not reported.
     */
    var onStripChanged: (() -> Unit)? = null

    /** The translate strip, or null while it is closed. */
    val strip: TranslateStrip?
        get() = stripEditor?.let { TranslateStrip(it.text, stripDirection, stripState) }

    /** Where typing goes: the translate strip while it is open, otherwise the text field. */
    private val target: Editor get() = stripEditor ?: editor

    /** The keys of the layer that is showing, top row first. */
    val rows: List<List<Key>>
        get() = when (layer) {
            Layer.Letters -> layoutFor(inputLanguage)
                .let { if (shift == ShiftState.Off) it.base else it.shift }
                .let { if (isTranslationAvailable && translator != null) it.withTranslateKey() else it }
            Layer.Symbols -> SymbolsLayout.first
            Layer.MoreSymbols -> SymbolsLayout.second
        }

    /** The name of the current input language, for the space bar. */
    val spaceLabel: String get() = layoutFor(inputLanguage).name

    /** The name of [language] in its own language, as on the space bar. */
    fun languageName(language: InputLanguage): String = layoutFor(language).name

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
        val stripTextBefore = stripEditor?.text
        type(alternate)
        if (stripTextBefore != null && stripEditor?.text != stripTextBefore) onStripTextChanged()
        afterEdit()
    }

    fun press(key: Key) {
        val stripTextBefore = stripEditor?.text
        when (key) {
            is CharKey -> type(key.text)
            is ActionKey -> perform(key.action)
        }
        if (stripTextBefore != null && stripEditor?.text != stripTextBefore) onStripTextChanged()
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
        val before = target.textBeforeCursor(MyanmarReordering.WINDOW)
        val edit = MyanmarReordering.type(before, text)
        val strip = stripEditor
        if (strip != null && strip.text.length - edit.delete + edit.insert.length > STRIP_MAX_CHARS) return
        if (edit.delete > 0) target.deleteBeforeCursor(edit.delete)
        target.commit(edit.insert)
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
            Action.Translate -> openStrip()
        }
    }

    private fun space() {
        if (conveniencesApply && conveniences.doubleSpaceFullStop) {
            val before = target.textBeforeCursor(2)
            if (before.length == 2 && before[0].isLetterOrDigit() && before[1] == ' ') {
                target.deleteBeforeCursor(1)
                target.commit(". ")
                return
            }
        }
        insert(" ")
    }

    private fun enter() {
        if (stripEditor != null) {
            translateStrip()
        } else if (textField.enterAction == EnterAction.NewLine) {
            insert("\n")
        } else {
            finishSyllable()
            editor.performEnterAction()
        }
    }

    /** Leaves nothing invisible in the text when a Myanmar syllable is left unfinished. */
    private fun finishSyllable() {
        val edit = MyanmarReordering.finish(target.textBeforeCursor(MyanmarReordering.WINDOW))
        if (edit.delete > 0) {
            target.deleteBeforeCursor(edit.delete)
            target.commit(edit.insert)
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
            !textField.isPassword &&
            stripEditor == null

    /**
     * Shows one-shot shift when a capital is coming, and takes it away again
     * when it is not. A shift the user chose is never taken away.
     */
    private fun updateAutomaticCapital() {
        val wanted = conveniencesApply && conveniences.capitaliseSentences && target.wantsCapital()
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
        if (target.deleteSelection()) return
        val units = MyanmarReordering.backspace(target.textBeforeCursor(2))
        if (units > 0) target.deleteBeforeCursor(units)
    }

    /** Opens an empty translate strip, translating from the current input language. */
    private fun openStrip() {
        finishSyllable()
        stripEditor = StripEditor()
        stripDirection = TranslationDirection.entries.first { it.from == inputLanguage }
        stripState = StripState.Idle
    }

    /** Closes the translate strip and forgets everything in it. */
    fun closeStrip() {
        stripEditor = null
        stripState = StripState.Idle
        stripRequest++
    }

    /**
     * Translates the strip's text. Does nothing when the strip is closed or
     * empty, or while a translation is already running.
     */
    fun translateStrip() {
        val strip = stripEditor ?: return
        val translator = translator ?: return
        val text = strip.text
        if (text.isBlank() || stripState == StripState.Translating) return
        stripState = StripState.Translating
        val request = ++stripRequest
        translator.translate(text, stripDirection) { result ->
            if (request != stripRequest) return@translate
            stripState = when {
                // The user kept typing: this translation is of older text.
                strip.text != text -> StripState.Idle
                result is TranslationResult.Translated -> StripState.Translated(result.text)
                result is TranslationResult.Failed -> StripState.Failed(result.reason)
                else -> StripState.Idle
            }
            // The pause that would have translated the newer text came while this one ran.
            if (stripState == StripState.Idle) onTypingPaused()
            onStripChanged?.invoke()
        }
    }

    /** The user has stopped typing for a moment: with [translateAsYouType] on, the strip translates by itself. */
    fun onTypingPaused() {
        // Idle means the strip's text has no translation yet; any other state already covers it.
        if (translateAsYouType && stripState == StripState.Idle) translateStrip()
    }

    /** Puts the translation into the text field at the cursor and closes the strip. */
    fun insertTranslation() {
        val translated = stripState as? StripState.Translated ?: return
        closeStrip()
        editor.commit(translated.translation)
    }

    /** A translation shown for earlier text no longer applies. */
    private fun onStripTextChanged() {
        if (stripState != StripState.Translating) stripState = StripState.Idle
    }

    /** Puts the translate key after the globe key, taking its width from the space bar. */
    private fun List<List<Key>>.withTranslateKey(): List<List<Key>> {
        val bottom = last().flatMap { key ->
            when {
                key is ActionKey && key.action == Action.Globe -> listOf(key, ActionKey(Action.Translate))
                key is ActionKey && key.action == Action.Space -> listOf(key.copy(width = key.width - 1f))
                else -> listOf(key)
            }
        }
        return dropLast(1) + listOf(bottom)
    }

    private companion object {
        /** Two taps on shift this close together are a double tap. */
        const val DOUBLE_TAP_MILLIS = 300L

        /** The most UTF-16 units the translate strip takes, so no translation runs for minutes. */
        const val STRIP_MAX_CHARS = 500
    }
}
