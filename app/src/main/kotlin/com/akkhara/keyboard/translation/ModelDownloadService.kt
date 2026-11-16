package com.akkhara.keyboard.translation

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.os.storage.StorageManager
import android.text.format.Formatter
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.akkhara.keyboard.AppActivity
import com.akkhara.keyboard.R
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** What the model download service is doing, for the app screen. Changed on the main thread only. */
object ModelDownloads {
    var state: DownloadState by mutableStateOf(DownloadState.Idle)
        internal set
}

sealed interface DownloadState {
    data object Idle : DownloadState

    data class Running(val model: TranslationModel, val step: Step, val bytes: Long, val total: Long) : DownloadState

    data class Failed(val model: TranslationModel, val reason: Reason) : DownloadState

    enum class Step { WaitingForWifi, Downloading, Copying, Checking }

    enum class Reason { Failed, NoSpace, NotAModel }
}

/**
 * The model download service: downloads one of Akkhara's listed translation
 * models (docs/adr/0005) in the background, one at a time, over unmetered
 * networks only, shown in a notification with its progress and Cancel.
 *
 * This is the only code in the app that uses the network (docs/adr/0004). It
 * fetches the pinned URL of a listed model and nothing else.
 *
 * The file is written to a partial file in private storage, continued with a
 * range request after a dropped connection, and counts as on the phone only
 * once it has the listed SHA-256 and opens as a model.
 *
 * It also copies a model file the user imports, which uses no network: the
 * copy runs in the background in the same way and must pass the same open
 * check. Only one download or import runs at a time.
 */
class ModelDownloadService : Service() {

    private val main = Handler(Looper.getMainLooper())
    private lateinit var models: ModelShelf
    private lateinit var notifications: NotificationManagerCompat
    private lateinit var checker: TranslatorClient

    private var job: Thread? = null
    private var model: TranslationModel? = null

    /** Set when the user cancels: the download stops and its partial file is deleted. */
    @Volatile private var cancelled = false

    /** Set when Android stops the download: it stops, and the partial file is kept to continue later. */
    @Volatile private var stopped = false

    @Volatile private var connection: HttpURLConnection? = null
    @Volatile private var lastReport = 0L

    override fun onCreate() {
        super.onCreate()
        models = ModelShelf(this)
        notifications = NotificationManagerCompat.from(this)
        notifications.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL, NotificationManagerCompat.IMPORTANCE_LOW)
                .setName(getString(R.string.download_channel))
                .build(),
        )
        checker = TranslatorClient(applicationContext)
    }

    override fun onBind(intent: Intent): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_DOWNLOAD -> {
                val requested = ListedModels.all.firstOrNull { it.id == intent.getStringExtra(EXTRA_MODEL) }
                val model = model ?: requested
                if (model == null) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                goForeground(progressNotification(model, DownloadState.Step.Downloading, 0, model.sizeBytes))
                // One download at a time: a second request while one runs changes nothing.
                if (job == null) start(model) { download(model) }
            }
            ACTION_IMPORT -> {
                val uri = intent.data
                val size = intent.getLongExtra(EXTRA_SIZE, -1)
                val imported = TranslationModel(ModelShelf.IMPORTED_ID, ModelShelf.IMPORTED_NAME, size, source = null)
                val model = model ?: imported
                goForeground(progressNotification(model, DownloadState.Step.Copying, 0, model.sizeBytes))
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

    /** Android's daily limit on data-sync work ran out: stop, keeping what has been downloaded. */
    override fun onTimeout(startId: Int, fgsType: Int) {
        stopped = true
        connection?.disconnect()
    }

    override fun onDestroy() {
        stopped = true
        connection?.disconnect()
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
        val first = if (model.source != null) DownloadState.Step.Downloading else DownloadState.Step.Copying
        report(model, first, models.partialFile(model).length(), force = true)
        job = Thread({
            val outcome = try {
                work()
            } catch (_: Exception) {
                Outcome.Failed(DownloadState.Reason.Failed)
            }
            main.post { finish(model, outcome) }
        }, "model-download").apply { start() }
    }

    private fun cancel() {
        cancelled = true
        connection?.disconnect()
    }

    private sealed interface Outcome {
        data object Done : Outcome
        data object Cancelled : Outcome
        data class Failed(val reason: DownloadState.Reason) : Outcome
    }

    /** Runs on the download thread. */
    private fun download(model: TranslationModel): Outcome {
        val source = model.source ?: return Outcome.Failed(DownloadState.Reason.Failed)
        val partial = models.partialFile(model)
        partial.parentFile?.mkdirs()
        if (partial.length() > model.sizeBytes) partial.delete()

        var attemptsWithoutProgress = 0
        while (partial.length() < model.sizeBytes) {
            if (cancelled || stopped) return stop(partial)
            val have = partial.length()
            if (freeBytes(partial.parentFile!!) < model.sizeBytes - have + SPARE_BYTES) {
                return Outcome.Failed(DownloadState.Reason.NoSpace)
            }
            val network = unmeteredNetwork()
            if (network == null) {
                report(model, DownloadState.Step.WaitingForWifi, have, force = true)
                pause(WIFI_POLL_MILLIS)
                continue
            }
            report(model, DownloadState.Step.Downloading, have, force = true)
            try {
                fetch(network, source.url, partial, model)
            } catch (_: IOException) {
                if (cancelled || stopped) return stop(partial)
            }
            if (partial.length() > have) {
                attemptsWithoutProgress = 0
            } else if (++attemptsWithoutProgress >= MAX_ATTEMPTS_WITHOUT_PROGRESS) {
                return Outcome.Failed(DownloadState.Reason.Failed)
            } else {
                pause(RETRY_MILLIS)
            }
        }
        if (cancelled || stopped) return stop(partial)

        report(model, DownloadState.Step.Checking, 0, force = true)
        val sha256 = sha256(partial, model) ?: return stop(partial)
        if (sha256 != source.sha256 || !opens(partial)) {
            partial.delete()
            return Outcome.Failed(DownloadState.Reason.Failed)
        }
        return if (models.install(model, partial)) Outcome.Done else Outcome.Failed(DownloadState.Reason.Failed)
    }

    /**
     * Runs on the download thread: copies the file the user chose into a
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
        if (!opens(partial)) {
            partial.delete()
            return Outcome.Failed(DownloadState.Reason.NotAModel)
        }
        return if (models.install(model, partial)) Outcome.Done else Outcome.Failed(DownloadState.Reason.Failed)
    }

    /** Cancelling deletes what was downloaded; anything else keeps it, to continue from later. */
    private fun stop(partial: File): Outcome {
        if (!cancelled) return Outcome.Failed(DownloadState.Reason.Failed)
        partial.delete()
        return Outcome.Cancelled
    }

    /** Fetches the rest of the file into [partial], continuing from its end when the server allows it. */
    private fun fetch(network: Network, url: String, partial: File, model: TranslationModel) {
        val have = partial.length()
        val connection = (network.openConnection(URL(url)) as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MILLIS
            readTimeout = TIMEOUT_MILLIS
            if (have > 0) setRequestProperty("Range", "bytes=$have-")
        }
        this.connection = connection
        try {
            val append = when (connection.responseCode) {
                HttpURLConnection.HTTP_PARTIAL -> {
                    if (connection.getHeaderField("Content-Range")?.startsWith("bytes $have-") != true) {
                        partial.delete() // a range we did not ask for: start again
                        throw IOException("Unexpected range")
                    }
                    true
                }
                HttpURLConnection.HTTP_OK -> false
                else -> throw IOException("HTTP ${connection.responseCode}")
            }
            connection.inputStream.use { input ->
                FileOutputStream(partial, append).use { output ->
                    val buffer = ByteArray(BUFFER_BYTES)
                    var written = if (append) have else 0L
                    while (!cancelled && !stopped) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        if (written + read > model.sizeBytes) throw IOException("Longer than listed")
                        output.write(buffer, 0, read)
                        written += read
                        report(model, DownloadState.Step.Downloading, written)
                    }
                }
            }
        } finally {
            connection.disconnect()
            this.connection = null
        }
    }

    /** How much can be written in [folder], counting cached files Android would clear to make room. */
    private fun freeBytes(folder: File): Long {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return folder.freeSpace
        val storage = getSystemService(StorageManager::class.java) ?: return folder.freeSpace
        return storage.getAllocatableBytes(storage.getUuidForPath(folder))
    }

    /** The network to download over, while there is one that is not metered (Wi-Fi only). */
    private fun unmeteredNetwork(): Network? {
        val connectivity = getSystemService(ConnectivityManager::class.java) ?: return null
        val network = connectivity.activeNetwork ?: return null
        val capabilities = connectivity.getNetworkCapabilities(network) ?: return null
        val usable = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
        return network.takeIf { usable }
    }

    /** The file's SHA-256 in lower-case hex, or null if the download was stopped while reading it. */
    private fun sha256(file: File, model: TranslationModel): String? {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(BUFFER_BYTES)
        var done = 0L
        file.inputStream().use { input ->
            while (true) {
                if (cancelled || stopped) return null
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
                done += read
                report(model, DownloadState.Step.Checking, done)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /** The open check, run in the translator's own process so a large model never loads in this one. */
    private fun opens(file: File): Boolean {
        val answered = CountDownLatch(1)
        var opens = false
        main.post {
            checker.check(file) {
                opens = it
                answered.countDown()
            }
        }
        return answered.await(OPEN_CHECK_MINUTES, TimeUnit.MINUTES) && opens
    }

    private fun pause(millis: Long) {
        val until = SystemClock.elapsedRealtime() + millis
        while (!cancelled && !stopped && SystemClock.elapsedRealtime() < until) Thread.sleep(PAUSE_STEP_MILLIS)
    }

    /** Shows progress on the screen and in the notification, at most a few times a second unless [force]d. */
    private fun report(model: TranslationModel, step: DownloadState.Step, bytes: Long, force: Boolean = false) {
        val now = SystemClock.elapsedRealtime()
        if (!force && now - lastReport < REPORT_MILLIS) return
        lastReport = now
        val total = model.sizeBytes.coerceAtLeast(bytes)
        main.post {
            ModelDownloads.state = DownloadState.Running(model, step, bytes, total)
            notify(progressNotification(model, step, bytes, total))
        }
    }

    private fun finish(model: TranslationModel, outcome: Outcome) {
        job = null
        this.model = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        ModelDownloads.state = when (outcome) {
            Outcome.Done -> {
                notify(doneNotification(getString(R.string.download_done, model.name)))
                DownloadState.Idle
            }
            Outcome.Cancelled -> DownloadState.Idle
            is Outcome.Failed -> {
                notify(doneNotification(failureText(this, outcome.reason, model)))
                DownloadState.Failed(model, outcome.reason)
            }
        }
        stopSelf()
    }

    private fun goForeground(notification: Notification) {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC else 0
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
    }

    private fun progressNotification(model: TranslationModel, step: DownloadState.Step, bytes: Long, total: Long) =
        NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(
                if (model.source != null) getString(R.string.download_title, model.name)
                else getString(R.string.import_title),
            )
            .setContentText(progressText(this, step, bytes, total))
            .setProgress(
                PROGRESS_MAX,
                (PROGRESS_MAX * bytes / total.coerceAtLeast(1)).toInt(),
                step == DownloadState.Step.WaitingForWifi || total <= 0,
            )
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openApp())
            .addAction(0, getString(R.string.cancel), cancelIntent(this))
            .build()

    private fun doneNotification(text: String) =
        NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(getString(R.string.models_title))
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(openApp())
            .build()

    private fun openApp() = PendingIntent.getActivity(
        this,
        0,
        Intent(this, AppActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    /** Shows [notification] if the user allows notifications; the download runs either way. */
    private fun notify(notification: Notification) {
        val allowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (allowed) notifications.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val ACTION_DOWNLOAD = "com.akkhara.keyboard.action.DOWNLOAD_MODEL"
        private const val ACTION_IMPORT = "com.akkhara.keyboard.action.IMPORT_MODEL"
        private const val ACTION_CANCEL = "com.akkhara.keyboard.action.CANCEL_DOWNLOAD"
        private const val EXTRA_MODEL = "model"
        private const val EXTRA_SIZE = "size"
        private const val CHANNEL = "model_download"
        private const val NOTIFICATION_ID = 1

        private const val BUFFER_BYTES = 1 shl 20
        private const val TIMEOUT_MILLIS = 30_000
        private const val WIFI_POLL_MILLIS = 3_000L
        private const val RETRY_MILLIS = 5_000L
        private const val PAUSE_STEP_MILLIS = 250L
        private const val REPORT_MILLIS = 500L
        private const val PROGRESS_MAX = 1_000
        private const val OPEN_CHECK_MINUTES = 3L

        /** Gives up after this many tries in a row that download nothing. */
        private const val MAX_ATTEMPTS_WITHOUT_PROGRESS = 12

        /** Free space left over after the download, so the phone is never filled to the last byte. */
        private const val SPARE_BYTES = 500L * 1024 * 1024

        /** Starts downloading [model] in the background. */
        fun download(context: Context, model: TranslationModel) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, ModelDownloadService::class.java)
                    .setAction(ACTION_DOWNLOAD)
                    .putExtra(EXTRA_MODEL, model.id),
            )
        }

        /**
         * Starts copying the model file at [uri], [size] bytes long or -1 if
         * unknown, in the background to become the imported model.
         */
        fun import(context: Context, uri: Uri, size: Long) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, ModelDownloadService::class.java)
                    .setAction(ACTION_IMPORT)
                    .setData(uri)
                    .putExtra(EXTRA_SIZE, size)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
            )
        }

        /** Stops the download or import and deletes what it fetched. */
        fun cancel(context: Context) {
            context.startService(Intent(context, ModelDownloadService::class.java).setAction(ACTION_CANCEL))
        }

        private fun cancelIntent(context: Context) = PendingIntent.getService(
            context,
            1,
            Intent(context, ModelDownloadService::class.java).setAction(ACTION_CANCEL),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

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
    }
}
