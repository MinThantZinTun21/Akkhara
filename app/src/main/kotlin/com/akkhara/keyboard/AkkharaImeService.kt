package com.akkhara.keyboard

import android.content.pm.PackageManager
import android.content.res.Configuration
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.core.view.WindowCompat
import com.akkhara.keyboard.engine.Conveniences
import com.akkhara.keyboard.engine.KeyboardEngine
import com.akkhara.keyboard.engine.SourceApp
import com.akkhara.keyboard.engine.TypingHistoryRecorder
import com.akkhara.keyboard.translation.ServiceTranslator
import com.akkhara.keyboard.translation.ModelShelf
import com.akkhara.keyboard.translation.TranslatorClient

/** Hosts the keyboard View and connects the engine to the current text field. */
class AkkharaImeService : InputMethodService(), KeyboardView.Host {

    private lateinit var settings: KeyboardSettings
    private lateinit var models: ModelShelf
    private lateinit var engine: KeyboardEngine
    private var keyboardView: KeyboardView? = null
    private lateinit var translatorClient: TranslatorClient
    private lateinit var keySoundPlayer: KeySoundPlayer

    /** Stops following key sound changes; holding it also keeps the listener alive. */
    private lateinit var stopWatchingKeySound: () -> Unit

    /** Keeps typing history while the user has it on (ADR 0009). */
    private lateinit var typingHistory: TypingHistoryRecorder

    /** Stops following the Remember what I type switch; holding it also keeps the listener alive. */
    private lateinit var stopWatchingRememberTyping: () -> Unit

    /** Whether the field started since the keyboard last opened is a new one, not the field it was in. */
    private var fieldIsNew = false

    /**
     * Whether the keyboard's last edit was a deletion, and the field's text has
     * not been read since, so a field it emptied is known to be the user's own doing.
     */
    private var keyboardDeleted = false

    /** Whether the field has text selected, which sits between the text before and after the cursor. */
    private var hasSelection = false

    /** Each app's name, looked up once. */
    private val appNames = mutableMapOf<String, SourceApp>()

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
        models = ModelShelf(this)
        translatorClient = TranslatorClient(this)
        keySoundPlayer = KeySoundPlayer(this)
        stopWatchingKeySound = settings.watchKeySound { keySoundPlayer.follow(settings) }
        typingHistory = TypingHistoryRecorder(TypingHistoryDatabase.get(this), System::currentTimeMillis, ownApp = packageName)
        typingHistory.isOn = settings.rememberTyping
        stopWatchingRememberTyping = settings.watchRememberTyping {
            typingHistory.isOn = settings.rememberTyping
            readFieldText()
        }
        engine = KeyboardEngine(
            editor = InputConnectionEditor(
                connection = { currentInputConnection },
                editorInfo = { currentInputEditorInfo },
                beforeEnterAction = {
                    readFieldText()
                    typingHistory.actionKeyPressed()
                },
                onEdit = { deleting -> keyboardDeleted = deleting },
            ),
            clock = SystemClock::uptimeMillis,
            inputLanguage = settings.inputLanguage,
            translator = ServiceTranslator(translatorClient, models),
        )
        // A translation arrives later, on its own: redraw the strip to show it.
        engine.onStripChanged = { keyboardView?.onEngineChanged() }
    }

    override fun onDestroy() {
        handler.removeCallbacks(typingPaused)
        translatorClient.close()
        stopWatchingKeySound()
        stopWatchingRememberTyping()
        typingHistory.leaveField()
        keySoundPlayer.release()
        super.onDestroy()
    }

    override fun onCreateInputView(): View {
        val theme = shownTheme()
        return KeyboardView(this, theme, KeyStyle.shown(settings.keyStyle, theme)).also { view ->
            view.bind(engine, this)
            keyboardView = view
            fitNavigationBarTo(theme, view)
        }
    }

    /** The theme to draw now: the user's choice, or Light or Dark from the phone while following the system. */
    private fun shownTheme(): Theme = Theme.shown(
        chosen = settings.theme,
        systemIsDark = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
            Configuration.UI_MODE_NIGHT_YES,
    )

    /**
     * Makes the navigation bar part of the keyboard: the theme's background,
     * with icons that suit the theme rather than the phone's light or dark mode.
     * See docs/research/keyboard-themes.md, part 2.
     */
    private fun fitNavigationBarTo(theme: Theme, view: View) {
        val window = window?.window ?: return
        WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !theme.isDark
        // From Android 15 the keyboard draws its own background behind the bar; this
        // stops the system laying a scrim over it when three-button navigation is on.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) window.isNavigationBarContrastEnforced = false
        // Before Android 15 the bar has a colour of its own.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            @Suppress("DEPRECATION")
            // The bar sits below the keyboard, so it takes the bottom of a gradient.
            window.navigationBarColor = theme.palette.backgroundBottom
        }
    }

    /**
     * Never the full-screen editor Android offers in landscape: the keyboard
     * keeps to half the screen there, so the text field itself stays in view.
     */
    override fun onEvaluateFullscreenMode(): Boolean = false

    override fun onStartInput(attribute: EditorInfo, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        if (!restarting) fieldIsNew = true
    }

    /** The keyboard is opening: this is when a changed setting takes effect. */
    override fun onStartInputView(info: EditorInfo, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        // A newly chosen theme or key style is drawn by a new View, as each View keeps the ones it was built with.
        val theme = shownTheme()
        val view = keyboardView
        if (view?.theme != theme || view.keyStyle != KeyStyle.shown(settings.keyStyle, theme)) setInputView(onCreateInputView())
        engine.isTranslationAvailable = models.inUse() != null
        engine.isGrammarCheckAvailable = models.grammarCheckModel() != null
        engine.translateAsYouType = settings.translateAsYouType
        engine.customPrompts = settings.customPrompts
        val field = info.toField()
        val app = sourceApp(info.packageName)
        if (app != null) typingHistory.startField(field, app, sameField = !fieldIsNew) else typingHistory.leaveField()
        fieldIsNew = false
        hasSelection = info.initialSelStart != info.initialSelEnd
        readFieldText()
        engine.startInput(
            field,
            Conveniences(
                capitaliseSentences = settings.capitaliseSentences,
                doubleSpaceFullStop = settings.doubleSpaceFullStop,
            ),
        )
        keySoundPlayer.follow(settings)
        keyboardView?.apply {
            hapticsEnabled = settings.hapticFeedback
            keySoundPlayer = this@AkkharaImeService.keySoundPlayer
            keyPreviewEnabled = settings.keyPreview
            onEngineChanged()
        }
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        edit { finishInput() }
        super.onFinishInputView(finishingInput)
    }

    /** The user has left the field, not just closed the keyboard: its text is finished. */
    override fun onFinishInput() {
        typingHistory.leaveField()
        super.onFinishInput()
    }

    /**
     * Tells typing history the field's text, read only while it would be saved.
     * It reads at most [TypingHistoryRecorder.MAX_CHARS] each side of the
     * cursor, so a long document is never copied whole on a key press.
     */
    private fun readFieldText() {
        val deleted = keyboardDeleted
        keyboardDeleted = false
        if (!typingHistory.wantsText) return
        val ic = currentInputConnection ?: return
        val max = TypingHistoryRecorder.MAX_CHARS
        // Null means the app could not answer, which is not the same as an empty field.
        val before = ic.getTextBeforeCursor(max, 0) ?: return
        val selected = if (hasSelection) ic.getSelectedText(0) ?: return else ""
        val after = ic.getTextAfterCursor(max, 0) ?: return
        typingHistory.textChanged("$before$selected$after", deletedByKeyboard = deleted)
    }

    /** The app a field belongs to, named as the launcher names it, or by its package when that is unknown. */
    private fun sourceApp(packageName: String?): SourceApp? {
        val name = packageName ?: return null
        return appNames.getOrPut(name) {
            val label = try {
                packageManager.getApplicationLabel(packageManager.getApplicationInfo(name, 0)).toString()
            } catch (_: PackageManager.NameNotFoundException) {
                name
            }
            SourceApp(name, label)
        }
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
        hasSelection = newSelStart != newSelEnd
        readFieldText()
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
