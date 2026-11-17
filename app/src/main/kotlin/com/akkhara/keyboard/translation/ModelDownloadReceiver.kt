package com.akkhara.keyboard.translation

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlin.concurrent.thread

/**
 * Android says a download is over, whether or not the app is open: a complete
 * model is checked at once, and a failed one is reported. After the app is
 * force stopped Android says nothing, and the app looks when it next opens.
 */
class ModelDownloadReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return
        val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
        // DownloadManager and a failed multi-GB file are kept off the main thread.
        val pending = goAsync()
        thread {
            try {
                val downloader = ModelDownloader(context)
                if (id == downloader.downloadId()) downloader.actIfOver()
            } finally {
                pending.finish()
            }
        }
    }
}
