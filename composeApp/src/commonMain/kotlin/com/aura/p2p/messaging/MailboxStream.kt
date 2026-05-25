package com.aura.p2p.messaging

import com.aura.domain.auth.SignatureVerifier
import com.aura.p2p.fetch.readFrame
import com.aura.p2p.fetch.writeFrame
import com.aura.p2p.relay.EpochClock
import com.aura.p2p.relay.MailboxKeys
import com.aura.p2p.relay.SystemClock
import com.aura.p2p.transport.Libp2pStream
import com.aura.p2p.transport.Transport
import com.aura.p2p.transport.PeerIdentity
import com.aura.p2p.transport.StreamHandler
import com.aura.p2p.transport.wire.MailboxRequest
import com.aura.p2p.transport.wire.MailboxResponse
import com.aura.records.canonical.decodeMailboxRequest
import com.aura.records.canonical.decodeMailboxResponse
import com.aura.records.canonical.encodeMailboxRequest
import com.aura.records.canonical.encodeMailboxResponse
import kotlinx.coroutines.flow.Flow
import kotlin.math.abs

/** libp2p stream protocol for offline mailbox deposit / pull (ADR-0001 / M5). */
const val MAILBOX_PROTOCOL_ID: String = "/aura/mailbox/1.0.0"

/** A pull can return several envelopes; 1 MiB is plenty for queued text messages. */
const val MAILBOX_MAX_FRAME: Int = 1 * 1024 * 1024

/** Round-trip on [MAILBOX_PROTOCOL_ID]; fakeable so [MailboxService] is unit-testable. */
interface MailboxStreamClient {
    suspend fun request(holderPeerId: String, request: MailboxRequest): MailboxResponse?
}

class Libp2pMailboxStreamClient(private val transport: Transport) : MailboxStreamClient {
    override suspend fun request(holderPeerId: String, request: MailboxRequest): MailboxResponse? {
        val stream = transport.openStream(holderPeerId, MAILBOX_PROTOCOL_ID)
        return try {
            stream.writeFrame(encodeMailboxRequest(request))
            decodeMailboxResponse(stream.readFrame(MAILBOX_MAX_FRAME))
        } finally {
            runCatching { stream.close() }
        }
    }
}

/** Finds mailbox-holder peerIds for a recipient via DHT provider records. */
interface MailboxHolderLocator {
    fun find(recipientDid: String): Flow<String>
}

class TransportMailboxHolderLocator(private val transport: Transport) : MailboxHolderLocator {
    override fun find(recipientDid: String): Flow<String> =
        transport.dhtFindProviders(MailboxKeys.holderKey(recipientDid))
}

/**
 * Holder-side handler: stores deposits into [holder] (after verifying the sender
 * signature) and serves pulls. A pull must be authenticated - the caller proves
 * control of the recipient DID by signing `pull|recipientDid|holderPeerId|authAtMs`
 * with the recipient's P-256 key - which closes the former open-pull oracle where any
 * peer could probe an arbitrary DID for pending mail. Binding to this holder's peerId
 * stops a malicious holder replaying a captured token to a different holder; the
 * freshness window ([maxSkewMs]) bounds replay. Content stays E2E-encrypted; full
 * sealed-sender (hiding the parties from the holder) remains deferred (ADR-0001).
 */
class MailboxStreamServer(
    private val holder: MailboxHolder,
    private val verifier: SignatureVerifier,
    private val clock: EpochClock = SystemClock,
    private val maxSkewMs: Long = DEFAULT_MAX_SKEW_MS,
) {
    fun register(transport: Transport) {
        val selfPeerId = transport.peerId
        transport.registerStreamHandler(MAILBOX_PROTOCOL_ID, object : StreamHandler {
            override suspend fun handle(stream: Libp2pStream) {
                try {
                    val request = decodeMailboxRequest(stream.readFrame(MAILBOX_MAX_FRAME))
                    stream.writeFrame(encodeMailboxResponse(respond(request, selfPeerId)))
                } catch (_: Throwable) {
                    // Malformed frame / dropped peer.
                } finally {
                    runCatching { stream.close() }
                }
            }
        })
    }

    private suspend fun respond(request: MailboxRequest, selfPeerId: String): MailboxResponse = when (request.op) {
        MailboxRequest.OP_DEPOSIT -> {
            val envelope = request.envelope
            when {
                envelope == null || envelope.recipientDid != request.recipientDid ->
                    MailboxResponse(ok = false, error = "bad_request")
                !MessageEnvelopeVerifier.verify(envelope, verifier) ->
                    MailboxResponse(ok = false, error = "unverified")
                else -> MailboxResponse(ok = holder.deposit(envelope))
            }
        }
        MailboxRequest.OP_PULL ->
            if (!authorizedPull(request, selfPeerId)) MailboxResponse(ok = false, error = "unauthorized")
            else MailboxResponse(ok = true, envelopes = holder.pull(request.recipientDid))
        else -> MailboxResponse(ok = false, error = "unknown_op")
    }

    /** True iff the request carries a fresh, valid signature by [MailboxRequest.recipientDid] for this holder. */
    internal fun authorizedPull(request: MailboxRequest, selfPeerId: String): Boolean {
        val signature = request.authSignature ?: return false
        if (abs(clock.nowMs() - request.authAtMs) > maxSkewMs) return false
        val publicKey = try {
            PeerIdentity.p256FromDidKey(request.recipientDid)
        } catch (_: IllegalArgumentException) {
            return false
        }
        val payload = MailboxRequest.pullSignable(request.recipientDid, selfPeerId, request.authAtMs)
        return verifier.verify(publicKey, payload, signature)
    }

    companion object {
        /** Allowed clock skew between recipient and holder for a pull token (5 min). */
        const val DEFAULT_MAX_SKEW_MS: Long = 5L * 60 * 1000
    }
}
