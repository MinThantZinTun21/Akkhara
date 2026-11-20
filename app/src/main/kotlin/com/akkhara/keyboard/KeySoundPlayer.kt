package com.akkhara.keyboard

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool

/**
 * Plays key sounds. The sounds are built into the app; nothing is ever
 * downloaded (ADR 0008). They load when the user's choices are followed
 * with key sound on, and are freed while it is off.
 *
 * They play as system sounds, so the phone's system-sounds volume and its
 * silent and vibrate modes apply, and the user's volume scales them further.
 */
class KeySoundPlayer(private val context: Context) {
    private val audio = context.getSystemService(AudioManager::class.java)
    private var pool: SoundPool? = null

    /** The id SoundPool gave each of the tone's sounds when it began loading it. */
    private var sampleIds: Map<KeySound, Int> = emptyMap()

    /** The sample ids that have finished loading, so play at once. */
    private val ready = mutableSetOf<Int>()

    /** A preview asked for before its sound had loaded, played as soon as it has. */
    private var waitingPreview: Int? = null

    private var isOn = false

    /** From 0 to 1. */
    private var volume = 0f

    /** The tone whose sounds are loaded. Changing it frees the old tone's sounds. */
    private var tone = KeySoundTone.Click
        set(value) {
            if (value != field) release()
            field = value
        }

    /**
     * Takes the user's key sound choices from [settings]. While key sound is
     * on, this loads the sounds, so the first press is heard; while it is
     * off, it frees them.
     */
    fun follow(settings: KeyboardSettings) {
        isOn = settings.keySound
        if (!isOn) return release()
        tone = settings.keySoundTone
        volume = settings.keySoundVolume / 100f
        if (pool == null) load()
    }

    /**
     * Plays [sound] for a key press. A press before the sounds have loaded
     * plays nothing: a sound that comes late is worse than none.
     */
    fun play(sound: KeySound) {
        if (!KeySound.plays(isOn, audio.ringerMode)) return
        val pool = pool ?: return
        val id = sampleIds[sound]?.takeIf { it in ready } ?: return
        start(pool, id)
    }

    /** Plays the tone's letter sound for the user to hear in the app, waiting for it to load if it must. */
    fun preview() {
        if (!KeySound.plays(isOn, audio.ringerMode)) return
        val pool = pool ?: load()
        val id = sampleIds[KeySound.Letter] ?: return
        if (id in ready) start(pool, id) else waitingPreview = id
    }

    /** Frees the loaded sounds. */
    fun release() {
        pool?.release()
        pool = null
        sampleIds = emptyMap()
        ready.clear()
        waitingPreview = null
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
            if (loadedPool !== this.pool) return@setOnLoadCompleteListener
            val preview = waitingPreview.takeIf { it == id }
            if (preview != null) waitingPreview = null
            if (status != LOADED) return@setOnLoadCompleteListener
            ready += id
            if (preview != null) start(loadedPool, id)
        }
        this.pool = pool
        // SoundPool gives 0 for a sound it could not start loading; such a sound never plays.
        sampleIds = KeySound.entries.associateWith { pool.load(context, tone.fileFor(it), 1) }.filterValues { it != 0 }
        return pool
    }

    private fun start(pool: SoundPool, id: Int) {
        pool.play(id, volume, volume, 1, 0, 1f)
    }

    private companion object {
        /** Enough for fast typing with two thumbs, where one sound has not ended before the next. */
        const val MAX_STREAMS = 4

        /** The status SoundPool reports for a sound that loaded. */
        const val LOADED = 0
    }
}
