package com.akkhara.keyboard.engine

/** The app a text field belongs to: its package, and the name the user knows it by. */
data class SourceApp(val packageName: String, val name: String)

/** One entry of typing history: a text field's whole text when it was saved. See GLOSSARY.md, "Typing history". */
data class HistoryEntry(val text: String, val app: SourceApp, val savedAt: Long)

/** Where typing history is kept. */
fun interface HistoryStore {
    /** Keeps [entry], unless its text is the same as the newest entry from the same app. */
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

    /**
     * A text field of [app] has started. Only a field that [mayKeep] is ever
     * saved. With [sameField], the keyboard is back in the field it was in, so
     * the text held so far is kept rather than saved half-made; it is dropped
     * if the field may no longer be kept.
     */
    fun startField(field: Field, app: SourceApp, sameField: Boolean = false) {
        if (!sameField) leaveField()
        this.app = app.takeIf { mayKeep(field, it) }
        if (!sameField || this.app == null) held = ""
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

    /**
     * The field's text is now [text]; [deletedByKeyboard] says the keyboard's
     * own deletion made it so. Ignored unless [wantsText]. When the app empties
     * the field itself, as a chat app does on send, the text it held is
     * saved; text the user deletes with the keyboard never is.
     */
    fun textChanged(text: String, deletedByKeyboard: Boolean = false) {
        if (!wantsText) return
        if (text.isEmpty() && !deletedByKeyboard) saveHeld()
        held = text
    }

    /** The keyboard's action key (Send, Search, Go, Done) is about to act on the field: its text is finished. */
    fun actionKeyPressed() = saveHeld()

    /** The user has left the field: its text is finished. */
    fun leaveField() {
        saveHeld()
        app = null
    }

    /**
     * Saves the text held, unless it is noise (blank, or one character), and
     * then holds nothing. The store skips text identical to the app's last entry.
     */
    private fun saveHeld() {
        val app = app
        val text = capped(held)
        held = ""
        if (!isOn || app == null || isNoise(text)) return
        store.save(HistoryEntry(text, app, clock()))
    }

    private fun isNoise(text: String): Boolean = text.trim().let { it.codePointCount(0, it.length) <= 1 }

    companion object {
        /** The most of one field's text an entry keeps; the rest, from the start, is dropped. */
        const val MAX_CHARS = 5_000

        /** How long an entry is kept before it deletes itself. */
        const val KEPT_DAYS = 90

        /** The oldest time, at [now], an entry may have been saved and still be kept. */
        fun keptSince(now: Long): Long = now - KEPT_DAYS * 24 * 60 * 60 * 1000L
    }
}
