package com.akkhara.keyboard.translation

import android.annotation.SuppressLint
import android.Manifest
import android.app.PendingIntent
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

/** Tells the user a downloaded or imported model is ready, or why it is not. */
internal object ModelNotifications {
    private const val CHANNEL = "model_download"
    private const val NOTIFICATION_ID = 2

    fun ready(context: Context, model: TranslationModel) =
        show(context, context.getString(R.string.download_done, model.name))

    fun failed(context: Context, model: TranslationModel, reason: DownloadState.Reason) =
        show(context, failureText(context, reason, model))

    /** Whether the user allows the app's notifications. Downloads and imports run either way. */
    fun allowed(context: Context): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /** The app's channel for model notifications, made the first time it is needed. */
    fun channel(context: Context): String {
        NotificationManagerCompat.from(context).createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL, NotificationManagerCompat.IMPORTANCE_LOW)
                .setName(context.getString(R.string.download_channel))
                .build(),
        )
        return CHANNEL
    }

    /** Opens the app on its screen. */
    fun openApp(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, AppActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    /** Shows [text] if the user allows notifications. */
    @SuppressLint("MissingPermission") // ModelNotifications.allowed checks it.
    private fun show(context: Context, text: String) {
        if (!allowed(context)) return
        NotificationManagerCompat.from(context).notify(
            NOTIFICATION_ID,
            NotificationCompat.Builder(context, channel(context))
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle(context.getString(R.string.models_title))
                .setContentText(text)
                .setAutoCancel(true)
                .setContentIntent(openApp(context))
                .build(),
        )
    }
}
