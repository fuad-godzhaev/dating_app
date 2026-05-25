package com.aura.p2p.background

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Android background control (Phase B): adaptive WorkManager serve windows + the opt-in "Stay
 * online" foreground service.
 *
 * ADAPTIVE CADENCE (sim-results phase2): instead of a fixed 15-min PeriodicWork (which cannot vary
 * by time of day), the serve window is a self-rescheduling OneTimeWork - each run enqueues the next
 * with a delay of [ServeCadence] (15 min during the evening peak, 45 min off-peak). This gives
 * peak-level delivery latency at off-peak battery (~9%/day vs ~13%/day). NO Wi-Fi gating: the
 * constraint stays [NetworkType.CONNECTED] (serve on cellular too). The first run still aligns to
 * the next wall-clock quarter-hour so peers' windows tend to overlap.
 */
class AndroidBackgroundService(private val context: Context) : BackgroundService {

    private val prefs = context.getSharedPreferences("aura.background", Context.MODE_PRIVATE)

    override fun scheduleBackgroundSync() {
        enqueueServeWindow(context, minutesToNextQuarter(), ExistingWorkPolicy.REPLACE)
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

    // Opt-out: enabled by default. A brand-new user runs the quiet/slotted/reachability-gated role;
    // a user who explicitly turned it off has the pref stored false.
    override fun isStayOnline(): Boolean = prefs.getBoolean(KEY_STAY_ONLINE, true)

    override fun startStayOnlineIfEnabled() {
        if (!isStayOnline()) return
        val intent = Intent(context, StayOnlineService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
        else context.startService(intent)
    }

    private fun minutesToNextQuarter(): Long {
        val minute = Calendar.getInstance().get(Calendar.MINUTE)
        val toNext = (15 - (minute % 15)) % 15
        return if (toNext == 0) 15L else toNext.toLong()
    }

    companion object {
        private const val KEY_STAY_ONLINE = "stay_online"

        /** Enqueue a single serve window after [delayMin]; the worker reschedules the next itself. */
        fun enqueueServeWindow(context: Context, delayMin: Long, policy: ExistingWorkPolicy) {
            val request = OneTimeWorkRequestBuilder<ServeWindowWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setInitialDelay(delayMin, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                ServeWindowWorker.UNIQUE_WORK_NAME, policy, request,
            )
        }

        /** Schedule the next serve window at the adaptive cadence for the current local hour. */
        fun scheduleNextAdaptive(context: Context) {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            enqueueServeWindow(context, ServeCadence.delayMinutesForHour(hour), ExistingWorkPolicy.REPLACE)
        }
    }
}
