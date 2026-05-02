package fyp.project.datingapp.p2p.relay

import fyp.project.datingapp.p2p.transport.GossipMessage
import fyp.project.datingapp.p2p.transport.Libp2pTransport
import kotlinx.coroutines.flow.Flow

/**
 * The narrow slice of [Libp2pTransport] that [DefaultGossipSubInvalidator] needs:
 * just GossipSub subscribe / unsubscribe / publish. [Libp2pTransport] is an
 * `expect class` and can't be faked in commonTest, so the invalidator depends on
 * this interface instead and tests supply a fake (mirrors the
 * [fyp.project.datingapp.p2p.fetch.ProfileStreamClient] seam).
 */
interface GossipChannel {
    fun subscribe(topic: String): Flow<GossipMessage>
    suspend fun unsubscribe(topic: String)
    suspend fun publish(topic: String, payload: ByteArray)
}

/** Production adapter binding [GossipChannel] to the real libp2p host. */
class TransportGossipChannel(private val transport: Libp2pTransport) : GossipChannel {
    override fun subscribe(topic: String): Flow<GossipMessage> = transport.gossipSubscribe(topic)
    override suspend fun unsubscribe(topic: String) = transport.gossipUnsubscribe(topic)
    override suspend fun publish(topic: String, payload: ByteArray) =
        transport.gossipPublish(topic, payload)
}
