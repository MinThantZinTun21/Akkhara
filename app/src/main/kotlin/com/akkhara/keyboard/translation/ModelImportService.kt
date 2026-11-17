package com.akkhara.keyboard.translation

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.os.storage.StorageManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.akkhara.keyboard.R
import java.io.File
import java.io.FileOutputStream

/** What the model import is doing, for the app screen. Changed on the main thread only. */
object ModelImports {
    var state: DownloadState by mutableStateOf(DownloadState.Idle)
        internal set
}

/**
 * Copies a model file the user imports, in the background, shown in a
 * notification with its progress and Cancel. It uses no network. The copy
 * must open as a model before it becomes the imported model.
 *
 * Listed models are downloaded by Android instead (see [ModelDownloader]).
 */
class ModelImportService : Service() {

    private val main = Handler(Looper.getMainLooper())
    private lateinit var models: ModelShelf
    private lateinit var notifications: NotificationManagerCompat
    private lateinit var checker: TranslatorClient

    private var job: Thread? = null
    private var model: TranslationModel? = null

    /** Set when the user cancels. */
    @Volatile private var cancelled = false

    /** Set when Android stops the import. */
    @Volatile private var stopped = false

    @Volatile private var lastReport = 0L

    override fun onCreate() {
        super.onCreate()
        models = ModelShelf(this)
        notifications = NotificationManagerCompat.from(this)
        checker = TranslatorClient(applicationContext)
    }

    override fun onBind(intent: Intent): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_IMPORT -> {
                val uri = intent.data
                val size = intent.getLongExtra(EXTRA_SIZE, -1)
                val imported = TranslationModel(ModelShelf.IMPORTED_ID, ModelShelf.IMPORTED_NAME, size, source = null)
                val model = model ?: imported
                goForeground(progressNotification(DownloadState.Step.Copying, 0, model.sizeBytes))
                if (job == null && uri != null) start(model) { copy(uri, model) }
                else if (job == null) stopEmpty()
            }
            ACTION_CANCEL -> {
                cancel()
                if (job == null) stopSelf()
            }
            else -> if (job == null) stopSelf()
        }
        return START_NOT_STICKY
    }

    /** Android's daily limit on data-sync work ran out: stop. */
    override fun onTimeout(startId: Int, fgsType: Int) {
        stopped = true
    }

    override fun onDestroy() {
        stopped = true
        checker.close()
        super.onDestroy()
    }

    private fun stopEmpty() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun start(model: TranslationModel, work: () -> Outcome) {
        this.model = model
        cancelled = false
        stopped = false
        report(model, DownloadState.Step.Copying, 0, force = true)
        job = Thread({
            val outcome = try {
                work()
            } catch (_: Exception) {
                Outcome.Failed(DownloadState.Reason.Failed)
            }
            main.post { finish(model, outcome) }
        }, "model-import").apply { start() }
    }

    private fun cancel() {
        cancelled = true
    }

    private sealed interface Outcome {
        data object Done : Outcome
        data object Cancelled : Outcome
        data class Failed(val reason: DownloadState.Reason) : Outcome
    }

    /**
     * Runs on the import thread: copies the file the user chose into a
     * partial file, then makes it the imported model if it opens as one. It
     * replaces any model imported before.
     */
    private fun copy(uri: Uri, model: TranslationModel): Outcome {
        val partial = models.partialFile(model)
        partial.parentFile?.mkdirs()
        partial.delete()
        if (model.sizeBytes > 0 && freeBytes(partial.parentFile!!) < model.sizeBytes + SPARE_BYTES) {
            return Outcome.Failed(DownloadState.Reason.NoSpace)
        }
        val input = contentResolver.openInputStream(uri) ?: return Outcome.Failed(DownloadState.Reason.Failed)
        input.use {
            FileOutputStream(partial).use { output ->
                val buffer = ByteArray(BUFFER_BYTES)
                var copied = 0L
                while (true) {
                    if (cancelled || stopped) {
                        partial.delete()
                        return if (cancelled) Outcome.Cancelled else Outcome.Failed(DownloadState.Reason.Failed)
                    }
                    val read = input.read(buffer)
                    if (read < 0) break
                    output.write(buffer, 0, read)
                    copied += read
                    report(model, DownloadState.Step.Copying, copied)
                }
            }
        }
        report(model, DownloadState.Step.Checking, 0, force = true)
        if (!checker.opens(partial)) {
            partial.delete()
            return Outcome.Failed(DownloadState.Reason.NotAModel)
        }
        return if (models.install(model, partial)) Outcome.Done else Outcome.Failed(DownloadState.Reason.Failed)
    }

    /** How much can be written in [folder], counting cached files Android would clear to make room. */
    private fun freeBytes(folder: File): Long {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return folder.freeSpace
        val storage = getSystemService(StorageManager::class.java) ?: return folder.freeSpace
        return storage.getAllocatableBytes(storage.getUuidForPath(folder))
    }

    /** Shows progress on the screen and in the notification, at most a few times a second unless [force]d. */
    private fun report(model: TranslationModel, step: DownloadState.Step, bytes: Long, force: Boolean = false) {
        val now = SystemClock.elapsedRealtime()
        if (!force && now - lastReport < REPORT_MILLIS) return
        lastReport = now
        val total = model.sizeBytes.coerceAtLeast(bytes)
        main.post {
            ModelImports.state = DownloadState.Running(model, step, bytes, total)
            notify(progressNotification(step, bytes, total))
        }
    }

    private fun finish(model: TranslationModel, outcome: Outcome) {
        job = null
        this.model = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        ModelImports.state = when (outcome) {
            Outcome.Done -> {
                ModelNotifications.ready(this, model)
                DownloadState.Idle
            }
            Outcome.Cancelled -> DownloadState.Idle
            is Outcome.Failed -> {
                ModelNotifications.failed(this, model, outcome.reason)
                DownloadState.Failed(model, outcome.reason)
            }
        }
        stopSelf()
    }

    private fun goForeground(notification: Notification) {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC else 0
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
    }

    private fun progressNotification(step: DownloadState.Step, bytes: Long, total: Long) =
        NotificationCompat.Builder(this, ModelNotifications.channel(this))
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(getString(R.string.import_title))
            .setContentText(progressText(this, step, bytes, total))
            .setProgress(
                PROGRESS_MAX,
                (PROGRESS_MAX * bytes / total.coerceAtLeast(1)).toInt(),
                total <= 0,
            )
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(ModelNotifications.openApp(this))
            .addAction(0, getString(R.string.cancel), cancelIntent(this))
            .build()

    /** Shows the import's progress if the user allows notifications; the import runs either way. */
    @SuppressLint("MissingPermission") // ModelNotifications.allowed checks it.
    private fun notify(notification: Notification) {
        if (ModelNotifications.allowed(this)) notifications.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val ACTION_IMPORT = "com.akkhara.keyboard.action.IMPORT_MODEL"
        private const val ACTION_CANCEL = "com.akkhara.keyboard.action.CANCEL_IMPORT"
        private const val EXTRA_SIZE = "size"
        private const val NOTIFICATION_ID = 1

        private const val BUFFER_BYTES = 1 shl 20
        private const val REPORT_MILLIS = 500L
        private const val PROGRESS_MAX = 1_000

        /** Free space left over after the copy, so the phone is never filled to the last byte. */
        private const val SPARE_BYTES = 500L * 1024 * 1024

        /**
         * Starts copying the model file at [uri], [size] bytes long or -1 if
         * unknown, in the background to become the imported model.
         */
        fun import(context: Context, uri: Uri, size: Long) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, ModelImportService::class.java)
                    .setAction(ACTION_IMPORT)
                    .setData(uri)
                    .putExtra(EXTRA_SIZE, size)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
            )
        }

        /** Stops the import and deletes what it copied. */
        fun cancel(context: Context) {
            context.startService(Intent(context, ModelImportService::class.java).setAction(ACTION_CANCEL))
        }

        private fun cancelIntent(context: Context) = PendingIntent.getService(
            context,
            1,
            Intent(context, ModelImportService::class.java).setAction(ACTION_CANCEL),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}
