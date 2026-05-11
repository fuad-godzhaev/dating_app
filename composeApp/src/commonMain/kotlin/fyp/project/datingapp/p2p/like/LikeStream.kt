package fyp.project.datingapp.p2p.like

import fyp.project.datingapp.p2p.discovery.PeerContact
import fyp.project.datingapp.p2p.fetch.readFrame
import fyp.project.datingapp.p2p.fetch.writeFrame
import fyp.project.datingapp.p2p.transport.Libp2pStream
import fyp.project.datingapp.p2p.transport.Libp2pTransport
import fyp.project.datingapp.p2p.transport.StreamHandler

/** libp2p stream protocol for delivering a signed Like to a peer (E: match loop). */
const val LIKE_PROTOCOL_ID: String = "/datingapp/like/1.0.0"

/** A like is one small signed envelope; 64 KiB is generous. */
const val LIKE_MAX_FRAME: Int = 64 * 1024

/**
 * Delivers a signed like ([fyp.project.datingapp.p2p.transport.wire.SignedEnvelope] wire
 * bytes) to a peer over [LIKE_PROTOCOL_ID] and reads a one-byte ack. Behind an interface
 * so [LikeService] is unit-testable without the (expect-class) transport.
 */
interface LikeStreamClient {
    suspend fun send(contact: PeerContact, envelopeBytes: ByteArray): Boolean
}

class Libp2pLikeStreamClient(private val transport: Libp2pTransport) : LikeStreamClient {
    override suspend fun send(contact: PeerContact, envelopeBytes: ByteArray): Boolean {
        contact.multiaddrs.firstOrNull()?.let { runCatching { transport.connect(it) } }
        val stream = transport.openStream(contact.peerId, LIKE_PROTOCOL_ID)
        return try {
            stream.writeFrame(envelopeBytes)
            val ack = stream.readFrame(16)
            ack.isNotEmpty() && ack[0].toInt() == 1
        } finally {
            runCatching { stream.close() }
        }
    }
}

/**
 * Recipient-side handler for [LIKE_PROTOCOL_ID]: reads the signed envelope bytes, hands
 * them to [onLike] (verify + store + match-check) and acks 1 on success, 0 otherwise.
 * Register on the transport after the host starts (from `PeerProfileFeed`).
 */
class LikeStreamServer(private val onLike: suspend (ByteArray) -> Boolean) {
    fun register(transport: Libp2pTransport) {
        transport.registerStreamHandler(LIKE_PROTOCOL_ID, object : StreamHandler {
            override suspend fun handle(stream: Libp2pStream) {
                try {
                    val bytes = stream.readFrame(LIKE_MAX_FRAME)
                    val ok = runCatching { onLike(bytes) }.getOrDefault(false)
                    stream.writeFrame(byteArrayOf(if (ok) 1 else 0))
                } catch (_: Throwable) {
                    // Malformed frame / dropped peer.
                } finally {
                    runCatching { stream.close() }
                }
            }
        })
    }
}
