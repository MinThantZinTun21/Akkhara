package com.akkhara.keyboard

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.akkhara.keyboard.engine.InputLanguage
import com.akkhara.keyboard.engine.ModelTask
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * The user's choices, kept on the device. Nothing typed with the keyboard is
 * ever stored here or anywhere else; the prompts are settings the user writes
 * in the app.
 *
 * The app screen writes these and the keyboard reads them each time it
 * opens; both run in the same process, so a change is seen at once.
 */
class KeyboardSettings internal constructor(private val preferences: SharedPreferences) {
    constructor(context: Context) : this(context.getSharedPreferences("akkhara", Context.MODE_PRIVATE))

    var hapticFeedback: Boolean by toggle("haptic_feedback")
    var keyPreview: Boolean by toggle("key_preview")
    var capitaliseSentences: Boolean by toggle("capitalise_sentences")
    var doubleSpaceFullStop: Boolean by toggle("double_space_full_stop")

    /** Off until the user turns it on, so nobody hears the keyboard who did not ask to. See GLOSSARY.md, "Key sound". */
    var keySound: Boolean by toggle("key_sound", default = false)

    /** How loud the key sound is, from 0 to 100 percent of the phone's system-sounds volume. */
    var keySoundVolume: Int
        get() = preferences.getInt(KEY_KEY_SOUND_VOLUME, DEFAULT_KEY_SOUND_VOLUME)
        set(value) {
            preferences.edit { putInt(KEY_KEY_SOUND_VOLUME, value.coerceIn(0, 100)) }
        }

    /** Off until the user turns it on: each translation keeps the phone busy for a while. */
    var translateAsYouType: Boolean by toggle("translate_as_you_type", default = false)

    /** The theme the user picked, or null while the keyboard follows the system. See GLOSSARY.md, "Theme". */
    var theme: Theme?
        get() {
            val name = preferences.getString(KEY_THEME, null)
            return Theme.entries.firstOrNull { it.name == name }
        }
        set(value) {
            preferences.edit { if (value == null) remove(KEY_THEME) else putString(KEY_THEME, value.name) }
        }

    /** The key style the user picked for every theme, or null while each theme draws its own. See GLOSSARY.md, "Key style". */
    var keyStyle: KeyStyle?
        get() = KeyStyle.named(preferences.getString(KEY_KEY_STYLE, null))
        set(value) {
            preferences.edit { if (value == null) remove(KEY_KEY_STYLE) else putString(KEY_KEY_STYLE, value.name) }
        }

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

    /** The user's own prompt for [task], or an empty string while the task uses its default. See GLOSSARY.md, "Prompt". */
    fun prompt(task: ModelTask): String = preferences.getString(promptKey(task), null).orEmpty()

    /** Keeps [prompt] for [task]. A blank prompt, or the default itself, goes back to the default. */
    fun setPrompt(task: ModelTask, prompt: String) {
        preferences.edit {
            if (prompt.isBlank() || prompt == task.defaultPrompt) remove(promptKey(task))
            else putString(promptKey(task), prompt.take(ModelTask.MAX_PROMPT_CHARS))
        }
    }

    /** Every prompt the user has rewritten, by task. */
    val customPrompts: Map<ModelTask, String>
        get() = ModelTask.entries.associateWith(::prompt).filterValues { it.isNotBlank() }

    private fun promptKey(task: ModelTask) = "prompt_" + task.name

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
        const val KEY_THEME = "theme"
        const val KEY_KEY_STYLE = "key_style"
        const val KEY_KEY_SOUND_VOLUME = "key_sound_volume"
        const val DEFAULT_KEY_SOUND_VOLUME = 30
    }
}
