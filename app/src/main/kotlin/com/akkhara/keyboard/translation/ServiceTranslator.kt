package com.akkhara.keyboard.translation

import com.akkhara.keyboard.engine.TranslationFailure
import com.akkhara.keyboard.engine.TranslationResult
import com.akkhara.keyboard.engine.Translator

/**
 * The engine's [Translator], answered by [TranslatorService] in its own
 * process with the model in use, read afresh from [models] for each request.
 */
class ServiceTranslator(
    private val client: TranslatorClient,
    private val models: ModelShelf,
) : Translator {

    override fun run(text: String, prompt: String, onResult: (TranslationResult) -> Unit) {
        val model = models.inUse() ?: return onResult(TranslationResult.Failed(TranslationFailure.NotInstalled))
        client.translate(text, prompt, models.file(model), model.asking) { reply ->
            onResult(
                when (reply) {
                    is TranslatorReply.Translated -> TranslationResult.Translated(reply.text)
                    is TranslatorReply.Failed -> TranslationResult.Failed(reply.reason)
                },
            )
        }
    }
}
