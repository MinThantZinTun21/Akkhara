package com.akkhara.keyboard.engine

/**
 * All keyboard behaviour: which keys are showing and what pressing one does to
 * the text field. It knows nothing about Android.
 */
class KeyboardEngine(private val editor: Editor) {

    /** The keys of the layer that is showing, top row first. */
    val rows: List<List<Key>> get() = EnglishLayout.base

    fun press(key: Key) {
        when (key) {
            is CharKey -> editor.commit(key.char)
            is ActionKey -> perform(key.action)
        }
    }

    private fun perform(action: Action) {
        when (action) {
            Action.Space -> editor.commit(" ")
            Action.Enter -> editor.commit("\n")
            Action.Backspace -> backspace()
        }
    }

    private fun backspace() {
        if (editor.deleteSelection()) return
        val before = editor.textBeforeCursor(2)
        if (before.isEmpty()) return
        val endsWithSurrogatePair = before.length == 2 &&
            Character.isSurrogatePair(before[0], before[1])
        editor.deleteBeforeCursor(if (endsWithSurrogatePair) 2 else 1)
    }
}
