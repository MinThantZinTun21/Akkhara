package com.akkhara.keyboard.translation

import com.akkhara.keyboard.engine.TranslationFailure
import com.akkhara.keyboard.engine.TranslationResult
import com.akkhara.keyboard.engine.Translator
import java.io.File

/**
 * The engine's [Translator], answered by [TranslatorService] in its own
 * process with the [model in use][modelInUse], read afresh for each request.
 */
class ServiceTranslator(
    private val client: TranslatorClient,
    private val modelInUse: () -> File?,
) : Translator {

    override fun run(text: String, prompt: String, onResult: (TranslationResult) -> Unit) {
        val model = modelInUse() ?: return onResult(TranslationResult.Failed(TranslationFailure.NotInstalled))
        client.translate(text, prompt, model) { reply ->
            onResult(
                when (reply) {
                    is TranslatorReply.Translated -> TranslationResult.Translated(reply.text)
                    is TranslatorReply.Failed -> TranslationResult.Failed(reply.reason)
                },
            )
        }
    }
}
