package com.akkhara.keyboard

import android.content.Context
import android.content.SharedPreferences
import android.inputmethodservice.InputMethodService
import android.os.SystemClock
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import com.akkhara.keyboard.engine.InputLanguage
import com.akkhara.keyboard.engine.KeyboardEngine

/** Hosts the keyboard View and connects the engine to the current text field. */
class AkkharaImeService : InputMethodService(), KeyboardView.Host {

    private lateinit var preferences: SharedPreferences
    private lateinit var engine: KeyboardEngine
    private var keyboardView: KeyboardView? = null

    override fun onCreate() {
        super.onCreate()
        preferences = getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        engine = KeyboardEngine(
            editor = InputConnectionEditor({ currentInputConnection }, { currentInputEditorInfo }),
            clock = SystemClock::uptimeMillis,
            inputLanguage = lastInputLanguage(),
        )
    }

    override fun onCreateInputView(): View =
        KeyboardView(this).also { view ->
            view.bind(engine, this)
            keyboardView = view
        }

    /**
     * Never the full-screen editor Android offers in landscape: the keyboard
     * keeps to half the screen there, so the text field itself stays in view.
     */
    override fun onEvaluateFullscreenMode(): Boolean = false

    override fun onStartInputView(info: EditorInfo, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        engine.startInput(info.toField())
        keyboardView?.onEngineChanged()
    }

    override fun onUpdateSelection(
        oldSelStart: Int,
        oldSelEnd: Int,
        newSelStart: Int,
        newSelEnd: Int,
        candidatesStart: Int,
        candidatesEnd: Int,
    ) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        val shiftBefore = engine.shift
        engine.onCursorMoved()
        if (engine.shift != shiftBefore) keyboardView?.onEngineChanged()
    }

    /** Runs one key's worth of engine work so the app sees its edits as a single change. */
    override fun edit(work: KeyboardEngine.() -> Unit) {
        val ic = currentInputConnection
        ic?.beginBatchEdit()
        try {
            engine.work()
        } finally {
            ic?.endBatchEdit()
        }
        rememberInputLanguage()
        keyboardView?.onEngineChanged()
    }

    override fun showKeyboardPicker() {
        getSystemService(InputMethodManager::class.java)?.showInputMethodPicker()
    }

    private fun lastInputLanguage(): InputLanguage {
        val name = preferences.getString(KEY_INPUT_LANGUAGE, null)
        return InputLanguage.entries.firstOrNull { it.name == name } ?: InputLanguage.English
    }

    private fun rememberInputLanguage() {
        val current = engine.inputLanguage.name
        if (preferences.getString(KEY_INPUT_LANGUAGE, null) != current) {
            preferences.edit().putString(KEY_INPUT_LANGUAGE, current).apply()
        }
    }

    private companion object {
        const val PREFERENCES = "akkhara"
        const val KEY_INPUT_LANGUAGE = "input_language"
    }
}
