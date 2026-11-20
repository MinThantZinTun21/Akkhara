package com.akkhara.keyboard.engine

/**
 * All keyboard behaviour: which keys are showing and what pressing one does to
 * the text field. It knows nothing about Android.
 *
 * [clock] gives the current time in milliseconds and only needs to be
 * monotonic; it is used to recognise a double tap on shift. [translator]
 * translates the text field's text for the translate strip.
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

    /** Whether a translation model is installed, which shows the translate key and the translate strip. */
    var isTranslationAvailable = false

    /**
     * Whether some model on the phone can check grammar, which offers the
     * grammar key. A model that translates only cannot. See GLOSSARY.md, "Translates only".
     */
    var isGrammarCheckAvailable = true

    /** Whether the translate strip translates by itself when typing pauses. See GLOSSARY.md, "Translate as you type". */
    var translateAsYouType = false

    /** The user's own prompts; a task left out, or given a blank prompt, uses its default. See GLOSSARY.md, "Prompt". */
    var customPrompts: Map<ModelTask, String> = emptyMap()

    private var stripState: StripState = StripState.Idle

    /** The text the shown translation was made from, so a changed text field can be noticed. */
    private var translatedSource: TranslationSource? = null

    /** Counts translation requests, so a result for an earlier one can be recognised and dropped. */
    private var stripRequest = 0

    /**
     * Called when the strip changes by itself, as when a translation arrives.
     * Changes caused by the engine's own methods are not reported.
     */
    var onStripChanged: (() -> Unit)? = null

    /**
     * What the translate strip shows, or null when there is none: without a
     * translation model, and in a password field, which is never translated.
     */
    val strip: StripState?
        get() = if (offersTranslation) stripState else null

    /**
     * Translation is offered with a model installed, except in a password
     * field, which is never translated, and on a pad, where there is nothing to translate.
     */
    private val offersTranslation: Boolean
        get() = isTranslationAvailable && translator != null && !textField.isPassword && !layer.isPad

    /** A grammar check is offered for English text while some model on the phone can check grammar. */
    private val offersGrammarCheck: Boolean
        get() = strip != null && isGrammarCheckAvailable && translationSource().isEnglish

    /** The keys of the layer that is showing, top row first. */
    val rows: List<List<Key>>
        get() = when (layer) {
            Layer.Letters -> layoutFor(inputLanguage)
                .let { if (shift == ShiftState.Off) it.base else it.shift }
                .let { if (offersTranslation) it.withTranslateKey() else it }
            Layer.Symbols -> SymbolsLayout.first
            Layer.MoreSymbols -> SymbolsLayout.second
            Layer.Emoji -> EmojiPanelLayout.bar
            Layer.NumberPad -> Pads.number(textField.kind as? FieldKind.Number ?: FieldKind.Number())
            Layer.PhonePad -> Pads.phone
            Layer.DateTimePad -> Pads.dateTime(textField.kind as? FieldKind.DateOrTime ?: FieldKind.DateAndTime)
        }

    /** The emoji chosen since the keyboard started, newest first. Never stored. See GLOSSARY.md, "Recent emoji". */
    private val recentEmoji = ArrayDeque<String>()

    /** The recent emoji as they were when the emoji panel opened, so the grid does not move under the finger. */
    private var shownRecentEmoji: List<Emoji> = emptyList()

    /** What the emoji panel offers, category by category: the recent emoji, then Unicode's categories. */
    val emojiCategories: List<EmojiCategory>
        get() = listOf(EmojiCategory(EmojiCategory.Kind.Recent, shownRecentEmoji)) + EmojiList.categories

    /** The name of the current input language, for the space bar. */
    val spaceLabel: String get() = if (layer.isPad) SPACE_SIGN else layoutFor(inputLanguage).name

    /**
     * How many rows tall the keys are drawn. A pad has fewer rows than the
     * letters but keeps their height, so the app above does not jump.
     */
    val heightInRows: Int get() = if (layer.isPad) layoutFor(inputLanguage).base.size else rows.size

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
        layer = when (field.kind) {
            FieldKind.Text, FieldKind.Address -> Layer.Letters
            FieldKind.Phone -> Layer.PhonePad
            is FieldKind.DateOrTime -> Layer.DateTimePad
            is FieldKind.Number -> Layer.NumberPad
        }
        setShift(ShiftState.Off)
        lastShiftTap = null
        updateAutomaticCapital()
        forgetTranslation()
    }

    /** Typing in the current text field is over, as when the keyboard closes. */
    fun finishInput() {
        finishSyllable()
        forgetTranslation()
    }

    /** The cursor went somewhere the keyboard did not put it. */
    fun onCursorMoved() = updateAutomaticCapital()

    /** What holding [key] down should do. Holding never types or changes state by itself. */
    fun hold(key: Key): Hold = when {
        key is ActionKey && key.action == Action.Globe -> Hold.KeyboardPicker
        key is ActionKey && key.action == Action.Backspace -> Hold.Repeat
        key is CharKey && key.alternates.isNotEmpty() -> Hold.Alternates(key.alternates)
        key is ActionKey && key.action == Action.Translate && offersGrammarCheck -> Hold.GrammarKey
        key is CharKey && layer == Layer.Letters && key.text == layoutFor(inputLanguage).comma -> Hold.EmojiKey
        else -> Hold.None
    }

    /** The user held [key], slid to [alternate] and lifted. */
    fun pressAlternate(key: CharKey, alternate: String) {
        require(alternate in key.alternates) { "'${key.text}' does not offer '$alternate'" }
        type(alternate)
        afterEdit()
        onTextFieldEdited()
    }

    /** The user held the comma key and lifted on the emoji key it offered. */
    fun showEmojiPanel() = show(Layer.Emoji)

    /** The user tapped [emoji] in the emoji panel. It is typed as it is, whatever the input language. */
    fun typeEmoji(emoji: String) {
        recentEmoji.remove(emoji)
        recentEmoji.addFirst(emoji)
        if (recentEmoji.size > MAX_RECENT_EMOJI) recentEmoji.removeLast()
        insert(emoji)
        afterEdit()
        onTextFieldEdited()
    }

    fun press(key: Key) {
        when (key) {
            is CharKey -> type(key.text)
            is ActionKey -> perform(key.action)
            is Blank -> return
        }
        if (key is CharKey || (key as ActionKey).action in EDITING_ACTIONS) onTextFieldEdited()
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
            Action.Translate -> translate()
            Action.Emoji -> show(Layer.Emoji)
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
        if (target == Layer.Emoji) shownRecentEmoji = recentEmoji.map { Emoji(it) }
        layer = target
        setShift(ShiftState.Off)
    }

    private fun backspace() {
        if (editor.deleteSelection()) return
        val before = editor.textBeforeCursor(EmojiText.WINDOW)
        val emoji = EmojiText.lengthAtEnd(before)
        val units = if (emoji > 0) emoji else MyanmarReordering.backspace(before.takeLast(2))
        if (units > 0) editor.deleteBeforeCursor(units)
    }

    /**
     * Translates the text field's text: the selection if there is one,
     * otherwise all of it. Myanmar text becomes English, and anything else
     * becomes Myanmar. Does nothing for an empty field or while the strip is
     * already busy.
     */
    fun translate() = ask(checkGrammar = false)

    /**
     * Checks the grammar of the text field's text: the selection if there is
     * one, otherwise all of it. Only English text is checked. See GLOSSARY.md,
     * "Grammar check".
     */
    fun checkGrammar() = ask(checkGrammar = true)

    private val isBusy: Boolean
        get() = stripState == StripState.Translating || stripState == StripState.CheckingGrammar

    private fun ask(checkGrammar: Boolean) {
        val translator = translator ?: return
        if (strip == null || isBusy) return
        finishSyllable()
        val source = translationSource()
        if (source.text.isBlank() || (checkGrammar && !(isGrammarCheckAvailable && source.isEnglish))) return
        if (source.text.length > MAX_TRANSLATION_CHARS) {
            stripState = StripState.Failed(TranslationFailure.TooLong)
            return
        }
        stripState = if (checkGrammar) StripState.CheckingGrammar else StripState.Translating
        val task = if (checkGrammar) ModelTask.GrammarCheck else source.translationTask
        val request = ++stripRequest
        translator.run(source.text, task, task.prompt(customPrompts[task])) { result ->
            if (request != stripRequest) return@run
            translatedSource = source
            stripState = when {
                // The text changed while this ran: the result is for older text.
                translationSource() != source -> StripState.Idle
                result is TranslationResult.Failed -> StripState.Failed(result.reason)
                result !is TranslationResult.Translated -> StripState.Idle
                !checkGrammar -> StripState.Translated(result.text)
                result.text.trim() == source.text.trim() -> StripState.NoMistakes
                else -> StripState.Corrected(result.text)
            }
            // The pause that would have translated the newer text came while this one ran.
            if (stripState == StripState.Idle) onTypingPaused()
            onStripChanged?.invoke()
        }
    }

    /** The user has stopped typing for a moment: with [translateAsYouType] on, the strip translates by itself. */
    fun onTypingPaused() {
        // Idle means the text has no translation shown yet; any other state already covers it.
        if (translateAsYouType && stripState == StripState.Idle) translate()
    }

    /** A translation shown for the text before this edit no longer applies. */
    private fun onTextFieldEdited() {
        if (!isBusy) stripState = StripState.Idle
    }

    /** Drops what the strip shows and any translation still running. */
    private fun forgetTranslation() {
        stripState = StripState.Idle
        translatedSource = null
        stripRequest++
    }

    /** Puts the translation or correction the strip shows in place of the text it was made from. */
    fun replaceWithResult() {
        val result = when (val state = stripState) {
            is StripState.Translated -> state.translation
            is StripState.Corrected -> state.correction
            else -> return
        }
        val source = translationSource()
        stripState = StripState.Idle
        if (source != translatedSource) return
        if (!source.isSelection) {
            editor.deleteBeforeCursor(source.before)
            editor.deleteAfterCursor(source.after)
        }
        editor.commit(result)
    }

    /** The text Translate would translate now, and where it sits in the text field. */
    private fun translationSource(): TranslationSource {
        val selected = editor.selectedText()
        if (selected.isNotEmpty()) return TranslationSource(selected, isSelection = true, before = 0, after = 0)
        // One unit more than the limit is enough to know the text is too long.
        val before = editor.textBeforeCursor(MAX_TRANSLATION_CHARS + 1)
        val after = editor.textAfterCursor(MAX_TRANSLATION_CHARS + 1)
        return TranslationSource(before + after, isSelection = false, before = before.length, after = after.length)
    }

    /** Text to translate: the selection, or the text field's text with [before] units before the cursor and [after] after it. */
    private data class TranslationSource(val text: String, val isSelection: Boolean, val before: Int, val after: Int) {
        private val myanmarLetters get() = text.count { it in '\u1000'..'\u109F' }
        private val latinLetters get() = text.count { it in 'A'..'Z' || it in 'a'..'z' }

        /** English text, judged by its letters: the only text a grammar check takes. */
        val isEnglish: Boolean get() = latinLetters > myanmarLetters

        /** Myanmar text becomes English, and anything else becomes Myanmar. */
        val translationTask: ModelTask
            get() = if (myanmarLetters > latinLetters) ModelTask.MyanmarToEnglish else ModelTask.EnglishToMyanmar
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

        /** The keys that change the text field's text. */
        val EDITING_ACTIONS = setOf(Action.Space, Action.Enter, Action.Backspace)

        const val MAX_RECENT_EMOJI = 32

        /** The most UTF-16 units translated at once, so no translation runs for minutes. */
        const val MAX_TRANSLATION_CHARS = 500
    }
}
