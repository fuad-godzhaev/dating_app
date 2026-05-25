package com.aura.p2p.feed

import android.content.Context
import android.util.Log
import com.aura.p2p.discovery.PresenceAnnouncer
import com.aura.p2p.transport.DeviceNet
import com.aura.p2p.transport.Transport
import com.aura.p2p.transport.NsdDiscovery
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Android LAN bootstrap over NsdManager (`_datingapp._tcp`): advertises this host
 * and dials discovered peers, forming the connections the DHT/GossipSub mesh needs.
 * Must be started after [Libp2pTransport.start] so the listen port + PeerId exist.
 */
class AndroidLanBootstrap(
    private val context: Context,
    private val transport: Transport,
) : LanBootstrap {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var nsd: NsdDiscovery? = null

    override fun start() {
        if (nsd != null) return
        val port = PresenceAnnouncer.parseTcpPort(transport.listenAddrs) ?: return
        val discovery = NsdDiscovery(context, transport.peerId) { maddr, _ ->
            scope.launch { runCatching { transport.connect(maddr) } }
        }
        discovery.register(port)
        discovery.discover()
        nsd = discovery
        Log.i(TAG, "NSD LAN bootstrap started port=$port ip=${DeviceNet.localIpv4(context)}")
    }

    override fun stop() {
        nsd?.stop()
        nsd = null
    }

    private companion object {
        const val TAG = "LanBootstrap"
    }
}
