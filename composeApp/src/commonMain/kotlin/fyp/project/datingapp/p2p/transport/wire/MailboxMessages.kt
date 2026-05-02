package fyp.project.datingapp.p2p.transport.wire

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request on `/datingapp/mailbox/1.0.0` (ADR-0001 / M5). One protocol, two ops:
 *  - `deposit`: a sender leaves a sealed [envelope] for [recipientDid] at a holder.
 *  - `pull`: a recipient retrieves the sealed envelopes queued for [recipientDid].
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
) {
    companion object {
        const val OP_DEPOSIT = "deposit"
        const val OP_PULL = "pull"
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
