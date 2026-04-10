package fyp.project.datingapp.p2p.transport

import android.content.Context

/**
 * Holds the application Context for the Android transport. The `Libp2pTransport`
 * expect constructor takes only a `Libp2pConfig` (common), so the actual can't
 * receive a Context directly; it reads it here to acquire a `MulticastLock` for
 * mDNS. Set once at app start (MainActivity).
 */
object AndroidTransportEnv {
    @Volatile
    var appContext: Context? = null
}
