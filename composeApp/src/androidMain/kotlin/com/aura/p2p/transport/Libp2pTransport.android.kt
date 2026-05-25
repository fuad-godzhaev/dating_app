package com.aura.p2p.transport

import android.content.Context
import android.net.wifi.WifiManager
import com.aura.golibp2p.Golibp2p
import com.aura.golibp2p.GossipCallback
import com.aura.golibp2p.ProviderCallback
import com.aura.golibp2p.StreamCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.aura.golibp2p.Host as GoHost
import com.aura.golibp2p.Stream as GoStream

/**
 * Android actual (ADR-0004) — thin JNI shim over the gomobile `golibp2p.aar`.
 * Go is pure transport; all record semantics stay in Kotlin. Blocking native
 * calls run on [Dispatchers.IO]; Go callbacks are bridged to [Flow] / coroutines.
 */
actual class Libp2pTransport actual constructor(private val config: Libp2pConfig) : Transport {
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + Dispatchers.IO)

    @Volatile private var host: GoHost? = null
    private var multicastLock: WifiManager.MulticastLock? = null

    private fun requireHost(): GoHost = host ?: error("Libp2pTransport not started")

    override suspend fun start() {
        withContext(Dispatchers.IO) {
            if (host != null) return@withContext
            // Only needed if go-libp2p's mDNS is on (Android drops inbound multicast
            // without a held lock). LAN discovery normally uses NsdManager instead,
            // so config.enableMdns is false on Android and no lock is taken.
            if (config.enableMdns) acquireMulticastLock()
            // Phase-2 efficiency toggles must be set BEFORE newHost (they are libp2p.New options).
            Golibp2p.setRelayServiceEnabled(config.relayServiceEnabled)
            Golibp2p.setAutoRelayStaticRelays(config.autoRelayStaticRelays)
            Golibp2p.setGossipHardeningEnabled(config.gossipHardeningEnabled)
            val h = Golibp2p.newHost(
                config.identityKey,
                config.listenAddrs.joinToString(","),
                config.bootstrapPeers.joinToString(","),
                config.protocolPrefix,
                config.enableMdns,
            )
            h.start()
            host = h
        }
    }

    override suspend fun stop() {
        withContext(Dispatchers.IO) {
            host?.stop()
            host = null
        }
        releaseMulticastLock()
        job.cancelChildren()
    }

    private fun acquireMulticastLock() {
        if (multicastLock?.isHeld == true) return
        val ctx = AndroidTransportEnv.appContext ?: return
        val wifi = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return
        multicastLock = wifi.createMulticastLock("datingapp-libp2p-mdns").apply {
            setReferenceCounted(false)
            runCatching { acquire() }
        }
    }

    private fun releaseMulticastLock() {
        multicastLock?.let { if (it.isHeld) runCatching { it.release() } }
        multicastLock = null
    }

    override suspend fun connect(multiaddr: String) {
        withContext(Dispatchers.IO) { requireHost().connect(multiaddr) }
    }

    override val peerId: String get() = requireHost().peerID()

    // AutoNAT verdict (Idea-C gate). "Unknown" until the host is up + AutoNAT has decided.
    override fun reachability(): String = host?.reachability() ?: "Unknown"

    override val listenAddrs: List<String>
        get() = requireHost().listenAddrs().split('\n').filter { it.isNotBlank() }

    // ---- DHT ----

    override suspend fun dhtPutValue(key: ByteArray, value: ByteArray) {
        withContext(Dispatchers.IO) { requireHost().dhtPut(key, value) }
    }

    override suspend fun dhtGetValues(key: ByteArray, maxResults: Int): List<ByteArray> =
        withContext(Dispatchers.IO) {
            val list = requireHost().dhtGet(key, maxResults.toLong())
            (0 until list.len()).map { list.get(it) }
        }

    override suspend fun dhtProvide(key: ByteArray) {
        withContext(Dispatchers.IO) { requireHost().dhtProvide(key) }
    }

    override fun dhtFindProviders(key: ByteArray): Flow<String> = callbackFlow {
        requireHost().dhtFindProviders(key, object : ProviderCallback {
            override fun onProvider(peerID: String) { trySend(peerID) }
        })
        // The Go goroutine ends on provider exhaustion or host ctx cancel (stop()).
        awaitClose { }
    }

    // ---- GossipSub ----

    override fun gossipSubscribe(topic: String): Flow<GossipMessage> = callbackFlow {
        requireHost().gossipSubscribe(topic, object : GossipCallback {
            override fun onMessage(t: String, from: String, payload: ByteArray) {
                trySend(GossipMessage(t, from, payload))
            }
        })
        awaitClose { runCatching { host?.gossipUnsubscribe(topic) } }
    }

    override suspend fun gossipUnsubscribe(topic: String) {
        withContext(Dispatchers.IO) { requireHost().gossipUnsubscribe(topic) }
    }

    override suspend fun gossipPublish(topic: String, payload: ByteArray) {
        withContext(Dispatchers.IO) { requireHost().gossipPublish(topic, payload) }
    }

    // ---- Streams ----

    override fun registerStreamHandler(protocolId: String, handler: StreamHandler) {
        requireHost().registerStreamHandler(protocolId, object : StreamCallback {
            override fun onStream(s: GoStream) {
                scope.launch { handler.handle(Libp2pStream().also { it.go = s }) }
            }
        })
    }

    override suspend fun openStream(remotePeerId: String, protocolId: String): Libp2pStream =
        withContext(Dispatchers.IO) {
            Libp2pStream().also { it.go = requireHost().openStream(remotePeerId, protocolId) }
        }
}

actual class Libp2pStream {
    internal var go: GoStream? = null
    private fun s(): GoStream = go ?: error("Libp2pStream not initialised")

    actual suspend fun readBytes(max: Int): ByteArray =
        withContext(Dispatchers.IO) { s().read(max.toLong()) }

    actual suspend fun writeBytes(bytes: ByteArray) {
        withContext(Dispatchers.IO) { s().write(bytes) }
    }

    actual suspend fun close() {
        withContext(Dispatchers.IO) { s().close() }
    }
}
