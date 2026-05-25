package com.aura.p2p.transport

import kotlinx.coroutines.flow.Flow

/**
 * Transport contract over libp2p (DHT + GossipSub + streams). All app code depends on this
 * interface, not the concrete [Libp2pTransport] expect class, so an alternative implementation
 * (notably the in-memory simulation transport used by the testbed) can be substituted. The
 * production actual is the gomobile go-libp2p host (ADR-0004).
 */
interface Transport {
    suspend fun start()
    suspend fun stop()
    val peerId: String
    val listenAddrs: List<String>

    /** AutoNAT's current reachability verdict: "Public", "Private", or "Unknown". Drives the
     *  Idea-C reachability gate for the opt-in serving role. "Unknown" before the host is started
     *  or before AutoNAT has decided. */
    fun reachability(): String

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
