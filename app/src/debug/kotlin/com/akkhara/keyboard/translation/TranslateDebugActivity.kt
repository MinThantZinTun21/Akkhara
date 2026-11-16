package com.akkhara.keyboard.translation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.akkhara.keyboard.AkkharaTheme
import com.akkhara.keyboard.engine.ModelTask
import java.util.Locale

/**
 * Debug builds only. Translates one text at a time through [TranslatorService]
 * and shows how long it took, so the model can be judged on a real phone (#12).
 */
class TranslateDebugActivity : ComponentActivity() {

    private lateinit var client: TranslatorClient
    private lateinit var models: ModelShelf

    private var text by mutableStateOf("")
    private var direction by mutableStateOf(ModelTask.MyanmarToEnglish)
    private var backend by mutableStateOf(TranslatorBackend.Cpu)
    private var busy by mutableStateOf(false)
    private var result by mutableStateOf("")
    private var timings by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        client = TranslatorClient(applicationContext)
        models = ModelShelf(applicationContext)
        setContent { AkkharaTheme { Surface(Modifier.fillMaxSize()) { Screen() } } }
    }

    override fun onDestroy() {
        client.close()
        super.onDestroy()
    }

    @androidx.compose.runtime.Composable
    private fun Screen() {
        Column(
            Modifier.safeDrawingPadding().padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val model = modelFile()
            Text("Translate (debug)", style = MaterialTheme.typography.headlineSmall)
            Text(
                if (model != null) "Model: ${model.name}, %.2f GB".format(Locale.ROOT, model.length() / 1e9)
                else "No translation model on the phone. Download one in the Akkhara app.",
                style = MaterialTheme.typography.bodySmall,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ModelTask.entries.forEach {
                    FilterChip(direction == it, onClick = { direction = it }, label = { Text(it.name) })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TranslatorBackend.entries.forEach {
                    FilterChip(backend == it, onClick = { backend = it }, label = { Text(it.name.uppercase()) })
                }
            }
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Text to translate") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = ::translate, enabled = !busy && text.isNotBlank()) {
                    Text(if (busy) "Translating…" else "Translate")
                }
                OutlinedButton(onClick = { client.unload(); timings = "Unloaded" }, enabled = !busy) { Text("Unload") }
                OutlinedButton(onClick = ::runSamples, enabled = !busy) { Text("Run samples") }
            }
            SelectionContainer { Text(result, style = MaterialTheme.typography.bodyLarge) }
            SelectionContainer { Text(timings, style = MaterialTheme.typography.bodySmall) }
        }
    }

    /** The file of the model in use, or null while there is none. */
    private fun modelFile() = models.inUse()?.let(models::file)

    /** Translates every sample sentence in turn and lists each result with its timings. */
    private fun runSamples() {
        val model = modelFile() ?: return
        busy = true
        result = ""
        timings = ""
        fun next(index: Int) {
            if (index == sampleSentences.size) {
                busy = false
                return
            }
            val (sampleTask, sample) = sampleSentences[index]
            client.translate(sample, sampleTask.defaultPrompt, model, backend) { reply ->
                result += "${index + 1}. $sample\n→ " + when (reply) {
                    is TranslatorReply.Translated -> reply.text + "\n   [" + reply.timings.describe(backend, reply.text) + "]"
                    is TranslatorReply.Failed -> "Failed: ${reply.reason}"
                } + "\n\n"
                next(index + 1)
            }
        }
        next(0)
    }

    private fun translate() {
        val model = modelFile() ?: return
        busy = true
        result = ""
        timings = ""
        client.translate(text, direction.defaultPrompt, model, backend) { reply ->
            busy = false
            when (reply) {
                is TranslatorReply.Translated -> {
                    result = reply.text
                    timings = reply.timings.describe(backend, reply.text)
                }
                is TranslatorReply.Failed -> result = "Failed: ${reply.reason}"
            }
        }
    }
}

private fun TranslationTimings.describe(backend: TranslatorBackend, translation: String): String =
    "Backend ${backend.name.uppercase()} · load $loadMs ms · translate $translateMs ms · " +
        "%.1f chars/s".format(Locale.ROOT, translation.length * 1000.0 / translateMs.coerceAtLeast(1))
