package fyp.project.datingapp.p2p.transport.wire

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request on `/datingapp/mailbox/1.0.0` (ADR-0001 / M5). One protocol, two ops:
 *  - `deposit`: a sender leaves a sealed [envelope] for [recipientDid] at a holder.
 *    Authenticated by the sender's signature on the envelope itself.
 *  - `pull`: a recipient retrieves the sealed envelopes queued for [recipientDid].
 *    Authenticated by [authSignature]: the recipient signs `pull|recipientDid|
 *    holderPeerId|authAtMs` with its P-256 key, proving control of [recipientDid] and
 *    binding the request to one holder + a fresh timestamp. This closes the former
 *    open-pull oracle (any peer could otherwise probe an arbitrary DID for pending
 *    mail). [authAtMs] is epoch-millis; the holder rejects stale requests.
 *
 * The envelope stays end-to-end encrypted the whole time; the holder only ever sees
 * ciphertext + routing metadata.
 */
@Serializable
data class MailboxRequest(
    @SerialName($$"$type") val type: String = "fyp.project.datingapp.p2p.mailboxRequest",
    val op: String,
    val recipientDid: String,
    val envelope: MessageEnvelope? = null,
    val authAtMs: Long = 0,
    val authSignature: ByteArray? = null,
) {
    companion object {
        const val OP_DEPOSIT = "deposit"
        const val OP_PULL = "pull"

        /** Bytes a recipient signs to authenticate a pull from a specific holder. */
        fun pullSignable(recipientDid: String, holderPeerId: String, authAtMs: Long): ByteArray =
            "pull|$recipientDid|$holderPeerId|$authAtMs".encodeToByteArray()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MailboxRequest) return false
        return type == other.type &&
            op == other.op &&
            recipientDid == other.recipientDid &&
            envelope == other.envelope &&
            authAtMs == other.authAtMs &&
            (authSignature?.contentEquals(other.authSignature ?: ByteArray(0)) ?: (other.authSignature == null))
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + op.hashCode()
        result = 31 * result + recipientDid.hashCode()
        result = 31 * result + (envelope?.hashCode() ?: 0)
        result = 31 * result + authAtMs.hashCode()
        result = 31 * result + (authSignature?.contentHashCode() ?: 0)
        return result
    }
}

/** Response: deposit ack ([ok]) or the pulled [envelopes]. */
@Serializable
data class MailboxResponse(
    @SerialName($$"$type") val type: String = "fyp.project.datingapp.p2p.mailboxResponse",
    val ok: Boolean = false,
    val envelopes: List<MessageEnvelope> = emptyList(),
    val error: String? = null,
)
