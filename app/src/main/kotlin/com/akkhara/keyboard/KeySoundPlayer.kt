package com.akkhara.keyboard

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool

/**
 * Plays key sounds. The sounds are built into the app and loaded once, the
 * first time they are wanted; nothing is ever downloaded (ADR 0008).
 *
 * They play as system sounds, so the phone's system-sounds volume and its
 * silent and vibrate modes apply, and [volume] scales them further.
 */
class KeySoundPlayer(private val context: Context) {
    private val audio = context.getSystemService(AudioManager::class.java)
    private var pool: SoundPool? = null
    private var loaded: Map<KeySound, Int> = emptyMap()

    /** From 0 to 1. */
    var volume = 0.3f

    /** Plays [sound], unless the phone is silenced. Loads the sounds on first use, so the very first press may be quiet. */
    fun play(sound: KeySound) {
        if (!KeySound.plays(isOn = true, ringerMode = audio.ringerMode)) return
        val pool = pool ?: load()
        val id = loaded[sound] ?: return
        pool.play(id, volume, volume, 1, 0, 1f)
    }

    /** Frees the loaded sounds. A later [play] loads them again. */
    fun release() {
        pool?.release()
        pool = null
        loaded = emptyMap()
    }

    private fun load(): SoundPool {
        val pool = SoundPool.Builder()
            .setMaxStreams(MAX_STREAMS)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .build()
        loaded = CLICK.mapValues { (_, res) -> pool.load(context, res, 1) }
        this.pool = pool
        return pool
    }

    private companion object {
        /** Enough for fast typing with two thumbs, where one sound has not ended before the next. */
        const val MAX_STREAMS = 4

        val CLICK = mapOf(
            KeySound.Letter to R.raw.key_sound_click_letter,
            KeySound.Space to R.raw.key_sound_click_space,
            KeySound.Delete to R.raw.key_sound_click_delete,
            KeySound.Enter to R.raw.key_sound_click_enter,
        )
    }
}
