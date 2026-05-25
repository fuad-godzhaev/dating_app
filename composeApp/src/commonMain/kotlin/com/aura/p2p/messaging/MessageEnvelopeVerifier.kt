package com.aura.p2p.messaging

import com.aura.domain.auth.SignatureVerifier
import com.aura.p2p.transport.PeerIdentity
import com.aura.p2p.transport.wire.MessageEnvelope
import com.aura.records.canonical.encodeCanonical

/**
 * Verifies the outer P-256 signature on a [MessageEnvelope] against the key in its
 * [MessageEnvelope.senderDid]. This authenticates the sender at the transport layer
 * (separate from the inner ECIES E2E encryption) so a recipient or mailbox
 * holder can reject spoofed / forged envelopes. Mirrors
 * [com.aura.p2p.discovery.PresenceVerifier].
 */
object MessageEnvelopeVerifier {

    fun verify(envelope: MessageEnvelope, verifier: SignatureVerifier): Boolean {
        if (envelope.signature.isEmpty()) return false
        val publicKey = try {
            PeerIdentity.p256FromDidKey(envelope.senderDid)
        } catch (_: IllegalArgumentException) {
            return false
        }
        return verifier.verify(publicKey, encodeCanonical(envelope), envelope.signature)
    }
}
