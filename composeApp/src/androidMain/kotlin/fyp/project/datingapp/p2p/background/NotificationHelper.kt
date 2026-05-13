package fyp.project.datingapp.p2p.background

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

/**
 * Local notifications for background message delivery + the "Stay online" foreground
 * service (Phase B). Uses platform APIs only (no extra dependency). On Android 13+ posting
 * is silently dropped without POST_NOTIFICATIONS, which is acceptable (best-effort).
 */
class NotificationHelper(private val context: Context) {

    private val manager: NotificationManager =
        context.getSystemService(NotificationManager::class.java)

    fun notifyNewMessages(count: Int) {
        ensureChannel(MESSAGES_CHANNEL, "Messages")
        val text = if (count == 1) "You have 1 new message" else "You have $count new messages"
        manager.notify(MESSAGES_NOTIF_ID, build(MESSAGES_CHANNEL, "Aura", text, ongoing = false))
    }

    /** Persistent notification required for the dataSync foreground service. */
    fun buildStayOnline(): Notification {
        ensureChannel(STAY_ONLINE_CHANNEL, "Staying online")
        return build(STAY_ONLINE_CHANNEL, "Aura", "Staying online to receive messages", ongoing = true)
    }

    private fun build(channel: String, title: String, text: String, ongoing: Boolean): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, channel)
        } else {
            @Suppress("DEPRECATION") Notification.Builder(context)
        }
        return builder
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(!ongoing)
            .setOngoing(ongoing)
            .build()
    }

    private fun ensureChannel(id: String, name: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(id, name, NotificationManager.IMPORTANCE_DEFAULT),
            )
        }
    }

    companion object {
        const val MESSAGES_CHANNEL = "aura.messages"
        const val STAY_ONLINE_CHANNEL = "aura.stay_online"
        const val MESSAGES_NOTIF_ID = 2001
        const val STAY_ONLINE_NOTIF_ID = 2002
    }
}
