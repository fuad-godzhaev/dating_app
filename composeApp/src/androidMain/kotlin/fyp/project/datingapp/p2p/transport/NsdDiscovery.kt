package fyp.project.datingapp.p2p.transport

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log

/**
 * LAN peer discovery via Android NsdManager (system-mediated mDNS/DNS-SD over
 * Binder) — the replacement for go-libp2p's built-in mDNS, which can't run on
 * Android (SELinux blocks the netlink interface enumeration it needs,
 * b/155595000). [register] advertises this peer as a "_datingapp._tcp" service
 * carrying its libp2p PeerId in a TXT record; [discover] finds others and emits
 * a dialable multiaddr per peer (feed it to [Libp2pTransport.connect]).
 *
 * TODO(iOS): the iOS equivalent is Bonjour — Network framework `NWListener` +
 * `NWBrowser` (or `NSNetService`). Implement when iOS leaves the lazy state.
 * TODO: `resolveService` + `NsdServiceInfo.host` are deprecated on API 34+;
 * migrate to `registerServiceInfoCallback`. Serialize resolves (NsdManager
 * historically allows one in-flight resolve at a time).
 */
class NsdDiscovery(
    private val context: Context,
    private val selfPeerId: String,
    private val onPeer: (multiaddr: String, peerId: String) -> Unit,
) {
    private val nsd: NsdManager =
        context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private var regListener: NsdManager.RegistrationListener? = null
    private var discListener: NsdManager.DiscoveryListener? = null

    /** Advertise this peer's libp2p host (listening on [listenPort]) on the LAN. */
    fun register(listenPort: Int) {
        val info = NsdServiceInfo().apply {
            serviceName = SERVICE_PREFIX + selfPeerId.takeLast(8)
            serviceType = SERVICE_TYPE
            port = listenPort
            setAttribute("peerId", selfPeerId)
        }
        val l = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(s: NsdServiceInfo) { Log.i(TAG, "registered ${s.serviceName}:$listenPort") }
            override fun onRegistrationFailed(s: NsdServiceInfo, e: Int) { Log.w(TAG, "register failed: $e") }
            override fun onServiceUnregistered(s: NsdServiceInfo) {}
            override fun onUnregistrationFailed(s: NsdServiceInfo, e: Int) {}
        }
        regListener = l
        nsd.registerService(info, NsdManager.PROTOCOL_DNS_SD, l)
    }

    /** Discover other peers; [onPeer] fires with a dialable multiaddr + PeerId. */
    fun discover() {
        val l = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(t: String) { Log.i(TAG, "discovery started") }
            override fun onServiceFound(s: NsdServiceInfo) {
                if (s.serviceType.trimEnd('.') == SERVICE_TYPE.trimEnd('.')) resolve(s)
            }
            override fun onServiceLost(s: NsdServiceInfo) {}
            override fun onDiscoveryStopped(t: String) {}
            override fun onStartDiscoveryFailed(t: String, e: Int) { Log.w(TAG, "discovery start failed: $e") }
            override fun onStopDiscoveryFailed(t: String, e: Int) {}
        }
        discListener = l
        nsd.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, l)
    }

    fun stop() {
        regListener?.let { runCatching { nsd.unregisterService(it) } }
        discListener?.let { runCatching { nsd.stopServiceDiscovery(it) } }
        regListener = null
        discListener = null
    }

    @Suppress("DEPRECATION") // resolveService/host: migrate to registerServiceInfoCallback (API 34+).
    private fun resolve(found: NsdServiceInfo) {
        nsd.resolveService(found, object : NsdManager.ResolveListener {
            override fun onResolveFailed(si: NsdServiceInfo, e: Int) { Log.w(TAG, "resolve failed: $e") }
            override fun onServiceResolved(si: NsdServiceInfo) {
                val peerId = si.attributes["peerId"]?.toString(Charsets.UTF_8) ?: return
                if (peerId == selfPeerId) return                 // skip our own service
                val host = si.host?.hostAddress ?: return
                if (host.contains(':')) return                   // IPv4 only for now
                onPeer("/ip4/$host/tcp/${si.port}/p2p/$peerId", peerId)
            }
        })
    }

    private companion object {
        const val TAG = "NsdDiscovery"
        const val SERVICE_TYPE = "_datingapp._tcp."
        const val SERVICE_PREFIX = "wirexia-"
    }
}
