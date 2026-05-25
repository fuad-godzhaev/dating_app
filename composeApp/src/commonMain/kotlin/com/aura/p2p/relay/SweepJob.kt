package com.aura.p2p.relay

import com.aura.database.appView.dao.DiscoveryDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Periodically drops relay-cached rows past their TTL.
 *
 * Phase A runs this inside the app's existing coroutine scope — WorkManager
 * / BGAppRefresh integration is deferred to Phase G alongside the rest of
 * the background scheduling work. The cadence only needs to be coarse:
 * rows carry their own `expiresAt`, so a miss by a few minutes merely means
 * a stale row is briefly served (and filtered in `RelayPolicy.get` anyway).
 */
class SweepJob(
    private val discoveryDao: DiscoveryDao,
    private val clock: EpochClock = SystemClock,
    private val intervalMs: Long = DEFAULT_INTERVAL_MS,
) {
    /** One-shot sweep. Exposed so tests can drive ticks without a real loop. */
    suspend fun runOnce(): Int = discoveryDao.sweepExpired(clock.nowMs())

    /**
     * Launch a long-running sweep loop on [scope]. The returned [Job] is
     * owned by the caller — cancel it to stop the sweep.
     */
    fun launchIn(scope: CoroutineScope): Job = scope.launch {
        while (isActive) {
            runOnce()
            delay(intervalMs)
        }
    }

    companion object {
        /** 15 minutes — cheap query, miss-window is the `get` read check anyway. */
        const val DEFAULT_INTERVAL_MS: Long = 15L * 60 * 1000
    }
}
