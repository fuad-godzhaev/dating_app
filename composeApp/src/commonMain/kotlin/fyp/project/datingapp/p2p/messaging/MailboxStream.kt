package fyp.project.datingapp.p2p.messaging

import fyp.project.datingapp.domain.auth.SignatureVerifier
import fyp.project.datingapp.p2p.fetch.readFrame
import fyp.project.datingapp.p2p.fetch.writeFrame
import fyp.project.datingapp.p2p.relay.MailboxKeys
import fyp.project.datingapp.p2p.transport.Libp2pStream
import fyp.project.datingapp.p2p.transport.Libp2pTransport
import fyp.project.datingapp.p2p.transport.StreamHandler
import fyp.project.datingapp.p2p.transport.wire.MailboxRequest
import fyp.project.datingapp.p2p.transport.wire.MailboxResponse
import fyp.project.datingapp.records.canonical.decodeMailboxRequest
import fyp.project.datingapp.records.canonical.decodeMailboxResponse
import fyp.project.datingapp.records.canonical.encodeMailboxRequest
import fyp.project.datingapp.records.canonical.encodeMailboxResponse
import kotlinx.coroutines.flow.Flow

/** libp2p stream protocol for offline mailbox deposit / pull (ADR-0001 / M5). */
const val MAILBOX_PROTOCOL_ID: String = "/datingapp/mailbox/1.0.0"

/** A pull can return several envelopes; 1 MiB is plenty for queued text messages. */
const val MAILBOX_MAX_FRAME: Int = 1 * 1024 * 1024

/** Round-trip on [MAILBOX_PROTOCOL_ID]; fakeable so [MailboxService] is unit-testable. */
interface MailboxStreamClient {
    suspend fun request(holderPeerId: String, request: MailboxRequest): MailboxResponse?
}

class Libp2pMailboxStreamClient(private val transport: Libp2pTransport) : MailboxStreamClient {
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

class TransportMailboxHolderLocator(private val transport: Libp2pTransport) : MailboxHolderLocator {
    override fun find(recipientDid: String): Flow<String> =
        transport.dhtFindProviders(MailboxKeys.holderKey(recipientDid))
}

/**
 * Holder-side handler: stores deposits into [holder] (after verifying the sender
 * signature) and serves pulls. Open pull v1 leaks a "recipient-has-mail" signal but
 * not content (E2E-encrypted); sealed-sender hardening is deferred (ADR-0001).
 */
class MailboxStreamServer(
    private val holder: MailboxHolder,
    private val verifier: SignatureVerifier,
) {
    fun register(transport: Libp2pTransport) {
        transport.registerStreamHandler(MAILBOX_PROTOCOL_ID, object : StreamHandler {
            override suspend fun handle(stream: Libp2pStream) {
                try {
                    val request = decodeMailboxRequest(stream.readFrame(MAILBOX_MAX_FRAME))
                    stream.writeFrame(encodeMailboxResponse(respond(request)))
                } catch (_: Throwable) {
                    // Malformed frame / dropped peer.
                } finally {
                    runCatching { stream.close() }
                }
            }
        })
    }

    private suspend fun respond(request: MailboxRequest): MailboxResponse = when (request.op) {
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
        MailboxRequest.OP_PULL -> MailboxResponse(ok = true, envelopes = holder.pull(request.recipientDid))
        else -> MailboxResponse(ok = false, error = "unknown_op")
    }
}
