package com.akkhara.keyboard.engine

/** The app a text field belongs to: its package, and the name the user knows it by. */
data class SourceApp(val packageName: String, val name: String)

/** One entry of typing history: a text field's whole text when it was saved. See GLOSSARY.md, "Typing history". */
data class HistoryEntry(val text: String, val app: SourceApp, val savedAt: Long)

/** Where typing history is kept. */
fun interface HistoryStore {
    fun save(entry: HistoryEntry)
}

/**
 * Decides what typing history keeps, and when (ADR 0009). It is told about
 * the text field and its text, holds that text in memory only, and hands an
 * entry to the [store] when the field's text is finished.
 */
class TypingHistoryRecorder(
    private val store: HistoryStore,
    private val clock: () -> Long,
    private val ownApp: String,
) {
    /** Whether the user has typing history on. Turning it off forgets the text held so far. */
    var isOn = false
        set(value) {
            field = value
            if (!value) held = ""
        }

    private var app: SourceApp? = null
    private var held = ""

    /** A text field of [app] has started. Only a field that [mayKeep] is ever saved. */
    fun startField(field: Field, app: SourceApp) {
        leaveField()
        this.app = app.takeIf { mayKeep(field, it) }
        held = ""
    }

    /**
     * Whether text from [field] of [app] may be kept: ordinary writing and
     * addresses, never passwords, numbers, phones, dates or times, never a
     * field the app marks as private, and never the Akkhara app's own fields.
     */
    private fun mayKeep(field: Field, app: SourceApp): Boolean =
        (field.kind == FieldKind.Text || field.kind == FieldKind.Address) &&
            !field.isPassword && field.mayLearn && app.packageName != ownApp

    /** The last [MAX_CHARS] of [text], never starting halfway through an emoji or other paired character. */
    private fun capped(text: String): String {
        if (text.length <= MAX_CHARS) return text
        val tail = text.takeLast(MAX_CHARS)
        return if (tail.first().isLowSurrogate()) tail.drop(1) else tail
    }

    /** Whether the current field's text would be saved, so is worth reading. */
    val wantsText: Boolean get() = isOn && app != null

    /** The field's text is now [text]. Ignored unless [wantsText]. */
    /**
     * The field's text is now [text]; [byKeyboard] says the keyboard's own
     * last edit made it so. Ignored unless [wantsText]. When the app empties
     * the field itself, as a chat app does on send, the text it held is
     * saved; text the user deletes with the keyboard never is.
     */
    fun textChanged(text: String, byKeyboard: Boolean = false) {
        if (!wantsText) return
        if (text.isEmpty() && !byKeyboard) saveHeld()
        held = text
    }

    /** The keyboard's action key (Send, Search, Go, Done) is about to act on the field: its text is finished. */
    fun actionKeyPressed() = saveHeld()

    /** The user has left the field: its text is finished. */
    fun leaveField() {
        saveHeld()
        app = null
    }

    /** The last text saved from each app, so the same text is not saved twice in a row. */
    private val lastSaved = mutableMapOf<String, String>()

    /**
     * Saves the text held, unless it is noise (blank, or one character) or the
     * same as the last entry from the same app, and then holds nothing.
     */
    private fun saveHeld() {
        val app = app
        val text = capped(held)
        held = ""
        if (!isOn || app == null || isNoise(text) || lastSaved[app.packageName] == text) return
        store.save(HistoryEntry(text, app, clock()))
        lastSaved[app.packageName] = text
    }

    private fun isNoise(text: String): Boolean = text.trim().let { it.codePointCount(0, it.length) <= 1 }

    companion object {
        /** The most of one field's text an entry keeps; the rest, from the start, is dropped. */
        const val MAX_CHARS = 5_000
    }
}
