package com.akkhara.keyboard

import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import com.akkhara.keyboard.engine.Conveniences
import com.akkhara.keyboard.engine.KeyboardEngine
import com.akkhara.keyboard.translation.ServiceTranslator
import com.akkhara.keyboard.translation.TranslationModel
import com.akkhara.keyboard.translation.TranslatorClient

/** Hosts the keyboard View and connects the engine to the current text field. */
class AkkharaImeService : InputMethodService(), KeyboardView.Host {

    private lateinit var settings: KeyboardSettings
    private lateinit var engine: KeyboardEngine
    private var keyboardView: KeyboardView? = null
    private lateinit var translatorClient: TranslatorClient
    private val handler = Handler(Looper.getMainLooper())

    /**
     * Tells the engine typing has paused, for translate as you type. It only
     * touches the strip, never the text field, so it is not an [edit].
     */
    private val typingPaused = Runnable {
        engine.onTypingPaused()
        keyboardView?.onEngineChanged()
    }

    override fun onCreate() {
        super.onCreate()
        settings = KeyboardSettings(this)
        translatorClient = TranslatorClient(this)
        engine = KeyboardEngine(
            editor = InputConnectionEditor({ currentInputConnection }, { currentInputEditorInfo }),
            clock = SystemClock::uptimeMillis,
            inputLanguage = settings.inputLanguage,
            translator = ServiceTranslator(translatorClient),
        )
        // A translation arrives later, on its own: redraw the strip to show it.
        engine.onStripChanged = { keyboardView?.onEngineChanged() }
    }

    override fun onDestroy() {
        handler.removeCallbacks(typingPaused)
        translatorClient.close()
        super.onDestroy()
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

    /** The keyboard is opening: this is when a changed setting takes effect. */
    override fun onStartInputView(info: EditorInfo, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        engine.isTranslationAvailable = TranslationModel.isInstalled(this)
        engine.translateAsYouType = settings.translateAsYouType
        engine.startInput(
            info.toField(),
            Conveniences(
                capitaliseSentences = settings.capitaliseSentences,
                doubleSpaceFullStop = settings.doubleSpaceFullStop,
            ),
        )
        keyboardView?.apply {
            hapticsEnabled = settings.hapticFeedback
            keyPreviewEnabled = settings.keyPreview
            onEngineChanged()
        }
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        edit { finishInput() }
        super.onFinishInputView(finishingInput)
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
        settings.inputLanguage = engine.inputLanguage
        keyboardView?.onEngineChanged()
        // Each key pressed while the strip is open starts the wait for a pause afresh.
        handler.removeCallbacks(typingPaused)
        if (engine.translateAsYouType && engine.strip != null) handler.postDelayed(typingPaused, TYPING_PAUSE_MILLIS)
    }

    override fun showKeyboardPicker() {
        getSystemService(InputMethodManager::class.java)?.showInputMethodPicker()
    }

    private companion object {
        /** How long typing must stop before translate as you type translates. */
        const val TYPING_PAUSE_MILLIS = 1000L
    }
}
