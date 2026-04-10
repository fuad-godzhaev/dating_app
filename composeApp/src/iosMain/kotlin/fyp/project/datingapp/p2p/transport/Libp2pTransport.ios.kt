package fyp.project.datingapp.p2p.transport

import kotlinx.coroutines.flow.Flow

// iOS libp2p deferred (no hardware, lazy state). Fail-loud stub.
// TODO(iOS): real transport via a Swift/native libp2p once iOS is in scope.
private fun unsupported(): Nothing =
    throw UnsupportedOperationException("iOS libp2p not wired (deferred)")

actual class Libp2pTransport actual constructor(config: Libp2pConfig) {
    actual suspend fun start(): Unit = unsupported()
    actual suspend fun stop(): Unit = unsupported()
    actual suspend fun connect(multiaddr: String): Unit = unsupported()
    actual val peerId: String get() = unsupported()
    actual val listenAddrs: List<String> get() = unsupported()

    actual suspend fun dhtPutValue(key: ByteArray, value: ByteArray): Unit = unsupported()
    actual suspend fun dhtGetValues(key: ByteArray, maxResults: Int): List<ByteArray> = unsupported()
    actual suspend fun dhtProvide(key: ByteArray): Unit = unsupported()
    actual fun dhtFindProviders(key: ByteArray): Flow<String> = unsupported()

    actual fun gossipSubscribe(topic: String): Flow<GossipMessage> = unsupported()
    actual suspend fun gossipUnsubscribe(topic: String): Unit = unsupported()
    actual suspend fun gossipPublish(topic: String, payload: ByteArray): Unit = unsupported()

    actual fun registerStreamHandler(protocolId: String, handler: StreamHandler): Unit = unsupported()
    actual suspend fun openStream(remotePeerId: String, protocolId: String): Libp2pStream = unsupported()
}

actual class Libp2pStream {
    actual suspend fun readBytes(max: Int): ByteArray = unsupported()
    actual suspend fun writeBytes(bytes: ByteArray): Unit = unsupported()
    actual suspend fun close(): Unit = unsupported()
}
