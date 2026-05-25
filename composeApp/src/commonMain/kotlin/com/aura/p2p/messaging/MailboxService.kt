package com.aura.p2p.messaging

import com.aura.p2p.relay.EpochClock
import com.aura.p2p.relay.SystemClock
import com.aura.p2p.transport.wire.MailboxRequest
import com.aura.p2p.transport.wire.MessageEnvelope
import com.aura.records.canonical.encodeMessageEnvelopeWire
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
        // Mailbox replication factor. Measured (sim-results phase2, mocknet at scale): rendezvous
        // recall_alive ~= 1-(1-p_survive)^R, holder-survival driven and ~independent of DHT scale.
        // At ~75% cumulative churn, r=3 holds ~55% but r=5 holds ~79%; at ~50% churn r=5 ~98.5%.
        // Raised 3 -> 5 for churn resilience. NB: this needs enough INBOUND-REACHABLE holders in the
        // recipient's cell to land 5 deposits (the ~17% reachable-holder constraint); it pairs with
        // the reachable/relay tier (quiet rostered FGS + volunteers), not replication alone.
        const val DEFAULT_MAX_HOLDERS: Int = 5
    }
}
