package com.akkhara.keyboard.translation

import android.app.job.JobParameters
import android.app.job.JobService
import android.os.Handler
import android.os.Looper
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Checks a model Android has finished downloading (docs/adr/0006): it must
 * have the listed SHA-256 and open as a model. Then it is on the phone;
 * otherwise its file is deleted and the download counts as failed.
 *
 * It runs as soon as the download completes, whether or not the app is open,
 * and when the app next opens if it could not run then.
 */
class ModelCheckJob : JobService() {

    private var worker: Thread? = null

    /** Set when Android stops the check; it runs again later. */
    @Volatile private var stopped = false

    override fun onStartJob(params: JobParameters): Boolean {
        stopped = false
        worker = Thread({
            val finished = check()
            if (finished) jobFinished(params, false)
        }, "model-check").apply { start() }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        stopped = true
        return true
    }

    /** Runs on the worker thread. False when it was stopped before it finished. */
    private fun check(): Boolean {
        val downloader = ModelDownloader(this)
        val shelf = ModelShelf(this)
        val model = downloader.model() ?: return true
        if (downloader.progress() != DownloadProgress.Complete) return true
        val file = shelf.downloadFile(model)
        val source = model.source
        if (file == null || source == null || !file.isFile) {
            downloader.finish(model, DownloadState.Reason.Failed)
            ModelNotifications.failed(this, model, DownloadState.Reason.Failed)
            return true
        }
        val sha256 = sha256(file) ?: return false
        val checker = TranslatorClient(applicationContext)
        val opens = try {
            sha256 == source.sha256 && checker.opens(file)
        } finally {
            Handler(Looper.getMainLooper()).post { checker.close() }
        }
        if (stopped) return false
        val installed = opens && shelf.install(model, file)
        if (installed) {
            downloader.finish(model, null)
            ModelNotifications.ready(this, model)
        } else {
            file.delete()
            downloader.finish(model, DownloadState.Reason.Failed)
            ModelNotifications.failed(this, model, DownloadState.Reason.Failed)
        }
        return true
    }

    /** The file's SHA-256 in lower-case hex, or null if the check was stopped while reading it. */
    private fun sha256(file: File): String? {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(BUFFER_BYTES)
        file.inputStream().use { input ->
            while (true) {
                if (stopped) return null
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    companion object {
        const val JOB_ID = 1
        private const val BUFFER_BYTES = 1 shl 20
    }
}

/**
 * The open check, run in the translator's own process so a large model never
 * loads in this one. Blocks the calling thread, which must not be the main one.
 */
internal fun TranslatorClient.opens(file: File): Boolean {
    val answered = CountDownLatch(1)
    var opens = false
    Handler(Looper.getMainLooper()).post {
        check(file) {
            opens = it
            answered.countDown()
        }
    }
    return answered.await(OPEN_CHECK_MINUTES, TimeUnit.MINUTES) && opens
}

private const val OPEN_CHECK_MINUTES = 3L
