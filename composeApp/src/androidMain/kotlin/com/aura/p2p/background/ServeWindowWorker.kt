package com.aura.p2p.background

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aura.database.AppDatabase
import com.aura.domain.auth.AuthRepository
import com.aura.p2p.feed.PeerProfileFeed
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Cooperative serve window (Phase B). On each periodic wake the device brings the libp2p
 * host up for a few minutes so it can re-advertise its provider records, answer profile /
 * blob / message / mailbox requests for peers, receive online messages, and pull its own
 * parked mail - then tears down. Aligning these windows across peers (scheduler init delay
 * + Doze batching) raises the chance two suspended peers overlap (DTN-style duty cycling).
 *
 * Dependencies come from Koin's global context (started in [com.aura.AuraApp]),
 * so this runs even with no Activity. If WE started the host (app not already foreground),
 * we stop it at the end; if the foreground was already serving, we leave it running.
 */
class ServeWindowWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params), KoinComponent {

    private val feed: PeerProfileFeed by inject()
    private val auth: AuthRepository by inject()
    private val db: AppDatabase by inject()
    private val notifications: NotificationHelper by inject()

    override suspend fun doWork(): Result {
        // Signed out / fresh install: no identity to serve or receive for. Do NOT reschedule -
        // scheduleBackgroundSync re-arms the chain on next sign-in.
        if (runCatching { auth.getDid() }.getOrNull() == null) return Result.success()

        val messageDao = db.conversationDao()
        val wasRunning = feed.isRunning
        val before = runCatching { messageDao.totalUnreadCount().first() }.getOrDefault(0)
        return try {
            feed.ensureStarted() // serves peers + pulls own mailbox (best-effort, internal)
            delay(SERVE_WINDOW_MS)
            val after = runCatching { messageDao.totalUnreadCount().first() }.getOrDefault(before)
            if (after > before) notifications.notifyNewMessages(after - before)
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        } finally {
            if (!wasRunning) runCatching { feed.stop() }
            // Self-reschedule the next window at the ADAPTIVE cadence (peak 15 / off-peak 45 min),
            // unless this worker was cancelled (cancelBackgroundSync) - then let the chain stop.
            if (!isStopped) runCatching { AndroidBackgroundService.scheduleNextAdaptive(applicationContext) }
        }
    }

    companion object {
        /** 5-minute window, within WorkManager's ~10-minute worker execution budget. */
        const val SERVE_WINDOW_MS: Long = 5L * 60 * 1000
        const val UNIQUE_WORK_NAME = "aura.serve_window"
    }
}
