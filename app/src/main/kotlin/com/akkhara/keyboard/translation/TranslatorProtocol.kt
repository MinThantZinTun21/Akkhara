package com.akkhara.keyboard.translation

import com.akkhara.keyboard.engine.TranslationFailure

/**
 * The messages the keyboard process and the translator process exchange.
 * They are plain `Message`s with a `Bundle`, so nothing crosses the process
 * boundary but the text being translated and its result.
 */
internal object TranslatorProtocol {
    const val MSG_TRANSLATE = 1
    const val MSG_UNLOAD = 2
    const val MSG_TRANSLATED = 10
    const val MSG_FAILED = 11

    const val KEY_TEXT = "text"
    const val KEY_PROMPT = "prompt"
    const val KEY_BACKEND = "backend"
    const val KEY_REASON = "reason"
    const val KEY_LOAD_MS = "load_ms"
    const val KEY_TRANSLATE_MS = "translate_ms"
}

/** Where the model runs. The faster one that works on the owner's phone is the default (#12). */
enum class TranslatorBackend { Cpu, Gpu }

/** How long a translation took, for the debug screen. */
data class TranslationTimings(
    /** Time spent loading the model for this request; 0 when it was already loaded. */
    val loadMs: Long,
    val translateMs: Long,
)

sealed interface TranslatorReply {
    data class Translated(val text: String, val timings: TranslationTimings) : TranslatorReply
    data class Failed(val reason: TranslationFailure) : TranslatorReply
}
