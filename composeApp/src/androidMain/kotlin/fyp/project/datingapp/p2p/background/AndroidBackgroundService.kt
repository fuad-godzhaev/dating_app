package fyp.project.datingapp.p2p.background

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Android background control (Phase B): periodic WorkManager serve windows + the opt-in
 * "Stay online" foreground service. The periodic work's initial delay aligns the first run
 * to the next wall-clock quarter-hour so peers' windows tend to overlap.
 */
class AndroidBackgroundService(private val context: Context) : BackgroundService {

    private val prefs = context.getSharedPreferences("aura.background", Context.MODE_PRIVATE)

    override fun scheduleBackgroundSync() {
        val request = PeriodicWorkRequestBuilder<ServeWindowWorker>(15, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setInitialDelay(minutesToNextQuarter(), TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            ServeWindowWorker.UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    override fun cancelBackgroundSync() {
        WorkManager.getInstance(context).cancelUniqueWork(ServeWindowWorker.UNIQUE_WORK_NAME)
    }

    override fun setStayOnline(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_STAY_ONLINE, enabled).apply()
        val intent = Intent(context, StayOnlineService::class.java)
        if (enabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
            else context.startService(intent)
        } else {
            context.stopService(intent)
        }
    }

    override fun isStayOnline(): Boolean = prefs.getBoolean(KEY_STAY_ONLINE, false)

    private fun minutesToNextQuarter(): Long {
        val minute = Calendar.getInstance().get(Calendar.MINUTE)
        val toNext = (15 - (minute % 15)) % 15
        return if (toNext == 0) 15L else toNext.toLong()
    }

    companion object {
        private const val KEY_STAY_ONLINE = "stay_online"
    }
}
