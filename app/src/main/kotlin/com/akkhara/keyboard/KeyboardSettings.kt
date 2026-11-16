package com.akkhara.keyboard

import android.content.Context
import androidx.core.content.edit
import com.akkhara.keyboard.engine.InputLanguage
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * The user's choices, kept on the device. Nothing the user types is ever
 * stored here or anywhere else.
 *
 * The app screen writes these and the keyboard reads them each time it
 * opens; both run in the same process, so a change is seen at once.
 */
class KeyboardSettings(context: Context) {
    private val preferences = context.getSharedPreferences("akkhara", Context.MODE_PRIVATE)

    var hapticFeedback: Boolean by toggle("haptic_feedback")
    var keyPreview: Boolean by toggle("key_preview")
    var capitaliseSentences: Boolean by toggle("capitalise_sentences")
    var doubleSpaceFullStop: Boolean by toggle("double_space_full_stop")

    /** Off until the user turns it on: each translation keeps the phone busy for a while. */
    var translateAsYouType: Boolean by toggle("translate_as_you_type", default = false)

    /** The input language the keyboard was last typing, so it opens in the same one. */
    var inputLanguage: InputLanguage
        get() {
            val name = preferences.getString(KEY_INPUT_LANGUAGE, null)
            return InputLanguage.entries.firstOrNull { it.name == name } ?: InputLanguage.English
        }
        set(value) {
            if (preferences.getString(KEY_INPUT_LANGUAGE, null) != value.name) {
                preferences.edit { putString(KEY_INPUT_LANGUAGE, value.name) }
            }
        }

    /** A setting that is [default] until the user changes it. */
    private fun toggle(key: String, default: Boolean = true) = object : ReadWriteProperty<Any?, Boolean> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): Boolean =
            preferences.getBoolean(key, default)

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) {
            preferences.edit { putBoolean(key, value) }
        }
    }

    private companion object {
        const val KEY_INPUT_LANGUAGE = "input_language"
    }
}
