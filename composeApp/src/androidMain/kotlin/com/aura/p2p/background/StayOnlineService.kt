package com.aura.p2p.background

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.aura.domain.auth.AuthRepository
import com.aura.p2p.feed.PeerProfileFeed
import com.aura.p2p.transport.ReachabilityProbe
import com.aura.p2p.transport.permitsServing
import java.util.Calendar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Opt-in "Stay online" dataSync foreground service. Keeps the libp2p host alive so the device
 * receives messages in real time and serves as a mailbox holder / relay coordinator for peers.
 *
 * Phase-2 efficiency design (sim-results) - the service now serves a QUIET, SLOTTED, REACHABILITY-
 * GATED role instead of an always-loud always-on one:
 *  - Idea C (reachability gate): if AutoNAT says we are UNREACHABLE (CGNAT), serving is pointless -
 *    we cannot be a holder/relay there - so we do NOT burn battery or the Android-15 6 h FGS budget;
 *    we stop. (The probe is fail-open until AutoNAT is surfaced from the .aar - see INTEGRATION.md.)
 *  - Idea D (deterministic rank slotting): the node serves only during its daily [FgsSlot] so the
 *    cell's reachable nodes tile the 24 h rather than all lighting up at the evening peak. Runtime is
 *    bounded to the slot end (<= 6 h), which aligns with the A15 dataSync cap.
 *  - Quiet / relay-only: the heavy GossipSub-mesh participation and blob serving are dropped in
 *    favour of on-demand mailbox/relay coordination - this is realised in the Go host (quiet DHT
 *    client + relay service); see the Go changes + INTEGRATION.md for the .aar wiring.
 *
 * NOTE (integration): re-(starting) the service at the next slot boundary needs a slot-aligned
 * alarm; that scheduling is documented in INTEGRATION.md (device-tested separately). This class
 * implements the gating + runtime bounding, which are the substantive, testable decisions.
 */
class StayOnlineService : Service(), KoinComponent {

    private val feed: PeerProfileFeed by inject()
    private val notifications: NotificationHelper by inject()
    private val auth: AuthRepository by inject()
    private val reachability: ReachabilityProbe by inject()
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
        scope.launch { runCatching { gateAndServe() } }
        return START_STICKY
    }

    /** Serve only if reachable (Idea C) and inside our slot (Idea D); bound runtime to the slot. */
    private suspend fun gateAndServe() {
        val did = runCatching { auth.getDid() }.getOrNull()
        if (did == null) { stopSelf(); return }

        // Idea C: don't run the FGS if AutoNAT proves we can't be reached (fail-open on UNKNOWN).
        if (!reachability.current().permitsServing()) { stopSelf(); return }

        // Idea D: only serve during this node's deterministic daily slot.
        val rank = FgsSlot.rankFromId(did, DEFAULT_ROSTER_SIZE)
        val slot = FgsSlot.slot(rank, DEFAULT_ROSTER_SIZE)
        val nowMin = minuteOfDay()
        if (!FgsSlot.isActiveAt(nowMin, slot[0], slot[1])) { stopSelf(); return }

        runCatching { feed.ensureStarted() }

        // Bound runtime to the remaining slot (<= 6 h), then stop - aligns with the A15 FGS cap.
        val remaining = remainingSlotMinutes(nowMin, slot[0], slot[1])
        if (remaining > 0) delay(remaining.toLong() * 60_000L)
        stopSelf()
    }

    override fun onDestroy() {
        // Tear the host down on a scope that outlives this service's (about-to-cancel) one.
        CoroutineScope(Dispatchers.Default).launch { runCatching { feed.stop() } }
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun minuteOfDay(): Int {
        val c = Calendar.getInstance()
        return c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE)
    }

    /** Minutes from [now] until the slot `[start, start+len)` ends, assuming we are inside it. */
    private fun remainingSlotMinutes(now: Int, start: Int, len: Int): Int {
        val day = FgsSlot.DAY_MIN
        val end = start + len // may exceed a day (slot wraps past midnight)
        val nowAbs = if (now >= start) now else now + day // unwrap the wrapped tail
        return (end - nowAbs).coerceIn(0, FgsSlot.MAX_SLOT_MIN)
    }

    companion object {
        /** Default cell roster size when the actual cell FGS set is not yet discovered: 4 -> four
         *  6 h slots covering the day (rank from the DID hash). The precise per-cell rank is the
         *  INTEGRATION follow-up (roster discovery over the cell's presence records). */
        const val DEFAULT_ROSTER_SIZE = 4
    }
}
