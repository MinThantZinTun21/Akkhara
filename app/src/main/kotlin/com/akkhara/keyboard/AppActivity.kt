package com.akkhara.keyboard

import android.content.Intent
import android.database.ContentObserver
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.akkhara.keyboard.translation.ModelDownloadService
import com.akkhara.keyboard.translation.ModelShelf
import com.akkhara.keyboard.translation.TranslatorClient

/**
 * The app screen: setup, a place to try the keyboard, and the settings.
 *
 * Setup happens in Android's own screens, so its two steps are read back from
 * Android whenever they may have changed rather than remembered here.
 */
class AppActivity : ComponentActivity() {

    private var setup by mutableStateOf(Setup(isEnabled = false, isSelected = false))
    private lateinit var translator: TranslatorClient

    private val setupObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) = readSetup()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val settings = KeyboardSettings(this)
        val models = ModelShelf(this)
        translator = TranslatorClient(applicationContext)
        setContent {
            AkkharaTheme {
                AppScreen(
                    setup = setup,
                    settings = settings,
                    models = models,
                    onOpenKeyboardSettings = { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) },
                    onChooseKeyboard = { inputMethodManager().showInputMethodPicker() },
                    onDownloadModel = { ModelDownloadService.download(this, it) },
                    onImportModel = { uri, size -> ModelDownloadService.import(this, uri, size) },
                    onCancelDownload = { ModelDownloadService.cancel(this) },
                    onRemoveModel = { model ->
                        // The translator lets go of the file first, so its space is freed at once.
                        translator.unload()
                        models.remove(model)
                    },
                )
            }
        }
    }

    override fun onDestroy() {
        translator.close()
        super.onDestroy()
    }

    override fun onStart() {
        super.onStart()
        for (name in listOf(Settings.Secure.ENABLED_INPUT_METHODS, Settings.Secure.DEFAULT_INPUT_METHOD)) {
            contentResolver.registerContentObserver(Settings.Secure.getUriFor(name), false, setupObserver)
        }
        readSetup()
    }

    override fun onStop() {
        contentResolver.unregisterContentObserver(setupObserver)
        super.onStop()
    }

    /** Android's keyboard picker is a dialog over this screen, so closing it resumes nothing. */
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) readSetup()
    }

    private fun readSetup() {
        val isEnabled = inputMethodManager().enabledInputMethodList.any { it.packageName == packageName }
        val selected = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
        setup = Setup(
            isEnabled = isEnabled,
            isSelected = isEnabled && selected?.substringBefore('/') == packageName,
        )
    }

    private fun inputMethodManager() = getSystemService(InputMethodManager::class.java)
}

/** How far the user has got with setup. The keyboard cannot be selected until it is enabled. */
data class Setup(val isEnabled: Boolean, val isSelected: Boolean)
