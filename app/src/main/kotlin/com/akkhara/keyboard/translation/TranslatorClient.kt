package com.akkhara.keyboard.translation

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.RemoteException
import com.akkhara.keyboard.engine.TranslationFailure
import com.akkhara.keyboard.translation.TranslatorProtocol.KEY_BACKEND
import com.akkhara.keyboard.translation.TranslatorProtocol.KEY_LOAD_MS
import com.akkhara.keyboard.translation.TranslatorProtocol.KEY_MODEL
import com.akkhara.keyboard.translation.TranslatorProtocol.KEY_OPENS
import com.akkhara.keyboard.translation.TranslatorProtocol.KEY_PROMPT
import com.akkhara.keyboard.translation.TranslatorProtocol.KEY_REASON
import com.akkhara.keyboard.translation.TranslatorProtocol.KEY_TEXT
import com.akkhara.keyboard.translation.TranslatorProtocol.KEY_TRANSLATE_MS
import java.io.File

/**
 * The app's side of [TranslatorService]: binds to it, sends one request at a
 * time, and calls back on the main thread. If the translator process dies, the
 * request in flight fails and the next one binds again.
 */
class TranslatorClient(private val context: Context) {

    private val main = Handler(Looper.getMainLooper())
    private var service: Messenger? = null
    private var bound = false
    private var queued: Message? = null
    private var waiting: ((Message) -> Unit)? = null

    private val replies = Messenger(Handler(Looper.getMainLooper()) { reply ->
        val callback = waiting ?: return@Handler true
        waiting = null
        callback(reply)
        true
    })

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            service = Messenger(binder)
            queued?.let { queued = null; deliver(it) }
        }

        override fun onServiceDisconnected(name: ComponentName) {
            service = null
            fail()
        }

        override fun onBindingDied(name: ComponentName) {
            unbind()
            fail()
        }
    }

    /**
     * Gives the [model] [text] with [prompt] as its instruction. A request
     * made while another is in flight replaces its callback.
     */
    fun translate(
        text: String,
        prompt: String,
        model: File,
        backend: TranslatorBackend = TranslatorBackend.Cpu,
        onReply: (TranslatorReply) -> Unit,
    ) {
        waiting = { onReply(it.toTranslatorReply()) }
        send(
            Message.obtain(null, TranslatorProtocol.MSG_TRANSLATE).apply {
                data = Bundle().apply {
                    putString(KEY_TEXT, text)
                    putString(KEY_PROMPT, prompt)
                    putString(KEY_MODEL, model.path)
                    putString(KEY_BACKEND, backend.name)
                }
                replyTo = replies
            },
        )
    }

    /** Tells [onResult] whether [model] opens as a translation model: the open check of a new file. */
    fun check(model: File, onResult: (Boolean) -> Unit) {
        waiting = { onResult(it.what == TranslatorProtocol.MSG_CHECKED && it.data.getBoolean(KEY_OPENS)) }
        send(
            Message.obtain(null, TranslatorProtocol.MSG_CHECK).apply {
                data = Bundle().apply { putString(KEY_MODEL, model.path) }
                replyTo = replies
            },
        )
    }

    /** Asks the translator to free the model's memory now, such as before the model's file is removed. */
    fun unload() = send(Message.obtain(null, TranslatorProtocol.MSG_UNLOAD))

    private fun send(request: Message) {
        if (service != null) deliver(request) else {
            queued = request
            bind()
        }
    }

    /** Stops using the translator. Any request in flight is dropped without a callback. */
    fun close() {
        waiting = null
        queued = null
        unbind()
    }

    private fun bind() {
        if (bound) return
        bound = context.bindService(Intent(context, TranslatorService::class.java), connection, Context.BIND_AUTO_CREATE)
        if (!bound) fail()
    }

    private fun unbind() {
        if (bound) context.unbindService(connection)
        bound = false
        service = null
    }

    private fun deliver(request: Message) {
        try {
            service!!.send(request)
        } catch (_: RemoteException) {
            unbind()
            fail()
        }
    }

    private fun fail() {
        val callback = waiting ?: return
        waiting = null
        queued = null
        main.post { callback(Message.obtain(null, TranslatorProtocol.MSG_FAILED)) }
    }
}

private fun Message.toTranslatorReply(): TranslatorReply = when (what) {
    TranslatorProtocol.MSG_TRANSLATED -> TranslatorReply.Translated(
        text = data.getString(KEY_TEXT).orEmpty(),
        timings = TranslationTimings(
            loadMs = data.getLong(KEY_LOAD_MS),
            translateMs = data.getLong(KEY_TRANSLATE_MS),
        ),
    )
    else -> TranslatorReply.Failed(
        data.getString(KEY_REASON)?.let(TranslationFailure::valueOf) ?: TranslationFailure.Failed,
    )
}
