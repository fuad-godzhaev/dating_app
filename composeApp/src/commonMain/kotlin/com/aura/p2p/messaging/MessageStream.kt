package com.aura.p2p.messaging

import com.aura.p2p.discovery.PeerContact
import com.aura.p2p.fetch.readFrame
import com.aura.p2p.fetch.writeFrame
import com.aura.p2p.transport.Libp2pStream
import com.aura.p2p.transport.Transport
import com.aura.p2p.transport.StreamHandler

/** libp2p stream protocol for online 1:1 message delivery (ADR-0001). */
const val MESSAGE_PROTOCOL_ID: String = "/aura/message/1.0.0"

/** Messages are small (ciphertext + routing metadata); 256 KiB is generous. */
const val MESSAGE_MAX_FRAME: Int = 256 * 1024

/**
 * Sends a wire [com.aura.p2p.transport.wire.MessageEnvelope] to a peer
 * over [MESSAGE_PROTOCOL_ID] and reads a one-byte ack. Behind an interface so
 * [MessageService] is unit-testable without the (expect-class) transport.
 */
interface MessageStreamClient {
    /** Deliver [envelopeWireBytes] to [contact]; true iff the peer acked receipt. */
    suspend fun send(contact: PeerContact, envelopeWireBytes: ByteArray): Boolean
}

/** Real client over the go-libp2p host. */
class Libp2pMessageStreamClient(private val transport: Transport) : MessageStreamClient {
    override suspend fun send(contact: PeerContact, envelopeWireBytes: ByteArray): Boolean {
        contact.multiaddrs.firstOrNull()?.let { runCatching { transport.connect(it) } }
        val stream = transport.openStream(contact.peerId, MESSAGE_PROTOCOL_ID)
        return try {
            stream.writeFrame(envelopeWireBytes)
            val ack = stream.readFrame(16)
            ack.isNotEmpty() && ack[0].toInt() == 1
        } finally {
            runCatching { stream.close() }
        }
    }
}

/**
 * Owner-side handler for [MESSAGE_PROTOCOL_ID]: reads a wire envelope, hands it to
 * [onEnvelope] (verify + decrypt + store) and acks 1 on success, 0 otherwise.
 * Register on the transport after the host starts (from `PeerProfileFeed`).
 */
class MessageStreamServer(private val onEnvelope: suspend (ByteArray) -> Boolean) {
    fun register(transport: Transport) {
        transport.registerStreamHandler(MESSAGE_PROTOCOL_ID, object : StreamHandler {
            override suspend fun handle(stream: Libp2pStream) {
                try {
                    val bytes = stream.readFrame(MESSAGE_MAX_FRAME)
                    val ok = runCatching { onEnvelope(bytes) }.getOrDefault(false)
                    stream.writeFrame(byteArrayOf(if (ok) 1 else 0))
                } catch (_: Throwable) {
                    // Malformed frame / dropped peer: nothing safe to send; just close.
                } finally {
                    runCatching { stream.close() }
                }
            }
        })
    }
}
