package fyp.project.datingapp.p2p.transport

import android.content.Context
import android.net.ConnectivityManager
import java.net.Inet4Address

/**
 * Device IPv4 via ConnectivityManager (Binder IPC). go-libp2p can't self-detect
 * its address on Android — SELinux blocks the netlink interface enumeration it
 * uses (b/155595000), so `host.Addrs()` only yields 127.0.0.1. This reads the
 * address the system already knows, for explicit listen/announce addrs and the
 * PresenceRecord multiaddrs (ADR-0002/B4). TODO(iOS): use NWPath / getifaddrs.
 */
object DeviceNet {
    @Suppress("DEPRECATION") // allNetworks: fallback only; fine for our use.
    fun localIpv4(context: Context): String? {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return null
        val networks = buildList {
            cm.activeNetwork?.let { add(it) }
            addAll(cm.allNetworks)
        }
        for (network in networks) {
            val lp = cm.getLinkProperties(network) ?: continue
            val ip = lp.linkAddresses
                .map { it.address }
                .filterIsInstance<Inet4Address>()
                .firstOrNull { !it.isLoopbackAddress && !it.isLinkLocalAddress }
            if (ip != null) return ip.hostAddress
        }
        return null
    }
}
