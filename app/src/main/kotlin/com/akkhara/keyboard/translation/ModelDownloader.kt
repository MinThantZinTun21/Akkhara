package com.akkhara.keyboard.translation

import android.app.DownloadManager
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.text.format.Formatter
import androidx.core.content.edit
import androidx.core.net.toUri
import com.akkhara.keyboard.R

/** What a model download or import is doing, for the app screen. */
sealed interface DownloadState {
    data object Idle : DownloadState

    data class Running(val model: TranslationModel, val step: Step, val bytes: Long, val total: Long) : DownloadState

    data class Failed(val model: TranslationModel, val reason: Reason) : DownloadState

    enum class Step { WaitingForWifi, Downloading, Copying, Checking }

    enum class Reason { Failed, NoSpace, NotAModel }
}

/** What a running download is doing, such as "Waiting for Wi-Fi" or "1.2 GB of 3.7 GB". */
fun progressText(context: Context, step: DownloadState.Step, bytes: Long, total: Long): String = when (step) {
    DownloadState.Step.WaitingForWifi -> context.getString(R.string.download_waiting_for_wifi)
    DownloadState.Step.Downloading -> context.getString(
        R.string.download_progress,
        Formatter.formatFileSize(context, bytes),
        Formatter.formatFileSize(context, total),
    )
    DownloadState.Step.Copying -> context.getString(
        R.string.import_progress,
        Formatter.formatFileSize(context, bytes),
        Formatter.formatFileSize(context, total),
    )
    DownloadState.Step.Checking -> context.getString(R.string.download_checking)
}

fun failureText(context: Context, reason: DownloadState.Reason, model: TranslationModel): String = when (reason) {
    DownloadState.Reason.Failed -> context.getString(R.string.download_failed)
    DownloadState.Reason.NotAModel -> context.getString(R.string.import_not_a_model)
    DownloadState.Reason.NoSpace -> context.getString(
        R.string.download_no_space,
        Formatter.formatFileSize(context, model.sizeBytes),
    )
}

/** Where a model download stands, read from Android's DownloadManager. */
sealed interface DownloadProgress {
    data class Running(val step: DownloadState.Step, val bytes: Long, val total: Long) : DownloadProgress

    /** The whole file is there, and still has to pass its checks. */
    data object Complete : DownloadProgress

    data class Failed(val reason: DownloadState.Reason) : DownloadProgress
}

/**
 * What DownloadManager's [status] and [reason] mean for the app. [total] is
 * -1 until the server has said how long the file is; [listedSize] stands in.
 */
fun downloadProgress(status: Int, reason: Int, bytes: Long, total: Long, listedSize: Long): DownloadProgress {
    val size = if (total > 0) total else listedSize
    return when (status) {
        DownloadManager.STATUS_SUCCESSFUL -> DownloadProgress.Complete
        DownloadManager.STATUS_FAILED -> DownloadProgress.Failed(
            if (reason == DownloadManager.ERROR_INSUFFICIENT_SPACE) DownloadState.Reason.NoSpace
            else DownloadState.Reason.Failed,
        )
        DownloadManager.STATUS_PAUSED
            if reason == DownloadManager.PAUSED_QUEUED_FOR_WIFI || reason == DownloadManager.PAUSED_WAITING_FOR_NETWORK ->
            DownloadProgress.Running(DownloadState.Step.WaitingForWifi, bytes, size)
        else -> DownloadProgress.Running(DownloadState.Step.Downloading, bytes, size)
    }
}

/**
 * Downloads listed translation models with Android's DownloadManager
 * (docs/adr/0006), one at a time: the system fetches the pinned file in its
 * own process, Wi-Fi only, with its own progress notification, whatever
 * happens to the app. The app opens no network connection itself.
 *
 * The download in progress is kept in the settings, so the app finds it again
 * after it is killed. Once complete, [ModelCheckJob] checks the file before
 * the model counts as on the phone.
 */
class ModelDownloader(private val context: Context) {
    private val manager = context.getSystemService(DownloadManager::class.java)
    private val models = ModelShelf(context)
    private val preferences = context.getSharedPreferences("akkhara", Context.MODE_PRIVATE)

    /** The model being downloaded or checked, or null. */
    fun model(): TranslationModel? {
        val id = preferences.getString(KEY_MODEL, null) ?: return null
        return ListedModels.all.firstOrNull { it.id == id }
    }

    /** The DownloadManager id of the download in progress, or -1. */
    fun downloadId(): Long = preferences.getLong(KEY_DOWNLOAD, -1)

    /** Starts downloading [model], unless a download is already in progress. A failed one makes way. */
    fun start(model: TranslationModel) {
        val source = model.source ?: return
        if (progress().let { it != null && it !is DownloadProgress.Failed }) return
        cancel()
        val file = models.downloadFile(model) ?: return
        file.parentFile?.mkdirs()
        file.delete()
        val request = DownloadManager.Request(source.url.toUri())
            .setTitle(model.name)
            .setDestinationInExternalFilesDir(context, null, "${file.parentFile?.name}/${file.name}")
            .setAllowedOverMetered(false)
            .setAllowedOverRoaming(false)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
        val id = manager.enqueue(request)
        preferences.edit {
            putString(KEY_MODEL, model.id)
            putLong(KEY_DOWNLOAD, id)
            remove(KEY_FAILED)
        }
    }

    /** Stops the download or its check and deletes what was downloaded. */
    fun cancel() {
        val model = model() ?: return
        context.getSystemService(JobScheduler::class.java).cancel(ModelCheckJob.JOB_ID)
        downloadId().takeIf { it >= 0 }?.let { manager.remove(it) }
        models.downloadFile(model)?.delete()
        forget()
    }

    /** Where the download stands: running, complete, failed, or null when there is none. */
    fun progress(): DownloadProgress? {
        val model = model() ?: return null
        val id = downloadId()
        manager.query(DownloadManager.Query().setFilterById(id)).use { row ->
            // The row is gone when the user cleared it from Android's downloads.
            if (!row.moveToFirst()) return null.also { forget() }
            return downloadProgress(
                status = row.getInt(row.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)),
                reason = row.getInt(row.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON)),
                bytes = row.getLong(row.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)),
                total = row.getLong(row.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)),
                listedSize = model.sizeBytes,
            )
        }
    }

    /**
     * What the app screen shows: the download's state, the check while it
     * runs, or the last failure until another download starts.
     */
    fun state(): DownloadState {
        val model = model()
        if (model == null) {
            val failed = preferences.getString(KEY_FAILED, null) ?: return DownloadState.Idle
            val (id, reason) = failed.split(':').let { it[0] to it.getOrNull(1) }
            val failedModel = ListedModels.all.firstOrNull { it.id == id } ?: return DownloadState.Idle
            return DownloadState.Failed(failedModel, reason?.let(DownloadState.Reason::valueOf) ?: DownloadState.Reason.Failed)
        }
        return when (val progress = progress()) {
            null -> DownloadState.Idle
            is DownloadProgress.Running -> DownloadState.Running(model, progress.step, progress.bytes, progress.total)
            DownloadProgress.Complete -> DownloadState.Running(model, DownloadState.Step.Checking, 0, model.sizeBytes)
            is DownloadProgress.Failed -> DownloadState.Failed(model, progress.reason)
        }
    }

    /** Schedules the check of a complete download, unless it is already scheduled or running. */
    fun checkIfComplete() {
        if (progress() != DownloadProgress.Complete) return
        val scheduler = context.getSystemService(JobScheduler::class.java)
        if (scheduler.getPendingJob(ModelCheckJob.JOB_ID) != null) return
        val job = JobInfo.Builder(ModelCheckJob.JOB_ID, ComponentName(context, ModelCheckJob::class.java))
        // Expedited runs it at once; when Android has none of those left for the app, it runs soon after.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            scheduler.schedule(job.setExpedited(true).build()) == JobScheduler.RESULT_SUCCESS
        ) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) job.setExpedited(false)
        scheduler.schedule(job.setOverrideDeadline(0).build())
    }

    /** The download is over: [reason] says why it failed, or null when it worked or was cancelled. */
    internal fun finish(model: TranslationModel, reason: DownloadState.Reason?) {
        downloadId().takeIf { it >= 0 }?.let { manager.remove(it) }
        forget()
        if (reason != null) preferences.edit { putString(KEY_FAILED, "${model.id}:${reason.name}") }
    }

    private fun forget() {
        preferences.edit {
            remove(KEY_MODEL)
            remove(KEY_DOWNLOAD)
        }
    }

    private companion object {
        const val KEY_MODEL = "download_model"
        const val KEY_DOWNLOAD = "download_id"
        const val KEY_FAILED = "download_failed"
    }
}
