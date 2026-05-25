package com.aura.p2p.messaging

import com.aura.p2p.discovery.PeerContact
import com.aura.p2p.fetch.readFrame
import com.aura.p2p.fetch.writeFrame
import com.aura.p2p.transport.Libp2pStream
import com.aura.p2p.transport.Transport
import com.aura.p2p.transport.StreamHandler

/** libp2p stream for delivery receipts (DELIVERED acks), ADR-0001. */
const val RECEIPT_PROTOCOL_ID: String = "/aura/receipt/1.0.0"

/** A receipt is a single msgId string; 4 KiB is plenty. */
const val RECEIPT_MAX_FRAME: Int = 4 * 1024

/**
 * Sends a delivery receipt (the original message's msgId) back to that message's sender
 * over [RECEIPT_PROTOCOL_ID] and reads a one-byte ack. This lets the sender flip an
 * outgoing message SENT -> DELIVERED once the recipient has actually stored it - notably
 * after the recipient pulls it from a mailbox holder. Behind an interface so
 * [MessageService] stays unit-testable without the (expect-class) transport.
 */
interface ReceiptStreamClient {
    /** Deliver [receiptBytes] (the msgId) to [contact]; true iff the peer acked. */
    suspend fun send(contact: PeerContact, receiptBytes: ByteArray): Boolean
}

/** Real client over the go-libp2p host. */
class Libp2pReceiptStreamClient(private val transport: Transport) : ReceiptStreamClient {
    override suspend fun send(contact: PeerContact, receiptBytes: ByteArray): Boolean {
        contact.multiaddrs.firstOrNull()?.let { runCatching { transport.connect(it) } }
        val stream = transport.openStream(contact.peerId, RECEIPT_PROTOCOL_ID)
        return try {
            stream.writeFrame(receiptBytes)
            val ack = stream.readFrame(16)
            ack.isNotEmpty() && ack[0].toInt() == 1
        } finally {
            runCatching { stream.close() }
        }
    }
}

/**
 * Owner-side handler for [RECEIPT_PROTOCOL_ID]: reads a receipt frame, hands it to
 * [onReceipt] (mark the matching outgoing message delivered) and acks 1 on success, 0
 * otherwise. Register on the transport after the host starts (from `PeerProfileFeed`).
 */
class ReceiptStreamServer(private val onReceipt: suspend (ByteArray) -> Boolean) {
    fun register(transport: Transport) {
        transport.registerStreamHandler(RECEIPT_PROTOCOL_ID, object : StreamHandler {
            override suspend fun handle(stream: Libp2pStream) {
                try {
                    val bytes = stream.readFrame(RECEIPT_MAX_FRAME)
                    val ok = runCatching { onReceipt(bytes) }.getOrDefault(false)
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
