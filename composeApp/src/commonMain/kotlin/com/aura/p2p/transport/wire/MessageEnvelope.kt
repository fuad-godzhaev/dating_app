package com.aura.p2p.transport.wire

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One end-to-end-encrypted chat message in transit (ADR-0001). The [ciphertext] is
 * an opaque [MessageCrypto] payload (P-256 ECIES) — never plaintext. The outer
 * [signature] is the *sender's* P-256 signature over the signable projection (every
 * field except `signature`); it authenticates the sender to the recipient and to any
 * mailbox holder (abuse control), independent of the inner E2E encryption.
 *
 * Carried online over `/aura/message/1.0.0` and, when the recipient is offline,
 * deposited at a cacheHolder over `/aura/mailbox/1.0.0`.
 */
@Serializable
data class MessageEnvelope(
    @SerialName($$"$type") val type: String = "com.aura.p2p.message",
    val senderDid: String,
    val recipientDid: String,
    val msgId: String,
    val ciphertext: ByteArray,
    // Crypto-scheme tag for the ciphertext (see MessageCrypto). Lets the wire format
    // evolve if the encryption scheme is ever versioned/changed.
    val messageType: Int,
    val sentAt: String,
    val signature: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MessageEnvelope) return false
        return type == other.type &&
            senderDid == other.senderDid &&
            recipientDid == other.recipientDid &&
            msgId == other.msgId &&
            ciphertext.contentEquals(other.ciphertext) &&
            messageType == other.messageType &&
            sentAt == other.sentAt &&
            signature.contentEquals(other.signature)
    }

    override fun hashCode(): Int {
        var h = type.hashCode()
        h = 31 * h + senderDid.hashCode()
        h = 31 * h + recipientDid.hashCode()
        h = 31 * h + msgId.hashCode()
        h = 31 * h + ciphertext.contentHashCode()
        h = 31 * h + messageType
        h = 31 * h + sentAt.hashCode()
        h = 31 * h + signature.contentHashCode()
        return h
    }
}
