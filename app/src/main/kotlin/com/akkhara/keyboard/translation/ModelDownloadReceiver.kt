package com.akkhara.keyboard.translation

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

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
        if (id != downloader.downloadId()) return
        when (val progress = downloader.progress()) {
            DownloadProgress.Complete -> downloader.checkIfComplete()
            is DownloadProgress.Failed -> downloader.failed(id, progress.reason)
            else -> Unit
        }
    }
}
