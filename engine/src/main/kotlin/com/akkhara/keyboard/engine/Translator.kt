package com.akkhara.keyboard.engine

/**
 * Runs the translation model on the phone. The engine asks; the app answers,
 * usually later and always on the thread the engine runs on.
 */
interface Translator {
    /**
     * Gives a model [text] to do [task] with [prompt] as its instruction, and
     * calls [onResult] exactly once with the reply. The task lets the app pick
     * the model, as a grammar check may need another model than translation.
     */
    fun run(text: String, task: ModelTask, prompt: String, onResult: (TranslationResult) -> Unit)
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

    /** The text is longer than is translated at once; it was not sent. */
    TooLong,
}
