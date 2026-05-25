package com.aura.p2p.background

/**
 * Platform control surface for background operation (Phase B). Android schedules
 * cooperative serve-window workers (WorkManager) and an opt-in "Stay online" foreground
 * service; iOS is a no-op for now. Kept as a plain interface (not expect/actual) so it
 * wires through Koin per-platform like the other transport seams.
 */
interface BackgroundService {
    /** Schedule periodic cooperative serve windows (idempotent). */
    fun scheduleBackgroundSync()

    /** Cancel scheduled serve windows (e.g. on sign-out). */
    fun cancelBackgroundSync()

    /** Enable/disable the always-on foreground "Stay online" service. */
    fun setStayOnline(enabled: Boolean)

    /** Whether the "Stay online" service is currently enabled. Defaults to ENABLED (opt-out): the
     *  service runs the quiet, slotted, reachability-gated role unless the user turns it off. */
    fun isStayOnline(): Boolean

    /** Start the "Stay online" service IF it is enabled. Call from a foreground context (Android
     *  forbids starting a foreground service from the background). The service self-gates on
     *  reachability + slot, so calling this outside the slot is harmless. */
    fun startStayOnlineIfEnabled()
}

/** Default no-op (iOS / platforms without a background scheduler). */
class NoopBackgroundService : BackgroundService {
    override fun scheduleBackgroundSync() {}
    override fun cancelBackgroundSync() {}
    override fun setStayOnline(enabled: Boolean) {}
    override fun isStayOnline(): Boolean = false
    override fun startStayOnlineIfEnabled() {}
}
