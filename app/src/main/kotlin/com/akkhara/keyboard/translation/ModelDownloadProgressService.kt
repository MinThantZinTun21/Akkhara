package com.akkhara.keyboard.translation

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import kotlin.concurrent.thread

/**
 * Shows a model download's progress in the app's own notification, with
 * Cancel, while the app's process is alive. Android's DownloadManager does the
 * downloading (docs/adr/0006) and shows its own notification too, but at its
 * lowest importance, folded away at the bottom of the shade.
 *
 * When the process is killed, as when HiOS clears the app from recent apps,
 * this notification goes. The download keeps going in DownloadManager's.
 */
class ModelDownloadProgressService : Service() {

    @Volatile private var running = false
    private var poller: Thread? = null

    override fun onBind(intent: Intent): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val downloader = ModelDownloader(this)
        // Android needs the notification at once; the poll fills in the progress.
        val model = downloader.model()
        goForeground(
            ModelNotifications.progress(this, model?.name.orEmpty(), DownloadState.Step.Downloading, 0, -1, cancelIntent(this)),
        )
        if (intent?.action == ACTION_CANCEL) thread { downloader.cancel() }
        if (poller == null) {
            running = true
            poller = thread(name = "model-download-progress") { poll(downloader) }
        }
        return START_NOT_STICKY
    }

    /** Android's daily limit on data-sync work ran out: the notification goes, the download goes on. */
    override fun onTimeout(startId: Int, fgsType: Int) {
        running = false
    }

    override fun onDestroy() {
        running = false
        super.onDestroy()
    }

    /** Runs on the poll thread until the download is no longer running. */
    private fun poll(downloader: ModelDownloader) {
        while (running) {
            val state = downloader.poll()
            if (state !is DownloadState.Running) break
            notify(ModelNotifications.progress(this, state.model.name, state.step, state.bytes, state.total, cancelIntent(this)))
            Thread.sleep(POLL_MILLIS)
        }
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun goForeground(notification: Notification) {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC else 0
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
    }

    /** Shows the progress if the user allows notifications; the download runs either way. */
    @SuppressLint("MissingPermission") // ModelNotifications.allowed checks it.
    private fun notify(notification: Notification) {
        if (running && ModelNotifications.allowed(this)) NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val ACTION_CANCEL = "com.akkhara.keyboard.action.CANCEL_DOWNLOAD"
        private const val NOTIFICATION_ID = 3
        private const val POLL_MILLIS = 1_000L

        /**
         * Shows the download in progress, if there is one. Android lets the
         * app start it only while the app is on screen; otherwise it is left
         * to DownloadManager's notification.
         */
        fun show(context: Context) {
            try {
                ContextCompat.startForegroundService(context, Intent(context, ModelDownloadProgressService::class.java))
            } catch (_: IllegalStateException) {
                // The app is in the background now (ForegroundServiceStartNotAllowedException).
            }
        }

        private fun cancelIntent(context: Context) = PendingIntent.getService(
            context,
            2,
            Intent(context, ModelDownloadProgressService::class.java).setAction(ACTION_CANCEL),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}
