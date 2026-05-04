package fyp.project.datingapp.p2p.messaging

import fyp.project.datingapp.domain.auth.SignatureVerifier
import fyp.project.datingapp.p2p.relay.EpochClock
import fyp.project.datingapp.p2p.relay.FakeCacheEncryption
import fyp.project.datingapp.p2p.transport.PeerIdentity
import fyp.project.datingapp.p2p.transport.wire.MailboxRequest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Authenticated-pull gate on the holder: a pull is served only if it carries a fresh
 * signature by the recipient DID, bound to this holder's peerId. Closes the open-pull
 * metadata oracle. Uses a real P-256 did:key golden vector + a capturing verifier.
 */
class MailboxPullAuthTest {

    private val recipientDid = "did:key:zDnaembgSGUhZULN2Caob4HLJPaxBh92N7rtH21TErzqf8HQo"
    private val holderPeerId = "12D3KooWHolderPeerIdExample000000000000000000"

    private class MutableClock(var t: Long = 10_000L) : EpochClock {
        override fun nowMs(): Long = t
    }

    private class CapturingVerifier(private val result: Boolean) : SignatureVerifier {
        var lastPublicKey: ByteArray? = null
        var lastData: ByteArray? = null
        var calls = 0
        override fun verify(publicKey: ByteArray, data: ByteArray, signature: ByteArray, algorithm: String): Boolean {
            calls++
            lastPublicKey = publicKey
            lastData = data
            return result
        }
    }

    private fun server(verifier: SignatureVerifier, clock: EpochClock) =
        MailboxStreamServer(
            holder = MailboxHolder(dao = FakeMailboxDao(), cache = FakeCacheEncryption(), clock = clock),
            verifier = verifier,
            clock = clock,
            maxSkewMs = 5_000,
        )

    private fun pull(atMs: Long, signature: ByteArray? = byteArrayOf(7, 7, 7)) =
        MailboxRequest(
            op = MailboxRequest.OP_PULL,
            recipientDid = recipientDid,
            authAtMs = atMs,
            authSignature = signature,
        )

    @Test fun validToken_passes_andSignsHolderBoundPayload() {
        val clock = MutableClock(10_000)
        val verifier = CapturingVerifier(result = true)
        assertTrue(server(verifier, clock).authorizedPull(pull(atMs = 10_000), holderPeerId))
        assertContentEquals(PeerIdentity.p256FromDidKey(recipientDid), verifier.lastPublicKey)
        assertContentEquals(
            MailboxRequest.pullSignable(recipientDid, holderPeerId, 10_000),
            verifier.lastData,
        )
    }

    @Test fun missingSignature_rejectedWithoutVerifying() {
        val verifier = CapturingVerifier(result = true)
        assertFalse(server(verifier, MutableClock()).authorizedPull(pull(atMs = 10_000, signature = null), holderPeerId))
        assertEquals(0, verifier.calls)
    }

    @Test fun staleTimestamp_rejected() {
        val clock = MutableClock(10_000)
        val verifier = CapturingVerifier(result = true)
        // 6s skew exceeds the 5s window.
        assertFalse(server(verifier, clock).authorizedPull(pull(atMs = 16_001), holderPeerId))
    }

    @Test fun malformedRecipientDid_rejected() {
        val clock = MutableClock(10_000)
        val verifier = CapturingVerifier(result = true)
        val bad = pull(atMs = 10_000).copy(recipientDid = "did:key:notvalid")
        assertFalse(server(verifier, clock).authorizedPull(bad, holderPeerId))
    }

    @Test fun verifierRejection_rejected() {
        val clock = MutableClock(10_000)
        assertFalse(server(CapturingVerifier(result = false), clock).authorizedPull(pull(atMs = 10_000), holderPeerId))
    }

    @Test fun tokenBoundToHolder_wrongHolderFails() {
        val clock = MutableClock(10_000)
        // Recipient signed for holderPeerId; the verifier checks the payload it is given.
        // A different holder presents its own peerId, so the signed payload differs and a
        // real verifier would reject. Here we assert the payload handed to verify is
        // holder-specific (the binding), using a capturing verifier.
        val verifier = CapturingVerifier(result = true)
        val otherHolder = "12D3KooWDifferentHolder1111111111111111111111"
        server(verifier, clock).authorizedPull(pull(atMs = 10_000), otherHolder)
        assertContentEquals(
            MailboxRequest.pullSignable(recipientDid, otherHolder, 10_000),
            verifier.lastData,
        )
    }
}
