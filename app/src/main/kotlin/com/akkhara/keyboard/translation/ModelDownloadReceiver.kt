package com.akkhara.keyboard.translation

import android.Manifest
import android.app.DownloadManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.akkhara.keyboard.AppActivity
import com.akkhara.keyboard.R

/**
 * Android says a download is over, whether or not the app is open: a complete
 * model is checked at once, and a failed one is reported. After the app is
 * force stopped Android says nothing, and the app looks when it next opens.
 */
class ModelDownloadReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return
        val downloader = ModelDownloader(context)
        val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
        val model = downloader.model() ?: return
        if (id != downloader.downloadId()) return
        when (val progress = downloader.progress()) {
            DownloadProgress.Complete -> downloader.checkIfComplete()
            is DownloadProgress.Failed -> {
                downloader.finish(model, progress.reason)
                ModelNotifications.failed(context, model, progress.reason)
            }
            else -> Unit
        }
    }
}

/** Tells the user a downloaded model is ready, or why it is not. */
internal object ModelNotifications {
    private const val CHANNEL = "model_download"
    private const val NOTIFICATION_ID = 2

    fun ready(context: Context, model: TranslationModel) =
        show(context, context.getString(R.string.download_done, model.name))

    fun failed(context: Context, model: TranslationModel, reason: DownloadState.Reason) =
        show(context, failureText(context, reason, model))

    /** Shows [text] if the user allows notifications. */
    private fun show(context: Context, text: String) {
        val allowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (!allowed) return
        val notifications = NotificationManagerCompat.from(context)
        notifications.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL, NotificationManagerCompat.IMPORTANCE_LOW)
                .setName(context.getString(R.string.download_channel))
                .build(),
        )
        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, AppActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        notifications.notify(
            NOTIFICATION_ID,
            NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle(context.getString(R.string.models_title))
                .setContentText(text)
                .setAutoCancel(true)
                .setContentIntent(openApp)
                .build(),
        )
    }
}
