package com.aura.p2p.transport

import kotlinx.coroutines.flow.Flow

// iOS libp2p deferred (no hardware, lazy state). Fail-loud stub.
// TODO(iOS): real transport via a Swift/native libp2p once iOS is in scope.
private fun unsupported(): Nothing =
    throw UnsupportedOperationException("iOS libp2p not wired (deferred)")

actual class Libp2pTransport actual constructor(config: Libp2pConfig) : Transport {
    override suspend fun start(): Unit = unsupported()
    override suspend fun stop(): Unit = unsupported()
    override suspend fun connect(multiaddr: String): Unit = unsupported()
    override val peerId: String get() = unsupported()
    override val listenAddrs: List<String> get() = unsupported()
    override fun reachability(): String = "Unknown" // iOS transport deferred; status query stays soft

    override suspend fun dhtPutValue(key: ByteArray, value: ByteArray): Unit = unsupported()
    override suspend fun dhtGetValues(key: ByteArray, maxResults: Int): List<ByteArray> = unsupported()
    override suspend fun dhtProvide(key: ByteArray): Unit = unsupported()
    override fun dhtFindProviders(key: ByteArray): Flow<String> = unsupported()

    override fun gossipSubscribe(topic: String): Flow<GossipMessage> = unsupported()
    override suspend fun gossipUnsubscribe(topic: String): Unit = unsupported()
    override suspend fun gossipPublish(topic: String, payload: ByteArray): Unit = unsupported()

    override fun registerStreamHandler(protocolId: String, handler: StreamHandler): Unit = unsupported()
    override suspend fun openStream(remotePeerId: String, protocolId: String): Libp2pStream = unsupported()
}

actual class Libp2pStream {
    actual suspend fun readBytes(max: Int): ByteArray = unsupported()
    actual suspend fun writeBytes(bytes: ByteArray): Unit = unsupported()
    actual suspend fun close(): Unit = unsupported()
}
