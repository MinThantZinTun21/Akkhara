package com.akkhara.keyboard

import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import com.akkhara.keyboard.engine.Editor

/**
 * Adapts Android's input connection to the engine's [Editor] port.
 *
 * The connection and the text field's description change with every text
 * field, so both are looked up on each call. With no connection every read is
 * empty and every edit is dropped.
 */
class InputConnectionEditor(
    private val connection: () -> InputConnection?,
    private val editorInfo: () -> EditorInfo?,
    /** Called just before the field's own action (Send, Search, ...) runs, while its text is still there. */
    private val beforeEnterAction: () -> Unit = {},
    /** Called after each change the keyboard makes to the text, saying whether it deleted. */
    private val onEdit: (deleting: Boolean) -> Unit = {},
) : Editor {

    override fun textBeforeCursor(maxChars: Int): String =
        connection()?.getTextBeforeCursor(maxChars, 0)?.toString().orEmpty()

    override fun textAfterCursor(maxChars: Int): String =
        connection()?.getTextAfterCursor(maxChars, 0)?.toString().orEmpty()

    override fun selectedText(): String =
        connection()?.getSelectedText(0)?.toString().orEmpty()

    override fun deleteAfterCursor(chars: Int) {
        connection()?.deleteSurroundingText(0, chars)
        onEdit(true)
    }

    override fun commit(text: String) {
        connection()?.commitText(text, 1)
        onEdit(false)
    }

    override fun deleteBeforeCursor(chars: Int) {
        connection()?.deleteSurroundingText(chars, 0)
        onEdit(true)
    }

    override fun deleteSelection(): Boolean {
        val ic = connection() ?: return false
        if (ic.getSelectedText(0).isNullOrEmpty()) return false
        ic.commitText("", 1)
        onEdit(true)
        return true
    }

    override fun wantsCapital(): Boolean {
        val ic = connection() ?: return false
        val info = editorInfo() ?: return false
        return ic.getCursorCapsMode(info.inputType) != 0
    }

    override fun performEnterAction() {
        val info = editorInfo() ?: return
        beforeEnterAction()
        connection()?.performEditorAction(info.imeOptions and EditorInfo.IME_MASK_ACTION)
    }
}
