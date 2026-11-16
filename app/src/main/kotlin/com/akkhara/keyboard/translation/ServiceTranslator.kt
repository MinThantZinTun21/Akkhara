package com.akkhara.keyboard.translation

import com.akkhara.keyboard.engine.ModelTask
import com.akkhara.keyboard.engine.TranslationFailure
import com.akkhara.keyboard.engine.TranslationResult
import com.akkhara.keyboard.engine.Translator

/**
 * The engine's [Translator], answered by [TranslatorService] in its own
 * process. Models are read afresh from [models] for each request: the model
 * in use translates, and the grammar check model checks grammar.
 */
class ServiceTranslator(
    private val client: TranslatorClient,
    private val models: ModelShelf,
) : Translator {

    override fun run(text: String, task: ModelTask, prompt: String, onResult: (TranslationResult) -> Unit) {
        val model = (if (task == ModelTask.GrammarCheck) models.grammarCheckModel() else models.inUse())
            ?: return onResult(TranslationResult.Failed(TranslationFailure.NotInstalled))
        val (message, instruction) = model.ask(task, text, prompt)
        client.translate(message, instruction, models.file(model), model.asking) { reply ->
            onResult(
                when (reply) {
                    is TranslatorReply.Translated -> TranslationResult.Translated(reply.text)
                    is TranslatorReply.Failed -> TranslationResult.Failed(reply.reason)
                },
            )
        }
    }
}

/**
 * The message and the instruction [this] model is sent for [task]: [text] with
 * [prompt], or, for a model that translates only, its own message and no instruction.
 */
internal fun TranslationModel.ask(task: ModelTask, text: String, prompt: String): Pair<String, String> =
    if (translatesOnly) translationOnlyMessage(task, text) to "" else text to prompt

/**
 * What a model that translates only is sent: Tencent's own instruction for
 * Hy-MT2, naming the language to translate into, then the text.
 */
private fun translationOnlyMessage(task: ModelTask, text: String): String {
    val into = if (task == ModelTask.MyanmarToEnglish) "English" else "Burmese"
    return "Translate the following text into $into. " +
        "Note that you should **only output the translated result without any additional explanation**:\n\n$text"
}
