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
        val downloader = ModelDownloader(this)
        val id = downloader.downloadId()
        Thread({
            val outcome = try {
                check(downloader, id, stop)
            } catch (_: Exception) {
                // An unreadable file, say: the download failed.
                downloader.checked(id, DownloadState.Reason.Failed)
                Outcome.Done
            }
            stopped.remove(params)
            when (outcome) {
                Outcome.Done -> jobFinished(params, false)
                Outcome.Again -> jobFinished(params, true)
                Outcome.Stopped -> Unit
            }
        }, "model-check").start()
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        stopped[params]?.set(true)
        return true
    }

    private enum class Outcome {
        Done,

        /** Android stopped it, and runs it again. */
        Stopped,

        /** The model did not answer in time, which says nothing about the file: it runs again later. */
        Again,
    }

    /** Runs on the worker thread: checks download [id] unless it has ended. */
    private fun check(downloader: ModelDownloader, id: Long, stop: AtomicBoolean): Outcome {
        val model = downloader.model() ?: return Outcome.Done
        if (downloader.downloadId() != id || downloader.progress() != DownloadProgress.Complete) return Outcome.Done
        val file = ModelShelf(this).downloadFile(model)
        if (file == null || !file.isFile) {
            downloader.checked(id, null)
            return Outcome.Done
        }
        val sha256 = sha256(file, stop) ?: return Outcome.Stopped
        if (sha256 != model.source?.sha256) {
            downloader.checked(id, DownloadState.Reason.Failed)
            return Outcome.Done
        }
        val checker = TranslatorClient(applicationContext)
        val opens = try {
            checker.opens(file, stop)
        } finally {
            Handler(Looper.getMainLooper()).post { checker.close() }
        }
        if (stop.get()) return Outcome.Stopped
        when (opens) {
            null -> return if (downloader.openTimedOut(id)) Outcome.Again else Outcome.Done
            true -> downloader.checked(id, null)
            false -> downloader.checked(id, DownloadState.Reason.NotAModel)
        }
        return Outcome.Done
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
 * Null when the model did not answer in time or [stop] was set first.
 */
internal fun TranslatorClient.opens(file: File, stop: AtomicBoolean = AtomicBoolean(false)): Boolean? {
    val answered = CountDownLatch(1)
    val opens = AtomicBoolean(false)
    Handler(Looper.getMainLooper()).post {
        check(file) {
            opens.set(it)
            answered.countDown()
        }
    }
    repeat(OPEN_CHECK_SECONDS) {
        if (answered.await(1, TimeUnit.SECONDS)) return opens.get()
        if (stop.get()) return null
    }
    return null
}

private const val OPEN_CHECK_SECONDS = 180
