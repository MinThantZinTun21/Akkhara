package com.akkhara.keyboard

import android.inputmethodservice.InputMethodService
import android.view.View
import com.akkhara.keyboard.engine.KeyboardEngine

/** Hosts the keyboard View and connects the engine to the current text field. */
class AkkharaImeService : InputMethodService() {

    private val engine = KeyboardEngine(InputConnectionEditor { currentInputConnection })
    private var keyboardView: KeyboardView? = null

    override fun onCreateInputView(): View =
        KeyboardView(this).also { view ->
            view.bind(engine, ::runAsOneEdit)
            keyboardView = view
        }

    /** Runs one key's worth of engine work so the app sees its edits as a single change. */
    private fun runAsOneEdit(work: KeyboardEngine.() -> Unit) {
        val ic = currentInputConnection
        ic?.beginBatchEdit()
        try {
            engine.work()
        } finally {
            ic?.endBatchEdit()
        }
        keyboardView?.onEngineChanged()
    }
}
