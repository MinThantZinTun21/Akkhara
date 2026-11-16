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
) : Editor {

    override fun textBeforeCursor(maxChars: Int): String =
        connection()?.getTextBeforeCursor(maxChars, 0)?.toString().orEmpty()

    override fun commit(text: String) {
        connection()?.commitText(text, 1)
    }

    override fun deleteBeforeCursor(chars: Int) {
        connection()?.deleteSurroundingText(chars, 0)
    }

    override fun deleteSelection(): Boolean {
        val ic = connection() ?: return false
        if (ic.getSelectedText(0).isNullOrEmpty()) return false
        ic.commitText("", 1)
        return true
    }

    override fun wantsCapital(): Boolean {
        val ic = connection() ?: return false
        val info = editorInfo() ?: return false
        return ic.getCursorCapsMode(info.inputType) != 0
    }

    override fun performEnterAction() {
        val info = editorInfo() ?: return
        connection()?.performEditorAction(info.imeOptions and EditorInfo.IME_MASK_ACTION)
    }
}
