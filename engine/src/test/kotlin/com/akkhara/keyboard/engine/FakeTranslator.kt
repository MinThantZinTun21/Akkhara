package com.akkhara.keyboard.engine

/**
 * A translator that answers only when a test tells it to, so tests can see
 * the strip while a translation is still running.
 */
class FakeTranslator : Translator {
    private val pending = ArrayDeque<Pair<Request, (TranslationResult) -> Unit>>()

    data class Request(val text: String, val prompt: String)

    /** Every request made so far, oldest first. */
    val requests = mutableListOf<Request>()

    override fun run(text: String, prompt: String, onResult: (TranslationResult) -> Unit) {
        val request = Request(text, prompt)
        requests += request
        pending += request to onResult
    }

    /** Answers the oldest request still waiting with [translation]. */
    fun answer(translation: String) = next()(TranslationResult.Translated(translation))

    /** Answers the oldest request still waiting with [failure]. */
    fun fail(failure: TranslationFailure = TranslationFailure.Failed) = next()(TranslationResult.Failed(failure))

    private fun next(): (TranslationResult) -> Unit = pending.removeFirst().second
}
