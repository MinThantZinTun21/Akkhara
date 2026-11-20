package com.akkhara.keyboard

import android.media.AudioManager
import com.akkhara.keyboard.engine.Action
import com.akkhara.keyboard.engine.ActionKey
import com.akkhara.keyboard.engine.Key

/**
 * Which of a tone's sounds a press plays: Space, Delete and Enter have their
 * own, and every other key, emoji included, plays the letter sound.
 * See GLOSSARY.md, "Key sound".
 */
enum class KeySound {
    Letter,
    Space,
    Delete,
    Enter,
    ;

    companion object {
        fun of(key: Key): KeySound = when ((key as? ActionKey)?.action) {
            Action.Space -> Space
            Action.Backspace -> Delete
            Action.Enter -> Enter
            else -> Letter
        }

        /** A key sound plays only while the user has it on and the phone is neither silent nor on vibrate. */
        fun plays(isOn: Boolean, ringerMode: Int): Boolean = isOn && ringerMode == AudioManager.RINGER_MODE_NORMAL
    }
}
