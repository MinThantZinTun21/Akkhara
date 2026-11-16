package com.akkhara.keyboard.engine

/**
 * Translates text on the phone. The engine asks; the app answers, usually
 * later and always on the thread the engine runs on.
 */
interface Translator {
    /** Translates [text] in [direction] and calls [onResult] exactly once. */
    fun translate(text: String, direction: TranslationDirection, onResult: (TranslationResult) -> Unit)
}

sealed interface TranslationResult {
    data class Translated(val text: String) : TranslationResult
    data class Failed(val reason: TranslationFailure) : TranslationResult
}

/** Why a translation produced no result. */
enum class TranslationFailure {
    /** There is no translation model on the phone. */
    NotInstalled,

    /** Anything else: the model failed, or the translator went away. */
    Failed,
}
