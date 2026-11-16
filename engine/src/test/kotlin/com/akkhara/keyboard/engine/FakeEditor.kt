package com.akkhara.keyboard.engine

/**
 * A text field held in memory. The only test double: tests press keys on the
 * engine and then read [text] to see what a user would see.
 */
class FakeEditor(initial: String = "") : Editor {
    private val buffer = StringBuilder(initial)
    private var selectionStart = initial.length
    private var selectionEnd = initial.length

    val text: String get() = buffer.toString()

    fun moveCursorTo(position: Int) {
        selectionStart = position
        selectionEnd = position
    }

    fun select(start: Int, end: Int) {
        selectionStart = start
        selectionEnd = end
    }

    override fun textBeforeCursor(maxChars: Int): String =
        buffer.substring(maxOf(0, selectionStart - maxChars), selectionStart)

    override fun commit(text: String) {
        buffer.replace(selectionStart, selectionEnd, text)
        selectionStart += text.length
        selectionEnd = selectionStart
    }

    override fun deleteBeforeCursor(chars: Int) {
        val from = maxOf(0, selectionStart - chars)
        buffer.delete(from, selectionStart)
        selectionEnd -= selectionStart - from
        selectionStart = from
    }

    override fun deleteSelection(): Boolean {
        if (selectionStart == selectionEnd) return false
        buffer.delete(selectionStart, selectionEnd)
        selectionEnd = selectionStart
        return true
    }
}
