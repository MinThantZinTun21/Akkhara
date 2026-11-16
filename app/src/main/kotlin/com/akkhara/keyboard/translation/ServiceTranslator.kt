package com.akkhara.keyboard.translation

import com.akkhara.keyboard.engine.TranslationDirection
import com.akkhara.keyboard.engine.TranslationResult
import com.akkhara.keyboard.engine.Translator

/** The engine's [Translator], answered by [TranslatorService] in its own process. */
class ServiceTranslator(private val client: TranslatorClient) : Translator {

    override fun translate(text: String, direction: TranslationDirection, onResult: (TranslationResult) -> Unit) {
        client.translate(text, direction) { reply ->
            onResult(
                when (reply) {
                    is TranslatorReply.Translated -> TranslationResult.Translated(reply.text)
                    is TranslatorReply.Failed -> TranslationResult.Failed(reply.reason)
                },
            )
        }
    }
}
