package fyp.project.datingapp.p2p.transport

import kotlinx.coroutines.flow.Flow

/** Max single-frame read on a libp2p stream (1 MiB). */
const val LIBP2P_MAX_FRAME: Int = 1 * 1024 * 1024

/**
 * Thin transport contract over libp2p (DHT + GossipSub + streams). Common
 * code depends only on this surface; the Android actual holds the real
 * jvm-libp2p `Host`. PROVISIONAL — signatures may shift once the jvm-libp2p
 * 1.2.2 API is wired in B.4.
 */
expect class Libp2pTransport(config: Libp2pConfig) {
    suspend fun start()
    suspend fun stop()
    val peerId: String
    val listenAddrs: List<String>

    /** Dial a peer by full multiaddr ("/ip4/.../tcp/.../p2p/<peerId>"). */
    suspend fun connect(multiaddr: String)

    // DHT
    suspend fun dhtPutValue(key: ByteArray, value: ByteArray)
    suspend fun dhtGetValues(key: ByteArray, maxResults: Int): List<ByteArray>
    suspend fun dhtProvide(key: ByteArray)
    fun dhtFindProviders(key: ByteArray): Flow<String>

    // GossipSub
    fun gossipSubscribe(topic: String): Flow<GossipMessage>
    suspend fun gossipUnsubscribe(topic: String)
    suspend fun gossipPublish(topic: String, payload: ByteArray)

    // Streams
    fun registerStreamHandler(protocolId: String, handler: StreamHandler)
    suspend fun openStream(remotePeerId: String, protocolId: String): Libp2pStream
}

data class Libp2pConfig(
    val identityKey: ByteArray,                                    // raw 32-byte Ed25519 seed
    val listenAddrs: List<String> = listOf("/ip4/0.0.0.0/tcp/0"),
    val bootstrapPeers: List<String> = emptyList(),
    val protocolPrefix: String = "/datingapp",
    // go-libp2p's built-in mDNS. Keep false on Android (SELinux blocks the netlink
    // interface enumeration it needs, b/155595000); LAN discovery uses NsdManager.
    // True only on desktop/tests.
    val enableMdns: Boolean = false,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Libp2pConfig) return false
        return identityKey.contentEquals(other.identityKey) &&
            listenAddrs == other.listenAddrs &&
            bootstrapPeers == other.bootstrapPeers &&
            protocolPrefix == other.protocolPrefix &&
            enableMdns == other.enableMdns
    }

    override fun hashCode(): Int {
        var h = identityKey.contentHashCode()
        h = 31 * h + listenAddrs.hashCode()
        h = 31 * h + bootstrapPeers.hashCode()
        h = 31 * h + protocolPrefix.hashCode()
        h = 31 * h + enableMdns.hashCode()
        return h
    }
}

data class GossipMessage(
    val topic: String,
    val from: String,
    val payload: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GossipMessage) return false
        return topic == other.topic && from == other.from && payload.contentEquals(other.payload)
    }

    override fun hashCode(): Int {
        var h = topic.hashCode()
        h = 31 * h + from.hashCode()
        h = 31 * h + payload.contentHashCode()
        return h
    }
}

interface StreamHandler {
    suspend fun handle(stream: Libp2pStream)
}

expect class Libp2pStream {
    suspend fun readBytes(max: Int = LIBP2P_MAX_FRAME): ByteArray
    suspend fun writeBytes(bytes: ByteArray)
    suspend fun close()
}
