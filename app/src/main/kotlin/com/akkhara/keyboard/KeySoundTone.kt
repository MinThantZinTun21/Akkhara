package com.akkhara.keyboard

import androidx.annotation.RawRes
import androidx.annotation.StringRes

/** One of the key sound's tones the user picks from, each with its four sounds. See GLOSSARY.md, "Tone". */
enum class KeySoundTone(
    @StringRes val nameRes: Int,
    @RawRes private val letter: Int,
    @RawRes private val space: Int,
    @RawRes private val delete: Int,
    @RawRes private val enter: Int,
) {
    Click(
        R.string.key_sound_click,
        R.raw.key_sound_click_letter, R.raw.key_sound_click_space, R.raw.key_sound_click_delete, R.raw.key_sound_click_enter,
    ),
    SoftTap(
        R.string.key_sound_soft_tap,
        R.raw.key_sound_tap_letter, R.raw.key_sound_tap_space, R.raw.key_sound_tap_delete, R.raw.key_sound_tap_enter,
    ),
    Bubble(
        R.string.key_sound_bubble,
        R.raw.key_sound_bubble_letter, R.raw.key_sound_bubble_space, R.raw.key_sound_bubble_delete, R.raw.key_sound_bubble_enter,
    ),
    Typewriter(
        R.string.key_sound_typewriter,
        R.raw.key_sound_typewriter_letter, R.raw.key_sound_typewriter_space,
        R.raw.key_sound_typewriter_delete, R.raw.key_sound_typewriter_enter,
    ),
    Glass(
        R.string.key_sound_glass,
        R.raw.key_sound_glass_letter, R.raw.key_sound_glass_space, R.raw.key_sound_glass_delete, R.raw.key_sound_glass_enter,
    ),
    Wood(
        R.string.key_sound_wood,
        R.raw.key_sound_wood_letter, R.raw.key_sound_wood_space, R.raw.key_sound_wood_delete, R.raw.key_sound_wood_enter,
    ),
    Retro(
        R.string.key_sound_retro,
        R.raw.key_sound_retro_letter, R.raw.key_sound_retro_space, R.raw.key_sound_retro_delete, R.raw.key_sound_retro_enter,
    ),
    Chips(
        R.string.key_sound_chips,
        R.raw.key_sound_chips_letter, R.raw.key_sound_chips_space, R.raw.key_sound_chips_delete, R.raw.key_sound_chips_enter,
    ),
    ;

    /** The file this tone plays for [sound]. */
    @RawRes
    fun fileFor(sound: KeySound): Int = when (sound) {
        KeySound.Letter -> letter
        KeySound.Space -> space
        KeySound.Delete -> delete
        KeySound.Enter -> enter
    }

    companion object {
        /** The tone stored as [name], or Click for none or one this version does not have. */
        fun named(name: String?): KeySoundTone = entries.firstOrNull { it.name == name } ?: Click
    }
}
