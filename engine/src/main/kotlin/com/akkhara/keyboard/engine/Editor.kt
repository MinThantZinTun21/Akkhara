package com.akkhara.keyboard.engine

/**
 * The engine's only view of the text field it is typing into.
 *
 * The app adapts Android's input connection to this; tests use an in-memory
 * text field.
 */
interface Editor {
    /** Up to [maxChars] UTF-16 units immediately before the cursor (or selection start). */
    fun textBeforeCursor(maxChars: Int): String

    /** Up to [maxChars] UTF-16 units immediately after the cursor (or selection end). */
    fun textAfterCursor(maxChars: Int): String

    /** The selected text; empty when nothing is selected. */
    fun selectedText(): String

    /** Inserts [text] at the cursor, replacing any selection. */
    fun commit(text: String)

    /** Deletes [chars] UTF-16 units immediately before the cursor (or selection start). */
    fun deleteBeforeCursor(chars: Int)

    /** Deletes [chars] UTF-16 units immediately after the cursor (or selection end). */
    fun deleteAfterCursor(chars: Int)

    /** Deletes the selected text. Returns false, changing nothing, when nothing is selected. */
    fun deleteSelection(): Boolean

    /** Whether this text field asks for a capital letter at the cursor, as at the start of a sentence. */
    fun wantsCapital(): Boolean

    /** Performs the text field's own enter action, such as send or search. */
    fun performEnterAction()
}
