package fyp.project.datingapp.p2p.background

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import fyp.project.datingapp.p2p.feed.PeerProfileFeed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Opt-in "Stay online" dataSync foreground service (Phase B). Keeps the libp2p host alive
 * while backgrounded so the device receives messages in real time and serves as a
 * cacheHolder/mailbox for peers. Android 15+ caps dataSync FGS runtime (~6h/24h); the
 * system stops it via onTimeout, which lands in onDestroy and tears the host down.
 */
class StayOnlineService : Service(), KoinComponent {

    private val feed: PeerProfileFeed by inject()
    private val notifications: NotificationHelper by inject()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = notifications.buildStayOnline()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NotificationHelper.STAY_ONLINE_NOTIF_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            startForeground(NotificationHelper.STAY_ONLINE_NOTIF_ID, notification)
        }
        scope.launch { runCatching { feed.ensureStarted() } }
        return START_STICKY
    }

    override fun onDestroy() {
        // Tear the host down on a scope that outlives this service's (about-to-cancel) one.
        CoroutineScope(Dispatchers.Default).launch { runCatching { feed.stop() } }
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
