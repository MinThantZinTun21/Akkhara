package com.akkhara.keyboard

import android.app.DownloadManager
import com.akkhara.keyboard.translation.DownloadProgress
import com.akkhara.keyboard.translation.DownloadState
import com.akkhara.keyboard.translation.downloadProgress
import org.junit.Assert.assertEquals
import org.junit.Test

class SystemDownloadTest {
    private val size = 1_000L

    @Test
    fun `a download that is queued or running is downloading, with its bytes`() {
        assertEquals(
            DownloadProgress.Running(DownloadState.Step.Downloading, 0, size),
            downloadProgress(DownloadManager.STATUS_PENDING, reason = 0, bytes = 0, total = -1, listedSize = size),
        )
        assertEquals(
            DownloadProgress.Running(DownloadState.Step.Downloading, 400, size),
            downloadProgress(DownloadManager.STATUS_RUNNING, reason = 0, bytes = 400, total = size, listedSize = size),
        )
    }

    @Test
    fun `a download paused for a network without a meter is waiting for Wi-Fi`() {
        for (reason in listOf(DownloadManager.PAUSED_QUEUED_FOR_WIFI, DownloadManager.PAUSED_WAITING_FOR_NETWORK)) {
            assertEquals(
                DownloadProgress.Running(DownloadState.Step.WaitingForWifi, 400, size),
                downloadProgress(DownloadManager.STATUS_PAUSED, reason, bytes = 400, total = size, listedSize = size),
            )
        }
    }

    @Test
    fun `a download paused to retry is still downloading`() {
        assertEquals(
            DownloadProgress.Running(DownloadState.Step.Downloading, 400, size),
            downloadProgress(DownloadManager.STATUS_PAUSED, DownloadManager.PAUSED_WAITING_TO_RETRY, 400, size, size),
        )
    }

    @Test
    fun `a successful download is complete, to be checked`() {
        assertEquals(
            DownloadProgress.Complete,
            downloadProgress(DownloadManager.STATUS_SUCCESSFUL, reason = 0, bytes = size, total = size, listedSize = size),
        )
    }

    @Test
    fun `a failed download says whether space ran out`() {
        assertEquals(
            DownloadProgress.Failed(DownloadState.Reason.NoSpace),
            downloadProgress(DownloadManager.STATUS_FAILED, DownloadManager.ERROR_INSUFFICIENT_SPACE, 400, size, size),
        )
        assertEquals(
            DownloadProgress.Failed(DownloadState.Reason.Failed),
            downloadProgress(DownloadManager.STATUS_FAILED, DownloadManager.ERROR_HTTP_DATA_ERROR, 400, size, size),
        )
    }
}
