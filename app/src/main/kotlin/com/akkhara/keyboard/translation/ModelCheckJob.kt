package com.akkhara.keyboard.translation

import android.app.job.JobParameters
import android.app.job.JobService
import android.os.Handler
import android.os.Looper
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Checks a model Android has finished downloading (docs/adr/0006): it must
 * have the listed SHA-256 and open as a model. Then it is on the phone;
 * otherwise its file is deleted and the download counts as failed.
 *
 * It runs as soon as the download completes, whether or not the app is open,
 * and when the app next opens if it could not run then.
 */
class ModelCheckJob : JobService() {

    /** Set when Android stops a run of the check, which runs again later. One flag for each run. */
    private val stopped = ConcurrentHashMap<JobParameters, AtomicBoolean>()

    override fun onStartJob(params: JobParameters): Boolean {
        val stop = AtomicBoolean(false)
        stopped[params] = stop
        Thread({
            val finished = try {
                check(stop)
            } catch (_: Exception) {
                // An unreadable file, say: the download failed.
                val downloader = ModelDownloader(this)
                downloader.settle { downloader.model()?.let { fail(downloader, it, ModelShelf(this).downloadFile(it)) } }
                true
            }
            stopped.remove(params)
            if (finished) jobFinished(params, false)
        }, "model-check").start()
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        stopped[params]?.set(true)
        return true
    }

    /** Runs on the worker thread. False when it was [stop]ped before it finished. */
    private fun check(stop: AtomicBoolean): Boolean {
        val downloader = ModelDownloader(this)
        val shelf = ModelShelf(this)
        val model = downloader.model() ?: return true
        val id = downloader.downloadId()
        if (downloader.progress() != DownloadProgress.Complete) return true
        val file = shelf.downloadFile(model)
        val source = model.source
        if (file != null && !file.isFile && shelf.isOnPhone(model)) {
            // An earlier run installed it and was killed before it said so.
            downloader.finish(model, null)
            return true
        }
        if (file == null || source == null || !file.isFile) {
            fail(downloader, model, file)
            return true
        }
        val sha256 = sha256(file, stop) ?: return false
        val checker = TranslatorClient(applicationContext)
        val opens = try {
            sha256 == source.sha256 && checker.opens(file)
        } finally {
            Handler(Looper.getMainLooper()).post { checker.close() }
        }
        if (stop.get()) return false
        downloader.settle {
            // Cancelled while it was checked: the cancel deleted the file, and nothing is reported.
            if (downloader.downloadId() != id) return@settle
            if (opens && shelf.install(model, file)) {
                downloader.finish(model, null)
                ModelNotifications.ready(this, model)
            } else {
                fail(downloader, model, file)
            }
        }
        return true
    }

    /** The downloaded [file] is no good: it goes, and the user is told. */
    private fun fail(downloader: ModelDownloader, model: TranslationModel, file: File?) {
        file?.delete()
        downloader.finish(model, DownloadState.Reason.Failed)
        ModelNotifications.failed(this, model, DownloadState.Reason.Failed)
    }

    /** The file's SHA-256 in lower-case hex, or null if the check was stopped while reading it. */
    private fun sha256(file: File, stop: AtomicBoolean): String? {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(BUFFER_BYTES)
        file.inputStream().use { input ->
            while (true) {
                if (stop.get()) return null
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
