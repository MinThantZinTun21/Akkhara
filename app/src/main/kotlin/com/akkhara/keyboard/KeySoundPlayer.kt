package com.akkhara.keyboard

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool

/**
 * Plays key sounds. The sounds are built into the app and loaded the first
 * time they are wanted; nothing is ever downloaded (ADR 0008).
 *
 * They play as system sounds, so the phone's system-sounds volume and its
 * silent and vibrate modes apply, and [volume] scales them further.
 */
class KeySoundPlayer(private val context: Context) {
    private val audio = context.getSystemService(AudioManager::class.java)
    private var pool: SoundPool? = null
    private var loaded: Map<KeySound, Int> = emptyMap()
    private val ready = mutableSetOf<Int>()

    /** A sound asked for before it had loaded, played as soon as it has. */
    private var waiting: Int? = null

    /** Whether the user has key sound on. Only [preview] plays while it is off. */
    var isOn = false

    /** From 0 to 1. */
    var volume = 0.3f

    var tone = KeySoundTone.Click
        set(value) {
            if (value != field) release()
            field = value
        }

    /** Plays [sound] for a key press. */
    fun play(sound: KeySound) = play(sound, isPreview = false)

    /** Plays the letter sound of the [tone], so the user can hear it in the app. */
    fun preview() = play(KeySound.Letter, isPreview = true)

    /** Frees the loaded sounds. The next sound loads them again. */
    fun release() {
        pool?.release()
        pool = null
        loaded = emptyMap()
        ready.clear()
        waiting = null
    }

    private fun play(sound: KeySound, isPreview: Boolean) {
        if (!KeySound.plays(isOn, audio.ringerMode, isPreview)) return
        val pool = pool ?: load()
        val id = loaded[sound] ?: return
        if (id in ready) pool.play(id, volume, volume, 1, 0, 1f) else waiting = id
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
        pool.setOnLoadCompleteListener { loadedPool, id, status ->
            if (status != 0 || loadedPool !== this.pool) return@setOnLoadCompleteListener
            ready += id
            if (waiting == id) {
                waiting = null
                loadedPool.play(id, volume, volume, 1, 0, 1f)
            }
        }
        this.pool = pool
        loaded = KeySound.entries.associateWith { pool.load(context, tone.sound(it), 1) }
        return pool
    }

    private companion object {
        /** Enough for fast typing with two thumbs, where one sound has not ended before the next. */
        const val MAX_STREAMS = 4
    }
}
