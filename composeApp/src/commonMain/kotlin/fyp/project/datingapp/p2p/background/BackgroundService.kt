package fyp.project.datingapp.p2p.background

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

    /** Whether the "Stay online" service is currently enabled. */
    fun isStayOnline(): Boolean
}

/** Default no-op (iOS / platforms without a background scheduler). */
class NoopBackgroundService : BackgroundService {
    override fun scheduleBackgroundSync() {}
    override fun cancelBackgroundSync() {}
    override fun setStayOnline(enabled: Boolean) {}
    override fun isStayOnline(): Boolean = false
}
