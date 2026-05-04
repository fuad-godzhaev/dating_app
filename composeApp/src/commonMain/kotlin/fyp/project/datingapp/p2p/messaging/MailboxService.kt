package fyp.project.datingapp.p2p.messaging

import fyp.project.datingapp.p2p.relay.EpochClock
import fyp.project.datingapp.p2p.relay.SystemClock
import fyp.project.datingapp.p2p.transport.wire.MailboxRequest
import fyp.project.datingapp.p2p.transport.wire.MessageEnvelope
import fyp.project.datingapp.records.canonical.encodeMessageEnvelopeWire
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList

/**
 * Drives offline store-and-forward (ADR-0001 / M5): a sender parks a sealed
 * envelope at the recipient's mailbox holders when online delivery fails, and a
 * recipient collects its queued mail on reconnect. Holders are found via DHT
 * provider records (reuses the Phase F machinery). Depends only on seams, so the
 * flow is unit-testable without the transport.
 */
class MailboxService(
    private val selfDid: suspend () -> String?,
    private val locator: MailboxHolderLocator,
    private val streamClient: MailboxStreamClient,
    // Feed a pulled envelope (as wire bytes) back through the normal receive path
    // (verify + decrypt + dedup + store) = MessageService.handleIncoming.
    private val onEnvelope: suspend (ByteArray) -> Boolean,
    // Signs the pull token proving we control our DID (P-256, = AuthRepository.sign).
    private val sign: suspend (ByteArray) -> ByteArray,
    private val clock: EpochClock = SystemClock,
    private val maxHolders: Int = DEFAULT_MAX_HOLDERS,
) {
    /** Park [envelope] at up to [maxHolders] of the recipient's mailbox holders. */
    suspend fun deposit(recipientDid: String, envelope: MessageEnvelope): Boolean {
        var accepted = false
        val holders = locator.find(recipientDid).take(maxHolders).toList()
        for (holderPeerId in holders) {
            val response = runCatching {
                streamClient.request(
                    holderPeerId,
                    MailboxRequest(op = MailboxRequest.OP_DEPOSIT, recipientDid = recipientDid, envelope = envelope),
                )
            }.getOrNull()
            if (response?.ok == true) accepted = true
        }
        return accepted
    }

    /** Pull this device's queued mail from its holders and deliver each. Returns the count delivered. */
    suspend fun pullOwnMail(): Int {
        val me = selfDid() ?: return 0
        var delivered = 0
        val holders = locator.find(me).take(maxHolders).toList()
        for (holderPeerId in holders) {
            // Authenticate the pull: sign a fresh token bound to this holder so only we
            // (the DID owner) can retrieve our mail and the token can't be replayed elsewhere.
            val authAtMs = clock.nowMs()
            val signature = runCatching { sign(MailboxRequest.pullSignable(me, holderPeerId, authAtMs)) }.getOrNull()
                ?: continue
            val response = runCatching {
                streamClient.request(
                    holderPeerId,
                    MailboxRequest(
                        op = MailboxRequest.OP_PULL,
                        recipientDid = me,
                        authAtMs = authAtMs,
                        authSignature = signature,
                    ),
                )
            }.getOrNull()
            response?.envelopes?.forEach { envelope ->
                val ok = runCatching { onEnvelope(encodeMessageEnvelopeWire(envelope)) }.getOrDefault(false)
                if (ok) delivered++
            }
        }
        return delivered
    }

    companion object {
        const val DEFAULT_MAX_HOLDERS: Int = 3
    }
}
