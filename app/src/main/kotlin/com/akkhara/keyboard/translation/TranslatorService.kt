package com.akkhara.keyboard.translation

import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.RemoteException
import android.os.SystemClock
import com.akkhara.keyboard.engine.TranslationFailure
import com.akkhara.keyboard.translation.TranslatorProtocol.KEY_ASKING
import com.akkhara.keyboard.translation.TranslatorProtocol.KEY_BACKEND
import com.akkhara.keyboard.translation.TranslatorProtocol.KEY_LOAD_MS
import com.akkhara.keyboard.translation.TranslatorProtocol.KEY_MODEL
import com.akkhara.keyboard.translation.TranslatorProtocol.KEY_OPENS
import com.akkhara.keyboard.translation.TranslatorProtocol.KEY_PROMPT
import com.akkhara.keyboard.translation.TranslatorProtocol.KEY_REASON
import com.akkhara.keyboard.translation.TranslatorProtocol.KEY_TEXT
import com.akkhara.keyboard.translation.TranslatorProtocol.KEY_TRANSLATE_MS
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.SamplerConfig
import java.io.File

/**
 * Runs the translation model in its own process (`:translator`), so the 1–2 GB
 * it needs can never cost the user their keyboard: if Android stops this
 * process, only the translation in progress fails.
 *
 * Each request names the model file to use. It loads on the first request and
 * stays loaded until a request names another model (the model in use changed),
 * [TranslatorProtocol.MSG_UNLOAD], or the service is destroyed. Requests run
 * one at a time on a worker thread.
 * Nothing the user types or the model writes is logged or stored.
 */
class TranslatorService : Service() {

    private lateinit var worker: HandlerThread
    private lateinit var work: Handler
    private lateinit var messenger: Messenger

    /** Touched only on the worker thread. */
    private var engine: Engine? = null
    private var engineModel: String? = null
    private var engineBackend: TranslatorBackend? = null

    override fun onCreate() {
        super.onCreate()
        worker = HandlerThread("translator").apply { start() }
        work = Handler(worker.looper)
        messenger = Messenger(Handler(Looper.getMainLooper(), ::onRequest))
    }

    override fun onBind(intent: Intent): IBinder = messenger.binder

    override fun onDestroy() {
        work.post(::unload)
        worker.quitSafely()
        super.onDestroy()
    }

    private fun onRequest(request: Message): Boolean {
        when (request.what) {
            TranslatorProtocol.MSG_TRANSLATE -> {
                val replyTo = request.replyTo ?: return true
                val data = Bundle(request.data)
                work.post { send(replyTo, translate(data)) }
            }
            TranslatorProtocol.MSG_CHECK -> {
                val replyTo = request.replyTo ?: return true
                val model = request.data.getString(KEY_MODEL) ?: return true
                work.post { send(replyTo, check(model)) }
            }
            TranslatorProtocol.MSG_UNLOAD -> work.post(::unload)
            else -> return false
        }
        return true
    }

    private fun translate(data: Bundle): Message {
        val text = data.getString(KEY_TEXT).orEmpty()
        val prompt = data.getString(KEY_PROMPT) ?: return failed(TranslationFailure.Failed)
        val asking = data.getString(KEY_ASKING)?.let(Asking::valueOf) ?: Asking.Chat
        val backend = data.getString(KEY_BACKEND)?.let(TranslatorBackend::valueOf) ?: TranslatorBackend.Cpu
        val model = data.getString(KEY_MODEL)?.takeIf { File(it).isFile }
            ?: return failed(TranslationFailure.NotInstalled)

        return try {
            val loadStart = SystemClock.elapsedRealtime()
            val engine = engineFor(model, backend)
            val loadMs = SystemClock.elapsedRealtime() - loadStart

            val config = ConversationConfig(
                systemInstruction = Contents.of(prompt),
                samplerConfig = SamplerConfig(topK = 1, topP = 1.0, temperature = 0.0, seed = 0),
                extraContext = if (asking == Asking.ChatWithoutThinking) mapOf("enable_thinking" to false) else emptyMap(),
            )
            val start = SystemClock.elapsedRealtime()
            engine.createConversation(config).use { conversation ->
                val reply = conversation.sendMessage(text)
                val translateMs = SystemClock.elapsedRealtime() - start
                val translation = reply.contents.contents
                    .filterIsInstance<Content.Text>()
                    .joinToString("") { it.text }
                    .trim()
                Message.obtain(null, TranslatorProtocol.MSG_TRANSLATED).apply {
                    this.data = Bundle().apply {
                        putString(KEY_TEXT, translation)
                        putLong(KEY_LOAD_MS, loadMs)
                        putLong(KEY_TRANSLATE_MS, translateMs)
                    }
                }
            }
        } catch (_: Exception) {
            failed(TranslationFailure.Failed)
        } catch (_: LinkageError) {
            // LiteRT-LM ships 64-bit libraries only, so a 32-bit phone cannot load it.
            failed(TranslationFailure.Failed)
        }
    }

    private fun engineFor(modelPath: String, backend: TranslatorBackend): Engine {
        engine?.let { if (engineModel == modelPath && engineBackend == backend) return it }
        unload()
        return load(modelPath, backend).also {
            engine = it
            engineModel = modelPath
            engineBackend = backend
        }
    }

    private fun load(modelPath: String, backend: TranslatorBackend): Engine {
        val config = EngineConfig(
            modelPath = modelPath,
            backend = when (backend) {
                TranslatorBackend.Cpu -> Backend.CPU()
                TranslatorBackend.Gpu -> Backend.GPU()
            },
            cacheDir = cacheDir.path,
        )
        return Engine(config).apply { initialize() }
    }

    /**
     * The open check a downloaded or imported file must pass before it counts
     * as on the phone: it loads as a model. The loaded model is freed first,
     * so two never take memory at once.
     */
    private fun check(modelPath: String): Message {
        unload()
        val opens = try {
            load(modelPath, TranslatorBackend.Cpu).close()
            true
        } catch (_: Exception) {
            false
        } catch (_: LinkageError) {
            false
        }
        return Message.obtain(null, TranslatorProtocol.MSG_CHECKED).apply {
            data = Bundle().apply { putBoolean(KEY_OPENS, opens) }
        }
    }

    private fun unload() {
        engine?.close()
        engine = null
        engineModel = null
        engineBackend = null
    }

    private fun failed(reason: TranslationFailure): Message =
        Message.obtain(null, TranslatorProtocol.MSG_FAILED).apply {
            data = Bundle().apply { putString(KEY_REASON, reason.name) }
        }

    private fun send(replyTo: Messenger, reply: Message) {
        try {
            replyTo.send(reply)
        } catch (_: RemoteException) {
            // The keyboard went away; nobody is waiting for this result.
        }
    }
}
